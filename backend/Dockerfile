# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package \
    && mv target/claims-*.jar /build/app.jar

FROM eclipse-temurin:17-jre-jammy AS runtime

RUN apt-get update \
    && apt-get install -y --no-install-recommends fontconfig fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

RUN useradd --system --create-home --uid 10001 claimguard
USER claimguard
WORKDIR /app

COPY --from=build --chown=claimguard:claimguard /build/app.jar /app/app.jar

# Sized for a 512 MB instance: the heap ceiling has to leave room for metaspace,
# code cache, thread stacks and PDFBox/S3 direct buffers, or the container OOM
# killer takes the process (exit 137) before the JVM ever sees an OutOfMemoryError.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=45 \
  -XX:MaxMetaspaceSize=160m \
  -XX:ReservedCodeCacheSize=48m \
  -XX:MaxDirectMemorySize=32m \
  -Xss512k \
  -XX:+UseSerialGC \
  -XX:+ExitOnOutOfMemoryError \
  -Djava.awt.headless=true \
  -Dfile.encoding=UTF-8"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
