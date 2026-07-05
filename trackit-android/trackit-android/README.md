# Trackit Android (Kotlin + XML Views)

App nativa que consume la REST API de `trackit-api`. Reemplaza los comandos
de Telegram por una interfaz visual propia (sección 3 y 9.1 del protocolo),
con 4 pantallas:

| Pantalla | Equivale a |
|---|---|
| **Dashboard** | Resumen: conteo de objetos/recordatorios, próximos recordatorios, objetos recientes |
| **Objetos** | `/RegistrarObjeto`, `/BuscarObjeto`, `/ActualizarObjeto`, `/EliminarObjeto`, `/VerObjetos` |
| **Recordatorios** | `/Recordatorio hora` |
| **Chat** | El `ChatbotView` original, ahora hablando con la REST API en vez de Telegram |

## Cómo abrirlo

1. Abre la carpeta `trackit-android` directo en Android Studio (`File > Open`).
   Android Studio va a ofrecerte configurar el Gradle Wrapper automáticamente
   la primera vez — acéptalo.
2. Antes de correr la app, revisa `app/build.gradle.kts` → `API_BASE_URL`:
   - Emulador de Android Studio → `http://10.0.2.2:8080/` (ya viene así)
   - Celular físico en tu misma red Wi-Fi → cambia a `http://TU_IP_LOCAL:8080/`
   - API desplegada en AWS → la URL pública real
3. Corre primero `trackit-api` (`./gradlew run` en esa carpeta), luego dale Run
   a esta app.
4. La primera pantalla pide registro/login — crea un usuario de prueba.

## Arquitectura de la app (capa de presentación)

```
data/
├── local/SessionManager.kt      # guarda el JWT en SharedPreferences
├── remote/                       # Retrofit: interfaces + DTOs + cliente con interceptor JWT
├── repository/                   # una clase por módulo del API, cada una expone ApiResult<T>
└── ServiceLocator.kt              # localizador simple de dependencias (sin Hilt/Dagger)

ui/
├── auth/        LoginActivity                        (login/registro)
├── main/        MainActivity + BottomNavigationView    (host de las 4 pantallas)
├── dashboard/   DashboardFragment + ViewModel
├── objects/     ObjectsFragment + ViewModel + Adapter
├── reminders/   RemindersFragment + ViewModel + Adapter
└── chat/        ChatFragment + ViewModel + ChatEngine (intérprete de comandos) + Adapter
```

Cada pantalla sigue el mismo patrón: **Fragment (XML + ViewBinding) → ViewModel
(LiveData) → Repository (Retrofit) → API**. No hay Hilt/Dagger a propósito,
para mantener el proyecto entendible; `ServiceLocator` hace ese trabajo a mano.

## La pieza importante: `ChatEngine`

`ui/chat/ChatEngine.kt` es el intérprete de comandos que reemplaza lo que
antes hacía Kybus Bot con las rutas de Telegram. Traduce texto como
`/Registrar café mesa` a una llamada real a `POST /api/objects`, y también
maneja el flujo guiado de `/Akinator nombre` (llama a
`/api/predict/akinator/start` y `/answer`, mostrando cada pregunta como
mensaje del bot hasta llegar a una respuesta con nivel de confianza).

Este intérprete vive en la app (capa de presentación) y no en el backend —
así, si más adelante agregan un cliente web o iOS, no tienen que hablar en
sintaxis de comandos de chat para usar el API.

## Notas y siguientes pasos

- **Seguridad del token:** el JWT se guarda en `SharedPreferences` normal.
  Para producción real, migra a `EncryptedSharedPreferences`
  (`androidx.security:security-crypto`) — se dejó simple aquí por alcance
  del proyecto escolar.
- **Notificaciones push reales:** el módulo de Recordatorios hoy solo hace
  CRUD contra el API; todavía falta programar notificaciones locales
  (`AlarmManager` / `WorkManager`) que lean estos recordatorios y avisen al
  usuario a la hora configurada — es el siguiente paso natural de la Fase 7
  del cronograma ("Integración de notificaciones push nativas").
- **Diseño de Figma:** estos layouts XML son funcionales pero no pasaron por
  un diseño en Figma todavía (sección 8 del protocolo lo pide). Si generan
  wireframes después, esta estructura de pantallas/ViewModels no cambia —
  solo se reemplazan los XML.
