package code;

import compilerTools.Token;
import compilerTools.ErrorLSSL; // Importante para que reconozca la clase de errores
import java.util.ArrayList;
import java.util.HashSet;

public class AuxSemantico {

    // 1. Validar variables que se usan pero nunca se declararon con 'VAR'
    public static void validarVariablesNoDeclaradas(ArrayList<ErrorLSSL> errores) {
        for (int i = 0; i < Repositorio.listaTokens.size(); i++) {
            Token token = Repositorio.listaTokens.get(i);

            if ("IDENTIFICADOR".equals(token.getLexicalComp())) {
                String nombreVar = token.getLexeme();

                // Si el token anterior es un tipo de dato y el anterior a ese es 'VAR', es una declaración
                // No lo marcamos como error de "no declarada" porque apenas se está creando
                if (i >= 2 && "VAR".equals(Repositorio.listaTokens.get(i - 2).getLexicalComp())) {
                    continue;
                }

                if (!Repositorio.tablaSimbolos.containsKey(nombreVar)) {
                    // Creamos el objeto ErrorLSSL pasando el ID, el mensaje y el TOKEN (que tiene la línea)
                    errores.add(new ErrorLSSL(101, "Error semántico: La variable '" + nombreVar + "' no ha sido declarada", token));
                }
            }
        }
    }

    // 2. Validar que no se use una variable si no tiene un valor inicial
    public static void validarVariablesNoInicializadas(ArrayList<ErrorLSSL> errores) {
        HashSet<String> inicializadas = new HashSet<>();

        for (int i = 0; i < Repositorio.listaTokens.size(); i++) {
            Token token = Repositorio.listaTokens.get(i);

            if ("IDENTIFICADOR".equals(token.getLexicalComp())) {
                String nombreVar = token.getLexeme();

                // --- EL CAMBIO ESTÁ AQUÍ ---
                // Si detectamos que es una DECLARACIÓN (var tipo id), 
                // la marcamos como inicializada (con su valor por defecto).
                if (i >= 2 && "VAR".equals(Repositorio.listaTokens.get(i - 2).getLexicalComp())) {
                    inicializadas.add(nombreVar);
                    continue;
                }
                // -----------------------------

                // Si el token que sigue es :=, se actualiza su estado (inicialización explícita)
                if (i + 1 < Repositorio.listaTokens.size()
                        && "ASIGNACION".equals(Repositorio.listaTokens.get(i + 1).getLexicalComp())) {
                    inicializadas.add(nombreVar);
                } else {
                    // Si se usa y NO está en la tabla de símbolos (no declarada), 
                    // ese error ya lo maneja otro método. Aquí solo checamos que tenga valor.
                    if (Repositorio.tablaSimbolos.containsKey(nombreVar)) {
                        if (!inicializadas.contains(nombreVar)) {
                            errores.add(new ErrorLSSL(102,
                                    "Error semántico: La variable '" + nombreVar + "' no tiene un valor asignado.",
                                    token));
                        }
                    }
                }
            }
        }
    }

