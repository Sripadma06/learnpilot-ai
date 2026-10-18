from fastapi import FastAPI
from pydantic import BaseModel, Field
from typing import Literal

app = FastAPI(title="LearnPilot ML Service", version="0.1.0")

class PredictionRequest(BaseModel):
    recent_accuracy: float = Field(ge=0, le=1)
    difficulty: int = Field(ge=1, le=5)
    previous_attempts: int = Field(ge=0, le=10000)
    days_since_practice: float = Field(ge=0, le=3650)

class PredictionResponse(BaseModel):
    probability_correct: float
    model: str
    note: str

def baseline_probability(payload: PredictionRequest) -> float:
    # Transparent placeholder baseline, NOT a trained model.
    # Replace with a serialized, evaluated scikit-learn model before making ML claims.
    accuracy = payload.recent_accuracy
    difficulty_penalty = (payload.difficulty - 1) * 0.055
    practice_bonus = min(payload.previous_attempts, 10) * 0.012
    forgetting_penalty = min(payload.days_since_practice, 60) * 0.002
    value = accuracy - difficulty_penalty + practice_bonus - forgetting_penalty
    return round(max(0.02, min(0.98, value)), 4)

@app.get("/health")
def health():
    return {"status": "ok", "model_ready": False}

@app.post("/internal/predict-correctness", response_model=PredictionResponse)
def predict(payload: PredictionRequest):
    return PredictionResponse(
        probability_correct=baseline_probability(payload),
        model="transparent-baseline-placeholder",
        note="Not a trained ML model. Train, evaluate, version, and load a real model before using for production recommendations."
    )
