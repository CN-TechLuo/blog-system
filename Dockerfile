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
#            -e ADMIN_BOOTSTRAP_ENABLED=true \
#            -v blog-uploads:/app/uploads \
#            -v blog-logs:/app/logs \
#            blog-systme:latest
# ============================================================

FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
# 先复制 Maven 构建文件并预取依赖，形成独立缓存层，加速重复构建
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN apk add --no-cache tzdata \
    && addgroup -S appgroup && adduser -S appuser -G appgroup \
    && mkdir -p /app/uploads /app/logs && chown -R appuser:appgroup /app
ENV TZ=Asia/Shanghai \
    SPRING_PROFILES_ACTIVE=prod
USER appuser
EXPOSE 8080
VOLUME /app/uploads
VOLUME /app/logs
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
