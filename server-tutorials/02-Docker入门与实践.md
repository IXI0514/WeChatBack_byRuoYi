# 43 · 第 2 篇：Docker 入门与服务器部署实践

> 定位：Docker 是完成第一阶段目标后的可选部署方式，不是静态网站、域名和 HTTPS 的前置条件。  
> 前置：理解[第 1 篇](./01-Nginx入门与实践.md)中的 Nginx、反向代理和回环端口。  
> 本篇不开发后台程序，只学习如何运行开发团队交付的镜像。

本课程保留宿主机 Nginx 作为唯一公网入口：

```text
公网 80/443 ─▶ 宿主机 Nginx ─▶ 127.0.0.1:8081 ─▶ Docker 容器
```

容器不直接占用 80/443，后台容器端口也不暴露到公网。

---

## 2.1 镜像、容器、仓库与数据

| 概念 | 简单理解 | 是否应保存业务数据 |
| --- | --- | --- |
| 镜像 Image | 应用及运行环境的只读交付包 | 否 |
| 容器 Container | 镜像启动后的进程及隔离环境 | 不应依赖容器可写层保存 |
| 仓库 Registry | 存储、分发镜像的服务 | 存镜像，不存运行数据 |
| 卷 Volume / 挂载目录 | 独立于容器生命周期的数据位置 | 是 |

```text
仓库 ──pull──▶ 镜像 ──run──▶ 容器
                         └──挂载──▶ 配置与持久化数据
```

删除容器不等于删除镜像；重建容器也不应丢失数据库或上传文件。能丢弃并重建是容器的重要使用方式。

---

## 2.2 【可选：需要容器部署时】安装 Docker Engine

生产或长期学习服务器优先使用 Docker 官方 apt 仓库。官方便捷脚本主要适合测试/开发，不作为这里的默认方案。

如果系统曾安装 `docker.io` 等冲突包，先按 Docker 官方文档检查冲突列表；不要在不知道数据位置时直接卸载已有 Docker。

### 添加官方仓库

```bash
sudo apt update
sudo apt install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

sudo tee /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF
```

这里动态读取 Ubuntu 代号，不把 `jammy` 硬编码到其他系统上。

这段命令较长，逐项拆解：

- `install -m 0755 -d /etc/apt/keyrings`：创建存放软件仓库密钥的目录，并设为所有人可读取/进入、只有 root 可写；
- `curl -fsSL URL -o 文件`：`f` 遇到 HTTP 错误即失败，`s` 隐藏进度，`S` 仍显示错误，`L` 跟随重定向，`-o` 指定保存位置；
- `chmod a+r`：为 all（所有用户类别）增加 read（读）权限，使 apt 能读取公钥；
- `tee ... <<EOF`：把随后直到单独一行 `EOF` 之间的多行内容写入 `docker.sources`；
- `$(...)`：先执行括号内命令，再把结果写进配置；
- `. /etc/os-release`：读取系统发布信息到当前 shell；`${UBUNTU_CODENAME:-$VERSION_CODENAME}` 优先取前者，没有时用后者；
- `dpkg --print-architecture`：输出当前系统的软件包架构，例如 `amd64` 或 `arm64`；
- `Signed-By`：限定这个仓库只信任刚保存的 Docker 公钥。

写入后可执行 `cat /etc/apt/sources.list.d/docker.sources` 检查：`Suites` 应是当前 Ubuntu 代号，Ubuntu 22.04 通常为 `jammy`；`Architectures` 应与服务器架构一致。

### 安装并验证

```bash
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
sudo systemctl status docker --no-pager
sudo docker run --rm hello-world
sudo docker compose version
```

`docker run --rm hello-world` 会从镜像创建并运行一次测试容器；`--rm` 表示进程结束后自动删除该容器，不会删除下载的镜像。`docker compose version` 中的 `compose` 是新版 Docker CLI 插件子命令，与旧式独立命令 `docker-compose` 不同。

**【验证结果】**

