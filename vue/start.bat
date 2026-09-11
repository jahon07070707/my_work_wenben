@echo off
chcp 65001 >nul
echo ========================================
echo  Vue 前端启动 (:5173)
echo  环境: Node.js 22.x
echo ========================================

cd /d "%~dp0"
if not exist node_modules (
    echo 首次运行，正在安装依赖...
    call npm install
)
npm run dev
