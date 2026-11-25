# ---------- Etapa 1: compilar con Maven (los tests se corren aparte con mvn test) ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Etapa 2: imagen liviana solo con el JRE y el .jar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/liquidador-service-2.0.0.jar app.jar
EXPOSE 8084
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
