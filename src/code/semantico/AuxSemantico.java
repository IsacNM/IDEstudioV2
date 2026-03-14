package code.semantico;

import compilerTools.Token;
import compilerTools.ErrorLSSL;
import java.util.HashSet;
import java.util.List;

public class AuxSemantico {

    public static void validarVariablesNoDeclaradas(List<ErrorLSSL> errores) {
        for (int i = 0; i < Repositorio.listaTokens.size(); i++) {
            Token token = Repositorio.listaTokens.get(i);

            if (!TokenTipo.IDENTIFICADOR.equals(token.getLexicalComp())) {
                continue;
            }
            if (TokenUtils.tokenEn(i - 2, TokenTipo.VAR)) {
                continue;
            }

            if (!Repositorio.tablaSimbolos.containsKey(token.getLexeme())) {
                errores.add(new ErrorLSSL(
                        ErrorSemantico.VAR_NO_DECLARADA.id,
                        "Error semántico: La variable '" + token.getLexeme() + "' no ha sido declarada.",
                        token
                ));
            }
        }
    }

    public static void validarVariablesNoInicializadas(List<ErrorLSSL> errores) {
        HashSet<String> inicializadas = new HashSet<>();

        for (int i = 0; i < Repositorio.listaTokens.size(); i++) {
            Token token = Repositorio.listaTokens.get(i);
            if (!TokenTipo.IDENTIFICADOR.equals(token.getLexicalComp())) {
                continue;
            }
            String nombreVar = token.getLexeme();

            // Caso 1: var entero a  →  VAR está 2 posiciones atrás
            if (TokenUtils.tokenEn(i - 2, TokenTipo.VAR)) {
                inicializadas.add(nombreVar);
                continue;
            }

            // Caso 2: var entero a, b, c  →  viene después de una COMA
            // y hay un VAR en algún lugar antes dentro de la misma declaración
            if (TokenUtils.tokenEn(i - 1, TokenTipo.COMA) && hayVarAntes(i)) {
                inicializadas.add(nombreVar);
                continue;
            }

            // Caso 3: a := 3  →  tiene asignación después
            if (TokenUtils.tokenEn(i + 1, TokenTipo.ASIGNACION)) {
                inicializadas.add(nombreVar);
            } else if (Repositorio.tablaSimbolos.containsKey(nombreVar)
                    && !inicializadas.contains(nombreVar)) {
                errores.add(new ErrorLSSL(
                        ErrorSemantico.VAR_NO_INICIALIZADA.id,
                        "Error semántico: La variable '" + nombreVar + "' no tiene un valor asignado.",
                        token
                ));
            }
        }
    }

// Busca hacia atrás desde la posición dada hasta encontrar VAR o PUNTO_COMA
    private static boolean hayVarAntes(int desdeIndex) {
        for (int j = desdeIndex - 1; j >= 0; j--) {
            String comp = Repositorio.listaTokens.get(j).getLexicalComp();
            if (TokenTipo.VAR.equals(comp)) {
                return true;
            }
            if (TokenTipo.PUNTO_COMA.equals(comp)) {
                return false; // cruzó otra sentencia
            }
        }
        return false;
    }

    public static void validarTiposEnAsignaciones(List<ErrorLSSL> errores) {
        for (int i = 0; i < Repositorio.listaTokens.size(); i++) {
            Token token = Repositorio.listaTokens.get(i);

            if (!TokenTipo.IDENTIFICADOR.equals(token.getLexicalComp())) {
                continue;
            }
            if (!TokenUtils.tokenEn(i + 1, TokenTipo.ASIGNACION)) {
                continue;
            }

            String nombreVar = token.getLexeme();
            if (!Repositorio.tablaSimbolos.containsKey(nombreVar)) {
                continue;
            }

            Simbolo simbolo = Repositorio.tablaSimbolos.get(nombreVar);
            String tipoVar = simbolo.getTipoDato();
            Token valorToken = Repositorio.listaTokens.get(i + 2);
            String tipoValor = TokenUtils.obtenerTipoDeToken(valorToken);

            if (tipoValor != null && !sonTiposCompatibles(tipoVar, tipoValor)) {
                errores.add(new ErrorLSSL(
                        ErrorSemantico.TIPOS_INCOMPATIBLES.id,
                        "Error semántico: La variable '" + nombreVar + "' es de tipo '" + tipoVar
                        + "' pero se intentó asignar un valor de tipo '" + tipoValor + "'.",
                        token
                ));
            }

            if (TokenTipo.TIPO_ENTERO.equals(tipoVar) || TokenTipo.TIPO_CORTO.equals(tipoVar)) {
                for (Token tk : TokenUtils.extraerHastaDelimitador(i + 2)) {

                    if (TokenTipo.FLOTANTE.equals(tk.getLexicalComp())) {
                        errores.add(new ErrorLSSL(
                                ErrorSemantico.TIPOS_INCOMPATIBLES.id,
                                "Error semántico: No se puede usar el valor decimal '" + tk.getLexeme()
                                + "' en una expresión asignada a la variable entera '" + nombreVar + "'.",
                                tk
                        ));
                        break;
                    }

                    if (TokenTipo.IDENTIFICADOR.equals(tk.getLexicalComp())
                            && Repositorio.tablaSimbolos.containsKey(tk.getLexeme())) {
                        if (TokenTipo.TIPO_DECIMAL.equals(
                                Repositorio.tablaSimbolos.get(tk.getLexeme()).getTipoDato())) {
                            errores.add(new ErrorLSSL(
                                    ErrorSemantico.TIPOS_INCOMPATIBLES.id,
                                    "Error semántico: No se puede usar la variable decimal '" + tk.getLexeme()
                                    + "' en una expresión asignada a la variable entera '" + nombreVar + "'.",
                                    tk
                            ));
                            break;
                        }
                    }
                }
            }
        }
    }

