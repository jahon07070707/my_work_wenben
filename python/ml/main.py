"""FastAPI ML inference service"""
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
from pathlib import Path
import json
import sys

sys.path.insert(0, str(Path(__file__).parent))
from predict import TextAnalyzer
from preprocess.text_processor import clean_text, tokenize, extract_keywords, text_to_string

MODEL_DIR = Path(__file__).parent / 'saved_models'
analyzer: Optional[TextAnalyzer] = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global analyzer
    if (MODEL_DIR / 'classifier.pt').exists():
        analyzer = TextAnalyzer(str(MODEL_DIR))
        print("ML model loaded")
    else:
        print("Warning: run train.py first")
    yield


app = FastAPI(title="Text Analysis ML Service", version="1.0.0", lifespan=lifespan)


class AnalyzeRequest(BaseModel):
    text: str


class BatchAnalyzeRequest(BaseModel):
    texts: List[str]


class PreprocessRequest(BaseModel):
    text: str


app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health():
    return {"status": "ok", "model_loaded": analyzer is not None}


@app.get("/metrics")
def get_metrics():
    metrics_file = MODEL_DIR / 'metrics.json'
    meta_file = MODEL_DIR / 'meta.json'
    meta = None
    if meta_file.exists():
        with open(meta_file, 'r', encoding='utf-8') as f:
            meta = json.load(f)
    metrics = None
    if metrics_file.exists():
        with open(metrics_file, 'r', encoding='utf-8') as f:
            metrics = json.load(f)
    if meta is None and metrics is None:
        return {
            "meta": None,
            "metrics": None,
            "message": "No model trained yet, click train button in web UI"
        }
    return {
        "meta": meta,
        "metrics": metrics,
        "message": None if metrics else "Train model to generate full metrics"
    }


@app.post("/preprocess")
def preprocess(req: PreprocessRequest):
    if not req.text.strip():
        raise HTTPException(400, "Text cannot be empty")
    words = tokenize(req.text)
    return {
        "original": req.text,
        "cleaned": clean_text(req.text),
        "tokens": words,
        "token_string": text_to_string(req.text),
        "keywords": extract_keywords(req.text, top_k=8),
        "token_count": len(words)
    }


@app.post("/analyze")
def analyze(req: AnalyzeRequest):
    if not analyzer:
        raise HTTPException(503, "Model not loaded")
    if not req.text.strip():
        raise HTTPException(400, "Text cannot be empty")
    return analyzer.analyze(req.text)


@app.post("/analyze/batch")
def analyze_batch(req: BatchAnalyzeRequest):
    if not analyzer:
        raise HTTPException(503, "Model not loaded")
    return analyzer.analyze_batch(req.texts)


@app.post("/reload")
def reload_model():
    global analyzer
    if not (MODEL_DIR / 'classifier.pt').exists():
        raise HTTPException(404, "Model files not found, train first")
    analyzer = TextAnalyzer(str(MODEL_DIR))
    return {"status": "ok", "model_loaded": True}


if __name__ == '__main__':
    import uvicorn
    uvicorn.run(app, host='0.0.0.0', port=8000)
