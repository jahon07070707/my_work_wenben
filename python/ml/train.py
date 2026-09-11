"""Model training with evaluation metrics and baseline comparison"""
import json
import csv
import argparse
from datetime import datetime
from pathlib import Path
import numpy as np
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader, TensorDataset
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, f1_score, confusion_matrix, classification_report
import joblib

import sys
sys.path.insert(0, str(Path(__file__).parent))

from preprocess.text_processor import TextVectorizer, CATEGORIES, SENTIMENTS, text_to_string
from models.network import TextClassifier, predict


def load_training_data(data_dir: Path):
    texts, categories, sentiments = [], [], []

    comments_file = data_dir / 'samples' / 'comments.csv'
    if comments_file.exists():
        with open(comments_file, 'r', encoding='utf-8') as f:
            for row in csv.DictReader(f):
                texts.append(row['content'])
                categories.append(row.get('category', '社会'))
                sentiments.append(row.get('sentiment', 'neutral'))

    news_file = data_dir / 'samples' / 'news_samples.csv'
    if news_file.exists():
        with open(news_file, 'r', encoding='utf-8') as f:
            for row in csv.DictReader(f):
                content = row.get('content', row.get('title', ''))
                texts.append(content)
                categories.append(row.get('category', '社会'))
                sentiments.append('neutral')

    augmented_texts, augmented_cats, augmented_sents = [], [], []
    for t, c, s in zip(texts, categories, sentiments):
        augmented_texts.extend([t, t + '。'])
        augmented_cats.extend([c, c])
        augmented_sents.extend([s, s])

    return augmented_texts, augmented_cats, augmented_sents


def train_model(X_train, y_train, X_val, y_val, num_classes, model_path, epochs=100, lr=0.001):
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    input_dim = X_train.shape[1]

    model = TextClassifier(input_dim, num_classes).to(device)
    criterion = nn.CrossEntropyLoss()
    optimizer = optim.Adam(model.parameters(), lr=lr, weight_decay=1e-4)
    scheduler = optim.lr_scheduler.StepLR(optimizer, step_size=20, gamma=0.5)

    train_loader = DataLoader(
        TensorDataset(torch.FloatTensor(X_train), torch.LongTensor(y_train)),
        batch_size=16, shuffle=True
    )

    best_acc = 0
    for epoch in range(epochs):
        model.train()
        total_loss = 0
        for batch_x, batch_y in train_loader:
            batch_x, batch_y = batch_x.to(device), batch_y.to(device)
            optimizer.zero_grad()
            loss = criterion(model(batch_x), batch_y)
            loss.backward()
            optimizer.step()
            total_loss += loss.item()
        scheduler.step()

        model.eval()
        with torch.no_grad():
            val_x = torch.FloatTensor(X_val).to(device)
            val_y = torch.LongTensor(y_val).to(device)
            preds = model(val_x).argmax(dim=1)
            acc = (preds == val_y).float().mean().item()
        if acc > best_acc:
            best_acc = acc
            torch.save(model.state_dict(), model_path)
        if (epoch + 1) % 20 == 0:
            print(f"  Epoch {epoch+1}/{epochs}, Loss: {total_loss/len(train_loader):.4f}, Val Acc: {acc:.4f}")

    print(f"  Best val accuracy: {best_acc:.4f}")
    return model, device


def evaluate_pytorch(model, X_val, y_val, labels, device):
    model.eval()
    with torch.no_grad():
        preds, _, _ = predict(model, X_val, device)
    y_pred = preds
    return build_metrics(y_val, y_pred, labels)


def evaluate_baseline(X_train, y_train, X_val, y_val, labels):
    clf = LogisticRegression(max_iter=1000, random_state=42)
    clf.fit(X_train, y_train)
    y_pred = clf.predict(X_val)
    return build_metrics(y_val, y_pred, labels)


