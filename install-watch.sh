#!/usr/bin/env bash
# ==============================================================================
# Google Pixel Watch 3 - RT Focus App Installer
# ==============================================================================
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APK="$DIR/dist/focusverandering-wear.apk"

# Find or setup ADB
if command -v adb >/dev/null 2>&1; then
    ADB="adb"
elif [ -x "$DIR/tools/platform-tools/adb" ]; then
    ADB="$DIR/tools/platform-tools/adb"
elif [ -x "$DIR/tools/adb" ]; then
    ADB="$DIR/tools/adb"
elif [ -f "$DIR/tools/platform-tools/adb" ]; then
    chmod +x "$DIR/tools/platform-tools/adb"
    ADB="$DIR/tools/platform-tools/adb"
else
    echo "⚠️ ADB (Android Debug Bridge) is niet gevonden op je systeem."
    echo ""
    echo "Installeer ADB:"
    echo " - Ubuntu / Debian: sudo apt install adb"
    echo " - Fedora:          sudo dnf install android-tools"
    echo " - Arch Linux:      sudo pacman -S android-tools"
    echo " - macOS:           brew install android-platform-tools"
    echo " - Of download de officiële Android SDK Platform-Tools:"
    echo "   https://developer.android.com/tools/releases/platform-tools"
    echo ""
    read -p "Wil je proberen platform-tools automatisch te downloaden naar tools/? (j/n): " AUTO_DL
    if [ "$AUTO_DL" = "j" ] || [ "$AUTO_DL" = "J" ]; then
        OS="$(uname -s)"
        case "$OS" in
            Linux*)  DL_URL="https://dl.google.com/android/repository/platform-tools-latest-linux.zip";;
            Darwin*) DL_URL="https://dl.google.com/android/repository/platform-tools-latest-darwin.zip";;
            *)       echo "Automatisch downloaden niet ondersteund voor $OS."; exit 1;;
        esac
        echo "Downloaden van $DL_URL..."
        mkdir -p "$DIR/tools"
        curl -L "$DL_URL" -o "$DIR/tools/platform-tools.zip"
        unzip -q -o "$DIR/tools/platform-tools.zip" -d "$DIR/tools/"
        rm -f "$DIR/tools/platform-tools.zip"
        ADB="$DIR/tools/platform-tools/adb"
        chmod +x "$ADB"
        echo "✓ ADB succesvol geïnstalleerd in tools/platform-tools/!"
    else
        exit 1
    fi
fi

echo "===================================================================="
echo "    RT FOCUS & FILM CALCULATOR - GOOGLE PIXEL WATCH 3 INSTALLER"
echo "===================================================================="
echo ""
echo "Volg deze stappen op je Google Pixel Watch 3:"
echo " 1. Ga naar Instellingen (tandwiel) op je horloge."
echo " 2. Scroll naar: Systeem -> Info -> Versies."
echo " 3. Tik 7 keer achter elkaar op 'Build-nummer' totdat er staat:"
echo "    'U bent nu ontwikkelaar!'."
echo " 4. Ga terug naar: Instellingen -> Ontwikkelaarsopties (helemaal onderaan)."
echo " 5. Schakel 'ADB-foutopsporing' IN."
echo " 6. Schakel 'Draadloos foutopsporing' IN (bevestig eventueel met 'Altijd toestaan')."
echo " 7. Zorg dat je Pixel Watch 3 met hetzelfde Wi-Fi netwerk verbonden is als deze pc."
echo "===================================================================="
echo ""

# Build APK if not present
if [ ! -f "$APK" ]; then
    echo "APK niet gevonden, bouwen..."
    "$DIR/tools/build-apk.sh"
fi

read -p "Druk op [ENTER] zodra Draadloos Foutopsporing op je horloge AAN staat..."

echo ""
echo "Kies je verbindingsmethode:"
echo " 1) Mijn horloge vraagt een koppelingscode (Eerste keer / 'Nieuw apparaat koppelen')"
echo " 2) Direct verbinden met IP en Poort (Horloge is al eerder gekoppeld)"
echo ""

METHOD=""
while [ "$METHOD" != "1" ] && [ "$METHOD" != "2" ]; do
    read -p "Keuze [1 of 2]: " METHOD
done

