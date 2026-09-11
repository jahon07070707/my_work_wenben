"""RSS 新闻采集器 - 支持新浪 RSS 等公开源"""
import feedparser
import hashlib
from datetime import datetime
from typing import List, Dict


class RSSCollector:
    """从 RSS 订阅源采集新闻标题与正文摘要"""

    def __init__(self, name: str, url: str, category: str = None):
        self.name = name
        self.url = url
        self.category = category

    def collect(self, max_items: int = 50) -> List[Dict]:
        results = []
        try:
            feed = feedparser.parse(self.url)
            for entry in feed.entries[:max_items]:
                title = entry.get('title', '').strip()
                content = entry.get('summary', entry.get('description', '')).strip()
                link = entry.get('link', '')
                if not content and title:
                    content = title
                if not title:
                    continue

                pub_time = None
                if hasattr(entry, 'published_parsed') and entry.published_parsed:
                    pub_time = datetime(*entry.published_parsed[:6]).isoformat()

                results.append({
                    'source_name': self.name,
                    'source_type': 'rss',
                    'category_hint': self.category,
                    'title': title,
                    'content': self._clean_html(content),
                    'url': link,
                    'author': entry.get('author', ''),
                    'publish_time': pub_time,
                    'content_hash': hashlib.md5((title + content).encode()).hexdigest()
                })
        except Exception as e:
            print(f"[RSS] 采集失败 {self.name}: {e}")
        return results

    @staticmethod
    def _clean_html(text: str) -> str:
        import re
        text = re.sub(r'<[^>]+>', '', text)
        text = re.sub(r'\s+', ' ', text).strip()
        return text
