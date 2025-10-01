FROM bellsoft/liberica-runtime-container:jre-24-cds-slim-musl
COPY build/libs/time-tracker-0.0.1-SNAPSHOT.jar /app.jar
EXPOSE 8081
CMD ["java", "-jar", "/app.jar"]