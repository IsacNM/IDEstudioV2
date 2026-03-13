package code;

import javax.swing.JTabbedPane;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

public class Edition {
    
    public static void copiar(JTabbedPane jTabbed) {
    RTextScrollPane sp = (RTextScrollPane) jTabbed.getSelectedComponent();
    if (sp != null) {
        RSyntaxTextArea textArea = (RSyntaxTextArea) sp.getViewport().getView();
        textArea.copy();
    }
}

public static void cortar(JTabbedPane jTabbed) {
    RTextScrollPane sp = (RTextScrollPane) jTabbed.getSelectedComponent();
    if (sp != null) {
        RSyntaxTextArea textArea = (RSyntaxTextArea) sp.getViewport().getView();
        textArea.cut();
    }
}

public static void pegar(JTabbedPane jTabbed) {
    RTextScrollPane sp = (RTextScrollPane) jTabbed.getSelectedComponent();
    if (sp != null) {
        RSyntaxTextArea textArea = (RSyntaxTextArea) sp.getViewport().getView();
        textArea.paste();
    }
}
}
