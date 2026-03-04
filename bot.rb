# frozen_string_literal: true

# Practicamente los comandos del bot, ahorita está asi pero se le añadirá un controlador para separar la lógica de los comandos, y así tener un código más limpio y organizado.
# enable_command_help!
# futuramente añadir hint: 'Registra un nuevo objeto'
require_relative 'models/modelo_objetos'
class Trackit < Kybus::Bot::Base
  def initialize(configs)
    super(configs)
    register_command('/RegistrarObjeto', nombre: '¿Qué objeto quieres registrar?', lugar: '¿En qué lugar suele estar?') do # esto es la vista
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

    register_command('/VerObjetos') do
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

    register_command('/EliminarObjeto', nombre: '¿Qué objeto quieres eliminar?') do
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
