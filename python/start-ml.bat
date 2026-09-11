@echo off
chcp 65001 >nul
echo ========================================
echo  仅启动 ML 推理服务 (端口 8000)
echo  跳过训练，适合模型已存在时使用
echo ========================================

cd /d "%~dp0"

if not exist .venv (
    echo 创建虚拟环境 .venv ...
    python -m venv .venv
)
call .venv\Scripts\activate.bat

if not exist ml\saved_models\classifier.pt (
    echo 模型不存在，正在训练...
    pip install -r requirements.txt -q
    cd ml
    python train.py --data-dir ../data --model-dir ./saved_models
    cd ..
)

echo 启动 ML 服务: http://localhost:8000
cd ml
python main.py
