package code.semantico;

import compilerTools.ErrorLSSL;
import compilerTools.Token;
import java.util.ArrayList;
import java.util.List;

public class ProcesadorAsignaciones {

    public static void procesar() {
        for (int i = 0; i < Repositorio.listaTokens.size(); i++) {
            Token token = Repositorio.listaTokens.get(i);

            if (!TokenTipo.IDENTIFICADOR.equals(token.getLexicalComp())) continue;
            if (!TokenUtils.tokenEn(i + 1, TokenTipo.ASIGNACION)) continue;

            // No re-procesar las inicializaciones que están dentro de una
            // declaración `var/const T id := expr [, id := expr]* ;`.
            // TablaSimbolos.construir() ya las evaluó con corte por coma
            // (soloHastaCom=true). Si pasamos por aquí, evaluarExpresion
            // extraería hasta el ';' final, contaminándose con los
            // inicializadores hermanos. P.ej. en
            //     var entero n1 := 2, n2 := 3;
            // sin este filtro, n1 termina recibiendo el valor "3" porque
            // su evaluación absorbe los tokens "2 , n2 := 3".
            if (esIdentDeDeclaracion(i)) continue;

            String nombreVar = token.getLexeme();

            if (!tieneErrorDeTipo(nombreVar, i)) {
                String valorEvaluado = evaluarExpresion(i + 2);
                if (Repositorio.tablaSimbolos.containsKey(nombreVar)) {
                    Repositorio.tablaSimbolos.get(nombreVar).setValor(valorEvaluado);
                }
            }

            int siguientePuntoComa = TokenUtils.indiceSiguiente(i + 2, TokenTipo.PUNTO_COMA);
            if (siguientePuntoComa < 0) break;
            i = siguientePuntoComa;
        }
    }

    /**
     * True si el identificador en {@code pos} es parte de una declaración:
     *   - precedido directamente por VAR/CONST + tipo (forma simple
     *     `var T id`), o
     *   - precedido por COMA dentro de una declaración (forma múltiple
     *     `var T a, b, c`).
     */
    private static boolean esIdentDeDeclaracion(int pos) {
        if (TokenUtils.tokenEn(pos - 2, TokenTipo.VAR)
                || TokenUtils.tokenEn(pos - 2, TokenTipo.CONST)) {
            return true;
        }
        if (!TokenUtils.tokenEn(pos - 1, TokenTipo.COMA)) return false;
        // ¿Hay un VAR/CONST en la misma sentencia (antes del último ';')?
        for (int j = pos - 1; j >= 0; j--) {
            String comp = Repositorio.listaTokens.get(j).getLexicalComp();
            if (TokenTipo.VAR.equals(comp) || TokenTipo.CONST.equals(comp)) return true;
            if (TokenTipo.PUNTO_COMA.equals(comp)) return false;
        }
        return false;
    }

    public static void validarSentenciasLeer() {
        for (int i = 0; i < Repositorio.listaTokens.size(); i++) {
            Token token = Repositorio.listaTokens.get(i);
            if (!TokenTipo.LEER.equals(token.getLexicalComp())) continue;
            if (!TokenUtils.tokenEn(i + 2, TokenTipo.IDENTIFICADOR)) continue;

            Token tokenInterno = Repositorio.listaTokens.get(i + 2);
            if (!Repositorio.tablaSimbolos.containsKey(tokenInterno.getLexeme())) {
                Repositorio.listaErrores.add(new ErrorLSSL(
                    ErrorSemantico.VAR_NO_INICIALIZADA.id,
                    "Error semántico: La variable '" + tokenInterno.getLexeme() + "' no ha sido declarada.",
                    tokenInterno));
            }
        }
    }

    public static String evaluarExpresion(int inicio) {
        return evaluarExpresion(inicio, false);
    }

    /**
     * Evalúa la expresión que comienza en {@code inicio}.
     * Si {@code soloHastaCom} es true, corta también en COMA (útil para
     * inicializadores en declaraciones múltiples).
     */
    public static String evaluarExpresion(int inicio, boolean soloHastaCom) {
        List<Token> tokensExpr = soloHastaCom
            ? extraerHastaComaOPuntoComa(inicio)
            : TokenUtils.extraerHastaDelimitador(inicio);

        List<String> expresion = new ArrayList<>();
        for (Token t : tokensExpr) {
            if (TokenTipo.IDENTIFICADOR.equals(t.getLexicalComp())) {
                if (Repositorio.tablaSimbolos.containsKey(t.getLexeme())) {
                    expresion.add(Repositorio.tablaSimbolos.get(t.getLexeme()).getValor());
                } else {
                    Repositorio.listaErrores.add(new ErrorLSSL(
                        ErrorSemantico.OPERACION_INVALIDA.id,
                        "Error semántico: La variable '" + t.getLexeme() + "' no ha sido declarada.",
                        t));
                    expresion.add("0");
                }
            } else {
                String op = tokenAOperando(t);
                if (op != null) expresion.add(op);
            }
        }

        if (expresion.isEmpty())     return "0";
        if (expresion.size() == 1)   return expresion.get(0);
        return EvaluadorExpresiones.evaluarPostfija(
                EvaluadorExpresiones.infijaAPostfija(new ArrayList<>(expresion)));
    }

    private static List<Token> extraerHastaComaOPuntoComa(int inicio) {
        List<Token> seg = new ArrayList<>();
        for (int i = inicio; i < Repositorio.listaTokens.size(); i++) {
            Token t = Repositorio.listaTokens.get(i);
            String comp = t.getLexicalComp();
            if (TokenTipo.COMA.equals(comp) || TokenTipo.PUNTO_COMA.equals(comp)) break;
            seg.add(t);
        }
        return seg;
    }

    private static boolean tieneErrorDeTipo(String nombreVar, int posAsignacion) {
        if (!Repositorio.tablaSimbolos.containsKey(nombreVar)) return false;

        String tipoVar = Repositorio.tablaSimbolos.get(nombreVar).getTipoDato();
        if (!TokenTipo.TIPO_ENTERO.equals(tipoVar) && !TokenTipo.TIPO_CORTO.equals(tipoVar)) return false;

        for (Token tk : TokenUtils.extraerHastaDelimitador(posAsignacion + 2)) {
            if (TokenTipo.FLOTANTE.equals(tk.getLexicalComp())) return true;

            if (TokenTipo.IDENTIFICADOR.equals(tk.getLexicalComp())
                    && Repositorio.tablaSimbolos.containsKey(tk.getLexeme())) {
                if (TokenTipo.TIPO_DECIMAL.equals(
                        Repositorio.tablaSimbolos.get(tk.getLexeme()).getTipoDato())) return true;
            }
        }
        return false;
    }

    private static String tokenAOperando(Token t) {
        switch (t.getLexicalComp()) {
            case TokenTipo.IDENTIFICADOR:
            case TokenTipo.ENTERO:
            case TokenTipo.FLOTANTE:
            case TokenTipo.CADENA:         return t.getLexeme();
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
            case TokenTipo.LOGICO_AND:     return "-y-";
            case TokenTipo.LOGICO_OR:      return "-o-";
            case TokenTipo.LOGICO_NOT:     return "~";
            default:                       return null;
        }
    }
}
