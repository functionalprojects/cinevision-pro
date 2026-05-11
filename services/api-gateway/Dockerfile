FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Copy only the files needed for api-gateway (since we're building from services/api-gateway)
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache wget && addgroup -S cinevision && adduser -S cinevision -G cinevision -u 1000

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

USER cinevision

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]