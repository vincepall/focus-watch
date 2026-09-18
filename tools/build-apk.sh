#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$DIR/.." && pwd)"

if [ -d "$PROJECT_ROOT/tools/android-sdk" ]; then
    SDK_ROOT="$PROJECT_ROOT/tools/android-sdk"
elif [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME" ]; then
    SDK_ROOT="$ANDROID_HOME"
elif [ -n "$ANDROID_SDK_ROOT" ] && [ -d "$ANDROID_SDK_ROOT" ]; then
    SDK_ROOT="$ANDROID_SDK_ROOT"
elif [ -d "$HOME/Android/Sdk" ]; then
    SDK_ROOT="$HOME/Android/Sdk"
elif [ -d "$HOME/Library/Android/sdk" ]; then
    SDK_ROOT="$HOME/Library/Android/sdk"
else
    echo "Fout: Android SDK niet gevonden!"
    echo "Stel \$ANDROID_HOME in of installeer de Android SDK (API 34 & build-tools 34.0.0)."
    exit 1
fi

BUILD_TOOLS="$SDK_ROOT/build-tools/34.0.0"
PLATFORM="$SDK_ROOT/platforms/android-34/android.jar"
SRC="$PROJECT_ROOT/android/app/src/main"
BUILD_DIR="$PROJECT_ROOT/android/build"
DIST_DIR="$PROJECT_ROOT/dist"
OUT_APK="$DIST_DIR/focusverandering-wear.apk"
KEYSTORE="$PROJECT_ROOT/tools/debug.keystore"
WEAR_TILES_LIBS="$PROJECT_ROOT/tools/wear-tiles-libs"

echo "=== Building Wear OS APK for Google Pixel Watch 3 ==="

# Sync latest web app files to assets
echo "[1/7] Syncing web app assets..."
rm -rf "$SRC/assets"
mkdir -p "$SRC/assets"
cp -r "$PROJECT_ROOT/app/"* "$SRC/assets/"

# Clean build directory
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"/{compiled_res,gen,classes,dex}
mkdir -p "$DIST_DIR"

# 1. Compile Android Resources
echo "[2/7] Compiling resources with aapt2..."
"$BUILD_TOOLS/aapt2" compile --dir "$SRC/res" -o "$BUILD_DIR/compiled_res.zip"

# 2. Link Resources & Assets
echo "[3/7] Linking package with aapt2..."
"$BUILD_TOOLS/aapt2" link \
    -o "$BUILD_DIR/res.apk" \
    -I "$PLATFORM" \
    --manifest "$SRC/AndroidManifest.xml" \
    -A "$SRC/assets" \
    --java "$BUILD_DIR/gen" \
    --auto-add-overlay \
    "$BUILD_DIR/compiled_res.zip"

# 3. Compile Java Source Code
echo "[4/7] Compiling Java code..."
javac -source 8 -target 8 \
    -bootclasspath "$PLATFORM" \
    -cp "$PLATFORM:$BUILD_DIR/gen:$WEAR_TILES_LIBS/*" \
    -d "$BUILD_DIR/classes" \
    $(find "$BUILD_DIR/gen" "$SRC/java" -name "*.java")

# 4. Convert bytecode to Dalvik Executable (classes.dex)
echo "[5/7] Converting bytecode to DEX with d8..."
CLASS_FILES=$(find "$BUILD_DIR/classes" -name "*.class")
"$BUILD_TOOLS/d8" --lib "$PLATFORM" --output "$BUILD_DIR/dex" $CLASS_FILES "$WEAR_TILES_LIBS"/*.jar

# 5. Package classes.dex into APK
echo "[6/7] Packaging APK..."
cp "$BUILD_DIR/res.apk" "$BUILD_DIR/unaligned.apk"
cd "$BUILD_DIR/dex"
zip -u "$BUILD_DIR/unaligned.apk" classes.dex
cd "$PROJECT_ROOT"

# 6. Zipalign
"$BUILD_TOOLS/zipalign" -f -p 4 "$BUILD_DIR/unaligned.apk" "$BUILD_DIR/aligned.apk"

# 7. Generate debug keystore if not present and sign APK
echo "[7/7] Signing APK with apksigner..."
if [ ! -f "$KEYSTORE" ]; then
    keytool -genkeypair -v \
        -keystore "$KEYSTORE" \
        -storepass android \
        -alias androiddebugkey \
        -keypass android \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -dname "CN=Android Debug,O=Android,C=US" > /dev/null 2>&1
fi

"$BUILD_TOOLS/apksigner" sign \
    --ks "$KEYSTORE" \
    --ks-pass pass:android \
    --ks-key-alias androiddebugkey \
    --key-pass pass:android \
    --out "$OUT_APK" \
    "$BUILD_DIR/aligned.apk"

# Verify signature
"$BUILD_TOOLS/apksigner" verify "$OUT_APK"

echo "=========================================================="
echo " SUCCESS: APK built successfully!"
echo " Output: $OUT_APK"
echo " Size:   $(du -h "$OUT_APK" | cut -f1)"
echo "=========================================================="
