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

                    // En `var T a, b := 5, c;` saltar la expresión hasta la
                    // siguiente COMA o ';' para no consumir variables aún
                    // no registradas.
                    if (TokenUtils.tokenEn(i + 1, TokenTipo.ASIGNACION)) {
                        i = indiceFinExpresionDecl(i + 2) - 1;
                    }
                }
                i++;
            }
        }
    }

    /** Índice del siguiente COMA o PUNTO_COMA desde {@code desde}. */
    private static int indiceFinExpresionDecl(int desde) {
        for (int j = desde; j < Repositorio.listaTokens.size(); j++) {
            String comp = Repositorio.listaTokens.get(j).getLexicalComp();
            if (TokenTipo.COMA.equals(comp) || TokenTipo.PUNTO_COMA.equals(comp)) return j;
        }
        return Repositorio.listaTokens.size();
    }

    private static void registrarVariable(Token t, String tipo, String valorDefault, int pos) {
        String nombreVar = t.getLexeme();

        if (Repositorio.tablaSimbolos.containsKey(nombreVar)) {
            Repositorio.listaErrores.add(new ErrorLSSL(
                ErrorSemantico.VAR_DUPLICADA.id,
                "Error semántico: La variable '" + nombreVar + "' ya fue declarada anteriormente.",
                t));
            return;
        }

        String valorInicial = TokenUtils.tokenEn(pos + 1, TokenTipo.ASIGNACION)
            ? ProcesadorAsignaciones.evaluarExpresion(pos + 2, true)
            : valorDefault;

        Simbolo simbolo = new Simbolo();
        simbolo.setIdent(nombreVar);
        simbolo.setTipoDato(tipo);
        simbolo.setValor(valorInicial);
        simbolo.setVarConstParam(TokenTipo.VAR);
        Repositorio.tablaSimbolos.put(nombreVar, simbolo);
    }
}
