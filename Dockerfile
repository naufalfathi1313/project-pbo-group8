# Dockerfile untuk menjalankan VERSI WEB kasir di server online (misalnya Render).
# Tahap 1: build JAR dengan Maven (unit test ikut dijalankan).
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q package

# Tahap 2: hanya JRE + JAR, supaya image kecil.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/workspace-rental.jar app.jar
# Alamat database diisi lewat environment variable DB_URL, DB_USER, DB_PASSWORD di dashboard Render.
EXPOSE 8080
CMD ["java", "-Duser.timezone=Asia/Jakarta", "-cp", "app.jar", "id.unsika.workspace.web.WebServer"]
