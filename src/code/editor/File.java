package code.editor;

import static code.editor.Editor.createEditor;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.*;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
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
            code.editor.ErrorLine.colorearLinea(textArea, lineaConError);
        }
    }
}
