# BrainBrawl QA Automation

This folder contains automated UI tests for the BrainBrawl app using [Maestro](https://maestro.mobile.dev/).

## Prerequisites
- Maestro CLI installed.
- Android Emulator or Physical Device connected via ADB.

## Tests Overview
- `smoke_test.yaml`: Basic app launch and guest login.
- `main_navigation.yaml`: Tests navigation between main screens and categories.
- `ranking_profile_friends_history.yaml`: Tests bottom nav and header action buttons.
- `one_x_one_matchmaking_entry.yaml`: Tests entering and cancelling 1x1 matchmaking.
- `two_x_two_matchmaking_entry.yaml`: Tests entering and cancelling 2x2 matchmaking.

## How to Run

### Run all tests
```bash
./qa/maestro/run_all.sh
```

### Run a specific test
```bash
maestro test qa/maestro/smoke_test.yaml
```

### Run on a specific device
```bash
maestro --device emulator-5554 test qa/maestro/smoke_test.yaml
```
