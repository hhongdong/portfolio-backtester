# ---- Build stage ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src ./src
RUN ./mvnw -B -q -DskipTests package && \
    mv target/*.jar target/app.jar

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl && \
    addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=build /workspace/target/app.jar app.jar
EXPOSE 8080
USER spring
ENTRYPOINT ["java", "-jar", "app.jar"]