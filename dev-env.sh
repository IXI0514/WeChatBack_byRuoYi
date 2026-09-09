#!/usr/bin/env bash
# 若依开发环境变量配置脚本
# 用法: 在 Git Bash 里执行 source dev-env.sh
# 每次开发前都要 source 一次(新开窗口后环境变量会重置)

# ===== JDK 8 =====
export JAVA_HOME="/c/DevelopApps/jdk8u504-b01"

# ===== Maven 3.9.15 =====
export M2_HOME="/c/DevelopApps/apache-maven-3.9.15"
export MAVEN_HOME="$M2_HOME"

# ===== PATH =====
export PATH="$JAVA_HOME/bin:$PATH"

# ===== Maven wrapper 函数 =====
# Git Bash 调用 Windows 程序时路径转换有问题,导致 mvn 脚本构建的 CLASSPATH 错误
# 此函数直接用 java.exe + Windows 原生路径启动 Maven,绕过该问题
mvn() {
    local maven_home_win="C:\\DevelopApps\\apache-maven-3.9.15"
    local classworlds_win="C:\\DevelopApps\\apache-maven-3.9.15\\boot\\plexus-classworlds-2.9.0.jar"
    local m2_conf_win="C:\\DevelopApps\\apache-maven-3.9.15\\bin\\m2.conf"
    local proj_dir
    proj_dir=$(cygpath -w "$(pwd)" 2>/dev/null || echo "C:\\")
    MSYS_NO_PATHCONV=1 "$JAVA_HOME/bin/java.exe" \
        -classpath "$classworlds_win" \
        -Dclassworlds.conf="$m2_conf_win" \
        -Dmaven.home="$maven_home_win" \
        -Dmaven.multiModuleProjectDirectory="$proj_dir" \
        org.codehaus.plexus.classworlds.launcher.Launcher "$@"
}

# ===== 验证 =====
echo "[dev-env] 环境已配置:"
echo "  JAVA_HOME=$JAVA_HOME"
echo "  Maven home=$M2_HOME"
echo "  Java: $(java -version 2>&1 | head -1)"
echo "  Maven: $(mvn -version 2>&1 | head -1)"
echo ""
echo "  用法: source dev-env.sh  (每次开发前)"
echo "  编译后端: cd ruoyi-source && mvn clean compile -DskipTests"
echo "  打包后端: cd ruoyi-source && mvn clean package -DskipTests"
echo "  启动前端: cd ruoyi-source/ruoyi-ui && npm run dev"
