FROM debian:buster-slim
MAINTAINER stormpx


RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
RUN echo 'Asia/Shanghai' >/etc/timezone


COPY "build/native/nativeCompile/animed" "/animed"

ENTRYPOINT [ "/animed" ]
