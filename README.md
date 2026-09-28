# Óhmetro Kelvin de 4 Hilos — ESP32 + MQTT + Android

> Trabajo Final — Diseño Electrónico, UIS 2026-II

Instrumento de medición de resistencias de bajo valor (rango: miliohmios) basado en el método Kelvin de 4 hilos. El ESP32 adquiere la señal, la procesa y la transmite vía MQTT a una app Android en tiempo real.

---

## Arquitectura del sistema
Circuito Kelvin → ESP32 → Hotspot celular → Internet → test.mosquitto.org → Internet → App Android

---

## 📁 Estructura del repositorio
ohmetro-kelvin-esp32/
├── firmware/
│ └── kelvin_esp32/
│ └── DisenoTF.ino # Firmware ESP32 (Arduino IDE)
├── android/
│ └── KelvinOhmeter/ # Proyecto Android completo
│ ├── app/src/main/java/com/uis/kelvinohmmeter/
│ │ ├── MqttManager.kt
│ │ ├── MainActivity.kt
│ │ ├── ConnectionActivity.kt
│ │ ├── SettingsActivity.kt
│ │ └── EdgeToEdgeUtils.kt
│ └── app/src/main/AndroidManifest.xml
├── .gitignore
├── LICENSE
└── README.md

