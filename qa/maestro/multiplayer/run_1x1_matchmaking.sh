#!/bin/bash

# Configuration
MAESTRO_BIN="/Users/dinisrato/.maestro/bin/maestro"
TEST_DIR="$(dirname "$0")"

echo "=================================================="
echo "🎮 Starting BrainBrawl 1x1 Matchmaking Parallel Test"
echo "=================================================="

# Detect devices
DEVICES=$(/Users/dinisrato/Library/Android/sdk/platform-tools/adb devices | grep -w "device" | awk '{print $1}')
DEVICE_COUNT=$(echo "$DEVICES" | wc -l | xargs)

if [ "$DEVICE_COUNT" -lt 2 ]; then
    echo "❌ ERROR: At least 2 devices are required. Found: $DEVICE_COUNT"
    echo "Current devices:"
    echo "$DEVICES"
    exit 1
fi

DEVICE_A=$(echo "$DEVICES" | sed -n '1p')
DEVICE_B=$(echo "$DEVICES" | sed -n '2p')

echo "📱 Player A Device: $DEVICE_A"
echo "📱 Player B Device: $DEVICE_B"
echo "--------------------------------------------------"

# Function to run maestro on a device
run_test() {
    local device=$1
    local script=$2
    local log_file=$3
    echo "🏃 Starting $script on $device..."
    $MAESTRO_BIN --device "$device" test "$script" > "$log_file" 2>&1
    return $?
}

# Ensure log directory exists
mkdir -p "$TEST_DIR/logs"

# Run tests in parallel
run_test "$DEVICE_A" "$TEST_DIR/1x1_player_a.yaml" "$TEST_DIR/logs/player_a.log" &
PID_A=$!

# Small delay to offset launch if needed, but matchmaking requires simultaneous entry
sleep 2

run_test "$DEVICE_B" "$TEST_DIR/1x1_player_b.yaml" "$TEST_DIR/logs/player_b.log" &
PID_B=$!

echo "⏳ Waiting for tests to complete..."

# Wait for both processes
wait $PID_A
STATUS_A=$?
wait $PID_B
STATUS_B=$?

echo "--------------------------------------------------"
if [ $STATUS_A -eq 0 ]; then echo "✅ Player A: PASSED"; else echo "❌ Player A: FAILED (Check logs/player_a.log)"; fi
if [ $STATUS_B -eq 0 ]; then echo "✅ Player B: PASSED"; else echo "❌ Player B: FAILED (Check logs/player_b.log)"; fi

if [ $STATUS_A -eq 0 ] && [ $STATUS_B -eq 0 ]; then
    echo "=================================================="
    echo "🎉 1x1 Matchmaking test successful!"
    echo "=================================================="
    exit 0
else
    echo "=================================================="
    echo "⚠️  Test suite had failures."
    echo "=================================================="
    exit 1
fi
