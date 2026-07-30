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
