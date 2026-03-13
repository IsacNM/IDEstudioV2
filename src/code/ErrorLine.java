package code;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.swing.JTextPane;
import javax.swing.text.*;

public class ErrorLine implements Highlighter.HighlightPainter {

    private final Color color;

    public ErrorLine(Color color) {
        this.color = color;
    }

    @Override
    public void paint(Graphics g, int offs0, int offs1,
            Shape bounds, JTextComponent c) {

        try {
            Rectangle rect = c.modelToView(offs0);

            if (rect == null) {
                return;
            }

            // Pinta SOLO esa línea
            int lineHeight = rect.height;
            int y = rect.y;

            g.setColor(color);
            g.fillRect(0, y, c.getWidth(), lineHeight);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

   public static void colorearLinea(JTextComponent area, int numeroLinea) {
    try {
        Element root = area.getDocument().getDefaultRootElement();

        if (numeroLinea < 1 || numeroLinea > root.getElementCount()) {
            return;
        }

        Element linea = root.getElement(numeroLinea - 1);
        int start = linea.getStartOffset();
        int end = linea.getEndOffset();

        Highlighter.HighlightPainter painter =
                new ErrorLine(new Color(255, 210, 210));

        area.getHighlighter().addHighlight(start, end, painter);

    } catch (Exception e) {
        e.printStackTrace();
    }
}

}
