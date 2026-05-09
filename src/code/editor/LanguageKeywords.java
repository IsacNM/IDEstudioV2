package code.editor;

/**
 * Diccionario centralizado de palabras del lenguaje IDEstudio.
 * Antes vivía duplicado en {@link AutoCompletadoPopUp},
 * {@link IDEstudioTokenMaker} y {@code view.JConfig}.
 */
public final class LanguageKeywords {

    /** Palabras reservadas (control de flujo, IO, declaración, booleanos). */
    public static final String[] RESERVED_WORDS = {
        "inicio", "fin",
        "si", "sino",
        "suich", "caso", "rompe", "defecto",
        "isac", "diego", "repite", "hasta",
        "mostrar", "leer",
        "var", "const",
        "cierto", "falso"
    };

    /** Tipos de dato. */
    public static final String[] DATA_TYPES = {
        "entero", "decimal", "texto", "car", "logico", "corto"
    };

    /** Reservadas + tipos: la unión que muestra el autocompletado. */
    public static String[] all() {
        String[] out = new String[RESERVED_WORDS.length + DATA_TYPES.length];
        System.arraycopy(RESERVED_WORDS, 0, out, 0, RESERVED_WORDS.length);
        System.arraycopy(DATA_TYPES, 0, out, RESERVED_WORDS.length, DATA_TYPES.length);
        return out;
    }

    private LanguageKeywords() {}
}
