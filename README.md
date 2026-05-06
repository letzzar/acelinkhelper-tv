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

## Prerequisites

| Requirement | Notes |
|---|---|
| **Android Studio** | Download from [developer.android.com/studio](https://developer.android.com/studio) |
| **JDK 17+** | Bundled with Android Studio — no separate install needed |
| **Android SDK** | Install via Android Studio → SDK Manager → Android 7.0+ (API 24+) |
| **VLC for Android** | Install [VLC](https://play.google.com/store/apps/details?id=org.videolan.vlc) on the TV device |
| **AceStream engine** | Accessible on the local network (e.g. running [acestream-server](https://github.com/letzzar/acestream-server)) |

No API keys required.

## Build

### Using Android Studio (recommended)

```bash
git clone https://github.com/letzzar/acelinkhelper-tv.git
cd acelinkhelper-tv
```

1. Open Android Studio → **Open an existing project** → select the cloned folder
2. Wait for Gradle sync to complete
3. Connect your Android TV device via ADB, or use an Android TV emulator
4. Press **▶ Run**

### Command line

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires signing config)
./gradlew assembleRelease
```

APK output: `app/build/outputs/apk/`

### Install via ADB

```bash
adb connect <tv-ip-address>
adb install app/build/outputs/apk/debug/app-debug.apk
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
- Lanza VLC para Android o el reproductor predeterminado con el stream
- Interfaz optimizada para TV (navegación con D-pad)

## Requisitos previos

| Requisito | Notas |
|---|---|
| **Android Studio** | Descarga desde [developer.android.com/studio](https://developer.android.com/studio) |
| **JDK 17+** | Incluido con Android Studio — no necesita instalación aparte |
| **Android SDK** | Instala desde Android Studio → SDK Manager → Android 7.0+ (API 24+) |
| **VLC para Android** | Instala [VLC](https://play.google.com/store/apps/details?id=org.videolan.vlc) en el dispositivo TV |
| **Motor AceStream** | Accesible en la red local (p. ej. ejecutando [acestream-server](https://github.com/letzzar/acestream-server)) |

No se necesitan claves API.

## Compilar

### Con Android Studio (recomendado)

```bash
git clone https://github.com/letzzar/acelinkhelper-tv.git
cd acelinkhelper-tv
```

1. Abre Android Studio → **Open an existing project** → selecciona la carpeta clonada
2. Espera a que Gradle sincronice
3. Conecta tu dispositivo Android TV por ADB o usa un emulador de Android TV
4. Pulsa **▶ Run**

### Línea de comandos

```bash
# APK de depuración
./gradlew assembleDebug

# APK de release (requiere configuración de firma)
./gradlew assembleRelease
```

APK en: `app/build/outputs/apk/`

### Instalar por ADB

```bash
adb connect <ip-del-tv>
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Licencia

MIT © 2026 letzzar
