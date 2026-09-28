package com.uis.kelvinohmmeter

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge

class ConnectionActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var retryButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_connection)

        findViewById<View>(R.id.main).aplicarInsetsSistema()

        progressBar  = findViewById(R.id.connectionProgress)
        statusText   = findViewById(R.id.connectionStatusText)
        subtitleText = findViewById(R.id.connectionSubtitle)
        retryButton  = findViewById(R.id.retryButton)

        retryButton.setOnClickListener {
            mostrarConectando()
            MqttManager.conectar()
        }

        MqttManager.onConectado = {
            runOnUiThread { irAMain() }
        }

        MqttManager.onDesconectado = {
            runOnUiThread { mostrarError() }
        }

        mostrarConectando()
        MqttManager.conectar()
    }

    private fun mostrarConectando() {
        progressBar.visibility  = View.VISIBLE
        retryButton.visibility  = View.GONE
        statusText.text         = getString(R.string.conectando)
        subtitleText.text       = getString(R.string.conectando_subtitulo)
    }

    private fun mostrarError() {
        progressBar.visibility  = View.GONE
        retryButton.visibility  = View.VISIBLE
        statusText.text         = getString(R.string.error_conexion)
        subtitleText.text       = getString(R.string.error_conexion_subtitulo)
    }

    private fun irAMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}