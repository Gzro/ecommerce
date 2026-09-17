FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml ./pom.xml
COPY inventory-service/pom.xml ./inventory-service/pom.xml
COPY inventory-service/src ./inventory-service/src

RUN mvn -f inventory-service/pom.xml clean package -DskipTests


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/inventory-service/target/inventory-service-*.jar inventory-service.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "inventory-service.jar"]