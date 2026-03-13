package code;

import compilerTools.Grammar;
import compilerTools.Production;
import compilerTools.Token;
import java.util.ArrayList;
import java.util.HashMap;

public class AnalizadorSintactico {

    private ArrayList<Token> tokens;

    public AnalizadorSintactico(ArrayList<Token> tokens) {
        this.tokens = tokens;
    }
    
public void ejecutar() {
    System.out.println("---- Ejecutando sintáctico ----");
    ArrayList<Token> lista = new ArrayList<>();
    for (Token t : tokens) {
        if (!"ERROR".equals(t.getLexicalComp())) {
            lista.add(t);
        }

    }
        lista = FiltroParentesis.filtrarYValidar(lista);

    
    
    Grammar g = new Grammar(lista, Repositorio.listaErrores);
    
    
    
    // ============================================================
    //  REGLAS BASE
    // ============================================================

    g.group("valor", "(ENTERO | FLOTANTE | CADENA | CARACTER | BOOLEAN_TRUE | BOOLEAN_FALSE)");
    g.group("tipo_dato", "(TIPO_ENTERO | TIPO_DECIMAL | TIPO_TEXTO | TIPO_CAR | TIPO_LOGICO | TIPO_CORTO)");

    // =====================
    // REGLAS RECURSIVAS
    // =====================
 g.loopForFunExecUntilChangeNotDetected(() -> {
    
  
        // PASO 1: DECLARACIONES (básica)
    g.group("declaracion", "VAR tipo_dato IDENTIFICADOR (COMA IDENTIFICADOR)* PUNTO_COMA");
    
    // ERROR: Dos identificadores seguidos sin coma (ej: var entero x y;)
g.group("declaracion", "VAR tipo_dato IDENTIFICADOR IDENTIFICADOR PUNTO_COMA", true, 11, 
    "Error sintáctico: Se esperaba una coma ',' entre los identificadores '" + 
    "#{IDENTIFICADOR[0]}" + "' y '" + "#{IDENTIFICADOR[1]}" + "'");


    // PASO 2: EXPRESIONES (reglas normales)
    g.group("factor", "(PAREN_IZQ expresion PAREN_DER | IDENTIFICADOR | valor)");
    g.group("termino", "factor ((MULTIPLICACION | DIVISION | MODULO) factor)*");
    g.group("expresion", "termino ((SUMA | RESTA | CONCAT) termino)*");
    
    // Declaración con inicialización
    g.group("declaracion", "VAR tipo_dato IDENTIFICADOR ASIGNACION expresion PUNTO_COMA");
    
    // Errores de declaración
    g.group("declaracion", "VAR tipo_dato IDENTIFICADOR ASIGNACION expresion", true, 3,
        "Error sintáctico: Se esperaba ';' al final de la declaración");
    
    g.group("declaracion","VAR tipo_dato IDENTIFICADOR (COMA IDENTIFICADOR)*",true,3,
        "Error sintáctico: Se esperaba ';' al final de la declaración");
    
    g.group("declaracion", "VAR tipo_dato valor", true, 3,
        "Error sintáctico: Se esperaba un identificador válido después del tipo de dato");
    
     
    // PASO 3: ASIGNACIÓN
    g.group("asignacion_st", "expresion ASIGNACION expresion PUNTO_COMA");
    
    g.group("asignacion_st","expresion ASIGNACION expresion",true,3, 
        "Error sintáctico: Se esperaba ';' al final de la asignación");
    
    // =====================

// ERROR: Operador sin operando derecho
g.group("asignacion_st", "IDENTIFICADOR ASIGNACION expresion (SUMA | RESTA | MULTIPLICACION | DIVISION | MODULO) PUNTO_COMA", true, 4,
    "Error sintáctico: La expresión de asignación está incompleta, falta un operando después del operador");

    
    // PASO 4: CONDICIONES
    g.group("condicion_rel",
        "expresion (OP_MAYOR | OP_MENOR | OP_MAYOR_IGUAL | OP_MENOR_IGUAL | OP_IGUAL_IGUAL | OP_DIFERENTE) expresion");
    g.group("condicion_bool", "(BOOLEAN_TRUE | BOOLEAN_FALSE | IDENTIFICADOR)");
    g.group("condicion", "(condicion_rel | condicion_bool)");
    
    
    // Lógicos
g.group("condicion", "LOGICO_NOT condicion");
g.group("condicion", "condicion LOGICO_AND condicion");
g.group("condicion", "condicion LOGICO_OR condicion");

// Errores lógicos
g.group("condicion", "LOGICO_NOT", true, 5,
    "Error sintáctico: Se esperaba una condición después del operador '~' (not)");
g.group("condicion", "condicion LOGICO_AND", true, 5,
    "Error sintáctico: Se esperaba una condición después del operador '-y-' (and)");
g.group("condicion", "condicion LOGICO_OR", true, 5,
    "Error sintáctico: Se esperaba una condición después del operador '-o-' (or)");
g.group("condicion", "LOGICO_AND condicion", true, 5,
    "Error sintáctico: Se esperaba una condición antes del operador '-y-' (and)");
g.group("condicion", "LOGICO_OR condicion", true, 5,
    "Error sintáctico: Se esperaba una condición antes del operador '-o-' (or)");
    // =====================
    // ERRORES EN CONDICIONES
    // =====================
    g.group("condicion_rel", "expresion expresion", true, 5,
        "Error sintáctico: Se esperaba un operador relacional (>, <, >=, <=, ::, :!) entre las expresiones");
    
    g.group("condicion_rel", "expresion (OP_MAYOR | OP_MENOR | OP_MAYOR_IGUAL | OP_MENOR_IGUAL | OP_IGUAL_IGUAL | OP_DIFERENTE)", true, 5,
        "Error sintáctico: Se esperaba una expresión después del operador relacional");
    
    g.group("condicion_rel", "(OP_MAYOR | OP_MENOR | OP_MAYOR_IGUAL | OP_MENOR_IGUAL | OP_IGUAL_IGUAL | OP_DIFERENTE) expresion", true, 5,
        "Error sintáctico: Se esperaba una expresión antes del operador relacional");
    
    g.group("condicion", "valor", true, 5,
        "Error sintáctico: Se esperaba una condición válida (expresión con operador relacional o valor booleano)");
    //////////
    /////////
    ///mostrar
    ///
    ///

    // 1. REGLA CORRECTA (La ideal)
g.group("mostrar_st", "MOSTRAR PAREN_IZQ expresion PAREN_DER PUNTO_COMA");

// 2. REGLA DE RESPALDO (Si la expresión falla, que al menos reconozca un valor simple)
g.group("mostrar_st", "MOSTRAR PAREN_IZQ (valor | IDENTIFICADOR) PAREN_DER PUNTO_COMA");

// 3. REGLA DE ERROR (Si falta el punto y coma)
g.group("mostrar_st", "MOSTRAR PAREN_IZQ (expresion | valor | IDENTIFICADOR)* PAREN_DER", true, 4,
    "Error sintáctico: Se esperaba ';' al final de mostrar");
    // LEER
    g.group("leer_st", "LEER PAREN_IZQ expresion PAREN_DER PUNTO_COMA");
    
    g.group("leer_st","LEER PAREN_IZQ expresion PAREN_DER",true,4, 
        "Error sintáctico: Se esperaba ';' al final de leer");
    
    // =====================
// ERRORES EN LEER: Solo debe aceptar identificadores
// =====================

// LEER - Solo acepta identificadores
g.group("leer_st", "LEER PAREN_IZQ IDENTIFICADOR PAREN_DER PUNTO_COMA");

// ERROR: Leer sin punto y coma
g.group("leer_st","LEER PAREN_IZQ IDENTIFICADOR PAREN_DER",true,4, 
    "Error sintáctico: Se esperaba ';' al final de leer");

// ERROR: Leer vacío
g.group("leer_st", "LEER PAREN_IZQ PAREN_DER PUNTO_COMA", true, 4,
    "Error sintáctico: 'leer' requiere un identificador (variable) dentro de los paréntesis");

// ERROR: Leer con cualquier otra cosa (expresion, valor, etc.) - captura genérica
g.group("leer_st", "LEER PAREN_IZQ expresion PAREN_DER PUNTO_COMA", true, 3,
    "Error sintáctico: 'leer' solo puede recibir variables (identificadores), no expresiones o valores");

g.group("leer_st", "LEER PAREN_IZQ valor PAREN_DER PUNTO_COMA", true, 3,
    "Error sintáctico: 'leer' solo puede recibir variables (identificadores), no valores literales");



// PASO 6: INCREMENTO/DECREMENTO
    g.group("incremento", "(OP_INCREMENTO | OP_DECREMENTO) expresion");
    
    // PASO 7: SENTENCIAS (sin estructuras de control)
    g.group("sentencia", "(declaracion | asignacion_st | mostrar_st | leer_st | if_st | for_st | while_st | switch_st)*");   
    //g.group("sentencia", "(declaracion | asignacion_st | mostrar_st | leer_st)");

    
    
    
    // PASO 8: LISTA DE SENTENCIAS
    g.group("lista_sentencias", "lista_sentencias sentencia"); 
    g.group("lista_sentencias", "sentencia lista_sentencias");
    
    g.group("lista_sentencias", "sentencia+");

    
    
    // PASO 9: BLOQUE (con y sin contenido)
    g.group("bloque", "LLAVE_IZQ lista_sentencias LLAVE_DER");
    g.group("bloque", "LLAVE_IZQ LLAVE_DER");
    
    // PASO 10: IF con ELSE IF
    g.group("if_st",
        "SI PAREN_IZQ condicion PAREN_DER bloque " +
        "(SINO SI PAREN_IZQ condicion PAREN_DER bloque)* " +
        "(SINO bloque)?");
    
    // 2. REGLAS DE ERROR (Solo si la de arriba falló)
g.group("if_st", "SI PAREN_IZQ condicion PAREN_DER LLAVE_IZQ lista_sentencias", true, 10, 
    "Error sintáctico: Se esperaba '}' para cerrar el bloque del 'si'");
    
    // =====================
    // ERRORES DEL IF
    // =====================
    
    // --- ERRORES DE CONDICIÓN INVÁLIDA EN SI ---
    g.group("if_st", "SI PAREN_IZQ PAREN_DER bloque", true, 9,
        "Error sintáctico: Se esperaba una condición dentro de los paréntesis del 'si'");
    
    g.group("if_st", "SI PAREN_IZQ PAREN_DER bloque SINO bloque", true, 9,
        "Error sintáctico: Se esperaba una condición dentro de los paréntesis del 'si'");
    
    g.group("if_st", "SI PAREN_IZQ (valor | expresion) PAREN_DER bloque", true, 9,
        "Error sintáctico: La condición del 'si' debe ser una expresión booleana o una comparación con operadores relacionales");
    
    g.group("if_st", "SI PAREN_IZQ (valor | expresion) PAREN_DER bloque SINO bloque", true, 9,
        "Error sintáctico: La condición del 'si' debe ser una expresión booleana o una comparación con operadores relacionales");
    
    // --- ERRORES DE CONDICIÓN INVÁLIDA EN SINO SI ---
    g.group("if_st", "SI PAREN_IZQ condicion PAREN_DER bloque SINO SI PAREN_IZQ PAREN_DER bloque", true, 9,
        "Error sintáctico: Se esperaba una condición dentro de los paréntesis del 'sino si'");
    
    g.group("if_st", "SI PAREN_IZQ condicion PAREN_DER bloque SINO SI PAREN_IZQ (valor | expresion) PAREN_DER bloque", true, 9,
        "Error sintáctico: La condición del 'sino si' debe ser una expresión booleana o una comparación con operadores relacionales");
    
    // --- ERRORES DE PARÉNTESIS EN SI ---
    g.group("if_st", "SI condicion PAREN_DER bloque", true, 10,
        "Error sintáctico: Se esperaba '(' después de 'si'");
    
    g.group("if_st", "SI PAREN_IZQ condicion bloque", true, 10,
        "Error sintáctico: Se esperaba ')' después de la condición en 'si'");
    
    g.group("if_st", "SI condicion bloque", true, 10,
        "Error sintáctico: Se esperaba '(' y ')' alrededor de la condición en 'si'");
    
    // --- ERRORES DE PARÉNTESIS EN SINO SI ---
    g.group("if_st", "SI PAREN_IZQ condicion PAREN_DER bloque SINO SI condicion PAREN_DER bloque", true, 10,
        "Error sintáctico: Se esperaba '(' después de 'sino si'");
    
    g.group("if_st", "SI PAREN_IZQ condicion PAREN_DER bloque SINO SI PAREN_IZQ condicion bloque", true, 10,
        "Error sintáctico: Se esperaba ')' después de la condición en 'sino si'");
    
    g.group("if_st", "SI PAREN_IZQ condicion PAREN_DER bloque SINO SI condicion bloque", true, 10,
        "Error sintáctico: Se esperaba '(' y ')' alrededor de la condición en 'sino si'");
    
    // --- ERRORES DE LLAVES EN SI ---
    g.group("if_st", "SI PAREN_IZQ condicion PAREN_DER lista_sentencias", true, 10,
        "Error sintáctico: Se esperaba '{' después de la condición del 'si'");
    
    g.group("if_st", "SI PAREN_IZQ condicion PAREN_DER LLAVE_IZQ lista_sentencias", true, 10,
        "Error sintáctico: Se esperaba '}' al final del bloque 'si'");
    
    // --- ERRORES DE LLAVES EN SINO SI ---
    g.group("if_st", "SI PAREN_IZQ condicion PAREN_DER bloque SINO SI PAREN_IZQ condicion PAREN_DER lista_sentencias", true, 10,
        "Error sintáctico: Se esperaba '{' después de la condición del 'sino si'");
    
    g.group("if_st", "SI PAREN_IZQ condicion PAREN_DER bloque SINO SI PAREN_IZQ condicion PAREN_DER LLAVE_IZQ lista_sentencias", true, 10,
        "Error sintáctico: Se esperaba '}' al final del bloque 'sino si'");
    
    // --- ERRORES DE LLAVES EN SINO ---
    g.group("if_st", "if_st SINO lista_sentencias", true, 10,
        "Error sintáctico: Se esperaba '{' después de 'sino'");
    
    g.group("if_st", "if_st SINO LLAVE_IZQ lista_sentencias", true, 10,
        "Error sintáctico: Se esperaba '}' al final del bloque 'sino'");
    
    g.group("if_st", "SI PAREN_IZQ condicion PAREN_DER bloque SINO lista_sentencias", true, 10,
        "Error sintáctico: Se esperaba '{' después de 'sino'");
    
    g.group("if_st", "SI PAREN_IZQ condicion PAREN_DER bloque SINO LLAVE_IZQ lista_sentencias", true, 10,
        "Error sintáctico: Se esperaba '}' al final del bloque 'sino'");
    
    g.group("if_st", "SI PAREN_IZQ condicion PAREN_DER LLAVE_IZQ lista_sentencias SINO lista_sentencias", true, 10,
        "Error sintáctico: Se esperaba '}' en el bloque 'si' antes de 'sino'");
    
    g.group("if_st", "SI PAREN_IZQ condicion PAREN_DER LLAVE_IZQ lista_sentencias LLAVE_DER SINO lista_sentencias", true, 10,
        "Error sintáctico: Se esperaba '{' después de 'sino'");
    
  // PASO 11: FOR
    g.group("for_st",
        "FOR PAREN_IZQ lista_sentencias condicion PUNTO_COMA incremento PAREN_DER bloque");
    
    
// =====================
// ERRORES EN FOR (ISAC)
// =====================

// ERROR: Falta ( después de ISAC
g.group("for_st", "FOR lista_sentencias condicion PUNTO_COMA incremento PAREN_DER bloque", true, 8,
    "Error sintáctico: Se esperaba '(' después de 'isac'");

// ERROR: Falta ) antes del bloque
g.group("for_st", "FOR PAREN_IZQ lista_sentencias condicion PUNTO_COMA incremento bloque", true, 8,
    "Error sintáctico: Se esperaba ')' después del incremento en 'isac'");

// ERROR: Falta ; entre condición e incremento
g.group("for_st", "FOR PAREN_IZQ lista_sentencias condicion incremento PAREN_DER bloque", true, 8,
    "Error sintáctico: Se esperaba ';' después de la condición en 'isac'");

// ERROR: Falta { después del encabezado
g.group("for_st", "FOR PAREN_IZQ lista_sentencias condicion PUNTO_COMA incremento PAREN_DER lista_sentencias", true, 8,
    "Error sintáctico: Se esperaba '{' después del encabezado del 'isac'");

// ERROR: Falta } al final
g.group("for_st", "FOR PAREN_IZQ lista_sentencias condicion PUNTO_COMA incremento PAREN_DER LLAVE_IZQ lista_sentencias", true, 8,
    "Error sintáctico: Se esperaba '}' al final del bloque 'isac'");

// ERROR: Falta := en la inicialización
g.group("for_st", "FOR PAREN_IZQ IDENTIFICADOR expresion PUNTO_COMA condicion PUNTO_COMA incremento PAREN_DER bloque", true, 8,
    "Error sintáctico: Se esperaba ':=' en la inicialización del 'isac'");

g.group("for_st", "FOR PAREN_IZQ IDENTIFICADOR valor PUNTO_COMA condicion PUNTO_COMA incremento PAREN_DER bloque", true, 8,
    "Error sintáctico: Se esperaba ':=' en la inicialización del 'isac'");

// ERROR: Falta operador relacional en la condición
g.group("for_st", "FOR PAREN_IZQ lista_sentencias IDENTIFICADOR expresion PUNTO_COMA incremento PAREN_DER bloque", true, 8,
    "Error sintáctico: Se esperaba un operador relacional (>, <, >=, <=, ::, :!) en la condición del 'isac'");

g.group("for_st", "FOR PAREN_IZQ lista_sentencias IDENTIFICADOR valor PUNTO_COMA incremento PAREN_DER bloque", true, 8,
    "Error sintáctico: Se esperaba un operador relacional (>, <, >=, <=, ::, :!) en la condición del 'isac'");

// ERROR: Falta operador de incremento/decremento
g.group("for_st", "FOR PAREN_IZQ lista_sentencias condicion PUNTO_COMA IDENTIFICADOR PAREN_DER bloque", true, 8,
    "Error sintáctico: Se esperaba un operador de incremento ('++>' o '--<') en el 'isac'");

g.group("for_st", "FOR PAREN_IZQ lista_sentencias condicion PUNTO_COMA expresion PAREN_DER bloque", true, 8,
    "Error sintáctico: Se esperaba un operador de incremento ('++>' o '--<') en el 'isac'");

// ERROR: Incremento sin identificador
g.group("for_st", "FOR PAREN_IZQ lista_sentencias condicion PUNTO_COMA OP_INCREMENTO PAREN_DER bloque", true, 8,
    "Error sintáctico: Se esperaba un identificador después del operador de incremento en 'isac'");

g.group("for_st", "FOR PAREN_IZQ lista_sentencias condicion PUNTO_COMA OP_DECREMENTO PAREN_DER bloque", true, 8,
    "Error sintáctico: Se esperaba un identificador después del operador de decremento en 'isac'");


// PASO 11.5: WHILE (diego)
g.group("while_st", "WHILE PAREN_IZQ condicion PAREN_DER bloque");

// =====================
// ERRORES EN WHILE (DIEGO)
// =====================

// ERROR: Falta ( después de WHILE
g.group("while_st", "WHILE condicion PAREN_DER bloque", true, 8,
    "Error sintáctico: Se esperaba '(' después de 'diego' (while)");

// ERROR: Falta ) antes del bloque
g.group("while_st", "WHILE PAREN_IZQ condicion bloque", true, 8,
    "Error sintáctico: Se esperaba ')' después de la condición en 'diego' (while)");

// ERROR: Condición vacía
g.group("while_st", "WHILE PAREN_IZQ PAREN_DER bloque", true, 8,
    "Error sintáctico: Se esperaba una condición dentro de los paréntesis de 'diego' (while)");

// ERROR: Falta { después de la condición
g.group("while_st", "WHILE PAREN_IZQ condicion PAREN_DER lista_sentencias", true, 8,
    "Error sintáctico: Se esperaba '{' después de la condición en 'diego' (while)");

// ERROR: Falta } al final
g.group("while_st", "WHILE PAREN_IZQ condicion PAREN_DER LLAVE_IZQ lista_sentencias", true, 8,
    "Error sintáctico: Se esperaba '}' al final del bloque 'diego' (while)");

    // PASO 12: SWITCH
    g.group("case_st", "CASE valor DOS_PUNTOS lista_sentencias");
    g.group("default_st", "DEFAULT DOS_PUNTOS lista_sentencias");
    g.group("switch_st",
        "SWITCH PAREN_IZQ expresion PAREN_DER LLAVE_IZQ case_st+ default_st? LLAVE_DER");
    
    // =====================
// ERRORES EN SWITCH
// =====================

// ERROR: Falta ( después de SWITCH
g.group("switch_st", "SWITCH expresion PAREN_DER LLAVE_IZQ case_st+ default_st? LLAVE_DER", true, 9,
    "Error sintáctico: Se esperaba '(' después de 'switch'");

// ERROR: Falta ) antes de {
g.group("switch_st", "SWITCH PAREN_IZQ expresion LLAVE_IZQ case_st+ default_st? LLAVE_DER", true, 9,
    "Error sintáctico: Se esperaba ')' después de la expresión en 'switch'");

// ERROR: Switch sin expresión (paréntesis vacíos)
g.group("switch_st", "SWITCH PAREN_IZQ PAREN_DER LLAVE_IZQ case_st+ default_st? LLAVE_DER", true, 9,
    "Error sintáctico: Se esperaba una expresión dentro de los paréntesis del 'switch'");

// ERROR: Falta { después del encabezado
g.group("switch_st", "SWITCH PAREN_IZQ expresion PAREN_DER case_st+ default_st?", true, 9,
    "Error sintáctico: Se esperaba '{' después del encabezado del 'switch'");

// ERROR: Falta } al final
g.group("switch_st", "SWITCH PAREN_IZQ expresion PAREN_DER LLAVE_IZQ case_st+ default_st?", true, 9,
    "Error sintáctico: Se esperaba '}' al final del 'switch'");

// ERROR: CASE sin dos puntos
g.group("case_st", "CASE valor lista_sentencias", true, 9,
    "Error sintáctico: Se esperaba ':' después del valor en 'case'");

// ERROR: CASE sin valor
g.group("case_st", "CASE DOS_PUNTOS lista_sentencias", true, 9,
    "Error sintáctico: Se esperaba un valor después de 'case'");

// ERROR: DEFAULT sin dos puntos
g.group("default_st", "DEFAULT lista_sentencias", true, 9,
    "Error sintáctico: Se esperaba ':' después de 'default'");

// ERROR: Switch vacío (sin cases)
g.group("switch_st", "SWITCH PAREN_IZQ expresion PAREN_DER LLAVE_IZQ LLAVE_DER", true, 9,
    "Error sintáctico: El 'switch' debe contener al menos un 'case'");
    
    // PASO 13: REDEFINIR sentencia incluyendo estructuras de control
    g.group("sentencia", "(declaracion | asignacion_st | mostrar_st | leer_st | if_st | for_st | switch_st)");
    
    g.group("asignacion_st", "IDENTIFICADOR ASIGNACION ENTERO ENTERO PUNTO_COMA", true, 7,
    "Error sintáctico: Se esperaba un operador aritmético (+, -, *, /, %) entre los valores en la asignación");

g.group("asignacion_st", "IDENTIFICADOR ASIGNACION FLOTANTE FLOTANTE PUNTO_COMA", true, 7,
    "Error sintáctico: Se esperaba un operador aritmético (+, -, *, /, %) entre los valores en la asignación");

g.group("asignacion_st", "IDENTIFICADOR ASIGNACION ENTERO FLOTANTE PUNTO_COMA", true, 7,
    "Error sintáctico: Se esperaba un operador aritmético (+, -, *, /, %) entre los valores en la asignación");

g.group("asignacion_st", "IDENTIFICADOR ASIGNACION FLOTANTE ENTERO PUNTO_COMA", true, 7,
    "Error sintáctico: Se esperaba un operador aritmético (+, -, *, /, %) entre los valores en la asignación");

g.group("asignacion_st", "IDENTIFICADOR ASIGNACION IDENTIFICADOR ENTERO PUNTO_COMA", true, 7,
    "Error sintáctico: Se esperaba un operador aritmético (+, -, *, /, %) entre los valores en la asignación");

g.group("asignacion_st", "IDENTIFICADOR ASIGNACION ENTERO IDENTIFICADOR PUNTO_COMA", true, 7,
    "Error sintáctico: Se esperaba un operador aritmético (+, -, *, /, %) entre los valores en la asignación");

g.group("asignacion_st", "IDENTIFICADOR ASIGNACION IDENTIFICADOR IDENTIFICADOR PUNTO_COMA", true, 7,
    "Error sintáctico: Se esperaba un operador aritmético (+, -, *, /, %) entre los valores en la asignación");


    // =====================
    // ERRORES: TOKENS INESPERADOS
    // =====================
    g.group("sentencia", "IDENTIFICADOR", true, 1,
        "Error sintáctico: Se encontró un símbolo inesperado en esta posición del programa");
    
    g.group("sentencia", "valor", true, 1,
        "Error sintáctico: Se encontró un símbolo inesperado en esta posición del programa");
    
    g.group("sentencia", "(SUMA | RESTA | MULTIPLICACION | DIVISION | MODULO)", true, 1,
        "Error sintáctico: Se encontró un símbolo inesperado en esta posición del programa");
    
    g.group("sentencia", "(OP_MAYOR | OP_MENOR | OP_IGUAL_IGUAL | OP_DIFERENTE)", true, 1,
        "Error sintáctico: Se encontró un símbolo inesperado en esta posición del programa");
    
    g.group("sentencia", "(COMA | PUNTO_COMA | DOS_PUNTOS)", true, 1,
        "Error sintáctico: Se encontró un símbolo inesperado en esta posición del programa");
    
    g.group("sentencia", "expresion", true, 1,
        "Error sintáctico: Se encontró un símbolo inesperado en esta posición del programa");
    
    // PASO 14: REDEFINIR lista_sentencias
    g.group("lista_sentencias", "sentencia+");
    
    // PASO 15: COMBINAR múltiples lista_sentencias
    g.group("lista_sentencias", "lista_sentencias lista_sentencias");
});
// =====================
// PROGRAMA FINAL
// =====================
g.group("bloque", "LLAVE_IZQ lista_sentencias LLAVE_DER");
g.group("bloque", "LLAVE_IZQ LLAVE_DER");
g.group("programa", "INICIO bloque FIN");

// ERRORES DE INICIO / FIN
g.group("programa","INICIO bloque",true,3, 
    "Error sintáctico: Se esperaba la palabra reservada 'FIN' al final del programa");

g.group("programa","bloque FIN",true,1,
    "Error sintáctico: Se esperaba la palabra reservada 'INICIO' al inicio del programa");

g.group("programa","bloque",true,1,
    "Error sintáctico: El programa debe iniciar con 'INICIO' y finalizar con 'FIN'");

// =====================
// ERRORES DE LLAVES EN PROGRAMA PRINCIPAL
// =====================
g.group("programa", "INICIO lista_sentencias LLAVE_DER FIN", true, 2,
    "Error sintáctico: Se esperaba '{' después de 'INICIO'");

// ERROR 15: Falta llave de cierre '}' antes de FIN
g.group("programa", "INICIO LLAVE_IZQ lista_sentencias FIN", true, 3,
    "Error sintáctico: Se esperaba '}' antes de 'FIN'. Verifica que todos los bloques (si, sino, isac) estén cerrados correctamente");

g.group("programa", "INICIO lista_sentencias FIN", true, 2,
    "Error sintáctico: Se esperaba '{' después de 'INICIO' y '}' antes de 'FIN'");

// ERROR: Solo INICIO sin nada más
g.group("programa", "INICIO", true, 1,
    "Error sintáctico: Se esperaba '{' después de 'INICIO', el cuerpo del programa y 'FIN'");

// ERROR: Solo FIN sin nada más
g.group("programa", "FIN", true, 1,
    "Error sintáctico: Se esperaba 'INICIO' antes de 'FIN' y la estructura completa del programa");

// ERROR: INICIO con algo que no es bloque ni lista_sentencias
g.group("programa", "INICIO sentencia", true, 2,
    "Error sintáctico: Se esperaba '{' después de 'INICIO'");

// ERROR: Algo antes de FIN que no es bloque
g.group("programa", "sentencia FIN", true, 1,
    "Error sintáctico: Se esperaba 'INICIO' antes de 'FIN'");

// ERROR: Sentencia antes de INICIO
g.group("programa", "sentencia INICIO bloque FIN", true, 1,
    "Error sintáctico: No se permite código antes de 'INICIO'. Todo el código debe estar dentro de 'INICIO { } FIN'");

// ERROR: Declaración antes de INICIO
g.group("programa", "declaracion INICIO bloque FIN", true, 1,
    "Error sintáctico: No se permite código antes de 'INICIO'. Todo el código debe estar dentro de 'INICIO { } FIN'");

// ERROR: Sentencia después de FIN
g.group("programa", "INICIO bloque FIN sentencia", true, 4,
    "Error sintáctico: No se permite código después de 'FIN'. Todo el código debe estar dentro de 'INICIO { } FIN'");

// ERROR: Declaración después de FIN
g.group("programa", "INICIO bloque FIN declaracion", true, 4,
    "Error sintáctico: No se permite código después de 'FIN'. Todo el código debe estar dentro de 'INICIO { } FIN'");

// ERROR: Código antes Y después
g.group("programa", "sentencia INICIO bloque FIN sentencia", true, 1,
    "Error sintáctico: No se permite código fuera de 'INICIO { } FIN'. Todo el código debe estar dentro de la estructura principal");


// =====================
// ERRORES: CÓDIGO SIN INICIO/FIN
// =====================

// ERROR: Solo sentencias sin estructura
g.group("programa", "sentencia+", true, 1,
    "Error sintáctico: El código debe estar dentro de la estructura 'INICIO { } FIN'");

// ERROR: Solo declaraciones sin estructura
g.group("programa", "declaracion+", true, 1,
    "Error sintáctico: El código debe estar dentro de la estructura 'INICIO { } FIN'");

// ERROR: Solo lista_sentencias sin estructura
g.group("programa", "lista_sentencias", true, 1,
    "Error sintáctico: El código debe estar dentro de la estructura 'INICIO { } FIN'");

// ERROR: Solo if_st sin estructura
g.group("programa", "if_st", true, 1,
    "Error sintáctico: El código debe estar dentro de la estructura 'INICIO { } FIN'");

// ERROR: Solo for sin estructura
g.group("programa", "for_st", true, 1,
    "Error sintáctico: El código debe estar dentro de la estructura 'INICIO { } FIN'");

// ERROR: Solo bloque sin INICIO/FIN
g.group("programa", "bloque", true, 1,
    "Error sintáctico: El código debe estar dentro de la estructura 'INICIO { } FIN'");


g.show();


}
private ArrayList<Token> filtrarParentesisEnAsignaciones(ArrayList<Token> tokens) {
    ArrayList<Token> resultado = new ArrayList<>();
    boolean dentroDeAsignacion = false;
    
    for (int i = 0; i < tokens.size(); i++) {
        Token t = tokens.get(i);
        
        // Detectar inicio de asignación
        if ("ASIGNACION".equals(t.getLexicalComp())) {
            dentroDeAsignacion = true;
            resultado.add(t);
            continue;
        }
        
        // Detectar fin de asignación
        if ("PUNTO_COMA".equals(t.getLexicalComp())) {
            dentroDeAsignacion = false;
            resultado.add(t);
            continue;
        }
        
        // Si estamos dentro de asignación, ignorar paréntesis
        if (dentroDeAsignacion && 
            ("PAREN_IZQ".equals(t.getLexicalComp()) || "PAREN_DER".equals(t.getLexicalComp()))) {
            // NO agregar paréntesis (los saltamos)
            continue;
        }
        
        // Agregar todos los demás tokens
        resultado.add(t);
    }
    
    return resultado;
}
}

