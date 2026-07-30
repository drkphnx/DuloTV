# Dulo TV

Lightweight Android TV / Mi TV Stick wrapper for https://dulo.gd with remote navigation and a low-end-device performance mode.

## TV optimizations
- Removes CSS animations and transitions.
- Disables backdrop blur, blur/glass/glow filters, shadows and smooth scrolling.
- Marks poster images for lazy loading and async decoding.
- Automatically removes the "Join Discord" modal when it appears.
- D-pad spatial navigation with a simple outline focus indicator (no scaling animation).
- Fullscreen video support, Play/Pause, and ±10 second seek with Left/Right in fullscreen.
- Branded splash screen and launcher/TV artwork.

## GitHub Actions build
Open **Actions → Build Dulo TV APK → Run workflow**.
After the run succeeds, download the **DuloTV-debug** artifact. It contains `app-debug.apk`.

## Ultra-low-memory playback mode

This build keeps the existing no-blur/no-animation performance mode and adds a playback-only memory saver for older 1 GB TV sticks. When an HTML5 video begins playing it disconnects DuloTV's DOM observers, unloads poster images, pauses unused video elements, removes navigation-hint overlays, and disables expensive compositing effects behind the player. It also handles Android WebView renderer death on API 26+ and provides a restart screen instead of allowing the renderer failure to take down the Activity.
