# AnaphygonMicros - Movie/Series Streaming Platform

🚧 **Project sedang dalam tahap awal development** 🚧

Platform streaming movie/series berbasis microservices menggunakan Kotlin dan Micronaut framework.

## 🎯 Project Goals

Membangun platform streaming modern dengan arsitektur microservices yang meliputi:
- User management & authentication
- Content catalog (movies/series)
- Video streaming service
- Recommendation engine
- Review & rating system

## 🛠️ Tech Stack

- **Framework**: Micronaut 4.8.2
- **Language**: Kotlin
- **Build Tool**: Gradle Kotlin DSL
- **Database**: MySQL + Flyway migrations
- **Caching**: Redis
- **Messaging**: Apache Kafka
- **Authentication**: JWT + OAuth2
- **Testing**: JUnit + Testcontainers
- **Documentation**: OpenAPI/Swagger
- **Monitoring**: Prometheus + Jaeger
- **Deployment**: Docker + GraalVM native compilation

## 🏃‍♂️ Getting Started

### Prerequisites
- JDK 21+
- Docker & Docker Compose

### Quick Start
```bash
# Clone repository
git clone <repository-url>
cd anaphygon-micros

# Start dependencies (MySQL, Redis, Kafka)
docker-compose up -d

# Build and run
./gradlew run