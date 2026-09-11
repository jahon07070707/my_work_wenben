"""本地 CSV 数据集采集器"""
import csv
import hashlib
from pathlib import Path
from typing import List, Dict


class LocalCSVCollector:
    """从本地 CSV 文件导入评论/新闻样本"""

    def __init__(self, name: str, file_path: str, data_type: str = 'comment'):
        self.name = name
        self.file_path = Path(file_path)
        self.data_type = data_type

    def collect(self) -> List[Dict]:
        results = []
        if not self.file_path.exists():
            print(f"[CSV] 文件不存在: {self.file_path}")
            return results

        with open(self.file_path, 'r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            for row in reader:
                content = row.get('content', '').strip()
                title = row.get('title', content[:50] if content else '').strip()
                if not content:
                    continue
                results.append({
                    'source_name': self.name,
                    'source_type': self.data_type,
                    'category_hint': row.get('category', ''),
                    'sentiment_hint': row.get('sentiment', ''),
                    'title': title,
                    'content': content,
                    'url': '',
                    'author': row.get('author', ''),
                    'publish_time': None,
                    'content_hash': hashlib.md5(content.encode()).hexdigest()
                })
        return results