    // 3. Validar que el valor asignado sea del mismo tipo que la variable
    public static void validarTiposEnAsignaciones(ArrayList<ErrorLSSL> errores) {
        for (int i = 0; i < Repositorio.listaTokens.size(); i++) {
            Token token = Repositorio.listaTokens.get(i);

            if ("IDENTIFICADOR".equals(token.getLexicalComp())) {
                if (i + 2 < Repositorio.listaTokens.size()
                        && "ASIGNACION".equals(Repositorio.listaTokens.get(i + 1).getLexicalComp())) {

                    Token valor = Repositorio.listaTokens.get(i + 2);
                    String nombreVar = token.getLexeme();

                    if (Repositorio.tablaSimbolos.containsKey(nombreVar)) {
                        Simbolo simbolo = Repositorio.tablaSimbolos.get(nombreVar);
                        String tipoVar = simbolo.getTipoDato();
                        String tipoValor = obtenerTipoDeToken(valor);

                        if (tipoValor != null && !sonTiposCompatibles(tipoVar, tipoValor)) {
                            errores.add(new ErrorLSSL(103,
                                    "Error semántico: Incompatibilidad de tipos. La variable '" + nombreVar
                                    + "' es de tipo '" + tipoVar + "' pero se intentó asignar un valor de tipo '"
                                    + tipoValor + "'", token));
                        }

                        // ← NUEVO: Detectar flotante literal en expresión asignada a entero
                        if ("TIPO_ENTERO".equals(tipoVar) || "TIPO_CORTO".equals(tipoVar)) {
                            for (int j = i + 2; j < Repositorio.listaTokens.size(); j++) {
                                Token tk = Repositorio.listaTokens.get(j);
                                if ("PUNTO_COMA".equals(tk.getLexicalComp())) {
                                    break;
                                }
                                if ("FLOTANTE".equals(tk.getLexicalComp())) {
                                    errores.add(new ErrorLSSL(103,
                                            "Error semántico: No se puede usar un valor decimal '" + tk.getLexeme()
                                            + "' en una expresión asignada a la variable entera '" + nombreVar + "'", tk));
                                    break;
                                }
                                // Verificar si algún identificador en la expresión es decimal
                                if ("IDENTIFICADOR".equals(tk.getLexicalComp())
                                        && Repositorio.tablaSimbolos.containsKey(tk.getLexeme())) {
                                    String tipoId = Repositorio.tablaSimbolos.get(tk.getLexeme()).getTipoDato();
                                    if ("TIPO_DECIMAL".equals(tipoId)) {
                                        errores.add(new ErrorLSSL(103,
                                                "Error semántico: No se puede usar la variable decimal '" + tk.getLexeme()
                                                + "' en una expresión asignada a la variable entera '" + nombreVar + "'", tk));
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 4. Validar que operaciones (+, -, *, /) solo se hagan entre números
    public static void validarTiposEnOperaciones(ArrayList<ErrorLSSL> errores) {
        for (int i = 1; i < Repositorio.listaTokens.size() - 1; i++) {
            Token token = Repositorio.listaTokens.get(i);

            if ("SUMA".equals(token.getLexicalComp()) || "RESTA".equals(token.getLexicalComp())
                    || "MULTIPLICACION".equals(token.getLexicalComp()) || "DIVISION".equals(token.getLexicalComp())) {

                Token op1 = Repositorio.listaTokens.get(i - 1);
                Token op2 = Repositorio.listaTokens.get(i + 1);

                String t1 = obtenerTipoDeToken(op1);
                String t2 = obtenerTipoDeToken(op2);

                if (t1 != null && t2 != null) {
                    if (!esTipoNumerico(t1) || !esTipoNumerico(t2)) {
                        errores.add(new ErrorLSSL(104, "Error semántico: Operación aritmética inválida entre '" + t1 + "' y '" + t2 + "'", token));
                    }
                }
            }
        }
    }

    // 5. Validar que las condiciones de SI/WHILE sean booleanas
    public static void validarCondicionesBooleanas(ArrayList<ErrorLSSL> errores) {
        for (int i = 0; i < Repositorio.listaTokens.size(); i++) {
            Token token = Repositorio.listaTokens.get(i);

            // Buscamos SI o WHILE
            if ("SI".equals(token.getLexicalComp()) || "WHILE".equals(token.getLexicalComp())) {

                // Vamos a recolectar los tokens dentro de los paréntesis
                boolean tieneRelacional = false;
                Token primerTokenCondicion = null;

                int j = i + 2; // Saltamos el SI y el (
                while (j < Repositorio.listaTokens.size()) {
                    Token t = Repositorio.listaTokens.get(j);
                    if ("PAREN_DER".equals(t.getLexicalComp())) {
                        break; // Terminó la condición
                    }
                    if (primerTokenCondicion == null) {
                        primerTokenCondicion = t;
                    }

                    // Si encontramos un operador relacional, la expresión RESULTARÁ en booleano
                    if (t.getLexicalComp().startsWith("OP_MAYOR")
                            || t.getLexicalComp().startsWith("OP_MENOR")
                            || "OP_IGUAL_IGUAL".equals(t.getLexicalComp())
                            || "OP_DIFERENTE".equals(t.getLexicalComp())) {
                        tieneRelacional = true;
                    }
                    j++;
                }

                // Si NO tiene un operador relacional (como >, <, etc.)
                // Entonces el token solito DEBE ser de tipo lógico
                if (!tieneRelacional && primerTokenCondicion != null) {
                    String tipo = obtenerTipoDeToken(primerTokenCondicion);
                    if (!"TIPO_LOGICO".equals(tipo)) {
                        errores.add(new ErrorLSSL(105,
                                "Error semántico: La condición debe ser booleana (comparación o valor lógico)",
                                primerTokenCondicion));
                    }
                }
            }
        }
    }

    // --- MÉTODOS AUXILIARES ---
    private static String obtenerTipoDeToken(Token token) {
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

    private static boolean sonTiposCompatibles(String tipo1, String tipo2) {
        if (tipo1.equals(tipo2)) {
            return true;
        }
        // Solo decimal puede recibir entero, NO al revés
        if ("TIPO_DECIMAL".equals(tipo1) && "TIPO_ENTERO".equals(tipo2)) {
            return true;
        }
        // TIPO_ENTERO NO puede recibir TIPO_DECIMAL ← antes faltaba bloquear esto explícitamente
        return false;
    }

    private static boolean esTipoNumerico(String tipo) {
        return "TIPO_ENTERO".equals(tipo) || "TIPO_DECIMAL".equals(tipo) || "TIPO_CORTO".equals(tipo);
    }

public static void validarTiposEnComparaciones(ArrayList<ErrorLSSL> errores) {
    for (int i = 0; i < Repositorio.listaTokens.size(); i++) {
        Token token = Repositorio.listaTokens.get(i);
        
        String comp = token.getLexicalComp();
        boolean esRelacional = "OP_MAYOR".equals(comp)       || "OP_MENOR".equals(comp)  ||
                               "OP_MAYOR_IGUAL".equals(comp) || "OP_MENOR_IGUAL".equals(comp) ||
                               "OP_IGUAL_IGUAL".equals(comp) || "OP_DIFERENTE".equals(comp);
        
        if (!esRelacional || i == 0 || i + 1 >= Repositorio.listaTokens.size()) continue;
        
        Token izq = Repositorio.listaTokens.get(i - 1);
        Token der = Repositorio.listaTokens.get(i + 1);
        
        String tipoIzq = obtenerTipoDeToken(izq);
        String tipoDer = obtenerTipoDeToken(der);
        
        if (tipoIzq == null || tipoDer == null) continue;
        
        String mensaje = null;
        
        // Numérico (entero/decimal/corto) vs lógico
        if ((esTipoNumerico(tipoIzq) && "TIPO_LOGICO".equals(tipoDer)) ||
            (esTipoNumerico(tipoDer) && "TIPO_LOGICO".equals(tipoIzq))) {
            mensaje = "No se puede comparar un número con un valor lógico (booleano)";
        }
        // Numérico vs texto
        else if ((esTipoNumerico(tipoIzq) && "TIPO_TEXTO".equals(tipoDer)) ||
                 (esTipoNumerico(tipoDer) && "TIPO_TEXTO".equals(tipoIzq))) {
            mensaje = "No se puede comparar un número con un texto";
        }
        // Lógico vs texto
        else if (("TIPO_LOGICO".equals(tipoIzq) && "TIPO_TEXTO".equals(tipoDer)) ||
                 ("TIPO_TEXTO".equals(tipoIzq)   && "TIPO_LOGICO".equals(tipoDer))) {
            mensaje = "No se puede comparar un valor lógico (booleano) con un texto";
        }
        
        if (mensaje != null) {
            errores.add(new ErrorLSSL(106,
                "Error semántico: " + mensaje +
                " (tipos incompatibles: '" + tipoIzq + "' y '" + tipoDer + "')", token));
        }
    }
}


}
