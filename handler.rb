# frozen_string_literal: true

require './main'

def lambda_handler(event:, _context:)
  secret_token = ENV.fetch('SECRET_TOKEN', nil)
  header_token = event.dig('headers', 'x-telegram-bot-api-secret-token')

  return { statusCode: 403, body: JSON.generate('Forbidden') } unless header_token == secret_token

  body = JSON.parse(event['body'])

  BOT.handle_message(body)
  { statusCode: 200, body: '' }
end
