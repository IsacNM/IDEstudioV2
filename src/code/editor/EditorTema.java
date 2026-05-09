package code.editor;

import java.awt.Color;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.TokenTypes;

/**
 * Aplica la configuración persistida (fuente, tamaño, estilo y colores
 * por tipo de texto) a un {@link RSyntaxTextArea}.
 *
 * Mapeo de las claves del diálogo {@code JConfig} a los token types de
 * RSyntaxTextArea:
 * <pre>
 *   "Palabras reservadas" → RESERVED_WORD, RESERVED_WORD_2, DATA_TYPE
 *   "Comentarios"         → COMMENT_EOL, COMMENT_MULTILINE, COMMENT_DOCUMENTATION
 *   "Cadenas"             → LITERAL_STRING_DOUBLE_QUOTE, LITERAL_CHAR, LITERAL_BACKQUOTE
 *   "Identificadores"     → IDENTIFIER
 *   "Números enteros"     → LITERAL_NUMBER_DECIMAL_INT, LITERAL_NUMBER_HEXADECIMAL
 *   "Números flotantes"   → LITERAL_NUMBER_FLOAT
 *   "Signos de puntuación"→ SEPARATOR, OPERATOR
 *   "General"             → foreground default del editor
 * </pre>
 *
 * El archivo se persiste por usuario en {@link ArchivoPropiedades}.
 */
public final class EditorTema {

    private static final Logger LOG = Logger.getLogger(EditorTema.class.getName());

    /** Fuente por defecto si no hay configuración previa. */
    public static final Font FUENTE_DEFAULT =
            new Font(Font.MONOSPACED, Font.PLAIN, 14);

    /**
     * Colores por defecto por categoría. Se usan tanto en la aplicación del
     * tema al editor como en el preview de {@code JConfig}.
     */
    public static final Map<String, Color> COLORES_DEFAULT;
    static {
        Map<String, Color> m = new LinkedHashMap<>();
        m.put("General",              Color.BLACK);
        m.put("Palabras reservadas",  new Color(127, 0, 85));
        m.put("Comentarios",          new Color(63, 127, 95));
        m.put("Cadenas",              Color.BLUE);
        m.put("Identificadores",      Color.BLACK);
        m.put("Números enteros",      Color.RED);
        m.put("Números flotantes",    Color.RED.darker());
        m.put("Signos de puntuación", Color.GRAY);
        COLORES_DEFAULT = java.util.Collections.unmodifiableMap(m);
    }

    /** Llave de propiedades para la categoría dada (formato `color_<cat>`). */
    public static String llaveColor(String categoria) {
        return "color_" + categoria.replace(" ", "_");
    }

    private EditorTema() {}

    /** Aplica el tema persistido al editor. Si no hay configuración, deja defaults. */
    public static void aplicar(RSyntaxTextArea editor) {
        Properties prop = new ArchivoPropiedades().LeerPropiedades();
        aplicar(editor, prop);
    }

