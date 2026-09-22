# app/main.py
import logging
from fastapi import FastAPI, APIRouter
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.routers import health, upload, audio

logging.basicConfig(level=logging.INFO, format="%(levelname)s:%(name)s: %(message)s")
log = logging.getLogger("voice_api")

def create_app() -> FastAPI:
    app = FastAPI(title="Voice API", version="1.0.0")

    # CORS
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"], allow_credentials=True,
        allow_methods=["*"], allow_headers=["*"],
    )

    # 라우터 묶기
    api = APIRouter()
    api.include_router(health.router, tags=["health"])
    api.include_router(upload.router, tags=["upload"])
    api.include_router(audio.router, tags=["audio"])

    # /voice-api/v1 접두어로 마운트
    app.include_router(api, prefix="/voice-api/v1")

    @app.on_event("startup")
    async def _startup():
        from app.minio import probe_minio
        log.info("[BOOT] MINIO_ENDPOINT=%s secure=%s", settings.MINIO_ENDPOINT, settings.MINIO_SECURE)
        try:
            probe_minio()
            log.info("[BOOT] MinIO reachable ✔")
            log.info("[BOOT] PUBLIC_API_BASE=%s", settings.PUBLIC_API_BASE)
        except Exception as e:
            log.error("[BOOT] MinIO unreachable ✖  %s", e)

    return app

app = create_app()

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host=settings.APP_HOST, port=settings.APP_PORT)
