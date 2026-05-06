# frozen_string_literal: true

# Comando para predecir la ubicación de un objeto
module Commands
  module PredecirObjeto
    def self.register(bot)
      bot.register_command('/PredecirObjeto', nombre: '¿Qué objeto quieres predecir?') do
        modelo_objetos = ModeloObjetos.new(metadata)
        name = params[:nombre]
        historial = modelo_objetos.listar
        predictor = Predictor.new(historial)
        lugar = predictor.predecir(name)
        if lugar
          send_message("Creo que #{name} podría estar en: #{lugar}")
        else
          send_message("No tengo historial de #{name}, regístralo primero con /RegistrarObjeto")
        end
      end
    end
  end
end