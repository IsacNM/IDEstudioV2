package code.editor;

import javax.swing.text.Segment;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;
import org.fife.ui.rsyntaxtextarea.TokenTypes;

/**
 * Tokenizador específico del lenguaje IDEstudio.
 *
 * Reconoce los lexemas reales del lenguaje:
 *   - Palabras reservadas: inicio, fin, si, sino, suich, caso, defecto,
 *     rompe, isac, diego, repite, hasta, mostrar, leer, var, const,
 *     cierto, falso
 *   - Tipos:               entero, decimal, texto, car, logico, corto
 *                          (se mapean a DATA_TYPE)
 *   - Comentarios:         `-# ...` (línea), `-#[ ... ]#` (bloque, multilínea)
 *   - Cadenas:             "..."  con escapes \n \t \\ \"  (no multilínea)
 *   - Caracteres:          '...'  con escapes (no multilínea)
 *   - Números:             enteros (123) y flotantes (3.14)
 *   - Operadores compuestos: `++>`, `--<`, `**`, `//`, `%%`, `>=`, `<=`,
 *                            `::`, `:!`, `:=`, `&+`, `-y-`, `-o-`, `-n-`
 *   - Operadores simples:  + - * / % > < : . , ; ( ) { } [ ]
 *   - Identificadores:     [_letras][_letras_dígitos]* (Unicode)
 *
 * Para usarlo basta con:
 * <pre>
 *   IDEstudioTokenMaker.registrar();
 *   editor.setSyntaxEditingStyle(IDEstudioTokenMaker.SYNTAX_STYLE);
 * </pre>
 */
public class IDEstudioTokenMaker extends AbstractTokenMaker {

    /** Identificador del estilo de sintaxis para
     *  {@code RSyntaxTextArea#setSyntaxEditingStyle}. */
    public static final String SYNTAX_STYLE = "text/idestudio";

    /** Registra el TokenMaker en la factoría por defecto. Idempotente. */
    public static void registrar() {
        org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory atmf =
                (org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory)
                org.fife.ui.rsyntaxtextarea.TokenMakerFactory.getDefaultInstance();
        atmf.putMapping(SYNTAX_STYLE, IDEstudioTokenMaker.class.getName());
    }

    // ----------- Keywords del lenguaje -----------

    @Override
    public TokenMap getWordsToHighlight() {
        TokenMap map = new TokenMap();
        for (String kw : LanguageKeywords.RESERVED_WORDS) map.put(kw, TokenTypes.RESERVED_WORD);
        for (String t  : LanguageKeywords.DATA_TYPES)     map.put(t,  TokenTypes.DATA_TYPE);
        return map;
    }

    @Override
    public String[] getLineCommentStartAndEnd(int languageIndex) {
        return new String[]{ "-#", null };
    }

    @Override
    public boolean getMarkOccurrencesOfTokenType(int type) {
        return type == TokenTypes.IDENTIFIER || type == TokenTypes.FUNCTION;
    }

    // ----------- Tokenización -----------

