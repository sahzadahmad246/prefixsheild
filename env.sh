# PrefixShield local Android build (no Android Studio)

export JAVA_HOME="${JAVA_HOME:-$HOME/tools/jdk-17}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$HOME/tools/gradle/bin:$PATH"
