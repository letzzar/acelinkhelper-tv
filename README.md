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

## Server Setup (Docker)

AcelinkHelper TV is designed to work with the AceStream engine running in **Docker** — typically on a home server or NAS on your network. The app on your Android TV simply points to that address.

**Requires:** [Docker + Docker Compose](https://docs.docker.com/get-docker/) on the server machine.

---

### Option 1 — Direct (no VPN)

Save as `docker-compose.yml` on your server and run `docker compose up -d`:

```yaml
version: "3"
services:
  acelink:
    image: blaiseio/acelink
    container_name: acelink
    platform: linux/amd64
    ports:
      - 6878:6878   # AceStream engine port — the TV app connects here
    restart: always
```

In the app settings, set the engine address to `http://<server-ip>:6878`.

---

### Option 2 — Behind a WireGuard VPN (recommended)

All AceStream traffic is tunnelled through a VPN using [Gluetun](https://github.com/qdm12/gluetun). AceStream is invisible to your ISP.

```yaml
version: "3"
services:
  gluetun:
    image: qmcgaw/gluetun:latest
    container_name: gluetun
    cap_add:
      - NET_ADMIN
    devices:
      - /dev/net/tun:/dev/net/tun
    ports:
      - 6878:6878   # Port exposed on host — the TV app connects here
    environment:
      - VPN_SERVICE_PROVIDER=custom
      - VPN_TYPE=wireguard
      - WIREGUARD_PRIVATE_KEY=<your_private_key>      # From [Interface] PrivateKey
      - WIREGUARD_ADDRESSES=172.16.0.2/32             # From [Interface] Address
      - WIREGUARD_ENDPOINT_IP=162.159.192.1           # From [Peer] Endpoint (IP)
      - WIREGUARD_ENDPOINT_PORT=2408                  # From [Peer] Endpoint (port)
      - WIREGUARD_PUBLIC_KEY=<peer_public_key>        # From [Peer] PublicKey
      - TZ=Europe/Madrid
    restart: always

  acelink:
    image: blaiseio/acelink
    container_name: acelink
    platform: linux/amd64
    network_mode: "service:gluetun"   # AceLink hides behind Gluetun
    depends_on:
      - gluetun
    restart: always
```

#### How to get your WireGuard keys

**From a VPN provider** (Mullvad, ProtonVPN, IVPN, etc.):

1. Log in to your provider's dashboard and download a **WireGuard config file** (`.conf`)
2. Open it — it looks like this:

```ini
[Interface]
PrivateKey = ABC123...          ← WIREGUARD_PRIVATE_KEY
Address    = 172.16.0.2/32      ← WIREGUARD_ADDRESSES

[Peer]
PublicKey  = XYZ789...          ← WIREGUARD_PUBLIC_KEY
Endpoint   = 162.159.192.1:2408 ← IP → WIREGUARD_ENDPOINT_IP  /  port → WIREGUARD_ENDPOINT_PORT
```

**Generate your own keys** (self-hosted WireGuard server):

```bash
# Install wireguard-tools
sudo apt install wireguard-tools   # Ubuntu / Debian
brew install wireguard-tools       # macOS

# Generate private key
wg genkey > wg_private.key
cat wg_private.key            # → paste as WIREGUARD_PRIVATE_KEY

# Derive public key
cat wg_private.key | wg pubkey > wg_public.key
cat wg_public.key             # → register this on your WireGuard server
```

> **Security:** Never share your private key. Never generate WireGuard keys using online tools.

---

## Prerequisites

| Requirement | Notes |
|---|---|
| **Android Studio** | Download from [developer.android.com/studio](https://developer.android.com/studio) |
| **JDK 17+** | Bundled with Android Studio — no separate install needed |
| **Android SDK** | Install via Android Studio → SDK Manager → Android 7.0+ (API 24+) |
| **VLC for Android** | Install [VLC](https://play.google.com/store/apps/details?id=org.videolan.vlc) on the TV device |
| **AceStream engine** | Running in Docker on your network (see Server Setup above) |

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
./gradlew assembleDebug    # Debug APK
./gradlew assembleRelease  # Release APK (requires signing config)
```

APK output: `app/build/outputs/apk/`

### Install via ADB

```bash
adb connect <tv-ip-address>
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Usage

1. Start the AceStream Docker container (see Server Setup above)
2. Install the app on your Android TV device
3. Open Settings and set the engine address to `http://<server-ip>:6878`
4. Click any `acestream://` link — the app intercepts it and starts playback in VLC

---

## Español

App para Android TV que intercepta URLs `acestream://` y reproduce el stream HTTP convertido mediante VLC o el reproductor de vídeo del sistema.

## Características

- Gestiona deep links `acestream://` en Android TV
- Se conecta a un motor AceStream configurable en la red local
- Convierte el hash de acestream en una URL de stream HTTP
- Lanza VLC para Android o el reproductor predeterminado con el stream
- Interfaz optimizada para TV (navegación con D-pad)

## Configuración del Servidor (Docker)

AcelinkHelper TV está diseñado para funcionar con el motor AceStream ejecutándose en **Docker** — normalmente en un servidor doméstico o NAS en tu red. La app en tu Android TV simplemente apunta a esa dirección.

**Requisito:** [Docker + Docker Compose](https://docs.docker.com/get-docker/) en la máquina servidor.

---

### Opción 1 — Directo (sin VPN)

Guarda como `docker-compose.yml` en el servidor y ejecuta `docker compose up -d`:

```yaml
version: "3"
services:
  acelink:
    image: blaiseio/acelink
    container_name: acelink
    platform: linux/amd64
    ports:
      - 6878:6878   # Puerto del motor AceStream — al que se conecta la app del TV
    restart: always
```

En los ajustes de la app, configura la dirección del motor a `http://<ip-del-servidor>:6878`.

---

### Opción 2 — Detrás de una VPN WireGuard (recomendado)

Todo el tráfico de AceStream se tuneliza a través de una VPN con [Gluetun](https://github.com/qdm12/gluetun). AceStream es invisible para tu ISP.

```yaml
version: "3"
services:
  gluetun:
    image: qmcgaw/gluetun:latest
    container_name: gluetun
    cap_add:
      - NET_ADMIN
    devices:
      - /dev/net/tun:/dev/net/tun
    ports:
      - 6878:6878   # Puerto expuesto en el host — la app del TV se conecta aquí
    environment:
      - VPN_SERVICE_PROVIDER=custom
      - VPN_TYPE=wireguard
      - WIREGUARD_PRIVATE_KEY=<tu_clave_privada>      # De [Interface] PrivateKey
      - WIREGUARD_ADDRESSES=172.16.0.2/32             # De [Interface] Address
      - WIREGUARD_ENDPOINT_IP=162.159.192.1           # De [Peer] Endpoint (IP)
      - WIREGUARD_ENDPOINT_PORT=2408                  # De [Peer] Endpoint (puerto)
      - WIREGUARD_PUBLIC_KEY=<clave_publica_peer>     # De [Peer] PublicKey
      - TZ=Europe/Madrid
    restart: always

  acelink:
    image: blaiseio/acelink
    container_name: acelink
    platform: linux/amd64
    network_mode: "service:gluetun"   # AceLink se oculta tras Gluetun
    depends_on:
      - gluetun
    restart: always
```

#### Cómo obtener tus claves WireGuard

**Desde un proveedor de VPN** (Mullvad, ProtonVPN, IVPN, etc.):

1. Inicia sesión en el panel de tu proveedor y descarga un **archivo de configuración WireGuard** (`.conf`)
2. Ábrelo — tiene este aspecto:

```ini
[Interface]
PrivateKey = ABC123...           ← WIREGUARD_PRIVATE_KEY
Address    = 172.16.0.2/32       ← WIREGUARD_ADDRESSES

[Peer]
PublicKey  = XYZ789...           ← WIREGUARD_PUBLIC_KEY
Endpoint   = 162.159.192.1:2408  ← IP → WIREGUARD_ENDPOINT_IP  /  puerto → WIREGUARD_ENDPOINT_PORT
```

**Generar tus propias claves** (servidor WireGuard propio):

```bash
# Instalar wireguard-tools
sudo apt install wireguard-tools   # Ubuntu / Debian
brew install wireguard-tools       # macOS

# Generar clave privada
wg genkey > wg_private.key
cat wg_private.key            # → pega esto como WIREGUARD_PRIVATE_KEY

# Derivar clave pública
cat wg_private.key | wg pubkey > wg_public.key
cat wg_public.key             # → registra esto en tu servidor WireGuard
```

> **Seguridad:** Nunca compartas tu clave privada. Nunca generes claves WireGuard con herramientas online.

---

## Requisitos previos

| Requisito | Notas |
|---|---|
| **Android Studio** | Descarga desde [developer.android.com/studio](https://developer.android.com/studio) |
| **JDK 17+** | Incluido con Android Studio — no necesita instalación aparte |
| **Android SDK** | Instala desde Android Studio → SDK Manager → Android 7.0+ (API 24+) |
| **VLC para Android** | Instala [VLC](https://play.google.com/store/apps/details?id=org.videolan.vlc) en el dispositivo TV |
| **Motor AceStream** | Ejecutándose en Docker en tu red (ver Configuración del Servidor arriba) |

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
./gradlew assembleDebug    # APK de depuración
./gradlew assembleRelease  # APK de release (requiere configuración de firma)
```

APK en: `app/build/outputs/apk/`

### Instalar por ADB

```bash
adb connect <ip-del-tv>
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Uso

1. Arranca el contenedor Docker de AceStream (ver Configuración del Servidor arriba)
2. Instala la app en tu dispositivo Android TV
3. Abre los ajustes y configura la dirección del motor a `http://<ip-del-servidor>:6878`
4. Haz clic en cualquier enlace `acestream://` — la app lo intercepta y comienza la reproducción en VLC

## Licencia

GNU General Public License v3.0 — ver [LICENSE](LICENSE)
