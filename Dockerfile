# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only the POM first so Maven can cache dependencies between builds
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Now copy the rest of the source and build the jar
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy only the built jar from the build stage (keeps the final image small)
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# JAVA_OPTS lets you tune JVM memory from Railway's environment variables
# without rebuilding the image.
ENV JAVA_OPTS="-Xmx512m"

# Railway injects its own PORT env var at runtime (it's not fixed like
# other platforms) - default to 8080 if it's not set, e.g. when running
# this image locally.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
