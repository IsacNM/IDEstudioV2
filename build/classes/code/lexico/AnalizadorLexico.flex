package code.lexico;

import compilerTools.Token;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

%%
%class AnalizadorLexico
%public
%type Token
%line
%column
%unicode

%init{
    //Seccion del Constructor  
%init}

%{
  private Token token(String lexema, String componenteLexico, int line, int column)
   {
    return new Token(lexema, componenteLexico, line+1, column+1);
   }
%}

    //  Un salto de línea es un \r, \n o \r\n dependiendo del SO  
    TerminadorLinea = \r|\n|\r\n
    Espacio     = {TerminadorLinea} | [ \t\f]

    // ENTERO: solo dígitos sin signo. Los literales negativos como `-5`
    // se tokenizan como dos tokens (RESTA + ENTERO) y se reconstruyen
    // en las fases sintáctica/semántica. Esto es intencional para no
    // romper expresiones aritméticas como `a-5` o `-a + b`.
    Entero = [0-9]+

    // ComentarioBloque -#[ ... ]#  (multilínea, no anidable)
    // Reescrito para parar limpiamente al ver "]#" sin importar lo que
    // haya antes. La versión anterior usaba `[^]]+ | "]" [^#]` que era
    // frágil con `]` adyacente a otros caracteres.
    ComentarioBloque   = "-#[" ( [^\]] | "]" [^#] )* "]" "#"

    // ComentarioLinea -# ...   (hasta fin de línea)
    // El primer char tras `-#` NO puede ser `[` para que no compita con
    // ComentarioBloque (que arranca con `-#[`). El cuerpo es opcional
    // para permitir comentarios vacíos `-#` solos en una línea.
    ComentarioLinea    = "-#" ( [^\[\n\r] [^\n\r]* )?

    Comentario         = {ComentarioBloque} | {ComentarioLinea}

    Flotante = {Entero}"."{Entero}

    // Identificador: ahora acepta letras y dígitos Unicode (incluye
    // acentos y ñ, p.ej.  `año`, `niño`, `año_actual`). Esto es
    // consistente con que los comentarios sí permiten Unicode.
    Identificador = [_\p{L}][_\p{L}\p{N}]*

    // Cadena: soporta escapes `\\`, `\"`, `\n`, `\t`, etc. La cadena no
    // puede contener saltos de línea sin escapar (siguen prohibidos).
    Cadena   = \"([^\"\\\r\n] | \\.)*\"

    // Caracter: una letra o un escape (\' \\ \n \t ...)
    Caracter = \'([^\'\\\r\n] | \\.)\'

%%

/* ===========================================================
   PALABRAS RESERVADAS
   =========================================================== */

    "inicio"        {return token(yytext(),"INICIO",yyline,yycolumn);}
    "fin"           {return token(yytext(),"FIN",yyline,yycolumn);}
    "si"            {return token(yytext(),"SI",yyline,yycolumn);}
    "sino"          {return token(yytext(),"SINO",yyline,yycolumn);}
    "mostrar"       {return token(yytext(),"MOSTRAR",yyline,yycolumn);}
    "leer"          {return token(yytext(),"LEER",yyline,yycolumn);}
    "var"           {return token(yytext(),"VAR",yyline,yycolumn);}
    "const"         {return token(yytext(),"CONST",yyline,yycolumn);}

    "entero"        {return token(yytext(),"TIPO_ENTERO",yyline,yycolumn);}
    "decimal"       {return token(yytext(),"TIPO_DECIMAL",yyline,yycolumn);}
    "texto"         {return token(yytext(),"TIPO_TEXTO",yyline,yycolumn);}
    "car"           {return token(yytext(),"TIPO_CAR",yyline,yycolumn);}
    "logico"        {return token(yytext(),"TIPO_LOGICO",yyline,yycolumn);}
    "corto"         {return token(yytext(),"TIPO_CORTO",yyline,yycolumn);}

    "suich"         {return token(yytext(),"SWITCH",yyline,yycolumn);}
    "caso"          {return token(yytext(),"CASE",yyline,yycolumn);}
    "defecto"       {return token(yytext(),"DEFAULT",yyline,yycolumn);}
    "rompe"         {return token(yytext(),"BREAK",yyline,yycolumn);}

    "isac"          {return token(yytext(),"FOR",yyline,yycolumn);}
    "diego"         {return token(yytext(),"WHILE",yyline,yycolumn);}
    "repite"        {return token(yytext(),"REPITE",yyline,yycolumn);}
    "hasta"         {return token(yytext(),"HASTA",yyline,yycolumn);}

    "cierto"        {return token(yytext(),"BOOLEAN_TRUE",yyline,yycolumn);}
    "falso"         {return token(yytext(),"BOOLEAN_FALSE",yyline,yycolumn);}

/* ===========================================================
   OPERADORES ARITMÉTICOS (PRIMERO LOS LARGOS)
   =========================================================== */

    "++>"           {return token(yytext(),"OP_INCREMENTO",yyline,yycolumn);}
    "--<"           {return token(yytext(),"OP_DECREMENTO",yyline,yycolumn);}

    "**"            {return token(yytext(),"OP_MULT_MULTI",yyline,yycolumn);}
"//" { return token(yytext(),"OP_DIV_DIV",yyline,yycolumn); }
    "%%"            {return token(yytext(),"OP_MOD_MOD",yyline,yycolumn);}

    
/* Simples */
    "+"             {return token(yytext(),"SUMA",yyline,yycolumn);}
    "-"             {return token(yytext(),"RESTA",yyline,yycolumn);}
    "*"             {return token(yytext(),"MULTIPLICACION",yyline,yycolumn);}
    "/"             {return token(yytext(),"DIVISION",yyline,yycolumn);}
    "%"             {return token(yytext(),"MODULO",yyline,yycolumn);}

/* ===========================================================
   OPERADORES RELACIONALES
   =========================================================== */

    ">="           {return token(yytext(),"OP_MAYOR_IGUAL",yyline,yycolumn);}
    "<="           {return token(yytext(),"OP_MENOR_IGUAL",yyline,yycolumn);}

    "::"            {return token(yytext(),"OP_IGUAL_IGUAL",yyline,yycolumn);}
    ":!"            {return token(yytext(),"OP_DIFERENTE",yyline,yycolumn);}

    ">"            {return token(yytext(),"OP_MAYOR",yyline,yycolumn);}
    "<"            {return token(yytext(),"OP_MENOR",yyline,yycolumn);}

    ":="            {return token(yytext(),"ASIGNACION",yyline,yycolumn);}
    ":"        { return token(yytext(), "DOS_PUNTOS", yyline, yycolumn); }


/* ===========================================================
   OPERADORES LÓGICOS
   =========================================================== */

    "-y-"           {return token(yytext(),"LOGICO_AND",yyline,yycolumn);}
    "-o-"           {return token(yytext(),"LOGICO_OR",yyline,yycolumn);}
    "-n-"             {return token(yytext(),"LOGICO_NOT",yyline,yycolumn);}

/* ===========================================================
   ASIGNACIÓN Y CONCATENACIÓN
   =========================================================== */

    "&+"            {return token(yytext(),"CONCAT",yyline,yycolumn);}

/* ===========================================================
   PUNTUACIÓN
   =========================================================== */

    ","        { return token(yytext(), "COMA", yyline, yycolumn); }
    ";"        { return token(yytext(), "PUNTO_COMA", yyline, yycolumn); }
    "("        { return token(yytext(), "PAREN_IZQ", yyline, yycolumn); }
    ")"        { return token(yytext(), "PAREN_DER", yyline, yycolumn); }
    "{"        { return token(yytext(), "LLAVE_IZQ", yyline, yycolumn); }
    "}"        { return token(yytext(), "LLAVE_DER", yyline, yycolumn); }

    "["             {return token(yytext(),"CORCH_IZQ",yyline,yycolumn);}
    "]"             {return token(yytext(),"CORCH_DER",yyline,yycolumn);}

    "."        { return token(yytext(), "PUNTO", yyline, yycolumn); }

/* ===========================================================
   TIPOS DE LEXEMAS YA DEFINIDOS POR TI
   =========================================================== */

    {Cadena}        {return token(yytext(),"CADENA",yyline,yycolumn);}
    {Caracter}      {return token(yytext(),"CARACTER",yyline,yycolumn);}
{Flotante}      {return token(yytext(),"FLOTANTE",yyline,yycolumn);}
    {Entero}        {return token(yytext(),"ENTERO",yyline,yycolumn);}      
    {Identificador} {return token(yytext(),"IDENTIFICADOR",yyline,yycolumn);}

/* ===========================================================
   IGNORAR COMENTARIOS Y ESPACIOS
   =========================================================== */

    // Una única regla `{Comentario}` cubre tanto la línea como el bloque
    // (la antigua regla duplicada `{ComentarioLinea}` era redundante).
    {Comentario}      {/* Ignorar */}
    {Espacio}         {/* Ignorar */}

/* ===========================================================
   ERROR POR DEFECTO
   =========================================================== */

.                    { return token(yytext(), "ERROR", yyline, yycolumn); }


