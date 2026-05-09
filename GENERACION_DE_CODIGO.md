# Generación de código — Pipeline completo

**Proyecto:** IDEstudioV2
**Lenguaje fuente:** `.id` (sintaxis tipo Pascal/C en español: `inicio`, `fin`, `var`, `si`, `sino`, `mientras`, `isac`/`for`, `suich`/`switch`, `mostrar`, `leer`, etc.)
**Lenguaje objetivo:** código intermedio de tres direcciones (`.3d`)

Este documento describe el pipeline de compilación paso a paso, dónde vive cada fase, cómo se conectan y cómo extender el lenguaje sin romper nada.

---

## 1. Vista general del pipeline

```
        archivo.id
           │
           ▼
  ┌────────────────────┐
  │ 1. Léxico          │  AnalizadorLexico (jflex → .java)
  │    Compilar.java   │  → Compilar.listaTokens (incluye ERROR)
  └────────────────────┘
           │
           ▼ (filtra ERROR)
  ┌────────────────────┐
  │ 2. Sintáctico      │  AnalizadorSintactico
  │    Grammar         │  → Repositorio.listaTokensFiltrados
  │    + FiltroParen   │  → Repositorio.listaErrores
  └────────────────────┘
           │
           ▼
  ┌────────────────────┐
  │ 3. Semántico       │  AnalizadorSemantico (fachada)
  │                    │  ├─ TablaSimbolos.construir()
  │                    │  ├─ ProcesadorAsignaciones.procesar()
  │                    │  ├─ ProcesadorAsignaciones.validarSentenciasLeer()
  │                    │  └─ AuxSemantico.* (6 validadores)
  │                    │  → Repositorio.tablaSimbolos
  │                    │  → Repositorio.listaErrores (más entradas)
  └────────────────────┘
           │
           ▼  (sólo si listaErrores está vacía y no hay ERROR léxico)
  ┌────────────────────┐
  │ 4. Intermedio      │  GeneradorCodigoIntermedio + Generador3D
  │    .3d             │  → archivo.3d en disco
  └────────────────────┘
```

Orquestador: `code.GestorCompilador.ejecutarCompilacion(...)`. Variante CLI: `code.TestCompiler.main(...)`.

---

## 2. Fase 1 — Análisis Léxico

**Punto de entrada:** `code.Compilar.analizar(RSyntaxTextArea, DefaultTableModel)`

**Cómo funciona:**

1. Toma el texto del editor.
2. Crea un `AnalizadorLexico` (generado por jflex desde `src/code/lexico/AnalizadorLexico.flex`).
3. Llama `yylex()` repetidamente hasta `null`.
4. Cada `Token` se vuelca a:
   - el `DefaultTableModel` (para el panel de tokens en la GUI),
   - `Compilar.listaTokens` (lista cruda, **incluye** tokens `ERROR`).
5. Cierra el reader y el lexer en `try-with-resources` para evitar fugas de file descriptors.

**Salida:** `Compilar.listaTokens : ArrayList<Token>` — todos los tokens, incluso los inválidos.

**Por qué se conservan los `ERROR`:** la GUI los muestra en su propia sección. El sintáctico los filtra justo antes de invocar el `Grammar`.

**Helper público:** `Compilar.hayErroresLexicos()` — retorna `true` si algún token tiene `lexicalComp == TokenTipo.ERROR`.

---

## 3. Fase 2 — Análisis Sintáctico

**Punto de entrada:** `code.sintactico.AnalizadorSintactico(tokens).ejecutar()`

### 3.1 Pre-validación estructural

Antes de invocar al `Grammar`, `validarEstructuraBasica()` inspecciona la lista para detectar:
- Programa vacío → error 200
- Falta `inicio` → error 201
- Falta `fin`    → error 202

Esto da línea/columna útiles que `compilerTools.Grammar` no produciría (devolvería `0,0`).

### 3.2 Filtro de paréntesis (semántico)

`FiltroParentesis.filtrarYValidar(lista)` recorre la lista linealmente:
- Detecta `IDENTIFICADOR ASIGNACION ...;` y cuenta `(` y `)` dentro.
- Si al `;` el contador > 0  → error `PAREN_DESEQUILIBRADO_ABIERTOS` (id 110).
- Si en algún punto el contador < 0 → error `PAREN_DESEQUILIBRADO_CERRADOS` (id 111).
- Si dentro de la asignación aparece una palabra clave de estructura (`mostrar`, `leer`, `si`, `sino`, `mientras`, `isac`, `suich`, `caso`, `defecto`, `rompe`), desactiva el modo asignación y sigue.

