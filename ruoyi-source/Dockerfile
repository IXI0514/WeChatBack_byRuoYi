# =====================================
# 若依后端 Dockerfile (多阶段构建 - 方案C)
# Stage 1: Maven + JDK8 编译打包
# Stage 2: JRE8 运行
# 服务器只需 Docker,无需装 JDK/Maven/Node
# =====================================

# ===== Stage 1: Build =====
FROM maven:3.9-eclipse-temurin-8 AS builder

WORKDIR /build

# 配置 Maven 阿里云镜像(加速依赖下载)
RUN mkdir -p /root/.m2 && \
    echo '<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0">' \
    '<mirrors><mirror>' \
    '<id>aliyun</id><mirrorOf>central</mirrorOf>' \
    '<name>Aliyun</name>' \
    '<url>https://maven.aliyun.com/repository/public</url>' \
    '</mirror></mirrors>' \
    '<profiles><profile>' \
    '<id>jdk8</id><activation><activeByDefault>true</activeByDefault></activation>' \
    '<properties><maven.compiler.source>1.8</maven.compiler.source>' \
    '<maven.compiler.target>1.8</maven.compiler.target></properties>' \
    '</profile></profiles>' \
    '</settings>' > /root/.m2/settings.xml

# 先拷贝所有 pom.xml 加速依赖缓存(改代码不重新下依赖)
COPY pom.xml .
COPY ruoyi-admin/pom.xml ruoyi-admin/pom.xml
COPY ruoyi-common/pom.xml ruoyi-common/pom.xml
COPY ruoyi-framework/pom.xml ruoyi-framework/pom.xml
COPY ruoyi-system/pom.xml ruoyi-system/pom.xml
COPY ruoyi-quartz/pom.xml ruoyi-quartz/pom.xml
COPY ruoyi-generator/pom.xml ruoyi-generator/pom.xml

# 下载依赖(利用 Docker 层缓存)
RUN mvn dependency:resolve -DskipTests || true

# 拷贝源码
COPY ruoyi-admin/src ruoyi-admin/src
COPY ruoyi-common/src ruoyi-common/src
COPY ruoyi-framework/src ruoyi-framework/src
COPY ruoyi-system/src ruoyi-system/src
COPY ruoyi-quartz/src ruoyi-quartz/src
COPY ruoyi-generator/src ruoyi-generator/src

# 打包
RUN mvn clean package -DskipTests

# ===== Stage 2: Runtime =====
FROM eclipse-temurin:8-jre

LABEL maintainer="ruoyi-deploy"
LABEL description="RuoYi-Vue admin backend (multi-stage build)"

WORKDIR /home/ruoyi

# 只拷贝打包好的 jar(镜像体积小)
COPY --from=builder /build/ruoyi-admin/target/ruoyi-admin.jar /home/ruoyi/ruoyi-admin.jar

VOLUME ["/home/ruoyi/logs", "/home/ruoyi/uploadPath"]

ENV JAVA_OPTS="-Xms512m -Xmx512m -Duser.timezone=Asia/Shanghai -Dfile.encoding=UTF-8"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /home/ruoyi/ruoyi-admin.jar"]
