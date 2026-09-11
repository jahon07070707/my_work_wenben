"""从 python 根目录一键启动 ML 推理服务"""
import runpy
from pathlib import Path

if __name__ == '__main__':
    ml_main = Path(__file__).parent / 'ml' / 'main.py'
    runpy.run_path(str(ml_main), run_name='__main__')
