# IDEstudioV2 - Entorno de Desarrollo para Lenguajes Personalizados

IDEstudioV2 es un entorno de desarrollo integrado (IDE) desarrollado en Java, diseñado específicamente para soportar el ciclo completo de compilación de un lenguaje de programación propio. Incluye un editor de texto avanzado, un compilador integrado con análisis en múltiples fases y una interfaz de usuario intuitiva para la gestión de errores y símbolos.

##  Características Principales

- **Editor Avanzado:** Basado en `RSyntaxTextArea`, ofrece resaltado de sintaxis, numeración de líneas y marcado de errores en tiempo real.
- **Compilador Multi-Fase:**
  - **Análisis Léxico:** Identificación de tokens mediante JFlex.
  - **Análisis Sintáctico:** Validación de la estructura gramatical del código.
  - **Análisis Semántico:** Comprobación de tipos, declaración de variables y lógica de asignaciones.
- **Gestión de Errores Detallada:** Consola interactiva que muestra errores léxicos, sintácticos y semánticos con ubicación exacta (línea y columna).
- **Tabla de Símbolos:** Visualización en tiempo real de las variables, constantes y parámetros declarados durante la compilación.
- **Interfaz Amigable:** Incluye Splash Screen, paneles de configuración y un sistema de pestañas para múltiples archivos.

##  Requisitos del Sistema

- **Java JDK:** Versión 8 o superior (recomendado JDK 11+).
- **IDE:** NetBeans (Proyecto basado en Ant).
- **Librerías Incluidas:** 
  - `RSyntaxTextArea` para el editor.
  - `CompilerTools` para utilidades de compilación.

##  Instalación y Ejecución

1. Clona o descarga el proyecto en tu máquina local.
2. Abre el proyecto en NetBeans: `File > Open Project > IDEstudioV2`.
3. Limpia y construye el proyecto para resolver las dependencias: `Shift + F11`.
4. Ejecuta la aplicación: `F6`.

##  Estructura del Proyecto

- `src/code/`: Lógica central del compilador.
  - `lexico/`: Definiciones de tokens y analizador JFlex.
  - `sintactico/`: Reglas de gramática y validación estructural.
  - `semantico/`: Validación de tipos, tabla de símbolos y lógica de negocio del lenguaje.
- `src/view/`: Componentes de la interfaz gráfica (Swing).
- `src/imgs/`: Recursos visuales del IDE.

---
Desarrollado como una solución integral para el estudio y creación de lenguajes de programación.
