#!/usr/bin/env bash
# 若依 RuoYi-Vue Docker 一键部署脚本
# 在 Linux 云主机上执行,用法:
#   ./deploy.sh init      # 首次初始化(检查文件、构建镜像、启动服务)
#   ./deploy.sh up        # 启动所有服务
#   ./deploy.sh down      # 停止所有服务
#   ./deploy.sh restart   # 重启所有服务
#   ./deploy.sh logs      # 查看实时日志
#   ./deploy.sh status    # 查看服务状态
#   ./deploy.sh backup    # 备份 MySQL
#   ./deploy.sh ssl       # 申请 Let's Encrypt 证书
#   ./deploy.sh build     # 重新构建前后端镜像
#   ./deploy.sh migrate   # 打包迁移用(生成迁移包)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 颜色
G='\033[0;32m'
Y='\033[1;33m'
R='\033[0;31m'
N='\033[0m'
log()  { echo -e "${G}[deploy]${N} $1"; }
warn() { echo -e "${Y}[warn]${N} $1"; }
err()  { echo -e "${R}[error]${N} $1"; exit 1; }

# 读取 .env 里的变量
get_env() {
    local key="$1"
    grep "^${key}=" .env 2>/dev/null | head -1 | cut -d'=' -f2- | tr -d '\r'
}

check_env_file() {
    if [ ! -f .env ]; then
        if [ -f .env.example ]; then
            warn ".env 不存在,从 .env.example 拷贝..."
            cp .env.example .env
            warn "请编辑 .env 修改密码和域名后重新运行"
            exit 1
        else
            err ".env 和 .env.example 都不存在"
        fi
    fi
}

check_docker() {
    command -v docker >/dev/null 2>&1 || err "docker 未安装,请先安装 Docker 和 Docker Compose"
    docker compose version >/dev/null 2>&1 || err "docker compose 不可用,请安装 Docker Compose V2"
}

cmd_init() {
    log "开始首次初始化..."
    check_env_file
    check_docker

    # 方案C: Docker 多阶段构建,Dockerfile 内自动编译打包,无需检查 jar/dist

    # 检查 SQL 脚本
    if [ -z "$(ls -A mysql/init/*.sql 2>/dev/null)" ]; then
        warn "mysql/init/ 下没有 .sql 脚本"
        warn "请把若依的 ry_*.sql 拷到 mysql/init/(见 mysql/init/README.md)"
        warn "首次启动数据库需要这些脚本建表和初始数据"
        echo ""
        warn "是否继续?(数据库会启动但无表结构) [y/N]"
        read -r ans
        [ "$ans" = "y" ] || exit 1
    fi

    # 创建数据目录
    log "创建数据目录..."
    mkdir -p data/mysql data/redis data/logs data/uploadPath data/nginx/logs

    # 构建镜像
    log "构建镜像(首次较慢)..."
    docker compose build

    # 启动
    log "启动服务..."
    docker compose up -d

    log "等待服务就绪(最多 60 秒)..."
    for i in $(seq 1 60); do
        if docker compose ps | grep -q "healthy"; then
            log "服务已就绪"
            break
        fi
        sleep 1
        [ $i -eq 60 ] && warn "部分服务可能还在启动中,请用 ./deploy.sh status 查看"
    done

    echo ""
    log "===== 初始化完成 ====="
    log "查看状态: ./deploy.sh status"
    log "查看日志: ./deploy.sh logs"
    log "申请证书: ./deploy.sh ssl"
    log "默认账号: admin / admin123 (登录后请立即改密码!)"
}

cmd_up()      { check_env_file; docker compose up -d; log "服务已启动"; }
cmd_down()    { docker compose down; log "服务已停止"; }
cmd_restart() { docker compose restart; log "服务已重启"; }
cmd_logs()     { docker compose logs -f --tail=200; }
cmd_status()   { docker compose ps; }

cmd_backup() {
    check_env_file
    local pwd_val
    pwd_val="$(get_env MYSQL_ROOT_PASSWORD)"
    local db
    db="$(get_env MYSQL_DATABASE || echo ry)"
    local ts
    ts="$(date +%Y%m%d_%H%M%S)"
    local file="mysql/backup/ry_${ts}.sql.gz"
    mkdir -p mysql/backup
    log "备份 MySQL 数据库 $db 到 $file ..."
    docker compose exec -T mysql sh -c "mysqldump -uroot -p'${pwd_val}' --databases ${db} --single-transaction --routines --triggers --events | gzip" > "$file"
    # 保留最近 30 天
    find mysql/backup -name "*.sql.gz" -mtime +30 -delete 2>/dev/null || true
    log "备份完成: $file ($(du -h "$file" | cut -f1))"
}

