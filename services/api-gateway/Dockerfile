FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY eureka-server/pom.xml eureka-server/pom.xml
COPY movieService/pom.xml movieService/pom.xml
COPY userService/pom.xml userService/pom.xml
COPY emailService/pom.xml emailService/pom.xml
COPY api-gateway/src api-gateway/src

RUN mvn -pl api-gateway -am clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache wget && addgroup -S cinevision && adduser -S cinevision -G cinevision -u 1000

WORKDIR /app

COPY --from=build /workspace/api-gateway/target/*.jar app.jar

USER cinevision

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
