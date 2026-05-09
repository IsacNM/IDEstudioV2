package code;

import code.lexico.AnalizadorLexico;
import code.semantico.TokenTipo;
import compilerTools.Token;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.DefaultTableModel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

public class Compilar {

    private static final Logger LOG = Logger.getLogger(Compilar.class.getName());

    /** Lista cruda de tokens (incluye ERROR para que el reporte léxico funcione). */
    public static final ArrayList<Token> listaTokens = new ArrayList<>();

    public static void analizar(RSyntaxTextArea editor, DefaultTableModel modelo) {
        if (editor == null) return;

        modelo.setRowCount(0);
        listaTokens.clear();

        try (StringReader sr = new StringReader(editor.getText())) {
            AnalizadorLexico analizador = new AnalizadorLexico(sr);
            try {
                Token token;
                while ((token = analizador.yylex()) != null) {
                    modelo.addRow(new Object[]{
                        token.getLexicalComp(),
                        token.getLexeme(),
                        "[" + token.getLine() + "," + token.getColumn() + "]"
                    });
                    listaTokens.add(token);
                }
            } finally {
                try { analizador.yyclose(); } catch (Exception ignore) { /* ya cerrado */ }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Fallo durante el análisis léxico", e);
        }
    }

    public static boolean hayErroresLexicos() {
        for (Token t : listaTokens) {
            if (TokenTipo.ERROR.equals(t.getLexicalComp())) return true;
        }
        return false;
    }
}
