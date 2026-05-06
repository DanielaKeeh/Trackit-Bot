# frozen_string_literal: true

# Comando de ayuda y bienvenida al bot
module Commands
  module Ayuda
    def self.register(bot)
      bot.register_command('/Ayuda') do
        mensaje = <<~MSG
          👋 ¡Hola! Soy *TrackitBot* 🤖
          Te ayudo a recordar dónde dejaste tus cosas.

          📋 *Comandos disponibles:*

          📦 /RegistrarObjeto — Registra un objeto y su lugar
          📍 /BuscarObjeto — Busca dónde dejaste algo
          📝 /VerObjetos — Lista todos tus objetos
          ✏️ /ActualizarObjeto — Cambia el lugar de un objeto
          🗑️ /EliminarObjeto — Elimina un objeto
          🔮 /PredecirObjeto — Predice dónde está algo según tu historial

          💡 *Tip:* Si olvidaste registrar algo, prueba /PredecirObjeto
        MSG
        send_message(mensaje)
      end
    end
  end
end
