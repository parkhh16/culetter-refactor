# app/schemas.py
# 요청과 응답의 스키마를 정의해서, 잘못된 입력을 자동 검사함

# Pydantic은 데이터(요청/응답)의 구조를 정의하고, 자동으로 검증해주는 라이브러리이다.
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any, Literal

# [입력] 사용자 하루 기록의 "한 항목"을 표현하는 모델
class Record(BaseModel):
    # time: "09:12"처럼 시:분 문자열 (엄격 검증은 하지 않음)
    time: str = Field(..., description="예: '09:12' (엄격 검증하지 않음)")
    # text: 사용자가 말한/적은 내용(이미 STT로 변환되어 들어올 것)
    text: str

# [입력] /questions API에 보내는 전체 요청 바디
class QuestionsIn(BaseModel):
    # 하루에 여러 개의 기록이 있을 수 있으므로 List로 받음
    records: List[Record]

# [출력] 각 기록별 감정 분석 결과
class PerRecordEmotion(BaseModel):
    # 해당 기록의 시간 (입력과 동일)
    time: str
    # 감정 라벨 (예: "감격", "뿌듯", "피곤")
    emotion: str
    # 감정 강도 1~5 (정수)
    intensity: int
    # 왜 그렇게 판단했는지 한 줄 근거
    rationale: str

# [출력] 각 기록별 질문들
class RecordQuestions(BaseModel):
    record_time: str
    questions: List[str]

# [출력] /questions 응답 전체 형식
class QuestionsOut(BaseModel):
    # 감정 분석 결과
    emotions: List[PerRecordEmotion]
    keywords: List[str]
    
    # 질문 생성 결과
    questions: List[RecordQuestions]
    
    # 전체 요약
    overall_summary: str

# [입력] 
class QuestionAnswer(BaseModel):
    question: str = Field(..., description="원본 질문")
    answer: str = Field(..., description="사용자 답변")
    record_time: str = Field(..., description="해당 기록의 시간")

# [입력] /reflection API에 보내는 전체 요청 바디
class ReflectionIn(BaseModel):
    # 원본 기록들 (회고 작성 시 참고용)
    original_records: List[Record]
    # 질문과 답변 쌍들
    question_answers: List[QuestionAnswer]
    # 날짜 정보 (선택사항)
    reflection_date: Optional[str] = None

# [출력] /reflection 응답 전체 형식
class ReflectionOut(BaseModel):
    # 데일리 회고 내용 (긴 텍스트)
    daily_reflection: str = Field(..., description="상세한 데일리 회고")
    # 한 줄 요약
    summary: str = Field(..., description="하루를 한 줄로 요약")

# [입력] 회고 데이터 (편지에 사용할 회고)
class ReflectionData(BaseModel):
    date: str = Field(..., description="회고 날짜")
    daily_reflection: str = Field(..., description="데일리 회고 내용")

# [입력] /letter API에 보내는 편지 요청 바디
class LetterIn(BaseModel):
    receiver : str = Field(..., description="받는 이")
    sender : str = Field(..., description="보낸 이")
    theme: str = Field(..., description="편지 테마")
    reflections: List[ReflectionData] = Field(..., description="기간 동안의 회고 데이터들")
    mood: str = Field(..., description="편지 분위기")
    tone: str = Field(..., description="편지 말투")

# [출력] /letter 응답 전체 형식
class LetterOut(BaseModel):
    letter_content: str = Field(..., description="생성된 편지 내용")
    letter_title: str = Field(..., description="편지 제목")
