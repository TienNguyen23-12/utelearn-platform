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

CMD ["java", "-Xmx384m", "-Xms256m", "-jar", "app.jar"]
