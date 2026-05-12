package code.editor;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

public class Position {

    private static final Logger LOG = Logger.getLogger(Position.class.getName());

    public static void actualizarPosicionPuntero(RSyntaxTextArea textArea, javax.swing.JLabel lblStatus) {
        int caretPos = textArea.getCaretPosition();
        try {
            int linea = textArea.getLineOfOffset(caretPos);
            int columna = caretPos - textArea.getLineStartOffset(linea);

            String texto = Time.idiomaEs
                    ? "Columna: " + (columna + 1) + ", Renglón: " + (linea + 1) + "."
                    : "Column: " + (columna + 1) + ", Line: " + (linea + 1) + ".";
            lblStatus.setText(texto);
        } catch (javax.swing.text.BadLocationException e) {
            // Posición fuera del documento — caso raro, registrar pero no
            // interrumpir el flujo del editor (la barra de estado queda intacta).
            LOG.log(Level.FINE, "BadLocation al actualizar posición de cursor", e);
        }
    }
}

