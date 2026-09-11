# 42 · 第 1 篇：Nginx 实战——静态网站、域名、HTTPS 与同域名 API

> 前置：完成[第 0 篇](./00-服务器基础与常用命令.md)，能以普通用户登录并使用 `sudo`。  
> 本篇终点：`https://example.com/` 提供静态网站，`https://example.com/api/health` 转发给独立后台服务。  
> 范围：学习部署链路，不开发真实后台接口。

建议从头顺序执行。每个阶段都先做本机验证，再做公网验证。

---

## 1.1 先理解最终架构

Nginx 在这里承担三个职责：

1. 读取 HTML、CSS、JavaScript 和图片等静态文件；
2. 接收公网 HTTPS，在一处管理证书；
3. 根据 URL 路径分流，把 `/api/` 请求反向代理给后台服务。

```text
                   ┌─ /、/assets/... ─▶ /var/www/mysite/current/
浏览器 ─▶ Nginx ──┤
                   └─ /api/... ──────▶ 127.0.0.1:8080
```

几个容易混淆的词：

| 名称 | 解决的问题 |
| --- | --- |
| DNS | 把 `example.com` 解析为服务器公网 IP |
| Nginx `server_name` | 同一台服务器上，按请求域名选择站点 |
| Nginx `location` | 同一域名内，按 URL 路径选择静态文件或后台服务 |
| TLS 证书 | 证明域名身份并加密 HTTP 通信，形成 HTTPS |
| 反向代理 | 用户只连接 Nginx，Nginx 再连接内部后台服务 |

DNS 不会把 `/api/` 分配给后台；路径分流是 Nginx 的工作。

---

## 1.2 【必做】部署前检查

在服务器执行：

```bash
cat /etc/os-release
ip -brief address
sudo ss -lntp
sudo ufw status verbose
```

在腾讯云控制台确认：

- 实例有固定公网 IP；
- 入站允许 TCP 22、80、443；
- 8080 没有对公网开放；
- 如果实例位于中国内地，计划使用的域名已按规则完成备案或接入备案。

> 80 端口不只是提供 HTTP，也常用于证书机构验证域名。配置 HTTPS 后可以把普通 HTTP 请求重定向到 443，但通常仍保留 80 入站。

---

## 1.3 【必做】安装并认识 Nginx

```bash
sudo apt update
sudo apt install -y nginx
sudo systemctl enable --now nginx
nginx -v
systemctl status nginx --no-pager
```

**【验证结果】** `systemctl status` 中应看到 `Active: active (running)`。如果是 `failed`，先看同一输出底部的错误，再执行 `journalctl -u nginx -n 100 --no-pager`。`enabled` 只表示开机启动，不能代替 `active (running)`。

如果已经启用 UFW：

```bash
sudo ufw allow 'Nginx Full'
sudo ufw status verbose
```

先在服务器本机验证：

```bash
curl -I http://127.0.0.1
sudo ss -lntp | grep -E ':80[[:space:]]'
```

再在自己的电脑浏览器打开：

```text
http://服务器公网IP
```

看到 Nginx 欢迎页，说明“安全组 → UFW → Nginx”的 HTTP 链路已经打通。

**【验证结果】**

| 检查 | 正常时关注的信息 |
| --- | --- |
| `curl -I http://127.0.0.1` | 第一行是 `HTTP/1.1 200 OK`，响应头通常包含 `Server: nginx` |
| `ss -lntp` | 存在 `*:80`、`0.0.0.0:80` 或 `[::]:80`，进程信息包含 nginx |
| 公网 IP 浏览器 | 显示 Nginx 欢迎页；本机成功但公网失败时重点查安全组/UFW |

### Nginx 的配置布局

Ubuntu 软件包采用以下结构：

```text
/etc/nginx/
├── nginx.conf
├── sites-available/   # 保存站点配置
├── sites-enabled/     # 只有这里启用的站点配置会生效
└── conf.d/
```

本课程保持一个规则：

```text
修改配置 → sudo nginx -t → 成功后 sudo systemctl reload nginx
```