- `systemctl status docker` 应显示 `Active: active (running)`；`enabled` 表示 Docker 会随服务器启动。
- `hello-world` 输出中应明确表示 Docker 安装可以正常工作。这个测试容器完成后会退出，配合 `--rm` 会自动删除，因此之后在 `docker ps` 中看不到它是正常的。
- `docker compose version` 应输出版本号，而不是 `docker: 'compose' is not a docker command`。

中国内地网络若无法访问 Docker 官方仓库或镜像仓库，应从当前云厂商控制台/官方文档取得可用镜像服务。不要长期复制来源不明或已经失效的公共加速地址，也不要让新配置覆盖 `/etc/docker/daemon.json` 中已有的日志、网络等设置。

### 【可选・谨慎】让普通用户直接运行 Docker

```bash
sudo usermod -aG docker deploy
```

重新登录后 `deploy` 可以不加 `sudo` 执行 Docker。必须理解：`docker` 组通常具有接近 root 的控制能力。多人服务器应严格限制成员；课程中也可以始终使用 `sudo docker ...`。

---

## 2.3 最常用的生命周期命令

```bash
sudo docker image ls                         # 本机镜像
sudo docker pull nginx:alpine                # 拉取教程镜像
sudo docker ps                               # 运行中的容器
sudo docker ps -a                            # 包括已停止容器
sudo docker logs --tail 100 容器名            # 最近日志
sudo docker logs -f 容器名                    # 实时日志，Ctrl+C 退出
sudo docker inspect 容器名                    # 完整配置与状态
sudo docker stats                            # 实时资源使用
sudo docker stop 容器名
sudo docker start 容器名
sudo docker restart 容器名
sudo docker rm 容器名                         # 删除已停止容器
```

- `logs --tail 100` 只显示最后 100 行，避免一次输出过多；`logs -f` 中的 `f` 是 follow，持续等待新日志；
- `inspect` 输出 Docker 保存的完整 JSON 元数据，常用来确认端口、挂载、环境变量、退出码和健康检查；
- `stats` 会持续刷新 CPU、内存和网络用量，按 `Ctrl+C` 退出；
- `rm` 删除的是容器记录和可写层，不等于删除镜像；删除真实容器前先确认持久化数据已挂载。

教程可以使用 `nginx:alpine` 方便练习；真实部署应使用团队测试过的版本标签，要求更高时固定镜像摘要 digest。浮动标签内容可能变化，不能天然保证可重复发布。

### `docker run` 的关键参数

```bash
sudo docker run -d \
  --name demo-service \
  -p 127.0.0.1:8081:80 \
  --restart unless-stopped \
  --memory 256m \
  --cpus 0.5 \
  nginx:alpine
```

| 参数 | 作用 |
| --- | --- |
| `-d` | 后台运行 |
| `--name` | 固定容器名，方便管理 |
| `-p 127.0.0.1:8081:80` | 宿主机回环 8081 转到容器 80 |
| `--restart unless-stopped` | 异常/重启后恢复，手工停止则保持停止 |
| `--memory`、`--cpus` | 限制单个容器资源 |

端口映射顺序是 `宿主机IP:宿主机端口:容器端口`。

```bash
-p 8081:80                    # 通常发布到所有宿主机地址，可能被公网直连
-p 127.0.0.1:8081:80          # 只允许宿主机本机访问，适合交给 Nginx 反代
```

Docker 发布端口可能绕过一些 UFW 使用者预期的过滤路径。安全不能只靠 UFW：还要绑定 `127.0.0.1`，并确保云安全组不放行后台端口。

---

## 2.4 【推荐练习】启动容器并接入宿主机 Nginx

### 步骤 1：启动测试容器

```bash
sudo docker run -d \
  --name demo-service \
  -p 127.0.0.1:8081:80 \
  --restart unless-stopped \
  nginx:alpine
```

验证容器与监听地址：

```bash
sudo docker ps
curl -I http://127.0.0.1:8081/
sudo ss -lntp | grep -E ':8081[[:space:]]'
```

必须看到 `127.0.0.1:8081`，而不是 `0.0.0.0:8081`。

`docker ps` 的主要列：

