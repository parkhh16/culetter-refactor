# app/config.py
import os
import urllib3
from dotenv import load_dotenv

# .env 로드 (없으면 패스)
try:
    load_dotenv()
except Exception:
    pass


def require_env(key: str) -> str:
    """필수 환경변수. 없으면 즉시 예외."""
    v = os.getenv(key)
    if v is None:
        raise RuntimeError(f"Missing required environment variable: {key}")
    return v


class Settings:
    # 서버
    APP_HOST: str = require_env("APP_HOST")
    APP_PORT: int = int(require_env("APP_PORT"))

    # MinIO
    MINIO_ENDPOINT: str   = require_env("MINIO_ENDPOINT")
    MINIO_ACCESS_KEY: str = require_env("MINIO_ACCESS_KEY")
    MINIO_SECRET_KEY: str = require_env("MINIO_SECRET_KEY")
    MINIO_SECURE: bool    = require_env("MINIO_SECURE").lower() == "true"

    # 외부에서 이 API에 접근할 베이스 URL (프록시 URL 생성에 사용)
    PUBLIC_API_BASE: str  = require_env("PUBLIC_API_BASE")

    # HTTP 타임아웃 (MinIO)
    HTTP_TIMEOUT = urllib3.util.Timeout(connect=3.0, read=10.0)


settings = Settings()
