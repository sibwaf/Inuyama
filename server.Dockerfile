FROM eclipse-temurin:11-jdk AS backend-builder

WORKDIR /build
COPY . .
RUN chmod +x ./gradlew && ./gradlew :Server:distTar

WORKDIR /build/Server/build/distributions
RUN tar -xf Server.tar

FROM node:19 AS frontend-builder

WORKDIR /build
COPY Web .

RUN npm install && npm run build

FROM eclipse-temurin:11-jre

WORKDIR /app
COPY --from=backend-builder /build/Server/build/distributions/Server .
COPY --from=frontend-builder /build/dist frontend

RUN chmod +x bin/Server

ENTRYPOINT ["/app/bin/Server"]
