# Immigration Ops Backend

1차 내부 OCR 운영 시스템용 Spring Boot 백엔드 프로젝트입니다.

## 범위

- 내부 사용자 전용 업로드 API
- OCR 처리 파이프라인 오케스트레이션
- 문서 분류
- 여권 MRZ 추출 연결 지점
- 저신뢰 문서 검수 분기
- Basic Auth 기반 Spring Security

## 패키지 구조

- `document`: 업로드 생명주기와 처리 오케스트레이션
- `ocr`: OCR 추상화와 임시 스텁 어댑터
- `classification`: 키워드 기반 문서 분류
- `passport`: MRZ 추출
- `template`: 문서별 필드 템플릿 레지스트리
- `review`: 저신뢰 검수 정책
- `storage`: 로컬 파일 저장
- `security`: 분리된 Spring Security 설정

## 참고

- `StubOcrEngine`은 자리만 잡아둔 임시 구현입니다. 실제 OCR 공급자 어댑터로 교체해야 합니다.
- OpenAI OCR 어댑터가 추가되어 있습니다. `APP_OCR_PROVIDER=openai` 와 `OPENAI_API_KEY`를 설정하면 이미지/PDF를 Responses API로 보내 전체 텍스트를 추출합니다.
- Google Vision OCR 어댑터가 추가되어 있습니다. `APP_OCR_PROVIDER=google-vision` 으로 바꾸고 자격 증명 경로를 설정하면 이미지 OCR과 PDF/TIFF 파일 OCR을 사용할 수 있습니다.
- `INFRA_ARCHITECTURE_PLAN.md`에 1차 인프라 방향을 정리해두었습니다.
- 저장 경로 기본값은 이 프로젝트 기준 `data/uploads` 입니다.
- 루트의 `application.yaml` 파일은 환경 변수 이름만 정리한 참고용 템플릿입니다.

## 실행

1. Gradle을 설치하거나 Gradle Wrapper를 추가합니다.
2. 필요하면 아래 환경 변수를 설정합니다.
   - `APP_SECURITY_USERNAME`
   - `APP_SECURITY_PASSWORD`
   - `APP_OCR_PROVIDER`
   - `OPENAI_API_KEY`
   - `OPENAI_OCR_MODEL`
   - `GOOGLE_VISION_CREDENTIALS_PATH`
   - `GOOGLE_VISION_PARENT`
   - `GOOGLE_VISION_MAX_PAGES`
3. 이 디렉터리에서 `gradle bootRun`으로 실행합니다.
