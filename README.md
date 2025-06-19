# YH_MSA_COMMUNICATIONS 🔧
> 마이크로서비스 아키텍처(MSA) 통신 패턴 실습을 위한 개인 프로젝트

## 1. 프로젝트 개요 (Overview)

이 프로젝트는 마이크로서비스 아키텍처(MSA)에서 사용되는 다양한 통신 패턴을 직접 구현하고 학습하기 위해 시작되었습니다.

오라일리의 **클라우드 네이티브 애플리케이션 디자인 패턴**에서 소개된 여러 동기/비동기 통신 기술들을 이론으로만 접하는 것을 넘어, 실제 코드로 경험하고 그 특징을 체감하는 것을 목표로 합니다.

현재 **REST**와 **gRPC**를 이용한 동기 통신 패턴 구현이 완료되었으며, 앞으로 **RabbitMQ(AMQP)**, **Kafka** 등을 이용한 비동기 패턴을 추가해나갈 예정입니다.

## 2. MSA 주요 통신 패턴 정리

프로젝트의 이론적 배경이 되는 MSA 통신 패턴을 아래와 같이 정리합니다.

| 구분 | 패턴 종류 | 주요 기술 | 핵심 개념 (서비스/스키마 레지스트리) |
| :--- | :--- | :--- | :--- |
| **동기 (Synchronous)** | 요청-응답 (Request-Response)<br/>RPC (Remote Procedure Call) | REST, gRPC,<br/>GraphQL, WebSocket | "어디에 보낼지"를 결정하기 위해 <br/> **서비스 레지스트리(Service Registry)** 를 사용 |
| **비동기 (Asynchronous)** | 단일/다중 수신자 (Single/Multiple Receiver)<br/>비동기 요청-응답 | AMQP (RabbitMQ),<br/>Kafka, NATS | "무엇을/어떻게 보낼지"를 명세하기 위해 <br/> **스키마 레지스트리(Schema Registry)** 를 사용 |

-   **동기 통신**: 클라이언트가 서버에 요청을 보내고, 응답이 올 때까지 기다리는 방식입니다. 구현이 직관적이지만, 서비스 간의 결합도가 높아지고 장애가 전파될 수 있습니다.
-   **비동기 통신**: 클라이언트가 요청(메시지)을 보내고 바로 다른 작업을 수행할 수 있는 방식입니다. 서비스 간 결합도를 낮추고 탄력성을 높일 수 있지만, 설계가 복잡하고 최종 응답을 보장하기 위한 추가적인 구현이 필요합니다.

## 3. 구현된 기능

-   [x] **RESTful API**를 이용한 동기 요청-응답 통신
-   [x] **gRPC**를 이용한 4가지 방식의 RPC 통신
    -   [x] Unary RPC
    -   [x] Server Streaming RPC
    -   [x] Client Streaming RPC
    -   [x] Bidirectional Streaming RPC
-   [x] **Docker**를 이용한 각 서비스의 컨테이너화 (`Dockerfile`)
-   [x] **Docker Compose**를 이용한 다중 컨테이너 관리
-   [x] **AWS EC2**를 이용한 클라우드 환경 배포 및 테스트

## 4. 프로젝트 사용법 (How to Use)

이 프로젝트를 테스트하는 방법은 두 가지입니다.

### 4.1. 원격 서버(AWS EC2)로 테스트하는 경우 (추천)

미리 배포된 서버를 대상으로 즉시 테스트할 수 있는 방법입니다. 서버는 24시간 실행 중입니다.

1.  **프로젝트 클론**
    ```bash
    git clone [https://github.com/your-username/YH_MSA_COMMUNICATIONS.git](https://github.com/your-username/YH_MSA_COMMUNICATIONS.git)
    cd YH_MSA_COMMUNICATIONS
    ```

2.  **클라이언트 설정 확인**
    `client/src/main/resources/application.yml` 파일의 호스트 주소가 아래와 같이 설정되어 있는지 확인합니다.
    ```yaml
    grpc:
      server:
        host: 43.201.23.166
        port: 9090

    Rest:
      server:
        host: 43.201.23.166
        port: 8080
    ```

3.  **클라이언트 애플리케이션 실행**
    프로젝트 루트 디렉토리에서 아래의 Gradle 명령어를 실행합니다.
    ```bash
    ./gradlew :client:bootRun
    ```
    로컬 터미널에 AWS EC2 서버와 통신하는 결과가 출력됩니다.

### 4.2. 로컬 환경에서 직접 서버를 실행하여 테스트하는 경우

로컬 PC에서 직접 서버들을 실행하여 테스트하는 방법입니다.

1.  **프로젝트 클론**
    ```bash
    git clone [https://github.com/your-username/YH_MSA_COMMUNICATIONS.git](https://github.com/your-username/YH_MSA_COMMUNICATIONS.git)
    cd YH_MSA_COMMUNICATIONS
    ```

2.  **클라이언트 설정 변경**
    `client/src/main/resources/application.yml` 파일의 `host` 주소를 모두 `localhost`로 변경합니다.
    ```yaml
    grpc:
      server:
        host: localhost # 변경
        port: 9090

    Rest:
      server:
        host: localhost # 변경
        port: 8080
    ```

3.  **각 서버 애플리케이션 실행**
    **각각 다른 터미널 창**을 열고, 아래의 Gradle 명령어를 실행하여 서버들을 띄웁니다.
    
    * **터미널 1 (REST 서버):**
        ```bash
        ./gradlew :rest-service:bootRun
        ```
    * **터미널 2 (gRPC 서버):**
        ```bash
        ./gradlew :grpc-service:bootRun
        ```

4.  **클라이언트 애플리케이션 실행**
    **또 다른 새 터미널 창**을 열고, 아래 명령어로 클라이언트를 실행합니다.
    ```bash
    ./gradlew :client:bootRun
    ```
    로컬 PC에서 실행 중인 서버들과 통신하는 결과가 출력됩니다.

## 5. 프로젝트 구조
YH_MSA_COMMUNICATIONS/
├── client/          # REST, gRPC 클라이언트 구현 및 테스트 모듈
├── common/          # 공통 DTO, gRPC .proto 파일 및 생성된 코드 모듈
├── grpc-service/    # gRPC 서버 구현 모듈
├── rest-service/    # RESTful API 서버 구현 모듈
├── build.gradle     # 루트 프로젝트 빌드 스크립트
├── settings.gradle  # 멀티 모듈 설정
└── ...
