"""文本预处理：分词、去停用词、向量化"""
import re
import jieba
import jieba.analyse
from pathlib import Path
from typing import List, Tuple
from sklearn.feature_extraction.text import TfidfVectorizer
import joblib

# NLTK 停用词（英文）+ 中文停用词
STOPWORDS_CN = set([
    '的', '了', '在', '是', '我', '有', '和', '就', '不', '人', '都', '一', '一个',
    '上', '也', '很', '到', '说', '要', '去', '你', '会', '着', '没有', '看', '好',
    '自己', '这', '那', '他', '她', '它', '我们', '他们', '这个', '那个', '什么',
    '怎么', '为什么', '可以', '已经', '还是', '但是', '因为', '所以', '如果',
    '虽然', '而且', '或者', '以及', '等', '与', '及', '被', '把', '让', '给',
    '对', '从', '向', '以', '为', '之', '其', '所', '能', '将', '还', '又', '再'
])

CATEGORIES = ['科技', '财经', '体育', '娱乐', '教育', '健康', '社会']
SENTIMENTS = ['positive', 'negative', 'neutral']
SENTIMENT_LABELS = {'positive': '正面', 'negative': '负面', 'neutral': '中性'}


def clean_text(text: str) -> str:
    """清洗文本：去除URL、HTML、特殊字符"""
    text = re.sub(r'http[s]?://\S+', '', text)
    text = re.sub(r'<[^>]+>', '', text)
    text = re.sub(r'[^\u4e00-\u9fff\w\s]', ' ', text)
    text = re.sub(r'\s+', ' ', text).strip()
    return text


def tokenize(text: str, remove_stopwords: bool = True) -> List[str]:
    """中文分词 + 去停用词"""
    text = clean_text(text)
    words = jieba.lcut(text)
    if remove_stopwords:
        words = [w for w in words if w.strip() and w not in STOPWORDS_CN and len(w) > 1]
    return words


def text_to_string(text: str) -> str:
    """分词后拼接为空格分隔字符串（供 TF-IDF 使用）"""
    return ' '.join(tokenize(text))


def extract_keywords(text: str, top_k: int = 5) -> List[str]:
    """提取关键词"""
    text = clean_text(text)
    kws = jieba.analyse.extract_tags(text, topK=top_k)
    return kws


class TextVectorizer:
    """TF-IDF 向量化器封装"""

    def __init__(self, max_features: int = 5000):
        self.vectorizer = TfidfVectorizer(max_features=max_features, ngram_range=(1, 2))
        self.max_features = max_features

    def fit(self, texts: List[str]):
        processed = [text_to_string(t) for t in texts]
        self.vectorizer.fit(processed)
        return self

    def transform(self, texts: List[str]):
        processed = [text_to_string(t) for t in texts]
        return self.vectorizer.transform(processed)

    def fit_transform(self, texts: List[str]):
        processed = [text_to_string(t) for t in texts]
        return self.vectorizer.fit_transform(processed)

    def save(self, path: str):
        joblib.dump(self.vectorizer, path)

    @classmethod
    def load(cls, path: str) -> 'TextVectorizer':
        obj = cls()
        obj.vectorizer = joblib.load(path)
        return obj
