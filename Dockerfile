FROM eclipse-temurin:23-jdk AS build
WORKDIR /app
COPY src/Main.java src/Main.java
RUN mkdir out && javac -d out src/Main.java

FROM eclipse-temurin:23-jre
WORKDIR /app
COPY --from=build /app/out ./out
COPY public ./public
ENV HOST=0.0.0.0
ENV PORT=8080
EXPOSE 8080
CMD ["java", "-cp", "out", "Main"]
