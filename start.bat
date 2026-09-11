@echo off
chcp 65001 >nul
echo ========================================
echo  智能文本分类与情感分析系统
echo ========================================
echo.
echo  请分别在三个目录启动服务:
echo.
echo  1. Python  ML服务+模型训练  python\start.bat   (:8000)
echo  2. Java    后端API           java\start.bat     (:8080)
echo  3. Vue     前端界面          vue\start.bat      (:5173)
echo.
echo  数据库初始化: mysql -u root -p ^< java\sql\init.sql
echo.
pause