`reload` 平滑载入新配置；`restart` 会停止后再启动。普通配置变更优先使用 `reload`。

---

## 1.4 【必做】第一站：先通过 IP 发布静态网页

### 步骤 1：创建站点目录

```bash
sudo install -d -o deploy -g www-data -m 0755 /var/www/mysite/current
nano /var/www/mysite/current/index.html
```

第一条不是“安装软件”，而是用 `install` 命令一次完成创建目录和设置属性：

| 片段 | 含义 |
| --- | --- |
| `sudo` | 创建 `/var/www` 下的目录需要管理员权限 |
| `install -d` | 创建目录；缺少的父目录也会一并创建 |
| `-o deploy` | owner，把目录所有者设为发布用户 `deploy` |
| `-g www-data` | group，把目录所属组设为 Nginx 使用的 `www-data` |
| `-m 0755` | mode，创建时直接设置权限 |
| `/var/www/mysite/current` | 要创建的目标目录 |

`0755` 中，第一位 `7` 表示所有者可读、写、进入目录，后两个 `5` 表示同组用户和其他用户可读、进入但不能写。数字来自 `读=4 + 写=2 + 执行/进入=1`。因此 `7=4+2+1`，`5=4+1`。

如果你的用户名不是 `deploy`，请替换命令中的用户名。写入：

```html
<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>我的第一个网站</title>
</head>
<body>
  <h1>Hello, Nginx!</h1>
  <p>静态网站已经由 Nginx 提供。</p>
</body>
</html>
```

验证 Nginx 用户有读取权限：

```bash
sudo -u www-data test -r /var/www/mysite/current/index.html && echo readable
```

这条命令模拟 Nginx 身份进行只读检查：`sudo -u www-data` 指定以 `www-data` 用户执行；`test -r` 检查该用户是否可读；只有检查成功，`&& echo readable` 才会输出文字。它不会修改文件。

输出 `readable` 才表示验证通过。如果没有任何输出，说明测试失败，可执行 `namei -l /var/www/mysite/current/index.html` 检查每一级目录权限。

### 步骤 2：创建站点配置

```bash
sudo nano /etc/nginx/sites-available/mysite
```

写入：

```nginx
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    server_name _;

    root /var/www/mysite/current;
    index index.html;

    location / {
        try_files $uri $uri/ =404;
    }
}
```

这几个指令的含义：

- `listen 80`：接收 HTTP；
- `listen [::]:80`：在 IPv6 地址上接收 HTTP；服务器没有公网 IPv6 时保留它通常也不影响 IPv4；
- `default_server` 与 `server_name _`：暂时接住直接访问 IP 的请求；
- `root`：静态文件根目录；
- `try_files $uri $uri/ =404`：先按 `$uri` 查文件，再按 `$uri/` 查目录，都不存在就直接返回 404；`$uri` 是 Nginx 当前处理的规范化请求路径。

### 步骤 3：启用新站点

先查看默认站点确实是软链接：

```bash
ls -l /etc/nginx/sites-enabled/
```

停用默认站点只需要移除这个软链接，不会删除 `/etc/nginx/sites-available/default` 原文件：

```bash
sudo unlink /etc/nginx/sites-enabled/default
sudo ln -s /etc/nginx/sites-available/mysite /etc/nginx/sites-enabled/mysite
sudo nginx -t
sudo systemctl reload nginx
```

- `unlink` 只删除 `sites-enabled/default` 这个链接，不删除它指向的原配置；
- `ln -s 源 目标` 创建软链接，使 `sites-enabled/mysite` 指向真正的配置文件；
- `nginx -t` 只测试配置，不应用配置；
- `reload` 在测试通过后平滑加载新配置。

**【验证结果】** `nginx -t` 必须同时出现 `syntax is ok` 和 `test is successful`。若失败，不要执行 reload；根据输出给出的文件名和行号修正配置。

如果 `unlink` 提示文件不存在，说明默认站点已经停用，可以继续。

### 步骤 4：验证

