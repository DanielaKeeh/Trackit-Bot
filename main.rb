# frozen_string_literal: true

require_relative 'config_loaders/autoconfig'
require_relative 'config_loaders/bot_builder'

BOT.run if $PROGRAM_NAME == __FILE__
