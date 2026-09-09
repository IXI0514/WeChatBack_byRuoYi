# =====================================
# 若依前端 Dockerfile (多阶段构建 - 方案C)
# Stage 1: Node 构建 dist
# Stage 2: Nginx 运行
# 服务器只需 Docker,无需装 Node/npm
# =====================================

# ===== Stage 1: Build =====
FROM node:18-alpine AS builder

WORKDIR /build

# 配置淘宝镜像加速
RUN npm config set registry https://registry.npmmirror.com

# 先拷 package.json 加速依赖缓存
COPY package.json package*.json ./
RUN npm install --legacy-peer-deps

# 拷贝源码
COPY . .

# 构建生产包
RUN npm run build:prod || npm run build

# ===== Stage 2: Runtime =====
FROM nginx:1.25-alpine

LABEL maintainer="ruoyi-deploy"
LABEL description="RuoYi-Vue frontend (multi-stage build)"

# 拷贝构建产物
COPY --from=builder /build/dist /usr/share/nginx/html

# 前端容器内 nginx 配置(服务静态资源 + 反代后端 API)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