cmd_ssl() {
    check_env_file
    local domain
    domain="$(get_env DOMAIN)"
    if [ -z "$domain" ] || [ "$domain" = "your-domain.com" ]; then
        err "请先在 .env 里设置 DOMAIN 为你的真实域名"
    fi
    log "申请 Let's Encrypt 证书 for $domain ..."
    mkdir -p nginx/certs/acme
    # 用 certbot 容器以 webroot 方式申请
    docker run --rm \
        -v "$PWD/nginx/certs/acme:/acme" \
        -v "$PWD/nginx/certs:/etc/nginx/certs" \
        certbot/certbot certonly \
        --webroot -w /acme \
        -d "$domain" \
        --non-interactive --agree-tos \
        --email "admin@${domain}" \
        --no-eff-email

    # certbot 把证书放在 /etc/letsencrypt/live/<domain>/ 下
    # 拷贝到 nginx/certs/
    local src="/etc/letsencrypt/live/${domain}"
    log "拷贝证书文件..."
    docker run --rm \
        -v "$PWD/nginx/certs:/out" \
        -v "/etc/letsencrypt:/letsencrypt:ro" \
        alpine sh -c "cp /letsencrypt/live/${domain}/fullchain.pem /out/fullchain.pem && cp /letsencrypt/live/${domain}/privkey.pem /out/privkey.pem"

    log "证书已就位: nginx/certs/fullchain.pem, privkey.pem"
    warn "现在编辑 nginx/nginx.conf,取消 443 server 块的注释,启用 HTTPS"
    warn "然后重启: ./deploy.sh restart"
}

cmd_build() {
    check_env_file
    log "重新构建前后端镜像..."
    docker compose build ruoyi-admin ruoyi-ui
    log "重启服务..."
    docker compose up -d ruoyi-admin ruoyi-ui
    log "完成"
}

cmd_migrate() {
    check_env_file
    local out="ruoyi-migrate-$(date +%Y%m%d).tar.gz"
    log "生成迁移包 $out ..."
    tar czf "$out" \
        docker-compose.yml \
        .env \
        deploy.sh \
        nginx/nginx.conf \
        nginx/certs/.gitkeep \
        ruoyi-admin/Dockerfile \
        ruoyi-admin/ruoyi-admin.jar \
        ruoyi-ui/Dockerfile \
        ruoyi-ui/nginx.conf \
        ruoyi-ui/dist \
        mysql/conf/my.cnf \
        mysql/init \
        redis/redis.conf \
        2>/dev/null || true
    log "迁移包生成完成: $out"
    log "在新机器上:"
    log "  1. 安装 Docker + Compose"
    log "  2. 解压迁移包: tar xzf $out"
    log "  3. 拷贝数据卷: data/ 目录(含 MySQL/Redis 数据)"
    log "  4. 启动: ./deploy.sh up"
}

case "${1:-}" in
    init)    cmd_init ;;
    up)      cmd_up ;;
    down)    cmd_down ;;
    restart) cmd_restart ;;
    logs)    cmd_logs ;;
    status)  cmd_status ;;
    backup)  cmd_backup ;;
    ssl)     cmd_ssl ;;
    build)   cmd_build ;;
    migrate) cmd_migrate ;;
    *)
        echo "若依 Docker 部署脚本"
        echo ""
        echo "用法: $0 <命令>"
        echo ""
        echo "命令:"
        echo "  init      首次初始化(检查文件、构建镜像、启动服务)"
        echo "  up        启动所有服务"
        echo "  down      停止所有服务"
        echo "  restart   重启所有服务"
        echo "  logs      查看实时日志(Ctrl+C 退出)"
        echo "  status    查看服务状态"
        echo "  backup    备份 MySQL(生成 .sql.gz,保留 30 天)"
        echo "  ssl       申请 Let's Encrypt HTTPS 证书"
        echo "  build     重新构建前后端镜像(改完代码后用)"
        echo "  migrate   生成迁移包(迁到新服务器用)"
        exit 1
        ;;
esac
