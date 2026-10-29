FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src
RUN chmod +x mvnw && ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:21-jre-noble

ARG YT_DLP_VERSION=2026.10.26
ARG YT_DLP_SHA256=e51e69e89489f199530f13d32a4342da3ddb2cf34842f364603a066db05afbf3
ARG FFMPEG_PACKAGE_VERSION=7:6.1.1-3ubuntu5

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       "ffmpeg=${FFMPEG_PACKAGE_VERSION}" \
       python3 \
       curl \
       ca-certificates \
    && curl -fsSL --retry 3 \
       "https://github.com/yt-dlp/yt-dlp/releases/download/${YT_DLP_VERSION}/yt-dlp" \
       -o /usr/local/bin/yt-dlp \
    && echo "${YT_DLP_SHA256}  /usr/local/bin/yt-dlp" | sha256sum --check - \
    && chmod 0755 /usr/local/bin/yt-dlp \
    && ffmpeg -version >/dev/null \
    && ffprobe -version >/dev/null \
    && /usr/local/bin/yt-dlp --version >/dev/null \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app app \
    && mkdir -p /app /tmp/media-converter \
    && chown -R app:app /app /tmp/media-converter

WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/conversor-0.0.1-SNAPSHOT.jar app.jar

USER 10001:10001
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD python3 -c "from urllib.request import urlopen; urlopen('http://127.0.0.1:8080/actuator/health', timeout=2).read()" || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
