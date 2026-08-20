# 산재근로자 공공데이터·판례 AI 안내 서비스

근로복지공단이 제공하는 공공데이터를 활용해 산재 지정 의료기관과 재활기관을 조회하고, 판례를 검색하거나 자연어로 질문할 수 있도록 구현한 개인 학습 프로젝트입니다.

> 이 프로젝트는 근로복지공단의 공식 서비스가 아니며, 학습·포트폴리오 목적으로 제작했습니다. 챗봇 답변은 법률·의학적 판단을 대신하지 않습니다.

## 시연 영상

### 1. 근로복지공단 공공데이터 통합 조회

근로복지공단 Open API를 연계하여 산재 지정 의료기관, 약국, 재활기관 등의 정보를 조회합니다.  
외부 XML 응답은 백엔드에서 가공하여 프론트엔드에 일관된 JSON 형식으로 제공합니다.

<br>

<img
  src="docs/images/demo/workcare-public-data.gif"
  alt="근로복지공단 공공데이터 통합 조회 시연"
  width="900"
/>

<br><br>

### 2. 유사 산재 판례 검색 챗봇

사용자가 사고 내용을 자연어로 입력하면 문장을 벡터로 변환하고, Oracle AI Vector Search를 이용하여 의미적으로 유사한 산재 판례를 검색합니다.

<br>

<img
  src="docs/images/demo/workcare-precedent-chatbot.gif"
  alt="산재 판례 검색 챗봇 시연"
  width="900"
/>

## 핵심 기능

- 산재 지정 의료기관·약국·재활인증 의료기관 조회
- 사회복귀 지원기관 조회
- 산재 판례 조건 검색 및 상세 조회
- 공공데이터 판례 수집과 Oracle `MERGE` 기반 중복 방지
- Ollama `bge-m3` 임베딩과 Oracle AI Vector Search를 이용한 유사 판례 검색
- 검색 근거를 함께 제공하는 판례 안내형 챗봇
- 공통 오류 응답과 외부 API 응답 검증

## 기술 구성

| 구분 | 기술 |
| --- | --- |
| Backend | Java 17, eGovFrame Boot 5.0, Spring Boot 3.5, MyBatis |
| Frontend | Vue 3, Vite, Vue Router |
| Database | Oracle AI Database 26ai Free, Oracle VECTOR |
| AI | Ollama, `bge-m3` 1024차원 임베딩 |
| External API | 공공데이터포털 근로복지공단 Open API |
| Development | Eclipse eGovFrame IDE, Docker Desktop, DBeaver |

## 시스템 구조

```mermaid
flowchart LR
    USER["사용자"] --> VUE["Vue 3 + Vite<br/>사용자 화면"]
    VUE -->|"REST API / JSON"| CONTROLLER["eGovFrame Boot<br/>REST Controller"]
    CONTROLLER --> SERVICE["Service<br/>업무 규칙·응답 변환"]

    SERVICE --> PUBLIC_CLIENT["Public Data Client<br/>외부 응답 검증·변환"]
    PUBLIC_CLIENT <-->|"HTTPS / XML"| PUBLIC_API["공공데이터포털<br/>근로복지공단 Open API"]

    SERVICE --> MAPPER["MyBatis Mapper"]
    MAPPER --> ORACLE[("Oracle AI Database 26ai<br/>판례·VECTOR")]

    SERVICE --> OLLAMA_CLIENT["Ollama Client"]
    OLLAMA_CLIENT <-->|"1024차원 임베딩"| OLLAMA["Ollama<br/>bge-m3"]
```

- 사용자 화면은 Vite 개발 프록시를 통해 eGovFrame Boot REST API와 통신합니다.
- 공공데이터 조회는 전용 Client가 근로복지공단 Open API의 XML 응답과 결과 코드를 검증한 후 내부 DTO로 변환합니다.
- 판례 수집 데이터와 임베딩 벡터는 MyBatis를 통해 Oracle에 저장하며, 질의 문장을 `bge-m3`로 임베딩한 뒤 Oracle Vector Search로 유사 판례를 검색합니다.

저장소 구성:

```text
workcare-ai-guide/
├── workcare-guide-api/   # eGovFrame Boot REST API
└── workcare-guide-ui/    # Vue 사용자 화면
```

