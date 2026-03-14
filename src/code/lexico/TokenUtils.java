package code.lexico;

import code.semantico.Repositorio;
import compilerTools.Token;

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

}
