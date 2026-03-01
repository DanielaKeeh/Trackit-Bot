require './main'

def lambda_handler(event:, context:)
  secret_token = ENV['SECRET_TOKEN']
  header_token = event.dig('headers', 'x-telegram-bot-api-secret-token')

  unless header_token == secret_token
    return { statusCode: 403, body: JSON.generate('Forbidden') }
  end

  body = JSON.parse(event['body'])

  BOT.handle_message(body)
  { statusCode: 200, body: '' }
end