| 列/状态 | 含义 |
| --- | --- |
| `IMAGE` | 容器使用的镜像 |
| `STATUS: Up ...` | 主进程仍在运行 |
| `STATUS: Up ... (healthy)` | 主进程运行且健康检查通过 |
| `STATUS: Up ... (health: starting)` | 健康检查仍在启动宽限期，短时间可正常 |
| `STATUS: Up ... (unhealthy)` | 进程存在，但健康检查连续失败，应看 logs/inspect |
| `STATUS: Restarting` | 主进程反复退出，重启策略不断拉起它 |
| `STATUS: Exited (0)` | 进程正常结束；对长期服务仍意味着当前未提供服务 |
| `STATUS: Exited (非 0)` | 进程异常结束，先看容器日志 |
| `PORTS: 127.0.0.1:8081->80/tcp` | 端口只发布在宿主机回环地址，符合本例 |

### 步骤 2：在 Nginx 中增加测试路径

在 `/etc/nginx/sites-available/mysite` 的 HTTPS 站点块中增加：

```nginx
location /container-demo/ {
    proxy_pass http://127.0.0.1:8081/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

```bash
sudo nginx -t
sudo systemctl reload nginx
curl -I https://example.com/container-demo/
```

**【验证结果】** 本机 8081 与公网 `/container-demo/` 都应返回 200。若容器显示 `Up`，但本机 curl 失败，检查容器内应用是否真的监听 80；若本机成功而公网返回 502，检查 Nginx 配置和错误日志。

这与第 1 篇代理 systemd 服务的原理完全相同。Nginx 不关心 8081 后面是普通进程还是容器。

练习完成后可以移除该 `location` 并删除测试容器：

```bash
sudo docker stop demo-service
sudo docker rm demo-service
```

删除前确认容器名。测试容器无业务数据，因此可重建；真实服务必须先确认挂载与备份。

---

## 2.5 持久化：配置、静态文件和业务数据

### 【可选练习】挂载宿主机目录

适合需要直接编辑、备份或由发布系统替换的文件：

```bash
sudo docker run -d \
  --name static-preview \
  -p 127.0.0.1:8081:80 \
  -v /var/www/mysite/current:/usr/share/nginx/html:ro \
  nginx:alpine
```

`:ro` 表示容器只读，避免容器修改宿主机静态文件。

验证完这一示例后先清理，避免后面的 Compose 示例与它争用 8081：

```bash
sudo docker stop static-preview
sudo docker rm static-preview
```

### 【必学概念】Docker 卷

适合由应用维护的持久化数据：

```bash
sudo docker volume create app-data
sudo docker volume ls
sudo docker volume inspect app-data
```

卷不因 `docker compose down` 自动删除，但 `docker compose down -v` 会删除 Compose 声明的卷。数据库卷被删除往往就是数据事故。

持久化不等于备份。卷仍在同一台服务器和磁盘上，必须另做一致性备份和恢复演练。

---

## 2.6 【可选：多容器时】容器网络

同一个自定义网络中的容器可以用服务名通信，不要依赖会变化的容器 IP：

```bash
sudo docker network create app-net
sudo docker run -d --name internal-web --network app-net nginx:alpine
sudo docker run --rm --network app-net curlimages/curl http://internal-web/
```

第二条临时启动一个带 curl 的工具容器：`--rm` 让它执行结束后自动删除，`--network app-net` 把它接入同一网络，`http://internal-web/` 中的 `internal-web` 是第一个容器名。请求成功说明 Docker 内置 DNS 与容器网络都正常。

第二个容器使用 `internal-web` 作为主机名，Docker 内置 DNS 会解析它。

网络边界应这样理解：

- `expose` 或容器镜像中的 `EXPOSE` 只是说明/容器网络可见性，不等于公网发布；
- Compose 中不写 `ports`，服务仍可在同一 Compose 网络互访；
- 只有需要让宿主机 Nginx 访问的服务才发布到 `127.0.0.1:某端口`；
- 数据库、缓存通常只加入内部网络，不写 `ports`。

---

## 2.7 【推荐：多容器时】Docker Compose

