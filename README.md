# Cooldowns

A small, single-screen Android app for starting labeled cooldown timers and keeping them visible after they finish.

## Product behavior

- **New timer** is the primary action. Add a label and a duration in `HH:MM:SS`; hours can exceed 24.
- Timers are sorted by soonest completion under **Running**. The countdown updates while the app is open.
- Finished timers stay in **Completed** until you delete them. Finishing is a state change only: there is no notification, sound, or automatic removal.
- Editing an in-progress timer updates its label and sets a new remaining duration starting now. The editor makes this reset explicit.
- Editing a completed timer updates its label and saved duration without changing its completed state. **Restart** uses that saved duration.
- Deleting either kind of timer asks for confirmation.
- The editor uses one full-screen layout, with a shared numeric keyboard for all time fields.

## State and lifecycle

Each timer stores an ID, label, duration, and absolute end timestamp. The app derives whether it is running or complete from `end timestamp <= current time`; it does not decrement and persist a counter in the background. Timer records are stored locally in app-private preferences. When Android pauses or kills the app, time still elapses naturally; opening or resuming the app recalculates the countdown from the saved deadline. There is no background service and no network access.

## Open and run

Open this folder in Android Studio, allow Gradle sync, then run the `app` configuration on a device or emulator (Android 14 / API 34 or newer). The project uses Kotlin, Jetpack Compose, and the Android Gradle Plugin 8.7.3. Release is optimized and signed with the local debug key so it can be installed directly for personal use; do not use that key to publish or distribute the app. Android Studio's debug build is larger to support debugging.
