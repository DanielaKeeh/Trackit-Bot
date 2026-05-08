# frozen_string_literal: true

require 'json'
require 'net/http'
require 'uri'
require 'aws-sdk-dynamodb'

TELEGRAM_TOKEN = ENV['TELEGRAM_TOKEN']
TABLE_NAME = ENV['DYNAMODB_TABLE'] || 'trackit-objects'
REMINDERS_TABLE = ENV['DYNAMODB_REMINDERS_TABLE'] || 'trackit-reminders'
DYNAMODB = Aws::DynamoDB::Client.new(region: 'us-east-1')

def lambda_handler(event:, context:)
  body = JSON.parse(event['body'] || '{}')
  message = body['message']
  return { statusCode: 200, body: 'ok' } unless message

  chat_id = message['chat']['id'].to_s
  text = message['text'] || ''

  response = handle_command(chat_id, text)
  send_message(chat_id, response)

  { statusCode: 200, body: 'ok' }
end

def normalizar(nombre)
  nombre.downcase.strip
end

def handle_command(chat_id, text)
  command = text.split.first

  case command
  when '/start', '/ayuda', '/Ayuda'
    ayuda
  when '/registrarobjeto', '/RegistrarObjeto'
    "Usa el formato: /Registrar nombre lugar\nEjemplo: /Registrar Llaves Entrada"
  when '/registrar', '/Registrar'
    parts = text.split(' ', 3)
    return 'Formato: /Registrar nombre lugar' if parts.length < 3
    registrar(chat_id, parts[1], parts[2])
  when '/verobjetos', '/VerObjetos'
    ver_objetos(chat_id)
  when '/eliminar', '/Eliminar'
    parts = text.split(' ', 2)
    return 'Formato: /Eliminar nombre' if parts.length < 2
    eliminar(chat_id, parts[1])
  when '/buscar', '/Buscar'
    parts = text.split(' ', 2)
    return 'Formato: /Buscar nombre' if parts.length < 2
    buscar(chat_id, parts[1])
  when '/actualizar', '/Actualizar'
    parts = text.split(' ', 3)
    return 'Formato: /Actualizar nombre nuevo_lugar' if parts.length < 3
    actualizar(chat_id, parts[1], parts[2])
  when '/predecir', '/Predecir'
    parts = text.split(' ', 2)
    return 'Formato: /Predecir nombre' if parts.length < 2
    predecir(chat_id, parts[1])
  when '/recordatorio', '/Recordatorio'
    parts = text.split(' ', 2)
    return "Formato: /Recordatorio hora\nEjemplo: /Recordatorio 8 o /Recordatorio 20" if parts.length < 2
    configurar_recordatorio(chat_id, parts[1])
  else
    "No entendí ese comando. Usa /Ayuda para ver los comandos disponibles."
  end
end

def registrar(chat_id, name, place)
  name = normalizar(name)
  resultado = buscar_dynamo(chat_id, name)
  return "Ya existe un objeto llamado #{name}" if resultado

  DYNAMODB.put_item(
    table_name: TABLE_NAME,
    item: {
      user_id: chat_id,
      object_name: name,
      place: place,
      created_at: Time.now.iso8601
    }
  )
  "Objeto #{name} registrado en #{place} ✅"
end

def ver_objetos(chat_id)
  resultado = DYNAMODB.query(
    table_name: TABLE_NAME,
    key_condition_expression: 'user_id = :uid',
    expression_attribute_values: { ':uid' => chat_id }
  )
  return 'No tienes objetos registrados' if resultado.items.empty?

  mensaje = "Tus objetos registrados:\n"
  resultado.items.each_with_index do |obj, i|
    mensaje += "#{i + 1}. #{obj['object_name']} - Lugar: #{obj['place']}\n"
  end
  mensaje
end

def buscar(chat_id, name)
  name = normalizar(name)
  obj = buscar_dynamo(chat_id, name)
  return "No encontré ningún objeto llamado #{name}" unless obj

  "Encontré #{obj['object_name']} - Lugar: #{obj['place']}"
end

def eliminar(chat_id, name)
  name = normalizar(name)
  return "No encontré ningún objeto llamado #{name}" unless buscar_dynamo(chat_id, name)

  DYNAMODB.delete_item(
    table_name: TABLE_NAME,
    key: { user_id: chat_id, object_name: name }
  )
  "Objeto #{name} eliminado correctamente 🗑️"
end

def actualizar(chat_id, name, nuevo_lugar)
  name = normalizar(name)
  return "No encontré ningún objeto llamado #{name}" unless buscar_dynamo(chat_id, name)

  DYNAMODB.update_item(
    table_name: TABLE_NAME,
    key: { user_id: chat_id, object_name: name },
    update_expression: 'SET place = :p, updated_at = :u',
    expression_attribute_values: {
      ':p' => nuevo_lugar,
      ':u' => Time.now.iso8601
    }
  )
  "Objeto #{name} actualizado al lugar: #{nuevo_lugar} ✅"
end

def predecir(chat_id, name)
  name = normalizar(name)
  resultado = DYNAMODB.query(
    table_name: TABLE_NAME,
    key_condition_expression: 'user_id = :uid AND object_name = :obj',
    expression_attribute_values: { ':uid' => chat_id, ':obj' => name }
  )
  return "No tengo historial de #{name}, regístralo primero con /Registrar" if resultado.items.empty?

  obj = resultado.items.first
  "🔮 Creo que #{name} está en: #{obj['place']}"
end

def configurar_recordatorio(chat_id, hora)
  hora_int = hora.to_i
  return 'Hora inválida, usa un número entre 0 y 23' unless hora_int.between?(0, 23)

  DYNAMODB.put_item(
    table_name: REMINDERS_TABLE,
    item: {
      user_id: chat_id,
      hora: hora_int,
      created_at: Time.now.iso8601
    }
  )
  "✅ Recordatorio configurado a las #{hora_int}:00 hrs"
end

def buscar_dynamo(chat_id, name)
  resultado = DYNAMODB.get_item(
    table_name: TABLE_NAME,
    key: { user_id: chat_id, object_name: name }
  )
  resultado.item
end

def send_message(chat_id, text)
  uri = URI("https://api.telegram.org/bot#{TELEGRAM_TOKEN}/sendMessage")
  Net::HTTP.post(uri, { chat_id: chat_id, text: text }.to_json, 'Content-Type' => 'application/json')
end

def ayuda
  <<~MSG
    👋 ¡Hola! Soy TrackitBot 🤖
    Soy un bot que te ayuda a recordar dónde dejaste tus cosas.

    📋 Comandos disponibles:

    /Registrar nombre lugar — Registra un objeto
    /Buscar nombre — Busca dónde dejaste algo
    /VerObjetos — Lista todos tus objetos
    /Actualizar nombre nuevo_lugar — Cambia el lugar
    /Eliminar nombre — Elimina un objeto
    /Predecir nombre — Predice dónde está algo
    /Recordatorio hora — Configura a qué hora quieres que te recuerde
  MSG
end