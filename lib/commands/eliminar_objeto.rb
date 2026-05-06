# frozen_string_literal: true

# Comando para eliminar un objeto registrado
module Commands
  module EliminarObjeto
    def self.register(bot)
      bot.register_command('/EliminarObjeto', nombre: '¿Qué objeto quieres eliminar?') do
        modelo_objetos = ModeloObjetos.new(metadata)
        name = params[:nombre]
        if modelo_objetos.buscar(name: name)
          modelo_objetos.eliminar(name: name)
          send_message("Objeto #{name} eliminado correctamente")
        else
          send_message("No encontré ningún objeto llamado #{name}")
        end
      end
    end
  end
end