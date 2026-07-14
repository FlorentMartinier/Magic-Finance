# Step 1: Build the application
FROM gradle:8-jdk21 AS builder
WORKDIR /app
COPY --chown=gradle:gradle . .

# Correction : On force le script gradlew à être exécutable sous Linux
RUN chmod +x gradlew

# On build l'application sans lancer les tests pour accélérer le déploiement
RUN ./gradlew bootJar -x test --no-daemon

# Step 2: Run the application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Optimisation JVM pour la mémoire sur de petits serveurs de prod
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]