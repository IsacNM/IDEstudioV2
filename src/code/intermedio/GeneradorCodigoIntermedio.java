package code.intermedio;

import code.semantico.EvaluadorExpresiones;
import code.semantico.Repositorio;
import code.semantico.TokenTipo;
import compilerTools.Token;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Genera código intermedio de tres direcciones (.3d) a partir de la lista
 * de tokens semántica. Recorre tokens con un cursor {@code pos} y por cada
 * sentencia emite las instrucciones equivalentes vía {@link Generador3D}.
 */
public class GeneradorCodigoIntermedio {

    private final ArrayList<Token> tokens;
    private final Generador3D gen;
    private int pos;

    public GeneradorCodigoIntermedio(Generador3D generador) {
        ArrayList<Token> base = Repositorio.listaTokensFiltrados.isEmpty()
                ? Repositorio.listaTokens
                : Repositorio.listaTokensFiltrados;

        ArrayList<Token> limpios = new ArrayList<>();
        for (Token t : base) {
            if (!TokenTipo.ERROR.equals(t.getLexicalComp())) limpios.add(t);
        }
        this.tokens = limpios;
        this.gen = generador;
        this.pos = 0;
    }

    // ── Helpers de cursor ────────────────────────────────────────────────

    private String comp(int i) {
        return (i < 0 || i >= tokens.size()) ? "" : tokens.get(i).getLexicalComp();
    }

    private String lex(int i) {
        return (i < 0 || i >= tokens.size()) ? "" : tokens.get(i).getLexeme();
    }

    /** Posición INMEDIATAMENTE DESPUÉS del primer token con tipo {@code tipo}. */
    private int posSigToken(int desde, String tipo) {
        int y = desde;
        while (y < tokens.size() && !comp(y).equals(tipo)) y++;
        return (y < tokens.size()) ? y + 1 : tokens.size();
    }

    private boolean esCierre(String c) { return TokenTipo.PAREN_DER.equals(c); }
    private boolean esApertura(String c) { return TokenTipo.PAREN_IZQ.equals(c); }

    /** Avanza pos hasta el ')' que cierra el actual nivel y devuelve true si lo consumió. */
    private boolean skipParenIzq() {
        if (esApertura(comp(pos))) { pos++; return true; }
        return false;
    }
    private boolean skipParenDer() {
        if (esCierre(comp(pos))) { pos++; return true; }
        return false;
    }
    private boolean skipLlaveIzq() {
        if (TokenTipo.LLAVE_IZQ.equals(comp(pos))) { pos++; return true; }
        return false;
    }
    private boolean skipLlaveDer() {
        if (TokenTipo.LLAVE_DER.equals(comp(pos))) { pos++; return true; }
        return false;
    }
    private boolean skipPuntoComa() {
        if (TokenTipo.PUNTO_COMA.equals(comp(pos))) { pos++; return true; }
        return false;
    }

    /**
     * Recolecta operandos (vía {@link #tokenAOperando(int)}) entre los
     * paréntesis del nivel actual (asume que pos ya está justo DESPUÉS del
     * '(' de apertura). Al terminar pos queda en el ')' de cierre, sin
     * consumirlo.
     */
    private ArrayList<String> recolectarHastaParenCierre() {
        ArrayList<String> exp = new ArrayList<>();
        int depth = 0;
        while (pos < tokens.size()) {
            String c = comp(pos);
            if (esCierre(c) && depth == 0) break;
            if (esApertura(c)) depth++;
            else if (esCierre(c)) depth--;
            String op = tokenAOperando(pos);
            if (op != null) exp.add(op);
            pos++;
        }
        return exp;
    }

