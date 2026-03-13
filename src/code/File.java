package code;

import static code.Repositorio.tablaSimbolos;
import compilerTools.ErrorLSSL;
import compilerTools.Token;
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import view.IDE;

public class File {

    public static IDE ide = new IDE();
    public static int count = 1;
    private static java.io.File ultimaRuta = null;

    public static void newFile(JTabbedPane jTabbed, JLabel positionLabel) {
        RSyntaxTextArea textArea = createEditor(positionLabel);
        RTextScrollPane sp = new RTextScrollPane(textArea);
        sp.putClientProperty("texto_original", "");

        configDocumentListener(jTabbed, textArea, sp);

        int numero = count(jTabbed);
        jTabbed.addTab("Nuevo Archivo " + numero + ".id", sp);
        jTabbed.setSelectedIndex(jTabbed.getTabCount() - 1);
        textArea.requestFocusInWindow();
    }

    public static void openFile(JTabbedPane jTabbed, JFileChooser jFileChooser, JLabel positionLabel) {
        configChooser(jFileChooser);

        if (jFileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        java.io.File archivo = jFileChooser.getSelectedFile();
        ultimaRuta = archivo.getParentFile();

        if (archivoYaAbierto(jTabbed, archivo)) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            RSyntaxTextArea textArea = createEditor(positionLabel);
            textArea.read(reader, null);

            RTextScrollPane sp = new RTextScrollPane(textArea);
            sp.putClientProperty("texto_original", textArea.getText());
            sp.putClientProperty("archivo_ruta", archivo.getAbsolutePath());

            configDocumentListener(jTabbed, textArea, sp);

            jTabbed.addTab(archivo.getName(), sp);
            int index = jTabbed.getTabCount() - 1;
            jTabbed.putClientProperty("ruta_" + index, archivo.getAbsolutePath());
            jTabbed.setSelectedIndex(index);
            textArea.requestFocusInWindow();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    public static void saveAsFile(JTabbedPane jTabbed, JFileChooser jFileChooser) {
        RTextScrollPane sp = (RTextScrollPane) jTabbed.getSelectedComponent();
        if (sp == null) {
            return;
        }
        configChooser(jFileChooser); 

        RSyntaxTextArea textArea = (RSyntaxTextArea) sp.getViewport().getView();

        if (jFileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            java.io.File archivoDestino = jFileChooser.getSelectedFile();

            if (!archivoDestino.getName().endsWith(".id")) {
                archivoDestino = new java.io.File(archivoDestino.getAbsolutePath() + ".id");
            }

            ultimaRuta = archivoDestino.getParentFile();

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivoDestino))) {
                writer.write(textArea.getText());

                int index = jTabbed.getSelectedIndex();
                jTabbed.setTitleAt(index, archivoDestino.getName());
                jTabbed.putClientProperty("ruta_" + index, archivoDestino.getAbsolutePath());
                sp.putClientProperty("archivo_ruta", archivoDestino.getAbsolutePath());

                markSaved(sp);
                String titulo = jTabbed.getTitleAt(index);
                if (titulo.endsWith("*")) {
                    jTabbed.setTitleAt(index, titulo.substring(0, titulo.length() - 1));
                }

                JOptionPane.showMessageDialog(null, "Archivo guardado con éxito.");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Error al guardar: " + e.getMessage());
            }
        }
    }

    public static void saveAllFiles(JTabbedPane jTabbed, JFileChooser jFileChooser) {
        for (int i = 0; i < jTabbed.getTabCount(); i++) {
            RTextScrollPane sp = (RTextScrollPane) jTabbed.getComponentAt(i);
            RSyntaxTextArea textArea = (RSyntaxTextArea) sp.getViewport().getView();

            String ruta = (String) sp.getClientProperty("archivo_ruta");
            if (ruta == null) {
                ruta = (String) jTabbed.getClientProperty("ruta_" + i);
            }

            if (ruta != null) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(ruta))) {
                    writer.write(textArea.getText());

                    markSaved(sp);
                    String titulo = jTabbed.getTitleAt(i);
                    if (titulo.endsWith("*")) {
                        jTabbed.setTitleAt(i, titulo.substring(0, titulo.length() - 1));
                    }
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "Error al guardar " + jTabbed.getTitleAt(i) + ": " + e.getMessage());
                }
            } else {
                jTabbed.setSelectedIndex(i);
                saveAsFile(jTabbed, jFileChooser);
            }
        }
        JOptionPane.showMessageDialog(null, "Operación 'Guardar Todo' finalizada.");
    }

    public static void saveFile(JTabbedPane jTabbed, JFileChooser jFileChooser) {
        int index = jTabbed.getSelectedIndex();
        if (index == -1) {
            return;
        }
        String rutaExistente = (String) jTabbed.getClientProperty("ruta_" + index);

        if (rutaExistente == null) {
            saveAsFile(jTabbed, jFileChooser);
        } else {
            RTextScrollPane sp = (RTextScrollPane) jTabbed.getSelectedComponent();
            RSyntaxTextArea textArea = (RSyntaxTextArea) sp.getViewport().getView();

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(rutaExistente))) {
                writer.write(textArea.getText());
                sp.putClientProperty("texto_original", textArea.getText());
                String titulo = jTabbed.getTitleAt(index);
                if (titulo.endsWith("*")) {
                    jTabbed.setTitleAt(index, titulo.substring(0, titulo.length() - 1));
                }
                System.out.println("Cambios guardados en: " + rutaExistente);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
            }
        }
    }

    public static void closeFile(JTabbedPane jTabbed, JFileChooser jFileChooser) {
        int index = jTabbed.getSelectedIndex();
        if (index == -1) {
            return;
        }

        RTextScrollPane sp = (RTextScrollPane) jTabbed.getComponentAt(index);
        RSyntaxTextArea textArea = (RSyntaxTextArea) sp.getViewport().getView();

        String textoActual = textArea.getText();
        String textoOriginal = (String) sp.getClientProperty("texto_original");
        if (textoOriginal == null) {
            textoOriginal = ""; 
        }
        if (!textoActual.equals(textoOriginal)) {
            int respuesta = JOptionPane.showConfirmDialog(
                    jTabbed,
                    "El archivo '" + jTabbed.getTitleAt(index) + "' tiene cambios sin guardar.\n¿Desea guardarlos?",
                    "Cerrar archivo",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (respuesta == JOptionPane.YES_OPTION) {
                saveFile(jTabbed, jFileChooser);
                jTabbed.remove(index);
            } else if (respuesta == JOptionPane.NO_OPTION) {
                jTabbed.remove(index);
            }
        } else {
            jTabbed.remove(index);
        }
    }

    public static void closeAllFiles(JTabbedPane jTabbed, JFileChooser jFileChooser) {
        for (int i = jTabbed.getTabCount() - 1; i >= 0; i--) {
            jTabbed.setSelectedIndex(i);
            closeFile(jTabbed, jFileChooser);
        }
    }

    public static void exitApp(JTabbedPane jTabbed, JFileChooser jFileChooser) {
        int confirmacion = JOptionPane.showConfirmDialog(
                null,
                "¿Está seguro de que desea salir de IDEstudio?",
                "Confirmar salida",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirmacion == JOptionPane.YES_OPTION) {
            closeAllFiles(jTabbed, jFileChooser);
            if (jTabbed.getTabCount() == 0) {
                System.exit(0);
            }
        }
    }

    //Métodos axuliares
    private static void configChooser(JFileChooser jFileChooser) {
        jFileChooser.setAcceptAllFileFilterUsed(false);
        jFileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos IDEstudio (*.id)", "id"));

        if (ultimaRuta != null) {
            jFileChooser.setCurrentDirectory(ultimaRuta);
        }
    }

    private static void configMenuContextual(RSyntaxTextArea textArea) {
        javax.swing.JPopupMenu popup = new javax.swing.JPopupMenu();
        String[] acciones = {"Copiar", "Cortar", "Pegar"};
        for (String accion : acciones) {
            javax.swing.JMenuItem item = new javax.swing.JMenuItem(accion);
            item.addActionListener(e -> {
                if (accion.equals("Copiar")) {
                    textArea.copy();
                } else if (accion.equals("Cortar")) {
                    textArea.cut();
                } else {
                    textArea.paste();
                }
            });
            popup.add(item);
        }
        textArea.setComponentPopupMenu(popup);
    }

    private static void configDocumentListener(JTabbedPane jTabbed, RSyntaxTextArea textArea, RTextScrollPane sp) {
    textArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {

        public void insertUpdate(javax.swing.event.DocumentEvent e) {
            verificar();
        }

        public void removeUpdate(javax.swing.event.DocumentEvent e) {
            verificar();
        }

        public void changedUpdate(javax.swing.event.DocumentEvent e) {
            verificar();
        }

        private void verificar() {
            int i = jTabbed.indexOfComponent(sp);
            if (i != -1) {
                updateSate(jTabbed, i);
                // ❌ NO limpiar errores aquí
            }
        }
    });
}

    private static int count(JTabbedPane jTabbed) {
        int n = 1;
        while (true) {
            String nameSearch = "Nuevo Archivo " + n + ".id";
            boolean existe = false;

            for (int i = 0; i < jTabbed.getTabCount(); i++) {
                String tituloTab = jTabbed.getTitleAt(i);

                // Si el título termina en *, se lo quitamos para la comparación
                if (tituloTab.endsWith("*")) {
                    tituloTab = tituloTab.substring(0, tituloTab.length() - 1);
                }

                if (tituloTab.equals(nameSearch)) {
                    existe = true;
                    break;
                }
            }

            if (!existe) {
                return n;
            }
            n++;
        }
    }

    private static boolean archivoYaAbierto(JTabbedPane jTabbed, java.io.File archivo) {
        for (int i = 0; i < jTabbed.getTabCount(); i++) {
            String rutaExistente = (String) jTabbed.getClientProperty("ruta_" + i);

            if (rutaExistente != null && archivo.getAbsolutePath().equals(rutaExistente)) {
                jTabbed.setSelectedIndex(i); 
                return true;
            }
        }
        return false;
    }

    public static void markSaved(RTextScrollPane sp) {
        RSyntaxTextArea textArea = (RSyntaxTextArea) sp.getViewport().getView();
        sp.putClientProperty("texto_original", textArea.getText());
    }

    public static void updateSate(JTabbedPane jTabbed, int index) {
        RTextScrollPane sp = (RTextScrollPane) jTabbed.getComponentAt(index);
        RSyntaxTextArea textArea = (RSyntaxTextArea) sp.getViewport().getView();

        String textoActual = textArea.getText();
        String textoOriginal = (String) sp.getClientProperty("texto_original");
        if (textoOriginal == null) {
            textoOriginal = "";
        }

        String tituloActual = jTabbed.getTitleAt(index);

        if (!textoActual.equals(textoOriginal)) {
            if (!tituloActual.endsWith("*")) {
                jTabbed.setTitleAt(index, tituloActual + "*");
            }
        } else {
            if (tituloActual.endsWith("*")) {
                jTabbed.setTitleAt(index, tituloActual.substring(0, tituloActual.length() - 1));
            }
        }
    }

    public static void errorTab(JTabbedPane jTabbed, int lineaConError) {
        RTextScrollPane sp = (RTextScrollPane) jTabbed.getSelectedComponent();
        if (sp != null) {
            RSyntaxTextArea textArea = (RSyntaxTextArea) sp.getViewport().getView();
            code.ErrorLine.colorearLinea(textArea, lineaConError);
        }
    }

    private static RSyntaxTextArea createEditor(JLabel positionLabel) {
        RSyntaxTextArea textArea = new RSyntaxTextArea();
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        textArea.setCodeFoldingEnabled(true);
        textArea.setCurrentLineHighlightColor(new Color(230, 240, 255));
        textArea.setHighlightCurrentLine(true);

        // Configuración de Autocompletado
        List<String> sugerencias = new ArrayList<>(List.of(code.AutoCompletadoPopUp.palabrasReservadas));
        new code.AutoCompletadoPopUp(textArea, sugerencias);

        // Listener de posición
        textArea.addCaretListener(e -> Position.actualizarPosicionPuntero(textArea, positionLabel));

        // Menú contextual (Popup)
        configMenuContextual(textArea);

        return textArea;
    }

