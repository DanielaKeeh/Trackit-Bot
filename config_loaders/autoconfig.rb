require 'kybus/bot'
require 'kybus/configs'


Dir[File.join(__dir__, './models', '*.rb')].each { |file| require file }

require_relative '../bot'

CONF_MANAGER = Kybus::Configuration.auto_load!
APP_CONF = CONF_MANAGER.configs
require_relative 'db'

require_relative "bot_builder"
