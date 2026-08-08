# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/*.jar app.jar
# Stockage local des documents déposés (voir DocumentStorageService) : doit exister
# et être inscriptible par l'utilisateur non-root AVANT le "USER app" ci-dessous,
# sinon Files.createDirectories() échoue en AccessDeniedException au démarrage et
# fait planter tout le contexte Spring (donc toute l'appli, y compris /auth/login).
RUN mkdir -p /app/data/documents && chown -R app:app /app
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
