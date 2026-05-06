# frozen_string_literal: true

# Comando para actualizar el lugar de un objeto
module Commands
  module ActualizarObjeto
    def self.register(bot)
      bot.register_command('/ActualizarObjeto',
                           nombre: '¿Qué objeto quieres actualizar?',
                           nuevo_lugar: '¿Cuál es el nuevo lugar?') do
        modelo_objetos = ModeloObjetos.new(metadata)
        name = params[:nombre]
        nuevo_lugar = params[:nuevo_lugar]
        if modelo_objetos.buscar(name: name)
          modelo_objetos.actualizar({ name: name }, { place: nuevo_lugar })
          send_message("Objeto #{name} actualizado al lugar: #{nuevo_lugar}")
        else
          send_message("No encontré ningún objeto llamado #{name}")
        end
      end
    end
  end
end