package code.editor;

import java.util.function.Consumer;
import javax.swing.JTabbedPane;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

public class Edition {

    public static void copiar(JTabbedPane jTabbed) { ejecutar(jTabbed, RSyntaxTextArea::copy);  }
    public static void cortar(JTabbedPane jTabbed) { ejecutar(jTabbed, RSyntaxTextArea::cut);   }
    public static void pegar(JTabbedPane jTabbed)  { ejecutar(jTabbed, RSyntaxTextArea::paste); }

    /** Ejecuta una operación sobre el RSyntaxTextArea del tab activo (no-op si no hay). */
    private static void ejecutar(JTabbedPane jTabbed, Consumer<RSyntaxTextArea> op) {
        RSyntaxTextArea textArea = File.getTextAreaActual(jTabbed);
        if (textArea != null) op.accept(textArea);
    }
}
