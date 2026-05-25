# BrainBrawl Multiplayer QA Automation

This folder contains automated UI tests for multiplayer flows using multiple devices.

## Setup
1. Ensure at least two Android emulators or physical devices are connected via ADB.
2. Ensure the `app-debug.apk` is installed on both devices.

## Running 1x1 Matchmaking Test
This test runs two Maestro flows in parallel to simulate two real players entering the matchmaking queue and playing a few rounds.

### Command
```bash
./qa/maestro/multiplayer/run_1x1_matchmaking.sh
```

### Flow Details
- **Player A:** Logs in as `GuestA`, enters 1x1 matchmaking, waits for game, taps options 1/2.
- **Player B:** Logs in as `GuestB`, enters 1x1 matchmaking, waits for game, taps options 3/4.

### Logs
Logs for each player are saved in the `logs/` directory created at runtime.
