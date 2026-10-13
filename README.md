# LineageIcons - Icon Pack

LineageOS system icons on any customizable launcher.

![](images/banner.svg)
![](images/preview.png)

### Styles

All three styles ship in a single APK. **New** is the default that `appfilter.xml`
applies everywhere; **Retro** and **Classic** can be assigned to individual apps from
your launcher's per-app icon editor (long-press an app -> edit icon -> Lineage Icons),
so styles can be mixed per app.

<details>
  <summary>New</summary>

- Current LineageOS icons, vector art
- Adaptive icons with `<monochrome>` on API 31+ (themed icons / Monet)
- Used by default for every themed app
</details>
<details>
  <summary>Classic</summary>

- Adaptive icons, raster art
- Broader launcher support
</details>
<details>
  <summary>Retro</summary>

- Flat legacy icons, no adaptive layers
- High definition icons (AI restoration)
</details>

### Build

Requires JDK 17 and an Android SDK with platform 35 and build-tools 35.0.0.
Point `ANDROID_HOME` (or `sdk.dir` in `local.properties`) at your SDK, then:

```bash
./gradlew assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease   # app/build/outputs/apk/release/
./gradlew bundleRelease     # app/build/outputs/bundle/release/app-release.aab
```

Release builds are unsigned unless a `keystore.properties` file exists in the
repository root:

```properties
storeFile=/path/to/your.jks
storePassword=...
keyAlias=...
keyPassword=...
```

With it, the APK is signed and named `app-release.apk`. Without it you get
`app-release-unsigned.apk`, which Android will refuse to install.

### Credits

- **[WSTxda](https://github.com/WSTxda)** - original creator and maintainer of this icon pack
- **[The LineageOS Project](https://lineageos.org)** - all icon artwork originates from LineageOS, and thanks go out to every designer behind it

The vector artwork here comes from LineageOS and keeps its original
`SPDX-FileCopyrightText: 2022 The LineageOS Project` and
`SPDX-License-Identifier: Apache-2.0` headers, preserved in each file.

This pack's own code is released under the [GPL-3.0](LICENSE) license found in this
repository.
