# Código duplicado / Spaguetti — Inventario y deduplicación

**Rama:** `limpieza`
**Fecha:** 2026-05-09

Este documento lista los patrones que aparecían repetidos en el codebase, su localización y qué se hizo (o se debe hacer) para deduplicarlos.

---

## 1. Comprobación de tokens ERROR

### Antes

```java
if (!"ERROR".equals(t.getLexicalComp())) { ... }     // GestorCompilador.java:44, 80, 121, 143
if (!t.getLexicalComp().equals("ERROR"))             // TestCompiler.java:69
if (!"ERROR".equals(t.getLexicalComp()))             // AnalizadorSintactico.java:102
if ("ERROR".equals(...)) { ... }                     // Compilar.java (filtro implícito)
```

7 puntos diferentes con el mismo string literal hardcodeado.

### Después

- `TokenTipo.ERROR` añadido al catálogo central.
- `Compilar.hayErroresLexicos()` encapsula el patrón "recorrer `Compilar.listaTokens` buscando un ERROR".
- `GestorCompilador` ahora llama a ese helper, eliminando 3 copias del bucle.

---

## 2. Tracking de profundidad de paréntesis

### Antes — 5 copias del mismo patrón en `GeneradorCodigoIntermedio.java`

```java
int depth = 0;
while (pos < tokens.size()) {
    String c = comp(pos);
    if ("PAREN_DER".equals(c) && depth == 0) break;
    if ("PAREN_IZQ".equals(c)) depth++;
    else if ("PAREN_DER".equals(c)) depth--;
    /* ...append/recolectar... */
    pos++;
}
if ("PAREN_DER".equals(comp(pos))) pos++;
```

Apariciones:

| Método | Línea aprox. (antes) | Propósito |
|--------|----------------------|-----------|
| `procesarMostrar` | 229-239 | recolectar operandos |
| `procesarSi` | 330-340 | recolectar lexemas para condición |
| `procesarWhile` | 398-408 | recolectar lexemas para condición |
| `procesarFor` | 450-462 | localizar `firstSemi/secondSemi/parenClose` |
| `procesarSwitch` | 603-613 | recolectar operandos del switch |

### Después

Dos helpers en `GeneradorCodigoIntermedio`:

```java
private ArrayList<String> recolectarHastaParenCierre()  // recolecta operandos vía tokenAOperando
private String recolectarCondicion()                    // recolecta lexemas crudos en "(…)"
```

Más helpers atómicos: `skipParenIzq`, `skipParenDer`, `skipLlaveIzq`, `skipLlaveDer`, `skipPuntoComa`. Cada uno:

```java
private boolean skipParenIzq() {
    if (esApertura(comp(pos))) { pos++; return true; }
    return false;
}
```

`procesarMostrar`, `procesarSi`, `procesarWhile`, `procesarSwitch` ahora consisten en tres líneas (`skipParenIzq → recolectar → skipParenDer`) en vez del bucle inline.

---

## 3. Tracking de profundidad de llaves dentro de switch

### Antes — 2 copias

Bucles casi idénticos en `procesarSwitch()`:
- Pasada 1 (localizar casos): líneas 635-673
- Cuerpos de cada caso: líneas 691-718

Ambos:
```java
int dC = 0;
while (pos < tokens.size()) {
    String c = comp(pos);
    if (dC == 0 && ("BREAK".equals(c) || "CASE".equals(c) || ...)) break;
    if ("LLAVE_IZQ".equals(c)) dC++;
    if ("LLAVE_DER".equals(c)) dC--;
    generarSentencia();
}
```

### Después

Extraído a `generarCuerpoCaso(int inicio)`. Llamado tanto desde la fase de cuerpos de cada `case` como del `default`.

---

## 4. Patrón "construir expresión y emitir 3D"

### Antes — 3 copias en `GeneradorCodigoIntermedio.java`

