# frozen_string_literal: true

require 'simplecov'
require 'minitest/test'
require 'minitest/autorun'
require 'mocha/minitest'
require 'webmock/minitest'

# SimpleCov.minimum_coverage 100 # Esta línea es para que truene si no se alcanza el 100% de cobertura
SimpleCov.start

require 'kybus/bot/test'
require_relative '../models/modelo_objetos'
require_relative '../main'

class BotTest < Minitest::Test
  def setup
    @default_channel = "test_channel_#{rand(1000..9999)}"
    @bot ||= Trackit.make_test_bot('channel_id' => @default_channel, 'inline_args' => true)
    nil
  end

  def expects(message)
    super(message, @default_channel)
  end

  def metadata
    data = JSON.parse(File.read("./storage/debug_message__#{@default_channel}.json"), symbolize_names: true)
    meta = data[:metadata]
    meta = JSON.parse(meta, symbolize_names: true) if meta.is_a?(String)
    meta || {}
  rescue Errno::ENOENT
    {}
  end

  def guardar_metadata(metadata)
    File.write("./storage/debug_message__#{@default_channel}.json", JSON.generate({ metadata: metadata }))
  end
end
