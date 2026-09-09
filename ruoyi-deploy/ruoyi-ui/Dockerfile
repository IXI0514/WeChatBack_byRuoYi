# 若依前端 Dockerfile
# 基于 nginx:alpine,服务打包好的 dist 静态资源
#
# 使用前请先在前端源码目录执行:
#   npm install --legacy-peer-deps
#   npm run build:prod
# 然后把 dist 拷到本目录:
#   cp -r <源码>/ruoyi-ui/dist <本目录>/dist
#
# 如果需要镜像内自动构建,可参考多阶段写法:
#   FROM node:16-alpine AS builder
#   WORKDIR /build
#   COPY package*.json ./
#   RUN npm config set registry https://registry.npmmirror.com && npm install --legacy-peer-deps
#   COPY . .
#   RUN npm run build:prod
#   FROM nginx:1.25-alpine
#   COPY --from=builder /build/dist /usr/share/nginx/html
#   COPY nginx.conf /etc/nginx/conf.d/default.conf

FROM nginx:1.25-alpine

LABEL maintainer="ruoyi-deploy"
LABEL description="RuoYi-Vue frontend"

# 拷贝前端构建产物
COPY dist /usr/share/nginx/html

# 前端容器内的 nginx 配置(处理静态资源 + 反代后端 API)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
