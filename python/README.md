# Python 服务快速启动

## PowerShell 中运行 bat 必须加 `.\`

```powershell
.\start-ml.bat    # 只启动 ML 服务（推荐）
.\start.bat       # 安装依赖 + 训练 + 启动
```

## 手动启动（当前已在 python 目录）

```powershell
# 方式1：根目录直接启动（main.py 已放在这里）
python main.py

# 方式2：进入 ml 目录启动
cd ml
python main.py
```

## 采集数据（另开终端）

```powershell
cd crawler
python main.py
```

## 成功标志

```
ML 模型加载完成
Uvicorn running on http://0.0.0.0:8000
```

浏览器访问 http://localhost:8000/health 应返回 model_loaded: true
