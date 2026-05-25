#!/bin/bash

# Configuration
MAESTRO_BIN="/Users/dinisrato/.maestro/bin/maestro"
TEST_DIR="$(dirname "$0")"
DEVICE="emulator-5554"

echo "=========================================="
echo "🚀 Starting BrainBrawl QA Automation Suite"
echo "Device: $DEVICE"
echo "=========================================="

# List of tests to run
TESTS=(
    "smoke_test.yaml"
    "main_navigation.yaml"
    "ranking_profile_friends_history.yaml"
    "one_x_one_matchmaking_entry.yaml"
    "two_x_two_matchmaking_entry.yaml"
)

for test in "${TESTS[@]}"; do
    echo "------------------------------------------"
    echo "🏃 Running: $test"
    $MAESTRO_BIN --device $DEVICE test "$TEST_DIR/$test"

    if [ $? -ne 0 ]; then
        echo "❌ FAILED: $test"
        echo "Aborting remaining tests."
        exit 1
    else
        echo "✅ PASSED: $test"
    fi
done

echo "=========================================="
echo "🎉 All tests passed successfully!"
echo "=========================================="
exit 0
