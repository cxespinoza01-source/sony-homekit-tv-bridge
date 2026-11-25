# Sony HomeKit TV Bridge

Proyecto Android TV en Kotlin para probar el control local de televisores Sony BRAVIA mediante Sony IP Control.

## Estado: alpha inicial

Esta primera versión es una aplicación de prueba de control Sony por red local. **Todavía no implementa el protocolo HomeKit/HAP y no se puede emparejar con Apple Casa.** No publiques tu clave PSK ni la incluyas en GitHub.

Funciones experimentales incluidas: consulta de información del televisor, consulta de energía, órdenes de encendido/apagado, volumen y silencio. La compatibilidad de cada orden debe validarse en el modelo y firmware concretos.

## Compilar APK con GitHub Actions

1. Abre la pestaña **Actions** del repositorio.
2. Selecciona **Build Android APK** y pulsa **Run workflow** (o haz un push a `main`).
3. Cuando termine, abre la ejecución y descarga el artefacto `sony-tv-bridge-debug-apk`.
4. Dentro estará `app-debug.apk`.

## Instalar con ADB desde Windows

Con el televisor y el PC en la misma red, habilita la depuración ADB en Android TV y autoriza la huella RSA en la pantalla del televisor. En la carpeta de Android Platform Tools ejecuta el método de conexión que muestre tu TV. En dispositivos que admiten ADB TCP clásico:

```bat
adb connect 192.168.100.40:5555
adb devices
adb install -r app-debug.apk
```

El puerto y el método de conexión dependen de la versión de Android TV. No abras ADB a redes que no sean de confianza.

## Datos del dispositivo

La aplicación muestra la IP por defecto `192.168.100.40`; puedes cambiarla. Introduce la PSK únicamente en el televisor, no en archivos del repositorio ni en mensajes. La aplicación usa HTTP local para el endpoint Sony; úsala solamente en una red doméstica de confianza.

## Limitaciones importantes

- La aplicación debe estar abierta para esta primera prueba; todavía no incluye un servicio persistente.
- No puede garantizar el encendido cuando Android TV está suspendido o apagado.
- El servidor HomeKit/HAP, el emparejamiento Apple Casa, Siri y las automatizaciones quedan pendientes.
- Proyecto experimental sin validación física en el XBR-49X839C.
