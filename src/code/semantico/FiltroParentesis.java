package code.semantico;

import compilerTools.ErrorLSSL;
import compilerTools.Token;
import java.util.ArrayList;

/**
 * Cuenta paréntesis dentro de cada asignación y reporta desequilibrios
 * (faltan ')' / sobra ')'). Los errores se publican en
 * {@link Repositorio#listaErrores} y, además, en {@link #erroresEncontrados}
 * para que la GUI pueda mostrarlos en una sección dedicada.
 */
public class FiltroParentesis {

    public static final ArrayList<ErrorLSSL> erroresEncontrados = new ArrayList<>();

    public static ArrayList<Token> filtrarYValidar(ArrayList<Token> tokens) {
        erroresEncontrados.clear();

        ArrayList<Token> resultado = new ArrayList<>();
        boolean dentroDeAsignacion = false;
        int contadorAsignacion = 0;
        Token ultimoParenAbierto = null;

        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            String comp = t.getLexicalComp();

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
                    registrarError(ErrorSemantico.PAREN_DESEQUILIBRADO_ABIERTOS.id,
                        "Error sintáctico: Faltan " + contadorAsignacion
                        + " paréntesis de cierre ')' en la asignación.",
                        ultimoParenAbierto != null ? ultimoParenAbierto : t);
                }
                dentroDeAsignacion = false;
                contadorAsignacion = 0;
                resultado.add(t);
                continue;
            }

            if (dentroDeAsignacion) {
                if (esPalabraReservadaEstructura(comp)) {
                    dentroDeAsignacion = false;
                    contadorAsignacion = 0;
                    resultado.add(t);
                    continue;
                }

                if (TokenTipo.PAREN_IZQ.equals(comp)) {
                    contadorAsignacion++;
                    ultimoParenAbierto = t;
                    resultado.add(t);
                    continue;
                }

                if (TokenTipo.PAREN_DER.equals(comp)) {
                    contadorAsignacion--;
                    if (contadorAsignacion < 0) {
                        registrarError(ErrorSemantico.PAREN_DESEQUILIBRADO_CERRADOS.id,
                            "Error sintáctico: Sobra un paréntesis de cierre ')' en la asignación.",
                            t);
                        contadorAsignacion = 0;
                    }
                    resultado.add(t);
                    continue;
                }
            }

            resultado.add(t);
        }
        return resultado;
    }

    private static boolean esPalabraReservadaEstructura(String comp) {
        return TokenTipo.MOSTRAR.equals(comp) || TokenTipo.LEER.equals(comp)
            || TokenTipo.SI.equals(comp)      || TokenTipo.SINO.equals(comp)
            || TokenTipo.WHILE.equals(comp)   || TokenTipo.FOR.equals(comp)
            || TokenTipo.SWITCH.equals(comp)  || TokenTipo.CASE.equals(comp)
            || TokenTipo.DEFAULT.equals(comp) || TokenTipo.BREAK.equals(comp)
            || TokenTipo.REPITE.equals(comp)  || TokenTipo.HASTA.equals(comp);
    }

    /** Registra el mismo objeto en ambas listas para poder filtrarlo por identidad. */
    private static void registrarError(int id, String mensaje, Token tok) {
        ErrorLSSL err = new ErrorLSSL(id, mensaje, tok);
        Repositorio.listaErrores.add(err);
        erroresEncontrados.add(err);
    }
}
