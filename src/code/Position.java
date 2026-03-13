package code;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

public class Position {
    public static void actualizarPosicionPuntero(RSyntaxTextArea textArea, javax.swing.JLabel lblStatus) {
    int caretPos = textArea.getCaretPosition();
    try {
        int linea = textArea.getLineOfOffset(caretPos);
        int columna = caretPos - textArea.getLineStartOffset(linea);
        
        lblStatus.setText("Columna: " + (columna + 1) + ", Renglón: " + (linea + 1) + ".");
    } catch (javax.swing.text.BadLocationException e) {
    }
}
}