创建项目目录：

```bash
sudo install -d -o deploy -g deploy -m 0750 /opt/compose-demo
cd /opt/compose-demo
nano compose.yaml
```

示例：一个由宿主机 Nginx 接入的 Web 容器，以及一个只在内部网络可见的 Redis。它们只是演示容器关系，不代表完整后台系统。

```yaml
services:
  web:
    image: nginx:alpine
    ports:
      - "127.0.0.1:8081:80"
    volumes:
      - /var/www/mysite/current:/usr/share/nginx/html:ro
    restart: unless-stopped
    mem_limit: 256m
    cpus: 0.5
    healthcheck:
      test: ["CMD-SHELL", "wget -q -O /dev/null http://127.0.0.1/ || exit 1"]
      interval: 30s
      timeout: 5s
      retries: 3

  cache:
    image: redis:7-alpine
    restart: unless-stopped
    volumes:
      - cache-data:/data

volumes:
  cache-data:
```

Compose 配置拆解：

| 字段 | 含义 |
| --- | --- |
| `services` | 定义这组应用包含哪些服务 |
| `web` / `cache` | 服务名，也会成为默认网络中的主机名 |
| `image` | 创建容器所使用的镜像 |
| `ports` | 发布端口；本例只把 web 发布到宿主机回环地址 |
| `volumes`（服务下） | 把宿主机目录或命名卷挂进容器 |
| `restart: unless-stopped` | 异常或服务器重启后恢复，但尊重人工停止 |
| `mem_limit` / `cpus` | 限制容器可使用的内存和 CPU |
| `healthcheck.test` | 容器内执行的健康检查命令 |
| `interval` / `timeout` / `retries` | 检查间隔、单次超时、连续失败阈值 |
| 顶层 `volumes` | 声明由 Compose 管理的命名卷 |

这个示例没有给 Redis 配置 `ports`，因此公网和宿主机不能直接通过 6379 访问它。真实 Redis 还要配置认证、持久化策略和资源限制。

> 精简镜像未必包含 `curl` 或 `wget`。实际项目应选择镜像已有的检查工具，或在构建镜像时加入专用健康检查程序。

启动前先让 Compose 渲染并校验配置：

```bash
sudo docker compose config
sudo docker compose up -d
sudo docker compose ps
sudo docker compose logs --tail 100 web
curl -I http://127.0.0.1:8081/
```

- `docker compose config`：解析 YAML、变量和默认值，输出最终配置，但不创建容器；
- `up -d`：按配置创建/更新并在后台运行服务；
- `ps`：只查看当前 Compose 项目的容器状态；
- `logs --tail 100 web`：只查看 `web` 服务最后 100 行日志。

**【验证结果】**

- `docker compose config` 应正常输出渲染后的配置；出现字段或缩进错误时不要继续启动。
- `docker compose ps` 中 `web`、`cache` 应为 `Up`/`running`；`web` 的健康状态应在启动后变为 `healthy`。
- `PORTS` 只应在 `web` 中看到 `127.0.0.1:8081->80/tcp`；`cache` 不应显示宿主机 6379 映射。
- `curl` 应返回 200。如果 `web` 长期为 `health: starting` 或变成 `unhealthy`，用 `docker inspect` 查看健康检查最近几次的输出。

常用命令：

```bash
sudo docker compose logs -f web
sudo docker compose restart web
sudo docker compose pull
sudo docker compose up -d
sudo docker compose down
```

`docker compose pull` 只下载镜像；再次 `up -d` 才会按新镜像重建需要更新的容器。更新前应记录旧镜像标识并准备回滚。

不要把生产密码直接写进 `compose.yaml`。可以通过权限受控的 `.env`、`env_file` 或部署平台的 secret 机制注入，并确保它不进入 Git。

---

## 2.8 【可选：接收自建镜像时】读懂 Dockerfile 交付边界

Dockerfile 是开发团队构建镜像的说明书。运维人员至少要能读懂：