```bash
curl -i http://127.0.0.1/
curl -I http://服务器公网IP/
```

浏览器访问 `http://服务器公网IP/`，应显示 `Hello, Nginx!`。

如果 `curl -i` 第一行是 `200 OK` 且正文包含 `Hello, Nginx!`，静态站点已正常；如果仍显示默认欢迎页，多半是默认站点未停用或新配置未启用。

---

## 1.5 【必做：发布真实网站时】上传静态网站文件

前端项目通常在开发电脑构建，服务器只接收构建产物，例如 `dist/`。先把文件上传到 `/home/deploy/site-upload/`，再在服务器执行：

```bash
test -f /home/deploy/site-upload/index.html
rsync -av --delete --dry-run /home/deploy/site-upload/ /var/www/mysite/current/
rsync -av --delete /home/deploy/site-upload/ /var/www/mysite/current/
```

`test -f` 用来确认首页确实存在；`rsync -av` 同步目录内容；第一条 rsync 的 `--dry-run` 只显示计划变更，第二条才真实执行；`--delete` 会删除目标端多余文件。源路径末尾的 `/` 表示复制目录里的内容，而不是再嵌套一层 `site-upload`。

第一条必须找到 `index.html`；`--dry-run` 的预览符合预期后才执行真实同步。

检查权限并刷新页面：

```bash
sudo find /var/www/mysite/current -type d -exec chmod 755 {} \;
sudo find /var/www/mysite/current -type f -exec chmod 644 {} \;
curl -I http://127.0.0.1/
```

### 【可选：仅 SPA】单页应用刷新 404

如果 Vue、React 等前端使用 history 路由，访问 `/users/1` 时服务器上通常没有同名文件。把静态站的 `location /` 改为：

```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

普通多页网站保留 `=404` 更清晰，不需要套用 SPA 配置。

---

## 1.6 【必做】给网站绑定域名

### 步骤 1：添加 DNS 记录

在域名 DNS 控制台添加 A 记录：

| 主机记录 | 类型 | 值 | 访问地址 |
| --- | --- | --- | --- |
| `@` | A | 服务器公网 IPv4 | `example.com` |
| `www` | A | 服务器公网 IPv4 | `www.example.com`；不需要 `www` 时可不添加 |

只有使用 IPv6 并已配置 IPv6 入站和 Nginx 监听时才添加 AAAA 记录。错误的 AAAA 记录可能导致部分用户优先走 IPv6 后访问失败。

### 步骤 2：验证 DNS

在自己的电脑执行：

```bash
nslookup example.com
nslookup www.example.com
```

安装了 `dig` 时可以查询指定公共 DNS：

```bash
dig +short A example.com @1.1.1.1
dig +short A www.example.com @1.1.1.1
```

`dig` 是 DNS 查询工具：`+short` 只显示简短答案，`A` 指定查询 IPv4 记录，末尾 `@1.1.1.1` 指定使用该公共 DNS 服务器而不是系统默认 DNS。它只用于交叉验证，不要求长期固定使用这个地址。

结果应包含服务器公网 IP。DNS 有 TTL 缓存，修改后不一定立即在所有网络生效。`ping` 被禁用也很常见，因此不把它作为唯一判断依据。

**【验证结果】** 只要 A 记录查询结果与腾讯云实例公网 IPv4 完全一致，就说明 DNS 这一层正确。`Non-authoritative answer` 只是表示结果来自递归 DNS 缓存，不是报错。返回旧 IP 时等待 TTL 或清理本机 DNS 缓存；返回 `NXDOMAIN` 表示该域名记录不存在。

### 步骤 3：修改 Nginx 的域名

编辑站点：

```bash
sudo nano /etc/nginx/sites-available/mysite
```

把开头改为：

```nginx
server {
    listen 80;
    listen [::]:80;
    server_name example.com www.example.com;

    root /var/www/mysite/current;
    index index.html;

    location / {
        try_files $uri $uri/ =404;
    }
}
```

应用并验证：

```bash
sudo nginx -t
sudo systemctl reload nginx
curl -I http://example.com/
```

此时应能通过 `http://example.com/` 访问。DNS 只把域名带到服务器，Nginx 再根据 HTTP `Host` 与 `server_name` 选择这个站点。

