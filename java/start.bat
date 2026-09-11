@echo off
chcp 65001 >nul
echo ========================================
echo  Java 后端启动 (Spring Boot :8080)
echo  环境: JDK 17
echo ========================================
echo 请先执行 java\sql\init.sql 初始化数据库
echo 并修改 src\main\resources\application.yml 中的数据库密码
echo.

cd /d "%~dp0"
mvn spring-boot:run
