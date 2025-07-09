FROM openjdk:21-jdk-slim AS build

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn/
COPY pom.xml .

RUN ./mvnw dependency:go-offline -B

COPY src ./src

RUN ./mvnw clean package -DskipTests


FROM openjdk:21-jdk-slim
WORKDIR /app

COPY --from=build /app/target/DFinancialStatement-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]