**【验证结果】** `curl -I` 应返回 `200`。如果返回的是其他站点内容，执行 `sudo nginx -T | grep -n server_name` 检查是否有重复或抢占的站点配置。

---

## 1.7 【必做】添加一个独立的模拟后台服务

这一节不用开发后台。我们用 Python 自带 HTTP 服务器模拟独立进程，让它只监听 `127.0.0.1:8080`，并交给 systemd 管理。

### 步骤 1：准备模拟响应

```bash
sudo install -d -o www-data -g www-data -m 0755 /opt/mock-api
echo '{"status":"ok","service":"mock-api"}' | sudo tee /opt/mock-api/health
sudo chmod 644 /opt/mock-api/health
```

普通的 `>` 重定向由当前 shell 执行，即使前面写 `sudo echo` 也未必有权写 `/opt`。这里用管道 `|` 把文本交给具有管理员权限的 `tee`：`tee 文件` 会把输入同时写入文件并显示在终端。`chmod 644` 让所有者可读写、其他用户只读。

### 步骤 2：创建 systemd 服务

```bash
sudo nano /etc/systemd/system/mock-api.service
```

写入：

```ini
[Unit]
Description=Tutorial mock API service
After=network.target

[Service]
Type=simple
User=www-data
Group=www-data
WorkingDirectory=/opt/mock-api
ExecStart=/usr/bin/python3 -m http.server 8080 --bind 127.0.0.1 --directory /opt/mock-api
Restart=on-failure
RestartSec=3
NoNewPrivileges=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
```

这个 `.service` 文件不是 shell 脚本，而是 systemd 配置：

| 配置 | 含义 |
| --- | --- |
| `[Unit]` | 服务的描述和启动顺序 |
| `After=network.target` | 在基础网络单元之后启动 |
| `[Service]` | 如何运行和管理进程 |
| `User` / `Group` | 以 `www-data` 低权限身份运行，而不是 root |
| `WorkingDirectory` | 进程启动时所在目录 |
| `ExecStart` | systemd 实际执行的完整启动命令 |
| `--bind 127.0.0.1` | Python 只监听本机回环地址 |
| `--directory /opt/mock-api` | 指定对外读取文件的目录，不依赖当前终端目录 |
| `Restart=on-failure` | 进程异常退出时重启，正常停止时不循环启动 |
| `RestartSec=3` | 失败后等待 3 秒再尝试重启 |
| `NoNewPrivileges` / `PrivateTmp` | 限制额外提权，并给服务隔离的临时目录 |
| `[Install]` / `WantedBy` | 定义 `systemctl enable` 时挂入哪个开机目标 |

第 3 篇会进一步解释真实后台服务的目录、环境变量、停止超时和安全限制。

启动并设置开机自启：

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now mock-api
systemctl status mock-api --no-pager
curl -i http://127.0.0.1:8080/health
sudo ss -lntp | grep -E ':8080[[:space:]]'
```

- `daemon-reload`：让 systemd 重新读取新增或修改的 `.service` 文件；它不会自动重启服务；
- `enable --now`：`enable` 设置开机启动，`--now` 同时立即启动；
- `grep -E`：使用扩展正则过滤端口行；`[[:space:]]` 表示端口号后必须跟空白，避免把 8080 错匹配成 80801。

验收重点：

- `curl` 正文包含 `"status":"ok"`；
- 监听地址是 `127.0.0.1:8080`，不是 `0.0.0.0:8080`；
- 云安全组不开放 8080；如果启用了 UFW，UFW 也不开放 8080。

`systemctl status mock-api` 应显示 `Active: active (running)` 和一个 `Main PID`。`curl -i` 第一行应为 `HTTP/1.0 200 OK`（Python 模拟服务的协议显示为 1.0 属正常），正文应包含 `"status":"ok"`。如果服务是 `failed`，执行 `journalctl -u mock-api -n 100 --no-pager` 查看启动错误。

这个服务只用于验证部署架构，不能作为真实 API 服务器。

### 步骤 3：让 `/api/` 走反向代理

在 `/etc/nginx/sites-available/mysite` 的同一个 `server` 块内，保留静态站 `location /`，再加入：

```nginx
location = /api {
    return 308 /api/;
}

