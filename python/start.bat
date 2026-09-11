@echo off
chcp 65001 >nul
echo ========================================
echo  Python 服务启动（采集 + ML 推理）
echo  环境: Python 3.11
echo ========================================

cd /d "%~dp0"

if not exist .venv (
    echo 创建虚拟环境 .venv ...
    python -m venv .venv
)
call .venv\Scripts\activate.bat

echo.
echo [1/2] 安装依赖并训练模型...
python -m pip install --upgrade pip -q
pip install -r requirements.txt -q
cd ml
python train.py --data-dir ../data --model-dir ./saved_models
if errorlevel 1 (
    echo 模型训练失败，请确认 Python 版本为 3.11
    pause
    exit /b 1
)

echo.
echo [2/2] 启动 ML 推理服务 (端口 8000)...
echo 采集命令: cd crawler && python main.py
echo.
python main.py
