# Hardwire - Inspector de Hardware Android via ADB

Hardwire es una aplicación Android que permite inspeccionar el hardware del dispositivo mediante una conexión ADB (USB o WiFi). Diseñada para desarrolladores, ingenieros de hardware y entusiastas que necesitan información detallada sobre los componentes del sistema.

## Características

- **Conexión USB y WiFi ADB**: Comunícate con dispositivos locales o remotos a través de ADB
- **10 categorías de hardware**: Información completa de CPU, GPU, memoria, almacenamiento, batería, pantalla, sensores, red, dispositivos USB y sistema operativo
- **Base de datos de SoC**: Base de datos integrada con información detallada de procesadores (Snapdragon, MediaTek, Exynos, etc.)
- **Multi-dispositivo**: Soporte para conectar y gestionar múltiples dispositivos simultáneamente
- **Tema oscuro/claro**: Interfaz adaptable a las preferencias del usuario con Material 3

## Requisitos

- Android 7.0 (API 24) o superior
- Habilitar depuración USB en el dispositivo
- Conexión USB para escaneo local
- Conexión WiFi para escaneo en red

## Arquitectura

Hardwire utiliza una arquitectura moderna y eficiente:

- **Módulo único**: Proyecto consolidado en un solo módulo para simplificar la compilación y mantenimiento
- **MVVM**: Patrón Model-View-ViewModel para una separación clara de responsabilidades
- **Jetpack Compose**: UI declarativa con los componentes más recientes
- **adblib bundled**: Librería ADB integrada directamente en el proyecto

## Stack Tecnológico

| Componente | Versión |
|-----------|---------|
| Kotlin | 2.4.0 |
| Jetpack Compose | Latest |
| Material 3 | Latest |
| Gradle | 9.4.1 |
| Target SDK | 35 |

## Instrucciones de Compilación

### Prerrequisitos
- Android Studio Hedgehog (2023.1) o posterior
- JDK 17
- SDK de Android con API 35 instalada

### Pasos

1. Clona el repositorio:
```bash
git clone https://github.com/hitomatito/Hardwire.git
cd Hardwire
```

2. Abre el proyecto en Android Studio

3. Sincroniza el proyecto con Gradle

4. Compila y ejecuta en un dispositivo o emulador:
```bash
./gradlew assembleDebug
```

## Estructura del Proyecto

```
Hardwire/
├── app/                          # Módulo principal
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/             # Código fuente Kotlin
│   │   │   ├── res/              # Recursos (iconos, strings, temas)
│   │   │   └── AndroidManifest.xml
│   │   ├── debug/                # Configuración de debug
│   │   └── release/              # Configuración de release
│   └── build.gradle.kts          # Configuración del módulo
├── gradle/                       # Configuración de Gradle
├── build.gradle.kts              # Configuración raíz
├── settings.gradle.kts           # Configuración de módulos
└── gradle.properties             # Propiedades del proyecto
```

## Licencia

Este proyecto está bajo la Licencia MIT. Consulta el archivo [LICENSE](LICENSE) para más detalles.
