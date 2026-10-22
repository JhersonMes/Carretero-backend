# Imagen para desplegar el backend en Render (o cualquier host con Docker).
# Etapa 1: compila el .jar con Maven. Se copia primero el pom.xml para que
# Docker reutilice la cache de dependencias si solo cambia el codigo.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# Etapa 2: imagen liviana solo con el JRE y el .jar.
# MaxRAMPercentage evita que la JVM se pase de los 512 MB del plan gratis.
# El puerto lo define el host con la variable PORT (ver application.properties).
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
