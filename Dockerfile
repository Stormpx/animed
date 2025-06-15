FROM container-registry.oracle.com/graalvm/native-image:21 AS builder

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

RUN microdnf install findutils
RUN microdnf --nobest install glibc-all-langpacks
RUN chmod +x ./gradlew
RUN ./gradlew NativeCompile

ENV ARTIFACT=build/native/nativeCompile/animed-$VERSION

RUN ./upx -9 $ARTIFACT

RUN mv "$ARTIFACT" "/artifact"

FROM debian:bookworm-slim

#RUN apt update
# Set the locale
#RUN apt install -y locales
#RUN sed -i '/en_US.UTF-8/s/^# //g' /etc/locale.gen && \
#    locale-gen
ENV LANG=en_US.UTF-8
ENV LANGUAGE=en_US:en
ENV LC_ALL=en_US.UTF-8

COPY --from=builder "/artifact" "/animed"

ENTRYPOINT [ "/animed" ]
