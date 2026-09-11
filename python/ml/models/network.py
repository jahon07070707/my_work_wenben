"""PyTorch 文本分类与情感分析模型"""
import torch
import torch.nn as nn
import torch.nn.functional as F


class TextClassifier(nn.Module):
    """基于全连接网络的文本分类/情感分析模型"""

    def __init__(self, input_dim: int, num_classes: int, hidden_dim: int = 256, dropout: float = 0.3):
        super().__init__()
        self.fc1 = nn.Linear(input_dim, hidden_dim)
        self.bn1 = nn.BatchNorm1d(hidden_dim)
        self.dropout1 = nn.Dropout(dropout)
        self.fc2 = nn.Linear(hidden_dim, hidden_dim // 2)
        self.bn2 = nn.BatchNorm1d(hidden_dim // 2)
        self.dropout2 = nn.Dropout(dropout)
        self.fc3 = nn.Linear(hidden_dim // 2, num_classes)

    def forward(self, x):
        x = self.dropout1(F.relu(self.bn1(self.fc1(x))))
        x = self.dropout2(F.relu(self.bn2(self.fc2(x))))
        return self.fc3(x)


def predict(model: nn.Module, features, device='cpu'):
    """模型推理"""
    model.eval()
    with torch.no_grad():
        if not isinstance(features, torch.Tensor):
            features = torch.FloatTensor(features)
        features = features.to(device)
        logits = model(features)
        probs = F.softmax(logits, dim=-1)
        conf, pred = torch.max(probs, dim=-1)
    return pred.cpu().numpy(), conf.cpu().numpy(), probs.cpu().numpy()
