package code.semantico;

import compilerTools.ErrorLSSL;
import compilerTools.Token;
import java.util.HashSet;
import java.util.List;

public class AuxSemantico {

    // ── Variables no declaradas ──────────────────────────────────────────
    public static void validarVariablesNoDeclaradas(List<ErrorLSSL> errores) {
        for (int i = 0; i < Repositorio.listaTokens.size(); i++) {
            Token token = Repositorio.listaTokens.get(i);

            if (!TokenTipo.IDENTIFICADOR.equals(token.getLexicalComp())) continue;
            if (TokenUtils.tokenEn(i - 2, TokenTipo.VAR)) continue;

            if (!Repositorio.tablaSimbolos.containsKey(token.getLexeme())) {
                errores.add(new ErrorLSSL(
                        ErrorSemantico.VAR_NO_DECLARADA.id,
                        "Error semántico: La variable '" + token.getLexeme() + "' no ha sido declarada.",
                        token));
            }
        }
    }

    // ── Variables no inicializadas ───────────────────────────────────────
    public static void validarVariablesNoInicializadas(List<ErrorLSSL> errores) {
        HashSet<String> inicializadas = new HashSet<>();

        for (int i = 0; i < Repositorio.listaTokens.size(); i++) {
            Token token = Repositorio.listaTokens.get(i);
            if (!TokenTipo.IDENTIFICADOR.equals(token.getLexicalComp())) continue;
            String nombreVar = token.getLexeme();

            boolean esDeclaracion = TokenUtils.tokenEn(i - 2, TokenTipo.VAR)
                    || (TokenUtils.tokenEn(i - 1, TokenTipo.COMA) && hayVarAntes(i));

            if (esDeclaracion) {
                if (TokenUtils.tokenEn(i + 1, TokenTipo.ASIGNACION)) inicializadas.add(nombreVar);
                continue;
            }

            // leer(id) inicializa la variable
            if (TokenUtils.tokenEn(i - 2, TokenTipo.LEER)
                    && TokenUtils.tokenEn(i - 1, TokenTipo.PAREN_IZQ)) {
                inicializadas.add(nombreVar);
                continue;
            }

            if (TokenUtils.tokenEn(i + 1, TokenTipo.ASIGNACION)) {
                inicializadas.add(nombreVar);
            } else if (Repositorio.tablaSimbolos.containsKey(nombreVar)
                    && !inicializadas.contains(nombreVar)) {
                errores.add(new ErrorLSSL(
                        ErrorSemantico.VAR_NO_INICIALIZADA.id,
                        "Error semántico: La variable '" + nombreVar + "' no tiene un valor asignado.",
                        token));
            }
        }
    }

    private static boolean hayVarAntes(int desdeIndex) {
        for (int j = desdeIndex - 1; j >= 0; j--) {
            String comp = Repositorio.listaTokens.get(j).getLexicalComp();
            if (TokenTipo.VAR.equals(comp))        return true;
            if (TokenTipo.PUNTO_COMA.equals(comp)) return false;
        }
        return false;
    }

    // ── Tipos en asignaciones ────────────────────────────────────────────
    public static void validarTiposEnAsignaciones(List<ErrorLSSL> errores) {
        for (int i = 0; i < Repositorio.listaTokens.size(); i++) {
            Token token = Repositorio.listaTokens.get(i);

            if (!TokenTipo.IDENTIFICADOR.equals(token.getLexicalComp())) continue;
            if (!TokenUtils.tokenEn(i + 1, TokenTipo.ASIGNACION)) continue;

            String nombreVar = token.getLexeme();
            if (!Repositorio.tablaSimbolos.containsKey(nombreVar)) continue;

            String tipoVar = Repositorio.tablaSimbolos.get(nombreVar).getTipoDato();
            if (i + 2 >= Repositorio.listaTokens.size()) continue;
            Token valorToken = Repositorio.listaTokens.get(i + 2);
            String tipoValor = TokenUtils.obtenerTipoDeToken(valorToken);

            if (tipoValor != null && !sonTiposCompatibles(tipoVar, tipoValor)) {
                errores.add(new ErrorLSSL(
                        ErrorSemantico.TIPOS_INCOMPATIBLES.id,
                        "Error semántico: La variable '" + nombreVar + "' es de tipo '" + tipoVar
                        + "' pero se intentó asignar un valor de tipo '" + tipoValor + "'.",
                        token));
            }

            validarExpresionPorDestino(i + 2, nombreVar, tipoVar, errores);
        }
    }

