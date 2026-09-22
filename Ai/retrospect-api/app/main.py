# app/main.py

# FastAPI: 웹 서버 프레임워크. HTTP 요청/응답 처리
from fastapi import FastAPI, HTTPException
# 입력/출력 스키마를 가져옴
from .schemas import QuestionsIn, QuestionsOut
from .schemas import ReflectionIn, ReflectionOut
from .schemas import LetterIn, LetterOut
# 파이프라인에서 LLM 호출/파싱하는 함수를 가져옴
from .pipeline import generate_questions
from .pipeline import generate_reflection
from .pipeline import generate_letter

# FastAPI 앱 인스턴스 생성 (title은 문서 페이지 등에 표시)
app = FastAPI(title="retrospect-api (question generation)")

# 헬스체크용 간단 엔드포인트: 서버가 살아있는지 확인
@app.get("/health")
def health():
    # 단순히 ok 상태를 반환
    return {"status": "ok"}

# POST /questions: 감정/키워드 추출 후 질문 생성 수행하는 메인 엔드포인트
# response_model=QuestionsOut: 응답이 이 스키마(형식)에 맞게 자동 검증/정렬
@app.post("/questions", response_model=QuestionsOut)
def questions(body: QuestionsIn):
    # records가 비어 있으면 400(Bad Request) 에러 반환
    if not body.records:
        raise HTTPException(status_code=400, detail="records가 비어 있습니다.")
    # pydantic 모델을 딕셔너리로 바꿔서 pipeline.generate_questions에 넘긴다.
    data = generate_questions([r.model_dump() for r in body.records])
    # FastAPI가 response_model에 맞게 자동 변환하여 응답으로 보낸다.
    return data

# POST /reflection: 회고 수행하는 메인 엔드포인트
# response_model=ReflectionOut: 응답이 이 스키마(형식)에 맞게 자동 검증/정렬
@app.post("/reflection", response_model=ReflectionOut)
def create_reflection(body: ReflectionIn):
    """질문-답변을 받아서 데일리 회고와 한 줄 요약을 생성"""
    if not body.question_answers:
        raise HTTPException(status_code=400, detail="질문-답변이 비어 있습니다.")
    
    # pipeline.generate_reflection 함수 호출
    reflection_data = generate_reflection(
        original_records=[r.model_dump() for r in body.original_records],
        qa_pairs=[qa.model_dump() for qa in body.question_answers],
        reflection_date=body.reflection_date
    )
    
    return reflection_data

# POST /letter: 편지 생성하는 메인 엔드포인트
# response_model=LetterOut: 응답이 이 스키마(형식)에 맞게 자동 검증/정렬
@app.post("/letter", response_model=LetterOut)
def create_letter(body: LetterIn):
    """회고 데이터들과 설정을 받아서 편지를 생성"""
    if not body.reflections:
        raise HTTPException(status_code=400, detail="회고 데이터가 비어 있습니다.")
    
    # pipeline.generate_letter 함수 호출 (그냥 바로 전달)
    letter_data = generate_letter(
        receiver=body.receiver,
        sender=body.sender,
        theme=body.theme,
        reflections=[r.model_dump() for r in body.reflections],
        mood=body.mood,
        tone=body.tone
    )
    
    return letter_data
