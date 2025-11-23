FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 創建用戶和組
RUN addgroup -S spring && adduser -S spring -G spring

# 創建日誌目錄並設置權限
RUN mkdir -p /app/logs && chown -R spring:spring /app/logs

# 複製 jar 文件
COPY --from=build /app/target/*.jar app.jar

# 切換到非 root 用戶
USER spring:spring

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
