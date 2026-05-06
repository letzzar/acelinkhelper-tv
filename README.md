# AcelinkHelper TV (Android TV)

**English** | [Español](#español)

---

An Android TV app that intercepts `acestream://` URLs and plays the converted HTTP stream via VLC or the system video player.

## Features

- Handles `acestream://` deep links on Android TV
- Connects to a configurable AceStream engine on the local network
- Converts the acestream hash to an HTTP stream URL
- Launches VLC for Android or the default video player with the stream
- Optimized for TV navigation (D-pad friendly UI)

## Requirements

- Android TV / Google TV device (Android 7+)
- [VLC for Android](https://play.google.com/store/apps/details?id=org.videolan.vlc) installed
- An AceStream engine accessible on the network

## Build

```bash
./gradlew assembleRelease
```

The APK is placed in `app/build/outputs/apk/release/`.

## Install

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

## Usage

1. Install the app on your Android TV device
2. Open Settings and configure the AceStream engine address
3. Click any `acestream://` link — AcelinkHelper TV intercepts it and starts playback

---

## Español

App para Android TV que intercepta URLs `acestream://` y reproduce el stream HTTP convertido mediante VLC o el reproductor de vídeo del sistema.

## Características

- Gestiona deep links `acestream://` en Android TV
- Se conecta a un motor AceStream configurable en la red local
- Convierte el hash de acestream en una URL de stream HTTP
- Lanza VLC para Android o el reproductor de vídeo predeterminado con el stream
- Interfaz optimizada para TV (navegación con D-pad)

## Requisitos

- Dispositivo Android TV / Google TV (Android 7+)
- [VLC para Android](https://play.google.com/store/apps/details?id=org.videolan.vlc) instalado
- Un motor AceStream accesible en la red

## Compilar

```bash
./gradlew assembleRelease
```

El APK queda en `app/build/outputs/apk/release/`.

## Licencia

MIT © 2026 letzzar
