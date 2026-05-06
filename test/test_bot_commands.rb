# frozen_string_literal: true

require_relative 'test_helper'

class TestBotCommands < BotTest
  def test_registrar_objeto
    @bot.expects(:send_message).with('¿Qué objeto quieres registrar?', anything)
    @bot.expects(:send_message).with('¿En qué lugar suele estar?', anything)
    @bot.expects(:send_message).with('Objeto TestObjeto registrado', anything)
    @bot.receives('/RegistrarObjeto')
    @bot.receives('TestObjeto')
    @bot.receives('Cocina')
    modelo = ModeloObjetos.new(metadata)
    resultado = modelo.listar.map { |o| o.slice(:name, :place) }
    assert_equal([{ name: 'TestObjeto', place: 'Cocina' }], resultado,
                 'El objeto debería haberse registrado correctamente')
  end

  def test_registrar_objeto_duplicado
    @bot.executor.dsl.stubs(:send_message)
    @bot.receives('/RegistrarObjeto')
    @bot.receives('TestObjeto')
    @bot.receives('Cocina')

    @bot.expects(:send_message).with('¿Qué objeto quieres registrar?', anything)
    @bot.expects(:send_message).with('¿En qué lugar suele estar?', anything)
    @bot.expects(:send_message).with('ya existe este objeto', anything)
    @bot.receives('/RegistrarObjeto')
    @bot.receives('TestObjeto')
    @bot.receives('Sala')

    modelo = ModeloObjetos.new(metadata)
    resultado = modelo.listar.map { |o| o.slice(:name, :place) }
    assert_equal([{ name: 'TestObjeto', place: 'Cocina' }], resultado)
  end

  def test_ver_objetos_vacio
    @bot.expects(:send_message).with('No tienes objetos registrados', anything)
    @bot.receives('/VerObjetos')
  end

  def test_ver_objetos_con_lista
    @bot.executor.dsl.stubs(:send_message)
    @bot.receives('/RegistrarObjeto')
    @bot.receives('Llaves')
    @bot.receives('Entrada')
    @bot.receives('/RegistrarObjeto')
    @bot.receives('Cartera')
    @bot.receives('Sala')
    esperado = "Tus objetos registrados:\n1. Llaves - Lugar: Entrada\n2. Cartera - Lugar: Sala\n"
    @bot.expects(:send_message).with(esperado, anything)
    @bot.receives('/VerObjetos')
  end

  def test_eliminar_objeto
    @bot.executor.dsl.stubs(:send_message)
    @bot.receives('/RegistrarObjeto')
    @bot.receives('Llaves')
    @bot.receives('Entrada')

    @bot.expects(:send_message).with('Objeto Llaves eliminado correctamente')
    @bot.receives('/EliminarObjeto')
    @bot.receives('Llaves')

    modelo = ModeloObjetos.new(metadata)
    assert_equal([], modelo.listar)
  end

  def test_eliminar_objeto_inexistente
    @bot.executor.dsl.stubs(:send_message)
    @bot.expects(:send_message).with('No encontré ningún objeto llamado Fantasma', anything)
    @bot.receives('/EliminarObjeto')
    @bot.receives('Fantasma')
  end

  def test_buscar_objeto
    @bot.executor.dsl.stubs(:send_message)
    @bot.receives('/RegistrarObjeto')
    @bot.receives('Mochila')
    @bot.receives('Cuarto')

    @bot.expects(:send_message).with('Encontré Mochila - Lugar: Cuarto', anything)
    @bot.receives('/BuscarObjeto')
    @bot.receives('Mochila')
  end

  def test_buscar_objeto_inexistente
    @bot.executor.dsl.stubs(:send_message)
    @bot.expects(:send_message).with('No encontré ningún objeto llamado Mochila', anything)
    @bot.receives('/BuscarObjeto')
    @bot.receives('Mochila')
  end

  def test_actualizar_objeto
    @bot.executor.dsl.stubs(:send_message)
    @bot.receives('/RegistrarObjeto')
    @bot.receives('Mochila')
    @bot.receives('Cuarto')

    @bot.expects(:send_message).with('Objeto Mochila actualizado al lugar: Sala', anything)
    @bot.receives('/ActualizarObjeto')
    @bot.receives('Mochila')
    @bot.receives('Sala')

    modelo = ModeloObjetos.new(metadata)
    resultado = modelo.listar.map { |o| o.slice(:name, :place) }
    assert_equal([{ name: 'Mochila', place: 'Sala' }], resultado)
  end

  def test_actualizar_objeto_inexistente
    @bot.executor.dsl.stubs(:send_message)
    @bot.expects(:send_message).with('No encontré ningún objeto llamado Fantasma', anything)
    @bot.receives('/ActualizarObjeto')
    @bot.receives('Fantasma')
    @bot.receives('Sala')
  end

  def test_ayuda
    @bot.expects(:send_message).with(anything, anything)
    @bot.receives('/Ayuda')
  end

  def test_predecir_objeto_sin_historial
    @bot.executor.dsl.stubs(:send_message)
    @bot.expects(:send_message).with('No tengo historial de Llaves, regístralo primero con /RegistrarObjeto', anything)
    @bot.receives('/PredecirObjeto')
    @bot.receives('Llaves')
  end

  def test_predecir_objeto_con_historial
    @bot.executor.dsl.stubs(:send_message)
    @bot.receives('/RegistrarObjeto')
    @bot.receives('Llaves')
    @bot.receives('Entrada')

    modelo = ModeloObjetos.new(metadata)
    predictor = Predictor.new(modelo.listar)
    assert_equal('Entrada', predictor.predecir('Llaves'))

    @bot.receives('/PredecirObjeto')
    @bot.receives('Llaves')
  end
end