location /api/ {
    proxy_pass http://127.0.0.1:8080/;
    proxy_http_version 1.1;

    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;

    proxy_connect_timeout 5s;
    proxy_read_timeout 60s;
}
```

配置拆解：

| 指令 | 含义 |
| --- | --- |
| `location = /api` | `=` 表示只精确匹配 `/api`，再用 308 补成带斜杠的 `/api/` |
| `location /api/` | 匹配所有以 `/api/` 开头的请求；它比通用的 `location /` 更具体 |
| `proxy_pass` | 指定后台地址；本例末尾 `/` 会去掉外层 `/api/` 前缀 |
| `proxy_http_version 1.1` | Nginx 与后台使用 HTTP/1.1 |
| `Host $host` | 把用户访问的域名告诉后台 |
| `X-Real-IP $remote_addr` | 把直接连接 Nginx 的客户端地址告诉后台 |
| `X-Forwarded-For $proxy_add_x_forwarded_for` | 在已有代理链后追加当前客户端地址 |
| `X-Forwarded-Proto $scheme` | 告诉后台用户最初使用的是 HTTP 还是 HTTPS |
| `proxy_connect_timeout 5s` | 最多等待 5 秒与后台建立连接 |
| `proxy_read_timeout 60s` | 后台两次数据读取之间最多等待 60 秒，不是整个请求的绝对总时长 |

注意 `proxy_pass` 末尾的 `/`：

```text
location /api/ + proxy_pass http://127.0.0.1:8080/
外部 /api/health  ─▶ 后台 /health
```

如果将来真实后台本身就要求接收 `/api/health`，应改成没有末尾斜杠的 `proxy_pass http://127.0.0.1:8080;`。这是 Nginx 最常见的路径错误之一。

应用并分层验证：

```bash
sudo nginx -t
sudo systemctl reload nginx

curl -i http://127.0.0.1:8080/health
curl -i -H 'Host: example.com' http://127.0.0.1/api/health
curl -i http://example.com/api/health
```

第二条中的 `-H 'Host: example.com'` 手工设置 HTTP Host 请求头：网络连接仍走 `127.0.0.1`，但让 Nginx 像收到域名请求一样选择 `server_name example.com` 的站点。它适合在不依赖公网 DNS 的情况下验证本机 Nginx 分流。

三次请求分别验证后台进程、Nginx 本机分流和公网域名链路。

**【验证结果】** 三次请求都应返回 200 和同一段 `mock-api` 内容。第一条失败查后台服务；第一条成功但第二条 502 查 `proxy_pass`；前两条成功但第三条失败查 DNS、安全组、UFW 或域名接入状态。

---

## 1.8 【必做】配置 HTTPS

申请证书前必须满足：

- `example.com` 确实解析到这台服务器；
- 如果同时申请 `www.example.com`，它也必须正确解析；
- 80 和 443 已在安全组、UFW 放行；
- `sudo nginx -t` 成功，且 HTTP 域名可以访问。

### 安装 Certbot 并申请证书

Certbot 官方目前推荐通过 Snap 安装，以便获得受支持的新版本和预配置的自动续期。先确认系统没有另一套 Certbot，避免 apt、pip 和 Snap 混用：

```bash
command -v certbot || true
sudo apt update
sudo apt install -y snapd
sudo snap install core
sudo snap refresh core
sudo snap install --classic certbot
sudo ln -s /snap/bin/certbot /usr/local/bin/certbot
certbot --version
```

