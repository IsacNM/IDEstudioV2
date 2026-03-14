package code.semantico;

import compilerTools.ErrorLSSL;
import compilerTools.Token;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AnalizadorSemantico {

    private static final Map<String, String> VALORES_DEFECTO = Map.of(
        "TIPO_ENTERO",  "0",
        "TIPO_DECIMAL", "0.0",
        "TIPO_TEXTO",   "\"\"",
        "TIPO_CAR",     "''",
        "TIPO_LOGICO",  "falso",
        "TIPO_CORTO",   "0"
    );

    public static void validarSemantica() {
        AuxSemantico.validarVariablesNoDeclaradas(Repositorio.listaErrores);
        AuxSemantico.validarVariablesNoInicializadas(Repositorio.listaErrores);
        AuxSemantico.validarTiposEnAsignaciones(Repositorio.listaErrores);
        AuxSemantico.validarTiposEnOperaciones(Repositorio.listaErrores);
        AuxSemantico.validarCondicionesBooleanas(Repositorio.listaErrores);
        AuxSemantico.validarTiposEnComparaciones(Repositorio.listaErrores);
    }

    public static void construirTablaSimbolos() {
        for (int i = 0; i < Repositorio.listaTokens.size(); i++) {
            Token token = Repositorio.listaTokens.get(i);

            if (!"VAR".equals(token.getLexicalComp())) continue;

            i++;
            if (i >= Repositorio.listaTokens.size()) break;

            String tipo = Repositorio.listaTokens.get(i).getLexicalComp();
            String valorDefault = VALORES_DEFECTO.getOrDefault(tipo, "null");
            i++;

            while (i < Repositorio.listaTokens.size()) {
                Token t = Repositorio.listaTokens.get(i);

                if ("PUNTO_COMA".equals(t.getLexicalComp())) break;

                if ("IDENTIFICADOR".equals(t.getLexicalComp())) {
                    String nombreVar = t.getLexeme();

                    if (Repositorio.tablaSimbolos.containsKey(nombreVar)) {
                        Repositorio.listaErrores.add(new ErrorLSSL(
                            ErrorSemantico.VAR_NO_DECLARADA.id,
                            "Error semántico: La variable '" + nombreVar + "' ya fue declarada anteriormente.",
                            t
                        ));
                    } else {
                        String valorInicial = valorDefault;

                        if (i + 1 < Repositorio.listaTokens.size()
                                && "ASIGNACION".equals(Repositorio.listaTokens.get(i + 1).getLexicalComp())) {
                            i += 2;
                            valorInicial = evaluarExpresion(i);

                            while (i < Repositorio.listaTokens.size()) {
                                String comp = Repositorio.listaTokens.get(i).getLexicalComp();
                                if ("PUNTO_COMA".equals(comp) || "COMA".equals(comp)) {
                                    i--;
                                    break;
                                }
                                i++;
                            }
                        }

                        Simbolo simbolo = new Simbolo();
                        simbolo.setIdent(nombreVar);
                        simbolo.setTipoDato(tipo);
                        simbolo.setValor(valorInicial);
                        simbolo.setVarConstParam("var");
                        Repositorio.tablaSimbolos.put(nombreVar, simbolo);
                    }
                }
                i++;
            }
        }
    }

    public static void procesarAsignaciones() {
        for (int i = 0; i < Repositorio.listaTokens.size(); i++) {
            Token token = Repositorio.listaTokens.get(i);

            if (!"IDENTIFICADOR".equals(token.getLexicalComp())) continue;
            if (i + 1 >= Repositorio.listaTokens.size()) continue;
            if (!"ASIGNACION".equals(Repositorio.listaTokens.get(i + 1).getLexicalComp())) continue;

            String nombreVar = token.getLexeme();
            List<String> expresionInfija = new ArrayList<>();
            int j = i + 2;

            while (j < Repositorio.listaTokens.size()) {
                Token t = Repositorio.listaTokens.get(j);
                if ("PUNTO_COMA".equals(t.getLexicalComp())) break;

                String operando = tokenAOperando(t);
                if (operando != null) expresionInfija.add(operando);
                j++;
            }

            boolean hayErrorTipo = tieneErrorDeTipo(nombreVar, i);

            if (!hayErrorTipo) {
                String valorEvaluado = evaluarInfija(expresionInfija, nombreVar);
                if (Repositorio.tablaSimbolos.containsKey(nombreVar)) {
                    Repositorio.tablaSimbolos.get(nombreVar).setValor(valorEvaluado);
                }
            }

            i = j;
        }
    }

    public static void validarSentenciasLeer() {
        for (int i = 0; i < Repositorio.listaTokens.size(); i++) {
            Token token = Repositorio.listaTokens.get(i);

            if (!"LEER".equals(token.getLexicalComp())) continue;
            if (i + 2 >= Repositorio.listaTokens.size()) continue;

            Token tokenInterno = Repositorio.listaTokens.get(i + 2);

            if ("IDENTIFICADOR".equals(tokenInterno.getLexicalComp())
                    && !Repositorio.tablaSimbolos.containsKey(tokenInterno.getLexeme())) {
                Repositorio.listaErrores.add(new ErrorLSSL(
                    ErrorSemantico.VAR_NO_INICIALIZADA.id,
                    "Error semántico: La variable '" + tokenInterno.getLexeme() + "' no ha sido declarada.",
                    tokenInterno
                ));
            }
        }
    }

    // Verifica si la expresión asignada a nombreVar contiene tipos incompatibles (decimal en entero/corto).
    private static boolean tieneErrorDeTipo(String nombreVar, int posAsignacion) {
        if (!Repositorio.tablaSimbolos.containsKey(nombreVar)) return false;

        String tipoVar = Repositorio.tablaSimbolos.get(nombreVar).getTipoDato();
        if (!"TIPO_ENTERO".equals(tipoVar) && !"TIPO_CORTO".equals(tipoVar)) return false;

        for (int k = posAsignacion + 2; k < Repositorio.listaTokens.size(); k++) {
            Token tk = Repositorio.listaTokens.get(k);
            if ("PUNTO_COMA".equals(tk.getLexicalComp())) break;

            if ("FLOTANTE".equals(tk.getLexicalComp())) return true;

            if ("IDENTIFICADOR".equals(tk.getLexicalComp())
                    && Repositorio.tablaSimbolos.containsKey(tk.getLexeme())) {
                if ("TIPO_DECIMAL".equals(Repositorio.tablaSimbolos.get(tk.getLexeme()).getTipoDato())) {
                    return true;
                }
            }
        }
        return false;
    }

    // Evalúa una expresión infija y retorna el valor resultante como String.
    private static String evaluarInfija(List<String> infija, String nombreVar) {
        if (infija.isEmpty()) return "0";

        if (infija.size() == 1) {
            String val = infija.get(0);
            return Repositorio.tablaSimbolos.containsKey(val)
                ? Repositorio.tablaSimbolos.get(val).getValor()
                : val;
        }

        return EvaluadorExpresiones.evaluarPostfija(
            EvaluadorExpresiones.infijaAPostfija(new ArrayList<>(infija))
        );
    }

    // Convierte un token a su representación string dentro de una expresión.
    // Retorna null si el token no es parte de una expresión evaluable.
    private static String tokenAOperando(Token t) {
        switch (t.getLexicalComp()) {
            case "IDENTIFICADOR":
            case "ENTERO":
            case "FLOTANTE":
            case "CADENA":         return t.getLexeme();
            case "BOOLEAN_TRUE":   return "cierto";
            case "BOOLEAN_FALSE":  return "falso";
            case "SUMA":           return "+";
            case "RESTA":          return "-";
            case "MULTIPLICACION": return "*";
            case "DIVISION":       return "/";
            case "MODULO":         return "%";
            case "CONCAT":         return "&+";
            case "PAREN_IZQ":      return "(";
            case "PAREN_DER":      return ")";
            case "LOGICO_AND":     return "-y-";
            case "LOGICO_OR":      return "-o-";
            case "LOGICO_NOT":     return "~";
            default:               return null;
        }
    }

    private static String evaluarExpresion(int inicio) {
        List<String> expresion = new ArrayList<>();

        for (int i = inicio; i < Repositorio.listaTokens.size(); i++) {
            Token t = Repositorio.listaTokens.get(i);
            if ("PUNTO_COMA".equals(t.getLexicalComp())) break;

            if ("IDENTIFICADOR".equals(t.getLexicalComp())) {
                if (Repositorio.tablaSimbolos.containsKey(t.getLexeme())) {
                    expresion.add(Repositorio.tablaSimbolos.get(t.getLexeme()).getValor());
                } else {
                    Repositorio.listaErrores.add(new ErrorLSSL(
                        ErrorSemantico.OPERACION_INVALIDA.id,
                        "Error semántico: La variable '" + t.getLexeme() + "' no ha sido declarada.",
                        t
                    ));
                    expresion.add("0");
                }
            } else {
                String op = tokenAOperando(t);
                if (op != null) expresion.add(op);
            }
        }

        if (expresion.isEmpty()) return "0";
        if (expresion.size() == 1) return expresion.get(0);

        return EvaluadorExpresiones.evaluarPostfija(
            EvaluadorExpresiones.infijaAPostfija(new ArrayList<>(expresion))
        );
    }
}