# MORE - Fintech Application

핀테크 서비스를 위한 Flutter 모바일 애플리케이션입니다.

---

## 목차

1. [환경 정보](#환경-정보)
2. [시작하기](#시작하기)
3. [아키텍처 개요](#아키텍처-개요)
4. [프로젝트 구조](#프로젝트-구조)
5. [각 계층 상세 설명](#각-계층-상세-설명)
6. [사용 라이브러리](#사용-라이브러리)
7. [코드 생성](#코드-생성)
8. [데이터 흐름](#데이터-흐름)
9. [새 기능 추가 가이드](#새-기능-추가-가이드)
10. [컨벤션](#컨벤션)

---

## 환경 정보

| 항목 | 버전 |
|------|------|
| Flutter | 3.38.9 (stable) |
| Dart | 3.10.8 |
| SDK 제약 | `^3.10.8` |

---

## 시작하기

### 사전 준비

1. [Flutter SDK](https://docs.flutter.dev/get-started/install) 설치
2. Windows의 경우 **개발자 모드** 활성화 권장 (`설정 > 개발자용`)

### 프로젝트 실행

```bash
# 의존성 설치
flutter pub get

# 코드 생성 (freezed, json_serializable 등)
dart run build_runner build --delete-conflicting-outputs

# 앱 실행
flutter run
```

### 코드 생성 감시 모드 (개발 중 권장)

```bash
dart run build_runner watch --delete-conflicting-outputs
```

---

## 아키텍처 개요

본 프로젝트는 **Clean Architecture** + **MVVM 패턴** + **Riverpod 상태관리**를 기반으로 설계되었습니다.

### 왜 Clean Architecture인가?

Clean Architecture는 코드를 **관심사별로 계층을 분리**하여 유지보수성과 테스트 용이성을 높이는 아키텍처입니다.

```
┌─────────────────────────────────────────────┐
│            Presentation (UI)                │  ← 사용자가 보는 화면
│         (Pages, Widgets, ViewModels)        │
├─────────────────────────────────────────────┤
│              Domain (비즈니스)                │  ← 핵심 비즈니스 로직 (순수 Dart)
│        (Entities, UseCases, Repos I/F)      │
├─────────────────────────────────────────────┤
│              Data (데이터)                   │  ← 외부 데이터 처리
│     (Models, DataSources, Repos Impl)       │
├─────────────────────────────────────────────┤
│              Core (공통)                     │  ← 전 계층에서 사용하는 공통 코드
│    (Theme, Network, Constants, Utils)       │
└─────────────────────────────────────────────┘
```

### 의존성 규칙

- **Presentation → Domain ← Data** 방향으로 의존합니다.
- **Domain 계층은 다른 계층을 절대 참조하지 않습니다** (순수 Dart 코드).
- Data 계층은 Domain에 정의된 Repository 인터페이스를 구현합니다.

### MVVM 패턴과 Riverpod

| MVVM 구성요소 | 프로젝트 매핑 | 설명 |
|-------------|-------------|------|
| **Model** | `domain/entities/`, `data/models/` | 비즈니스 데이터 |
| **View** | `presentation/pages/`, `presentation/widgets/` | UI 화면, 위젯 |
| **ViewModel** | `presentation/viewmodels/` | Riverpod `StateNotifier`로 상태 관리 |

View는 ViewModel을 구독(watch)하고, ViewModel은 UseCase를 호출하여 데이터를 가져옵니다.

---

## 프로젝트 구조

```
lib/
├── main.dart                              # 앱 진입점 (ProviderScope로 Riverpod 초기화)
│
├── core/                                  # 공통 코드 (전 계층에서 사용)
│   ├── constants/
│   │   └── app_constants.dart             # 앱 전역 상수 (baseUrl, timeout 등)
│   ├── errors/
│   │   ├── exceptions.dart                # 예외 클래스 (ServerException, CacheException 등)
│   │   └── failures.dart                  # 실패 타입 정의 (freezed sealed class)
│   ├── extensions/
│   │   └── context_extensions.dart        # BuildContext 확장 메서드
│   ├── network/
│   │   ├── api_client.dart                # Dio 기반 HTTP 클라이언트 설정
│   │   ├── auth_interceptor.dart          # 인증 토큰 자동 삽입 인터셉터
│   │   └── network_info.dart              # 네트워크 연결 상태 확인
│   ├── theme/
│   │   ├── app_colors.dart                # 컬러 팔레트 정의
│   │   ├── app_text_styles.dart           # 텍스트 스타일 정의
│   │   └── app_theme.dart                 # MaterialApp 테마 설정
│   └── utils/
│       └── currency_formatter.dart        # 통화 포맷팅 유틸리티
│
├── domain/                                # 도메인 계층 (순수 비즈니스 로직)
│   ├── entities/
│   │   └── user.dart                      # User 엔티티 (freezed 불변 객체)
│   ├── repositories/
│   │   └── auth_repository.dart           # AuthRepository 인터페이스 (추상 클래스)
│   └── usecases/
│       └── usecase.dart                   # UseCase 베이스 클래스
│
├── data/                                  # 데이터 계층 (외부 데이터 처리)
│   ├── datasources/
│   │   ├── local/
│   │   │   └── auth_local_datasource.dart # 로컬 토큰 저장/조회
│   │   └── remote/
│   │       └── auth_remote_datasource.dart# API 호출 (로그인, 유저 정보 등)
│   ├── models/
│   │   └── user_model.dart                # UserModel DTO (JSON 직렬화 + Entity 변환)
│   └── repositories/
│       └── auth_repository_impl.dart      # AuthRepository 구현체
│
├── presentation/                          # 프레젠테이션 계층 (UI + 상태관리)
│   ├── navigation/
│   │   └── app_router.dart                # GoRouter 라우팅 설정
│   ├── pages/
│   │   ├── home/
│   │   │   └── home_page.dart             # 홈 화면
│   │   └── splash/
│   │       └── splash_page.dart           # 스플래시 화면
│   ├── viewmodels/
│   │   └── auth_viewmodel.dart            # 인증 상태 관리 (StateNotifier + Riverpod)
│   └── widgets/                           # 재사용 가능한 공통 위젯
│
assets/
├── images/                                # 이미지 리소스
├── icons/                                 # 아이콘 리소스 (SVG 등)
└── animations/                            # Lottie 애니메이션 파일
```

---

## 각 계층 상세 설명

### 1. Core 계층 (`lib/core/`)

전 계층에서 공통으로 사용하는 코드를 모아둔 곳입니다. 특정 비즈니스 로직에 종속되지 않습니다.

| 폴더 | 역할 | 예시 |
|------|------|------|
| `constants/` | 앱 전역 상수 | API URL, 타임아웃 시간 |
| `errors/` | 에러/실패 타입 정의 | `ServerFailure`, `NetworkException` |
| `extensions/` | Dart 확장 메서드 | `context.showSnackBar()` |
| `network/` | HTTP 클라이언트 설정 | Dio 설정, 인증 인터셉터 |
| `theme/` | 앱 테마 및 디자인 토큰 | 컬러, 텍스트 스타일, 테마 |
| `utils/` | 유틸리티 함수 | 통화 포맷팅, 날짜 변환 |

### 2. Domain 계층 (`lib/domain/`)

**앱의 핵심 비즈니스 로직**을 담당합니다. Flutter나 외부 라이브러리에 의존하지 않는 **순수 Dart 코드**로만 작성합니다.

| 폴더 | 역할 | 설명 |
|------|------|------|
| `entities/` | 비즈니스 모델 | 앱 내에서 사용하는 핵심 데이터 구조 (freezed 불변 객체) |
| `repositories/` | 리포지토리 인터페이스 | 데이터 접근 규약을 정의하는 **추상 클래스** |
| `usecases/` | 유스케이스 | 하나의 비즈니스 작업을 수행하는 단위 (예: 로그인, 송금) |

**핵심 원칙**: Domain 계층은 `import 'package:flutter/...'`를 사용하지 않습니다.

```dart
// ✅ 올바른 예 - 순수 Dart
abstract class AuthRepository {
  Future<User> login({required String email, required String password});
}

// ❌ 잘못된 예 - Flutter 의존성
import 'package:flutter/material.dart'; // Domain에서 금지
```

### 3. Data 계층 (`lib/data/`)

외부 데이터(API, 로컬 DB 등)를 처리하고, Domain 계층의 Repository 인터페이스를 **구현**합니다.

| 폴더 | 역할 | 설명 |
|------|------|------|
| `datasources/remote/` | 원격 데이터 소스 | API 서버 호출 (Retrofit/Dio 사용) |
| `datasources/local/` | 로컬 데이터 소스 | SharedPreferences, SecureStorage, Hive |
| `models/` | DTO (Data Transfer Object) | JSON ↔ Dart 변환, `toEntity()` 메서드로 Entity 변환 |
| `repositories/` | 리포지토리 구현체 | Remote + Local DataSource를 조합하여 Domain Repository 구현 |

**Model과 Entity의 차이**:

```dart
// Entity (domain/) - 앱 내부에서 사용하는 순수 데이터
class User {
  final String id;
  final String name;
}

// Model (data/) - API JSON과 매핑되는 DTO
class UserModel {
  final String id;
  final String name;
  @JsonKey(name: 'profile_image_url')  // API 필드명 매핑
  final String? profileImageUrl;

  User toEntity() => User(id: id, name: name);  // Entity로 변환
}
```

### 4. Presentation 계층 (`lib/presentation/`)

사용자가 실제로 보고 상호작용하는 **UI 계층**입니다. MVVM 패턴에서 **View**와 **ViewModel**이 위치합니다.

| 폴더 | 역할 | 설명 |
|------|------|------|
| `pages/` | 화면 (View) | 각 화면을 **기능별 하위 폴더**로 관리 (`home/`, `splash/` 등) |
| `viewmodels/` | 뷰모델 | Riverpod `StateNotifier`로 화면 상태를 관리 |
| `widgets/` | 재사용 위젯 | 여러 화면에서 공유하는 공통 UI 컴포넌트 |
| `navigation/` | 라우팅 | GoRouter 기반 화면 전환 설정 |

**ViewModel 사용 예시**:

```dart
// viewmodels/auth_viewmodel.dart
class AuthViewModel extends StateNotifier<AuthState> {
  AuthViewModel() : super(const AuthState.initial());

  Future<void> login({required String email, required String password}) async {
    state = const AuthState.loading();
    // UseCase 호출 → 상태 업데이트
  }
}

final authViewModelProvider =
    StateNotifierProvider<AuthViewModel, AuthState>((ref) => AuthViewModel());
```

```dart
// pages/login/login_page.dart (View에서 ViewModel 사용)
class LoginPage extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authState = ref.watch(authViewModelProvider); // 상태 구독
    // authState에 따라 UI 렌더링
  }
}
```

---

## 사용 라이브러리

### 상태관리

| 라이브러리 | 용도 |
|-----------|------|
| `flutter_riverpod` | MVVM ViewModel 상태관리 |
| `riverpod_annotation` | Riverpod 코드 생성 어노테이션 |

### 네트워크

| 라이브러리 | 용도 |
|-----------|------|
| `dio` | HTTP 클라이언트 (인터셉터, 타임아웃 등 지원) |
| `retrofit` | 타입 안전한 REST API 클라이언트 자동 생성 |

### 로컬 저장소

| 라이브러리 | 용도 |
|-----------|------|
| `shared_preferences` | 간단한 키-값 저장 (설정값 등) |
| `flutter_secure_storage` | 민감 데이터 암호화 저장 (토큰 등) |
| `hive` / `hive_flutter` | 빠른 NoSQL 로컬 데이터베이스 |

### 라우팅

| 라이브러리 | 용도 |
|-----------|------|
| `go_router` | 선언적 라우팅 (딥링크, 리다이렉트 지원) |

### 의존성 주입 (DI)

| 라이브러리 | 용도 |
|-----------|------|
| `get_it` | 서비스 로케이터 패턴 DI 컨테이너 |
| `injectable` | get_it 코드 자동 생성 |

### 직렬화 / 코드 생성

| 라이브러리 | 용도 |
|-----------|------|
| `freezed` / `freezed_annotation` | 불변 데이터 클래스 + sealed union 자동 생성 |
| `json_serializable` / `json_annotation` | JSON 직렬화/역직렬화 코드 자동 생성 |
| `build_runner` | 코드 생성기 실행 도구 |

### UI / UX

| 라이브러리 | 용도 |
|-----------|------|
| `flutter_screenutil` | 반응형 레이아웃 (다양한 화면 크기 대응) |
| `flutter_svg` | SVG 이미지 렌더링 |
| `cached_network_image` | 네트워크 이미지 캐싱 |
| `shimmer` | 로딩 스켈레톤 UI 효과 |
| `lottie` | Lottie JSON 애니메이션 재생 |

### 핀테크 관련

| 라이브러리 | 용도 |
|-----------|------|
| `fl_chart` | 금융 데이터 차트 (라인, 바, 파이 등) |
| `intl` | 숫자/날짜/통화 국제화 포맷팅 |
| `currency_text_input_formatter` | 금액 입력 시 자동 포맷팅 (1,000,000원) |

### 보안

| 라이브러리 | 용도 |
|-----------|------|
| `local_auth` | 생체 인증 (지문, Face ID) |
| `flutter_secure_storage` | 토큰/비밀번호 암호화 저장 |

### 유틸리티

| 라이브러리 | 용도 |
|-----------|------|
| `logger` | 구조화된 로그 출력 |
| `connectivity_plus` | 네트워크 연결 상태 감지 |
| `permission_handler` | 디바이스 권한 요청/관리 |
| `url_launcher` | 외부 URL/전화/이메일 열기 |

### 테스트 (dev_dependencies)

| 라이브러리 | 용도 |
|-----------|------|
| `mockito` | Mock 객체 생성 (단위 테스트) |
| `build_verify` | 코드 생성 결과 검증 |

---

## 코드 생성

본 프로젝트는 `freezed`, `json_serializable`, `retrofit`, `riverpod_generator`를 사용하여 보일러플레이트 코드를 자동 생성합니다.

### 생성 명령어

```bash
# 일회성 빌드
dart run build_runner build --delete-conflicting-outputs

# 파일 변경 감시 (개발 중 권장)
dart run build_runner watch --delete-conflicting-outputs
```

### 자동 생성 파일 규칙

| 원본 파일 | 생성 파일 | 생성기 |
|----------|----------|--------|
| `user.dart` | `user.freezed.dart` | freezed |
| `user_model.dart` | `user_model.g.dart` | json_serializable |
| `user_model.dart` | `user_model.freezed.dart` | freezed |
| `auth_viewmodel.dart` | `auth_viewmodel.freezed.dart` | freezed |

> 자동 생성된 `*.g.dart`, `*.freezed.dart` 파일은 직접 수정하지 마세요. `analysis_options.yaml`에서 분석 대상에서 제외되어 있습니다.

---

## 데이터 흐름

사용자 액션부터 데이터 반환까지의 전체 흐름입니다.

```
[사용자 터치]
     │
     ▼
┌─────────────┐
│    View      │  Page/Widget (UI)
│  (화면)      │  ref.read(viewModelProvider.notifier).login(...)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  ViewModel   │  StateNotifier (Riverpod)
│  (상태관리)   │  state = Loading → UseCase 호출 → state = Success/Error
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   UseCase    │  단일 비즈니스 작업 수행
│ (비즈니스)    │  repository.login(email, password)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Repository  │  인터페이스: Domain / 구현체: Data
│  (데이터접근)  │  remoteDataSource + localDataSource 조합
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ DataSource   │  Remote: API 호출 (Dio/Retrofit)
│ (데이터원천)   │  Local: Hive/SecureStorage 접근
└──────┬──────┘
       │
       ▼
   [서버/DB]
```

---

## 새 기능 추가 가이드

예시: **"계좌 조회"** 기능을 추가하는 경우

### Step 1. Domain 계층

```
lib/domain/entities/account.dart          ← Entity 정의
lib/domain/repositories/account_repository.dart  ← Repository 인터페이스
lib/domain/usecases/get_accounts.dart     ← UseCase 구현
```

### Step 2. Data 계층

```
lib/data/models/account_model.dart        ← DTO (JSON 직렬화 + toEntity)
lib/data/datasources/remote/account_remote_datasource.dart  ← API 호출
lib/data/repositories/account_repository_impl.dart          ← Repository 구현
```

### Step 3. Presentation 계층

```
lib/presentation/viewmodels/account_viewmodel.dart  ← ViewModel (상태관리)
lib/presentation/pages/account/account_page.dart    ← View (화면)
lib/presentation/navigation/app_router.dart         ← 라우트 추가
```

### Step 4. 코드 생성

```bash
dart run build_runner build --delete-conflicting-outputs
```

---

## 컨벤션

### 파일 네이밍

| 유형 | 네이밍 규칙 | 예시 |
|------|-----------|------|
| Entity | `{이름}.dart` | `user.dart`, `account.dart` |
| Model (DTO) | `{이름}_model.dart` | `user_model.dart` |
| Repository 인터페이스 | `{이름}_repository.dart` | `auth_repository.dart` |
| Repository 구현체 | `{이름}_repository_impl.dart` | `auth_repository_impl.dart` |
| DataSource | `{이름}_{local/remote}_datasource.dart` | `auth_remote_datasource.dart` |
| ViewModel | `{이름}_viewmodel.dart` | `auth_viewmodel.dart` |
| Page | `{이름}_page.dart` | `home_page.dart` |
| Widget | `{이름}_widget.dart` 또는 의미있는 이름 | `balance_card.dart` |

### 페이지 폴더 구조

각 페이지는 **기능별 폴더** 안에 관련 파일을 함께 둡니다.

```
lib/presentation/pages/
├── home/
│   ├── home_page.dart
│   └── widgets/           # 해당 페이지에서만 사용하는 위젯
│       └── balance_card.dart
├── transfer/
│   ├── transfer_page.dart
│   └── widgets/
│       └── amount_input.dart
```

### 상태 클래스 (ViewModel)

freezed sealed class를 활용하여 상태를 명확하게 정의합니다.

```dart
@freezed
sealed class AccountState with _$AccountState {
  const factory AccountState.initial() = AccountInitial;
  const factory AccountState.loading() = AccountLoading;
  const factory AccountState.loaded(List<Account> accounts) = AccountLoaded;
  const factory AccountState.error(String message) = AccountError;
}
```
