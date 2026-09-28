FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /workspace/target/erp_backend-0.0.1-SNAPSHOT.jar /app/erp_backend.jar
COPY render_start.sh /app/render_start.sh
RUN chmod +x /app/render_start.sh

EXPOSE 8080
ENTRYPOINT ["/app/render_start.sh"]
