# Module: Timer

## Purpose
Calculate and validate absolute timestamps for the lock deadline.

## Key Files
- `TimerManager.kt`: Helper class for deadline calculations.

## Invariants
> Active deadline can never move backwards.
> A timer is active if `currentTime < lockEndTime`.

## Dependencies
- None, purely business logic.

## Used By
- `ui/` (when setting the timer)
- `blocking/` (when verifying if lock is still active)

## Modification Rules
- All durations must be strictly added to the *current time* when a new lock is created.
- If extending an existing lock, the new duration must be added to the *current deadline*.
