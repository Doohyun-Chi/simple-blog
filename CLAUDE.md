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
- 한 branch는 한 feature를 담는다.
- branch를 생성할 때는 현재 working branch를 기준으로 삼는다.
- 하나의 기능에 대한 스펙, BE, FE 변경은 하나의 커밋에 함께 담을 수 있다.
- branch가 main에 머지되면 별도의 지시가 없다면 해당 branch는 삭제한다.

### Coding 기본 룰
- 한 라인은 150자 이하가 되도록 한다.
- 한 메소드의 Line 수는 최대 20줄이 넘지 않아야 한다.
- 메소드의 이름은 get 혹은 set으로 시작하지 않는다.
- 조회를 위한 이름은 명사 혹은 복수 항목을 조회하는 경우에는 `listOf` 를 복수형 명사의 prefix로 사용해 명명한다.
- 행동을 수행하는 경우 Action에 부합하는 동사를 사용해 명명한다.

### BE 구조
1. MVC 구조를 따라 Controller/Service/Component/Repository 계층을 따른다.
2. package 계정은 Application 하위에 계층을 수평 구조로 배치한다.
3. JPA로 작업하는 경우 OneToOne 혹은 OneToMany 규칙을 적용하고, 이외 복잡도가 있는 경우에는 QueryDSL을 적용하되 반드시 승인을 얻는다.

### FE 구조
1. Compositor, Component, Model에 따른 구조와 패키지 구조를 갖는다.
2. CSS를 포함(관련된) 부분은 실제 코드에 포함되지 않도록 한다.

### 리팩터링 컨벤션

공통
- 함수의 cyclomatic complexity는 10 이하로 유지한다. BE는 `./gradlew coverage`의 JaCoCo COMPLEXITY로 확인하고, FE는 `npm run lint`가
  10을 넘는 함수를 경고한다 (`fe/.oxlintrc.json`의 `complexity`).
- 리팩터링은 동작을 바꾸지 않으며 기능 변경과 같은 커밋에 섞지 않는다. 테스트가 없는 FE 로직은 바꾸기 전과 후의 결과를 같은 입력으로 비교해
  확인한다 (예: `routeOf`를 old/new로 나란히 실행하는 scratchpad 스크립트).
- 상태, 타입처럼 정해진 도메인 값은 문자열 리터럴로 비교하지 않는다. BE는 enum, FE는 const 객체로 정의하고 그것으로 비교한다.
- 실제로 저장될 수 없는 값을 방어하는 분기와 테스트는 두지 않는다. 다룰 값의 범위는 enum이 정한다.

BE
- 도메인 코드 값은 `code()`(`@JsonValue`)와 `of(code)`를 가진 enum으로 정의하고, `@Converter(autoApply = true)`인 `AttributeConverter`로
  기존 코드 문자열 그대로 저장한다. 예: `PostType`, `PostStatus`. API의 JSON 값은 code 그대로 유지된다.
- 값에 따라 달라지는 규칙은 enum의 필드나 메소드로 둔다 (예: `PostStatus.settable()`). 서비스에 `Set<String>` 같은 허용 목록을 두지 않는다.
- JPQL에서는 enum 상수를 fully-qualified name으로 쓴다 (예: `com.simpleblog.entity.PostStatus.PUBLISH`).
- DB에 CHECK 제약을 두지 않는다. 테스트의 H2도 `NoColumnCheckH2Dialect`로 Hibernate가 enum 컬럼에 CHECK를 만들지 못하게 한다.

FE
- `erasableSyntaxOnly`이므로 TS `enum` 대신 `as const` 객체와 같은 이름의 union type을 쓴다 (예: `model/Post.ts`의 `PostType`, `PostStatus`).
- 종류별로 갈라지는 `if` 체인은 순서 있는 matcher 목록으로 바꾼다 (예: `route.ts`의 `ROUTE_MATCHERS`, 먼저 맞는 것이 이긴다).
- 종류마다 다른 화면을 고르는 긴 `switch`는 mapped type으로 모든 종류를 강제하는 lookup table로 바꾼다 (예: `MainPage.tsx`의 `ROUTE_VIEWS`).
- 큰 컴포넌트는 나눈다. 상태와 행동은 `model/use*.ts` hook으로 (예: `usePostDraft`), 마크업은 역할별 component 파일로 옮긴다
  (예: `PostEditorActions`, `PostEditorField`, `PostEditorNotes`). 남은 컴포넌트는 배치만 담당한다.
- 같은 조건을 여러 속성에서 반복하면 그 조건을 가진 작은 컴포넌트로 묶는다 (예: `PublishButton`).
- hook은 `window.confirm` 같은 UI를 직접 부르지 않고 콜백으로 받는다 (예: `remove(confirmed)`). 화면 문구는 component에 두고, hook은
  상태만 돌려준다 (예: `RemoveFailure`를 돌려주고 문구는 `PostEditorNotes`가 정한다).
- 화면 규칙을 인라인 조건으로 쓰지 않고 이름 있는 helper로 둔다 (예: `isEditorRoute`, `showsAsideAd`).
- `.tsx`에서는 컴포넌트만 export한다 (`react/only-export-components`). 공유할 로직은 `model/`의 `.ts`에 둔다.


## API contract

`be/apispec.yaml`이 BE와 FE 사이의 유일한 기준이다.

API는 HTTP Method에 따른 용도를 준용한다.
- GET: 목록을 포함한 조회
- POST: 신규 생성
- PUT: 기존 데이터 갱신
- DELETE: 삭제

API의 endpoint는 다음의 분류에 따라 sub URI를 갖는다.
-. `/public/api/v{N}/{URI}` FE와의 통신용 API
-. `/internal/api/v{N}/{URI}` 내부 확인 혹은 어드민용 API
-. `/mcp/v{N}/{URI}` MCP 용도

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
./gradlew bootRun --args='--spring.profiles.active=local'
./gradlew test
./gradlew coverage   # tests with JaCoCo; report at build/reports/jacoco/test/html/index.html
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
