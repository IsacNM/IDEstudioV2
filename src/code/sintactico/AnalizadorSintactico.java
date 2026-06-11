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
     * detiene el análisis y se reporta - protege ante entradas patológicas
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

            // Expresiones y condiciones
            g.group("factor", "PAREN_IZQ (expresion | condicion) PAREN_DER");
            g.group("factor", "IDENTIFICADOR | valor");
            g.group("termino", "factor (MULTIPLICACION | DIVISION | MODULO) factor");
            g.group("termino", "factor");
            g.group("expresion", "termino (SUMA | RESTA | CONCAT) termino");
            g.group("expresion", "termino");
            g.group("expresion", "expresion (SUMA | RESTA | CONCAT | MULTIPLICACION | DIVISION | MODULO) expresion");

            g.group("condicion_rel", "expresion (OP_MAYOR | OP_MENOR | OP_MAYOR_IGUAL | OP_MENOR_IGUAL | OP_IGUAL_IGUAL | OP_DIFERENTE) expresion");
            g.group("condicion", "condicion_rel | BOOLEAN_TRUE | BOOLEAN_FALSE | IDENTIFICADOR | factor");
            g.group("condicion", "LOGICO_NOT (condicion | expresion | factor)");
            g.group("condicion", "(condicion | expresion | factor) (LOGICO_AND | LOGICO_OR) (condicion | expresion | factor)");

            // Sentencias atómicas
            g.group("decl_st",
                    "VAR tipo_dato"
                    + " (IDENTIFICADOR | expresion) (ASIGNACION (expresion | condicion))?"
                    + " (COMA (IDENTIFICADOR | expresion) (ASIGNACION (expresion | condicion))?)*"
                    + " PUNTO_COMA");
            g.group("const_st",
                    "CONST tipo_dato"
                    + " (IDENTIFICADOR | expresion) ASIGNACION (expresion | condicion)"
                    + " (COMA (IDENTIFICADOR | expresion) ASIGNACION (expresion | condicion))*"
                    + " PUNTO_COMA");
            g.group("asig_st", "(IDENTIFICADOR | expresion | condicion) ASIGNACION (expresion | condicion) PUNTO_COMA");
            g.group("io_st", "(MOSTRAR | LEER) (PAREN_IZQ (expresion | condicion | IDENTIFICADOR) PAREN_DER | expresion | condicion | IDENTIFICADOR) PUNTO_COMA");
            g.group("inc_st", "(OP_INCREMENTO | OP_DECREMENTO) (expresion | condicion | IDENTIFICADOR)");
            g.group("brk_st", "BREAK PUNTO_COMA");

            // Bloques y control de flujo
            g.group("sent_atomic", "decl_st | const_st | asig_st | io_st | inc_st | brk_st");
            g.group("list_atomic", "sent_atomic+");
            g.group("list_atomic", "list_atomic sent_atomic");
            g.group("list_atomic", "sent_atomic list_atomic");
            g.group("list_atomic", "list_atomic list_atomic");

            g.group("list_complex", "list_atomic");
            g.group("list_complex", "list_atomic list_complex");
            g.group("list_complex", "list_complex list_atomic");

            g.group("bloque", "LLAVE_IZQ (list_atomic | list_complex)? LLAVE_DER");

            g.group("if_base", "SI (condicion | expresion) bloque");
            g.group("if_st", "if_base (SINO if_base)* (SINO bloque)?");

            g.group("while_st", "WHILE (condicion | expresion) bloque");
            g.group("for_st", "FOR PAREN_IZQ (list_atomic | list_complex)? (condicion | expresion) PUNTO_COMA (inc_st | list_atomic | list_complex)? PAREN_DER bloque");
            g.group("do_while_st", "REPITE bloque HASTA (condicion | expresion) PUNTO_COMA");

            // Switch
            g.group("case_st", "CASE (expresion | condicion) DOS_PUNTOS (list_atomic | list_complex)");
            g.group("def_st", "DEFAULT DOS_PUNTOS (list_atomic | list_complex)");
            g.group("case_st", "case_st (list_atomic | list_complex)");
            g.group("def_st", "def_st (list_atomic | list_complex)");
            g.group("list_casos", "case_st+");
            g.group("list_casos", "list_casos case_st");
            g.group("list_casos", "case_st list_casos");
            g.group("list_casos", "list_casos list_casos");
            g.group("list_casos", "list_casos (list_atomic | list_complex)");
            g.group("switch_st", "SWITCH (expresion | condicion) LLAVE_IZQ list_casos (def_st)? LLAVE_DER");
            g.group("switch_st", "SWITCH (expresion | condicion) LLAVE_IZQ def_st LLAVE_DER");

            // Finalización
            g.group("sent_complex", "if_st | for_st | while_st | do_while_st | switch_st");
            g.group("sentencia", "sent_atomic | sent_complex");
            g.group("list_complex", "sentencia+");
            g.group("list_complex", "list_complex sentencia");
            g.group("list_complex", "sentencia list_complex");
            g.group("list_complex", "list_complex list_complex");

            g.group("programa", "INICIO bloque FIN");
        });

        // Errores finales
        g.group("programa", "INICIO bloque", true, 3, "Error sintáctico: Falta 'fin'");
        g.group("programa", "bloque FIN", true, 1, "Error sintáctico: Falta 'inicio'");

        g.show();

        // Validación post-loop
        if (!formoProduccionPrograma(g) && Repositorio.listaErrores.isEmpty()) {
            // Usamos el primer token disponible para la línea/columna del
            // reporte; si no hay tokens, ya se reportó otro error antes.
            Token ref = tokens.isEmpty() ? null : tokens.get(0);
            if (ref != null) {
                Repositorio.listaErrores.add(new compilerTools.ErrorLSSL(
                    203,
                    "Error sintáctico: la estructura del programa no pudo ser reconocida en su totalidad. "
                    + "Revise sentencias mal cerradas, condiciones malformadas, "
                    + "o estructuras `si/sino` con anidación ambigua.",
                    new Token(ref.getLexeme(), "ESTRUCTURA_INVALIDA",
                              ref.getLine(), ref.getColumn())));
            }
        }
    }

    /**
     * True si la gramática reduce con éxito al menos una producción
     * {@code programa}. Inspecciona por reflexión el campo
     * {@code producciones} de {@link Grammar} (lista de
     * {@code Produccion} con método {@code getName()}). Si la reflexión
     * falla (incompatibilidad del jar), asume éxito para no introducir
     * falsos positivos.
     */
    private static boolean formoProduccionPrograma(Grammar g) {
        try {
            Field campoProd = Grammar.class.getDeclaredField("producciones");
            campoProd.setAccessible(true);
            Object lista = campoProd.get(g);
            if (!(lista instanceof java.util.List)) return true;
            for (Object p : (java.util.List<?>) lista) {
                if (p == null) continue;
                try {
                    java.lang.reflect.Method m = p.getClass().getMethod("getName");
                    Object name = m.invoke(p);
                    if ("programa".equals(name)) return true;
                } catch (NoSuchMethodException ignore) {
                    // Versión incompatible: damos por bueno
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return true;
        }
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
            // Versión incompatible de compilerTools - fallback seguro.
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