    public static void validarTiposEnOperaciones(List<ErrorLSSL> errores) {
        for (int i = 1; i < Repositorio.listaTokens.size() - 1; i++) {
            Token token = Repositorio.listaTokens.get(i);
            String comp = token.getLexicalComp();

            if (!TokenTipo.SUMA.equals(comp) && !TokenTipo.RESTA.equals(comp)
                    && !TokenTipo.MULTIPLICACION.equals(comp) && !TokenTipo.DIVISION.equals(comp)) {
                continue;
            }

            String t1 = TokenUtils.obtenerTipoDeToken(Repositorio.listaTokens.get(i - 1));
            String t2 = TokenUtils.obtenerTipoDeToken(Repositorio.listaTokens.get(i + 1));

            if (t1 != null && t2 != null && (!esTipoNumerico(t1) || !esTipoNumerico(t2))) {
                errores.add(new ErrorLSSL(
                        ErrorSemantico.OPERACION_INVALIDA.id,
                        "Error semántico: Operación aritmética inválida entre '" + t1 + "' y '" + t2 + "'.",
                        token
                ));
            }
        }
    }

    public static void validarCondicionesBooleanas(List<ErrorLSSL> errores) {
        for (int i = 0; i < Repositorio.listaTokens.size(); i++) {
            Token token = Repositorio.listaTokens.get(i);

            if (!TokenTipo.SI.equals(token.getLexicalComp())
                    && !TokenTipo.WHILE.equals(token.getLexicalComp())) {
                continue;
            }

            boolean tieneRelacional = false;
            Token primerToken = null;

            for (Token t : TokenUtils.extraerHastaComponente(i + 2, TokenTipo.PAREN_DER)) {
                if (primerToken == null) {
                    primerToken = t;
                }
                if (TokenUtils.esOperadorRelacional(t.getLexicalComp())) {
                    tieneRelacional = true;
                    break;
                }
            }
            if (!tieneRelacional && primerToken != null) {
                String tipo = TokenUtils.obtenerTipoDeToken(primerToken);
                if (!TokenTipo.TIPO_LOGICO.equals(tipo)) {
                    errores.add(new ErrorLSSL(
                            ErrorSemantico.CONDICION_NO_BOOLEANA.id,
                            "Error semántico: La condición debe ser booleana (comparación o valor lógico).",
                            primerToken
                    ));
                }
            }
        }
    }

    public static void validarTiposEnComparaciones(List<ErrorLSSL> errores) {
        for (int i = 1; i < Repositorio.listaTokens.size() - 1; i++) {
            Token token = Repositorio.listaTokens.get(i);
            String comp = token.getLexicalComp();

            boolean esRelacional = TokenTipo.OP_MAYOR.equals(comp) || TokenTipo.OP_MENOR.equals(comp)
                    || TokenTipo.OP_MAYOR_IGUAL.equals(comp) || TokenTipo.OP_MENOR_IGUAL.equals(comp)
                    || TokenTipo.OP_IGUAL_IGUAL.equals(comp) || TokenTipo.OP_DIFERENTE.equals(comp);

            if (!esRelacional) {
                continue;
            }

            String tipoIzq = TokenUtils.obtenerTipoDeToken(Repositorio.listaTokens.get(i - 1));
            String tipoDer = TokenUtils.obtenerTipoDeToken(Repositorio.listaTokens.get(i + 1));

            if (tipoIzq == null || tipoDer == null) {
                continue;
            }

            String mensaje = mensajeComparacionInvalida(tipoIzq, tipoDer);
            if (mensaje != null) {
                errores.add(new ErrorLSSL(
                        ErrorSemantico.COMPARACION_INVALIDA.id,
                        "Error semántico: " + mensaje + " (tipos: '" + tipoIzq + "' y '" + tipoDer + "').",
                        token
                ));
            }
        }
    }

    // Retorna el mensaje de error si los dos tipos no son comparables, null si son válidos.
    private static String mensajeComparacionInvalida(String t1, String t2) {
        if ((esTipoNumerico(t1) && TokenTipo.TIPO_LOGICO.equals(t2))
                || (esTipoNumerico(t2) && TokenTipo.TIPO_LOGICO.equals(t1))) {
            return "No se puede comparar un número con un valor lógico";
        }
        if ((esTipoNumerico(t1) && TokenTipo.TIPO_TEXTO.equals(t2))
                || (esTipoNumerico(t2) && TokenTipo.TIPO_TEXTO.equals(t1))) {
            return "No se puede comparar un número con un texto";
        }
        if ((TokenTipo.TIPO_LOGICO.equals(t1) && TokenTipo.TIPO_TEXTO.equals(t2))
                || (TokenTipo.TIPO_TEXTO.equals(t1) && TokenTipo.TIPO_LOGICO.equals(t2))) {
            return "No se puede comparar un valor lógico con un texto";
        }
        return null;
    }

    private static boolean sonTiposCompatibles(String tipo1, String tipo2) {
        if (tipo1.equals(tipo2)) {
            return true;
        }
        return TokenTipo.TIPO_DECIMAL.equals(tipo1) && TokenTipo.TIPO_ENTERO.equals(tipo2);
    }

    private static boolean esTipoNumerico(String tipo) {
        return TokenTipo.TIPO_ENTERO.equals(tipo) || TokenTipo.TIPO_DECIMAL.equals(tipo) || TokenTipo.TIPO_CORTO.equals(tipo);
    }
}