- `command -v certbot`：查询 shell 最终会执行哪个 Certbot 路径；未安装时它会返回失败；
- `|| true`：把“尚未安装”视为本次检查可接受，避免复制到脚本中时提前中止；
- `snap install --classic`：通过 Snap 安装，并允许 Certbot 按经典权限访问 Nginx 与证书目录；
- `ln -s`：给 `/snap/bin/certbot` 创建一个位于常用命令路径中的软链接。

如果第一条已经找到 Certbot，先查明它的安装来源，再按官方迁移说明处理；不要直接叠加安装。若创建软链接时提示已存在，也应先检查该路径指向什么，而不是强制覆盖。

**【二选一】** 根域名和 `www` 都已解析、并且都需要访问时执行：

```bash
sudo certbot --nginx -d example.com -d www.example.com --redirect
```

`--nginx` 表示使用 Nginx 插件完成验证并修改站点配置；每个 `-d` 添加一个要写入证书的域名；`--redirect` 要求把对应 HTTP 请求跳转到 HTTPS。

**【二选一】** 如果只配置了根域名，就只申请它：

```bash
sudo certbot --nginx -d example.com --redirect
```

Certbot 会验证域名控制权、保存证书，并修改当前 Nginx 站点。不要为尚未解析的域名添加 `-d`，否则整次申请可能失败。

### 验证 HTTPS 与跳转

```bash
sudo nginx -t
curl -I http://example.com/
curl -I https://example.com/
curl -i https://example.com/api/health
sudo certbot certificates
```

预期结果：

- HTTP 返回 301 或 308，并跳转到 HTTPS；
- HTTPS 首页返回 200；
- HTTPS `/api/health` 返回模拟后台内容；
- 浏览器证书域名与当前域名一致。

可以用一条命令只查看状态码和最终跳转地址：

```bash
curl -sS -o /dev/null -w 'status=%{http_code} redirect=%{redirect_url}\n' http://example.com/
```

正常时常见结果为 `status=301` 或 `status=308`，`redirect` 以 `https://example.com/` 开头。

### 验证自动续期

Certbot 安装方式通常会预配置自动续期。先检查现状，不要再重复创建 cron：

```bash
systemctl list-timers --all | grep -E 'certbot|snap'
sudo certbot renew --dry-run
```

`renew --dry-run` 最终明确表示所有模拟续期成功才算通过。若失败，保存完整错误信息，优先检查 DNS、80 端口、证书域名与 Nginx；不要反复执行正式申请命令，以免触发签发频率限制。

证书文件位于 `/etc/letsencrypt/`，不要手工复制后长期使用；让 Nginx 引用 Certbot 管理的路径，续期后才能继续生效。

> 自签名证书不被普通浏览器信任，不能代表公网 HTTPS 已正确上线。本课程的第一阶段验收使用受信任证书。

---

## 1.9 【验证】最终站点配置应该表达什么

Certbot 可能调整 `listen 443 ssl`、证书路径和 HTTP 跳转块，生成结果会因版本而略有不同。不要为了和教程逐字一致而覆盖 Certbot 的证书配置。

使用下面的命令查看 Nginx 实际加载的完整配置：

```bash
sudo nginx -T
```

小写 `-t` 只检查配置；大写 `-T` 会先检查，再把 Nginx 实际加载的完整配置输出到终端，包括 `include` 进来的文件。配置较长时可以使用 `sudo nginx -T | less` 分页查看，按 `q` 退出。

无论具体排版如何，HTTPS 的站点块中应同时包含以下逻辑：

```nginx
server {
    server_name example.com www.example.com;
    root /var/www/mysite/current;
    index index.html;

    location / {
        try_files $uri $uri/ =404;
    }

    location = /api {
        return 308 /api/;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Certbot 管理的 listen 443、ssl_certificate 等配置会在这里或包含文件中。
}
```

Nginx 会选择更具体的 `/api/`，其他路径才落到 `/`，因此静态站和后台接口不会互相覆盖。

---

## 1.10 【可选・推荐】更新静态网站与快速回滚

直接覆盖 `current/` 简单，但上传到一半时用户可能读到不完整文件。熟悉基础流程后，可以使用版本目录与软链接：

