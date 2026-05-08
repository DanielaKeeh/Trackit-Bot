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
  hora_actual = Time.now.utc.hour - 6 # Ajuste a hora México central
  hora_actual += 24 if hora_actual.negative?

  recordatorios = DYNAMODB.scan(
    table_name: REMINDERS_TABLE,
    filter_expression: 'hora = :h',
    expression_attribute_values: { ':h' => hora_actual }
  )

  recordatorios.items.each do |reminder|
    user_id = reminder['user_id']
    objetos = obtener_objetos(user_id)
    next if objetos.empty?

    mensaje = "⏰ ¡Recordatorio TrackitBot!\n\n"
    mensaje += "Tus objetos registrados:\n"
    objetos.each_with_index do |obj, i|
      mensaje += "#{i + 1}. #{obj['object_name']} - Lugar: #{obj['place']}\n"
    end
    mensaje += "\n💡 Usa /Buscar nombre si no encuentras algo."
    send_message(user_id, mensaje)
  end

  { statusCode: 200, body: 'Recordatorios enviados' }
end

def obtener_objetos(user_id)
  resultado = DYNAMODB.query(
    table_name: TABLE_NAME,
    key_condition_expression: 'user_id = :uid',
    expression_attribute_values: { ':uid' => user_id }
  )
  resultado.items
end

def send_message(chat_id, text)
  uri = URI("https://api.telegram.org/bot#{TELEGRAM_TOKEN}/sendMessage")
  Net::HTTP.post(uri, { chat_id: chat_id, text: text }.to_json, 'Content-Type' => 'application/json')
end