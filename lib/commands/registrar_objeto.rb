# frozen_string_literal: true

# Comando para registrar un objeto y su lugar
module Commands
  module RegistrarObjeto
    def self.register(bot)
      bot.register_command('/RegistrarObjeto',
                           nombre: '¿Qué objeto quieres registrar?',
                           lugar: '¿En qué lugar suele estar?') do
        modelo_objetos = ModeloObjetos.new(metadata)
        name = params[:nombre]
        place = params[:lugar]
        if modelo_objetos.buscar(name: name)
          send_message('ya existe este objeto')
          next
        end
        modelo_objetos.crear(name: name, place: place)
        send_message("Objeto #{name} registrado")
      end
    end
  end
end