package code.semantico;

import compilerTools.Token;
import java.util.ArrayList;

public class FiltroParentesis {

    public static ArrayList<String> erroresEncontrados = new ArrayList<>();

    public static ArrayList<Token> filtrarYValidar(ArrayList<Token> tokens) {
        ArrayList<Token> resultado = new ArrayList<>();
        boolean dentroDeAsignacion = false;
        int contadorAsignacion = 0;
        Token ultimoParenAbierto = null;

        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            String comp = t.getLexicalComp();

            //si el paréntesis pertenece a una estructura de control, nO se filtraran sus paréntesis
            if (TokenTipo.ASIGNACION.equals(comp)) {
                if (i > 0 && TokenTipo.IDENTIFICADOR.equals(tokens.get(i - 1).getLexicalComp())) {
                    dentroDeAsignacion = true;
                    contadorAsignacion = 0;
                }
                resultado.add(t);
                continue;
            }
            if (TokenTipo.PUNTO_COMA.equals(comp)) {
                if (dentroDeAsignacion && contadorAsignacion > 0) {
                    Repositorio.listaErrores.add(new compilerTools.ErrorLSSL(110,
                        "Error sintáctico: Faltan " + contadorAsignacion +
                        " paréntesis de cierre ')' en la asignación.",
                        ultimoParenAbierto != null ? ultimoParenAbierto : t));
                }
                dentroDeAsignacion = false;
                contadorAsignacion = 0;
                resultado.add(t);
                continue;
            }
            if (dentroDeAsignacion) {
                if (TokenTipo.PAREN_IZQ.equals(comp)) {
                    contadorAsignacion++;
                    ultimoParenAbierto = t;
                    continue;
                }
                if (TokenTipo.PAREN_DER.equals(comp)) {
                    contadorAsignacion--;
                    if (contadorAsignacion < 0) {
                        Repositorio.listaErrores.add(new compilerTools.ErrorLSSL(111,
                            "Error sintáctico: Sobra un paréntesis de cierre ')' en la asignación.", t));
                        contadorAsignacion = 0;
                    }
                    continue;
                }
            }
            resultado.add(t);
        }

        return resultado;
    }
}
