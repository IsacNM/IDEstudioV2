package code;

import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

public class AutoCompletadoPopUp {

    private final RSyntaxTextArea editor;
    private final JPopupMenu popup;
    private final JList<String> list;
    private final JScrollPane scroll;
    private final List<String> palabras;

    public static String[] palabrasReservadas = {
        "inicio", "fin",
        "si", "sino",
        "elige", "caso", "romper", "defecto",
        "isac", "Diego", "repite", "hasta",
        "mostrar", "leer",
        "var", "const",
        "entero", "decimal", "texto", "car", "logico", "corto",
        "cierto", "falso"
    };

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
                if (e.getKeyCode() == KeyEvent.VK_ENTER && popup.isVisible()) {
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
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
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

            // Si no hay texto, ocultamos el popup
            if (prefijo.isEmpty()) {
                popup.setVisible(false);
                return;
            }

            // --- LÓGICA DE FILTRADO ---
            List<String> filtradas = new ArrayList<>();
            for (String p : palabras) {
                // Filtra ignorando mayúsculas/minúsculas
                if (p.toLowerCase().startsWith(prefijo.toLowerCase())) {
                    filtradas.add(p);
                }
            }

            if (filtradas.isEmpty()) {
                popup.setVisible(false);
                return;
            }

            // Actualizamos la lista con las coincidencias
            list.setListData(filtradas.toArray(new String[0]));
            list.setSelectedIndex(0);

            // Posicionamiento del Popup debajo del cursor
            Rectangle r = editor.modelToView(pos);
            if (r != null) {
                popup.show(editor, r.x, r.y + r.height);
                editor.requestFocusInWindow(); // Mantiene el foco en el editor para seguir escribiendo
            }

        } catch (BadLocationException ex) {
            ex.printStackTrace();
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

            int i = pos - 1;
            while (i >= 0 && Character.isLetterOrDigit(texto.charAt(i))) {
                i--;
            }

            int inicio = i + 1;

            // Reemplazar palabra por la sugerencia
            editor.getDocument().remove(inicio, pos - inicio);
            editor.getDocument().insertString(inicio, seleccionada, null);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
