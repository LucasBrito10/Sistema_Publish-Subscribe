# Estágio de Build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Otimização de cache com as dependências
COPY pom.xml .
RUN mvn dependency:go-offline

# Cópia e compilação do código
COPY src ./src
RUN mvn clean package -DskipTests

# Estágio de Execução
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Copia o JAR do cliente
COPY --from=build /app/target/cliente-1.0-SNAPSHOT.jar app.jar

# Comando para rodar a aplicação usando a classe principal do manifesto
CMD ["java", "-jar", "app.jar"]