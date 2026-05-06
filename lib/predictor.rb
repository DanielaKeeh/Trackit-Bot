# frozen_string_literal: true
require 'time'
# Algoritmo heurístico para predecir la ubicación de un objeto
# basado en frecuencia de lugar y hora del día
class Predictor
  FRANJAS = {
    madrugada: (0..5),
    mañana: (6..11),
    tarde: (12..17),
    noche: (18..23)
  }.freeze

  def initialize(historial)
    @historial = historial
  end

  def predecir(name)
    registros = @historial.select { |r| r[:name] == name }
    return nil if registros.empty?

    franja_actual = franja(Time.now.hour)

    # Primero intenta por franja horaria
    por_franja = registros.select { |r| franja(hora_de(r)) == franja_actual }
    return lugar_mas_frecuente(por_franja) if por_franja.any?

    # Si no hay datos en esa franja, usa frecuencia general
    lugar_mas_frecuente(registros)
  end

  private

  def franja(hora)
    FRANJAS.find { |_nombre, rango| rango.include?(hora) }&.first
  end

  def hora_de(registro)
    timestamp = registro[:updated_at] || registro[:created_at]
    timestamp = Time.parse(timestamp.to_s) unless timestamp.is_a?(Time)
    timestamp.hour
  end

  def lugar_mas_frecuente(registros)
    registros
      .group_by { |r| r[:place] }
      .max_by { |_lugar, grupo| grupo.size }
      &.first
  end
end