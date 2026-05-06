# frozen_string_literal: true

# Comando para buscar un objeto y ver su lugar
module Commands
  module BuscarObjeto
    def self.register(bot)
      bot.register_command('/BuscarObjeto', nombre: '¿Qué objeto quieres buscar?') do
        modelo_objetos = ModeloObjetos.new(metadata)
        name = params[:nombre]
        objeto = modelo_objetos.buscar(name: name)
        if objeto
          send_message("Encontré #{objeto[:name]} - Lugar: #{objeto[:place]}")
        else
          send_message("No encontré ningún objeto llamado #{name}")
        end
      end
    end
  end
end