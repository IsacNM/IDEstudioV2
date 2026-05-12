package code.semantico;

/**
 * Catálogo central de los lexicalComp emitidos por el lexer.
 * Toda comparación de tokens en el resto del compilador debería usar
 * estas constantes en vez de string literales.
 */
public final class TokenTipo {
    private TokenTipo() {}

    // Identificadores y estructura
    public static final String IDENTIFICADOR  = "IDENTIFICADOR";
    public static final String VAR            = "VAR";
    public static final String CONST          = "CONST";
    public static final String ASIGNACION     = "ASIGNACION";
    public static final String COMA           = "COMA";
    public static final String PUNTO_COMA     = "PUNTO_COMA";
    public static final String DOS_PUNTOS     = "DOS_PUNTOS";
    public static final String PAREN_IZQ      = "PAREN_IZQ";
    public static final String PAREN_DER      = "PAREN_DER";
    public static final String LLAVE_IZQ      = "LLAVE_IZQ";
    public static final String LLAVE_DER      = "LLAVE_DER";
    public static final String ERROR          = "ERROR";

    // Programa y bloques
    public static final String INICIO         = "INICIO";
    public static final String FIN            = "FIN";

    // Tipos de dato
    public static final String TIPO_ENTERO    = "TIPO_ENTERO";
    public static final String TIPO_DECIMAL   = "TIPO_DECIMAL";
    public static final String TIPO_TEXTO     = "TIPO_TEXTO";
    public static final String TIPO_LOGICO    = "TIPO_LOGICO";
    public static final String TIPO_CAR       = "TIPO_CAR";
    public static final String TIPO_CORTO     = "TIPO_CORTO";

    // Literales
    public static final String ENTERO         = "ENTERO";
    public static final String FLOTANTE       = "FLOTANTE";
    public static final String CADENA         = "CADENA";
    public static final String CARACTER       = "CARACTER";
    public static final String BOOLEAN_TRUE   = "BOOLEAN_TRUE";
    public static final String BOOLEAN_FALSE  = "BOOLEAN_FALSE";

    // Operadores aritméticos
    public static final String SUMA           = "SUMA";
    public static final String RESTA          = "RESTA";
    public static final String MULTIPLICACION = "MULTIPLICACION";
    public static final String DIVISION       = "DIVISION";
    public static final String MODULO         = "MODULO";
    public static final String CONCAT         = "CONCAT";
    public static final String OP_INCREMENTO  = "OP_INCREMENTO";
    public static final String OP_DECREMENTO  = "OP_DECREMENTO";

    // Operadores lógicos
    public static final String LOGICO_AND     = "LOGICO_AND";
    public static final String LOGICO_OR      = "LOGICO_OR";
    public static final String LOGICO_NOT     = "LOGICO_NOT";

    // Operadores relacionales
    public static final String OP_MAYOR       = "OP_MAYOR";
    public static final String OP_MENOR       = "OP_MENOR";
    public static final String OP_MAYOR_IGUAL = "OP_MAYOR_IGUAL";
    public static final String OP_MENOR_IGUAL = "OP_MENOR_IGUAL";
    public static final String OP_IGUAL_IGUAL = "OP_IGUAL_IGUAL";
    public static final String OP_DIFERENTE   = "OP_DIFERENTE";

    // I/O
    public static final String MOSTRAR        = "MOSTRAR";
    public static final String LEER           = "LEER";

    // Estructuras de control
    public static final String SI             = "SI";
    public static final String SINO           = "SINO";
    public static final String WHILE          = "WHILE";
    public static final String FOR            = "FOR";
    public static final String SWITCH         = "SWITCH";
    public static final String CASE           = "CASE";
    public static final String DEFAULT        = "DEFAULT";
    public static final String BREAK          = "BREAK";
    public static final String REPITE         = "REPITE";
    public static final String HASTA          = "HASTA";
}
