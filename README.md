# TrackitBot 🤖

A bot that helps you remember where you left your stuff. It started as a Telegram bot and has since grown into a small ecosystem: the original bot, a REST API in Kotlin, and a native Android app that talks to that API.

<img width="738" height="1600" alt="WhatsApp Image 2026-07-13 at 10 02 32 AM" src="https://github.com/user-attachments/assets/27ad10d5-9f18-4c21-9e08-bb97db619e1e" />


## ⚠️ Before anything else: rotate the Telegram token

`config/config.yaml` is in `.gitignore`, but if this repo (or some zip of the project) was ever shared with the `bot_token` or the webhook's `secret_token` in plain text, anyone who saw it could have copied it. Go to **@BotFather → `/revoke`** (or `/token`) to generate a new one before continuing to use the bot, and keep credentials only in environment variables or in a git-ignored file.

## 🧩 What's in this repo

| Component | What it is | Stack |
|---|---|---|
| **Main bot** (`bot.rb`, `main.rb`, `lib/`, `models/`) | The Telegram bot, built with the **Kybus Bot** framework | Ruby 3.2.2 |
| **`lambda/`** | An alternate implementation of the bot as pure AWS Lambda functions (no Kybus), meant to be deployed directly with API Gateway + DynamoDB | Ruby + AWS SDK |
| **`trackit-api/`** | A translation of the bot's logic into a **REST API**, backend for the Android app | Kotlin + Ktor |
| **`trackit-android/`** | A native app that replaces the Telegram commands with a visual interface | Kotlin + XML Views |

<img width="1179" height="1316" alt="image" src="https://github.com/user-attachments/assets/d38a9d94-1ee3-413f-b268-e2904d59822d" />


## 🏗️ Architecture

```
Telegram → API Gateway → Lambda bot_handler → DynamoDB
                                                  ↑
                        EventBridge (hourly) → Lambda reminder

Android App → Trackit API (Ktor) → SQLite (dev) / DynamoDB (prod)
```

The bot and the API share the same business logic (register, search, update, delete, and predict an object's location); the API is that same logic rewritten in Kotlin so the Android app doesn't depend on Telegram.

## 💬 Bot commands

| Command | Description |
|---------|-------------|
| `/RegistrarObjeto` | Registers an object and the place it's usually kept |
| `/BuscarObjeto` | Looks up where you left something |
| `/VerObjetos` | Lists all your registered objects |
| `/ActualizarObjeto` | Changes the location of an existing object |
| `/EliminarObjeto` | Deletes a registered object |
| `/PredecirObjeto` | Predicts where something is based on your history |
| `/Recordatorio hora` | Sets what time you want to be reminded about your objects (only in `lambda/`) |
| `/Ayuda` | Shows the command menu |

> Note: the Kybus Bot version (`bot.rb`) uses CamelCase command names (`/RegistrarObjeto`); the pure Lambda version (`lambda/bot_handler`) also accepts shorter aliases like `/Registrar name place`.

## 🔮 Prediction algorithm

A two-level heuristic (lives in `lib/predictor.rb`, with its Kotlin equivalent in `trackit-api/.../predictor/Predictor.kt`):

1. Filters the object's records by time-of-day slot (early morning / morning / afternoon / night).
2. Predicts the most frequent location within that slot.
3. If there's no data for that slot, falls back to the object's overall frequency.

The API also includes an **Akinator mode**: a guided question flow that stops asking as soon as it reaches high enough confidence in the answer, instead of always asking the same things.

## ▶️ How to run each part

### Main bot (Telegram, local)

```bash
bundle install
bundle exec ruby main.rb
```

Requires Ruby 3.2.2 and your own `config/config.yaml` (use `config/config.default.yaml` as a template) with your bot token.

### Bot tests

```bash
bundle exec rake test
```

### Trackit API (Kotlin + Ktor)

```bash
cd trackit-api/trackit-api
export JWT_SECRET="something-long-and-random"   # optional in dev, required in prod
./gradlew run
```

Runs on `http://localhost:8080` and automatically creates a `trackit.db` (SQLite) file the first time you start the server.

```bash
curl http://localhost:8080/health
# {"status":"ok"}
```

### Trackit Android

1. Open the `trackit-android` folder directly in Android Studio.
2. Run `trackit-api` first (see above), and check `API_BASE_URL` in `app/build.gradle.kts`:
   - Android emulator → `http://10.0.2.2:8080/` (already set by default)
   - Physical phone on the same network → your local IP
   - Deployed API → the public URL
3. Hit Run on the app. The first screen asks you to sign up / log in.

<img width="1179" height="1322" alt="image" src="https://github.com/user-attachments/assets/697d71d3-6839-481e-9312-884371855dd3" />
<img width="1179" height="1314" alt="image" src="https://github.com/user-attachments/assets/9c646c06-959a-49d5-b3c4-5dcd6a7f05ec" />



## ☁️ AWS deployment (original bot)

The bot uses **Kybus CLI** to deploy as Lambda + DynamoDB + API Gateway in webhook mode. The configuration lives in `kybusbot.yaml`. See `kynusbot doc.md` in this repo for the full details of the Kybus Bot framework (commands, persistent state, pagination, async jobs, etc.).

## 🛠️ Overall stack

- **Ruby 3.2.2** + Kybus Bot (Telegram bot)
- **AWS Lambda + API Gateway** (webhook) + **DynamoDB** (persistence) + **EventBridge** (reminders)
- **Kotlin + Ktor + Exposed** (REST API)
- **Kotlin + Android Views + Retrofit** (Android app)

## 📌 Notes and next steps

- Each object's history only stores its *current* location; a natural next step is to keep a log of locations to improve predictions with real usage.
- The Android app's JWT is currently stored in plain `SharedPreferences`; for real production use it's worth migrating to `EncryptedSharedPreferences`.
- The API's CORS is currently open (`anyHost()`) for development; it needs to be restricted before deploying to production.
- Real local notifications (`AlarmManager` / `WorkManager`) for reminders still need to be implemented in the app.

---

**— Dan**