```text
/var/www/mysite/
├── releases/
│   ├── 20260907-120000/
│   └── 20260908-093000/
└── current -> releases/20260908-093000
```

发布思路：上传完整的新目录，检查 `index.html`，最后一次性切换 `current` 软链接。回滚就是把链接指回上一个版本。静态文件更新通常不需要重载 Nginx；只有 Nginx 配置改变才需要 `nginx -t` 和 `reload`。

首次练习可继续使用 1.5 节的 rsync，掌握后再按[第 3 篇](./03-服务器应用运行与维护.md)学习发布、回滚和备份。

---

## 1.11 排障：从后台向浏览器逐层检查

### 固定检查清单

```bash
systemctl status mock-api --no-pager
journalctl -u mock-api -n 50 --no-pager
curl -i http://127.0.0.1:8080/health

sudo nginx -t
systemctl status nginx --no-pager
sudo tail -n 50 /var/log/nginx/error.log
curl -i -H 'Host: example.com' http://127.0.0.1/api/health

sudo ss -lntp
sudo ufw status verbose
nslookup example.com
curl -Iv https://example.com/
```

### 常见现象

| 现象 | 最可能的原因 | 优先检查 |
| --- | --- | --- |
| IP 也打不开 | Nginx 未运行、80 未监听、安全组/UFW 拦截 | `systemctl`、`ss`、两层防火墙 |
| 域名打不开但 IP 可以 | DNS 错误、备案/接入问题、`server_name` 不匹配 | `nslookup`、云平台、Nginx 配置 |
| 403 | Nginx 无权读取目录或文件 | `namei -l`、`sudo -u www-data test -r ...` |
| 静态文件 404 | `root` 或文件路径错误 | `ls -la`、access/error log |
| SPA 首页正常、刷新子路由 404 | 未配置 SPA fallback | `try_files ... /index.html` |
| `/api/` 返回 502 | 后台未运行或端口不对 | 后台 `status`、日志、本机 8080 `curl` |
| `/api/` 返回后台 404 | 转发后的路径不符合后台预期 | 检查 `proxy_pass` 末尾斜杠 |
| HTTPS 申请失败 | DNS 未生效、80 不通、域名未备案/被拦截、申请过于频繁 | DNS、公网 HTTP、Certbot 输出 |
| 修改配置后无变化 | 修改的文件未启用或忘记 reload | `ls -l sites-enabled`、`nginx -T` |

不要用 `chmod -R 777`、关闭防火墙、开放 8080 来“验证是否能好”。这些做法会掩盖真正原因并扩大攻击面。

---

## 1.12 第一阶段验收清单

- [ ] `https://example.com/` 显示自己的静态网站。
- [ ] `http://example.com/` 自动跳转 HTTPS。
- [ ] `https://example.com/api/health` 返回 `mock-api` 内容。
- [ ] `systemctl status nginx` 与 `systemctl status mock-api` 都是 `active (running)`。
- [ ] `ss -lntp` 显示 Nginx 监听 80/443，模拟后台只监听 `127.0.0.1:8080`。
- [ ] 安全组与 UFW 没有开放 8080、3306、6379。
- [ ] `sudo certbot renew --dry-run` 成功。
- [ ] 能说明 DNS、Nginx、静态文件、后台服务、证书分别负责哪一段。

完成后建议学习：[03-服务器应用运行与维护.md](./03-服务器应用运行与维护.md)。需要容器化时再学习：[02-Docker入门与实践.md](./02-Docker入门与实践.md)。

## 参考资料

- [Nginx：请求如何选择 server 与 location](https://nginx.org/en/docs/http/request_processing.html)
- [Nginx：反向代理模块与 proxy_pass 路径规则](https://nginx.org/en/docs/http/ngx_http_proxy_module.html)
- [Certbot：Nginx on Linux (Snap) 安装说明](https://certbot.eff.org/instructions?ws=nginx&os=snap)
- [腾讯云 ICP 备案](https://cloud.tencent.com/document/product/243)
