# ✉️ See You Letter

<p align="center">
  <!-- Tech badges -->
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white" />
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-007396?logo=java&logoColor=white" />
  <img alt="Spring Boot 3.5" src="https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white" />
  <img alt="JPA" src="https://img.shields.io/badge/JPA-59666C?logo=hibernate&logoColor=white" />
  <img alt="QueryDSL" src="https://img.shields.io/badge/QueryDSL-334155" />
  <img alt="MySQL 8" src="https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white" />
  <img alt="MinIO" src="https://img.shields.io/badge/MinIO-EB1F2A?logo=minio&logoColor=white" />
  <img alt="IPFS" src="https://img.shields.io/badge/IPFS-65C2CB?logo=ipfs&logoColor=white" />
  <img alt="Docker" src="https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white" />
  <img alt="AWS EC2" src="https://img.shields.io/badge/AWS-EC2-FF9900?logo=amazon%20aws&logoColor=white" />
  <img alt="Jenkins" src="https://img.shields.io/badge/Jenkins-D24939?logo=jenkins&logoColor=white" />
  <img alt="Firebase" src="https://img.shields.io/badge/Firebase-FFCA28?logo=firebase&logoColor=white" />
</p>

<p align="center">
  <!-- Quick links -->
  <a href="https://www.canva.com/design/DAGz2Rc7FMY/x96ehzGOsQSt35aA7T2VPQ/view?utm_content=DAGz2Rc7FMY&utm_campaign=designshare&utm_medium=link2&utm_source=uniquelinks&utlId=hf72ab5af50">
    <img alt="Slides" src="https://img.shields.io/badge/Slides-Canva-00C4CC?logo=canva&logoColor=white" />
  </a>
  <a href="#-서비스-소개">
    <img alt="Docs" src="https://img.shields.io/badge/Docs-README-0A0A0A?logo=readme&logoColor=white" />
  </a>
  <img alt="Made in Korea" src="https://img.shields.io/badge/Made%20in-Korea-000000?logo=google-earth&logoColor=white" />
</p>

> **AI와 함께 사랑하는 사람에게 전하는 디지털 타임캡슐**  
> 간편하게 기록하고, 감정을 담아 전하고, NFT/음성복제로 특별함을 더합니다.

---

## 🧭 목차
- [💌 서비스 소개](#-서비스-소개)
- [📅 개발 기간](#-개발-기간)
- [👥 멤버](#-멤버)
- [🧰 개발 환경 (기술 스택)](#-개발-환경-기술-스택)
- [✨ 주요 기능](#-주요-기능)
- [🏗️ 프로젝트 아키텍쳐](#-프로젝트-아키텍쳐)
- [🗃️ ERD](#-erd)
- [🖼️ 서비스 화면](#-서비스-화면)
- [🎞️ 발표 자료 (PPT)](#️-발표-자료-ppt)

---

## 💌 서비스 소개
**AI와 함께 사랑하는 사람에게 전하는 디지털 타임캡슐 서비스**
1. **감정 전달에 집중**: 아이/연인/가족 등 소중한 사람에게 진심을 담아 전해요.
2. **간편한 기록**: 짧은 글/음성만으로 쉽게 편지를 만들 수 있어요.
3. **특별한 선물化**: **NFT**와 **음성 복제(Voice Clone)**로 가치 있는 형태로 보관·전달합니다.

---

## 📅 개발 기간
**2025.09.01 ~ 2025.09.29 (4주)**

---

## 👥 멤버
### 팀 소개
| 하규원 | 김주희 | 박태원 | 박현호 | 이성준 | 이동하 |
|:---:|:---:|:---:|:---:|:---:|:---:|
| FE | AI | FE | BE | BE | BE |

> 역할: **FE(프론트엔드)**, **BE(백엔드)**, **AI(모델/파이프라인)**

---

## 🧰 개발 환경 (기술 스택)
| 구분 | 사용 기술 |
|---|---|
| **Front-end** | Kotlin (Jetpack Compose) |
| **Back-end** | Java 21, Spring Boot 3.5, Spring Security (JWT/OAuth2), JPA & QueryDSL |
| **Data & Storage** | MySQL, MinIO (S3-compatible), IPFS (Pinata) |
| **Infra / DevOps** | Docker & Docker Compose, AWS EC2, Jenkins, Firebase Auth |

---

## ✨ 주요 기능
- 🔐 **인증/보안**: Firebase Auth + JWT(OAuth2) 기반 인증
- 📝 **편지 작성**: 텍스트/음성 입력 → AI 보정으로 문맥·톤 개선
- 🎙️ **음성 복제**: 지정 화자톤으로 레터 오디오 생성
- ⏰ **예약 발송**: 지정 시점에 자동 전달(스케줄러 기반)
- 🧩 **NFT 발행**: 편지/오디오/메타데이터를 IPFS에 저장하고 NFT로 민팅
- 📦 **스토리지**: MinIO ↔ Pinata(IPFS) 안전 업로드 및 CID 관리
- 🛰️ **웹훅/비동기 처리**: 오디오 생성/업로드 완료 시 후속 파이프라인 자동 실행
- 🗄️ **백오피스**(선택): 발행/실패 로그, 재시도 관리

---

## 🏗️ 프로젝트 아키텍쳐
![See You Letter Architecture](<See you Letter 아키텍쳐.png>)

> 백엔드: Spring Boot / Scheduler / WebClient  
> 스토리지: MinIO(S3) ↔ Pinata(IPFS)  
> 온체인: NFT 민팅 및 메타데이터 참조(CID)

---

## 🗃️ ERD
![See You Letter ERD](<See you Letter ERD.png>)

---

## 🖼️ 서비스 화면
> (스크린샷 삽입 예정)

---

## 🎞️ 발표 자료 (PPT)
- Canva 링크: https://www.canva.com/design/DAGz2Rc7FMY/x96ehzGOsQSt35aA7T2VPQ/view?utm_content=DAGz2Rc7FMY&utm_campaign=designshare&utm_medium=link2&utm_source=uniquelinks&utlId=hf72ab5af50

---