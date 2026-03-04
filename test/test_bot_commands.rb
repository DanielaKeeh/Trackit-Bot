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
    assert_equal([{ name: 'TestObjeto', place: 'Cocina' }], modelo.listar,
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
    assert_equal([{ name: 'TestObjeto', place: 'Cocina' }], modelo.listar)
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
end
