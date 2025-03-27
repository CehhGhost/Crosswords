# Используем базовый образ с Java 17
FROM openjdk:17-jdk-alpine

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем собранный JAR-файл в контейнер
COPY target/crosswords-0.1.1-SNAPSHOT.jar app.jar

# Открываем порт, на котором работает приложение
EXPOSE 8081

# Команда для запуска приложения
ENTRYPOINT ["java", "-jar", "app.jar"]