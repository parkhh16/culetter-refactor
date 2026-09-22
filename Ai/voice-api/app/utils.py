# app/utils.py
def norm_segment(s: str) -> str:
    """앞뒤 슬래시/공백 제거 + 중복 슬래시 정규화"""
    return "/".join(seg for seg in s.strip().strip("/ ").split("/") if seg)

def is_audio(name: str) -> bool:
    l = name.lower()
    return l.endswith((".wav", ".mp3", ".flac"))