Devuelve la lista intacta — los paréntesis se preservan para el `Grammar` y para la fase 3D.

### 3.3 `compilerTools.Grammar`

Construye reglas BNF en agrupaciones bottom-up (`g.group(nombre, "...")`). Las reglas se procesan en un loop con tope `MAX_ITERACIONES_SINTACTICO = 1000` que aborta si la gramática no converge.

Categorías:
- **Expresiones**: `factor`, `termino`, `expresion`, `condicion_rel`, `condicion`
- **Sentencias atómicas**: `decl_st`, `asig_st`, `io_st`, `inc_st`, `brk_st`
- **Estructuras**: `bloque`, `if_base`, `if_st`, `while_st`, `for_st`, `case_st`, `def_st`, `switch_st`
- **Programa**: `INICIO bloque FIN`

**Solapamiento intencional `expresion | condicion`:** los paréntesis pueden contener cualquiera de las dos; el parser bottom-up prueba ambas alternativas. Romper este invariante (un consumidor que sólo acepte una) silencia errores.

### 3.4 Salida ANSI

Si `System.console() == null` (CI, tests, GUI), la salida del `Grammar` se redirige a un buffer y se sanea con regex `\033\\[[;\\d]*m` antes de imprimirse.

---

## 4. Fase 3 — Análisis Semántico

**Fachada:** `code.semantico.AnalizadorSemantico` — métodos estáticos que delegan en submódulos.

Orden de invocación desde `GestorCompilador`:

```java
AnalizadorSemantico.construirTablaSimbolos();        // 4.1
AnalizadorSemantico.procesarAsignaciones();          // 4.2
AnalizadorSemantico.validarSentenciasLeer();         // 4.3
AnalizadorSemantico.validarSemantica();              // 4.4 (los 6 validadores)
```

### 4.1 `TablaSimbolos.construir()`

Recorre `Repositorio.listaTokens` buscando `VAR tipo id [, id, …]`:
- Para cada IDENTIFICADOR en una declaración, crea un `Simbolo` con tipo, valor por defecto (`0`, `0.0`, `""`, `''`, `falso`, según tipo) y categoría `VAR`.
- Si la variable trae `:= expr`, evalúa la expresión (con `ProcesadorAsignaciones.evaluarExpresion(... soloHastaCom=true)`) y guarda ese valor.
- Variables duplicadas → error `VAR_DUPLICADA` (id 107).

**Atención al recorrido**: en `var T a, b := 5, c, d;` el cursor debe avanzar hasta la siguiente COMA o ';' tras consumir el `:=` para no devorar las variables siguientes.

### 4.2 `ProcesadorAsignaciones.procesar()`

Para cada `IDENTIFICADOR ASIGNACION ... ;`:
- Comprueba `tieneErrorDeTipo` (si la variable es entero/corto y la expresión contiene FLOTANTE o variable DECIMAL).
- Si pasa el filtro, evalúa la expresión con `evaluarExpresion(...)` y actualiza `simbolo.valor` en la tabla.

### 4.3 `ProcesadorAsignaciones.validarSentenciasLeer()`

Para cada `LEER ( id ) ;`, verifica que `id` esté en `tablaSimbolos`. Si no, registra `VAR_NO_INICIALIZADA` (id 102) — mensaje "no ha sido declarada" porque ese es el escenario que dispara este validador.

### 4.4 `AuxSemantico.validarSemantica()` — los 6 validadores

| Validador | Detecta |
|-----------|---------|
| `validarVariablesNoDeclaradas` | uso de un id sin entrada en la tabla |
| `validarVariablesNoInicializadas` | uso de una var declarada pero sin valor (no `:=`, no `leer(id)`) |
| `validarTiposEnAsignaciones` | tipo destino vs tipo expresión, recorriendo cada token de la RHS (FLOTANTE en entera, CADENA en decimal, etc.) |
| `validarTiposEnOperaciones` | aritmética (`+ - * /`) con operandos no numéricos |
| `validarCondicionesBooleanas` | condiciones de `si`/`mientras` que no son comparación ni `logico` |
| `validarTiposEnComparaciones` | `<`, `>`, `==`, etc. entre tipos no comparables |

