# =====================================================================
# Stage 1: Build the shaded JAR using Maven and OpenJDK 25
# =====================================================================
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Compile and package the shaded JAR
RUN mvn clean package -DskipTests

# =====================================================================
# Stage 2: Minimal runtime container with GTK and X11 libraries
# =====================================================================
FROM eclipse-temurin:21-jre

# Install X11 and GTK dependencies required by JavaFX
RUN apt-get update && apt-get install -y \
    libgtk-3-0 \
    libglu1-mesa \
    libasound2 \
    libxxf86vm1 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder /app/target/JettraPluginStore-1.0-SNAPSHOT.jar /app/jettrapluginstore.jar

# Set environment variables for JavaFX X11 display
ENV DISPLAY=:0

# Command to run (launches interactive shell by default)
ENTRYPOINT ["java", "-jar", "/app/jettrapluginstore.jar"]
