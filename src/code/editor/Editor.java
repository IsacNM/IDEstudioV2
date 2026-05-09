package code.editor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

public class Editor {

    static {
        // Registrar el TokenMaker custom de IDEstudio una sola vez (idempotente)
        // antes de crear cualquier editor.
        IDEstudioTokenMaker.registrar();
    }

    public static RSyntaxTextArea createEditor(JLabel positionLabel) {
        RSyntaxTextArea textArea = new RSyntaxTextArea();
        textArea.setSyntaxEditingStyle(IDEstudioTokenMaker.SYNTAX_STYLE);
        textArea.setCodeFoldingEnabled(true);
        textArea.setCurrentLineHighlightColor(new Color(230, 240, 255));
        textArea.setHighlightCurrentLine(true);

        // Aplicar tema persistido por usuario (~/.idestudio/config.properties):
        // fuente, tamaño, estilo y colores por tipo de token.
        EditorTema.aplicar(textArea);

        // Configuración de Autocompletado
        List<String> sugerencias = new ArrayList<>(List.of(LanguageKeywords.all()));
        new AutoCompletadoPopUp(textArea, sugerencias);

        // Listener de posición
        textArea.addCaretListener(e -> Position.actualizarPosicionPuntero(textArea, positionLabel));

        // Menú contextual (Popup)
        configMenuContextual(textArea);

        return textArea;
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
}
