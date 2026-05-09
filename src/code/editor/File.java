package code.editor;

import static code.editor.Editor.createEditor;
import java.awt.Component;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

public class File {

    public static int count = 1;
    private static java.io.File ultimaRuta = null;

    // ── Acceso al RSyntaxTextArea de un tab ─────────────────────────────

    /** RSyntaxTextArea del tab seleccionado, o null si no hay ninguno. */
    public static RSyntaxTextArea getTextAreaActual(JTabbedPane jTabbed) {
        return getTextAreaAt(jTabbed, jTabbed.getSelectedIndex());
    }

    /** RSyntaxTextArea del tab indicado, o null si el índice es inválido. */
    public static RSyntaxTextArea getTextAreaAt(JTabbedPane jTabbed, int index) {
        if (index < 0 || index >= jTabbed.getTabCount()) return null;
        RTextScrollPane sp = (RTextScrollPane) jTabbed.getComponentAt(index);
        return sp == null ? null : (RSyntaxTextArea) sp.getViewport().getView();
    }

    /** Ruta absoluta del tab indicado (o null si no se ha guardado). */
    public static String getRutaArchivo(JTabbedPane jTabbed, int index) {
        if (index < 0 || index >= jTabbed.getTabCount()) return null;
        RTextScrollPane sp = (RTextScrollPane) jTabbed.getComponentAt(index);
        if (sp == null) return null;
        String ruta = (String) sp.getClientProperty("archivo_ruta");
        if (ruta == null) ruta = (String) jTabbed.getClientProperty("ruta_" + index);
        return ruta;
    }

    /** Quita el sufijo '*' del título del tab si existía. */
    private static void quitarMarcaNoGuardado(JTabbedPane jTabbed, int index) {
        String titulo = jTabbed.getTitleAt(index);
        if (titulo.endsWith("*")) {
            jTabbed.setTitleAt(index, titulo.substring(0, titulo.length() - 1));
        }
    }

    // ── Operaciones de archivo ───────────────────────────────────────────

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

        if (jFileChooser.showOpenDialog(parentOf(jTabbed)) != JFileChooser.APPROVE_OPTION) return;

        java.io.File archivo = jFileChooser.getSelectedFile();
        ultimaRuta = archivo.getParentFile();

