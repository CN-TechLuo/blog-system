# ============================================================
# 博客系统 Docker 镜像构建
# ============================================================
# 构建命令: docker build -t blog-systme:latest .
# 运行命令: docker run -d -p 8080:8080 \
#            -e DB_USERNAME=root \
#            -e DB_PASSWORD=yourpassword \
#            -e JWT_SECRET=yourbase64secret \
#            -e SPRING_DATASOURCE_URL=jdbc:mysql://host:3306/blog_db \
#            -e CORS_ORIGINS=https://your-frontend.com \
#            blog-systme:latest
# ============================================================

FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src
RUN ./mvnw package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