    /** Aplica un tema concreto (útil tras un guardado, sin re-leer disco). */
    public static void aplicar(RSyntaxTextArea editor, Properties prop) {
        if (editor == null || prop == null) return;

        // ---- Fuente ----
        String familia = prop.getProperty("fuente", FUENTE_DEFAULT.getFamily());
        int estilo;
        try {
            estilo = Integer.parseInt(prop.getProperty("estilo",
                    String.valueOf(FUENTE_DEFAULT.getStyle())));
        } catch (NumberFormatException e) {
            estilo = FUENTE_DEFAULT.getStyle();
        }
        int tamano;
        try {
            // La clave histórica usa "tamaño" con ñ; aceptamos ambas por compat.
            String t = prop.getProperty("tamaño", prop.getProperty("tamano",
                    String.valueOf(FUENTE_DEFAULT.getSize())));
            tamano = Integer.parseInt(t);
        } catch (NumberFormatException e) {
            tamano = FUENTE_DEFAULT.getSize();
        }
        Font fuente = new Font(familia, estilo, tamano);
        editor.setFont(fuente);

        // ---- Color general (foreground) ----
        Color general = leerColor(prop, "General");
        editor.setForeground(general);

        // ---- Colores por tipo de token ----
        SyntaxScheme scheme = editor.getSyntaxScheme();

        Color cReservadas  = leerColor(prop, "Palabras reservadas");
        Color cComentarios = leerColor(prop, "Comentarios");
        Color cCadenas     = leerColor(prop, "Cadenas");
        Color cIdent       = leerColor(prop, "Identificadores");
        Color cEnteros     = leerColor(prop, "Números enteros");
        Color cFlotantes   = leerColor(prop, "Números flotantes");
        Color cPunt        = leerColor(prop, "Signos de puntuación");

        Font font = fuente;
        Font fontBold = fuente.deriveFont(Font.BOLD);

        setStyle(scheme, TokenTypes.RESERVED_WORD,            fontBold, cReservadas);
        setStyle(scheme, TokenTypes.RESERVED_WORD_2,          fontBold, cReservadas);
        setStyle(scheme, TokenTypes.DATA_TYPE,                fontBold, cReservadas);

        setStyle(scheme, TokenTypes.COMMENT_EOL,              font.deriveFont(Font.ITALIC), cComentarios);
        setStyle(scheme, TokenTypes.COMMENT_MULTILINE,        font.deriveFont(Font.ITALIC), cComentarios);
        setStyle(scheme, TokenTypes.COMMENT_DOCUMENTATION,    font.deriveFont(Font.ITALIC), cComentarios);

        setStyle(scheme, TokenTypes.LITERAL_STRING_DOUBLE_QUOTE, font, cCadenas);
        setStyle(scheme, TokenTypes.LITERAL_CHAR,                font, cCadenas);
        setStyle(scheme, TokenTypes.LITERAL_BACKQUOTE,           font, cCadenas);

        setStyle(scheme, TokenTypes.IDENTIFIER,                  font, cIdent);
        setStyle(scheme, TokenTypes.VARIABLE,                    font, cIdent);

        setStyle(scheme, TokenTypes.LITERAL_NUMBER_DECIMAL_INT,  font, cEnteros);
        setStyle(scheme, TokenTypes.LITERAL_NUMBER_HEXADECIMAL,  font, cEnteros);

        setStyle(scheme, TokenTypes.LITERAL_NUMBER_FLOAT,        font, cFlotantes);

        setStyle(scheme, TokenTypes.SEPARATOR,                   font, cPunt);
        setStyle(scheme, TokenTypes.OPERATOR,                    font, cPunt);

        editor.setSyntaxScheme(scheme);
        editor.repaint();
    }

    private static void setStyle(SyntaxScheme scheme, int tokenType, Font font, Color fg) {
        Style s = scheme.getStyle(tokenType);
        if (s == null) {
            s = new Style(fg, null, font);
        } else {
            s.foreground = fg;
            s.font = font;
        }
        scheme.setStyle(tokenType, s);
    }

    /** Lee el color para {@code categoria} desde props, cayendo a {@link #COLORES_DEFAULT}. */
    public static Color leerColor(Properties prop, String categoria) {
        Color fallback = COLORES_DEFAULT.getOrDefault(categoria, Color.BLACK);
        if (prop == null) return fallback;
        String hex = prop.getProperty(llaveColor(categoria));
        if (hex == null || hex.isBlank()) return fallback;
        try {
            return Color.decode(hex);
        } catch (NumberFormatException e) {
            LOG.log(Level.FINE, "Valor inválido para " + categoria + ": '" + hex + "'");
            return fallback;
        }
    }

    /** Convierte un color a su representación hex `#RRGGBB`. */
    public static String aHex(Color c) {
        return String.format("#%06X", 0xFFFFFF & c.getRGB());
    }
}
