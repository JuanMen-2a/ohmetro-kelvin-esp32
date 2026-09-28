package com.uis.kelvinohmmeter

import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

object MqttManager {

    private const val BROKER_URL = "tcp://test.mosquitto.org:1883"
    private const val CLIENT_ID       = "KelvinApp_Android"
    private const val TOPIC_MEDICION  = "kelvin/medicion/juanuis2026"
    private const val TOPIC_COMANDO   = "kelvin/comando/juanuis2026"

    private var client: MqttClient? = null

    var onMedicionRecibida: ((MedicionData) -> Unit)? = null
    var onConectado: (() -> Unit)? = null
    var onDesconectado: (() -> Unit)? = null

    fun conectar() {
        Thread {
            try {
                Log.d("MQTT", "Intentando conectar a $BROKER_URL")
                client = MqttClient(BROKER_URL, CLIENT_ID, MemoryPersistence())

                val opciones = MqttConnectOptions().apply {
                    isCleanSession = true
                    connectionTimeout = 10
                    keepAliveInterval = 30
                }

                client?.setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        Log.e("MQTT", "Conexión perdida: ${cause?.message}")
                        onDesconectado?.invoke()
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        val json = message?.toString() ?: return
                        Log.d("MQTT", "Mensaje recibido: $json")
                        try {
                            val data = parsearMedicion(json)
                            onMedicionRecibida?.invoke(data)
                        } catch (e: Exception) {
                            Log.e("MQTT", "Error al parsear: ${e.message}")
                            e.printStackTrace()
                        }
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                })

                client?.connect(opciones)
                client?.subscribe(TOPIC_MEDICION)
                Log.d("MQTT", "Conectado al broker y suscrito a $TOPIC_MEDICION")
                onConectado?.invoke()

            } catch (e: MqttException) {
                Log.e("MQTT", "Error al conectar: ${e.message}, código: ${e.reasonCode}")
                e.printStackTrace()
                onDesconectado?.invoke()
            }
        }.start()
    }

    fun publicarComando(comando: String) {
        Thread {
            try {
                val mensaje = MqttMessage(comando.toByteArray()).apply {
                    qos = 1
                }
                client?.publish(TOPIC_COMANDO, mensaje)
                Log.d("MQTT", "Comando publicado: $comando")
            } catch (e: MqttException) {
                Log.e("MQTT", "Error al publicar: ${e.message}")
                e.printStackTrace()
            }
        }.start()
    }

    fun desconectar() {
        Thread {
            try {
                client?.disconnect()
                client = null
                Log.d("MQTT", "Desconectado del broker")
            } catch (e: MqttException) {
                Log.e("MQTT", "Error al desconectar: ${e.message}")
                e.printStackTrace()
            }
        }.start()
    }

    fun estaConectado(): Boolean = client?.isConnected ?: false

    private fun parsearMedicion(json: String): MedicionData {
        val resistencia = extraerFloat(json, "resistencia_mohm")
        val bateria     = extraerInt(json, "bateria_porcentaje")
        val cargando    = extraerBoolean(json, "cargando")
        val midiendo    = extraerBoolean(json, "midiendo")
        return MedicionData(resistencia, bateria, cargando, midiendo)
    }

    private fun extraerFloat(json: String, clave: String): Float {
        val regex = Regex("\"$clave\":\\s*([\\d.]+)")
        return regex.find(json)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
    }

    private fun extraerInt(json: String, clave: String): Int {
        val regex = Regex("\"$clave\":\\s*(\\d+)")
        return regex.find(json)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    private fun extraerBoolean(json: String, clave: String): Boolean {
        val regex = Regex("\"$clave\":\\s*(true|false)")
        return regex.find(json)?.groupValues?.get(1) == "true"
    }
}

data class MedicionData(
    val resistenciaMOhm: Float,
    val bateriaPorcentaje: Int,
    val cargando: Boolean,
    val midiendo: Boolean
)