# Estágio de Build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Otimização de cache
COPY pom.xml .
RUN mvn dependency:go-offline

# Cópia e compilação do código
COPY src ./src
RUN mvn clean package -DskipTests

# Estágio de Execução
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Copia o JAR do sensor
COPY --from=build /app/target/sensor-1.0-SNAPSHOT.jar app.jar

# Define o comando base para executar o JAR
ENTRYPOINT ["java", "-cp", "app.jar"]

# Define a classe padrão a ser executada se nenhuma for especificada
CMD ["implementation.SensorTemperature"]