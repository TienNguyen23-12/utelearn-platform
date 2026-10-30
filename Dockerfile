FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY . .

RUN chmod +x mvnw && ./mvnw test

RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

ENV SERVER_PORT=10000
EXPOSE 10000

CMD ["java", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-Xss256k", "-Xms128m", "-Xmx256m", "-XX:MaxMetaspaceSize=128m", "-jar", "app.jar"]
