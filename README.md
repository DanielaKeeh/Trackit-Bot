# TrackitBot 🤖

Bot de Telegram que te ayuda a recordar dónde dejaste tus cosas.

## Arquitectura
Telegram → API Gateway → Lambda bot_handler → DynamoDB
↑
EventBridge (cada hora) → Lambda reminder

## Comandos

| Comando | Descripción |
|---------|-------------|
| `/Registrar nombre lugar` | Registra un objeto y su lugar |
| `/Buscar nombre` | Busca dónde dejaste algo |
| `/VerObjetos` | Lista todos tus objetos |
| `/Actualizar nombre nuevo_lugar` | Cambia el lugar de un objeto |
| `/Eliminar nombre` | Elimina un objeto |
| `/Predecir nombre` | Predice dónde está algo según tu historial |
| `/Recordatorio hora` | Configura a qué hora quieres que te recuerde |
| `/Ayuda` | Muestra este menú |

## Algoritmo de predicción

Heurístico de dos niveles:
1. Filtra registros del objeto por franja horaria (madrugada/mañana/tarde/noche)
2. Predice el lugar más frecuente en esa franja
3. Si no hay datos en esa franja, usa frecuencia general

## Stack

- Ruby 3.2.2
- Kybus (bot local)
- AWS Lambda + API Gateway (webhook)
- DynamoDB (persistencia)
- EventBridge (recordatorios)

## Correr localmente

```bash
bundle install
bundle exec ruby main.rb
```

## Tests

```bash
bundle exec rake test
```