class BaseModel
  def initialize(almacenamiento, tabla)
    @almacenamiento = almacenamiento
    @tabla = tabla
    @almacenamiento[@tabla] ||= []
  end

  def crear(datos)
    @almacenamiento[@tabla] << datos
  end

  def buscar(criterio)
    @almacenamiento[@tabla].find { |registro| criterio.all? { |k, v| registro[k] == v } }
  end

  def actualizar(criterio, nuevos_datos)
    registro = buscar(criterio)
    return unless registro

    nuevos_datos.each { |k, v| registro[k] = v }
  end

  def eliminar(criterio)
    @almacenamiento[@tabla].delete_if { |registro| criterio.all? { |k, v| registro[k] == v } }
  end

  def listar
    @almacenamiento[@tabla]
  end
end