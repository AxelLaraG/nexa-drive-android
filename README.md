# NexaDrive - Android Native Client

Aplicación móvil nativa para Android diseñada para la gestión digital de renta de vehículos. Este cliente proporciona una experiencia de usuario optimizada y fluida, permitiendo la exploración del catálogo de automóviles, registro seguro de usuarios y control de arrendamientos desde dispositivos Android.

## Características Principales

* **Exploración de Catálogo Dinámico:** Visualización eficiente del inventario de vehículos utilizando `RecyclerView` con adaptadores personalizados (`CarAdapter`) para un rendimiento óptimo en listas largas.
* **Navegación Arquitectónica:** Implementación del Jetpack Navigation Component, gestionado a través de un Drawer Layout centralizado para transiciones consistentes entre pantallas.
* **Gestión de Entidades de Negocio:** Modelado estructurado de datos para manejar la lógica de vehículos y rentas de forma segura (`Car.kt`, `Rentas.kt`).
* **Autenticación e Identidad:** Flujo de acceso y registro de usuarios completamente nativo (`LoginActivity`, `RegisterActivity`), garantizando el control de sesiones.

## Stack Tecnológico

* **Lenguaje Principal:** Kotlin.
* **Sistema de Compilación:** Gradle configurado con Kotlin DSL (`build.gradle.kts`).
* **Interfaz de Usuario:** XML Layouts clásicos de Android integrados con Activities y Fragments.
* **Infraestructura Backend:** Servicios de Firebase integrados nativamente.

## Despliegue y Configuración Local

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/AxelLaraG/nexa-drive-android.git
   ```
2. **Abrir el entorno:**
   Importa el proyecto directamente en **Android Studio**. El entorno sincronizará automáticamente las dependencias nativas definidas en los archivos de Gradle.
3. **Conexión a la base de datos:**
   Para enlazar la aplicación con una base de datos propia, descarga tu archivo de configuración `google-services.json` desde la consola del proveedor y colócalo dentro del directorio `/app`.
4. **Ejecución:**
   Selecciona un emulador (AVD) o conecta un dispositivo físico mediante depuración USB (ADB) y ejecuta el proyecto.
## Estructura y Arquitectura
El código fuente principal está segmentado para facilitar la escalabilidad y el mantenimiento por módulos lógicos:

- `/adapters`: Componentes de enlace que conectan las fuentes de datos con los componentes visuales de listas.
- `/models`: Clases de datos (data class en Kotlin) que definen la estructura de las entidades del negocio.
- `/ui`: Contiene la lógica de las pantallas separada en módulos independientes (home, creditos, etc.), implementando el patrón de Fragments para reutilización de UI.
