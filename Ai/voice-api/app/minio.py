# app/minio.py
import urllib3
from minio import Minio
from app.config import settings

_minio: Minio | None = None

def get_minio() -> Minio:
    """MinIO 싱글톤 클라이언트"""
    global _minio
    if _minio is None:
        http_client = urllib3.PoolManager(timeout=settings.HTTP_TIMEOUT)
        _minio = Minio(
            endpoint=settings.MINIO_ENDPOINT,
            access_key=settings.MINIO_ACCESS_KEY,
            secret_key=settings.MINIO_SECRET_KEY,
            secure=settings.MINIO_SECURE,
            http_client=http_client,
        )
    return _minio

def ensure_bucket(bucket: str):
    """버킷이 없으면 생성"""
    mc = get_minio()
    if not mc.bucket_exists(bucket):
        mc.make_bucket(bucket)

def probe_minio():
    """연결 확인(부팅시 한 번)"""
    list(get_minio().list_buckets())
