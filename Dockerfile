# Bước 1: Build file .jar bằng Maven và Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
ENV MAVEN_OPTS="-Xmx256m"
RUN mvn clean package -Dmaven.test.skip=true --no-transfer-progress

# Bước 2: Chạy ứng dụng bằng môi trường Java JRE
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Asia/Ho_Chi_Minh
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-Duser.timezone=Asia/Ho_Chi_Minh", "-Xmx300m", "-jar", "app.jar"]