# IDEstudioV2 

**IDEstudioV2** es un Entorno de Desarrollo Integrado (IDE) desarrollado en Java, diseñado para gestionar el ciclo completo de vida de un lenguaje de programación personalizado. Este proyecto integra herramientas de análisis léxico, sintáctico y semántico para proporcionar una experiencia de desarrollo robusta y educativa.

---

## 🛠️ Características Principales

* **Editor de Código Inteligente:** Construido sobre `RSyntaxTextArea`, permite resaltado de sintaxis, numeración de líneas y visualización inmediata de errores.
* **Compilador de Alta Precisión:**
    * **Análisis Léxico:** Generación de tokens eficiente mediante `JFlex`.
    * **Análisis Sintáctico:** Validación rigurosa de la gramática del lenguaje.
    * **Análisis Semántico:** Control profundo de tipos, ámbito de variables y verificación lógica.
* **Diagnóstico de Errores:** Consola interactiva con localización exacta (línea y columna) para una depuración rápida.
* **Gestión de Símbolos:** Tabla de símbolos en tiempo real para el seguimiento de variables, constantes y parámetros.
* **Experiencia de Usuario:** Interfaz moderna (Swing) con sistema de pestañas, Splash Screen y paneles configurables.

---

## 🏗️ Arquitectura del Proyecto

El proyecto sigue una estructura modular para facilitar su mantenimiento y escalabilidad:

```text
src/
├── code/           # Lógica core del compilador
│   ├── lexico/     # Definiciones y analizador JFlex
│   ├── sintactico/ # Reglas gramaticales y parseo
│   └── semantico/  # Tabla de símbolos y validación semántica
├── view/           # Componentes de la UI (Swing)
└── imgs/           # Recursos y assets gráficos
