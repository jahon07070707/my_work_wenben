"""文本分析推理服务"""
import json
from pathlib import Path
import numpy as np
import torch
import joblib

from preprocess.text_processor import (
    TextVectorizer, extract_keywords, CATEGORIES, SENTIMENTS, SENTIMENT_LABELS
)
from models.network import TextClassifier, predict


class TextAnalyzer:
    """加载模型并提供分类+情感分析"""

    def __init__(self, model_dir: str = './saved_models'):
        self.model_dir = Path(model_dir)
        self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
        self._load_models()

    def _load_models(self):
        with open(self.model_dir / 'meta.json', 'r', encoding='utf-8') as f:
            self.meta = json.load(f)

        self.vectorizer = TextVectorizer.load(str(self.model_dir / 'vectorizer.pkl'))
        self.cat_encoder = joblib.load(str(self.model_dir / 'category_encoder.pkl'))
        self.sent_encoder = joblib.load(str(self.model_dir / 'sentiment_encoder.pkl'))

        input_dim = self.meta['input_dim']
        self.classifier = TextClassifier(input_dim, len(CATEGORIES)).to(self.device)
        self.classifier.load_state_dict(
            self._load_checkpoint(self.model_dir / 'classifier.pt')
        )

        self.sentiment_model = TextClassifier(input_dim, len(SENTIMENTS)).to(self.device)
        self.sentiment_model.load_state_dict(
            self._load_checkpoint(self.model_dir / 'sentiment.pt')
        )

    def _load_checkpoint(self, path: Path):
        """兼容 PyTorch 2.0+ 的 weights_only 参数"""
        try:
            return torch.load(path, map_location=self.device, weights_only=True)
        except TypeError:
            return torch.load(path, map_location=self.device)

    def analyze(self, text: str) -> dict:
        features = self.vectorizer.transform([text]).toarray()

        cat_pred, cat_conf, cat_probs = predict(self.classifier, features, self.device)
        sent_pred, sent_conf, sent_probs = predict(self.sentiment_model, features, self.device)

        category = self.cat_encoder.inverse_transform(cat_pred)[0]
        sentiment = self.sent_encoder.inverse_transform(sent_pred)[0]
        keywords = extract_keywords(text, top_k=5)

        return {
            'text': text,
            'category': category,
            'category_confidence': float(cat_conf[0]),
            'category_probs': {
                CATEGORIES[i]: float(cat_probs[0][i]) for i in range(len(CATEGORIES))
            },
            'sentiment': sentiment,
            'sentiment_label': SENTIMENT_LABELS.get(sentiment, sentiment),
            'sentiment_confidence': float(sent_conf[0]),
            'sentiment_probs': {
                SENTIMENTS[i]: float(sent_probs[0][i]) for i in range(len(SENTIMENTS))
            },
            'keywords': keywords
        }

    def analyze_batch(self, texts: list) -> list:
        return [self.analyze(t) for t in texts]
