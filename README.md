# DaySpark

반복 목표(도전), 완료 기록, 포모도로 집중 타이머, 달력·통계, 공부 복습 플랜을 제공하는
단일 사용자용 Spring Boot 웹앱입니다. 소프트웨어공학 수업 팀 프로젝트입니다.

## 개발 환경

| 항목 | 기준 |
|---|---|
| Java | **17** (`pom.xml`의 `java.version`; JDK 17 이상에서 실행 가능) |
| 빌드 | Maven Wrapper (`mvnw`, `mvnw.cmd`) — Maven을 따로 설치하지 않아도 됨 |
| 프레임워크 | Spring Boot 3.5 (Web, Thymeleaf, Data JPA, Validation) |
| DB | H2 파일 DB (`./dayspark_v2.mv.db`) |
| 테스트 | JUnit 5, Spring Boot Test(MockMvc) |

## Maven Wrapper

`mvnw`/`mvnw.cmd`가 저장소에 없다면 **한 번만** 생성해서 커밋합니다 (Maven이 설치된 환경 필요).

```bash
# WSL / Linux
sudo apt install -y maven            # 이미 있으면 생략
mvn -N wrapper:wrapper -Dmaven=3.9.11 -Dtype=only-script
chmod +x mvnw
git add mvnw mvnw.cmd .mvn && git commit -m "build: add Maven Wrapper"
```

이후에는 Maven 설치 여부와 상관없이 아래 명령만 사용합니다.

## 테스트

```bash
./mvnw test      # 단위 + 통합 테스트
./mvnw verify    # 전체 빌드 + 테스트
```

Windows CMD/PowerShell에서는 `mvnw.cmd test`, `mvnw.cmd verify`.
