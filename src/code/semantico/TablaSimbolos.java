package code.semantico;

import compilerTools.ErrorLSSL;
import compilerTools.Token;
import java.util.Map;

public class TablaSimbolos {

    private static final Map<String, String> VALORES_DEFECTO = Map.of(
        TokenTipo.TIPO_ENTERO,  "0",
        TokenTipo.TIPO_DECIMAL, "0.0",
        TokenTipo.TIPO_TEXTO,   "\"\"",
        TokenTipo.TIPO_CAR,     "''",
        TokenTipo.TIPO_LOGICO,  "falso",
        TokenTipo.TIPO_CORTO,   "0"
    );

    public static void construir() {
        for (int i = 0; i < Repositorio.listaTokens.size(); i++) {
            Token token = Repositorio.listaTokens.get(i);

            if (!TokenTipo.VAR.equals(token.getLexicalComp())) continue;

            i++;
            if (i >= Repositorio.listaTokens.size()) break;

            String tipo = Repositorio.listaTokens.get(i).getLexicalComp();
            String valorDefault = VALORES_DEFECTO.getOrDefault(tipo, "null");
            i++;

            while (i < Repositorio.listaTokens.size()) {
                Token t = Repositorio.listaTokens.get(i);

                if (TokenTipo.PUNTO_COMA.equals(t.getLexicalComp())) break;

                if (TokenTipo.IDENTIFICADOR.equals(t.getLexicalComp())) {
                    registrarVariable(t, tipo, valorDefault, i);

                    if (TokenUtils.tokenEn(i + 1, TokenTipo.ASIGNACION)) {
                        i += 2;
                        i = TokenUtils.indiceSiguiente(i, TokenTipo.PUNTO_COMA) - 1;
                    }
                }
                i++;
            }
        }
    }

    private static void registrarVariable(Token t, String tipo, String valorDefault, int pos) {
        String nombreVar = t.getLexeme();

        if (Repositorio.tablaSimbolos.containsKey(nombreVar)) {
            Repositorio.listaErrores.add(new ErrorLSSL(
                ErrorSemantico.VAR_NO_DECLARADA.id,
                "Error semántico: La variable '" + nombreVar + "' ya fue declarada anteriormente.",
                t
            ));
            return;
        }

        String valorInicial = TokenUtils.tokenEn(pos + 1, TokenTipo.ASIGNACION)
            ? ProcesadorAsignaciones.evaluarExpresion(pos + 2)
            : valorDefault;

        Simbolo simbolo = new Simbolo();
        simbolo.setIdent(nombreVar);
        simbolo.setTipoDato(tipo);
        simbolo.setValor(valorInicial);
        simbolo.setVarConstParam(TokenTipo.VAR);
        Repositorio.tablaSimbolos.put(nombreVar, simbolo);
    }
}