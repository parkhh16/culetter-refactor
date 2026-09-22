# app/routers/upload.py
import io, os, zipfile, logging, mimetypes, re   # ← re 추가
from typing import List
from fastapi import APIRouter, UploadFile, File, Form
from fastapi.responses import JSONResponse
from minio.error import S3Error

from app.minio import get_minio, ensure_bucket
from app.utils import norm_segment

log = logging.getLogger("voice_api.upload")
router = APIRouter()

BUCKET_NAME = "voice-logs"

def _strip_spaces_and_quotes(s: str) -> str:
    """양끝 공백/따옴표 제거 + 모든 공백문자(스페이스/탭/개행) 삭제"""
    s = (s or "").strip().strip('"').strip("'")
    return re.sub(r"\s+", "", s)

@router.post("/upload_zip")
async def upload_zip(
    user_id: str = Form(...),
    letter_id: str = Form(...),
    bundle: UploadFile = File(...),
):
    """
    ZIP 내 파일을 모두 s3://voice-logs/{user_id}/{letter_id}/ 아래 업로드
      - *.wav -> audio/wav (강제)
      - *.txt -> text/plain (강제)
      - *.json -> text/plain (강제)
      - 기타   -> mimetypes.guess_type() 결과 또는 application/octet-stream
    """
    try:
        mc = get_minio()
        ensure_bucket(BUCKET_NAME)

        # --- 공백/따옴표 제거 후 정상화 ---
        user_clean = _strip_spaces_and_quotes(user_id)
        letter_clean = _strip_spaces_and_quotes(letter_id)

        if not user_clean or not letter_clean:
            return JSONResponse({"error": "user_id/letter_id is empty after sanitization"}, status_code=400)

        user_norm = norm_segment(user_clean)
        letter_norm = norm_segment(letter_clean)
        # ---------------------------------

        zip_bytes = await bundle.read()
        with zipfile.ZipFile(io.BytesIO(zip_bytes), "r") as zf:
            saved: List[str] = []
            for name in zf.namelist():
                if name.endswith("/"):
                    continue

                data = zf.read(name)
                filename = os.path.basename(name)
                object_name = f"{user_norm}/{letter_norm}/{filename}"

                low = filename.lower()
                if low.endswith(".wav"):
                    content_type = "audio/wav"
                elif low.endswith(".txt"):
                    content_type = "text/plain"
                elif low.endswith(".json"):
                    content_type = "text/plain"
                else:
                    guessed, _ = mimetypes.guess_type(filename)
                    content_type = guessed or "application/octet-stream"

                mc.put_object(
                    BUCKET_NAME,
                    object_name,
                    io.BytesIO(data),
                    len(data),
                    content_type=content_type,
                )
                saved.append(object_name)

        log.info(
            "[upload_zip] %d files -> s3://%s/%s/%s/ (user_in=%r letter_in=%r)",
            len(saved), BUCKET_NAME, user_norm, letter_norm, user_id, letter_id
        )
        return {
            "bucket": BUCKET_NAME,
            "user_id": user_norm,
            "letter_id": letter_norm,
            "saved": saved,
            "count": len(saved),
        }

    except zipfile.BadZipFile:
        return JSONResponse({"error": "Invalid ZIP"}, status_code=400)
    except S3Error as e:
        return JSONResponse({"error": f"MinIO error: {e}"}, status_code=500)
    except Exception as e:
        log.exception("[upload_zip] error: %s", e)
        return JSONResponse({"error": str(e)}, status_code=500)
