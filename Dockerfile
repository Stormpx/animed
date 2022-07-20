FROM eclipse-temurin:17.0.3_7-jre
MAINTAINER stormpx


RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
RUN echo 'Asia/Shanghai' >/etc/timezone


COPY "animedown.jar" "/app.jar"

ENTRYPOINT [ "java" , "-jar",  "/app.jar" ]
