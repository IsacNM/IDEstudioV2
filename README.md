# IDEstudioV2

IDEstudioV2 es un entorno de desarrollo integrado (IDE) hecho en Java para un lenguaje de programación propio. El proyecto incluye las herramientas necesarias para el análisis léxico, sintáctico y semántico del código.

## Funciones principales

- Editor de texto con resaltado de sintaxis (usando RSyntaxTextArea).
- Compilador integrado:
  - Analizador léxico con JFlex.
  - Analizador sintáctico para validar la estructura del lenguaje.
  - Analizador semántico con tabla de símbolos y validación de tipos.
- Consola de salida para errores de compilación con línea y columna.
- Interfaz gráfica sencilla con Swing.

## Organización del código

- `src/code/`: Contiene la lógica del compilador dividida en carpetas para léxico, sintáctico y semántico.
- `src/view/`: Clases de la interfaz gráfica y componentes de Swing.
- `src/imgs/`: Imágenes y recursos usados en el IDE.
- `lib/`: Librerías externas necesarias (JFlex, RSyntaxTextArea, etc).

## Cómo usarlo

El proyecto se puede abrir en NetBeans o compilar usando el archivo `build.xml` con Ant. Es necesario tener instaladas las librerías que están en la carpeta `lib`.
