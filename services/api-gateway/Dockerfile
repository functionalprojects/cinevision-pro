FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy standalone POM and download dependencies  for caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build packaging
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache wget && addgroup -S cinevision && adduser -S cinevision -G cinevision -u 1000

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

USER cinevision

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
