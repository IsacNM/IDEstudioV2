# Informe de Bugs — Auditoría 2026-05-09

**Rama:** `limpieza`
**Alcance:** Auditoría completa por subsistema (léxico → sintáctico → semántico → intermedio → IDE) buscando bugs latentes, código duplicado, comentarios excesivos y oportunidades de reordenación. Acompaña a `BUGS.txt` (historial de fixes anteriores).

Severidad:

- 🔴 **CRÍTICO** — rompe funcionalidad, NPE/AIOOBE bajo entrada normal, pérdida de datos
- 🟠 **ALTO** — produce salida incorrecta, semántica equivocada, fugas
- 🟡 **MEDIO** — degrada UX, oculta información, fricción del mantenedor
- 🟢 **BAJO** — estilo, naming, comentarios, micro-ineficiencias

---

## 🔴 BUGS CRÍTICOS

### C1. `ProcesadorAsignaciones.procesar()` entra en bucle silencioso si falta `;`
**Archivo:** `src/code/semantico/ProcesadorAsignaciones.java` (versión previa, línea 26)
**Antes:**
```java
i = TokenUtils.indiceSiguiente(i + 2, TokenTipo.PUNTO_COMA);
```
`indiceSiguiente` devuelve `-1` cuando no encuentra el delimitador. El loop hacía `i++` quedando en `0` y reescaneando todo desde el inicio.
**Estado:** ✅ **CORREGIDO** en este branch — ahora se valida `< 0` y se hace `break`.

### C2. `procesarDeclaracion()` accede a `comp(pos+1)` sin guardia de tamaño
**Archivo:** `src/code/intermedio/GeneradorCodigoIntermedio.java`
**Antes:** si la lista terminaba en VAR sin más tokens, `comp(pos+1)` podía leer fuera del rango si la fuente era patológica (la implementación de `comp` lo amortigua devolviendo `""`, pero el flujo posterior asumía que había al menos un token de tipo). Reescrito: `procesarDeclaracion` ahora avanza con `pos++` controlado y `pos < tokens.size()`.
**Estado:** ✅ **CORREGIDO** en este branch.

---

## 🟠 BUGS ALTOS

### A1. `EvaluadorExpresiones.evaluarPostfija()` retorna prematuro al primer booleano
**Archivo:** `src/code/semantico/EvaluadorExpresiones.java:86-88`
```java
if ("cierto".equals(resultado) || "falso".equals(resultado)) {
    return resultado;          // ← sale de la evaluación tras el primer -y-/-o-
}
```
Para `a -y- b -o- c` la evaluación termina tras `a -y- b`. Solo afecta a la **evaluación constante** del semántico (la generación de 3D usa `generarPostfija3D`, no esta función), pero al pasar valores de la tabla de símbolos a `evaluarExpresion`, una expresión con dos operadores lógicos pierde el segundo.
**Repro:** `var logico a := cierto -y- falso -o- cierto;` el valor en la tabla queda como `falso` aunque la lógica espera `cierto`.
**Fix sugerido:** convertir el booleano a `1.0`/`0.0`, hacer push y continuar; sólo devolver el string al final.

### A2. `EvaluadorExpresiones.resolverOperando()` traga errores
**Archivo:** `src/code/semantico/EvaluadorExpresiones.java:106-119`
Variables con valor no numérico (`"hola"`) se silencian a `0.0` en dos catch anidados sin reportar nada. La aritmética sobre cadenas no marca error semántico aquí, contradice las validaciones de tipo en `AuxSemantico` (ya hay error en otra fase, pero el doble silencio dificulta depurar resultados raros en la tabla).
**Fix sugerido:** loguear `Level.FINE` con el token para inspección.

### A3. `Repositorio.idDeclaraciones` y `Repositorio.erroresSemanticos` eran código muerto
**Archivo:** `src/code/semantico/Repositorio.java`
Ambos campos se declaraban públicos, se limpiaban en `limpiar()`, pero ningún código del proyecto los escribía nunca. (Confirmado con `grep -rn`.)
**Estado:** ✅ **ELIMINADOS** en este branch.

### A4. `Compilar.listaTokens` pública y mutable
**Archivo:** `src/code/Compilar.java`
Era `public static ArrayList<Token>`. Cualquier código podía reasignarla. El campo se conserva como contenedor compartido (necesario para el reporte léxico aparte de `Repositorio.listaTokens`), pero ahora es `public static final` y se añadió helper `hayErroresLexicos()` para encapsular el patrón duplicado en `GestorCompilador` y `TestCompiler`.
**Estado:** ✅ **CORREGIDO** en este branch.

