package code;

import compilerTools.Token;
import java.io.StringReader;
import java.util.ArrayList;
import javax.swing.table.DefaultTableModel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

public class compilar {
    public static ArrayList<Token> listaTokens = new ArrayList<>();


    public static void analizar(RSyntaxTextArea editor, DefaultTableModel modelo) {
    try {
        if (editor == null) return;

        String codigo = editor.getText();

        AnalizadorLexico analizador =
                new AnalizadorLexico(new StringReader(codigo));

        modelo.setRowCount(0);
        listaTokens.clear();

        Token token = analizador.yylex();

        while (token != null) {

            Object[] fila = {
                token.getLexicalComp(),
                token.getLexeme(),
                "[" + token.getLine() + "," + token.getColumn() + "]"
            };

            modelo.addRow(fila);

            if (!token.getLexicalComp().equals("ERROR")) {
                listaTokens.add(token);
            }

            token = analizador.yylex();
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
}

}
