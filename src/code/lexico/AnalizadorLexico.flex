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
%ignorecase
%column

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

    Entero = [0-9]+
    UnCaracter = [^\r\n] //Todo lo que no sea \r\n
    CaracterSinComillas = [^"\""]
    ContenidoComentario  = ( [^*] | \*+ [^/*] )*   /* lo que puede contener un comentario */
    ComentarioDocumentar = "/**" {ContenidoComentario} "*"+ "/"
    ComentarioBloque   = "-#[" ([^]]+ | "]" [^#] )* "]#"
    ComentarioLinea    = "-#" [^\n\r]*
    Comentario         = {ComentarioLinea} | {ComentarioBloque}
Flotante = {Entero}"."{Entero}

Identificador = [_a-zA-Z][_a-zA-Z0-9]*
  Cadena = \"[^\"\r\n]*\"
Caracter = \'[^\r\n]\'

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

    "switch"        {return token(yytext(),"SWITCH",yyline,yycolumn);}
    "case"          {return token(yytext(),"CASE",yyline,yycolumn);}
    "default"       {return token(yytext(),"DEFAULT",yyline,yycolumn);}
    "break"         {return token(yytext(),"BREAK",yyline,yycolumn);}

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
    "~"             {return token(yytext(),"LOGICO_NOT",yyline,yycolumn);}

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

    {ComentarioLinea} {/* Ignorar */}
    {Comentario}      {/* Ignorar */}
    {Espacio}         {/* Ignorar */}

/* ===========================================================
   ERROR POR DEFECTO
   =========================================================== */

.                    { return token(yytext(), "ERROR", yyline, yycolumn); }


