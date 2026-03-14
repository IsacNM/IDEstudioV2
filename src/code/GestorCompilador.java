package code;

import code.semantico.Simbolo;
import code.semantico.FiltroParentesis;
import code.sintactico.AnalizadorSintactico;
import code.semantico.AnalizadorSemantico;
import static code.editor.File.errorTab;
import code.semantico.Repositorio;
import compilerTools.ErrorLSSL;
import compilerTools.Token;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

public class GestorCompilador {

    public static void ejecutarCompilacion(JTabbedPane jTabbed, DefaultTableModel modeloTabla, JTextArea consola, JTable tablaSimbolos) {
        RTextScrollPane sp = (RTextScrollPane) jTabbed.getSelectedComponent();
        if (sp == null) {
            return;
        }
        RSyntaxTextArea textArea = (RSyntaxTextArea) sp.getViewport().getView();

        // 1. Limpiar todo antes de empezar
        Repositorio.limpiar();
        textArea.getHighlighter().removeAllHighlights();
        consola.setText("");

        // 2. Análisis Léxico
        Compilar.analizar(textArea, modeloTabla);
        Repositorio.listaTokens.addAll(Compilar.listaTokens);

        // Validar si no hay tokens
        if (Repositorio.listaTokens.isEmpty()) {
            consola.append("═══════════════════════════════════════════════════\n");
            consola.append("    ⚠ ERROR DE COMPILACIÓN\n");
            consola.append("═══════════════════════════════════════════════════\n\n");
            consola.append("Error sintáctico: Se esperaba la estructura completa del programa con 'INICIO' y 'FIN'\n");
            return;
        }

        // 3. Análisis Sintáctico (Llena listaErrores con fallos de gramática)
        AnalizadorSintactico sint = new AnalizadorSintactico(Repositorio.listaTokens);
        sint.ejecutar();

        // 4. Análisis Semántico (Llena listaErrores con fallos de lógica)
        AnalizadorSemantico.construirTablaSimbolos();
        AnalizadorSemantico.procesarAsignaciones();
        AnalizadorSemantico.validarSentenciasLeer();
        AnalizadorSemantico.validarSemantica(); // <--- Esta es la nueva llamada que creamos

        // 5. MOSTRAR RESULTADOS EN LA INTERFAZ
        mostrarTablaSimbolos(tablaSimbolos);

        // 6. PINTAR LÍNEAS ROJAS
        // Como unificamos todo en Repositorio.listaErrores, este ciclo 
        // marcará tanto errores de punto y coma como de variables no declaradas.
        if (!Repositorio.listaErrores.isEmpty()) {
            for (ErrorLSSL error : Repositorio.listaErrores) {
                // Mandamos llamar a tu método que pone el color rojo en el editor
                errorTab(jTabbed, error.getLine());
            }
        }

        // 7. Imprimir los mensajes detallados en la consola
        mostrarErroresEnConsola(consola);
    }

    private static void mostrarErroresEnConsola(JTextArea consola) {
        boolean hayErroresLexicos = false;
        for (Token t : Repositorio.listaTokens) {
            if ("ERROR".equals(t.getLexicalComp())) {
                hayErroresLexicos = true;
                break;
            }
        }

        boolean hayErroresParentesis = !FiltroParentesis.erroresEncontrados.isEmpty();

        if (Repositorio.listaErrores.isEmpty() && !hayErroresLexicos && !hayErroresParentesis) {
            consola.append("════════════════════════════════════════════════════════════════════════════════\n");
            consola.append("                               ✓ COMPILACIÓN EXITOSA\n");
            consola.append("════════════════════════════════════════════════════════════════════════════════\n");
            consola.append("No se encontraron errores.\n");
        } else {
            consola.append("════════════════════════════════════════════════════════════════════════════════\n");
            consola.append("                   ⚠ ERRORES DETECTADOS EN LA COMPILACIÓN\n");
            consola.append("════════════════════════════════════════════════════════════════════════════════\n\n");

            // 1. Mostrar Errores Léxicos
            if (hayErroresLexicos) {
                consola.append("--- ERRORES LÉXICOS ---\n\n");
                for (Token t : Repositorio.listaTokens) {
                    if ("ERROR".equals(t.getLexicalComp())) {
                        consola.append(String.format("Línea %d, Columna %d\n", t.getLine(), t.getColumn()));
                        consola.append("  ➤ Error léxico: Símbolo no reconocido '" + t.getLexeme() + "'\n");
                        consola.append("───────────────────────────────────────────────────\n");
                    }
                }
            }

            // 2. Mostrar Errores de Estructura y Lógica (Sintáctico + Semántico)
            if (!Repositorio.listaErrores.isEmpty()) {
                consola.append("--- ERRORES DE ESTRUCTURA Y LÓGICA ---\n\n");

                // FILTRO DE DUPLICADOS: Usamos un Set para no repetir el mismo mensaje en la misma línea
                java.util.HashSet<String> erroresVistos = new java.util.HashSet<>();
                int contadorReal = 1;

                for (ErrorLSSL error : Repositorio.listaErrores) {
                    // Creamos una "llave" única para identificar si el error ya se mostró
                    String llaveError = error.getLine() + "|" + error.toString();

                    if (!erroresVistos.contains(llaveError)) {
                        consola.append(String.format("[Error %d]\n", contadorReal++));
                        consola.append("  Línea: " + error.getLine() + ", Columna: " + error.getColumn() + "\n");
                        consola.append("  Detalle: " + error.toString() + "\n");
                        consola.append("───────────────────────────────────────────────────\n");

                        erroresVistos.add(llaveError); // Lo marcamos como visto
                    }
                }
            }

            // 3. Mostrar Errores de Paréntesis
            if (hayErroresParentesis) {
                consola.append("--- ERRORES DE PARÉNTESIS ---\n\n");
                for (String error : FiltroParentesis.erroresEncontrados) {
                    consola.append("  ➤ " + error + "\n");
                    consola.append("───────────────────────────────────────────────────\n");
                }
            }

            // Cálculo del total (solo errores únicos)
            consola.append("\nCompilación finalizada con errores.\n");
        }
    }

// Método para mostrar símbolos
    private static void mostrarTablaSimbolos(JTable tabla) {
        DefaultTableModel modelo = (DefaultTableModel) tabla.getModel();

        // Limpiar tabla antes de llenarla
        modelo.setRowCount(0);

        // Llenar con los símbolos del repositorio
        for (Simbolo simbolo : Repositorio.tablaSimbolos.values()) {
            Object[] fila = new Object[]{
                simbolo.getIdent(),
                simbolo.getTipoDato(),
                simbolo.getValor(),
                simbolo.getVarConstParam()
            };
            modelo.addRow(fila);
        }
    }

}