### A5. Hardcodes de strings de tokens dispersos
Antes de esta auditoría, los siguientes string literals aparecían dispersos en lugar de constantes en `TokenTipo`:
`"INICIO"`, `"FIN"`, `"MOSTRAR"`, `"FOR"`, `"SWITCH"`, `"CASE"`, `"DEFAULT"`, `"BREAK"`, `"SINO"`, `"LLAVE_IZQ"`, `"LLAVE_DER"`, `"DOS_PUNTOS"`, `"OP_INCREMENTO"`, `"OP_DECREMENTO"`, `"ERROR"`, `"PAREN_IZQ"`, `"PAREN_DER"`.

Esto rompía la "única fuente de verdad" del catálogo de tokens. Un cambio de nombre en el lexer requería buscar y editar en 7+ archivos.
**Estado:** ✅ **CORREGIDO** en este branch — todas las constantes faltantes añadidas a `TokenTipo` y referenciadas desde:
- `GeneradorCodigoIntermedio` (21+ usos)
- `AnalizadorSintactico` (`INICIO`, `FIN`, `ERROR`)
- `Compilar`, `GestorCompilador`, `TestCompiler` (`ERROR`, `FIN`)
- `FiltroParentesis` (10 keywords de estructura)
- `TokenUtils` (literales del switch de tipos)

---

## 🟡 BUGS MEDIOS

### M1. `AuxSemantico.validarVariablesNoDeclaradas` cruza `i-2` sin chequeo explícito
**Archivo:** `src/code/semantico/AuxSemantico.java:17`
`tokenEn(i-2, …)` se apoya en el guardia interno `pos < 0 → false`, pero la intención (saltar declaraciones) se pierde. Funciona, pero es frágil ante cambios futuros.
**Fix sugerido:** `if (i < 2 || TokenUtils.tokenEn(i - 2, TokenTipo.VAR)) continue;` por claridad.

### M2. `validarTiposEnAsignaciones` accede a `i+2` sin chequeo de tamaño
**Archivo (anterior):** `AuxSemantico.java:109`
`Repositorio.listaTokens.get(i + 2)` lanzaba `IndexOutOfBounds` si una asignación quedaba truncada (`a :=` al final del archivo sin valor ni `;`).
**Estado:** ✅ **CORREGIDO** — añadida guardia `if (i + 2 >= listaTokens.size()) continue;`.

### M3. `EvaluadorExpresiones.esOperando` acepta IDs no declarados
**Archivo:** `src/code/semantico/EvaluadorExpresiones.java:143-153`
Cualquier identificador con regex `[a-zA-Z_][a-zA-Z0-9_]*` se acepta como operando aunque no esté en `tablaSimbolos`. `resolverOperando` luego devuelve `0.0` silencioso. La validación previa de "variable no declarada" cubre el caso, pero si alguien pasa una expresión sin pasar por el flujo principal (ej. test directo), obtiene resultados engañosos.

### M4. `loopConTope` por reflexión depende del nombre interno `producciones`
**Archivo:** `src/code/sintactico/AnalizadorSintactico.java:250`
Si `compilerTools.jar` cambia ese nombre privado, hay fallback al método sin tope. Cubierto, pero idealmente la dependencia se documenta junto con la versión esperada del jar.
**Recordatorio:** La versión actualmente referenciada es `compilerTools-2.3.7.jar`.

### M5. `MAX_ITERACIONES_SINTACTICO = 1000`
**Archivo:** `src/code/sintactico/AnalizadorSintactico.java:24`
Si la gramática crece (anidamientos profundos), el tope podría volverse insuficiente. Los 12 tests actuales convergen en <50 iteraciones, así que hay holgura ~20×, pero conviene exponerlo como propiedad de sistema (`-Didestudio.maxIter=…`) para depuración.

### M6. `GestorCompilador` asume que `RTextScrollPane` puede no tener "archivo_ruta"
**Archivo:** `src/code/GestorCompilador.java`
Si el archivo no se ha guardado, el código emite el mensaje correcto, pero la pestaña queda sin pista visual de que la generación 3D no ocurrió. UX-fix: deshabilitar el botón "Compilar y Correr" hasta que haya ruta o forzar diálogo "Guardar antes de ejecutar".