```java
ArrayList<String> expArit = new ArrayList<>();
while (j < endPos /* y otras condiciones */) {
    String op = tokenAOperando(j);
    if (op != null) expArit.add(op);
    j++;
}
if (expArit.size() == 1) {
    gen.gc("ASIGNACION", expArit.get(0), "", nombreVar);
} else {
    String res = generarPostfija3D(EvaluadorExpresiones.infijaAPostfija(expArit));
    gen.gc(":=", res, "", nombreVar);
}
```

Apariciones:
- `procesarDeclaracion`  (líneas 182-200)
- `procesarAsignacion`   (líneas 284-302)
- `generarInitFor`       (líneas 538-570)

### Después

Dos helpers:

```java
private String emitirExpresion(ArrayList<String> exp);                 // exp ya recolectada
private String emitirExpresionEnRango(int start, int endExcl);         // exp por rango de tokens
```

Cada llamador queda en 2-4 líneas: localizar rango, llamar helper, emitir `gc(...)` con el resultado.

---

## 5. Familias de tipos (numérico / no numérico)

### Antes — 3 implementaciones de la misma idea

```java
// AuxSemantico.java:207-211
private static boolean esTipoNoNumerico(String tipo) {
    return TokenTipo.TIPO_TEXTO.equals(tipo)
        || TokenTipo.TIPO_CAR.equals(tipo)
        || TokenTipo.TIPO_LOGICO.equals(tipo);
}

// AuxSemantico.java:340-342
private static boolean esTipoNumerico(String tipo) {
    return TokenTipo.TIPO_ENTERO.equals(tipo)
        || TokenTipo.TIPO_DECIMAL.equals(tipo)
        || TokenTipo.TIPO_CORTO.equals(tipo);
}

// Implícito en `tieneErrorDeTipo` (ProcesadorAsignaciones.java:107)
if (!TokenTipo.TIPO_ENTERO.equals(tipoVar) && !TokenTipo.TIPO_CORTO.equals(tipoVar))
```

### Después

Movidas a **`TokenUtils`** como API pública:

```java
public static boolean esTipoNumerico(String tipo);
public static boolean esTipoNoNumerico(String tipo);
```

`AuxSemantico` ya no las redefine, sólo las consume.

---

## 6. Mapeo de operadores (token → símbolo string)

### Patrón duplicado entre dos clases

| Mapping | Ubicación |
|---------|-----------|
| `TokenTipo.SUMA → "+"` etc. | `ProcesadorAsignaciones.tokenAOperando` (líneas 123-144) |
| `TokenTipo.SUMA → "+"` etc. | `GeneradorCodigoIntermedio.tokenAOperando` (líneas 777-795) |

Ambos métodos cubren el mismo conjunto de operadores; difieren en si manejan `LOGICO_AND/OR/NOT` y `CARACTER`.

### Estado

**No deduplicado**. Razón: las dos clases viven en paquetes distintos (`code.semantico` vs `code.intermedio`) y tienen necesidades sutilmente distintas:
- `ProcesadorAsignaciones` necesita `~`/`-y-`/`-o-` para evaluar condiciones lógicas en tiempo de compilación.
- `GeneradorCodigoIntermedio` no genera 3D para esos operadores actualmente (los condicionales se vuelcan al `.3d` como cadena cruda dentro del IF).

**Recomendación futura:** crear `code.lenguaje.OperadorRegistry` con tres bandas (aritméticos, relacionales, lógicos) y que ambas clases consulten ahí.

---

## 7. Bucle `for-i` sobre `Repositorio.listaTokens`

### Frecuencia

9 apariciones casi idénticas:

```java
for (int i = 0; i < Repositorio.listaTokens.size(); i++) {
    Token token = Repositorio.listaTokens.get(i);
    if (!XYZ.equals(token.getLexicalComp())) continue;
    /* ...validar... */
}
```

| Archivo | Métodos |
|---------|---------|
| `AuxSemantico.java` | 6× (cada validador) |
| `TablaSimbolos.java` | 1× (`construir`) |
| `ProcesadorAsignaciones.java` | 2× (`procesar`, `validarSentenciasLeer`) |

### Estado