    /**
     * Recolecta el lexeme bruto de cada token entre el nivel actual de
     * paréntesis y construye una cadena tipo "(a > b)". pos queda en el
     * ')' de cierre.
     */
    private String recolectarCondicion() {
        StringBuilder sb = new StringBuilder("(");
        int depth = 0;
        while (pos < tokens.size()) {
            String c = comp(pos);
            if (esCierre(c) && depth == 0) break;
            if (esApertura(c)) depth++;
            else if (esCierre(c)) depth--;
            sb.append(lex(pos));
            pos++;
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Convierte una lista de operandos en una expresión intermedia y
     * devuelve el nombre del temporal/var resultado (string vacío si la
     * lista venía vacía).
     */
    private String emitirExpresion(ArrayList<String> exp) {
        if (exp.isEmpty())   return "";
        if (exp.size() == 1) return exp.get(0);
        return generarPostfija3D(EvaluadorExpresiones.infijaAPostfija(exp));
    }

    /**
     * Recolecta una expresión arbitraria entre [start, endExcl) y emite el
     * código intermedio para ella. Devuelve el valor resultado o vacío.
     */
    private String emitirExpresionEnRango(int start, int endExcl) {
        ArrayList<String> exp = new ArrayList<>();
        for (int j = start; j < endExcl; j++) {
            String op = tokenAOperando(j);
            if (op != null) exp.add(op);
        }
        return emitirExpresion(exp);
    }

    // ── Punto de entrada ─────────────────────────────────────────────────

    /**
     * Procesa tokens hasta encontrar {@code tipoTokenFinal} (sin consumirlo).
     * Llamada raíz: {@code generar(TokenTipo.FIN)}.
     * Llamada de bloque: {@code generar(TokenTipo.LLAVE_DER)}.
     */
    public void generar(String tipoTokenFinal) {
        while (pos < tokens.size() && !comp(pos).equals(tipoTokenFinal)) {
            generarSentencia();
        }
    }

    /** Genera código para una sola sentencia o avanza si no la reconoce. */
    private void generarSentencia() {
        switch (comp(pos)) {
            case TokenTipo.VAR:           procesarDeclaracion(); break;
            // CONST tiene la misma forma léxica que VAR: KEYWORD tipo id := expr;
            // (con asignación obligatoria, validada por el semántico). El
            // emisor de 3 direcciones puede reusarse tal cual porque ya
            // tolera la presencia del inicializador.
            case TokenTipo.CONST:         procesarDeclaracion(); break;
            case TokenTipo.MOSTRAR:       procesarMostrar();     break;
            case TokenTipo.LEER:          procesarLeer();        break;
            case TokenTipo.SI:            procesarSi();          break;
            case TokenTipo.WHILE:         procesarWhile();       break;
            case TokenTipo.REPITE:        procesarRepite();      break;
            case TokenTipo.FOR:           procesarFor();         break;
            case TokenTipo.SWITCH:        procesarSwitch();      break;
            case TokenTipo.IDENTIFICADOR:
                if (TokenTipo.ASIGNACION.equals(comp(pos + 1))) procesarAsignacion();
                else pos++;
                break;
            case TokenTipo.BREAK:
                pos++;
                skipPuntoComa();
                break;
            case TokenTipo.INICIO:
            case TokenTipo.LLAVE_IZQ:
            case TokenTipo.LLAVE_DER:
            default:
                pos++;
                break;
        }
    }

    // ── Declaración: VAR tipo id [:= expr] (, id [:= expr])* ; ───────────

    private void procesarDeclaracion() {
        pos++; // VAR
        if (pos < tokens.size()) pos++; // tipo

        while (pos < tokens.size() && !TokenTipo.PUNTO_COMA.equals(comp(pos))) {
            if (TokenTipo.COMA.equals(comp(pos))) { pos++; continue; }

            if (TokenTipo.IDENTIFICADOR.equals(comp(pos))
                    && TokenTipo.ASIGNACION.equals(comp(pos + 1))) {
                String nombreVar = lex(pos);

                int j = pos + 2;
                while (j < tokens.size()
                        && !TokenTipo.PUNTO_COMA.equals(comp(j))
                        && !TokenTipo.COMA.equals(comp(j))) j++;

                String resultado = emitirExpresionEnRango(pos + 2, j);
                if (!resultado.isEmpty()) {
                    String op = (j - (pos + 2) == 1) ? "ASIGNACION" : ":=";
                    gen.gc(op, resultado, "", nombreVar);
                }
                pos = j;
                continue;
            }
            pos++;
        }
        skipPuntoComa();
    }

    // ── MOSTRAR (expr) ; ─────────────────────────────────────────────────

    private void procesarMostrar() {
        pos++; // MOSTRAR
        if (!skipParenIzq()) {
            pos = posSigToken(pos, TokenTipo.PUNTO_COMA);
            return;
        }
        ArrayList<String> exp = recolectarHastaParenCierre();
        skipParenDer();
        gen.gc("MOSTRAR", "", "", emitirExpresion(exp));
        skipPuntoComa();
    }

    // ── LEER (id) ; ──────────────────────────────────────────────────────

    private void procesarLeer() {
        pos++; // LEER
        skipParenIzq();
        String id = "";
        if (TokenTipo.IDENTIFICADOR.equals(comp(pos))) {
            id = lex(pos);
            pos++;
        }
        skipParenDer();
        skipPuntoComa();
        gen.gc("LEER", "", "", id);
    }

    // ── id := expr ; ─────────────────────────────────────────────────────

    private void procesarAsignacion() {
        String nombreVar = lex(pos);
        pos++; // id
        pos++; // :=

        int start = pos;
        while (pos < tokens.size() && !TokenTipo.PUNTO_COMA.equals(comp(pos))) pos++;
        int end = pos;
        skipPuntoComa();

        String resultado = emitirExpresionEnRango(start, end);
        if (!resultado.isEmpty()) gen.gc(":=", resultado, "", nombreVar);
    }

    // ── SI (cond) { } [SINO ...] ─────────────────────────────────────────

    private void procesarSi() {
        String etiqTrue  = gen.nuevaEtiq();
        String etiqFalso = gen.nuevaEtiq();

        pos++; // SI
        skipParenIzq();
        String condicion = recolectarCondicion();
        skipParenDer();

        gen.gc("IF",    condicion,    "", etiqTrue);
        gen.gc("GOTO",  "",           "", etiqFalso);
        gen.gc("LABEL", "",           "", etiqTrue);

        skipLlaveIzq();
        generar(TokenTipo.LLAVE_DER);
        skipLlaveDer();

        if (TokenTipo.SINO.equals(comp(pos))) {
            pos++;
            String etiqFin = gen.nuevaEtiq();
            gen.gc("GOTO",  "", "", etiqFin);
            gen.gc("LABEL", "", "", etiqFalso);

            if (TokenTipo.SI.equals(comp(pos))) {
                procesarSi();
            } else {
                skipLlaveIzq();
                generar(TokenTipo.LLAVE_DER);
                skipLlaveDer();
            }
            gen.gc("LABEL", "", "", etiqFin);
        } else {
            gen.gc("LABEL", "", "", etiqFalso);
        }
    }

    // ── REPITE { } HASTA (cond);  (do-while, continuación) ───────────────
    //
    // Semántica:  ejecuta el cuerpo al menos una vez; mientras la condición
    // de HASTA siga siendo cierta, se vuelve a ejecutar. Cuando deja de
    // cumplirse, se sale.
    //
    // 3 direcciones (mismo estilo que procesarSi / procesarWhile):
    //     LABEL Linicio:
    //         <cuerpo>
    //         IF cond GOTO Linicio
    //         GOTO Lfin
    //     LABEL Lfin:
    private void procesarRepite() {
        String etiqInicio = gen.nuevaEtiq();
        String etiqFin    = gen.nuevaEtiq();

        gen.gc("LABEL", "", "", etiqInicio);

        pos++; // REPITE
        skipLlaveIzq();
        generar(TokenTipo.LLAVE_DER);
        skipLlaveDer();

        // HASTA (cond) ;
        if (TokenTipo.HASTA.equals(comp(pos))) pos++; // HASTA
        skipParenIzq();
        String condicion = recolectarCondicion();
        skipParenDer();
        skipPuntoComa();

        gen.gc("IF",    condicion, "", etiqInicio);
        gen.gc("GOTO",  "",        "", etiqFin);
        gen.gc("LABEL", "",        "", etiqFin);
    }

    // ── WHILE (cond) { } ─────────────────────────────────────────────────

    private void procesarWhile() {
        String etiqRegreso = gen.nuevaEtiq();
        String etiqTrue    = gen.nuevaEtiq();
        String etiqFalso   = gen.nuevaEtiq();

        gen.gc("LABEL", "", "", etiqRegreso);

        pos++; // WHILE
        skipParenIzq();
        String condicion = recolectarCondicion();
        skipParenDer();

        gen.gc("IF",    condicion, "", etiqTrue);
        gen.gc("GOTO",  "",        "", etiqFalso);
        gen.gc("LABEL", "",        "", etiqTrue);

        skipLlaveIzq();
        generar(TokenTipo.LLAVE_DER);
        skipLlaveDer();

        gen.gc("GOTO",  "", "", etiqRegreso);
        gen.gc("LABEL", "", "", etiqFalso);
    }

    // ── FOR (init ; cond ; incr) { } ─────────────────────────────────────

    private void procesarFor() {
        String etiqRegreso = gen.nuevaEtiq();
        String etiqTrue    = gen.nuevaEtiq();
        String etiqFalso   = gen.nuevaEtiq();

        pos++; // FOR
        skipParenIzq();

        int firstSemi = -1, secondSemi = -1, parenClose = -1, depth = 0;
        for (int j = pos; j < tokens.size(); j++) {
            String c = comp(j);
            if (esApertura(c))           depth++;
            else if (esCierre(c)) {
                if (depth == 0) { parenClose = j; break; }
                depth--;
            } else if (TokenTipo.PUNTO_COMA.equals(c)) {
                if (firstSemi == -1)       firstSemi  = j;
                else if (secondSemi == -1) secondSemi = j;
            }
        }
        if (firstSemi < 0 || secondSemi < 0 || parenClose < 0) {
            pos = posSigToken(pos, TokenTipo.LLAVE_DER);
            return;
        }

        // Inicialización
        generarInitFor(firstSemi);
        pos = firstSemi + 1;

        gen.gc("LABEL", "", "", etiqRegreso);

        // Condición
        StringBuilder cond = new StringBuilder("(");
        while (pos < secondSemi) { cond.append(lex(pos)); pos++; }
        cond.append(")");
        pos = secondSemi + 1;

        gen.gc("IF",    cond.toString(), "", etiqTrue);
        gen.gc("GOTO",  "",              "", etiqFalso);
        gen.gc("LABEL", "",              "", etiqTrue);

        // Incrementos diferidos
        ArrayList<String[]> incrementos = new ArrayList<>();
        while (pos < parenClose) {
            String c = comp(pos);
            if (TokenTipo.OP_INCREMENTO.equals(c) && (pos + 1) < parenClose) {
                incrementos.add(new String[]{"++", lex(pos + 1)});
                pos += 2;
            } else if (TokenTipo.OP_DECREMENTO.equals(c) && (pos + 1) < parenClose) {
                incrementos.add(new String[]{"--", lex(pos + 1)});
                pos += 2;
            } else {
                pos++;
            }
        }
        skipParenDer();

        skipLlaveIzq();
        generar(TokenTipo.LLAVE_DER);
        skipLlaveDer();

        for (String[] incr : incrementos) {
            String op = "++".equals(incr[0]) ? "+" : "-";
            gen.gc(op, incr[1], "1", incr[1]);
        }

        gen.gc("GOTO",  "", "", etiqRegreso);
        gen.gc("LABEL", "", "", etiqFalso);
    }

    /** Inicialización del for: VAR tipo id := expr  |  id := expr. */
    private void generarInitFor(int endPos) {
        if (TokenTipo.VAR.equals(comp(pos))) {
            pos++; // VAR
            if (pos < tokens.size()) pos++; // tipo
        }
        if (TokenTipo.IDENTIFICADOR.equals(comp(pos))
                && TokenTipo.ASIGNACION.equals(comp(pos + 1))) {
            String nombreVar = lex(pos);
            String resultado = emitirExpresionEnRango(pos + 2, endPos);
            if (!resultado.isEmpty()) {
                String op = (endPos - (pos + 2) == 1) ? "ASIGNACION" : ":=";
                gen.gc(op, resultado, "", nombreVar);
            }
        }
        pos = endPos;
    }

    // ── SWITCH ───────────────────────────────────────────────────────────

    private void procesarSwitch() {
        pos++; // SWITCH

        skipParenIzq();
        ArrayList<String> expSwitch = recolectarHastaParenCierre();
        skipParenDer();
        skipLlaveIzq();

        String varSwitch = emitirExpresion(expSwitch);

        // Pasada 1: localizar casos y la posición de cada cuerpo
        List<String>  valoresCaso = new ArrayList<>();
        List<String>  etiqsCaso   = new ArrayList<>();
        List<Integer> iniciosCaso = new ArrayList<>();
        String etiqDefecto = null;
        int    inicioDefecto = -1;
        String etiqFin = gen.nuevaEtiq();

        int depthL = 0;
        while (pos < tokens.size()) {
            String c = comp(pos);
            if (TokenTipo.LLAVE_DER.equals(c) && depthL == 0) break;

            if (TokenTipo.LLAVE_IZQ.equals(c)) { depthL++; pos++; continue; }
            if (TokenTipo.LLAVE_DER.equals(c)) { depthL--; pos++; continue; }

            if (depthL == 0) {
                if (TokenTipo.CASE.equals(c)) {
                    pos++;
                    String valor = lex(pos);
                    pos++;
                    if (TokenTipo.DOS_PUNTOS.equals(comp(pos))) pos++;
                    valoresCaso.add(valor);
                    etiqsCaso.add(gen.nuevaEtiq());
                    iniciosCaso.add(pos);
                } else if (TokenTipo.DEFAULT.equals(c)) {
                    pos++;
                    if (TokenTipo.DOS_PUNTOS.equals(comp(pos))) pos++;
                    etiqDefecto = gen.nuevaEtiq();
                    inicioDefecto = pos;
                } else {
                    pos++;
                }
            } else {
                pos++;
            }
        }
        int finSwitch = pos;

        // Pasada 2: saltos
        for (int i = 0; i < valoresCaso.size(); i++) {
            gen.gc("IF", "(" + varSwitch + "::" + valoresCaso.get(i) + ")",
                   "", etiqsCaso.get(i));
        }
        gen.gc("GOTO", "", "", etiqDefecto != null ? etiqDefecto : etiqFin);

        // Pasada 3: cuerpos
        for (int i = 0; i < valoresCaso.size(); i++) {
            gen.gc("LABEL", "", "", etiqsCaso.get(i));
            generarCuerpoCaso(iniciosCaso.get(i));
            gen.gc("GOTO", "", "", etiqFin);
        }

        if (etiqDefecto != null && inicioDefecto >= 0) {
            gen.gc("LABEL", "", "", etiqDefecto);
            generarCuerpoCaso(inicioDefecto);
        }

        gen.gc("LABEL", "", "", etiqFin);

        pos = finSwitch;
        skipLlaveDer();
    }

    /** Genera código desde {@code inicio} hasta el primer break/case/default/'}'. */
    private void generarCuerpoCaso(int inicio) {
        pos = inicio;
        int depth = 0;
        while (pos < tokens.size()) {
            String c = comp(pos);
            if (depth == 0 && (TokenTipo.BREAK.equals(c)   || TokenTipo.CASE.equals(c)
                            || TokenTipo.DEFAULT.equals(c) || TokenTipo.LLAVE_DER.equals(c))) {
                break;
            }
            if (TokenTipo.LLAVE_IZQ.equals(c)) depth++;
            if (TokenTipo.LLAVE_DER.equals(c)) depth--;
            generarSentencia();
        }
    }

    // ── Expresiones aritméticas ──────────────────────────────────────────

    /** Recibe postfija y emite instrucciones 3D. Devuelve el temporal final. */
    private String generarPostfija3D(ArrayList<String> postfija) {
        Stack<String> pila = new Stack<>();
        for (String tok : postfija) {
            if (esOperador(tok)) {
                String op2 = pila.isEmpty() ? "0" : pila.pop();
                String op1 = pila.isEmpty() ? "0" : pila.pop();
                String temp = gen.nuevaTemp();
                gen.gc(tok, op1, op2, temp);
                pila.push(temp);
            } else {
                pila.push(tok);
            }
        }
        return pila.isEmpty() ? "0" : pila.pop();
    }

    /** Convierte un token a operando string para infija→postfija. */
    private String tokenAOperando(int i) {
        switch (comp(i)) {
            case TokenTipo.IDENTIFICADOR:
            case TokenTipo.ENTERO:
            case TokenTipo.FLOTANTE:
            case TokenTipo.CADENA:
            case TokenTipo.CARACTER:       return lex(i);
            case TokenTipo.BOOLEAN_TRUE:   return "cierto";
            case TokenTipo.BOOLEAN_FALSE:  return "falso";
            case TokenTipo.SUMA:           return "+";
            case TokenTipo.RESTA:          return "-";
            case TokenTipo.MULTIPLICACION: return "*";
            case TokenTipo.DIVISION:       return "/";
            case TokenTipo.MODULO:         return "%";
            case TokenTipo.CONCAT:         return "&+";
            case TokenTipo.PAREN_IZQ:      return "(";
            case TokenTipo.PAREN_DER:      return ")";
            default:                       return null;
        }
    }

    private boolean esOperador(String tok) {
        return "+".equals(tok) || "-".equals(tok)
            || "*".equals(tok) || "/".equals(tok)
            || "%".equals(tok) || "&+".equals(tok);
    }
}
