# Builder part for maven packaging.
FROM maven:3-eclipse-temurin-17 as builder

WORKDIR /app

# Do the actual build
COPY ./source /app
RUN mvn --no-transfer-progress -DskipTests=true clean package

# Move the resultant jar to a known name so it can be picked up by the final image.
RUN mv -v target/aerius-file-server-*.jar app.jar \
# Clean up image so we can rerun without the cache being invalidated because target files are changed
    && mvn clean

#actual image
FROM debian:bullseye

# Get java from eclipse-temurin image (could perhaps do something with jlink later on)
ENV JAVA_HOME=/opt/java/openjdk
COPY --from=eclipse-temurin:17 $JAVA_HOME $JAVA_HOME
ENV PATH "${JAVA_HOME}/bin:${PATH}"

# Set defaults
ENV JAVA_OPTIONS="-XX:MaxRAMPercentage=75"

# Copy the actual application
COPY --from=builder /app/app.jar /

# Set up a non root account called aerius (debian based)
RUN groupadd -r aerius && useradd -r -g aerius aerius

USER aerius

# Set working directory to a directory a non-root user will have access to
WORKDIR /tmp

CMD java -server $JAVA_OPTIONS -jar /app.jar
