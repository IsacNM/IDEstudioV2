/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package code;

import compilerTools.Token;
import java.util.ArrayList;

public class FiltroParentesis {
    
    /**
     * Filtra paréntesis en asignaciones y switch, pero valida que estén balanceados
     */
   public static ArrayList<String> erroresEncontrados = new ArrayList<>();
    
public static ArrayList<Token> filtrarYValidar(ArrayList<Token> tokens) {
    ArrayList<Token> resultado = new ArrayList<>();
    boolean dentroDeAsignacion = false;
    int contadorAsignacion = 0; 
    Token ultimoParenAbierto = null; // Para saber en qué línea falló

    for (Token t : tokens) {
        String comp = t.getLexicalComp();

        // 1. Entramos a la zona de asignación
        if ("ASIGNACION".equals(comp)) {
            dentroDeAsignacion = true;
            contadorAsignacion = 0; // Reiniciamos contador para esta expresión
            resultado.add(t);
            continue;
        }

        // 2. Al llegar al PUNTO_COMA, revisamos si se cerraron todos
        if ("PUNTO_COMA".equals(comp)) {
            if (dentroDeAsignacion && contadorAsignacion > 0) {
                // ERROR: Se acabó la línea y faltaron paréntesis de cierre
                Repositorio.listaErrores.add(new compilerTools.ErrorLSSL(110, 
                    "Error sintáctico: Faltan " + contadorAsignacion + " paréntesis de cierre ')' en la asignación.", 
                    ultimoParenAbierto != null ? ultimoParenAbierto : t));
            }
            dentroDeAsignacion = false;
            contadorAsignacion = 0;
            resultado.add(t);
            continue;
        }

        // 3. Contabilizar paréntesis
        if ("PAREN_IZQ".equals(comp)) {
            if (dentroDeAsignacion) {
                contadorAsignacion++;
                ultimoParenAbierto = t; // Guardamos el token para reportar la línea exacta
                continue; // Lo quitamos para el sintáctico
            }
        }

        if ("PAREN_DER".equals(comp)) {
            if (dentroDeAsignacion) {
                contadorAsignacion--;
                if (contadorAsignacion < 0) {
                    // ERROR: Sobró un paréntesis de cierre
                    Repositorio.listaErrores.add(new compilerTools.ErrorLSSL(111, 
                        "Error sintáctico: Sobra un paréntesis de cierre ')' en la asignación.", t));
                    contadorAsignacion = 0; // Reset para no arrastrar el error
                }
                continue; // Lo quitamos para el sintáctico
            }
        }

        // 4. Agregar tokens normales (incluye paréntesis de 'si' y 'mostrar' porque no están en asignación)
        resultado.add(t);
    }

    return resultado;
}
    private static void agregarError(int linea, int columna, String mensaje) {
        String errorCompleto = "Línea " + linea + ", Columna " + columna + ": " + mensaje;
        erroresEncontrados.add(errorCompleto);
    }
}
    
    /**
     * Agrega un error a la lista de errores
     */
 
