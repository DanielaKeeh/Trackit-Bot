# frozen_string_literal: true

# Bot principal de Trackit
require_relative 'models/modelo_objetos'
require_relative 'lib/commands/registrar_objeto'
require_relative 'lib/commands/ver_objetos'
require_relative 'lib/commands/eliminar_objeto'
require_relative 'lib/commands/buscar_objeto'
require_relative 'lib/commands/actualizar_objeto'
require_relative 'lib/commands/predecir_objeto'
require_relative 'lib/predictor'
require_relative 'lib/commands/ayuda'

class Trackit < Kybus::Bot::Base
  def initialize(configs)
    super
    Commands::RegistrarObjeto.register(self)
    Commands::VerObjetos.register(self)
    Commands::EliminarObjeto.register(self)
    Commands::BuscarObjeto.register(self)
    Commands::ActualizarObjeto.register(self)
    Commands::PredecirObjeto.register(self)
    Commands::Ayuda.register(self)
  end
end