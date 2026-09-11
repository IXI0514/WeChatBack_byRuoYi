# 14 · 若依 RuoYi-Vue Docker 部署套件

日常发布：[12 · 服务器增量更新](../ruoyi-source/docs/服务器增量更新.md)。全部文档：[00 · 文档目录](../ruoyi-source/docs/文档目录.md)。

把若依（登录 + 后台管理 + 接口服务）一键部署到公网云主机，含 MySQL、Redis、后端、前端、Nginx 反代 + HTTPS。

## 目录结构

```
ruoyi-deploy/
├── docker-compose.yml          # 编排: mysql + redis + admin + ui + nginx
├── .env.example                # 环境变量模板(拷成 .env 后改)
├── .gitignore
├── deploy.sh                   # 一键部署/备份/迁移脚本
├── README.md                   # 本文件
├── ruoyi-admin/
│   ├── Dockerfile              # 后端镜像(基于 eclipse-temurin:8-jre)
│   └── ruoyi-admin.jar         # ← 需自己拷过来(打包产物)
├── ruoyi-ui/
│   ├── Dockerfile              # 前端镜像(基于 nginx:alpine)
│   ├── nginx.conf              # 前端容器内 nginx(服务 dist + 反代 API)
│   └── dist/                   # ← 需自己拷过来(打包产物)
├── nginx/
│   ├── nginx.conf              # 顶层反代 + HTTPS
│   └── certs/                  # SSL 证书目录(fullchain.pem, privkey.pem)
├── mysql/
│   ├── conf/my.cnf             # MySQL 8 配置
│   ├── init/                   # ← 把若依 ry_*.sql 拷到这里(首次启动自动执行)
│   └── backup/                 # 备份目录
├── redis/
│   └── redis.conf              # Redis 7 配置
└── data/                       # 数据卷(MySQL/Redis/日志/上传,自动生成)
```

## 前置条件

| 项目 | 要求 |
|---|---|
| 云主机 | 2核 4G 起步,50G 系统盘,CentOS 7+ / Ubuntu 20.04+ |
| Docker | 20.10+ 和 Docker Compose V2 |
| 域名 | 已备案(国内云主机要求),A 记录解析到云主机 IP |
| 端口 | 安全组开放 80、443、22 |
| 若依源码 | RuoYi-Vue 官方仓库 |

## 完整部署步骤

### 第一步: 准备若依源码并打包

#### 1.1 克隆若依源码
```bash
git clone https://gitee.com/y_project/RuoYi-Vue.git
cd RuoYi-Vue
```

#### 1.2 改后端配置

编辑 `ruoyi-admin/src/main/resources/application-druid.yml`，把数据库连接改成容器内的：

```yaml
spring:
  datasource:
    druid:
      master:
        url: jdbc:mysql://mysql:3306/ry?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=Asia/Shanghai
        username: root
        password: ChangeMe_StrongPwd_2026   # 与 .env 里 MYSQL_ROOT_PASSWORD 一致
```

编辑 `ruoyi-admin/src/main/resources/application.yml`，把 Redis 连接改成容器内的：

```yaml
spring:
  redis:
    host: redis
    port: 6379
    password: ChangeMe_RedisPwd_2026       # 与 .env 里 REDIS_PASSWORD 一致
```

#### 1.3 打包后端 jar
```bash
# 在若依源码根目录
mvn clean package -DskipTests
# 产物: ruoyi-admin/target/ruoyi-admin.jar
```

#### 1.4 打包前端 dist
```bash
cd ruoyi-ui
npm config set registry https://registry.npmmirror.com
npm install --legacy-peer-deps
npm run build:prod
# 产物: ruoyi-ui/dist/
cd ..
```

### 第二步: 拷贝产物到部署目录

```bash
# 假设若依源码在 ~/RuoYi-Vue,部署套件在 ~/ruoyi-deploy
cp ~/RuoYi-Vue/ruoyi-admin/target/ruoyi-admin.jar ~/ruoyi-deploy/ruoyi-admin/
cp -r ~/RuoYi-Vue/ruoyi-ui/dist ~/ruoyi-deploy/ruoyi-ui/

# 拷贝 SQL 脚本(MySQL 首次启动会自动执行)
cp ~/RuoYi-Vue/sql/ry_*.sql ~/ruoyi-deploy/mysql/init/
```

### 第三步: 配置环境变量

```bash
cd ~/ruoyi-deploy
cp .env.example .env
vi .env
```

**必须改的项：**
- `MYSQL_ROOT_PASSWORD` —— 改成强密码
- `REDIS_PASSWORD` —— 改成强密码

