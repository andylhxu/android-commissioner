#!/bin/bash

# verify number of arguments
if [ "$#" -ne 1 ]; then
	echo "Usage: $0 sdk-path";
	exit 1;
fi
chmod +x gradlew || { echo "Failed to change the permission of gradlew file,
make sure you have the right permission"; }

# register environment variable
echo "Setting environment variable for Android SDK";

export ANDROID_HOME="$1" || { echo "Failed to export PATH of android sdk"; exit 1; }

echo "Building apk";

./gradlew assembleDebug || { echo "Build apk failed, please check sdk packages"; exit 1; } 

echo "Copying the file";

cp ./app/build/outputs/apk/app-debug.apk . || { echo "Copy failed, apk not found"; exit 1; }

echo "Renaming the file";

mv app-debug.apk BootStrapServer_debug.apk || { echo "Rename failed"; exit 1; }

echo "Autoscript runned successfully, BootStrapServer_debug.apk exported";
