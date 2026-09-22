# 1. Python 베이스 이미지
FROM python:3.10-slim

# 2. 작업 디렉토리
WORKDIR /app

# 3. 의존성 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 4. 앱 코드 복사
COPY . .

# 5. 환경 변수 (기본값, 필요시 docker run -e 로 덮어쓰기 가능)
ENV APP_HOST=0.0.0.0 \
    APP_PORT=8081

# 6. 포트 개방
EXPOSE 8081

# 7. uvicorn 실행 (리로드 제거, 프로덕션용)
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8081"]
