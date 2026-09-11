# 15 · 公开静态站点

此目录会挂载到顶层 Nginx 的 `/usr/share/nginx/html`，访问 `https://73ham.top/` 时展示其中的静态文件。

- 初始首页：`index.html`。
- 以后使用博客生成器时，将生成结果复制到本目录即可。
- 管理后台不在本目录，固定从 `https://73ham.top/admin/` 访问。
- 小程序接口不在本目录，固定使用 `https://73ham.top/api/`。

修改静态文件后只需执行：

```bash
cd ~/mywebshow/ruoyi-deploy
docker compose restart nginx
```
