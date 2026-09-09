# 若依后端 Dockerfile
# 基于 eclipse-temurin JRE 8,只跑不打包
#
# 使用前请先在若依源码根目录执行:
#   mvn clean package -DskipTests
# 然后把 jar 拷到本目录:
#   cp <源码>/ruoyi-admin/target/ruoyi-admin.jar <本目录>/

FROM eclipse-temurin:8-jre

LABEL maintainer="ruoyi-deploy"
LABEL description="RuoYi-Vue admin backend"

WORKDIR /home/ruoyi

# 拷贝打包好的 jar
COPY ruoyi-admin.jar /home/ruoyi/ruoyi-admin.jar

# 日志和上传目录外挂(在 compose 里映射)
VOLUME ["/home/ruoyi/logs", "/home/ruoyi/uploadPath"]

# JVM 参数,可按机器配置调整
ENV JAVA_OPTS="-Xms512m -Xmx512m -Duser.timezone=Asia/Shanghai -Dfile.encoding=UTF-8"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /home/ruoyi/ruoyi-admin.jar"]
