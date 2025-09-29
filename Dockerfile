FROM bellsoft/liberica-runtime-container:jre-25-cds-slim-musl
COPY build/libs/time-tracker-reai-app-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
CMD ["java", "-XX:+UseCompactObjectHeaders", "-jar", "/app.jar"]