**No deduplicado**. Razón: la mayoría de los validadores necesita acceder al **índice `i`** (no sólo al token) para mirar `i-1`, `i+1`, `i+2`. Un `forEach` con consumer pierde esa información. Una alternativa con `BiConsumer<Integer, Token>` añadiría más ruido del que quita en métodos cortos.

**Aceptable** dejarlo como está; el patrón es legible y consistente.

---

## 8. Filtrado de `ERROR` antes del sintáctico

### Antes — 3 copias

```java
ArrayList<Token> lista = new ArrayList<>();
for (Token t : tokens) {
    if (!"ERROR".equals(t.getLexicalComp())) lista.add(t);
}
```

| Archivo | Línea (antes) |
|---------|---------------|
| `Compilar.java` | implícito en analizar |
| `GestorCompilador.java` | 43-47 |
| `TestCompiler.java` | 69-71 |
| `AnalizadorSintactico.ejecutarInterno` | 100-105 |

### Estado

Sintáctico mantiene su filtro local porque el `Repositorio.listaTokens` ya viene filtrado desde `GestorCompilador`, pero el sintáctico se invoca también desde `TestCompiler` (que no filtra; ahí los ERROR se cuentan aparte). El doble filtro (gestor + sintáctico) es defensa por capas y se queda.

`GestorCompilador` y `TestCompiler` ahora consultan `Compilar.hayErroresLexicos()` en vez de reimplementar el bucle.

---

## 9. Mensaje plantilla "Error semántico: La variable '...' no ha sido declarada"

### Antes

```java
new ErrorLSSL(
    ErrorSemantico.VAR_NO_DECLARADA.id,
    "Error semántico: La variable '" + token.getLexeme() + "' no ha sido declarada.",
    token);
```

Este string aparece literalmente en:
- `AuxSemantico.validarVariablesNoDeclaradas`
- `ProcesadorAsignaciones.validarSentenciasLeer` (con id distinto pero mismo mensaje)
- `ProcesadorAsignaciones.evaluarExpresion` (con `OPERACION_INVALIDA` como id)

### Estado

**No deduplicado**. Los 3 sitios usan **IDs distintos** intencionalmente (la GUI puede filtrar por id). Compactar el mensaje en una constante perdería la información de fase. Aceptable.

---

## 10. Dispatcher de sentencias en intermedio

### Antes — 2 implementaciones casi idénticas

```java
// generar() línea 92-157  vs  generarSentencia() línea 730-749
switch (comp(pos)) {
    case TokenTipo.VAR:        procesarDeclaracion();  break;
    case "MOSTRAR":            procesarMostrar();      break;
    /* ...todos los casos... */
}
```

`generar()` lo tenía inline + bucle exterior; `generarSentencia()` (usado dentro de switch) lo tenía idéntico pero sin bucle.

### Después

Un solo `generarSentencia()` con todos los casos. `generar(tipoFinal)` ahora es:

```java
public void generar(String tipoTokenFinal) {
    while (pos < tokens.size() && !comp(pos).equals(tipoTokenFinal)) {
        generarSentencia();
    }
}
```

---

## Resumen cuantitativo

| Patrón | Antes | Después |
|--------|-------|---------|
| Comprobación `"ERROR"` literal | 7 sitios | 1 helper + 1 constante |
| Depth tracking de paréntesis | 5 bucles inline | 2 helpers |
| Depth tracking de llaves | 2 bucles inline | 1 helper |
| Patrón "expArit + emitir" | 3 sitios | 1 helper (`emitirExpresionEnRango`) |
| `esTipoNumerico/esTipoNoNumerico` | 3 lugares | 1 lugar (`TokenUtils`) |
| Dispatcher de sentencias | 2 switches | 1 switch (`generarSentencia`) |
| Bloque `// ─── XXX ───` decorativo | 11 | 8 (sólo separadores semánticos restantes) |
| Comentario "CORRECCIONES" obsoleto | 17 líneas | 0 |

Reducción en `GeneradorCodigoIntermedio.java`: de **803** a **551 líneas** (-31 %).