if [ "$METHOD" = "1" ]; then
    echo ""
    echo "--------------------------------------------------------------------"
    echo " STAP 1: KOPPELEN (Pairing)"
    echo " Tik op je horloge op: 'Nieuw apparaat koppelen' (Pair new device)."
    echo " LET OP: Laat dit venster OPEN staan op je horloge tijdens het invoeren!"
    echo " Je ziet twee regels:"
    echo "   A) Wi-Fi-koppelingscode (6 cijfers, bijv. 729104)"
    echo "   B) IP-adres en poort (bijv. 192.168.2.17:33973)"
    echo "--------------------------------------------------------------------"
    
    PAIR_SUCCESS=0
    while [ $PAIR_SUCCESS -eq 0 ]; do
        PAIR_ADDR=""
        while [ -z "$PAIR_ADDR" ]; do
            read -p "Voer IP en Koppel-poort in (bijv. 192.168.2.17:33973): " PAIR_ADDR
        done

        PAIR_CODE=""
        while [ -z "$PAIR_CODE" ]; do
            read -p "Voer de 6-cijferige Koppelcode in (bijv. 729104): " PAIR_CODE
        done

        # Check if user accidentally swapped them
        if [[ "$PAIR_CODE" == *":"* ]] && [[ "$PAIR_ADDR" != *":"* ]]; then
            echo "Tip: IP en code leken omgewisseld, we corrigeren dit automatisch..."
            TMP="$PAIR_ADDR"
            PAIR_ADDR="$PAIR_CODE"
            PAIR_CODE="$TMP"
        fi

        echo ""
        echo "Koppelen met $PAIR_ADDR met code $PAIR_CODE..."
        set +e
        PAIR_OUTPUT=$("$ADB" pair "$PAIR_ADDR" "$PAIR_CODE" 2>&1)
        PAIR_EXIT=$?
        set -e
        echo "$PAIR_OUTPUT"

        if [ $PAIR_EXIT -eq 0 ] && [[ "$PAIR_OUTPUT" == *"Successfully paired"* ]]; then
            PAIR_SUCCESS=1
            echo "✓ Succesvol gekoppeld!"
        else
            echo ""
            echo "⚠️ Koppelen mislukt. Let op:"
            echo " - Het 'Nieuw apparaat koppelen' pop-up schermpje op je horloge moet open blijven staan!"
            echo " - Als het schermpje sloot, genereert het horloge een NIEUWE poort en code."
            read -p "Wil je het opnieuw proberen? (j/n): " RETRY
            if [ "$RETRY" != "j" ] && [ "$RETRY" != "J" ]; then
                exit 1
            fi
        fi
    done
    
    echo ""
    echo "--------------------------------------------------------------------"
    echo " STAP 2: VERBINDEN (Connect)"
    echo " Sluit nu het koppel-venstertje op je horloge (tik op het vinkje of ga terug)."
    echo " Je bent nu weer op het hoofdscherm van 'Draadloze foutopsporing'."
    echo " Kijk naar het 'IP-adres en poort' dat DAAR staat."
    echo " (Let op: de poort is meestal ANDERS dan de koppel-poort van daarnet!)"
    echo "--------------------------------------------------------------------"
    
    CONNECT_ADDR=""
    while [ -z "$CONNECT_ADDR" ]; do
        read -p "Voer IP-adres en Verbindings-poort in (bijv. 192.168.2.17:41235): " CONNECT_ADDR
    done
    
    echo "Verbinden met $CONNECT_ADDR..."
    "$ADB" connect "$CONNECT_ADDR"
else
    CONNECT_ADDR=""
    while [ -z "$CONNECT_ADDR" ]; do
        read -p "Voer het IP-adres en de Poort van je horloge in (bijv. 192.168.2.17:5555): " CONNECT_ADDR
    done
    echo "Verbinden met $CONNECT_ADDR..."
    "$ADB" connect "$CONNECT_ADDR"
fi

echo ""
echo "Apparatenlijst controleren..."
"$ADB" devices -l

# Determine target device to avoid 'more than one device' error when mDNS also connects
TARGET_DEVICE="$CONNECT_ADDR"
if [ -z "$TARGET_DEVICE" ]; then
    TARGET_DEVICE=$("$ADB" devices | grep -E "\s+device$" | head -n 1 | awk '{print $1}')
fi

echo ""
echo "Bezig met installeren van RT Focus op je Google Pixel Watch 3 ($TARGET_DEVICE)..."
"$ADB" -s "$TARGET_DEVICE" install -r -t "$APK"

echo ""
echo "App starten op het horlogescherm..."
"$ADB" -s "$TARGET_DEVICE" shell am start -n com.rt.focuswatch/.MainActivity

echo ""
echo "===================================================================="
echo " GEFELICITEERD! De app is succesvol geïnstalleerd op je Pixel Watch 3!"
echo " Je kunt de app nu openen via de applicatielijst op je horloge."
echo "===================================================================="
