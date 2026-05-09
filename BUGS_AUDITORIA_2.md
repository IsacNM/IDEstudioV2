# Informe de Bugs — Auditoría 2026-05-09

**Rama:** `limpieza`
**Última actualización:** 2026-05-09 (post-refactor)
**Alcance:** Auditoría completa por subsistema (léxico → sintáctico → semántico → intermedio → IDE). Acompaña a `BUGS.txt` (historial de fixes anteriores).

Severidad:

- 🔴 **CRÍTICO** — rompe funcionalidad, NPE/AIOOBE bajo entrada normal, pérdida de datos
- 🟠 **ALTO** — produce salida incorrecta, semántica equivocada, fugas
- 🟡 **MEDIO** — degrada UX, oculta información, fricción del mantenedor
- 🟢 **BAJO** — estilo, naming, comentarios, micro-ineficiencias

---

## 📋 BUGS PENDIENTES

### 🟠 A1. `EvaluadorExpresiones.evaluarPostfija()` retorna prematuro al primer booleano

**Archivo:** `src/code/semantico/EvaluadorExpresiones.java:86-88`
```java
if ("cierto".equals(resultado) || "falso".equals(resultado)) {
    return resultado;          // ← sale tras el primer -y-/-o-
}
```

Para `a -y- b -o- c` la evaluación termina tras `a -y- b` y descarta el resto. Solo afecta a la **evaluación constante** del semántico (la generación de 3D usa `generarPostfija3D`, otra ruta), pero al asignar valor inicial a una `var logico` con dos operadores lógicos se pierde el segundo.

**Repro:**
```
var logico a := cierto -y- falso -o- cierto;
```
La tabla queda `a := falso` cuando la lógica espera `cierto`.

**Fix sugerido:** convertir el booleano a `1.0` / `0.0`, hacer push y continuar; sólo devolver el string al terminar.

**Por qué no se tocó:** cambio de semántica que podría afectar ramas no testeadas. Conviene un test dirigido antes.

---

### 🟠 A2. `EvaluadorExpresiones.resolverOperando()` traga errores

**Archivo:** `src/code/semantico/EvaluadorExpresiones.java:106-119`

Variables con valor no numérico (`"hola"`) caen en dos catch anidados que retornan `0.0` sin reportar nada. La validación de tipos en `AuxSemantico` ya marca el error en otra fase, pero el doble silencio dificulta entender resultados raros en la tabla de símbolos al depurar.

**Fix sugerido:** loguear `Level.FINE` con el token para inspección.

**Por qué no se tocó:** comportamiento defensivo a propósito; añadir log podría ensuciar consola en uso normal.

---

### 🟡 M1. `AuxSemantico.validarVariablesNoDeclaradas` cruza `i-2` sin chequeo explícito

**Archivo:** `src/code/semantico/AuxSemantico.java:17`

`tokenEn(i-2, …)` se apoya en el guard interno (`pos < 0 → false`), pero la intención (saltar declaraciones) se diluye.

**Fix sugerido:**
```java
if (i < 2 || TokenUtils.tokenEn(i - 2, TokenTipo.VAR)) continue;
```

**Por qué no se tocó:** funcionalmente correcto; cambio puramente estilístico.

---

### 🟡 M3. `EvaluadorExpresiones.esOperando` acepta IDs no declarados

**Archivo:** `src/code/semantico/EvaluadorExpresiones.java:143-153`

Cualquier identificador con regex `[a-zA-Z_][a-zA-Z0-9_]*` se acepta como operando aunque no esté en `tablaSimbolos`. `resolverOperando` luego devuelve `0.0` silencioso.

La validación previa de "variable no declarada" cubre el caso en el flujo principal, pero un consumidor que invoque `EvaluadorExpresiones` directo (test, futura herramienta) obtiene resultados engañosos.

**Por qué no se tocó:** sólo afecta uso fuera del flujo principal.

---

### 🟡 M4. `loopConTope` por reflexión depende del nombre interno `producciones`

**Archivo:** `src/code/sintactico/AnalizadorSintactico.java:250`

Si `compilerTools.jar` cambia ese nombre privado, hay fallback al método sin tope (sin guard de iteraciones). Cubierto, pero la dependencia debe documentarse junto con la versión esperada del jar.

**Versión actual:** `compilerTools-2.3.7.jar`.

**Por qué no se tocó:** es la mejor herramienta disponible mientras compilerTools no exponga API pública.

---

### 🟡 M5. `MAX_ITERACIONES_SINTACTICO = 1000` hardcoded

**Archivo:** `src/code/sintactico/AnalizadorSintactico.java:24`

