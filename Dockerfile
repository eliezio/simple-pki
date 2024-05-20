FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace
COPY . .
ARG JAR_FILE=build/libs/*.jar
ARG GRADLE_USER_HOME=/tmp/build_cache/gradle
RUN --mount=type=cache,target=/tmp/build_cache/gradle \
    set -ex \
    && chmod +x gradlew \
    && ./gradlew build -i \
        -x test -x check -x asciidoctor \
    && rm -f build/libs/*-plain.jar \
    && java -Djarmode=layertools -jar $JAR_FILE extract

FROM eclipse-temurin:17-jre
USER 999:0
WORKDIR /app
COPY --from=builder /workspace/dependencies/ ./
COPY --from=builder /workspace/spring-boot-loader/ ./
COPY --from=builder /workspace/snapshot-dependencies/ ./
COPY --from=builder /workspace/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
