package code.semantico;

import compilerTools.Token;
import java.util.ArrayList;
import java.util.List;

public class TokenUtils {

    /**
     * Devuelve el TIPO_* asociado a un token (mapea literales y resuelve
     * IDENTIFICADOR contra la tabla de símbolos). null si es desconocido.
     */
    public static String obtenerTipoDeToken(Token token) {
        switch (token.getLexicalComp()) {
            case TokenTipo.ENTERO:         return TokenTipo.TIPO_ENTERO;
            case TokenTipo.FLOTANTE:       return TokenTipo.TIPO_DECIMAL;
            case TokenTipo.CADENA:         return TokenTipo.TIPO_TEXTO;
            case TokenTipo.CARACTER:       return TokenTipo.TIPO_CAR;
            case TokenTipo.BOOLEAN_TRUE:
            case TokenTipo.BOOLEAN_FALSE:  return TokenTipo.TIPO_LOGICO;
            case TokenTipo.IDENTIFICADOR:
                Simbolo s = Repositorio.tablaSimbolos.get(token.getLexeme());
                return s == null ? null : s.getTipoDato();
            default: return null;
        }
    }

    public static List<Token> extraerHastaDelimitador(int inicio) {
        return extraerHastaComponente(inicio, TokenTipo.PUNTO_COMA);
    }

    public static List<Token> extraerHastaComponente(int inicio, String delimitador) {
        List<Token> segmento = new ArrayList<>();
        for (int i = inicio; i < Repositorio.listaTokens.size(); i++) {
            Token t = Repositorio.listaTokens.get(i);
            if (delimitador.equals(t.getLexicalComp())) break;
            segmento.add(t);
        }
        return segmento;
    }

    public static int indiceSiguiente(int desde, String componente) {
        for (int i = desde; i < Repositorio.listaTokens.size(); i++) {
            if (componente.equals(Repositorio.listaTokens.get(i).getLexicalComp())) {
                return i;
            }
        }
        return -1;
    }

    /** True si la posición existe y su lexicalComp coincide con {@code componente}. */
    public static boolean tokenEn(int pos, String componente) {
        if (pos < 0 || pos >= Repositorio.listaTokens.size()) return false;
        return componente.equals(Repositorio.listaTokens.get(pos).getLexicalComp());
    }

    public static boolean esOperadorRelacional(String comp) {
        return TokenTipo.OP_MAYOR.equals(comp)        || TokenTipo.OP_MENOR.equals(comp)
            || TokenTipo.OP_MAYOR_IGUAL.equals(comp)  || TokenTipo.OP_MENOR_IGUAL.equals(comp)
            || TokenTipo.OP_IGUAL_IGUAL.equals(comp)  || TokenTipo.OP_DIFERENTE.equals(comp);
    }

    /** True si {@code comp} es un operador lógico (-y-, -o-, -n-). */
    public static boolean esOperadorLogico(String comp) {
        return TokenTipo.LOGICO_AND.equals(comp)
            || TokenTipo.LOGICO_OR.equals(comp)
            || TokenTipo.LOGICO_NOT.equals(comp);
    }

    public static boolean esTipoNumerico(String tipo) {
        return TokenTipo.TIPO_ENTERO.equals(tipo)
            || TokenTipo.TIPO_DECIMAL.equals(tipo)
            || TokenTipo.TIPO_CORTO.equals(tipo);
    }

    public static boolean esTipoNoNumerico(String tipo) {
        return TokenTipo.TIPO_TEXTO.equals(tipo)
            || TokenTipo.TIPO_CAR.equals(tipo)
            || TokenTipo.TIPO_LOGICO.equals(tipo);
    }
}
