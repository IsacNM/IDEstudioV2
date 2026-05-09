package code.editor;

import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.text.BadLocationException;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

/**
 * Popup de autocompletado con soporte de snippets:
 *  - Mientras se escribe se sugieren palabras reservadas.
 *  - Al confirmar (Enter o Tab) si la palabra tiene plantilla asociada
 *    se expande a la estructura completa y el caret se posiciona en el
 *    primer "hueco" marcado por «|».
 *  - Si no hay plantilla, se inserta la palabra tal cual (comportamiento
 *    anterior).
 *
 *  Las plantillas usan «|» como marcador de posición del caret y se
 *  reindentan automáticamente respetando la indentación de la línea
 *  donde se invoca el snippet.
 */
public class AutoCompletadoPopUp {

    private static final Logger LOG = Logger.getLogger(AutoCompletadoPopUp.class.getName());

    /** Marcador de posición del caret dentro de un snippet. */
    private static final String CURSOR = "|";

    /** Indentación de un nivel anidado (4 espacios). */
    private static final String INDENT = "    ";

    private final RSyntaxTextArea editor;
    private final JPopupMenu popup;
    private final JList<String> list;
    private final JScrollPane scroll;
    private final List<String> palabras;

    /** @deprecated usar {@link LanguageKeywords#all()}. Conservado por compat. */
    @Deprecated
    public static final String[] palabrasReservadas = LanguageKeywords.all();

    /**
     * Plantillas para cada palabra que actúa como inicio de estructura.
     * El marcador {@link #CURSOR} indica dónde queda el caret tras la
     * expansión. Cada salto de línea es reindentado al insertar.
     */
    private static final Map<String, String> SNIPPETS = new HashMap<>();
    static {
        // Programa
        SNIPPETS.put("inicio",
                "inicio{\n" +
                INDENT + CURSOR + "\n" +
                "} fin");

        // Condicionales
        SNIPPETS.put("si",
                "si (" + CURSOR + ") {\n" +
                INDENT + "\n" +
                "}");
        SNIPPETS.put("sino",
                "sino {\n" +
                INDENT + CURSOR + "\n" +
                "}");

        // Switch
        SNIPPETS.put("suich",
                "suich (" + CURSOR + ") {\n" +
                INDENT + "caso 1:\n" +
                INDENT + INDENT + "\n" +
                INDENT + INDENT + "rompe;\n" +
                INDENT + "defecto:\n" +
                INDENT + INDENT + "\n" +
                INDENT + INDENT + "rompe;\n" +
                "}");
        SNIPPETS.put("caso",
                "caso " + CURSOR + ":\n" +
                INDENT + "\n" +
                INDENT + "rompe;");
        SNIPPETS.put("defecto",
                "defecto:\n" +
                INDENT + CURSOR + "\n" +
                INDENT + "rompe;");

        // Bucles
        SNIPPETS.put("isac",                                  // for
                "isac (i := 1; i <= " + CURSOR + "; ++>i) {\n" +
                INDENT + "\n" +
                "}");
        SNIPPETS.put("diego",                                 // while
                "diego (" + CURSOR + ") {\n" +
                INDENT + "\n" +
                "}");
        SNIPPETS.put("repite",                                // do-while
                "repite {\n" +
                INDENT + CURSOR + "\n" +
                "} hasta (cierto);");

        // E/S
        SNIPPETS.put("mostrar", "mostrar(" + CURSOR + ");");
        SNIPPETS.put("leer",    "leer(" + CURSOR + ");");

        // Declaraciones rápidas (con tipo predeterminado: entero, ya con
        // asignación inicial — el patrón más común en los .id existentes).
        // El caret queda en el nombre; tras escribirlo, el usuario navega
        // al valor con la flecha derecha o un click.
        SNIPPETS.put("var",   "var entero " + CURSOR + " := 0;");
        SNIPPETS.put("const", "const entero " + CURSOR + " := 0;");
    }

