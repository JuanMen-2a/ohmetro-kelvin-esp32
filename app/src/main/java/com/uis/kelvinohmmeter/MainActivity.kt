package com.uis.kelvinohmmeter

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var valueText: TextView
    private lateinit var startButton: Button
    private lateinit var batteryPercentText: TextView
    private lateinit var batteryStatusText: TextView
    private lateinit var batteryIcon: ImageView

    private var midiendo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.main).aplicarInsetsSistema()

        valueText = findViewById(R.id.valueText)
        startButton = findViewById(R.id.startButton)
        batteryPercentText = findViewById(R.id.batteryPercentText)
        batteryStatusText = findViewById(R.id.batteryStatusText)
        batteryIcon = findViewById(R.id.batteryIcon)

        findViewById<ImageView>(R.id.settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        startButton.setOnClickListener {
            if (midiendo) detenerMedicion() else iniciarMedicion()
        }

        // Recibir datos del ESP32 vía MQTT
        MqttManager.onMedicionRecibida = { data ->
            runOnUiThread {
                actualizarMedicion(data.resistenciaMOhm.toInt())
                batteryPercentText.text = getString(R.string.bateria_formato, data.bateriaPorcentaje)
                batteryStatusText.text = if (data.cargando) getString(R.string.cargando) else getString(R.string.estado_bateria)
            }
        }

        MqttManager.onDesconectado = {
            runOnUiThread {
                valueText.text = "--"
                batteryStatusText.text = "Sin conexión"
            }
        }
    }

    private fun actualizarMedicion(valorMOhm: Int) {
        valueText.text = valorMOhm.toString()
    }

    private fun iniciarMedicion() {
        midiendo = true
        startButton.text = getString(R.string.detener_medicion)
        MqttManager.publicarComando("iniciar")
    }

    private fun detenerMedicion() {
        midiendo = false
        startButton.text = getString(R.string.iniciar_medicion)
        MqttManager.publicarComando("detener")
    }

    override fun onDestroy() {
        super.onDestroy()
        MqttManager.onMedicionRecibida = null
    }
}