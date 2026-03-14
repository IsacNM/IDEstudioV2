package code.semantico;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;


public class EvaluadorExpresiones {

    private static final Map<String, Integer> PRECEDENCIA = new HashMap<>();

    static {
        PRECEDENCIA.put("+",   1);
        PRECEDENCIA.put("-",   1);
        PRECEDENCIA.put("&+",  1);
        PRECEDENCIA.put("*",   2);
        PRECEDENCIA.put("/",   2);
        PRECEDENCIA.put("%",   2);
        PRECEDENCIA.put("-o-", 0);
        PRECEDENCIA.put("-y-", 0);
        PRECEDENCIA.put("~",   3);
    }

    public static ArrayList<String> infijaAPostfija(ArrayList<String> infija) {
        ArrayList<String> postfija = new ArrayList<>();
        Stack<String> pila = new Stack<>();

        for (String token : infija) {

            if ("~".equals(token)) {
                pila.push(token);

            } else if (esOperando(token)) {
                postfija.add(token);

            } else if ("(".equals(token)) {
                pila.push(token);

            } else if (")".equals(token)) {
                while (!pila.isEmpty() && !"(".equals(pila.peek())) {
                    postfija.add(pila.pop());
                }
                if (!pila.isEmpty()) pila.pop();

            } else if (esOperador(token)) {
                while (!pila.isEmpty()
                        && esOperador(pila.peek())
                        && PRECEDENCIA.get(pila.peek()) >= PRECEDENCIA.get(token)) {
                    postfija.add(pila.pop());
                }
                pila.push(token);
            }
        }

        while (!pila.isEmpty()) postfija.add(pila.pop());

        return postfija;
    }

    public static String evaluarPostfija(ArrayList<String> postfija) {
        Stack<Double> pila = new Stack<>();

        for (String token : postfija) {

            if ("~".equals(token)) {
                if (!pila.isEmpty()) {
                    pila.push(pila.pop() == 0 ? 1.0 : 0.0);
                }
                continue;
            }

            if (esOperando(token)) {
                pila.push(resolverOperando(token));
                continue;
            }

            if (esOperador(token)) {
                if (pila.size() < 2) continue;
                double op2 = pila.pop();
                double op1 = pila.pop();
                String resultado = aplicarOperador(token, op1, op2);

                if ("cierto".equals(resultado) || "falso".equals(resultado)) {
                    return resultado;
                }
                pila.push(Double.parseDouble(resultado));
            }
        }

        if (pila.isEmpty()) return "0";

        double resultadoFinal = pila.pop();
        return resultadoFinal == (int) resultadoFinal
            ? String.valueOf((int) resultadoFinal)
            : String.valueOf(resultadoFinal);
    }

    // Resuelve un operando a su valor numérico
    private static double resolverOperando(String token) {
        if ("cierto".equals(token)) return 1.0;
        if ("falso".equals(token))  return 0.0;

        try {
            return Double.parseDouble(token);
        } catch (NumberFormatException e) {
            if (Repositorio.tablaSimbolos.containsKey(token)) {
                String valor = Repositorio.tablaSimbolos.get(token).getValor()
                                .replace("\"", "");
                try {
                    return Double.parseDouble(valor);
                } catch (NumberFormatException ex) {
                    return 0.0;
                }
            }
            return 0.0;
        }
    }

    // Aplica un operador binario y retorna el resultado como String
    private static String aplicarOperador(String operador, double op1, double op2) {
        switch (operador) {
            case "+":   return formatear(op1 + op2);
            case "-":   return formatear(op1 - op2);
            case "*":   return formatear(op1 * op2);
            case "/":   return op2 != 0 ? formatear(op1 / op2) : "0";
            case "%":   return op2 != 0 ? formatear(op1 % op2) : "0";
            case "-y-": return (op1 != 0 && op2 != 0) ? "cierto" : "falso";
            case "-o-": return (op1 != 0 || op2 != 0) ? "cierto" : "falso";
            default:    return "0";
        }
    }

    // Formatea un double eliminando el .0 si es entero
    private static String formatear(double valor) {
        return valor == (int) valor
            ? String.valueOf((int) valor)
            : String.valueOf(valor);
    }

    private static boolean esOperando(String token) {
        if (token == null || token.isEmpty()) return false;
        try {
            Double.parseDouble(token);
            return true;
        } catch (NumberFormatException e) {
            return token.matches("[a-zA-Z_][a-zA-Z0-9_]*")
                || "cierto".equals(token)
                || "falso".equals(token);
        }
    }

    private static boolean esOperador(String token) {
        return PRECEDENCIA.containsKey(token);
    }
}