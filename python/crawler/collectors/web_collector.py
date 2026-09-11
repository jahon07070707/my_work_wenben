"""网页新闻采集器 - 人民网等公开新闻页面（示例）"""
import re
import hashlib
import requests
from bs4 import BeautifulSoup
from typing import List, Dict
from datetime import datetime


class WebNewsCollector:
    """从新闻列表页采集标题与摘要（礼貌爬取，仅作演示）"""

    HEADERS = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) TextAnalysisBot/1.0'
    }

    def __init__(self, name: str, url: str, category: str = '社会'):
        self.name = name
        self.url = url
        self.category = category

    def collect(self, max_items: int = 20) -> List[Dict]:
        results = []
        try:
            resp = requests.get(self.url, headers=self.HEADERS, timeout=15)
            resp.encoding = resp.apparent_encoding or 'utf-8'
            soup = BeautifulSoup(resp.text, 'lxml')

            links = soup.select('a[href]')
            seen = set()
            for a in links:
                title = a.get_text(strip=True)
                href = a.get('href', '')
                if len(title) < 8 or len(title) > 80:
                    continue
                if not href or href.startswith('javascript'):
                    continue
                if href.startswith('/'):
                    from urllib.parse import urljoin
                    href = urljoin(self.url, href)
                key = title[:30]
                if key in seen:
                    continue
                seen.add(key)
                results.append({
                    'source_name': self.name,
                    'source_type': 'news',
                    'category_hint': self.category,
                    'title': title,
                    'content': title,
                    'url': href,
                    'author': '',
                    'publish_time': datetime.now().isoformat(),
                    'content_hash': hashlib.md5(title.encode()).hexdigest()
                })
                if len(results) >= max_items:
                    break
        except Exception as e:
            print(f"[Web] 采集失败 {self.name}: {e}")
        return results
