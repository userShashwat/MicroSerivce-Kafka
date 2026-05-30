FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
COPY src/main/resources/templates templates/
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]