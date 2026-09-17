FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml ./pom.xml
COPY payment-service/pom.xml ./payment-service/pom.xml
COPY payment-service/src ./payment-service/src

RUN mvn -f payment-service/pom.xml clean package -DskipTests


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/payment-service/target/payment-service-*.jar payment-service.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "payment-service.jar"]