**注意：** `.env` 里的密码要和第一步改的 `application-druid.yml`、`application.yml` 里的密码完全一致。`redis/redis.conf` 里的 `requirepass` 也要改成同一个 Redis 密码。

### 第四步: 一键部署

```bash
chmod +x deploy.sh
./deploy.sh init
```

脚本会：
1. 检查 jar、dist、SQL 文件是否就位
2. 创建数据目录
3. 构建前后端镜像
4. 启动所有容器
5. 等待健康检查通过

完成后访问 `http://<云主机IP>` 即可看到若依登录页。
默认账号：`admin / admin123`（**登录后请立即在系统里改密码**）

### 第五步: 申请 HTTPS 证书（可选，推荐）

```bash
# 先在 .env 里设好 DOMAIN
./deploy.sh ssl
```

脚本会用 certbot 容器申请 Let's Encrypt 免费证书，证书文件放在 `nginx/certs/`。

然后编辑 `nginx/nginx.conf`，取消 443 server 块的注释，启用 HTTPS 跳转：

```nginx
# 在 80 server 块的 location / 里取消注释:
location / {
    return 301 https://$host$request_uri;
}
```

重启 nginx：
```bash
./deploy.sh restart
```

## 日常运维命令

| 命令 | 作用 |
|---|---|
| `./deploy.sh status` | 查看所有容器状态 |
| `./deploy.sh logs` | 查看实时日志（Ctrl+C 退出） |
| `./deploy.sh up` | 启动所有服务 |
| `./deploy.sh down` | 停止所有服务 |
| `./deploy.sh restart` | 重启所有服务 |
| `./deploy.sh build` | 改完代码后重新构建镜像 |
| `./deploy.sh backup` | 备份 MySQL（生成 .sql.gz，保留 30 天） |
| `./deploy.sh ssl` | 申请/续期 HTTPS 证书 |
| `./deploy.sh migrate` | 生成迁移包（迁新服务器用） |

## 迁移到新服务器（3 步搞定）

这是 Docker 部署最大的优势——迁移几乎零成本。

```bash
# === 在旧机器上 ===
./deploy.sh down                    # 停服务
./deploy.sh backup                  # 备份数据库
./deploy.sh migrate                 # 生成迁移包 ruoyi-migrate-YYYYMMDD.tar.gz
# 拷贝数据卷(关键!):
tar czf ruoyi-data.tar.gz data/     # MySQL/Redis 数据

# === 在新机器上 ===
# 1. 安装 Docker + Docker Compose V2
# 2. 解压迁移包
tar xzf ruoyi-migrate-YYYYMMDD.tar.gz
# 3. 恢复数据卷
tar xzf ruoyi-data.tar.gz
# 4. 启动
./deploy.sh up
```

新机器 5 分钟内服务恢复，环境完全一致，无需重装 JDK/MySQL/Redis/Nginx。

## 备份与恢复

### 自动备份（推荐加 cron）

```bash
# 编辑 crontab
crontab -e

# 每天凌晨 3 点备份
0 3 * * * cd ~/ruoyi-deploy && ./deploy.sh backup >> /var/log/ruoyi-backup.log 2>&1
```

### 手动恢复

```bash
# 停服务
./deploy.sh down

# 清空 MySQL 数据卷(谨慎!)
rm -rf data/mysql/*

# 把备份的 SQL 导入(先启动 MySQL 单独一个)
docker compose up -d mysql
sleep 30

# 解压并导入
gunzip < mysql/backup/ry_20260101_030000.sql.gz | docker compose exec -T mysql \
    mysql -uroot -p'<你的MySQL密码>'

# 重启全部
docker compose up -d
```

## 小程序对接

你的后端给小程序提供接口服务（验证用户、存储数据），这部分说明小程序怎么对接。

### HTTPS 必须先配好

微信小程序强制要求 HTTPS，HTTP 接口无法在小程序里调用。所以：

1. 先完成上面的"第五步: 申请 HTTPS 证书"
2. 启用 nginx 443 配置
3. 小程序通过 `https://your-domain.com/prod-api/xxx` 调接口

### 接口前缀约定

当前架构支持两种接口前缀，按你的小程序代码选择：

| 前缀 | 用途 | 状态 |
|---|---|---|
| `/prod-api/` | 若依 PC 端默认前缀 | 已启用，顶层 nginx 直接反代到后端 |
| `/api/` 或自定义 | 小程序专用前缀 | 默认注释，按需启用 |

**如果你的小程序接口用的是 `/api/` 或其他前缀**，编辑 `nginx/nginx.conf`，在 80 和 443 的 server 块里取消 `/api/` location 的注释，把前缀改成你的：

```nginx
location /api/ {
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_pass http://ruoyi_admin/;
}
```

