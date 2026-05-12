package code.sintactico;

import code.semantico.FiltroParentesis;
import code.semantico.Repositorio;
import code.semantico.TokenTipo;
import compilerTools.Grammar;
import compilerTools.Token;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class AnalizadorSintactico {

    /**
     * Tope defensivo de iteraciones para
     * {@link Grammar#loopForFunExecUntilChangeNotDetected}.
     * 1000 ciclos cubre con holgura los programas más anidados de la suite
     * (que típicamente convergen en &lt; 50 iter). Si se alcanza, se
     * detiene el análisis y se reporta — protege ante entradas patológicas
     * que pudieran no converger.
     */
    private static final int MAX_ITERACIONES_SINTACTICO = 1000;

    private ArrayList<Token> tokens;

    public AnalizadorSintactico(ArrayList<Token> tokens) {
        this.tokens = tokens;
    }

    public void ejecutar() {
        // Validación previa de estructura básica: archivo vacío / sin INICIO
        // / sin FIN. Reporta con línea/columna útiles antes de invocar al
        // Grammar (que devolvería línea 0, columna 0 en estos casos).
        validarEstructuraBasica();

        // Si stdout no es un TTY (tests, CI, archivo, GUI), redirigimos toda
        // la salida del análisis a un buffer y filtramos los códigos ANSI
        // que emite compilerTools.Grammar. En terminal interactivo se deja
        // pasar tal cual para conservar el coloreado.
        boolean esTty = System.console() != null;
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = esTty ? null : new ByteArrayOutputStream();
        if (!esTty) {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        }
        try {
            System.out.println("Ejecutando sintáctico:");
            ejecutarInterno();
        } finally {
            if (!esTty) {
                System.out.flush();
                System.setOut(original);
                original.print(ANSI.matcher(buffer.toString(StandardCharsets.UTF_8)).replaceAll(""));
            }
        }
    }

    /** Valida que el programa tenga al menos `inicio` y `fin`. */
    private void validarEstructuraBasica() {
        if (tokens.isEmpty()) {
            Repositorio.listaErrores.add(new compilerTools.ErrorLSSL(
                200,
                "Error sintáctico: Programa vacío. Se esperaba 'inicio { ... } fin'.",
                new Token("<vacío>", "INICIO_AUSENTE", 1, 1)
            ));
            return;
        }

        boolean tieneInicio = false;
        boolean tieneFin    = false;
        for (Token t : tokens) {
            if (TokenTipo.INICIO.equals(t.getLexicalComp())) tieneInicio = true;
            else if (TokenTipo.FIN.equals(t.getLexicalComp())) tieneFin = true;
        }

        Token primer = tokens.get(0);
        Token ultimo = tokens.get(tokens.size() - 1);

        if (!tieneInicio) {
            Repositorio.listaErrores.add(new compilerTools.ErrorLSSL(
                201,
                "Error sintáctico: Falta la palabra reservada 'inicio' al comenzar el programa.",
                new Token(primer.getLexeme(), "INICIO_AUSENTE",
                          primer.getLine(), primer.getColumn())
            ));
        }
        if (!tieneFin) {
            Repositorio.listaErrores.add(new compilerTools.ErrorLSSL(
                202,
                "Error sintáctico: Falta la palabra reservada 'fin' al cerrar el programa.",
                new Token(ultimo.getLexeme(), "FIN_AUSENTE",
                          ultimo.getLine(), ultimo.getColumn())
            ));
        }
    }

    private void ejecutarInterno() {
        ArrayList<Token> lista = new ArrayList<>();
        for (Token t : tokens) {
            if (!TokenTipo.ERROR.equals(t.getLexicalComp())) {
                lista.add(t);
            }
        }
        lista = FiltroParentesis.filtrarYValidar(lista);
        Repositorio.listaTokensFiltrados.addAll(lista);

        Grammar g = new Grammar(lista, Repositorio.listaErrores);

        // ============================================================
        //  REGLAS BASE
        // ============================================================
        g.group("valor", "ENTERO | FLOTANTE | CADENA | CARACTER | BOOLEAN_TRUE | BOOLEAN_FALSE");
        g.group("tipo_dato", "TIPO_ENTERO | TIPO_DECIMAL | TIPO_TEXTO | TIPO_CAR | TIPO_LOGICO | TIPO_CORTO");

        loopConTope(g, MAX_ITERACIONES_SINTACTICO, () -> {

            // --- 1. EXPRESIONES Y CONDICIONES (Cimientos) ---
            //
            // NOTA SOBRE EL SOLAPAMIENTO factor/condicion (a propósito):
            //   `factor` y `condicion` aceptan ambos `PAREN_IZQ ... PAREN_DER`.
            //   Esto es intencional porque el parser bottom-up no sabe a priori
            //   si un grupo entre paréntesis es aritmético (`(a+b)`) o lógico
            //   (`(a > 0)`). Las dos reglas se prueban en cada iteración y la
            //   que case con el contexto consumidor (asignación, condición de
            //   if/while/for) es la que se mantiene.
            //
            //   Esto solo es seguro mientras todos los nodos consumidores
            //   ('asig_st', 'if_base', 'while_st', 'for_st', 'switch_st',
            //   'io_st', 'inc_st') acepten `(expresion | condicion)` como
            //   alternativa. Si alguien añade una regla que SOLO acepte uno
            //   de los dos, fallará silenciosamente.
            g.group("factor", "PAREN_IZQ (expresion | condicion) PAREN_DER");
            g.group("factor", "IDENTIFICADOR | valor");
            g.group("termino", "factor (MULTIPLICACION | DIVISION | MODULO) factor");
            g.group("termino", "factor");
            g.group("expresion", "termino (SUMA | RESTA | CONCAT) termino");
            g.group("expresion", "termino");

            g.group("condicion_rel", "expresion (OP_MAYOR | OP_MENOR | OP_MAYOR_IGUAL | OP_MENOR_IGUAL | OP_IGUAL_IGUAL | OP_DIFERENTE) expresion");
            g.group("condicion", "condicion_rel | BOOLEAN_TRUE | BOOLEAN_FALSE | IDENTIFICADOR | factor");
            g.group("condicion", "PAREN_IZQ (condicion | expresion) PAREN_DER");
            g.group("condicion", "LOGICO_NOT (condicion | expresion)");
            g.group("condicion", "(condicion | expresion) (LOGICO_AND | LOGICO_OR) (condicion | expresion)");

            // --- 2. SENTENCIAS ATÓMICAS (Sin control de flujo aún) ---
            // Declaración:
            //   var entero x;                        → declaración simple
            //   var entero a := 5;                   → declaración + asignación inicial
            //   var entero a, b, c;                  → declaración múltiple
            //   var entero a := 1, b, c := 3;        → mezcla con/sin valor
            //
            // El `(ASIGNACION (expresion | condicion))?` opcional permite
            // que cada variable pueda traer su valor inicial. Sin esto, la
            // gramática reducía `a := 5;` como `asig_st` independiente y el
            // `decl_st` se quedaba sin `;`, dejando VAR / tipo_dato sueltos.
            g.group("decl_st",
                    "VAR tipo_dato"
                    + " (IDENTIFICADOR | expresion) (ASIGNACION (expresion | condicion))?"
                    + " (COMA (IDENTIFICADOR | expresion) (ASIGNACION (expresion | condicion))?)*"
                    + " PUNTO_COMA");
            // const_st: declaración de constante. A diferencia de decl_st, la
            // asignación es OBLIGATORIA — una constante debe nacer con valor.
            //   const entero PI := 3;                      → simple
            //   const decimal X := 1.5, Y := 2.5;          → múltiple
            g.group("const_st",
                    "CONST tipo_dato"
                    + " (IDENTIFICADOR | expresion) ASIGNACION (expresion | condicion)"
                    + " (COMA (IDENTIFICADOR | expresion) ASIGNACION (expresion | condicion))*"
                    + " PUNTO_COMA");
            g.group("asig_st", "(IDENTIFICADOR | expresion | condicion) ASIGNACION (expresion | condicion) PUNTO_COMA");
            g.group("io_st", "(MOSTRAR | LEER) (PAREN_IZQ (expresion | condicion | IDENTIFICADOR) PAREN_DER | expresion | condicion | IDENTIFICADOR) PUNTO_COMA");
            g.group("inc_st", "(OP_INCREMENTO | OP_DECREMENTO) (expresion | condicion | IDENTIFICADOR)");
            g.group("brk_st", "BREAK PUNTO_COMA");

            // --- 3. EMPAQUETADO TEMPORAL PARA BLOQUES ---
            // Solo incluimos lo que NO sea 'crecimiento' (if, for, etc.)
            g.group("sent_atomic", "decl_st | const_st | asig_st | io_st | inc_st | brk_st");
            g.group("list_atomic", "sent_atomic+");
            g.group("list_atomic", "list_atomic sent_atomic");
            g.group("list_atomic", "sent_atomic list_atomic");
            g.group("list_atomic", "list_atomic list_atomic");

            // --- 4. PROMOCIÓN: list_atomic se considera list_complex ---
            // Esto permite que cuerpos que mezclan sentencias atómicas y
            // estructuras de control (caso 1: if-else + rompe;) se fusionen
            // en un único list_complex antes de evaluarse case_st / bloque.
            g.group("list_complex", "list_atomic");
            g.group("list_complex", "list_atomic list_complex");
            g.group("list_complex", "list_complex list_atomic");

            // --- 5. ZONA DE CRECIMIENTO PROTEGIDA ---
            g.group("bloque", "LLAVE_IZQ (list_atomic | list_complex)? LLAVE_DER");

            // IF: cadena por if_base (no por tokens crudos), para permitir
            //   si {...} sino si {...} sino si {...} sino {...}
            g.group("if_base", "SI (condicion | expresion) bloque");
            g.group("if_st", "if_base (SINO if_base)* (SINO bloque)?");

            // Bucles
            g.group("while_st", "WHILE (condicion | expresion) bloque");
            g.group("for_st", "FOR PAREN_IZQ (list_atomic | list_complex)? (condicion | expresion) PUNTO_COMA (inc_st | list_atomic | list_complex)? PAREN_DER bloque");

            // REPITE { ... } HASTA (cond);   (do-while, continuación)
            // El cuerpo se ejecuta al menos una vez; mientras la condición
            // sea cierta se vuelve a repetir. La condición se evalúa después
            // del bloque y, gracias a `factor`/`condicion` que aceptan
            // paréntesis envolventes, `(condicion | expresion)` matchea el
            // `(cond)` envuelto.
            g.group("do_while_st", "REPITE bloque HASTA (condicion | expresion) PUNTO_COMA");

            // Switch: cuerpo OBLIGATORIO. Si dejamos un fallback "CASE expr :"
            // sin cuerpo dentro del loop, matchea en la iter 1 (cuando list_complex
            // del if/for/while interno aún no se ha formado) y deja el cuerpo
            // huérfano. brk_st ya queda dentro de list_atomic/list_complex.
            g.group("case_st", "CASE (expresion | condicion) DOS_PUNTOS (list_atomic | list_complex)");
            g.group("def_st", "DEFAULT DOS_PUNTOS (list_atomic | list_complex)");
            // Extender un case_st/def_st ya formado cuando en iteraciones
            // posteriores aparezca otro list_atomic / list_complex contiguo
            // (p. ej. el for_st o while_st del cuerpo recién promovido).
            g.group("case_st", "case_st (list_atomic | list_complex)");
            g.group("def_st", "def_st (list_atomic | list_complex)");
            g.group("list_casos", "case_st+");
            g.group("list_casos", "list_casos case_st");
            g.group("list_casos", "case_st list_casos");
            g.group("list_casos", "list_casos list_casos");
            // Si list_casos se cerró antes de que el for/while/if interno
            // se promoviera a list_complex, permitir que crezca después.
            g.group("list_casos", "list_casos (list_atomic | list_complex)");
            g.group("switch_st", "SWITCH (expresion | condicion) LLAVE_IZQ list_casos (def_st)? LLAVE_DER");
            g.group("switch_st", "SWITCH (expresion | condicion) LLAVE_IZQ def_st LLAVE_DER");

            // --- 6. FUSIÓN DE ESTRUCTURAS ---
            g.group("sent_complex", "if_st | for_st | while_st | do_while_st | switch_st");
            g.group("sentencia", "sent_atomic | sent_complex");
            g.group("list_complex", "sentencia+");
            g.group("list_complex", "list_complex sentencia");
            g.group("list_complex", "sentencia list_complex");
            g.group("list_complex", "list_complex list_complex");

            // --- 6. PROGRAMA ---
            g.group("programa", "INICIO bloque FIN");
        });

        // Errores finales (solo se muestran si 'programa' no se formó)
        g.group("programa", "INICIO bloque", true, 3, "Error sintáctico: Falta 'fin'");
        g.group("programa", "bloque FIN", true, 1, "Error sintáctico: Falta 'inicio'");

        g.show();
    }

    /** Secuencias SGR de ANSI ([...m) emitidas por compilerTools.Grammar. */
    private static final Pattern ANSI = Pattern.compile("\033\\[[;\\d]*m");

    /**
     * Reemplaza {@link Grammar#loopForFunExecUntilChangeNotDetected} con un
     * loop que cuenta iteraciones y aborta si se alcanza {@code maxIter},
     * protegiendo ante gramáticas que no converjan. Se considera "convergido"
     * cuando el conteo de producciones internas no cambia entre dos llamadas
     * sucesivas al runnable (mismo criterio que la implementación original).
     *
     * Si la reflexión sobre el campo privado {@code producciones} falla
     * (incompatibilidad de versión de compilerTools.jar), se hace fallback
     * al método original para no romper la compilación.
     */
    private static void loopConTope(Grammar g, int maxIter, Runnable r) {
        Field campoProd;
        try {
            campoProd = Grammar.class.getDeclaredField("producciones");
            campoProd.setAccessible(true);
        } catch (NoSuchFieldException e) {
            // Versión incompatible de compilerTools — fallback seguro.
            g.loopForFunExecUntilChangeNotDetected(r);
            return;
        }
        int prevSize = -1;
        for (int i = 0; i < maxIter; i++) {
            r.run();
            int currSize;
            try {
                currSize = ((java.util.ArrayList<?>) campoProd.get(g)).size();
            } catch (IllegalAccessException e) {
                g.loopForFunExecUntilChangeNotDetected(r);
                return;
            }
            if (currSize == prevSize) return;  // convergió
            prevSize = currSize;
        }
        System.err.println("[!] Análisis sintáctico detenido por seguridad: "
                + maxIter + " iteraciones alcanzadas sin converger.");
    }
}