| 指令 | 含义 |
| --- | --- |
| `FROM` | 基础镜像 |
| `WORKDIR` | 容器内工作目录 |
| `COPY` | 将构建上下文文件加入镜像 |
| `RUN` | 构建阶段执行命令 |
| `USER` | 容器内运行用户 |
| `EXPOSE` | 声明应用端口，不会自动发布公网 |
| `ENTRYPOINT` / `CMD` | 容器启动命令 |

部署前应向镜像提供方确认：

- 镜像来源、版本标签或 digest；
- 容器内部监听端口；
- 配置注入方式；
- 必须挂载的数据目录；
- 运行用户与文件权限；
- 健康检查命令；
- 优雅停止所需时间；
- 升级与数据库迁移是否可回滚。

镜像里不要内置生产密码、私钥或可写业务数据。优先使用非 root 用户运行，并尽可能减少镜像内不必要的软件。

---

## 2.9 日志、资源与磁盘

```bash
sudo docker compose logs --tail 100 服务名
sudo docker stats
sudo docker system df
sudo du -sh /var/lib/docker
```

容器持续向标准输出写日志时，Docker 默认日志可能占满磁盘。配置日志轮转前，先查看现有 `/etc/docker/daemon.json`，合并而不是覆盖：

```json
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
```

**【验证】** 修改前后都验证 JSON：

```bash
sudo jq empty /etc/docker/daemon.json
```

`jq` 是 JSON 解析工具；`empty` 不打印内容，只验证输入能否被正确解析。因此“没有输出且退出码为 0”表示 JSON 语法正确；出现行号和解析错误时不要重启 Docker。

**【谨慎】** 重启 Docker 可能影响所有容器，应选择维护窗口并在操作后逐项验证：

```bash
sudo systemctl restart docker
sudo docker ps
```

清理命令必须先看占用和目标。`docker system prune` 会删除未使用资源，`docker compose down -v` 会删除卷；不要把它们加入不理解的自动清理脚本。

---

## 2.10 排障顺序

```bash
sudo docker compose ps
sudo docker ps -a
sudo docker logs --tail 100 容器名
sudo docker inspect 容器名
sudo ss -lntp
curl -i http://127.0.0.1:8081/
sudo nginx -t
sudo tail -n 50 /var/log/nginx/error.log
```

| 现象 | 方向 |
| --- | --- |
| 镜像拉取超时 | 网络、仓库地址、代理/镜像服务、DNS |
| `pull access denied` | 镜像名错误、私有仓库未登录或无权限 |
| `port is already allocated` | 宿主机端口被其他进程/容器占用 |
| 容器反复 Restarting | 启动命令、配置、权限、依赖失败，先看 logs |
| 容器显示 Up 但服务不通 | 应用未监听预期端口、健康检查缺失或失败 |
| Nginx 502 | 回环端口不通、端口映射错误、容器未就绪 |
| 宿主机 Nginx 无法启动 | 容器错误占用了 80/443 |
| 数据重建后消失 | 数据放在容器可写层，未正确挂载卷/目录 |

---

## 2.11 安全与验收清单

- [ ] Docker 来自受信任的官方或企业仓库。
- [ ] 知道 `docker` 组接近 root 权限。
- [ ] 宿主机 Nginx 独占公网 80/443。
- [ ] 业务容器发布端口使用 `127.0.0.1:宿主机端口:容器端口`。
- [ ] 数据库和缓存没有 `ports`，安全组也未放行其端口。
- [ ] 配置和数据通过挂载持久化，密码未写进镜像或 Git。
- [ ] 配置了合理的重启策略、健康检查、日志轮转和资源限制。
- [ ] 更新镜像前记录旧版本，有数据备份和回滚步骤。
- [ ] 能从容器日志、本机 `curl`、Nginx 日志逐层排障。

## 参考资料

- [Docker Engine：Ubuntu 安装](https://docs.docker.com/engine/install/ubuntu/)
- [Docker Compose 插件安装](https://docs.docker.com/compose/install/linux/)
- [Docker 防火墙说明](https://docs.docker.com/engine/network/packet-filtering-firewalls/)
- [Docker 存储卷](https://docs.docker.com/engine/storage/volumes/)