改完重启 nginx：`./deploy.sh restart`

### 微信小程序后台配置

1. 登录 [微信公众平台](https://mp.weixin.qq.com)
2. 开发 → 开发管理 → 开发设置 → 服务器域名
3. **request 合法域名**里加上：`https://your-domain.com`
4. 如果有 WebSocket，也加到 socket 合法域名

### 小程序用户认证

你的后端给小程序做用户验证，建议用 JWT token 方式（无状态，适合小程序）：

1. 小程序 `wx.login()` 拿 code
2. 调后端 `POST /api/login/verify`，传小程序标识及用户信息；部署在统一入口时由 Nginx 转发到后端
3. 后端用 code 换 openid，生成 JWT token 返回
4. 小程序存 token，后续请求带 `Authorization: Bearer <token>` 头
5. 后端写一个 JWT 拦截器验证 token（若依自带 Spring Security，加一个 filter 即可）

注意：小程序用户和若依后台 admin 用户是两套体系，不要混用。建议小程序用户单独建表（如 `miniapp_user`），单独的登录接口和权限拦截。

### 前端管理页

你定制的两个管理页（接口日志查看、会员状态管理）属于 PC 端后台功能，跟着 `dist` 打包，不需要额外部署。登录后台后在菜单里能看到。

## 常见问题

### Q: 启动后访问 80 端口打不开？

```bash
# 1. 检查容器状态
./deploy.sh status

# 2. 看日志
./deploy.sh logs

# 3. 常见原因
#    - 后端 jar 没拷贝: 重新 mvn 打包并拷贝
#    - 前端 dist 没拷贝: 重新 npm build 并拷贝
#    - MySQL 没初始化 SQL: 检查 mysql/init/ 下有没有 .sql
#    - 端口被占: lsof -i:80 看看谁占了
```

### Q: MySQL 连接失败？

后端连 MySQL 用的是容器名 `mysql`，不是 `localhost`。确认：
- `application-druid.yml` 里 url 是 `jdbc:mysql://mysql:3306/ry`
- `.env` 里 `MYSQL_ROOT_PASSWORD` 和 yml 里 password 一致

### Q: Redis 连接失败？

同理，后端连 Redis 用容器名 `redis`。确认：
- `application.yml` 里 host 是 `redis`
- `redis/redis.conf` 里 `requirepass` 和 `.env` 里 `REDIS_PASSWORD` 一致

### Q: HTTPS 证书申请失败？

```bash
# 1. 确认域名 A 记录已解析到云主机 IP
dig your-domain.com

# 2. 确认安全组开放了 80 端口(Let's Encrypt 验证要走 80)
# 3. 确认 .env 里 DOMAIN 是真实域名
```

### Q: 想重新初始化数据库？

```bash
./deploy.sh down
rm -rf data/mysql/*           # 清空 MySQL 数据卷
docker compose up -d mysql   # 重启 MySQL,会重新执行 init/ 下的 SQL
sleep 30
docker compose up -d         # 启动全部
```

### Q: 怎么调试 MySQL/Redis（不对外暴露端口）？

临时开放端口，编辑 `docker-compose.yml`，取消 mysql 服务的 `ports` 注释：
```yaml
ports:
  - "127.0.0.1:3306:3306"   # 只本机访问
```
然后 `./deploy.sh restart`，用 `mysql -h 127.0.0.1 -P 3306 -uroot -p` 连接。

## 安全加固清单

部署上线前请逐项检查：

- [ ] 改掉 MySQL root 密码（`.env` 里 `MYSQL_ROOT_PASSWORD`）
- [ ] 改掉 Redis 密码（`.env` 和 `redis/redis.conf` 里 `requirepass`）
- [ ] 登录若依后台，改掉默认 `admin/admin123` 密码
- [ ] 启用 HTTPS（`./deploy.sh ssl` + 取消 nginx 443 配置注释）
- [ ] 云主机安全组只开 22/80/443，不开 3306/6379/8080
- [ ] 配置 MySQL 每日备份 cron
- [ ] 设置 `fail2ban` 防 SSH 爆破
- [ ] 关闭 root SSH 登录，改用密钥

## 技术栈版本

| 组件 | 版本 |
|---|---|
| MySQL | 8.0 |
| Redis | 7-alpine |
| JDK | 8 (eclipse-temurin:8-jre) |
| Nginx | 1.25-alpine |
| 若依 | RuoYi-Vue (Spring Boot 2.x + Vue 2) |

## 学习成本

- 会用 `docker compose up/down/logs/ps` 这几条命令即可
- 半小时上手，迁移受用终身
- 详见 `deploy.sh` 封装了所有操作，不用记 docker 命令
