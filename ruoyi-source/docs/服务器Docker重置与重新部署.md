# 13 · 服务器 Docker 重置与重新部署（不保留数据）

## 快捷脚本方式（推荐用于已准备 env/证书的环境）

本地私有目录 `ruoyi-source/docs/quickdeploy/` 提供 `reset-deploy.sh`、`validate.py` 和编号 17 的 README；该目录整体排除 Git，需手动上传为服务器 `/home/ubuntu/mywebshow/quickdeploy/`，包含隐藏文件 `.env` 以及 `73ham.top.pem/key`。

使用 ubuntu 账号执行（不要 sudo bash）：

```bash
cd /home/ubuntu/mywebshow
bash quickdeploy/reset-deploy.sh --check
# 检查通过后正式执行，按中文提示确认清空：
bash quickdeploy/reset-deploy.sh
```

脚本先下载当前 Gitee 分支并预构建，之后才确认清空。仅清空源码和部署目录、还原 Git 跟踪文件，保留根目录 quickdeploy；不删除整个项目根目录，不清理其他 Docker 项目。自动准备 SQL（含 Quartz）、配置、证书，并验证 MySQL 实际查询、Redis PONG、HTTPS 和应用入口。失败停止，不自动回滚。

**脚本方式与下面手动方式二选一，不要混用。** 下文删除整个根目录的命令会连同 quickdeploy 一起删除；使用快捷脚本时不要执行它。脚本仍会永久删除业务数据，不做备份。

## 手动方式

适用目录：`/home/ubuntu/mywebshow`。本文用于彻底清空当前项目的 Docker 容器、MySQL、Redis、上传文件、日志、`.env` 和本地证书，再从 Gitee 重新拉取源码并部署。

> **不可恢复：** 以下流程会删除当前项目全部业务数据、数据库、Redis 缓存、上传文件与服务器上未提交的源码改动。你已确认不需要备份或保留；如果日后需要保留数据，请不要使用本文。

本文默认使用 Ubuntu 用户，不创建新系统账号。Docker 命令统一使用 `sudo`。

## 1. 记录 Git 地址并停止项目

在删除项目目录前，从现有仓库读取 Gitee remote 地址。不要手工猜测仓库地址。

```bash
cd /home/ubuntu/mywebshow
test -d .git && echo "当前目录是 Git 仓库"
repository_url="$(git config --get remote.origin.url)"
test -n "$repository_url" && echo "已读取 Gitee 远程地址"
git status --short
```

最后一行如有输出，代表服务器存在未提交改动；本流程会将它们永久删除。

停止并移除 **mywebshow 项目** 的容器和网络：

```bash
cd /home/ubuntu/mywebshow/ruoyi-deploy
sudo docker compose down --remove-orphans
sudo docker compose ps -a
```

## 2. 删除当前项目目录

先离开项目目录，再核验待删除路径。命令只删除已明确指定的 `/home/ubuntu/mywebshow`，不清理 `/home/ubuntu` 下其他内容。

```bash
cd /home/ubuntu
test -d /home/ubuntu/mywebshow/ruoyi-deploy && echo "删除目标已确认"
sudo rm -rf -- /home/ubuntu/mywebshow
test ! -e /home/ubuntu/mywebshow && echo "旧项目已删除"
```

此操作会同时删除 bind mount 数据目录，因此原来的 MySQL、Redis、上传文件、Nginx 日志和 `.env` 均不会保留。

## 3. 可选：清理本项目遗留镜像

重新构建会自动替换应用镜像，因此这一步通常可以跳过。若只想删除本项目的应用镜像，可先核对名称后执行：

```bash
sudo docker images
sudo docker image rm ruoyi-admin:latest ruoyi-ui:latest
```

MySQL、Redis、Nginx 官方镜像可以保留，下次部署会复用或自动拉取。

> 不要为了本项目重置执行 `docker system prune -a --volumes`，除非你已确认整台服务器没有其他 Docker 项目。该命令会清理服务器上所有未使用的容器、镜像、缓存与卷。

## 4. 从 Gitee 重新拉取源码

同一个终端中继续使用第 1 节保存的 `repository_url`。若已关闭终端，请从 Gitee 页面复制当前仓库地址后重新设置该变量。

```bash
cd /home/ubuntu
git clone "$repository_url" /home/ubuntu/mywebshow
cd /home/ubuntu/mywebshow
git status --short
```

