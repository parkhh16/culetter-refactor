# app/routers/audio.py
import logging
from typing import Optional, Iterable
from fastapi import APIRouter, Query, Request
from fastapi.responses import JSONResponse, StreamingResponse

from app.config import settings
from app.minio import get_minio, ensure_bucket
from app.utils import norm_segment, is_audio

router = APIRouter()
log = logging.getLogger("voice_api.audio")


def _iter_objects(bucket: str, prefix: str) -> Iterable:
    mc = get_minio()
    return mc.list_objects(bucket, prefix=prefix, recursive=True)

def _find_latest_audio(bucket: str, device: str, prefix: str, verbose: bool = True):
    """
    s3://{bucket}/{device}/{prefix}/ 아래 최신 오디오 오브젝트 탐색 (없으면 None)
    """
    device_norm = norm_segment(device)
    prefix_norm = norm_segment(prefix)
    base_prefix = f"{device_norm}/{prefix_norm}/" if prefix_norm else f"{device_norm}/"
    total_scanned = 0
    total_audio = 0
    latest_obj = None

    if verbose:
        log.info("[latest_audio] bucket=%s device=%s prefix=%s -> base_prefix=%s",
                 bucket, device_norm, prefix_norm, base_prefix)

    for obj in _iter_objects(bucket, base_prefix):
        total_scanned += 1
        name = getattr(obj, "object_name", "")
        lower = name.lower()
        aud = is_audio(lower)
        if verbose:
            log.info("  - object: %s (size=%s, last_modified=%s)%s",
                     name, getattr(obj, "size", "?"), getattr(obj, "last_modified", "?"),
                     " [AUDIO]" if aud else "")

        if not aud:
            continue

        total_audio += 1
        if latest_obj is None or obj.last_modified > latest_obj.last_modified:
            latest_obj = obj

    if latest_obj is None and verbose:
        log.warning("[latest_audio] audio not found. scanned=%d, audio=%d, base_prefix=%s",
                    total_scanned, total_audio, base_prefix)

    return latest_obj


@router.get("/audio_proxy")
def audio_proxy(
    bucket: str = Query(..., description="예: voice-logs"),
    object_name: str = Query(..., description="예: sm-g973.../result/xxx.wav"),
    request: Request = None,
):
    """
    MinIO 객체를 FastAPI가 직접 스트리밍(프록시).
    MediaPlayer의 Range 요청도 그대로 전달.
    """
    ensure_bucket(bucket)
    mc = get_minio()

    req_headers = {}
    if request and "range" in request.headers:
        req_headers["Range"] = request.headers["range"]

    try:
        obj = mc.get_object(bucket, object_name, request_headers=req_headers or None)
    except Exception as e:
        log.exception("[audio_proxy] get_object error: %s", e)
        return JSONResponse({"error": str(e)}, status_code=502)

    # Content-Type 추정
    low = object_name.lower()
    if low.endswith(".wav"):
        media_type = "audio/wav"
    elif low.endswith(".mp3"):
        media_type = "audio/mpeg"
    elif low.endswith(".flac"):
        media_type = "audio/flac"
    else:
        media_type = "application/octet-stream"

    # 원본 Range/Length 헤더 전달
    hdrs = {}
    resp_hdrs = getattr(obj, "response_headers", {}) or {}
    for k, v in resp_hdrs.items():
        lk = k.lower()
        if lk in ("accept-ranges", "content-range", "content-length"):
            hdrs[k] = v

    log.info("[audio_proxy] streaming s3://%s/%s", bucket, object_name)
    return StreamingResponse(obj, media_type=media_type, headers=hdrs)


@router.get("/latest_audio")
def latest_audio(
    bucket: str = Query(..., description="예: voice-logs"),
    device: str = Query(..., description="예: galaxy-s23-abc123"),
    prefix: str = Query("result", description="기본: result, 없으면 outputs로 폴백"),
    fallback_prefix: Optional[str] = Query("outputs", description="result 아래 없으면 여기서 탐색"),
    verbose: bool = Query(True, description="오브젝트 스캔 로그 출력 여부"),
):
    """
    최신 오디오를 찾아 presigned 대신 FastAPI 프록시 URL을 반환.
      - {"found": true, "url": ".../audio_proxy?bucket=...&object_name=..."}
      - {"found": false}
    """
    try:
        ensure_bucket(bucket)

        latest_obj = _find_latest_audio(bucket, device, prefix, verbose=verbose)
        if latest_obj is None and fallback_prefix:
            log.info("[latest_audio] fallback to prefix: %s", fallback_prefix)
            latest_obj = _find_latest_audio(bucket, device, fallback_prefix, verbose=verbose)

        if latest_obj is None:
            return {"found": False}

        proxy_url = (
            f"{settings.PUBLIC_API_BASE.rstrip('/')}/voice-api/v1/audio_proxy"
            f"?bucket={bucket}&object_name={latest_obj.object_name}"
        )

        log.info("[latest_audio] FOUND: %s", latest_obj.object_name)
        log.info("[latest_audio] proxy url : %s", proxy_url)
        return {"found": True, "url": proxy_url, "object": latest_obj.object_name}

    except Exception as e:
        log.exception("[latest_audio] error: %s", e)
        return JSONResponse({"found": False, "error": str(e)}, status_code=500)
