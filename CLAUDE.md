# simple-blog

블로그 서비스. 백엔드와 프론트엔드를 하나의 에이전트가 함께 개발한다.

## 구조

| 경로 | 역할 | 스택 |
| --- | --- | --- |
| `be/` | 백엔드 | Spring Boot 4.1, Java 25, Gradle Wrapper |
| `fe/` | 프론트엔드 | React 19, TypeScript, Vite, npm |
| `be/apispec.yaml` | API contract | OpenAPI 3.1 |

이 루트 디렉토리가 단일 git 저장소이고, `be/`와 `fe/`는 그 안의 일반 디렉토리다.

## 작업 방식

- 단일 에이전트로 진행한다. BE와 FE 작업을 서브에이전트로 나누지 말고 직접 처리한다.
- 작업은 항상 이 루트 디렉토리에서 시작하고, 필요한 쪽의 코드를 직접 수정한다.
- git 명령은 루트에서 실행한다.
- 하나의 기능에 대한 스펙, BE, FE 변경은 하나의 커밋에 함께 담을 수 있다.

## API contract

`be/apispec.yaml`이 BE와 FE 사이의 유일한 기준이다.

API를 추가하거나 변경할 때는 다음 순서를 따른다.

1. `be/apispec.yaml`을 먼저 수정한다.
2. 스펙에 맞춰 BE를 구현하고 테스트한다.
3. 스펙에 맞춰 FE를 구현하고 테스트한다.

- 스펙에 없는 엔드포인트나 필드를 BE에 구현하거나 FE에서 사용하지 않는다.
- 구현 중 스펙이 잘못되었음을 발견하면 코드를 먼저 맞추지 말고 스펙부터 고친다.
- 기존 필드의 삭제, 이름 변경, 타입 변경은 호환성을 깨는 변경이므로 진행 전에 사용자에게 확인한다.

## 명령어

BE는 JDK 25가 필요하다. Homebrew의 `openjdk@25`는 PATH에 자동으로 잡히지 않으므로 `JAVA_HOME`이 없으면 `JAVA_HOME=/opt/homebrew/opt/openjdk@25`를 붙여 실행한다.

```sh
# BE (be/ 에서 실행, http://localhost:8080)
./gradlew bootRun
./gradlew test
./gradlew build

# FE (fe/ 에서 실행, http://localhost:5173)
npm install
npm run dev
npm run lint
npm run build
```

FE에는 아직 테스트 러너가 없다.

## 완료 기준

변경 범위에 해당하는 빌드와 테스트가 통과해야 완료로 본다. API를 변경했다면 `be/apispec.yaml`, BE, FE가 서로 일치하는지도 확인한다.
