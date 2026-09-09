# 项目长期记忆 - mywebshow 工作区

## 项目概况
- 工作区路径: D:\Code2027\YQ_ArchiveSystem2026\mywebshow
- 用户主导宿州电厂（宿州钱营孜电厂二期扩建项目）资料处理工作
- 同时开发 Windows 桌面工具（PDF 处理、批量重命名等，Python tkinter）

## 部署方案约定
- **若依 RuoYi-Vue + Docker + 公网云主机** 是确认的部署路线
- 部署脚手架位置: `ruoyi-deploy/` 目录（D:\Code2027\YQ_ArchiveSystem2026\mywebshow\ruoyi-deploy\）
- 技术栈: MySQL 8 + Redis 7 + JDK 8 (eclipse-temurin) + Nginx 1.25 + Spring Boot 2.x + Vue 2
- 容器编排: docker-compose,五个容器(mysql/redis/ruoyi-admin/ruoyi-ui/nginx)
- 安全: 数据库/Redis/后端端口默认不对外暴露,仅 docker 网络内通信
- 一键脚本: `deploy.sh`,支持 init/up/down/restart/logs/status/backup/ssl/build/migrate

## 个性化定制需求
- 后端: 在若依基础上加了小程序接口服务(给微信小程序提供 REST API)
  - 小程序用户验证(wx.login → code 换 openid → JWT token)
  - 数据存储(小程序用户单独建表 miniapp_user,不与若依 admin 混用)
- 前端: 深度定制了 PC 端管理页（基于 RuoYi-Vue3: Vue 3.5 + Vite 6 + Element Plus）
  - 接口日志查看页
  - 会员状态管理页(修改会员状态)
- 源码在本地开发机,需手动拷贝 jar+dist+SQL 到云主机
- 暂不加新容器(MinIO/MQ 等),未来可能加
- nginx 反代: /prod-api/ 直连后端(PC 端),/api/ 预留给小程序(默认注释,按需启用)

## 用户偏好
- Windows 环境,用户名 Shen,Shell 用 bash (Git Bash)
- 偏好 Python 栈,但部署若依接受 Java 栈
- 喜欢"顶部配置、无需 CLI 参数"的脚本风格
- 重命名/工具脚本: 三位纯数字编号 001 起步,send2trash 进回收站,重命名前实时预览
