#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PORT=8089

echo "===================================================================="
echo "    RT FOCUS & FILM - PIXEL WATCH 3 SIMULATOR"
echo "===================================================================="
echo "Start lokale test server op http://localhost:$PORT ..."
echo ""
echo "Open in je browser:"
echo " -> Simulator:  http://localhost:$PORT/simulator/simulator.html"
echo " -> Standalone: http://localhost:$PORT/app/index.html"
echo ""
echo "Druk op Ctrl+C om de server te stoppen."
echo "===================================================================="

cd "$DIR"

# Probeer browser automatisch te openen indien mogelijk
if which xdg-open > /dev/null 2>&1; then
    (sleep 1 && xdg-open "http://localhost:$PORT/simulator/simulator.html") &
fi

python3 -m http.server $PORT
