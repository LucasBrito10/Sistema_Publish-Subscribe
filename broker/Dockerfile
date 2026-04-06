# Estágio de Build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copia apenas o necessário para baixar dependências primeiro (cache)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copia o código fonte de acordo com a nova estrutura
COPY src ./src

# Compila o projeto e gera o Fat JAR
RUN mvn clean package -DskipTests

# Estágio de Execução
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Copia o JAR gerado no estágio de build
COPY --from=build /app/target/broker-1.0-SNAPSHOT.jar app.jar

# Comando para rodar a aplicação
CMD ["java", "-jar", "app.jar"]