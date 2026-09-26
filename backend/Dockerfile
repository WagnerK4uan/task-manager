FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

COPY --from=build /build/target/task-manager-*.jar app.jar

RUN useradd --uid 1001 --no-create-home --shell /usr/sbin/nologin taskmanager
USER taskmanager

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