若 Git 提示认证失败，请先修复 Gitee SSH Key 或 Personal Access Token；不要把 Token 写进 Git 仓库、脚本或 `.env`。

## 5. 重新创建部署配置与 HTTPS 证书

`.env` 和证书已随旧目录删除，必须重新创建。

```bash
cd /home/ubuntu/mywebshow/ruoyi-deploy
cp .env.example .env
chmod 600 .env
nano .env
```

至少设置以下值：

```dotenv
MYSQL_ROOT_PASSWORD=自行设置强密码
MYSQL_DATABASE=ry
REDIS_PASSWORD=自行设置强密码
DOMAIN=73ham.top
```

确认 `73ham.top` 已添加 A 记录并指向当前服务器。从阿里云下载覆盖 `73ham.top` 的 **Nginx（pem/key）** 证书，上传到服务器后保存为：

```bash
cd /home/ubuntu/mywebshow/ruoyi-deploy
mkdir -p nginx/certs
# 将上传后的实际文件名替换到下面两条命令中
cp /home/ubuntu/webfile/73ham.top.pem nginx/certs/fullchain.pem
cp /home/ubuntu/webfile/73ham.top.key nginx/certs/privkey.pem
chmod 600 nginx/certs/fullchain.pem nginx/certs/privkey.pem

test -s nginx/certs/fullchain.pem && test -s nginx/certs/privkey.pem && echo "HTTPS 证书已就位"
```

当前 Nginx 配置启用了 HTTPS；未准备好这两个文件时，Nginx 会启动失败。

## 6. 准备首次初始化 SQL

全新 MySQL 数据目录只会执行一次 `mysql/init/` 下的 SQL。按顺序放入初始化脚本：

```bash
cd /home/ubuntu/mywebshow/ruoyi-deploy
mkdir -p mysql/init

cp ../ruoyi-source/docs/sql/1-基础数据.sql mysql/init/01-base.sql
cp ../ruoyi-source/docs/sql/2-菜单初始化SQL.sql mysql/init/02-miniapp-menu.sql
cp ../ruoyi-source/docs/sql/4-小程序建表脚本.sql mysql/init/03-miniapp-table.sql
cp ../ruoyi-source/docs/sql/6-中继台管理增量.sql mysql/init/04-repeater.sql

ls -lah mysql/init
```

`4-小程序建表脚本.sql` 含有重建表逻辑，仅适用于当前这种全新、无数据的环境。

## 7. 全新构建并启动所有服务

这一步会创建新的 MySQL、Redis、后端、前端与 Nginx 容器：

```bash
cd /home/ubuntu/mywebshow/ruoyi-deploy
sudo docker compose config --quiet && echo "Compose 配置有效"
sudo docker compose build --no-cache ruoyi-admin ruoyi-ui
sudo docker compose up -d
sudo docker compose ps
```

首次构建会下载 Maven、Node 依赖和镜像，耗时较长。构建过程中可查看日志：

```bash
sudo docker compose logs -f --tail=100 mysql redis ruoyi-admin ruoyi-ui nginx
```

## 8. 验收 MySQL、Redis、Nginx、首页和后台

```bash
cd /home/ubuntu/mywebshow/ruoyi-deploy

sudo docker compose exec -T mysql sh -c 'mysqladmin ping -h localhost -uroot -p"$MYSQL_ROOT_PASSWORD"'
sudo docker compose exec -T redis sh -c 'redis-cli --no-auth-warning -a "$REDIS_PASSWORD" ping'
sudo docker compose exec nginx nginx -t

curl -I https://73ham.top/
curl -I https://73ham.top/admin/
curl -I https://73ham.top/admin/login
```

预期结果：

- MySQL 输出 `mysqld is alive`；
- Redis 输出 `PONG`；
- Nginx 输出 `syntax is ok` 和 `test is successful`；
- `https://73ham.top/` 显示公开静态首页；
- `https://73ham.top/admin/login` 显示后台登录页；
- 小程序调用 `https://73ham.top/api/...`。

## 9. 最后安全检查

- `.env`、`nginx/certs/privkey.pem` 均应为 `600` 权限，且不提交 Git。
- 云服务器安全组仅开放 `22`、`80`、`443`，不要开放 `3306`、`6379`、`8080`。
- 微信小程序后台的 request/upload/download 合法域名填写 `https://73ham.top`。
- 后台地址固定使用 `https://73ham.top/admin/`；根域名用于公开静态首页。
