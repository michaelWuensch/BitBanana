# Use Debian slim as the base
FROM docker.io/debian:stable-20240722-slim

# 1) Install required system packages (including Java)
RUN set -ex; \
    apt-get update; \
    DEBIAN_FRONTEND=noninteractive apt-get install --yes -o APT::Install-Suggests=false --no-install-recommends \
        bzip2 make automake ninja-build g++-multilib libtool binutils-gold \
        bsdmainutils pkg-config python3 patch bison curl unzip git openjdk-17-jdk disorderfs; \
    rm -rf /var/lib/apt/lists/*;

# 2) Set environment variables for Android SDK
ENV ANDROID_SDK_ROOT=/sdk
ENV ANDROID_SDK=/sdk
ENV ANDROID_HOME=/sdk
ENV ANDROID_SDK_URL=https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
ENV ANDROID_BUILD_TOOLS_VERSION=34.0.0
ENV ANDROID_VERSION=34
ENV ANDROID_NDK_VERSION=25.1.8937393
ENV ANDROID_CMAKE_VERSION=3.18.1
ENV ANDROID_NDK_HOME=${ANDROID_HOME}/ndk/${ANDROID_NDK_VERSION}/
ENV PATH=${PATH}:${ANDROID_HOME}/tools:${ANDROID_HOME}/platform-tools
ENV PATH=${ANDROID_NDK_HOME}:$PATH
ENV PATH=${ANDROID_NDK_HOME}/prebuilt/linux-x86_64/bin/:$PATH

# 3) Download & unzip command-line tools
RUN set -ex; \
    mkdir "$ANDROID_HOME" && \
    cd "$ANDROID_HOME" && \
    curl -o sdk.zip $ANDROID_SDK_URL && \
    unzip sdk.zip && \
    rm sdk.zip

# 4) Accept licenses and install Android SDK components
RUN yes | ${ANDROID_HOME}/cmdline-tools/bin/sdkmanager --sdk_root=$ANDROID_HOME --licenses
RUN $ANDROID_HOME/cmdline-tools/bin/sdkmanager --sdk_root=$ANDROID_HOME --update
RUN $ANDROID_HOME/cmdline-tools/bin/sdkmanager --sdk_root=$ANDROID_HOME \
    "build-tools;${ANDROID_BUILD_TOOLS_VERSION}" \
    "platforms;android-${ANDROID_VERSION}" \
    "cmake;$ANDROID_CMAKE_VERSION" \
    "platform-tools" \
    "ndk;$ANDROID_NDK_VERSION"

# 5) Preinstall gradle
ENV GRADLE_VERSION=8.12
RUN set -ex; \
    curl -fSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-all.zip" -o /tmp/gradle.zip; \
    unzip /tmp/gradle.zip -d /opt; \
    rm /tmp/gradle.zip; \
    ln -s "/opt/gradle-${GRADLE_VERSION}/bin/gradle" /usr/local/bin/gradle