def build_metrics(y_true, y_pred, labels):
    report = classification_report(y_true, y_pred, labels=labels, output_dict=True, zero_division=0)
    return {
        'accuracy': round(float(accuracy_score(y_true, y_pred)), 4),
        'f1_macro': round(float(f1_score(y_true, y_pred, average='macro', zero_division=0)), 4),
        'f1_weighted': round(float(f1_score(y_true, y_pred, average='weighted', zero_division=0)), 4),
        'confusion_matrix': confusion_matrix(y_true, y_pred, labels=labels).tolist(),
        'labels': labels,
        'classification_report': report
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--data-dir', default='../data')
    parser.add_argument('--model-dir', default='./saved_models')
    args = parser.parse_args()

    data_dir = Path(args.data_dir)
    model_dir = Path(args.model_dir)
    model_dir.mkdir(parents=True, exist_ok=True)

    print("Loading training data...")
    texts, categories, sentiments = load_training_data(data_dir)
    print(f"  Samples: {len(texts)}")

    print("Vectorizing...")
    vectorizer = TextVectorizer(max_features=3000)
    X = vectorizer.fit_transform(texts).toarray()
    vectorizer.save(str(model_dir / 'vectorizer.pkl'))

    metrics = {
        'trained_at': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
        'sample_count': len(texts),
        'classification': {},
        'sentiment': {}
    }

    # Classification model
    print("\nTraining classification model...")
    cat_encoder = LabelEncoder()
    cat_encoder.fit(CATEGORIES)
    y_cat = cat_encoder.transform(categories)
    joblib.dump(cat_encoder, str(model_dir / 'category_encoder.pkl'))

    X_train, X_val, y_train, y_val = train_test_split(
        X, y_cat, test_size=0.2, random_state=42, stratify=y_cat
    )
    cat_model, device = train_model(
        X_train, y_train, X_val, y_val, len(CATEGORIES),
        str(model_dir / 'classifier.pt'), epochs=100
    )
    cat_labels = list(range(len(CATEGORIES)))
    metrics['classification']['pytorch_mlp'] = evaluate_pytorch(cat_model, X_val, y_val, cat_labels, device)
    metrics['classification']['baseline_lr'] = evaluate_baseline(X_train, y_train, X_val, y_val, cat_labels)
    metrics['classification']['label_names'] = CATEGORIES

    # Sentiment model
    print("\nTraining sentiment model...")
    sent_encoder = LabelEncoder()
    sent_encoder.fit(SENTIMENTS)
    y_sent = sent_encoder.transform(sentiments)
    joblib.dump(sent_encoder, str(model_dir / 'sentiment_encoder.pkl'))

    X_train, X_val, y_train, y_val = train_test_split(
        X, y_sent, test_size=0.2, random_state=42, stratify=y_sent
    )
    sent_model, device = train_model(
        X_train, y_train, X_val, y_val, len(SENTIMENTS),
        str(model_dir / 'sentiment.pt'), epochs=100
    )
    sent_labels = list(range(len(SENTIMENTS)))
    metrics['sentiment']['pytorch_mlp'] = evaluate_pytorch(sent_model, X_val, y_val, sent_labels, device)
    metrics['sentiment']['baseline_lr'] = evaluate_baseline(X_train, y_train, X_val, y_val, sent_labels)
    metrics['sentiment']['label_names'] = SENTIMENTS

    meta = {
        'categories': CATEGORIES,
        'sentiments': SENTIMENTS,
        'input_dim': int(X.shape[1]),
        'sample_count': len(texts),
        'classification_accuracy': metrics['classification']['pytorch_mlp']['accuracy'],
        'sentiment_accuracy': metrics['sentiment']['pytorch_mlp']['accuracy']
    }
    with open(model_dir / 'meta.json', 'w', encoding='utf-8') as f:
        json.dump(meta, f, ensure_ascii=False, indent=2)
    with open(model_dir / 'metrics.json', 'w', encoding='utf-8') as f:
        json.dump(metrics, f, ensure_ascii=False, indent=2)

    print(f"\nModels saved to {model_dir}")
    print(f"  Classification acc: {meta['classification_accuracy']}")
    print(f"  Sentiment acc: {meta['sentiment_accuracy']}")


if __name__ == '__main__':
    main()
