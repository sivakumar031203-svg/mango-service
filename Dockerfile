FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copy only required files first (faster build)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

# Now copy source code
COPY src src

# Build jar
RUN ./mvnw clean package -DskipTests

# Rename jar (important)
RUN mv target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]