    @Override
    public Token getTokenList(Segment text, int initialTokenType, int startOffset) {
        resetTokenList();

        char[] arr = text.array;
        int offset = text.offset;
        int end    = offset + text.count;
        int docOff = startOffset - offset;

        int i = offset;

        // Si la línea anterior dejó un comentario de bloque abierto,
        // continuar consumiendo hasta `]#` (o fin de línea).
        if (initialTokenType == TokenTypes.COMMENT_MULTILINE) {
            int cierre = buscarCierreBloque(arr, i, end);
            if (cierre < 0) {
                addToken(arr, i, end - 1, TokenTypes.COMMENT_MULTILINE, docOff + i);
                addNullToken();
                return firstToken;
            }
            addToken(arr, i, cierre - 1, TokenTypes.COMMENT_MULTILINE, docOff + i);
            i = cierre;
        }

        while (i < end) {
            char c = arr[i];

            // Whitespace
            if (Character.isWhitespace(c)) {
                int j = i;
                while (j < end && Character.isWhitespace(arr[j])) j++;
                addToken(arr, i, j - 1, TokenTypes.WHITESPACE, docOff + i);
                i = j;
                continue;
            }

            // Comentarios `-#...` y `-#[...]#`
            if (c == '-' && i + 1 < end && arr[i + 1] == '#') {
                if (i + 2 < end && arr[i + 2] == '[') {
                    // Bloque (puede cruzar líneas)
                    int cierre = buscarCierreBloque(arr, i + 3, end);
                    if (cierre < 0) {
                        addToken(arr, i, end - 1, TokenTypes.COMMENT_MULTILINE, docOff + i);
                        i = end;
                    } else {
                        addToken(arr, i, cierre - 1, TokenTypes.COMMENT_MULTILINE, docOff + i);
                        i = cierre;
                    }
                } else {
                    // Línea: hasta fin del segment
                    addToken(arr, i, end - 1, TokenTypes.COMMENT_EOL, docOff + i);
                    i = end;
                }
                continue;
            }

            // Cadena "..."  con escapes
            if (c == '"') {
                int j = i + 1;
                while (j < end && arr[j] != '"') {
                    if (arr[j] == '\\' && j + 1 < end) j += 2;
                    else j++;
                }
                int fin = (j < end ? j : end - 1);
                addToken(arr, i, fin, TokenTypes.LITERAL_STRING_DOUBLE_QUOTE, docOff + i);
                i = fin + 1;
                continue;
            }

            // Carácter '.'
            if (c == '\'') {
                int j = i + 1;
                while (j < end && arr[j] != '\'') {
                    if (arr[j] == '\\' && j + 1 < end) j += 2;
                    else j++;
                }
                int fin = (j < end ? j : end - 1);
                addToken(arr, i, fin, TokenTypes.LITERAL_CHAR, docOff + i);
                i = fin + 1;
                continue;
            }

            // Números
            if (Character.isDigit(c)) {
                int j = i;
                boolean flot = false;
                while (j < end && Character.isDigit(arr[j])) j++;
                if (j < end && arr[j] == '.' && j + 1 < end && Character.isDigit(arr[j + 1])) {
                    flot = true;
                    j++;
                    while (j < end && Character.isDigit(arr[j])) j++;
                }
                addToken(arr, i, j - 1,
                        flot ? TokenTypes.LITERAL_NUMBER_FLOAT
                             : TokenTypes.LITERAL_NUMBER_DECIMAL_INT,
                        docOff + i);
                i = j;
                continue;
            }

            // Operadores compuestos del lenguaje (-y-, -o-, -n-, &+, :=, ::, :!, etc.)
            int operLen = matchOperadorCompuesto(arr, i, end);
            if (operLen > 0) {
                addToken(arr, i, i + operLen - 1, TokenTypes.OPERATOR, docOff + i);
                i += operLen;
                continue;
            }

            // Identificadores y keywords
            if (esInicioIdent(c)) {
                int j = i + 1;
                while (j < end && esContIdent(arr[j])) j++;

                // Buscar en el TokenMap (keywords)
                int tipo = wordsToHighlight.get(arr, i, j - 1);
                if (tipo == -1) tipo = TokenTypes.IDENTIFIER;
                addToken(arr, i, j - 1, tipo, docOff + i);
                i = j;
                continue;
            }

            // Operadores simples y separadores
            int simpleType = clasificarOperadorSimple(c);
            if (simpleType != TokenTypes.NULL) {
                addToken(arr, i, i, simpleType, docOff + i);
                i++;
                continue;
            }

            // Carácter desconocido -> ERROR_IDENTIFIER (lo coloreará rojo
            // por defecto, similar a lexer ERROR)
            addToken(arr, i, i, TokenTypes.ERROR_IDENTIFIER, docOff + i);
            i++;
        }

        addNullToken();
        return firstToken;
    }

    @Override
    public boolean isIdentifierChar(int languageIndex, char ch) {
        return esContIdent(ch);
    }

    // ----------- Helpers -----------

    /** Devuelve el índice JUSTO DESPUÉS de "]#" (o -1 si no aparece). */
    private static int buscarCierreBloque(char[] arr, int from, int end) {
        for (int k = from; k + 1 < end; k++) {
            if (arr[k] == ']' && arr[k + 1] == '#') {
                return k + 2;
            }
        }
        return -1;
    }

    /** Reconoce operadores compuestos (devuelve longitud, o 0 si no matchea). */
    private static int matchOperadorCompuesto(char[] arr, int i, int end) {
        if (matches(arr, i, end, "++>")) return 3;
        if (matches(arr, i, end, "--<")) return 3;
        if (matches(arr, i, end, "-y-")) return 3;
        if (matches(arr, i, end, "-o-")) return 3;
        if (matches(arr, i, end, "-n-")) return 3;
        if (matches(arr, i, end, ">="))  return 2;
        if (matches(arr, i, end, "<="))  return 2;
        if (matches(arr, i, end, "::"))  return 2;
        if (matches(arr, i, end, ":!"))  return 2;
        if (matches(arr, i, end, ":="))  return 2;
        if (matches(arr, i, end, "&+"))  return 2;
        if (matches(arr, i, end, "**"))  return 2;
        if (matches(arr, i, end, "//"))  return 2;
        if (matches(arr, i, end, "%%"))  return 2;
        return 0;
    }

    private static boolean matches(char[] arr, int i, int end, String s) {
        if (i + s.length() > end) return false;
        for (int k = 0; k < s.length(); k++) {
            if (arr[i + k] != s.charAt(k)) return false;
        }
        return true;
    }

    /** Operadores y separadores de un solo carácter. */
    private static int clasificarOperadorSimple(char c) {
        switch (c) {
            case '+': case '-': case '*': case '/': case '%':
            case '>': case '<': case ':':
                return TokenTypes.OPERATOR;
            case ',': case ';': case '(': case ')':
            case '{': case '}': case '[': case ']':
            case '.':
                return TokenTypes.SEPARATOR;
            default:
                return TokenTypes.NULL;
        }
    }

    private static boolean esInicioIdent(char c) {
        return c == '_' || Character.isLetter(c);
    }

    private static boolean esContIdent(char c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }
}
