# Kybus Bot

Kybus Bot is a Ruby framework to build chat bots with a shared command DSL and multiple providers.
It includes state management, async job forking, and a UX layer for pagination and buttons.

## Features
- Provider adapters: Telegram and Discord.
- Command DSL with params, redirects, and replies.
- Persistent state and metadata per channel.
- Pagination UX with inline keyboards in Telegram.
- Optional button rendering helper.
- Forkers for async jobs.
- Test helpers with a debug adapter.

## Requirements
- Ruby 3.x
- A storage backend for `bot_sessions` (Sequel or Dynamoid)

## Installation
Add to your Gemfile:

```ruby
gem 'kybus-bot'
```

Then install:

```bash
bundle install
```

## Quick Start (Telegram)

```ruby
require 'kybus/bot'

conf = {
  'name' => 'testbot',
  'state_repository' => {
    'name' => 'sequel',
    'endpoint' => 'sqlite://storage/kybus-bot.db'
  },
  'pool_size' => 1,
  'provider' => {
    'name' => 'telegram',
    'token' => 'YOUR_TELEGRAM_BOT_TOKEN',
    'parse_mode' => 'MarkdownV2'
  }
}.freeze

Kybus::Bot::Migrator.run_migrations!(conf['state_repository'])

bot = Kybus::Bot::Base.new(conf)

bot.register_command('/hello') do
  send_message('hello human')
end

bot.run
```

## Configuration

### Core Settings
```ruby
conf = {
  'name' => 'mybot',
  'pool_size' => 2,
  'inline_args' => true,
  'state_repository' => {
    'name' => 'sequel',
    'endpoint' => 'sqlite://storage/kybus-bot.db'
  },
  'provider' => {
    'name' => 'telegram',
    'token' => 'TOKEN',
    'parse_mode' => 'MarkdownV2'
  }
}
```

Key fields:
- `inline_args`: enables inline parsing like `/cmdA__B`.
- `pool_size`: number of worker loops.
- `state_repository`: where `bot_sessions` live.
- `provider`: adapter configuration.

### Telegram Adapter
```ruby
conf['provider'] = {
  'name' => 'telegram',
  'token' => 'TOKEN',
  'parse_mode' => 'MarkdownV2'
}
```

### Discord Adapter
```ruby
conf['provider'] = {
  'name' => 'discord',
  'token' => 'TOKEN'
}
```

## State Repository

### Sequel
```ruby
conf['state_repository'] = {
  'name' => 'sequel',
  'endpoint' => 'sqlite://storage/kybus-bot.db'
}
Kybus::Bot::Migrator.run_migrations!(conf['state_repository'])
```

### DynamoDB (Dynamoid)
```ruby
conf['state_repository'] = {
  'name' => 'dynamoid',
  'access_key' => 'AKIA...',
  'secret_key' => '...',
  'region' => 'us-east-1',
  'endpoint' => 'https://dynamodb.us-east-1.amazonaws.com',
  'namespace' => 'kybus',
  'read_capacity' => 1,
  'write_capacity' => 1
}
Kybus::Bot::Migrator.run_migrations!(conf['state_repository'])
```

## Running

Long-polling (default):
```ruby
bot.run
```

Webhook mode:
```ruby
post '/webhook' do
  bot.handle_message(JSON.parse(request.body.read))
  status 200
end
```

## Kybus CLI Integration

Kybus CLI can bootstrap new bot projects and deploy them to AWS using Lambda + DynamoDB.  
Key commands (from `kybus-cli`):
- `bot init NAME` (requires `--db-adapter`): scaffolds a new bot project.
- `bot add_controller NAME`: adds a controller to an existing bot.
- `bot deploy-init NAME`: generates a deployment file for AWS.
- `bot deploy`: packages and deploys to AWS Lambda.
- `bot destroy`: removes the AWS deployment.

Typical flow:
1. Initialize a new project:
   - `kybus bot init my_bot --db-adapter dynamoid --with-deployment-file --cloud-provider aws`
2. Generate deployment config:
   - `kybus bot deploy-init my_bot --dynamo-capacity on_demand --dynamo-table bot_sessions`
3. Deploy:
   - `kybus bot deploy`

Deployment files:
- `kybusbot.yaml`: used by the deployer.
  - Includes `cloud_provider`, `dynamo.capacity`, `dynamo.table_name`, `chat_provider`, and tokens.

AWS deployment details (from `kybus-cli`):
- Creates/updates Lambda, IAM Role, DynamoDB policy, CloudWatch log group.
- If `forker.queue` is configured, creates an SQS queue.
- Deploys a secondary Lambda for SQS job processing when a queue is present.

Once deployed, the bot runs in webhook mode and uses DynamoDB for `bot_sessions`.

## Sidekiq Mode

Kybus Bot supports running command execution through Sidekiq.  
Enable it with:

```ruby
conf['sidekiq'] = true
```

When enabled, commands are enqueued to Sidekiq and executed asynchronously.
Note: Kybus CLI does not configure Sidekiq; it must be set up separately in your app/infrastructure.

### Running with Sidekiq

You still run the bot process (it reads provider messages and enqueues jobs), and you also run Sidekiq workers to execute commands.

