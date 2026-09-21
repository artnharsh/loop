# Decisions

## Use AccessibilityService over DeviceAdmin/AppUsage
### Context
Need a reliable way to detect when a blocked app is opened and prevent the user from continuing.

### Decision
Use Android `AccessibilityService` instead of DeviceOwner/DeviceAdmin APIs or AppUsageStats.

### Reason
- AccessibilityService allows real-time detection of `TYPE_WINDOW_STATE_CHANGED`.
- It does not require factory-resetting the device (unlike DeviceOwner).
- AppUsageStats has polling delays and is unreliable for instant blocking.

### Alternatives
- DeviceOwner (Too heavy, requires complex setup).
- AppUsageStats polling (Too slow).

### Consequences
- Requires the user to manually enable the Accessibility Service in settings.
- We must handle edge cases where the service might be killed or disabled.
