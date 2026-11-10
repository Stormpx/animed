FROM ghcr.io/graalvm/native-image-community:25 AS builder

COPY . /animed
WORKDIR /animed
ARG TARGETARCH
ARG VERSION=0.0.1
ARG OS_ARCH=$TARGETARCH

ARG UPX_VERSION=5.0.0
ARG UPX_ARCHIVE=upx-${UPX_VERSION}-${OS_ARCH}_linux.tar.xz
RUN microdnf -y install wget xz && \
    wget -q https://github.com/upx/upx/releases/download/v${UPX_VERSION}/${UPX_ARCHIVE} && \
    tar -xJf ${UPX_ARCHIVE} && \
    rm -rf ${UPX_ARCHIVE} && \
    mv upx-${UPX_VERSION}-${OS_ARCH}_linux/upx . && \
    rm -rf upx-${UPX_VERSION}-${OS_ARCH}_linux

RUN microdnf install -y findutils
RUN microdnf --nobest install -y glibc-all-langpacks
RUN chmod +x ./gradlew
RUN ./gradlew NativeCompile --no-daemon

ENV OUTPUT=build/native/nativeCompile
ENV ARTIFACT=$OUTPUT/animed-$VERSION

# GraalVM 25 会把 AWT 等 JDK 库拆成可执行文件旁边的 lib*.so（libjava/libjvm 是 shim），
# 运行时按可执行文件所在目录查找，所以整份输出目录都要带走，不能只 mv 一个文件
RUN mkdir -p /artifact && \
    cp -a $OUTPUT/. /artifact/ && \
    mv "/artifact/animed-$VERSION" "/artifact/animed" && \
    ./upx -9 "/artifact/animed"

FROM debian:bookworm-slim

#RUN apt update
# Set the locale
#RUN apt install -y locales
#RUN sed -i '/en_US.UTF-8/s/^# //g' /etc/locale.gen && \
#    locale-gen
ENV LANG=en_US.UTF-8
ENV LANGUAGE=en_US:en
ENV LC_ALL=en_US.UTF-8

COPY --from=builder "/artifact" "/app"
ENV PATH="/app:${PATH}"

ENTRYPOINT [ "/app/animed" ]
