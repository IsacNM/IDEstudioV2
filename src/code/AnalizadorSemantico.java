package code;

import compilerTools.ErrorLSSL;
import compilerTools.Token;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Diego-CT
 */
public class AnalizadorSemantico {

    public static void validarSemantica() {
        // Pasamos la lista única de errores para que todos se guarden ahí
        AuxSemantico.validarVariablesNoDeclaradas(Repositorio.listaErrores);
        AuxSemantico.validarVariablesNoInicializadas(Repositorio.listaErrores);
        AuxSemantico.validarTiposEnAsignaciones(Repositorio.listaErrores);
        AuxSemantico.validarTiposEnOperaciones(Repositorio.listaErrores);
        AuxSemantico.validarCondicionesBooleanas(Repositorio.listaErrores);
        AuxSemantico.validarTiposEnComparaciones(Repositorio.listaErrores); // ← NUEVO

    }

    public static void construirTablaSimbolos() {
        // 1. Configuración de valores por defecto según el tipo
        HashMap<String, String> valoresDefecto = new HashMap<>();
        valoresDefecto.put("TIPO_ENTERO", "0");
        valoresDefecto.put("TIPO_DECIMAL", "0.0");
        valoresDefecto.put("TIPO_TEXTO", "\"\"");
        valoresDefecto.put("TIPO_CAR", "''");
        valoresDefecto.put("TIPO_LOGICO", "falso");
        valoresDefecto.put("TIPO_CORTO", "0");

        for (int i = 0; i < Repositorio.listaTokens.size(); i++) {
            Token token = Repositorio.listaTokens.get(i);

            // Detectar el inicio de una declaración con la palabra 'VAR'
            if ("VAR".equals(token.getLexicalComp())) {
                i++; // Avanzar al tipo de dato (ej. entero, decimal...)
                if (i >= Repositorio.listaTokens.size()) {
                    break;
                }

                Token tokenTipo = Repositorio.listaTokens.get(i);
                String tipo = tokenTipo.getLexicalComp();
                String valorDefault = valoresDefecto.getOrDefault(tipo, "null");

                i++; // Avanzar al primer identificador

                // Bucle para procesar todos los identificadores hasta el punto y coma
                while (i < Repositorio.listaTokens.size()) {
                    Token t = Repositorio.listaTokens.get(i);

                    // Si llegamos al final de la línea de declaración, salimos del while interno
                    if ("PUNTO_COMA".equals(t.getLexicalComp())) {
                        break;
                    }

                    if ("IDENTIFICADOR".equals(t.getLexicalComp())) {
                        String nombreVar = t.getLexeme();
                        String valorInicial = valorDefault;

                        // ============================================================
                        // GESTIÓN DE ERROR: VARIABLE YA DECLARADA (REDECLARACIÓN)
                        // ============================================================
                        if (Repositorio.tablaSimbolos.containsKey(nombreVar)) {
                            // Si ya existe en el HashMap, mandamos el error al Repositorio
                            Repositorio.listaErrores.add(new ErrorLSSL(
                                    101,
                                    "Error Semántico: La variable '" + nombreVar + "' ya ha sido declarada anteriormente.",
                                    t
                            ));
                            // Nota: No la añadimos de nuevo para no sobreescribir la original
                        } else {
                            // Si no existe, procedemos con la lógica normal

                            // Verificar si el siguiente token es una ASIGNACIÓN (:=)
                            if (i + 1 < Repositorio.listaTokens.size()
                                    && "ASIGNACION".equals(Repositorio.listaTokens.get(i + 1).getLexicalComp())) {

                                i += 2; // Saltar el ID y el :=
                                valorInicial = evaluarExpresion(i);

                                // Avanzar el puntero hasta encontrar una COMA o PUNTO_COMA
                                // para que el bucle principal no intente procesar la expresión
                                while (i < Repositorio.listaTokens.size()) {
                                    Token temp = Repositorio.listaTokens.get(i);
                                    String comp = temp.getLexicalComp();
                                    if ("PUNTO_COMA".equals(comp) || "COMA".equals(comp)) {
                                        i--; // Retroceder uno para que el i++ del final o el break lo manejen
                                        break;
                                    }
                                    i++;
                                }
                            }

                            // Crear el objeto Símbolo y guardarlo en la tabla
                            Simbolo simbolo = new Simbolo();
                            simbolo.setIdent(nombreVar);
                            simbolo.setTipoDato(tipo);
                            simbolo.setValor(valorInicial);
                            simbolo.setVarConstParam("var");

                            Repositorio.tablaSimbolos.put(nombreVar, simbolo);
                        }
                    }
                    i++; // Siguiente token en la declaración
                }
            }
        }
    }

