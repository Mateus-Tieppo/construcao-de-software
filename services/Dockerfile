# Imagem de execução comum aos serviços Spring Boot.
# Pré-requisito: "mvn -DskipTests package" na raiz (gera services/<servico>/target/app.jar).
FROM eclipse-temurin:21-jre-alpine
ARG SERVICO
RUN addgroup -S app && adduser -S app -G app && mkdir -p /dados/ingressos && chown app:app /dados/ingressos
WORKDIR /app
COPY ${SERVICO}/target/app.jar app.jar
USER app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