### 4.5 `EvaluadorExpresiones`

Implementación clásica de **Shunting-Yard**:
- `infijaAPostfija(ArrayList<String>)` — convierte tokens infijos a postfijos respetando precedencia.
- `evaluarPostfija(ArrayList<String>)` — evalúa el postfijo con una pila de doubles, devuelve string formateado.

**Precedencia (de mayor a menor):**

| Nivel | Operador |
|-------|----------|
| 5 | `~` (NOT) |
| 4 | `*` `/` `%` |
| 3 | `+` `-` `&+` (concat) |
| 2 | `-y-` (AND) |
| 1 | `-o-` (OR) |

---

## 5. Fase 4 — Generación de código intermedio (.3d)

**Sólo se ejecuta si la compilación está limpia** (no `ERROR` léxicos, no errores en `listaErrores`, no errores de paréntesis).

**Orquestación:** `GestorCompilador.generarIntermedio(...)` en GUI, o directo desde `TestCompiler.main`.

```java
Generador3D gen3D = new Generador3D();
gen3D.inicializar();                                  // contadores T1=0, L1=0
gen3D.crearArchivo(rutaArchivoFuente);                // abre archivo.3d UTF-8
new GeneradorCodigoIntermedio(gen3D).generar(TokenTipo.FIN);
gen3D.cerrar();
```

### 5.1 `Generador3D` — emisor de instrucciones

Mantiene contadores `contadorTemp` (T1, T2, …) y `contadorEtiq` (L1, L2, …). Su único método de salida es:

```java
gc(operacion, op1, op2, resultado)
```

Casos soportados:

| `operacion` | Salida en .3d |
|-------------|---------------|
| `"ASIGNACION"` | `resultado := op1 ;` |
| `":="` | `resultado := op1 ;` |
| `"+"`, `"-"`, `"*"`, `"/"`, `"%"`, `"&+"` | `resultado := op1 OP op2 ;` |
| `"LEER"` | `LEER resultado ;` |
| `"MOSTRAR"` | `MOSTRAR resultado ;` |
| `"IF"` | `if op1 then GOTO resultado ;` |
| `"GOTO"` | `GOTO resultado ;` |
| `"LABEL"` | `resultado :` |

### 5.2 `GeneradorCodigoIntermedio` — recorrido y emisión

**Cursor:** `pos` apunta al token actual de `tokens` (lista filtrada de `Repositorio`).

**Helpers atómicos** (todos avanzan `pos` si la condición se cumple):

```java
boolean skipParenIzq();        // ( → pos++
boolean skipParenDer();        // ) → pos++
boolean skipLlaveIzq();        // { → pos++
boolean skipLlaveDer();        // } → pos++
boolean skipPuntoComa();       // ; → pos++
ArrayList<String> recolectarHastaParenCierre();   // recolecta operandos
String recolectarCondicion();                      // recolecta lexemas crudos en "(…)"
String emitirExpresion(ArrayList<String> exp);     // 0/1 operando o postfija → 3D
String emitirExpresionEnRango(int start, int endExcl);
```

**Punto de entrada:** `generar(tipoTokenFinal)` corre `generarSentencia()` en bucle hasta encontrar `tipoTokenFinal` (sin consumirlo).
- Llamada raíz: `generar(TokenTipo.FIN)`
- Llamada de bloque: `generar(TokenTipo.LLAVE_DER)`

**`generarSentencia()`** despacha por `comp(pos)` a uno de:

| Tipo de token | Procesador |
|---------------|------------|
| `VAR` | `procesarDeclaracion()` |
| `MOSTRAR` | `procesarMostrar()` |
| `LEER` | `procesarLeer()` |
| `SI` | `procesarSi()` |
| `WHILE` | `procesarWhile()` |
| `FOR` | `procesarFor()` |
| `SWITCH` | `procesarSwitch()` |
| `IDENTIFICADOR` (con `:=` después) | `procesarAsignacion()` |
| `BREAK` | consume `BREAK` y `;` (sólo flag para switch) |
| Otros | `pos++` |

### 5.3 Ejemplo: `procesarSi`

Pseudocódigo de la emisión:

