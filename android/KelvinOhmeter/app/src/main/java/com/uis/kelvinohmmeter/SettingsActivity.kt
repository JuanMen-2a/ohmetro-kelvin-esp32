package com.uis.kelvinohmmeter

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        findViewById<View>(R.id.main).aplicarInsetsSistema()

        prefs = getSharedPreferences("kelvin_prefs", MODE_PRIVATE)

        val ssidInput = findViewById<EditText>(R.id.ssidInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val offsetInput = findViewById<EditText>(R.id.offsetInput)
        val unitsGroup = findViewById<RadioGroup>(R.id.unitsRadioGroup)

        // Cargar valores guardados previamente
        ssidInput.setText(prefs.getString("wifi_ssid", ""))
        passwordInput.setText(prefs.getString("wifi_password", ""))
        offsetInput.setText(prefs.getFloat("calibracion_offset", 0f).toString())
        if (prefs.getString("unidad", "mohm") == "ohm") {
            unitsGroup.check(R.id.radioOhm)
        } else {
            unitsGroup.check(R.id.radioMohm)
        }

        findViewById<Button>(R.id.saveWifiButton).setOnClickListener {
            val ssid = ssidInput.text.toString()
            val password = passwordInput.text.toString()
            prefs.edit {
                putString("wifi_ssid", ssid)
                putString("wifi_password", password)
            }
            Toast.makeText(this, "WiFi guardado: $ssid", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.calibrateZeroButton).setOnClickListener {
            if (MqttManager.estaConectado()) {
                MqttManager.publicarComando("calibrar")
                offsetInput.setText("0.0")
                prefs.edit { putFloat("calibracion_offset", 0f) }
                Toast.makeText(this, "Calibrado en cero", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Sin conexión con el ESP32", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.saveSettingsButton).setOnClickListener {
            val offset = offsetInput.text.toString().toFloatOrNull() ?: 0f
            val unidad = if (unitsGroup.checkedRadioButtonId == R.id.radioOhm) "ohm" else "mohm"
            prefs.edit {
                putFloat("calibracion_offset", offset)
                putString("unidad", unidad)
            }
            Toast.makeText(this, getString(R.string.guardar_ajustes), Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }
    }
}