package code;

import compilerTools.Token;
import java.util.*;

public class EvaluadorExpresiones {
    
    private static final Map<String, Integer> PRECEDENCIA = new HashMap<>();
    
    static {
        PRECEDENCIA.put("+", 1);
        PRECEDENCIA.put("-", 1);
        PRECEDENCIA.put("&+", 1);
        PRECEDENCIA.put("*", 2);
        PRECEDENCIA.put("/", 2);
        PRECEDENCIA.put("%", 2);
        PRECEDENCIA.put("-o-", 0);  // OR menor precedencia
        PRECEDENCIA.put("-y-", 0);  // AND igual que OR
        PRECEDENCIA.put("~", 3);    // NOT mayor precedencia
    }
    
    public static ArrayList<String> infijaAPostfija(ArrayList<String> infija) {
        ArrayList<String> postfija = new ArrayList<>();
        Stack<String> pila = new Stack<>();
        
        for (String token : infija) {
            // NOT unario: siempre se apila
            if (token.equals("~")) {
                pila.push(token);
                continue;
            }
            
            // Operando normal
            if (esOperando(token)) {
                postfija.add(token);
            }
            // Paréntesis izquierdo
            else if (token.equals("(")) {
                pila.push(token);
            }
            // Paréntesis derecho
            else if (token.equals(")")) {
                while (!pila.isEmpty() && !pila.peek().equals("(")) {
                    postfija.add(pila.pop());
                }
                if (!pila.isEmpty()) {
                    pila.pop(); // Quitar el '('
                }
            }
            // Operador binario
            else if (esOperador(token)) {
                while (!pila.isEmpty() && 
                       esOperador(pila.peek()) && 
                       PRECEDENCIA.get(pila.peek()) >= PRECEDENCIA.get(token)) {
                    postfija.add(pila.pop());
                }
                pila.push(token);
            }
        }
        
        while (!pila.isEmpty()) {
            postfija.add(pila.pop());
        }
        
        return postfija;
    }
    
    public static String evaluarPostfija(ArrayList<String> postfija) {
        Stack<Double> pila = new Stack<>();
        
        for (String token : postfija) {
            
            // NOT unario (necesita solo un operando)
            if (token.equals("~")) {
                if (pila.isEmpty()) continue;
                double operando = pila.pop();
                pila.push(operando == 0 ? 1.0 : 0.0);
                continue;
            }
            
            if (esOperando(token)) {
                try {
                    pila.push(Double.parseDouble(token));
                } catch (NumberFormatException e) {
                    if ("cierto".equals(token)) {
                        pila.push(1.0); // cierto = 1
                        continue;
                    } else if ("falso".equals(token)) {
                        pila.push(0.0); // falso = 0
                        continue;
                    }
                    
                    if (Repositorio.tablaSimbolos.containsKey(token)) {
                        String valor = Repositorio.tablaSimbolos.get(token).getValor();
                        valor = valor.replace("\"", "");
                        try {
                            pila.push(Double.parseDouble(valor));
                        } catch (NumberFormatException ex) {
                            return valor;
                        }
                    } else {
                        pila.push(0.0);
                    }
                }
            }
            else if (esOperador(token)) {
                if (pila.size() < 2) continue;
                
                double operando2 = pila.pop();
                double operando1 = pila.pop();
                double resultado = 0;
                
                switch (token) {
                    case "+":  resultado = operando1 + operando2; break;
                    case "-":  resultado = operando1 - operando2; break;
                    case "*":  resultado = operando1 * operando2; break;
                    case "/":  if (operando2 != 0) resultado = operando1 / operando2; break;
                    case "%":  if (operando2 != 0) resultado = operando1 % operando2; break;
                    case "-y-": return (operando1 != 0 && operando2 != 0) ? "cierto" : "falso";
                    case "-o-": return (operando1 != 0 || operando2 != 0) ? "cierto" : "falso";
                }
                
                pila.push(resultado);
            }
        }
        
        if (pila.isEmpty()) return "0";
        
        double resultadoFinal = pila.pop();
        
        if (resultadoFinal == (int) resultadoFinal) {
            return String.valueOf((int) resultadoFinal);
        }
        return String.valueOf(resultadoFinal);
    }
    
    private static boolean esOperando(String token) {
        if (token == null || token.isEmpty()) return false;
        try {
            Double.parseDouble(token);
            return true;
        } catch (NumberFormatException e) {
            return token.matches("[a-zA-Z_][a-zA-Z0-9_]*") || 
                   token.equals("cierto") || 
                   token.equals("falso");
        }
    }
    
    private static boolean esOperador(String token) {
        return PRECEDENCIA.containsKey(token);
    }
}