# Lapwise backend

Spring Boot API for the Lapwise iOS app: Strava OAuth, on-demand swim sync, optional AI insight on each new swim.

This repo is the API only. The iOS client is [lapwise-frontend](https://github.com/vicenzorm/lapwise-frontend).

The API is not meant to sit on the public internet. Run it on your Mac. Use the Simulator with `localhost`, or a physical iPhone over Tailscale (private network, not Funnel).

## What you need

- macOS
- [Java 25](https://adoptium.net/) (the `pom.xml` pins this; `./mvnw` uses whatever `java` is on your `PATH`)
- Docker Desktop (Postgres via `compose.yaml`)
- A [Strava API application](https://www.strava.com/settings/api) (client id, secret, and a callback URL)
- An [OpenRouter](https://openrouter.ai/) key if you want insights during `POST /sync` (without it, backfill returns `503` `insight_unavailable`)
- [Tailscale](https://tailscale.com/download) on the Mac **and** the iPhone, same account — only if you want Swagger or the iOS app on a real phone

## 1. Clone and env

```bash
cd lapwise-backend
cp .env.example .env
```

Edit `.env`. Do not commit it.

| Variable | What to put |
|---|---|
| `DB_USERNAME` / `DB_PASSWORD` | Keep `lapwise` / `lapwise`. Compose uses these to create Postgres. Spring currently connects as `lapwise` / `lapwise` in `application.properties` — they have to match. |
| `STRAVA_CLIENT_ID` / `STRAVA_CLIENT_SECRET` | From the Strava API application |
| `STRAVA_REDIRECT_URI` | `http://localhost:8080/auth/strava/callback` for Mac/Simulator. Add the Tailscale HTTPS callback later for a physical phone (see below). |
| `SESSION_SECRET` | At least 32 random bytes. Example: `openssl rand -base64 32`. This signs the Lapwise JWT, not the Strava token. |
| `OPENROUTER_API_KEY` | OpenRouter key, or leave blank if you are not generating insights yet |
| `OPENROUTER_MODEL` | Defaults in `.env.example` are fine |

In the Strava app settings, the **Authorization Callback Domain** / redirect URL must include whatever you put in `STRAVA_REDIRECT_URI` (localhost for the Mac, and the `*.ts.net` host if you log in from the phone).

Spring loads `.env` from the process working directory (or one level up) on boot. Restart the API after you change it.

## 2. Postgres

From the repo root:

```bash
docker compose up -d
```

That starts Postgres 17 on `localhost:5432`, database `lapwise`. Hibernate (`ddl-auto=update`) creates tables on first API boot. Stopping the API does **not** wipe data. `docker compose down -v` does.

If something else already owns port 5432, stop it or this container will fail to bind.

## 3. Run the API (Mac)

```bash
./mvnw spring-boot:run
```

Wait until the log says the app started. Then on **this Mac**:

- Swagger: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

`localhost` means this computer. That is what the iOS **Simulator** should call (`http://127.0.0.1:8080`). A physical iPhone’s `localhost` is the phone, not the Mac — use Tailscale for that.

### Login in the browser (Mac)

Do **not** use Swagger Try it out on `GET /auth/strava/authorize`. Open that URL in a real browser so the redirect and the `lapwise_oauth_state` cookie work. After Strava consent you land on `/auth/strava/callback` and get JSON with `sessionToken`. In Swagger: **Authorize** → paste that token (not a Strava access token) → call `POST /sync` and the swim routes.

`POST /sync` is blocking. Several new swims each hit Strava detail and OpenRouter; the request can take much longer than a couple of seconds.

## 4. Physical iPhone (Tailscale)

Tailscale puts the Mac and the phone on a private mesh. Nothing is exposed to the public internet. Do **not** run `tailscale funnel`.

### One-time on the Mac

1. Install Tailscale, sign in, leave it connected.
2. In the [admin console → DNS](https://login.tailscale.com/admin/dns): MagicDNS on, **HTTPS Certificates** on.
3. With the API already running on port 8080:

```bash
tailscale serve --bg 8080
tailscale serve status
```

If `tailscale` is not on your `PATH`:

`/Applications/Tailscale.app/Contents/MacOS/Tailscale`

`serve status` prints a URL like `https://your-mac.tailxxxx.ts.net`. Serve is a reverse proxy: that HTTPS host on the tailnet forwards to `http://127.0.0.1:8080`. First HTTPS request can take a few seconds while the certificate is issued.

### One-time on the iPhone

1. Install Tailscale, sign in with the **same** account.
2. VPN **on**. Enable **Use Tailscale DNS**.

### Every time you want Swagger on the phone

1. Postgres up, API running on the Mac (`./mvnw spring-boot:run`).
2. Tailscale connected on Mac and iPhone. `tailscale serve status` still shows the proxy; after a reboot run `tailscale serve --bg 8080` if it is gone.
3. On the phone, **Safari** (not Chrome) open:

`https://<the-host-from-serve-status>/swagger-ui.html`

If that fails, Tailscale is often idle on iOS — open the Tailscale app and confirm the VPN is on. Fallback to check routing (HTTP): `http://<mac-tailscale-ipv4>:8080/swagger-ui.html` (`tailscale ip -4` on the Mac). If the IP works and the `*.ts.net` name does not, turn on Use Tailscale DNS.

Mac sleep stops the API. Postgres data stays on disk.

### iOS app from Xcode (physical iPhone)

Two separate pipes. Mixing them up is why this feels confusing.

| Pipe | What it is for |
|---|---|
| **USB (or Xcode wireless debugging)** | Xcode installs the `.app` on the phone and attaches the debugger. The phone does **not** talk to Spring this way. |
| **Tailscale** | `URLSession` / `ASWebAuthenticationSession` in the app reach Spring at `https://your-mac.tailxxxx.ts.net`. |

The iOS project is [lapwise-frontend](https://github.com/vicenzorm/lapwise-frontend). Xcode, signing, Simulator vs device, and where `baseURL` lives are in that README. `LiveAPIClient` should use one base URL: Serve HTTPS on a real device, `http://127.0.0.1:8080` in the Simulator.

```swift
#if targetEnvironment(simulator)
let baseURL = URL(string: "http://127.0.0.1:8080")!
#else
let baseURL = URL(string: "https://your-mac.tailxxxx.ts.net")! // copy from `tailscale serve status`
#endif
```

No port on the HTTPS URL (Serve listens on 443). Paths stay `/sync`, `/swim-activities`, and so on. Do not use `localhost` in the device build.

**Before Xcode**

1. Postgres up, `./mvnw spring-boot:run` on the Mac.
2. `tailscale serve --bg 8080` and confirm `tailscale serve status`.
3. Tailscale **on** on the Mac and the iPhone (VPN toggle + Use Tailscale DNS).
4. On the phone, Safari: `https://<serve-host>/swagger-ui.html` must load. If Safari cannot see Swagger, the app will not either — fix Tailscale first.

**Xcode**

1. Cable the iPhone to the Mac (or set up wireless debugging after pairing once). Unlock the phone; Trust This Computer if asked.
2. On the iPhone: **Settings → Privacy & Security → Developer Mode** on (iOS 16+), then reboot if it asks.
3. Open `lapwise-frontend` in Xcode. Top of the window: set the run destination to **your iPhone**, not a Simulator.
4. **Signing & Capabilities**: pick your Team (Personal Team is enough for your own phone). Bundle id must be unique if Apple rejects the default.
5. Paste the Serve origin into the device `baseURL` (the `tailxxxx.ts.net` host from `tailscale serve status`). Rebuild if you change it.
6. Run (⌘R). First time, the phone may say Untrusted Developer: **Settings → General → VPN & Device Management** → trust your Apple ID, then Run again.

Keep the Tailscale app connected **while Lapwise is in the foreground**. iOS may idle the VPN; if requests fail, open Tailscale, confirm the switch is on, retry. You do not start Tailscale from Xcode — it is just another app on the phone.

Serve HTTPS means App Transport Security is satisfied. If you point the app at `http://100.x.x.x:8080` instead, iOS will block cleartext unless you add an ATS exception — prefer Serve.

**Simulator in Xcode:** destination = iPhone Simulator, `baseURL` = `http://127.0.0.1:8080`, Tailscale not required.

For Strava login **from the phone**, `STRAVA_REDIRECT_URI` and the Strava app settings must use the same host the phone can load, e.g. `https://your-mac.tailxxxx.ts.net/auth/strava/callback`. Strava only redirects the browser; the phone, already on Tailscale, fetches that callback. Keep the localhost URI for Simulator/Mac.

## 5. Stop

- API: Ctrl+C in the `spring-boot:run` terminal.
- Serve (optional): `tailscale serve reset`
- Postgres (keeps data): `docker compose stop`  
  Wipe the database: `docker compose down -v`

## Routes the iOS app uses

| Method | Path | Auth |
|---|---|---|
| `GET` | `/auth/strava/authorize` | Public (browser) |
| `GET` | `/auth/strava/callback` | Public (Strava redirect) |
| `POST` | `/sync` | Bearer Lapwise JWT |
| `GET` | `/swim-activities?cursor=&limit=` | Bearer |
| `GET` | `/swim-activities/{id}` | Bearer (splits + `insight`, which may be `null`) |

List has no splits or insight. Missing usable splits skip the insight row; they do not `422` `/sync`.

| Status | `error` code |
|---|---|
| `401` | session JWT missing or invalid |
| `429` | `strava_rate_limited` or `insight_rate_limited` |
| `422` | unusable Strava payload (e.g. incomplete token) |
| `502` | `strava_unavailable` |
| `503` | `insight_unavailable` |

Architecture and package rules: [`AGENTS.md`](AGENTS.md).
