# frozen_string_literal: true

# Clase base para modelos de datos, proporcionando métodos CRUD básicos.
class BaseModel
  def initialize(almacenamiento, tabla)
    @almacenamiento = almacenamiento
    @tabla = tabla
    @almacenamiento[@tabla] ||= []
  end

  def crear(datos)
    @almacenamiento[@tabla] << datos.merge(created_at: Time.now)
  end

  def buscar(criterio)
    @almacenamiento[@tabla].find { |registro| criterio.all? { |k, v| registro[k] == v } }
  end

  def actualizar(criterio, nuevos_datos)
    registro = buscar(criterio)
    return unless registro

    nuevos_datos.each { |k, v| registro[k] = v }
    registro[:updated_at] = Time.now
  end

  def eliminar(criterio)
    @almacenamiento[@tabla].delete_if { |registro| criterio.all? { |k, v| registro[k] == v } }
  end

  def listar
    @almacenamiento[@tabla]
  end
end