    public AutoCompletadoPopUp(RSyntaxTextArea editor, List<String> palabras) {
        this.editor = editor;
        this.palabras = palabras;

        popup = new JPopupMenu();
        popup.setFocusable(false);

        list = new JList<>();
        list.setFocusable(false);

        scroll = new JScrollPane(list);
        scroll.setFocusable(false);

        popup.add(scroll);

        editor.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (popup.isVisible()
                        && (e.getKeyCode() == KeyEvent.VK_ENTER
                            || e.getKeyCode() == KeyEvent.VK_TAB)) {
                    insertar();
                    popup.setVisible(false);
                    e.consume();
                    return;
                }

                if (popup.isVisible()) {

                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        moverSeleccion(1);
                        e.consume();
                        return;
                    }

                    if (e.getKeyCode() == KeyEvent.VK_UP) {
                        moverSeleccion(-1);
                        e.consume();
                        return;
                    }

                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    popup.setVisible(false);
                    return;
                }
                if (Character.isLetterOrDigit(e.getKeyChar())) {
                    mostrar();
                }
            }
        });

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    insertar();
                    popup.setVisible(false);
                }
            }
        });
        list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER
                        || e.getKeyCode() == KeyEvent.VK_TAB) {
                    insertar();
                    popup.setVisible(false);
                    e.consume();
                }
            }
        });
    }

    private void moverSeleccion(int dir) {
        int index = list.getSelectedIndex();
        int total = list.getModel().getSize();

        if (total == 0) {
            return;
        }

        index += dir;

        if (index < 0) {
            index = 0;
        }
        if (index >= total) {
            index = total - 1;
        }

        list.setSelectedIndex(index);
        list.ensureIndexIsVisible(index);
    }

    private void mostrar() {
        try {
            int pos = editor.getCaretPosition();
            String texto = editor.getDocument().getText(0, pos);

            int i = pos - 1;
            while (i >= 0 && Character.isLetterOrDigit(texto.charAt(i))) {
                i--;
            }

            String prefijo = texto.substring(i + 1);

            if (prefijo.isEmpty()) {
                popup.setVisible(false);
                return;
            }

            List<String> filtradas = new ArrayList<>();
            for (String p : palabras) {
                if (p.toLowerCase().startsWith(prefijo.toLowerCase())) {
                    filtradas.add(p);
                }
            }

            if (filtradas.isEmpty()) {
                popup.setVisible(false);
                return;
            }

            list.setListData(filtradas.toArray(new String[0]));
            list.setSelectedIndex(0);

            Rectangle r = editor.modelToView(pos);
            if (r != null) {
                popup.show(editor, r.x, r.y + r.height);
                editor.requestFocusInWindow();
            }

        } catch (BadLocationException ex) {
            LOG.log(Level.WARNING, "Error al mostrar el popup de autocompletado", ex);
        }
    }

    private void insertar() {
        String seleccionada = list.getSelectedValue();
        if (seleccionada == null) {
            return;
        }

        try {
            int pos = editor.getCaretPosition();
            String texto = editor.getText(0, pos);

            // Encontrar el inicio del prefijo escrito (palabra que se está completando)
            int i = pos - 1;
            while (i >= 0 && Character.isLetterOrDigit(texto.charAt(i))) {
                i--;
            }
            int inicioPrefijo = i + 1;

            String snippet = SNIPPETS.get(seleccionada);

            if (snippet == null) {
                // Sin plantilla → simple reemplazo del prefijo (comportamiento clásico)
                editor.getDocument().remove(inicioPrefijo, pos - inicioPrefijo);
                editor.getDocument().insertString(inicioPrefijo, seleccionada, null);
                return;
            }

            // Calcular indentación de la línea actual (espacios/tabs antes de la palabra)
            String indent = indentDeLineaActual(texto, inicioPrefijo);

            // Reindentar el snippet: cada salto de línea recibe la indentación base
            String expandido = aplicarIndent(snippet, indent);

            // Posición del marcador del caret (puede no haber → caret al final)
            int caretRelativo = expandido.indexOf(CURSOR);
            if (caretRelativo >= 0) {
                expandido = expandido.substring(0, caretRelativo)
                          + expandido.substring(caretRelativo + CURSOR.length());
            }

            editor.getDocument().remove(inicioPrefijo, pos - inicioPrefijo);
            editor.getDocument().insertString(inicioPrefijo, expandido, null);

            // Mover el caret
            int destino = (caretRelativo >= 0)
                    ? inicioPrefijo + caretRelativo
                    : inicioPrefijo + expandido.length();
            editor.setCaretPosition(destino);

        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Error al insertar snippet de autocompletado", ex);
        }
    }

    /**
     * Indentación (espacios o tabs) de la línea que contiene la posición
     * dada. Útil para reindentar las líneas adicionales del snippet.
     */
    private static String indentDeLineaActual(String texto, int posEnLinea) {
        // Ir hacia atrás hasta el último '\n' (o al inicio)
        int inicioLinea = posEnLinea;
        while (inicioLinea > 0 && texto.charAt(inicioLinea - 1) != '\n') {
            inicioLinea--;
        }
        // Tomar los caracteres en blanco al inicio de la línea
        int j = inicioLinea;
        while (j < texto.length()
                && (texto.charAt(j) == ' ' || texto.charAt(j) == '\t')) {
            j++;
        }
        return texto.substring(inicioLinea, j);
    }

    /**
     * Aplica la indentación base a cada línea del snippet excepto la
     * primera (la primera ya está en la columna correcta del editor).
     */
    private static String aplicarIndent(String snippet, String indent) {
        if (indent.isEmpty()) return snippet;
        StringBuilder out = new StringBuilder(snippet.length() + 32);
        String[] lineas = snippet.split("\n", -1);
        for (int i = 0; i < lineas.length; i++) {
            if (i > 0) {
                out.append('\n').append(indent);
            }
            out.append(lineas[i]);
        }
        return out.toString();
    }
}
