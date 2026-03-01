require_relative 'base_model'
class ModeloObjetos < BaseModel
  def initialize(almacenamiento)
    super(almacenamiento, :objects)
  end
end