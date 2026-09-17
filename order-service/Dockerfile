FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml ./pom.xml
COPY order-service/pom.xml ./order-service/pom.xml
COPY order-service/src ./order-service/src

RUN mvn -f order-service/pom.xml clean package -DskipTests


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/order-service/target/order-service-*.jar order-service.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "order-service.jar"]