/*
    RTextScrollPane sp = (RTextScrollPane) jTabbed.getSelectedComponent();
    if (sp == null) return;

    RSyntaxTextArea textArea = (RSyntaxTextArea) sp.getViewport().getView();

    // Limpiar errores previos
    Repositorio.limpiar();
    textArea.getHighlighter().removeAllHighlights();

    // 1. Léxico
    compilar.analizar(textArea, modeloTabla);

    // 2. Sincronizar tokens
    Repositorio.listaTokens.addAll(compilar.listaTokens);

    // 3. Sintáctico
    AnalizadorSintactico sint = new AnalizadorSintactico(Repositorio.listaTokens);
    sint.ejecutar();

    // 4. Marcar errores sintácticos
    if (!Repositorio.listaErrores.isEmpty()) {
        for (ErrorLSSL error : Repositorio.listaErrores) {
            int linea = error.getLine();   // ← ESTA es la llamada correcta
            errorTab(jTabbed, linea);
        }
    }
    }
    */
    
   

 public static void ejecutarCompilacion(JTabbedPane jTabbed, DefaultTableModel modeloTabla, JTextArea consola, JTable tablaSimbolos) {
    RTextScrollPane sp = (RTextScrollPane) jTabbed.getSelectedComponent();
    if (sp == null) return;
    RSyntaxTextArea textArea = (RSyntaxTextArea) sp.getViewport().getView();
    
    // 1. Limpiar todo antes de empezar
    Repositorio.limpiar();
    textArea.getHighlighter().removeAllHighlights();
    consola.setText("");
    
    // 2. Análisis Léxico
    compilar.analizar(textArea, modeloTabla);
    Repositorio.listaTokens.addAll(compilar.listaTokens);
    
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

// Método auxiliar para mostrar errores

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