1. Configure Sidekiq + Redis in your app:
```ruby
require 'sidekiq'

Sidekiq.configure_client do |config|
  config.redis = { url: ENV.fetch('REDIS_URL', 'redis://localhost:6379/0') }
end

Sidekiq.configure_server do |config|
  config.redis = { url: ENV.fetch('REDIS_URL', 'redis://localhost:6379/0') }
end
```

2. Start Redis (example with Docker):
```bash
docker run --rm -p 6379:6379 redis:7
```

3. Run the bot process:
```ruby
bot.run
```

4. Run Sidekiq workers in a separate process:
```bash
bundle exec sidekiq -r ./main.rb
```

### Redis Notes
- Set `REDIS_URL` to point at your Redis instance.
- Use a dedicated Redis DB for bots if you share Redis across services.

## Commands

Register a command:
```ruby
bot.register_command('/ping') do
  send_message('pong')
end
```

Command with params:
```ruby
bot.register_command('/sum', %i[a b]) do
  send_message((params[:a].to_i + params[:b].to_i).to_s)
end
```

Inline args (when `inline_args` is true):
```ruby
bot.register_command('/hello', %i[number letter]) do
  send_message("N=#{params[:number]}, L=#{params[:letter]}")
end
# /hello8__a
```

Replies:
```ruby
bot.register_command('/ask') { send_message('Answer?') }

bot.register_command('default') do
  if last_message.reply?
    send_message("Reply: #{last_message.raw_message}")
  end
end
```

## Pagination UX

Use `define_paginated_query` to handle commands with previous/next buttons.

```ruby
bot.define_paginated_query('/history', params: [:room_id], per_page: 20) do |dsl, room_id, page, per_page|
  # Return a hash with header/body and pagination controls
  {
    header: "History #{room_id}",
    body: "...",
    total_pages: 5,
    key: "history:#{room_id}",
    prev_cmd: "/history#{room_id}__#{page - 1}",
    next_cmd: "/history#{room_id}__#{page + 1}"
  }
end
```

Telegram behavior:
- Non-callback invocation sends a placeholder message, edits it in place, and injects `mid` into the callback data.
- Callback invocation edits the specific message id derived from the callback data.

## Buttons Helper

Send buttons with a single call:
```ruby
send_message_with_buttons(
  'Choose an option',
  [
    ['History', '/historyROOM'],
    ['Files', '/filesROOM']
  ]
)
```

Base UX output:
```
Choose an option
History - /historyROOM
Files - /filesROOM
```

Telegram UX output:
- Inline keyboard buttons with `callback_data` set to the commands.

## Async Jobs (Forkers)

Register jobs:
```ruby
register_job('my_job', args: { user_id: 'string' }) do |args|
  send_message("Job for #{args[:user_id]}")
end
```

Invoke jobs:
```ruby
fork('my_job', user_id: '123')
```

Lambda SQS forker:
```ruby
conf['forker'] = {
  'provider' => 'sqs',
  'queue' => 'MyQueue'
}
```

## DSL Reference

Main DSL methods available inside command blocks:

| Method | Description |
| --- | --- |
| `send_message(content, channel = nil)` | Send a text message. If `channel` is nil, uses the current channel. |
| `send_message_with_buttons(content, buttons, channel = nil)` | Send a message with buttons. Base UX prints lines, Telegram uses inline buttons. |
| `send_image(content, channel = nil, caption: nil)` | Send an image file or URL with optional caption. |
| `send_video(content, channel = nil, caption: nil)` | Send a video file or URL with optional caption. |
| `send_audio(content, channel = nil)` | Send an audio file or URL. |
| `send_document(content, channel = nil)` | Send a document file or URL. |
| `params` | Hash of parsed command parameters. |
| `files` | Hash of uploaded files keyed by param name. |
| `metadata` | Hash of persisted state for the current channel. |
| `file(name)` | Fetch a file builder from `files`. |
| `mention(name)` | Build a mention for a user. |
| `current_user` | Returns the current user identifier from the provider. |
| `current_channel` | Returns the current channel identifier. |
| `last_message` | Returns the last message object. |
| `is_private?` | True when the message is private. |
| `command_name` | Current command name. |
| `save_metadata!` | Persist `metadata` to the state repository. |
| `redirect(command, *params)` | Redirect to another command with params. |
| `abort(msg = nil)` | Abort execution with an optional message. |
| `fork(command, arguments = {})` | Enqueue a background job with arguments. |
| `fork_with_delay(command, delay, arguments = {})` | Enqueue a background job after a delay. |

## Testing

Use the built-in test helpers:
```ruby
require 'kybus/bot/test'
```

Examples:
```ruby
bot = Kybus::Bot::Base.make_test_bot
bot.register_command('/ping') { send_message('pong') }
bot.expects(:send_message).with('pong')
bot.receives('/ping')
```

Inline args:
```ruby
bot = Kybus::Bot::Base.make_test_bot('inline_args' => true)
bot.register_command('/hello', %i[number letter]) { send_message("#{params[:number]}#{params[:letter]}") }
bot.expects(:send_message).with('8a')
bot.receives('/hello8__a')
```

Reply handling:
```ruby
bot.register_command('/reply') { send_message('Hello') }
bot.register_command('default') do
  if last_message.reply?
    send_message("Reply: #{last_message.raw_message}")
  end
end
bot.receives('/reply')
bot.expects(:send_message).with('Reply: World')
bot.replies('World')
```

## RDoc
Generate API docs locally:
```bash
bundle exec rake rdoc
```

## License
See `LICENSE.txt`.