        if (archivoYaAbierto(jTabbed, archivo)) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(archivo, StandardCharsets.UTF_8))) {
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
        if (sp == null) return;

        configChooser(jFileChooser);
        RSyntaxTextArea textArea = (RSyntaxTextArea) sp.getViewport().getView();

        if (jFileChooser.showSaveDialog(parentOf(jTabbed)) != JFileChooser.APPROVE_OPTION) return;

        java.io.File archivoDestino = jFileChooser.getSelectedFile();
        if (!archivoDestino.getName().endsWith(".id")) {
            archivoDestino = new java.io.File(archivoDestino.getAbsolutePath() + ".id");
        }
        ultimaRuta = archivoDestino.getParentFile();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivoDestino, StandardCharsets.UTF_8))) {
            writer.write(textArea.getText());

            int index = jTabbed.getSelectedIndex();
            jTabbed.setTitleAt(index, archivoDestino.getName());
            jTabbed.putClientProperty("ruta_" + index, archivoDestino.getAbsolutePath());
            sp.putClientProperty("archivo_ruta", archivoDestino.getAbsolutePath());

            markSaved(sp);
            quitarMarcaNoGuardado(jTabbed, index);

            JOptionPane.showMessageDialog(null, "Archivo guardado con éxito.");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error al guardar: " + e.getMessage());
        }
    }

    public static void saveAllFiles(JTabbedPane jTabbed, JFileChooser jFileChooser) {
        for (int i = 0; i < jTabbed.getTabCount(); i++) {
            RTextScrollPane sp = (RTextScrollPane) jTabbed.getComponentAt(i);
            RSyntaxTextArea textArea = (RSyntaxTextArea) sp.getViewport().getView();
            String ruta = getRutaArchivo(jTabbed, i);

            if (ruta != null) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(ruta, StandardCharsets.UTF_8))) {
                    writer.write(textArea.getText());
                    markSaved(sp);
                    quitarMarcaNoGuardado(jTabbed, i);
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
        if (index == -1) return;

        String rutaExistente = (String) jTabbed.getClientProperty("ruta_" + index);
        if (rutaExistente == null) {
            saveAsFile(jTabbed, jFileChooser);
            return;
        }

        RSyntaxTextArea textArea = getTextAreaActual(jTabbed);
        if (textArea == null) return;
        RTextScrollPane sp = (RTextScrollPane) jTabbed.getSelectedComponent();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(rutaExistente, StandardCharsets.UTF_8))) {
            writer.write(textArea.getText());
            sp.putClientProperty("texto_original", textArea.getText());
            quitarMarcaNoGuardado(jTabbed, index);
            System.out.println("Cambios guardados en: " + rutaExistente);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    public static void closeFile(JTabbedPane jTabbed, JFileChooser jFileChooser) {
        int index = jTabbed.getSelectedIndex();
        if (index == -1) return;

        RTextScrollPane sp = (RTextScrollPane) jTabbed.getComponentAt(index);
        RSyntaxTextArea textArea = (RSyntaxTextArea) sp.getViewport().getView();

        String textoActual   = textArea.getText();
        String textoOriginal = (String) sp.getClientProperty("texto_original");
        if (textoOriginal == null) textoOriginal = "";

        if (textoActual.equals(textoOriginal)) {
            jTabbed.remove(index);
            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(
                jTabbed,
                "El archivo '" + jTabbed.getTitleAt(index) + "' tiene cambios sin guardar.\n¿Desea guardarlos?",
                "Cerrar archivo",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (respuesta == JOptionPane.YES_OPTION) {
            saveFile(jTabbed, jFileChooser);
            jTabbed.remove(index);
        } else if (respuesta == JOptionPane.NO_OPTION) {
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
                JOptionPane.QUESTION_MESSAGE);

        if (confirmacion == JOptionPane.YES_OPTION) {
            closeAllFiles(jTabbed, jFileChooser);
            if (jTabbed.getTabCount() == 0) System.exit(0);
        }
    }

    // ── Auxiliares ───────────────────────────────────────────────────────

    /** Frame padre del componente para anclar diálogos modales. */
    private static Window parentOf(Component c) {
        return c == null ? null : SwingUtilities.getWindowAncestor(c);
    }

    private static void configChooser(JFileChooser jFileChooser) {
        jFileChooser.setAcceptAllFileFilterUsed(false);
        jFileChooser.setFileFilter(new FileNameExtensionFilter("Archivos IDEstudio (*.id)", "id"));
        if (ultimaRuta != null) jFileChooser.setCurrentDirectory(ultimaRuta);
    }

    private static void configDocumentListener(JTabbedPane jTabbed, RSyntaxTextArea textArea, RTextScrollPane sp) {
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { verificar(); }
            @Override public void removeUpdate(DocumentEvent e)  { verificar(); }
            @Override public void changedUpdate(DocumentEvent e) { verificar(); }
            private void verificar() {
                int i = jTabbed.indexOfComponent(sp);
                if (i != -1) updateSate(jTabbed, i);
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
                if (tituloTab.endsWith("*")) tituloTab = tituloTab.substring(0, tituloTab.length() - 1);
                if (tituloTab.equals(nameSearch)) { existe = true; break; }
            }
            if (!existe) return n;
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
        RSyntaxTextArea textArea = getTextAreaAt(jTabbed, index);
        if (textArea == null) return;
        RTextScrollPane sp = (RTextScrollPane) jTabbed.getComponentAt(index);

        String textoActual   = textArea.getText();
        String textoOriginal = (String) sp.getClientProperty("texto_original");
        if (textoOriginal == null) textoOriginal = "";

        String tituloActual = jTabbed.getTitleAt(index);
        if (!textoActual.equals(textoOriginal)) {
            if (!tituloActual.endsWith("*")) jTabbed.setTitleAt(index, tituloActual + "*");
        } else {
            quitarMarcaNoGuardado(jTabbed, index);
        }
    }

    public static void errorTab(JTabbedPane jTabbed, int lineaConError) {
        RSyntaxTextArea textArea = getTextAreaActual(jTabbed);
        if (textArea != null) {
            ErrorLine.colorearLinea(textArea, lineaConError);
        }
    }
}
