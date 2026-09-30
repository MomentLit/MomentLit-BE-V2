FROM amazoncorretto:21-alpine AS builder

WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY common ./common
COPY modules ./modules
COPY src ./src

RUN ./gradlew bootJar -x test --no-daemon

# OpenCV(bytedeco) 네이티브 라이브러리가 glibc 기반이라 musl(alpine) 런타임에서는 로드되지 않는다.
FROM eclipse-temurin:21-jre

# 리눅스용 OpenCV의 Stitcher가 highgui(GTK2)에 링크되어 있어, 화면을 쓰지 않아도 이 라이브러리가 있어야 로드된다.
RUN apt-get update \
    && apt-get install -y --no-install-recommends libgtk2.0-0 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
