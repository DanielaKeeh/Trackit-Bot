# frozen_string_literal: true

# Comando para listar todos los objetos registrados
module Commands
  module VerObjetos
    def self.register(bot)
      bot.register_command('/VerObjetos') do
        modelo_objetos = ModeloObjetos.new(metadata)
        listado = modelo_objetos.listar
        if listado.empty?
          send_message('No tienes objetos registrados')
        else
          message = "Tus objetos registrados:\n"
          listado.each_with_index do |obj, index|
            message += "#{index + 1}. #{obj[:name]} - Lugar: #{obj[:place]}\n"
          end
          send_message(message)
        end
      end
    end
  end
end