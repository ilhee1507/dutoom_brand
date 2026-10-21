FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app && mkdir -p /app/uploads && chown app:app /app/uploads
COPY --from=build /src/target/dutoom-shop-*.jar app.jar
USER app
EXPOSE 8080
ENV SHOP_UPLOAD_DIR=/app/uploads
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
