# Trackit API (Kotlin + Ktor)

Traducción de la lógica de negocio del bot original (`Trackit-bot`, Ruby +
Kybus Bot + AWS Lambda) a una **REST API en Kotlin**, según la sección 6.2 y
9.2 del protocolo de proyecto. Esta API es el backend que consumirá la app
nativa de Android.

## ⚠️ Antes de nada: rota el token de Telegram

En el zip del bot original (`kybusbot.yaml`) venía el `bot_token` de Telegram
y el `secret_token` del webhook **en texto plano**. Si ese repo llegó a
subirse a GitHub, cualquiera pudo verlo. Ve a **@BotFather → /revoke** (o
`/token`) para generar uno nuevo y no lo vuelvas a commitear directo al
repo — usa variables de entorno o un `.gitignore` sobre el archivo de config.

## Requisitos

- JDK 17+
- Gradle (o usa el wrapper una vez que lo generes: `gradle wrapper`)
- No necesitas Docker ni AWS para desarrollo local: usa SQLite automáticamente.

## Cómo correrlo

```bash
export JWT_SECRET="algo-largo-y-aleatorio"   # opcional en dev, obligatorio en prod
./gradlew run
```

El servidor levanta en `http://localhost:8080`. Se crea un archivo
`trackit.db` (SQLite) en la raíz del proyecto la primera vez que corres la app.

Prueba rápida:
```bash
curl http://localhost:8080/health
# {"status":"ok"}
```

## Estructura del proyecto

```
src/main/kotlin/com/trackit/
├── Application.kt          # main() + wiring de plugins y rutas
├── plugins/
│   ├── Security.kt         # JWT (equivalente al SECRET_TOKEN del webhook)
│   ├── Serialization.kt    # JSON (kotlinx.serialization)
│   ├── Databases.kt        # conexión SQLite vía Exposed
│   └── StatusPages.kt      # errores -> JSON consistente
├── models/
│   ├── Tables.kt           # esquema Exposed (Users, TrackedObjects, Reminders, Tasks)
│   └── Dtos.kt              # objetos de request/response
├── repositories/            # capa de acceso a datos (CRUD), una clase por módulo
├── predictor/
│   ├── Predictor.kt         # port 1:1 de lib/predictor.rb (franja horaria + frecuencia)
│   └── AkinatorEngine.kt    # flujo de preguntas guiadas con nivel de confianza
└── routes/                  # un archivo por grupo de endpoints
```

## Especificación de la API (Fase 1 del cronograma)

Todas las rutas bajo `/api/objects`, `/api/reminders`, `/api/tasks` y
`/api/predict` requieren el header:

```
Authorization: Bearer <token>
```

### Auth — `/api/auth`

| Método | Ruta | Body | Descripción |
|---|---|---|---|
| POST | `/api/auth/register` | `{ "username": "...", "password": "..." }` | Crea usuario, devuelve token |
| POST | `/api/auth/login` | `{ "username": "...", "password": "..." }` | Devuelve token |

### Objetos — `/api/objects` (equivalente a /RegistrarObjeto, /BuscarObjeto, etc.)

| Método | Ruta | Equivale a | Body |
|---|---|---|---|
| GET | `/api/objects` | `/VerObjetos` | — |
| GET | `/api/objects/{name}` | `/BuscarObjeto` | — |
| POST | `/api/objects` | `/RegistrarObjeto` | `{ "name": "...", "place": "..." }` |
| PUT | `/api/objects/{name}` | `/ActualizarObjeto` | `{ "place": "..." }` |
| DELETE | `/api/objects/{name}` | `/EliminarObjeto` | — |

### Recordatorios — `/api/reminders` (evoluciona /Recordatorio hora)

| Método | Ruta | Body |
|---|---|---|
| GET | `/api/reminders` | — |
| POST | `/api/reminders` | `{ "message": "...", "hour": "HH:mm", "recurring": false, "daysOfWeek": null }` |
| PUT | `/api/reminders/{id}` | campos parciales a actualizar |
| DELETE | `/api/reminders/{id}` | — |

### Pendientes — `/api/tasks` (módulo nuevo, sección 3.1)

| Método | Ruta | Body |
|---|---|---|
| GET | `/api/tasks` | — |
| POST | `/api/tasks` | `{ "description": "...", "dueDate": null }` |
| PUT | `/api/tasks/{id}` | `{ "description"?, "done"?, "dueDate"? }` |
| DELETE | `/api/tasks/{id}` | — |

### Predicción — `/api/predict` (equivalente a /PredecirObjeto + modo Akinator)

| Método | Ruta | Body | Descripción |
|---|---|---|---|
| GET | `/api/predict/{name}` | — | Heurístico directo, sin preguntas (igual que el bot) |
| POST | `/api/predict/akinator/start` | `{ "objectName": "..." }` | Primer paso del flujo guiado |
| POST | `/api/predict/akinator/answer` | `{ "objectName": "...", "step": 1, "answers": {"franja": "tarde"} }` | Siguiente paso o resultado final |

La respuesta de Akinator es adaptativa: si con las respuestas ya hay
confianza ≥ 70%, el servidor responde `finished: true` con el lugar sugerido
en vez de seguir preguntando — igual que el juego real deja de preguntar
cuando ya está seguro.

## Notas de diseño importantes

1. **Historial de un solo registro por objeto.** Igual que el bot original
   (`ModeloObjetos`/`BaseModel` en Ruby), cada objeto guarda solo su
   ubicación *actual*; al actualizar se sobreescribe, no se acumula
   historial. Esto significa que hoy el análisis por franja horaria y el
   modo Akinator tienen poca variedad de datos reales para comparar. Un
   siguiente paso natural (Fase 5 del cronograma) es guardar un log de
   ubicaciones por objeto en vez de solo la última, para que la predicción
   mejore con el uso real.
2. **JWT en vez de `SECRET_TOKEN` de webhook.** El bot validaba un secreto
   fijo en cada request de Telegram; la API ahora autentica usuarios reales
   con contraseña (PBKDF2, sin dependencias externas) y emite JWT con
   expiración de 7 días.
3. **SQLite hoy, DynamoDB después.** Los repositorios (`repositories/*.kt`)
   son la única capa que toca la base de datos. Para migrar a DynamoDB en
   producción (sección 7.3), se reemplaza la implementación interna de cada
   repositorio manteniendo la misma firma de métodos (`crear`, `buscar`,
   `actualizar`, `eliminar`, `listar`); las rutas no cambian.
4. **CORS abierto (`anyHost()`).** Está bien para desarrollo, pero antes de
   desplegar a producción hay que restringirlo al dominio real o quitarlo
   si solo la app Android (no un navegador) va a consumir la API.

## Próximos pasos sugeridos

- Generar el Gradle wrapper (`gradle wrapper`) para no depender de tener
  Gradle instalado localmente.
- Escribir tests unitarios para `Predictor` y `AkinatorEngine` (son puros,
  no tocan base de datos — fáciles de probar igual que `test_bot_commands.rb`).
- Definir el esquema de "historial de ubicaciones" antes de conectar la app
  Android al modo Akinator, para que las preguntas tengan datos reales que analizar.
