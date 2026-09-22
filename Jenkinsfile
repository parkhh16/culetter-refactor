// Jenkinsfile
pipeline {
    agent any

    environment {
        ENV_FILE = ".env"
    }

    stages {
        stage('Checkout') {
            steps {
                // GitLab 저장소에서 최신 코드를 가져오기
                checkout scm
            }
        }

        stage('Build & Deploy with Docker Compose') {
            steps {
                script {

                    // .env 파일이 실제로 존재하는지 먼저 확인합니다.

                    if (!fileExists(env.ENV_FILE)) {
                        error ".env 파일이 Jenkins 작업 공간에 없습니다!"
                    }

                    echo "🚀 변경 감지된 서비스가 있어 Docker Compose로 빌드 및 배포를 진행합니다."

                    // docker-compose 명령어에 --env-file 옵션을 명시적으로 추가합니다.
                    sh "docker-compose --env-file ${env.ENV_FILE} up --build -d --force-recreate"

                }
            }
        }

        stage('Cleanup Docker Images') {
            steps {
                echo "🧹 불필요한 도커 이미지 정리중..."

                // 빌드 과정에서 사용된 중간 이미지나, 더 이상 사용되지 않는 이미지들을 삭제하여 EC2 서버의 디스크 용량을 확보
                sh 'docker image prune -f'
            }
        }
    }

    post {
        always {
            echo "🏁 파이프라인 실행 종료"
        }
        success {
            echo "✅ 배포 성공!"
        }
        failure {
            echo "❌ 배포 실패!"
        }
    }
}