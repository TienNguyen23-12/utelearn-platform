# Stage 1: Build file .jar với Maven và Java 21
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy toàn bộ mã nguồn vào container
COPY . .

# Cấp quyền cho mvnw và chạy toàn bộ unit test trước (Bước Test trong quy trình CI/CD)
RUN chmod +x mvnw && ./mvnw test

# Sau khi test thành công, đóng gói ứng dụng thành file .jar
RUN ./mvnw package -DskipTests

# Stage 2: Môi trường chạy ứng dụng nhẹ gọn với JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy file jar đã build thành công từ Stage 1
COPY --from=builder /app/target/*.jar app.jar

# Render mặc định dùng cổng 10000
ENV SERVER_PORT=10000
EXPOSE 10000

# Khởi chạy Spring Boot với giới hạn RAM tối ưu cho gói Free của Render
CMD ["java", "-Xmx384m", "-Xms256m", "-jar", "app.jar"]
