# ClipMind Android — Phase 1

Goal: import a video → it appears in the library → tap → ExoPlayer plays it. **No backend, no AI.**

## How to drop this into your repo

You already have a `clipmind/android/` folder with just a placeholder README from Phase 0.
Replace its contents with the contents of *this* zip's `android/` folder, then:

```bash
cd clipmind
git add android
git commit -m "feat(android): phase 1 — library + player MVP"
git push
```

## Open in Android Studio

1. **File → Open** → select `clipmind/android/`. Wait for Gradle sync.
   - First sync downloads ~500MB of dependencies. Grab coffee.
2. If Studio prompts to upgrade Gradle/AGP, **decline** for now — versions in
   `gradle/libs.versions.toml` are pinned and known good.
3. If you see "missing gradle wrapper," run from the `android/` folder:
   ```bash
   gradle wrapper --gradle-version 8.11.1
   ```
   (Or in Studio: **File → New → Project from Version Control → ignore that, use the
   "sync project with gradle files" button** — Studio will offer to generate the wrapper.)

## Run

- Tools → Device Manager → Create a Pixel 7 / API 34 emulator if you don't have one.
- Hit ▶. App should launch with an empty state and an "Add video" FAB.
- Tap "Add video" → pick any video on the device → it appears in the library → tap → plays.

## Test corpus on the emulator

The default emulator has no videos. Quickest way to add one:

```bash
# from your dev machine, with emulator running
adb push ~/Movies/some-test-video.mp4 /sdcard/Movies/
```

Or drag-and-drop the file onto the emulator window. Then in the picker, navigate to
`Internal storage → Movies`.

## Acceptance criteria

- [ ] App launches without crash.
- [ ] Empty state visible.
- [ ] "Add video" picker opens.
- [ ] Selected video appears in the list with thumbnail, title, duration, size.
- [ ] Tap → player screen opens, ExoPlayer plays with controls.
- [ ] Back navigation returns to library.
- [ ] Kill the app, reopen — video is still there (Room persistence works).

When all six pass, commit a screen recording (`adb shell screenrecord /sdcard/demo.mp4`,
`adb pull /sdcard/demo.mp4`) and update the top-level README's roadmap row to ✅.

## What's intentionally NOT here

- No upload to backend. Phase 2.
- No transcription. Phase 3/4.
- No tests beyond scaffolding. Phase 1's bar is "it runs"; Phase 2 introduces the test rhythm.
- No release signing config. Internal testing track lands in Phase 9.

## Architecture notes

- Single-activity, Compose, Hilt, Room — all wired.
- Clean Architecture layers: `data` (Room) ← `domain` (interfaces + models) ← `presentation` (ViewModels + Compose).
- Presentation never imports `data`; only `domain`. If you find yourself reaching across, it's a smell.
- `OpenDocument()` SAF picker over legacy MediaStore — works on API 26 through 35 without
  permission popups, and the persistable URI permission lets the player open the file later
  even after restart.