    /**
     * Recorre la expresión hasta el siguiente PUNTO_COMA y reporta el
     * primer token incompatible con el tipo destino.
     *   ENTERO/CORTO → prohíbe FLOTANTE y vars DECIMAL
     *   DECIMAL      → prohíbe CADENA, CARACTER, BOOLEAN y vars TEXTO/CAR/LOGICO
     */
    private static void validarExpresionPorDestino(int inicio, String nombreVar,
            String tipoVar, List<ErrorLSSL> errores) {

        boolean destinoEntero  = TokenTipo.TIPO_ENTERO.equals(tipoVar)
                              || TokenTipo.TIPO_CORTO.equals(tipoVar);
        boolean destinoDecimal = TokenTipo.TIPO_DECIMAL.equals(tipoVar);
        if (!destinoEntero && !destinoDecimal) return;

        for (Token tk : TokenUtils.extraerHastaDelimitador(inicio)) {
            String comp = tk.getLexicalComp();

            if (destinoEntero && TokenTipo.FLOTANTE.equals(comp)) {
                errores.add(new ErrorLSSL(
                        ErrorSemantico.TIPOS_INCOMPATIBLES.id,
                        "Error semántico: No se puede usar el valor decimal '" + tk.getLexeme()
                        + "' en una expresión asignada a la variable entera '" + nombreVar + "'.",
                        tk));
                return;
            }
            if (destinoDecimal && (TokenTipo.CADENA.equals(comp) || TokenTipo.CARACTER.equals(comp)
                    || TokenTipo.BOOLEAN_TRUE.equals(comp) || TokenTipo.BOOLEAN_FALSE.equals(comp))) {
                errores.add(new ErrorLSSL(
                        ErrorSemantico.TIPOS_INCOMPATIBLES.id,
                        "Error semántico: No se puede usar el valor '" + tk.getLexeme()
                        + "' en una expresión aritmética asignada a la variable decimal '"
                        + nombreVar + "'.",
                        tk));
                return;
            }

            if (TokenTipo.IDENTIFICADOR.equals(comp)
                    && Repositorio.tablaSimbolos.containsKey(tk.getLexeme())) {
                String tipoOtra = Repositorio.tablaSimbolos.get(tk.getLexeme()).getTipoDato();

                if (destinoEntero && TokenTipo.TIPO_DECIMAL.equals(tipoOtra)) {
                    errores.add(new ErrorLSSL(
                            ErrorSemantico.TIPOS_INCOMPATIBLES.id,
                            "Error semántico: No se puede usar la variable decimal '" + tk.getLexeme()
                            + "' en una expresión asignada a la variable entera '" + nombreVar + "'.",
                            tk));
                    return;
                }
                if (destinoDecimal && TokenUtils.esTipoNoNumerico(tipoOtra)) {
                    errores.add(new ErrorLSSL(
                            ErrorSemantico.TIPOS_INCOMPATIBLES.id,
                            "Error semántico: No se puede usar la variable '" + tk.getLexeme()
                            + "' (de tipo " + tipoOtra
                            + ") en una expresión aritmética asignada a la variable decimal '"
                            + nombreVar + "'.",
                            tk));
                    return;
                }
            }
        }
    }

    // ── Tipos en operaciones aritméticas ─────────────────────────────────
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

            if (t1 != null && t2 != null
                    && (!TokenUtils.esTipoNumerico(t1) || !TokenUtils.esTipoNumerico(t2))) {
                errores.add(new ErrorLSSL(
                        ErrorSemantico.OPERACION_INVALIDA.id,
                        "Error semántico: Operación aritmética inválida entre '" + t1 + "' y '" + t2 + "'.",
                        token));
            }
        }
    }

    // ── Condiciones booleanas en si/mientras ─────────────────────────────
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
                if (primerToken == null) primerToken = t;
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
                            primerToken));
                }
            }
        }
    }

    // ── Tipos en comparaciones relacionales ──────────────────────────────
    public static void validarTiposEnComparaciones(List<ErrorLSSL> errores) {
        for (int i = 1; i < Repositorio.listaTokens.size() - 1; i++) {
            Token token = Repositorio.listaTokens.get(i);
            if (!TokenUtils.esOperadorRelacional(token.getLexicalComp())) continue;

            String tipoIzq = TokenUtils.obtenerTipoDeToken(Repositorio.listaTokens.get(i - 1));
            String tipoDer = TokenUtils.obtenerTipoDeToken(Repositorio.listaTokens.get(i + 1));
            if (tipoIzq == null || tipoDer == null) continue;

            String mensaje = mensajeComparacionInvalida(tipoIzq, tipoDer);
            if (mensaje != null) {
                errores.add(new ErrorLSSL(
                        ErrorSemantico.COMPARACION_INVALIDA.id,
                        "Error semántico: " + mensaje + " (tipos: '" + tipoIzq + "' y '" + tipoDer + "').",
                        token));
            }
        }
    }

    private static String mensajeComparacionInvalida(String t1, String t2) {
        if ((TokenUtils.esTipoNumerico(t1) && TokenTipo.TIPO_LOGICO.equals(t2))
                || (TokenUtils.esTipoNumerico(t2) && TokenTipo.TIPO_LOGICO.equals(t1))) {
            return "No se puede comparar un número con un valor lógico";
        }
        if ((TokenUtils.esTipoNumerico(t1) && TokenTipo.TIPO_TEXTO.equals(t2))
                || (TokenUtils.esTipoNumerico(t2) && TokenTipo.TIPO_TEXTO.equals(t1))) {
            return "No se puede comparar un número con un texto";
        }
        if ((TokenTipo.TIPO_LOGICO.equals(t1) && TokenTipo.TIPO_TEXTO.equals(t2))
                || (TokenTipo.TIPO_TEXTO.equals(t1) && TokenTipo.TIPO_LOGICO.equals(t2))) {
            return "No se puede comparar un valor lógico con un texto";
        }
        return null;
    }

    /**
     * Compatibilidad de tipos en asignación (sólo upcasts permitidos):
     *   decimal ← entero | corto
     *   entero  ↔ corto
     */
    private static boolean sonTiposCompatibles(String tipo1, String tipo2) {
        if (tipo1.equals(tipo2)) return true;
        if (TokenTipo.TIPO_DECIMAL.equals(tipo1)
                && (TokenTipo.TIPO_ENTERO.equals(tipo2) || TokenTipo.TIPO_CORTO.equals(tipo2))) return true;
        if (TokenTipo.TIPO_ENTERO.equals(tipo1) && TokenTipo.TIPO_CORTO.equals(tipo2))  return true;
        if (TokenTipo.TIPO_CORTO.equals(tipo1)  && TokenTipo.TIPO_ENTERO.equals(tipo2)) return true;
        return false;
    }
}