```
IF (cond) GOTO L_true        ← gen.gc("IF", cond, "", L_true)
GOTO L_false                  ← gen.gc("GOTO", "", "", L_false)
L_true:                       ← gen.gc("LABEL", "", "", L_true)
   {body}                     ← generar(LLAVE_DER)
[Si hay SINO:
GOTO L_fin                    ← gen.gc("GOTO", "", "", L_fin)
L_false:                      ← gen.gc("LABEL", "", "", L_false)
   {else-body}
L_fin:                        ← gen.gc("LABEL", "", "", L_fin)]
[Si NO hay SINO:
L_false:                      ← gen.gc("LABEL", "", "", L_false)]
```

### 5.4 Ejemplo: `procesarFor`

```
FOR ( init ; cond ; incr ) { body }
```

1. **Localizar** `firstSemi`, `secondSemi` y `parenClose` con un solo recorrido respetando profundidad de paréntesis anidados.
2. **Emitir init** (`generarInitFor`): puede ser `VAR tipo id := expr` o `id := expr`.
3. `LABEL L_regreso`
4. Reconstruir condición como string `"(...)"` (no se evalúa en compilación).
5. `IF (cond) GOTO L_true / GOTO L_false / LABEL L_true`
6. **Recolectar incrementos** (`++>id` / `--<id`) sin emitirlos todavía — se almacenan en una lista.
7. Procesar el cuerpo con `generar(LLAVE_DER)`.
8. **Emitir incrementos** después del cuerpo: `gen.gc("+", id, "1", id)` o `gen.gc("-", id, "1", id)`.
9. `GOTO L_regreso / LABEL L_falso`.

### 5.5 Ejemplo: `procesarSwitch`

Es de tres pasadas:

1. **Pasada 1** — recorrer el bloque `{ ... }` localizando cada `caso valor:` y `defecto:`. Almacenar valores y posiciones de inicio de cuerpo. Asignar etiquetas.
2. **Pasada 2** — emitir los `IF (var :: valor) GOTO L_caso_i` y un `GOTO L_defecto` o `GOTO L_fin` final.
3. **Pasada 3** — para cada caso, emitir `LABEL L_caso_i` y `generarCuerpoCaso(inicio)` (que reusa `generarSentencia` hasta encontrar `BREAK`/siguiente `CASE`/`DEFAULT`/`}`), luego `GOTO L_fin`.

### 5.6 Expresiones aritméticas — `generarPostfija3D`

Recibe la lista postfija (de `EvaluadorExpresiones.infijaAPostfija`) y emite código de tres direcciones con pila de operandos. Por cada operador, hace `pop op2; pop op1; T_n := op1 OP op2; push T_n`. Devuelve el último temporal/operando como resultado.

---

## 6. Cómo añadir una sentencia nueva al lenguaje

Ejemplo: añadir `repetir N { body }` (loop con contador).

### 6.1 Lexer (`AnalizadorLexico.flex`)

```
"repetir" { return new Token(yytext(), "REPETIR", yyline+1, yycolumn+1); }
```

Regenerar: `jflex src/code/lexico/AnalizadorLexico.flex` (o desde NetBeans).

### 6.2 Constante en `TokenTipo`

```java
public static final String REPETIR = "REPETIR";
```

### 6.3 Gramática (`AnalizadorSintactico.ejecutarInterno`)

```java
g.group("repetir_st", "REPETIR (expresion | IDENTIFICADOR) bloque");
g.group("sent_complex", "if_st | for_st | while_st | switch_st | repetir_st");
```

### 6.4 Generador intermedio (`GeneradorCodigoIntermedio`)

1. Añadir caso en `generarSentencia()`:

```java
case TokenTipo.REPETIR: procesarRepetir(); break;
```

2. Implementar `procesarRepetir()` siguiendo el patrón de `procesarWhile`:

```java
private void procesarRepetir() {
    String etiqRegreso = gen.nuevaEtiq();
    String etiqFin     = gen.nuevaEtiq();
    String contador    = gen.nuevaTemp();

    pos++;                                   // REPETIR
    String n = lex(pos); pos++;              // N (id o número)

    gen.gc("ASIGNACION", "0", "", contador);
    gen.gc("LABEL", "", "", etiqRegreso);
    gen.gc("IF", "(" + contador + "<" + n + ")", "", etiqRegreso + "_true");
    gen.gc("GOTO", "", "", etiqFin);
    gen.gc("LABEL", "", "", etiqRegreso + "_true");

    skipLlaveIzq();
    generar(TokenTipo.LLAVE_DER);
    skipLlaveDer();

    gen.gc("+", contador, "1", contador);
    gen.gc("GOTO", "", "", etiqRegreso);
    gen.gc("LABEL", "", "", etiqFin);
}
```

