# app/pipeline.py

# 표준 라이브러리: 운영체제 환경변수 읽기(os), JSON 처리(json), 정규식(re)
import os, json, re
# 타입 힌트용
from typing import Dict, Any, List
# FastAPI의 HTTPException: 오류 시 HTTP 상태코드와 함께 에러 응답을 내보낼 때 사용
from fastapi import HTTPException
# OpenAI SDK: LLM을 호출하기 위해 사용
from openai import OpenAI
from anthropic import Anthropic
# 앞서 만든 프롬프트 불러오기
from .prompts import QUESTION_SYSTEM, QUESTION_USER_TMPL
from .prompts import REFLECTION_SYSTEM, REFLECTION_USER_TMPL
from .prompts import LETTER_SYSTEM, LETTER_USER_TMPL


# 사용할 LLM 모델명. 환경변수 LLM_MODEL이 없으면 "gpt-5"로 기본 설정
_API_KEY  = os.getenv("API_KEY",  "").strip()      

_QUESTIONS_MODEL = os.getenv("QUESTIONS_MODEL", "gpt-5").strip()
_REFLECTION_MODEL = os.getenv("REFLECTION_MODEL", "gpt-5").strip()
_LETTER_MODEL = os.getenv("LETTER_MODEL", "gpt-5").strip()

# 각 서비스별 클라이언트
_OPENAI_CLIENT = OpenAI(
    api_key=_API_KEY,
    base_url=os.getenv("OPENAI_BASE_URL")
)

_ANTHROPIC_CLIENT = Anthropic(
    api_key=_API_KEY,
    base_url=os.getenv("ANTHROPIC_BASE_URL")
)

def _chat(system: str, user: str, model: str) -> str:
    """모델에 따라 적절한 클라이언트 선택"""
    try:
        if model.startswith("claude"):
            # Claude 모델 사용
            resp = _ANTHROPIC_CLIENT.messages.create(
                model=model,
                max_tokens=4000,
                system=system,
                messages=[{"role": "user", "content": user}]
            )
            return resp.content[0].text.strip()
        else:
            # OpenAI 모델 사용
            resp = _OPENAI_CLIENT.chat.completions.create(
                model=model,
                messages=[
                    {"role": "system", "content": system},
                    {"role": "user", "content": user},
                ]
            )
            return resp.choices[0].message.content.strip()
    except Exception as e:
        # 외부 호출 실패는 502로 감싸서 반환
        raise HTTPException(status_code=502, detail=f"LLM 호출 실패: {e}")

def _parse_json(txt: str) -> Dict[str, Any]:
    """
    LLM이 JSON만 내야 하지만, 가끔 코드블록(```json ... ```) 등으로 섞어 내는 경우가 있어
    안전하게 JSON만 추출하려는 함수입니다.
    - 먼저 json.loads를 시도
    - 실패하면 정규식으로 { ... } 구간을 찾아서 다시 시도
    - 그래도 안되면 500 에러
    """
    try:
        # 가장 단순한 경우: 전체가 JSON 문자열인 경우
        return json.loads(txt)
    except Exception:
        # 중간에 불필요한 문구가 섞였을 때, { ... } 첫 블록만 가져와서 재시도
        m = re.search(r"\{[\s\S]*\}", txt)
        if m:
            return json.loads(m.group(0))
    # 두 방법 다 실패하면 500(서버 에러)
    raise HTTPException(status_code=500, detail="LLM JSON 파싱 실패")

def generate_questions(records: List[Dict[str, str]]) -> Dict[str, Any]:
    """
    기록들을 받아서 감정 분석 + 질문 생성을 한 번에 처리
    
    Args:
        records: [{"time":"09:12","text":"..."}, ...] 형태의 딕셔너리 리스트
    
    Returns:
        {
            "emotions": [...],
            "keywords": [...], 
            "questions": [...],
            "overall_summary": "..."
        }
    """
    # 사용자 메시지 생성 (기록들만)
    user_msg = QUESTION_USER_TMPL.format(
        records_json=json.dumps(records, ensure_ascii=False)
    )
    
    # LLM 호출 (감정분석 + 질문생성 통합)
    # out_text = _chat(QUESTION_SYSTEM, user_msg)
    out_text = _chat(QUESTION_SYSTEM, user_msg, _QUESTIONS_MODEL)
    
    # 안전하게 JSON 파싱
    return _parse_json(out_text)

def generate_reflection(original_records: List[Dict], qa_pairs: List[Dict], reflection_date: str = None) -> Dict[str, Any]:
    """
    원본 기록과 질문-답변을 받아서 데일리 회고를 생성
    """
    # 회고 생성 프롬프트 생성
    user_msg = REFLECTION_USER_TMPL.format(
        records_json=json.dumps(original_records, ensure_ascii=False),
        qa_json=json.dumps(qa_pairs, ensure_ascii=False),
        date=reflection_date or "오늘"
    )
    
    # LLM 호출
    # out_text = _chat(REFLECTION_SYSTEM, user_msg)
    out_text = _chat(REFLECTION_SYSTEM, user_msg, _REFLECTION_MODEL)
    
    # JSON 파싱 및 반환
    return _parse_json(out_text)

def generate_letter(receiver:str, sender:str, theme: str, reflections: List[Dict], mood: str, tone: str) -> Dict[str, Any]:
    """
    회고 데이터들과 설정을 받아서 편지를 생성
    """
    
    # 편지 생성 프롬프트 생성
    user_msg = LETTER_USER_TMPL.format(
        receiver=receiver,
        sender=sender,
        theme=theme,
        reflections_json=json.dumps(reflections, ensure_ascii=False),
        mood=mood,
        tone=tone
    )
    
    # LLM 호출
    # out_text = _chat(LETTER_SYSTEM, user_msg)
    out_text = _chat(LETTER_SYSTEM, user_msg, _LETTER_MODEL)
    
    # JSON 파싱 및 반환
    return _parse_json(out_text)