## 보안 설정

인증키와 비밀번호는 소스코드에 저장하지 않습니다. 실행 전에 다음 환경변수를 설정해야 합니다.

```bash
export WORKCARE_DB_PASSWORD="본인의 Oracle 비밀번호"
export DATA_GO_KR_SERVICE_KEY="공공데이터포털 일반 인증키(Decoding)"
```

필요하면 다음 값도 실행환경에 맞게 변경할 수 있습니다.

```bash
export WORKCARE_DB_URL="jdbc:oracle:thin:@//localhost:1521/FREEPDB1"
export WORKCARE_DB_USERNAME="WORKCARE_APP"
export OLLAMA_BASE_URL="http://localhost:11434"
export OLLAMA_EMBEDDING_MODEL="bge-m3"
```

## 실행 방법

### 1. 사전 준비

- Java 17
- Oracle AI Database 26ai Free
- Node.js 24 이상
- Ollama와 `bge-m3` 모델
- 공공데이터포털 활용신청 및 인증키

```bash
ollama pull bge-m3
```

### 2. Backend

```bash
cd workcare-guide-api
mvn spring-boot:run
```

정상 실행 확인:

```bash
curl -H "Accept: application/json" http://localhost:8080/api/health
```

### 3. Frontend

```bash
cd workcare-guide-ui
npm install
npm run dev
```

브라우저에서 `http://localhost:5173`으로 접속합니다. 개발 중 `/api` 요청은 Vite 프록시를 통해 `http://localhost:8080`으로 전달됩니다.

## 구현 과정에서 해결한 문제

- XML·JSON이 혼재한 공공데이터 응답을 DTO 계층으로 분리해 안정적으로 역직렬화
- 공공데이터 재수집 시 중복 저장을 막기 위해 사건번호·법원 기준 `MERGE` 적용
- 원문 해시가 변경된 판례만 갱신해 불필요한 재임베딩 방지
- 판례 원문을 청크로 나누고 1024차원 벡터로 저장해 의미 기반 유사도 검색 구현
- Oracle Docker 컨테이너를 영속 볼륨으로 전환하고 기동 파일과 데이터를 복구
- 비밀번호와 공공데이터 인증키를 환경변수로 분리해 Git 노출 방지

## 향후 개선

- DB 초기화 스크립트와 자동화 테스트 보강
- 검색 품질 평가 데이터셋과 정량 지표 구축
- 운영용 인증·인가 및 요청량 제한 적용
- 출처 링크와 면책 문구를 포함한 답변 검증 강화

## 학습 성과

- 전자정부프레임워크 Boot 환경에서 Controller–Service–Client·Mapper 계층을 분리하고 각 계층의 책임을 이해했습니다.
- 외부 공공데이터의 HTTP 성공 여부만 확인하는 것으로는 부족하며, 응답 결과 코드와 XML 구조까지 검증해야 안정적으로 연계할 수 있다는 점을 학습했습니다.
- 외부 API DTO와 사용자에게 제공하는 응답 DTO를 분리하여 외부 데이터 형식의 변경이 화면에 직접 전파되지 않도록 구성했습니다.
- Oracle `MERGE`와 원문 해시를 활용해 반복 수집의 멱등성을 확보하고 변경된 판례만 다시 임베딩하는 처리 방식을 적용했습니다.
- 판례 원문을 검색 가능한 단위로 청크화하고 `bge-m3` 임베딩과 Oracle Vector Search를 연결하며 벡터 기반 의미 검색 흐름을 경험했습니다.
- 검색 결과와 근거 판례를 함께 제공하도록 구성하면서 AI 안내 기능에서는 정확성뿐 아니라 출처 제시와 책임 범위 안내도 중요하다는 점을 배웠습니다.
- Docker 컨테이너의 쓰기 계층과 영속 볼륨의 차이를 이해하고 Oracle 데이터와 기동 파일을 복구하며 데이터 영속성을 점검했습니다.
- DB 비밀번호와 공공데이터 인증키를 환경변수로 외부화하여 저장소에 비밀정보가 포함되지 않도록 관리했습니다.
- Vue와 REST API를 분리하고 Vite 프록시를 적용하면서 프런트엔드와 백엔드의 독립적인 개발·실행 구조를 익혔습니다.