### 6.5 (Opcional) Validación semántica

Si `repetir N` exige `N entero/corto`, añadir un caso a `AuxSemantico` (nuevo método `validarRepetir`).

### 6.6 (Opcional) Highlighter

Añadir la palabra reservada al `IDEstudioTokenMaker` para que el editor la pinte como keyword.

---

## 7. Cosas a recordar (que se olvidan)

1. **`Repositorio.limpiar()` debe llamarse al inicio de cada compilación.** `GestorCompilador.ejecutarCompilacion` lo hace; los tests también deberían si construyen su propio flujo.
2. **`Compilar.listaTokens` mantiene `ERROR`; `Repositorio.listaTokens` no.** Cuando dudes cuál usar, recuerda: léxico = "todo lo que vio el lexer"; sintáctico/semántico/3D = "tokens válidos".
3. **El `.3d` se genera SIEMPRE en `~/dirArchivo.id` reemplazando `.id` por `.3d`.** Si el archivo no se ha guardado, no hay ruta y se omite la generación con un mensaje informativo.
4. **`compilerTools.Grammar` reduce bottom-up.** Una regla nueva puede competir con `factor` o `condicion` y eliminarlas si no se piensa bien la precedencia. Si añades una sentencia, prefiérela como `_st` que se promueva a `sent_complex`.
5. **Los lexemas dentro de condiciones de `si`/`mientras`/`isac` se vuelcan crudos al `.3d`.** No se evalúan: el `.3d` representa el AND/OR como string para que el intérprete del 3D los procese.
6. **Los IDs de error tienen rangos:** 100-109 = semántica, 110-119 = paréntesis, 200-299 = sintáctico estructural. Mantener la convención simplifica filtrado en GUI.
7. **`MAX_ITERACIONES_SINTACTICO = 1000`** es defensivo. Si el programa converge en >50 iter, conviene investigar antes de subir el tope.
8. **Para depurar el .3d**, es más rápido `./test.sh test/prueba_xxx.id` que abrir la GUI. El script recompila si los `.java` cambiaron.
9. **Los validadores semánticos son independientes.** Cada uno recorre `listaTokens` por sí mismo, porque cada uno mira ventanas distintas (`i-2..i+2`). No intentes fusionarlos en un solo pase: pierdes legibilidad.
10. **El `loopConTope` usa reflexión** sobre el campo `producciones` de `compilerTools.Grammar`. Si actualizas el jar y el campo cambia de nombre, hay fallback al método sin tope, pero el guard se pierde.

---

## 8. Archivos clave (referencia rápida)

| Concepto | Archivo |
|----------|---------|
| Catálogo de tokens | `src/code/semantico/TokenTipo.java` |
| Catálogo de IDs de error | `src/code/semantico/ErrorSemantico.java` |
| Estado global de la compilación | `src/code/semantico/Repositorio.java` |
| Helpers de tokens | `src/code/semantico/TokenUtils.java` |
| Lexer (definición) | `src/code/lexico/AnalizadorLexico.flex` |
| Lexer (generado) | `src/code/lexico/AnalizadorLexico.java` |
| Sintáctico | `src/code/sintactico/AnalizadorSintactico.java` |
| Semántico (fachada) | `src/code/semantico/AnalizadorSemantico.java` |
| Validadores semánticos | `src/code/semantico/AuxSemantico.java` |
| Tabla de símbolos | `src/code/semantico/TablaSimbolos.java` |
| Asignaciones / lecturas | `src/code/semantico/ProcesadorAsignaciones.java` |
| Filtro de paréntesis | `src/code/semantico/FiltroParentesis.java` |
| Evaluador shunting-yard | `src/code/semantico/EvaluadorExpresiones.java` |
| Generador 3D (recorrido) | `src/code/intermedio/GeneradorCodigoIntermedio.java` |
| Generador 3D (emisor de líneas) | `src/code/intermedio/Generador3D.java` |
| Orquestador GUI | `src/code/GestorCompilador.java` |
| CLI driver | `src/code/TestCompiler.java` |
| Léxico → tabla GUI | `src/code/Compilar.java` |
