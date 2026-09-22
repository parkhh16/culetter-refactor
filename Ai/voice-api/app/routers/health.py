# app/routers/health.py
from fastapi import APIRouter
from fastapi.responses import JSONResponse
from app.minio import get_minio

router = APIRouter()

@router.get("/health")
def health():
    try:
        list(get_minio().list_buckets())
        return {"ok": True}
    except Exception as e:
        return JSONResponse({"ok": False, "error": str(e)}, status_code=500)