Si la gramática crece (anidamientos profundos), el tope podría volverse insuficiente. Los 12 tests actuales convergen en <50 iteraciones (holgura ~20×).

**Fix sugerido:** exponer como propiedad de sistema:
```java
private static final int MAX_ITERACIONES_SINTACTICO =
    Integer.getInteger("idestudio.maxIter", 1000);
```

---

### 🟡 M6. Sin feedback claro cuando no hay ruta de archivo

**Archivo:** `src/code/GestorCompilador.java`

Si el archivo no se ha guardado, `generarIntermedio` emite el mensaje correcto en consola, pero la pestaña queda sin pista visual de que el `.3d` no se generó.

**Fix sugerido (UX):** deshabilitar el botón "Compilar y Correr" hasta que haya ruta, o forzar diálogo "Guardar antes de ejecutar".

**Por qué no se tocó:** decisión de UX que requiere visto bueno.

---

### 🟡 M7. Salida ANSI cubre solo SGR

**Archivo:** `src/code/sintactico/AnalizadorSintactico.java`

La regex `\033\\[[;\\d]*m` cubre Select Graphic Rendition (colores). Si `compilerTools.Grammar` empieza a emitir otras secuencias CSI (cursor, viewport), pasarían sin filtrar.

**Estado:** sin reportes de otras secuencias en el jar 2.3.7. Vigilancia.

---

### 🟢 B4. `e.printStackTrace()` en `TestCompiler.main`

**Archivo:** `src/code/TestCompiler.java`

Se mantiene **intencionalmente**: es CLI y la traza completa es útil al desarrollador. Documentado.

---

## 📊 Resumen

| Severidad | Pendientes |
|-----------|-----------|
| 🔴 Críticos | 0 |
| 🟠 Altos | 2 (A1, A2) |
| 🟡 Medios | 5 (M1, M3, M4, M5, M6, M7) |
| 🟢 Bajos | 1 (B4, intencional) |

**Verificación funcional:** 12/12 tests en `test/` siguen produciendo `.3d` válidos tras el refactor.

---

## ✅ RESUELTOS (en este branch — historial)

Detalle completo en el commit `a1b469e`:

- **C1** — `ProcesadorAsignaciones.procesar()` entraba en bucle silencioso si `indiceSiguiente` devolvía -1. Ahora valida y rompe el loop.
- **C2** — `procesarDeclaracion()` accedía a `comp(pos+1)` sin guardia. Reescrito con `pos < tokens.size()`.
- **A3** — `Repositorio.idDeclaraciones` y `erroresSemanticos` eran código muerto. Eliminados.
- **A4** — `Compilar.listaTokens` ahora `public static final` + helper `hayErroresLexicos()`.
- **A5** — 19 string literals (`"INICIO"`, `"FIN"`, `"MOSTRAR"`, `"FOR"`, `"SWITCH"`, `"CASE"`, `"DEFAULT"`, `"BREAK"`, `"SINO"`, `"LLAVE_IZQ"`, `"LLAVE_DER"`, `"DOS_PUNTOS"`, `"OP_INCREMENTO"`, `"OP_DECREMENTO"`, `"ERROR"`, `"PAREN_IZQ"`, `"PAREN_DER"`, `"LEER"`) movidos a constantes en `TokenTipo` y referenciados desde 7 archivos.
- **M2** — `validarTiposEnAsignaciones` accedía a `i+2` sin chequeo. Añadida guardia `i + 2 >= listaTokens.size()`.
- **B1** — Import huérfano `code.semantico.FiltroParentesis` removido de `TestCompiler.java`.
- **B2** — 11 comment-fences decorativas `// ─── XXX ───` reducidas a separadores semánticos.
- **B3** — Bloque "CORRECCIONES" de 17 líneas en `GeneradorCodigoIntermedio` (paráfrasis de fixes pasados) eliminado.

Refactors estructurales relacionados:

- `GeneradorCodigoIntermedio.java`: 803 → 551 líneas (-31 %); helpers de cursor (`skipParenIzq/Der`, `skipLlaveIzq/Der`, `skipPuntoComa`), helpers de recolección (`recolectarHastaParenCierre`, `recolectarCondicion`), helpers de emisión (`emitirExpresion`, `emitirExpresionEnRango`), dispatcher unificado (`generarSentencia`, `generarCuerpoCaso`).
- `GestorCompilador.java`: partido en 7 métodos cortos por responsabilidad.
- `AuxSemantico.java`: 343 → 267 líneas; helpers de tipo movidos a `TokenUtils`.
- `Repositorio.java`: colecciones marcadas `final`.

---

_Última auditoría: 2026-05-09. Próxima revisión sugerida tras añadir un test específico para A1 (`logico := -y- -o-`)._
