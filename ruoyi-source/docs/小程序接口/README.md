# 小程序接口

本目录专门存放小程序客户端对接文档，与后台管理接口、部署文档分开维护。

## 正式环境地址

| 用途 | 地址 |
| --- | --- |
| 小程序 API 基础地址 | `https://73ham.top/api` |
| 公开网站 | `https://73ham.top/` |
| 管理后台 | `https://73ham.top/admin/` |

小程序只调用 `https://73ham.top/api/...`，不调用 `/prod-api/` 或后台管理接口 `/system/miniapp/...`。公开 Controller 直接映射 `/api/` 路径。

## 微信公众平台配置

在“开发管理 → 开发设置 → 服务器域名”填写：

| 配置项 | 值 |
| --- | --- |
| request 合法域名 | `https://73ham.top` |
| uploadFile 合法域名 | `https://73ham.top` |
| downloadFile 合法域名 | `https://73ham.top` |
| socket / udp / tcp | 未使用时留空 |

不得填写路径、端口或末尾分号，例如不要填写 `https://73ham.top/api/login/verify`。

## 上线前置条件

1. `73ham.top` 已解析到服务器且 HTTPS 证书有效。
2. Nginx 配置更新并通过 `sudo docker compose exec nginx nginx -t`。
3. 小程序项目中的 API 基础地址改为 `https://73ham.top/api`。

## 接口文档

- [加密凭证规范](./加密凭证.md)：中继台查询必需的 AES-GCM 请求头、密钥配置与客户端生成格式。

- [小程序 API 接口说明](./接口说明.md)：统一对应 `MiniappApiController`，包含登录验证、公开中继台查询。

## 通用响应

接口响应为 JSON，成功时通常包含 `code: 200`。小程序应对网络失败、非 200 业务 code、超时和服务端错误进行提示与重试控制。
