# 1. 使用支持 JDK 11 和 Tomcat 10.1 的官方基础镜像
FROM tomcat:10.1-jdk11-openjdk

# 2. 删除 Tomcat 自带的默认应用 (可选，为了干净)
RUN rm -rf /usr/local/tomcat/webapps/*

# 3. 将本地编译好的 WAR 包复制到 Tomcat 的 webapps 目录
# 注意：这里假设你的 WAR 包名是 oj_tracker.war
COPY target/oj_tracker.war /usr/local/tomcat/webapps/oj_tracker.war

# 4. 暴露 8080 端口
EXPOSE 8080

# 5. 启动 Tomcat
CMD ["catalina.sh", "run"]