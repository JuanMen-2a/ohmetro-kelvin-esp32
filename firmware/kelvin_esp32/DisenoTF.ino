#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>

// ---------- CONFIG WiFi ----------
const char* WIFI_SSID     = "Juan's A16";
const char* WIFI_PASSWORD = "mendozas24";

// ---------- CONFIG MQTT ----------
const char* MQTT_BROKER   = "test.mosquitto.org";
const int   MQTT_PORT     = 1883;
const char* TOPIC_MEDICION = "kelvin/medicion/juanuis2026";
const char* TOPIC_COMANDO  = "kelvin/comando/juanuis2026";

// ---------- CONFIG KELVIN ----------
const int   PIN_ADC         = 34;      // GPIO34 - ADC1 (no usar ADC2 con WiFi)
const float V_REF           = 3.3;     // Voltaje de referencia del ESP32
const int   ADC_RESOLUTION  = 4095;    // 12 bits
const float I_EXCITACION    = 0.050;   // 50 mA = 0.050 A
const int   N_MUESTRAS      = 20;      // Promedio para reducir ruido

// MODO DE OPERACIÓN:
// true  = usa ADC real (circuito Kelvin conectado)
// false = usa valores simulados (para pruebas sin hardware)
const bool MODO_REAL = true;

WiFiClient   espClient;
PubSubClient mqtt(espClient);

// ---------- ESTADO ----------
bool midiendo = false;
float calibracionOffset = 0.0; // offset en mOhm aplicado a la lectura real
float valorSimulado = 200.0;   // solo se usa cuando MODO_REAL = false
int bateriaPorcentaje = 87;
bool cargando = false;

unsigned long ultimaPublicacion = 0;
const long INTERVALO_MS = 1000;

// ---------- LECTURA ADC REAL (método Kelvin 4 hilos) ----------
float leerResistenciaMOhm() {
  // Promedio de N_MUESTRAS lecturas para reducir ruido del ADC
  long suma = 0;
  for (int i = 0; i < N_MUESTRAS; i++) {
    suma += analogRead(PIN_ADC);
    delayMicroseconds(200);
  }
  float lecturaPromedio = suma / (float)N_MUESTRAS;

  // Conversión ADC → Voltaje
  float voltaje = (lecturaPromedio / ADC_RESOLUTION) * V_REF;

  // Ley de Ohm: R = V / I
  // Con I = 50mA = 0.050A
  // Resultado en Ohm → convertir a mOhm multiplicando por 1000
  float resistenciaOhm  = voltaje / I_EXCITACION;
  float resistenciaMOhm = (resistenciaOhm * 1000.0) + calibracionOffset;

  // Evitar valores negativos por offset o ruido
  if (resistenciaMOhm < 0) resistenciaMOhm = 0;

  return resistenciaMOhm;
}

// ---------- CONECTAR WIFI ----------
void conectarWiFi() {
  Serial.print("Conectando a WiFi: ");
  Serial.println(WIFI_SSID);
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  int intentos = 0;
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
    intentos++;
    if (intentos > 40) {
      Serial.println("\nNo se pudo conectar al WiFi. Reiniciando...");
      ESP.restart();
    }
  }
  Serial.println("\nWiFi conectado");
  Serial.print("IP del ESP32: ");
  Serial.println(WiFi.localIP());
}

// ---------- CALLBACK: comandos recibidos ----------
void onMensaje(char* topic, byte* payload, unsigned int length) {
  String mensaje = "";
  for (unsigned int i = 0; i < length; i++) {
    mensaje += (char)payload[i];
  }

  Serial.print("Comando recibido: ");
  Serial.println(mensaje);

  if (mensaje == "iniciar") {
    midiendo = true;
    Serial.println("Medición iniciada");
  } else if (mensaje == "detener") {
    midiendo = false;
    Serial.println("Medición detenida");
  } else if (mensaje == "calibrar") {
    if (MODO_REAL) {
      // Toma la lectura actual como offset para que el resultado sea 0
      float lecturaActual = leerResistenciaMOhm() - calibracionOffset;
      calibracionOffset = -lecturaActual;
      Serial.print("Calibrado. Offset: ");
      Serial.print(calibracionOffset);
      Serial.println(" mOhm");
    } else {
      valorSimulado = 0.0;
      Serial.println("Calibrado (modo simulado)");
    }
  }
}

// ---------- CONECTAR MQTT ----------
void conectarMQTT() {
  while (!mqtt.connected()) {
    Serial.print("Conectando al broker MQTT...");
    String clientId = "ESP32_Kelvin_" + String(random(0xffff), HEX);
    if (mqtt.connect(clientId.c_str())) {
      Serial.println(" conectado");
      mqtt.subscribe(TOPIC_COMANDO);
    } else {
      Serial.print(" falló, rc=");
      Serial.print(mqtt.state());
      Serial.println(" reintentando en 3s");
      delay(3000);
    }
  }
}

// ---------- SETUP ----------
void setup() {
  Serial.begin(115200);
  randomSeed(analogRead(0));
  analogReadResolution(12);

  Serial.println(MODO_REAL ? "Modo: REAL (ADC)" : "Modo: SIMULADO");

  conectarWiFi();

  mqtt.setServer(MQTT_BROKER, MQTT_PORT);
  mqtt.setCallback(onMensaje);
}

// ---------- LOOP ----------
void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi perdido, reconectando...");
    conectarWiFi();
  }

  if (!mqtt.connected()) {
    conectarMQTT();
  }
  mqtt.loop();

  unsigned long ahora = millis();
  if (ahora - ultimaPublicacion >= INTERVALO_MS) {
    ultimaPublicacion = ahora;

    float valor = 0.0;

    if (midiendo) {
      if (MODO_REAL) {
        // Lectura real del circuito Kelvin
        valor = leerResistenciaMOhm();
      } else {
        // Simulación con ruido aleatorio
        valorSimulado += random(-50, 51) / 10.0;
        if (valorSimulado < 0) valorSimulado = 0;
        valor = valorSimulado;
      }
    }

    StaticJsonDocument<200> doc;
    doc["resistencia_mohm"]   = valor;
    doc["bateria_porcentaje"] = bateriaPorcentaje;
    doc["cargando"]           = cargando;
    doc["midiendo"]           = midiendo;

    String payload;
    serializeJson(doc, payload);

    mqtt.publish(TOPIC_MEDICION, payload.c_str());
    Serial.println("Publicado: " + payload);
  }
}