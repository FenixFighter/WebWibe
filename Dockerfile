# Production stage
FROM openjdk:21-jdk
WORKDIR /app
ENV PROFILES_ACTIVE=prod
# Копируем JAR файл
COPY build/libs/ww2-0.0.1-SNAPSHOT.jar app.jar
# Указываем команду запуска для приложения
ENTRYPOINT ["java", "-jar", "app.jar"]