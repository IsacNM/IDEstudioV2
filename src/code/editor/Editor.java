package code.editor;

import code.utils.Position;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

public class Editor {

    public static RSyntaxTextArea createEditor(JLabel positionLabel) {
        RSyntaxTextArea textArea = new RSyntaxTextArea();
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        textArea.setCodeFoldingEnabled(true);
        textArea.setCurrentLineHighlightColor(new Color(230, 240, 255));
        textArea.setHighlightCurrentLine(true);

        // Configuración de Autocompletado
        List<String> sugerencias = new ArrayList<>(List.of(code.utils.AutoCompletadoPopUp.palabrasReservadas));
        new code.utils.AutoCompletadoPopUp(textArea, sugerencias);

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