### M7. Salida ANSI en `AnalizadorSintactico.ejecutarInterno`
Funciona (existente fix B4). La regex `\033\\[[;\\d]*m` cubre SGR, pero no otras secuencias CSI. No hemos visto otras emitidas por el jar, sin embargo es bueno saberlo.

---

## 🟢 BUGS BAJOS / ESTILO

### B1. Imports sin usar
- `TestCompiler.java` importaba `code.semantico.FiltroParentesis` sin usarlo. ✅ **REMOVIDO**.

### B2. Comment fences decorativos `// ─── XXX ───`
Antes había 11+ separadores de método en `GeneradorCodigoIntermedio`. ✅ **REDUCIDOS** a un único patrón `// ── label ───────` por sección lógica.

### B3. Bloque histórico de "CORRECCIONES" en `GeneradorCodigoIntermedio`
Lista de 17 líneas detallando bugs ya fijados (pertenece al historial git). ✅ **REMOVIDO**.

### B4. `e.printStackTrace()` quedaba en `TestCompiler.main`
Dado que es CLI y la traza es útil al desarrollador, se mantiene; documentado.

---

## ✅ RESUMEN DE CORRECCIONES APLICADAS EN ESTA AUDITORÍA

| # | Acción | Archivo |
|---|--------|---------|
| 1 | Constantes faltantes añadidas a `TokenTipo` | `TokenTipo.java` |
| 2 | Reemplazo de string literals por `TokenTipo.*` (21+ sitios) | `GeneradorCodigoIntermedio.java`, `AnalizadorSintactico.java`, `Compilar.java`, `GestorCompilador.java`, `TestCompiler.java`, `FiltroParentesis.java`, `TokenUtils.java` |
| 3 | Eliminado bloque "CORRECCIONES" obsoleto | `GeneradorCodigoIntermedio.java` |
| 4 | Reemplazadas comment-fences decorativas | `GeneradorCodigoIntermedio.java` |
| 5 | Guardia `< 0` antes de usar el resultado de `indiceSiguiente` | `ProcesadorAsignaciones.java` |
| 6 | Guardia `i + 2 >= size()` antes de acceder a token de valor | `AuxSemantico.java` |
| 7 | Eliminados campos muertos `idDeclaraciones`, `erroresSemanticos` | `Repositorio.java` |
| 8 | Colecciones de `Repositorio` ahora `final` | `Repositorio.java` |
| 9 | `Compilar.listaTokens` ahora `final` + helper `hayErroresLexicos()` | `Compilar.java` |
| 10 | `GestorCompilador` partido en métodos cortos por responsabilidad | `GestorCompilador.java` |
| 11 | Helpers compartidos en `TokenUtils` (`esTipoNumerico`, `esTipoNoNumerico`) | `TokenUtils.java`, `AuxSemantico.java` |
| 12 | Helpers de cursor en `GeneradorCodigoIntermedio` (`skipParenIzq`, `skipParenDer`, `skipLlaveIzq`, `skipLlaveDer`, `recolectarHastaParenCierre`, `recolectarCondicion`, `emitirExpresion`, `emitirExpresionEnRango`, `generarCuerpoCaso`, `generarSentencia`) | `GeneradorCodigoIntermedio.java` |
| 13 | Eliminado `import` huérfano | `TestCompiler.java` |

**Verificación:** 12/12 tests en `test/` siguen produciendo .3d válidos tras el refactor (mismo número de líneas y semántica preservada).

---

## 🔬 BUGS PENDIENTES (no corregidos, requieren decisión)

| # | Bug | Por qué no se tocó |
|---|-----|--------------------|
| A1 | Retorno temprano en `evaluarPostfija` con booleanos | Cambio de semántica que podría afectar ramas no testeadas; conviene un test dirigido antes |
| A2 | Errores tragados en `resolverOperando` | Es defensivo a propósito; añadir log podría ensuciar consola |
| M3 | `esOperando` acepta IDs no declarados | Solo afecta uso fuera del flujo principal |
| M4 | Reflexión sobre `producciones` privado | Es la mejor herramienta disponible mientras compilerTools no exponga API |
| M6 | Falta de feedback cuando no hay ruta de archivo | Decisión de UX |

---

_Generado el 2026-05-09 — auditoría completa de subsistemas tras refactor de organización por carpetas._
