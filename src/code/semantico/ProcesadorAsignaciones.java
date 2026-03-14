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

            String nombreVar = token.getLexeme();

            if (!tieneErrorDeTipo(nombreVar, i)) {
                String valorEvaluado = evaluarExpresion(i + 2);
                if (Repositorio.tablaSimbolos.containsKey(nombreVar)) {
                    Repositorio.tablaSimbolos.get(nombreVar).setValor(valorEvaluado);
                }
            }

            i = TokenUtils.indiceSiguiente(i + 2, TokenTipo.PUNTO_COMA);
        }
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
                    tokenInterno
                ));
            }
        }
    }

    public static String evaluarExpresion(int inicio) {
        List<String> expresion = new ArrayList<>();

        for (Token t : TokenUtils.extraerHastaDelimitador(inicio)) {
            if (TokenTipo.IDENTIFICADOR.equals(t.getLexicalComp())) {
                if (Repositorio.tablaSimbolos.containsKey(t.getLexeme())) {
                    expresion.add(Repositorio.tablaSimbolos.get(t.getLexeme()).getValor());
                } else {
                    Repositorio.listaErrores.add(new ErrorLSSL(
                        ErrorSemantico.OPERACION_INVALIDA.id,
                        "Error semántico: La variable '" + t.getLexeme() + "' no ha sido declarada.",
                        t
                    ));
                    expresion.add("0");
                }
            } else {
                String op = tokenAOperando(t);
                if (op != null) expresion.add(op);
            }
        }

        if (expresion.isEmpty()) return "0";
        if (expresion.size() == 1) return expresion.get(0);

        return EvaluadorExpresiones.evaluarPostfija(
            EvaluadorExpresiones.infijaAPostfija(new ArrayList<>(expresion))
        );
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
                        Repositorio.tablaSimbolos.get(tk.getLexeme()).getTipoDato())) {
                    return true;
                }
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