    public static void procesarAsignaciones() {
        for (int i = 0; i < Repositorio.listaTokens.size(); i++) {
            Token token = Repositorio.listaTokens.get(i);

            if ("IDENTIFICADOR".equals(token.getLexicalComp())) {
                if (i + 1 < Repositorio.listaTokens.size()) {
                    Token sigToken = Repositorio.listaTokens.get(i + 1);

                    if ("ASIGNACION".equals(sigToken.getLexicalComp())) {
                        String nombreVar = token.getLexeme();

                        ArrayList<String> expresionInfija = new ArrayList<>();
                        int j = i + 2;

                        while (j < Repositorio.listaTokens.size()) {
                            Token t = Repositorio.listaTokens.get(j);

                            if ("PUNTO_COMA".equals(t.getLexicalComp())) {
                                break;
                            }

                            if ("IDENTIFICADOR".equals(t.getLexicalComp())) {
                                expresionInfija.add(t.getLexeme());
                            } else if ("ENTERO".equals(t.getLexicalComp())
                                    || "FLOTANTE".equals(t.getLexicalComp())) {
                                expresionInfija.add(t.getLexeme());
                            } else if ("SUMA".equals(t.getLexicalComp())) {
                                expresionInfija.add("+");
                            } else if ("RESTA".equals(t.getLexicalComp())) {
                                expresionInfija.add("-");
                            } else if ("MULTIPLICACION".equals(t.getLexicalComp())) {
                                expresionInfija.add("*");
                            } else if ("DIVISION".equals(t.getLexicalComp())) {
                                expresionInfija.add("/");
                            } else if ("MODULO".equals(t.getLexicalComp())) {
                                expresionInfija.add("%");
                            } else if ("CONCAT".equals(t.getLexicalComp())) {
                                expresionInfija.add("&+");
                            } else if ("PAREN_IZQ".equals(t.getLexicalComp())) {
                                expresionInfija.add("(");
                            } else if ("PAREN_DER".equals(t.getLexicalComp())) {
                                expresionInfija.add(")");
                            } else if ("CADENA".equals(t.getLexicalComp())) {
                                expresionInfija.add(t.getLexeme());
                            } else if ("BOOLEAN_TRUE".equals(t.getLexicalComp())) {
                                expresionInfija.add("cierto");
                            } else if ("BOOLEAN_FALSE".equals(t.getLexicalComp())) {
                                expresionInfija.add("falso");
                            } else if ("LOGICO_AND".equals(t.getLexicalComp())) {
                                expresionInfija.add("-y-");
                            } else if ("LOGICO_OR".equals(t.getLexicalComp())) {
                                expresionInfija.add("-o-");
                            } else if ("LOGICO_NOT".equals(t.getLexicalComp())) {
                                expresionInfija.add("~");
                            }

                            j++;
                        }

                        // ============================================================
                        // VERIFICAR COMPATIBILIDAD DE TIPOS ANTES DE ACTUALIZAR
                        // ============================================================
                        boolean hayErrorTipo = false;

                        if (Repositorio.tablaSimbolos.containsKey(nombreVar)) {
                            String tipoVar = Repositorio.tablaSimbolos.get(nombreVar).getTipoDato();

                            if ("TIPO_ENTERO".equals(tipoVar) || "TIPO_CORTO".equals(tipoVar)) {
                                // Revisar cada token de la expresión (entre := y ;)
                                for (int k = i + 2; k < Repositorio.listaTokens.size(); k++) {
                                    Token tk = Repositorio.listaTokens.get(k);
                                    if ("PUNTO_COMA".equals(tk.getLexicalComp())) {
                                        break;
                                    }

                                    // Flotante literal en la expresión
                                    if ("FLOTANTE".equals(tk.getLexicalComp())) {
                                        hayErrorTipo = true;
                                        break;
                                    }

                                    // Variable de tipo decimal usada en la expresión
                                    if ("IDENTIFICADOR".equals(tk.getLexicalComp())
                                            && Repositorio.tablaSimbolos.containsKey(tk.getLexeme())) {
                                        String tipoId = Repositorio.tablaSimbolos.get(tk.getLexeme()).getTipoDato();
                                        if ("TIPO_DECIMAL".equals(tipoId)) {
                                            hayErrorTipo = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        // Solo evaluar y actualizar si no hay error de tipo
                        if (!hayErrorTipo) {
                            String valorEvaluado;

                            if (expresionInfija.isEmpty()) {
                                valorEvaluado = "0";
                            } else if (expresionInfija.size() == 1) {
                                String val = expresionInfija.get(0);
                                if (Repositorio.tablaSimbolos.containsKey(val)) {
                                    valorEvaluado = Repositorio.tablaSimbolos.get(val).getValor();
                                } else {
                                    valorEvaluado = val;
                                }
                            } else {
                                ArrayList<String> postfija = EvaluadorExpresiones.infijaAPostfija(expresionInfija);
                                valorEvaluado = EvaluadorExpresiones.evaluarPostfija(postfija);

                                System.out.println("Expresión infija: " + expresionInfija);
                                System.out.println("Expresión postfija: " + postfija);
                                System.out.println("Resultado: " + valorEvaluado);
                            }

                            if (Repositorio.tablaSimbolos.containsKey(nombreVar)) {
                                Repositorio.tablaSimbolos.get(nombreVar).setValor(valorEvaluado);
                            }
                        }
                        // Si hayErrorTipo == true, el valor en la tabla NO se toca

                        i = j;
                    }
                }
            }
        }
    }

// Nuevo método para evaluar expresiones
    private static String evaluarExpresion(int inicio) {
        ArrayList<String> expresion = new ArrayList<>();

        for (int i = inicio; i < Repositorio.listaTokens.size(); i++) {
            Token t = Repositorio.listaTokens.get(i);
            if ("PUNTO_COMA".equals(t.getLexicalComp())) {
                break;
            }

            if ("IDENTIFICADOR".equals(t.getLexicalComp())) {
                // ============================================================
                // GESTIÓN DE ERROR: VARIABLE NO DECLARADA EN OPERACIÓN
                // ============================================================
                if (Repositorio.tablaSimbolos.containsKey(t.getLexeme())) {
                    String valor = Repositorio.tablaSimbolos.get(t.getLexeme()).getValor();
                    expresion.add(valor);
                } else {
                    // Si la variable no existe, registramos el error
                    Repositorio.listaErrores.add(new ErrorLSSL(
                            104,
                            "Error Semántico: La variable '" + t.getLexeme() + "' no ha sido declarada y no puede usarse en la expresión.",
                            t
                    ));
                    expresion.add("0"); // Ponemos un 0 temporal para que el cálculo no truene
                }
            } // ... el resto de tus validaciones de ENTERO, FLOTANTE, SUMA, etc.
            else if ("ENTERO".equals(t.getLexicalComp()) || "FLOTANTE".equals(t.getLexicalComp())) {
                expresion.add(t.getLexeme());
            } else if ("SUMA".equals(t.getLexicalComp())) {
                expresion.add("+");
            }
            // ... (etcétera)
        }
        return evaluarExpresionMatematica(expresion);
    }

// Evaluar expresión matemática simple
    private static String evaluarExpresionMatematica(ArrayList<String> tokens) {
        if (tokens.isEmpty()) {
            return "0";
        }
        if (tokens.size() == 1) {
            return tokens.get(0);
        }

        try {
            // Evaluar operaciones de izquierda a derecha
            double resultado = Double.parseDouble(tokens.get(0));

            for (int i = 1; i < tokens.size(); i += 2) {
                if (i + 1 >= tokens.size()) {
                    break;
                }

                String operador = tokens.get(i);
                double valor = Double.parseDouble(tokens.get(i + 1));

                switch (operador) {
                    case "+":
                        resultado += valor;
                        break;
                    case "-":
                        resultado -= valor;
                        break;
                    case "*":
                        resultado *= valor;
                        break;
                    case "/":
                        if (valor != 0) {
                            resultado /= valor;
                        }
                        break;
                }
            }

            // Si es entero, retornar sin decimales
            if (resultado == (int) resultado) {
                return String.valueOf((int) resultado);
            }
            return String.valueOf(resultado);

        } catch (Exception e) {
            return "ERROR";
        }

    }

    public static void validarSentenciasLeer() {
        for (int i = 0; i < Repositorio.listaTokens.size(); i++) {
            Token token = Repositorio.listaTokens.get(i);

            // 1. Buscamos el token de la palabra reservada LEER
            if ("LEER".equals(token.getLexicalComp())) {

                // La estructura es: leer ( IDENTIFICADOR ) ;
                // El identificador está 2 posiciones después de 'leer'
                if (i + 2 < Repositorio.listaTokens.size()) {
                    Token tokenInterno = Repositorio.listaTokens.get(i + 2);

                    // 2. Verificamos si es un identificador
                    if ("IDENTIFICADOR".equals(tokenInterno.getLexicalComp())) {
                        String nombreVar = tokenInterno.getLexeme();

                        // 3. ¡LA PRUEBA DE FUEGO! ¿Está en nuestra tabla de símbolos?
                        if (!Repositorio.tablaSimbolos.containsKey(nombreVar)) {
                            Repositorio.listaErrores.add(new ErrorLSSL(
                                    102,
                                    "Error Semántico: La variable '" + nombreVar + "' no ha sido declarada. No se puede leer un dato en una variable inexistente.",
                                    tokenInterno
                            ));
                        }
                    } // 4. Si lo que hay adentro NO es un identificador (ej. leer("hola"); o leer(10);)
                    // Tu gramática sintáctica ya debería capturar esto, pero aquí lo reforzamos
                    else if (!"PAREN_DER".equals(tokenInterno.getLexicalComp())) {
                        // Esto sirve de respaldo por si el sintáctico dejó pasar algo
                    }
                }
            }
        }
    }
}
