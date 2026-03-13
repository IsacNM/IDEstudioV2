package code;

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

            // ============================================================
            // DETECTAR si el paréntesis pertenece a una estructura de control
            // SI, WHILE, FOR, MOSTRAR, LEER → NO filtrar sus paréntesis
            // ============================================================
            if ("ASIGNACION".equals(comp)) {
                // Verificar que el token anterior NO sea parte de estructura de control
                // (es decir, que sea una asignación real: IDENTIFICADOR :=)
                if (i > 0 && "IDENTIFICADOR".equals(tokens.get(i - 1).getLexicalComp())) {
                    dentroDeAsignacion = true;
                    contadorAsignacion = 0;
                }
                resultado.add(t);
                continue;
            }

            // Al llegar al PUNTO_COMA cerramos la zona de asignación
            if ("PUNTO_COMA".equals(comp)) {
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

            // Solo filtramos paréntesis si estamos DENTRO de una asignación real
            if (dentroDeAsignacion) {
                if ("PAREN_IZQ".equals(comp)) {
                    contadorAsignacion++;
                    ultimoParenAbierto = t;
                    continue; // Quitar paréntesis de la expresión de asignación
                }
                if ("PAREN_DER".equals(comp)) {
                    contadorAsignacion--;
                    if (contadorAsignacion < 0) {
                        Repositorio.listaErrores.add(new compilerTools.ErrorLSSL(111,
                            "Error sintáctico: Sobra un paréntesis de cierre ')' en la asignación.", t));
                        contadorAsignacion = 0;
                    }
                    continue; // Quitar paréntesis de la expresión de asignación
                }
            }

            // Todos los demás tokens pasan tal cual
            // (incluyendo paréntesis de SI, WHILE, FOR, MOSTRAR, LEER)
            resultado.add(t);
        }

        return resultado;
    }
}