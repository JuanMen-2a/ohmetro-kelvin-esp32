# Óhmetro Kelvin de 4 Hilos — ESP32 + MQTT + Android

> Trabajo Final — Diseño Electrónico, UIS 2026-II

Instrumento de medición de resistencias de bajo valor (rango: miliohmios) basado en el método Kelvin de 4 hilos. El ESP32 adquiere la señal, la procesa y la transmite vía MQTT a una app Android en tiempo real.

---

## Arquitectura del sistema
Circuito Kelvin → ESP32 → Hotspot celular → Internet → test.mosquitto.org → Internet → App Android

---


