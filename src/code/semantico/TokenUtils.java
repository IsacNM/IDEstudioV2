package code.semantico;

import compilerTools.Token;
import java.util.ArrayList;
import java.util.List;

public class TokenUtils {

    public static String obtenerTipoDeToken(Token token) {
        String comp = token.getLexicalComp();
        switch (comp) {
            case "ENTERO":
                return "TIPO_ENTERO";
            case "FLOTANTE":
                return "TIPO_DECIMAL";
            case "CADENA":
                return "TIPO_TEXTO";
            case "CARACTER":
                return "TIPO_CAR";
            case "BOOLEAN_TRUE":
            case "BOOLEAN_FALSE":
                return "TIPO_LOGICO";
            case "IDENTIFICADOR":
                if (Repositorio.tablaSimbolos.containsKey(token.getLexeme())) {
                    return Repositorio.tablaSimbolos.get(token.getLexeme()).getTipoDato();
                }
                return null;
            default:
                return null;
        }
    }

    public static List<Token> extraerHastaDelimitador(int inicio) {
        return extraerHastaComponente(inicio, TokenTipo.PUNTO_COMA);
    }

    public static int indiceSiguiente(int desde, String componente) {
        for (int i = desde; i < Repositorio.listaTokens.size(); i++) {
            if (componente.equals(Repositorio.listaTokens.get(i).getLexicalComp())) {
                return i;
            }
        }
        return -1;
    }

// Verifica si el token en la posición dada existe y tiene ese componente
    public static boolean tokenEn(int pos, String componente) {
        if (pos < 0 || pos >= Repositorio.listaTokens.size()) {
            return false;
        }
        return componente.equals(Repositorio.listaTokens.get(pos).getLexicalComp());
    }

    public static List<Token> extraerHastaComponente(int inicio, String delimitador) {
        List<Token> segmento = new ArrayList<>();
        for (int i = inicio; i < Repositorio.listaTokens.size(); i++) {
            Token t = Repositorio.listaTokens.get(i);
            if (delimitador.equals(t.getLexicalComp())) {
                break;
            }
            segmento.add(t);
        }
        return segmento;
    }

    public static boolean esOperadorRelacional(String comp) {
        return TokenTipo.OP_MAYOR.equals(comp) || TokenTipo.OP_MENOR.equals(comp)
                || TokenTipo.OP_MAYOR_IGUAL.equals(comp) || TokenTipo.OP_MENOR_IGUAL.equals(comp)
                || TokenTipo.OP_IGUAL_IGUAL.equals(comp) || TokenTipo.OP_DIFERENTE.equals(comp);
    }

}
