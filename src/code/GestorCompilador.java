package code;

import code.editor.File;
import static code.editor.File.errorTab;
import code.intermedio.Generador3D;
import code.intermedio.GeneradorCodigoIntermedio;
import code.semantico.AnalizadorSemantico;
import code.semantico.FiltroParentesis;
import code.semantico.Repositorio;
import code.semantico.Simbolo;
import code.semantico.TokenTipo;
import code.sintactico.AnalizadorSintactico;
import compilerTools.ErrorLSSL;
import compilerTools.Token;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

public class GestorCompilador {

    private static final Logger LOG = Logger.getLogger(GestorCompilador.class.getName());

    private static final String SEPARADOR =
            "════════════════════════════════════════════════════════════════════════════════\n";
    private static final String DIVISOR =
            "───────────────────────────────────────────────────\n";

    public static void ejecutarCompilacion(JTabbedPane jTabbed, DefaultTableModel modeloTabla,
                                           JTextArea consola, JTable tablaSimbolos) {
        RSyntaxTextArea textArea = File.getTextAreaActual(jTabbed);
        if (textArea == null) return;

        Repositorio.limpiar();
        textArea.getHighlighter().removeAllHighlights();
        consola.setText("");

        // 1. Léxico (Compilar.listaTokens conserva ERROR para el reporte)
        Compilar.analizar(textArea, modeloTabla);
        for (Token t : Compilar.listaTokens) {
            if (!TokenTipo.ERROR.equals(t.getLexicalComp())) Repositorio.listaTokens.add(t);
        }

        // 2. Sintáctico
        new AnalizadorSintactico(Repositorio.listaTokens).ejecutar();

        // 3. Semántico
        AnalizadorSemantico.construirTablaSimbolos();
        AnalizadorSemantico.procesarAsignaciones();
        AnalizadorSemantico.validarSentenciasLeer();
        AnalizadorSemantico.validarSemantica();

        // 4. UI: tabla y resaltado de líneas con error
        mostrarTablaSimbolos(tablaSimbolos);
        for (ErrorLSSL error : Repositorio.listaErrores) errorTab(jTabbed, error.getLine());

        // 5. Reporte en consola
        mostrarErroresEnConsola(consola);

        // 6. Generación 3D si todo está limpio
        if (compilacionLimpia()) generarIntermedio(jTabbed, consola);
    }

    // ── Helpers de estado ────────────────────────────────────────────────

    private static boolean compilacionLimpia() {
        return Repositorio.listaErrores.isEmpty()
            && !Compilar.hayErroresLexicos()
            && FiltroParentesis.erroresEncontrados.isEmpty();
    }

    // ── Generación de código intermedio ──────────────────────────────────

    private static void generarIntermedio(JTabbedPane jTabbed, JTextArea consola) {
        String ruta = File.getRutaArchivo(jTabbed, jTabbed.getSelectedIndex());
        if (ruta == null) {
            consola.append("\n[Info]: Guarda el archivo (.id) primero para generar el código intermedio (.3d)\n");
            return;
        }

        try {
            Generador3D gen3D = new Generador3D();
            gen3D.inicializar();
            gen3D.crearArchivo(ruta);
            new GeneradorCodigoIntermedio(gen3D).generar(TokenTipo.FIN);
            gen3D.cerrar();

            consola.append("\n" + SEPARADOR);
            consola.append("                      ✓ CÓDIGO INTERMEDIO GENERADO\n");
            consola.append(SEPARADOR);
            consola.append("Archivo .3d: " + Generador3D.obtenerRuta3D(ruta) + "\n");
        } catch (Exception e) {
            consola.append("\n[Error al generar código intermedio]: " + e.getMessage() + "\n");
            LOG.log(Level.SEVERE, "Error al generar código intermedio", e);
        }
    }

    // ── Reporte de errores en consola ────────────────────────────────────

    private static void mostrarErroresEnConsola(JTextArea consola) {
        boolean lex = Compilar.hayErroresLexicos();
        boolean paren = !FiltroParentesis.erroresEncontrados.isEmpty();

        if (Repositorio.listaErrores.isEmpty() && !lex && !paren) {
            consola.append(SEPARADOR);
            consola.append("                               ✓ COMPILACIÓN EXITOSA\n");
            consola.append(SEPARADOR);
            consola.append("No se encontraron errores.\n");
            return;
        }

        consola.append(SEPARADOR);
        consola.append("                   ⚠ ERRORES DETECTADOS EN LA COMPILACIÓN\n");
        consola.append(SEPARADOR + "\n");

        if (lex) reportarErroresLexicos(consola);
        if (!Repositorio.listaErrores.isEmpty()) reportarErroresEstructuraLogica(consola);
        if (paren) reportarErroresParentesis(consola);

        consola.append("\nCompilación finalizada con errores.\n");
    }

    private static void reportarErroresLexicos(JTextArea consola) {
        consola.append("--- ERRORES LÉXICOS ---\n\n");
        for (Token t : Compilar.listaTokens) {
            if (!TokenTipo.ERROR.equals(t.getLexicalComp())) continue;
            consola.append(String.format("Línea %d, Columna %d%n", t.getLine(), t.getColumn()));
            consola.append("  ➤ Error léxico: Símbolo no reconocido '" + t.getLexeme() + "'\n");
            consola.append(DIVISOR);
        }
    }

    private static void reportarErroresEstructuraLogica(JTextArea consola) {
        IdentityHashMap<ErrorLSSL, Boolean> esParen = new IdentityHashMap<>();
        for (ErrorLSSL e : FiltroParentesis.erroresEncontrados) esParen.put(e, true);

        HashSet<String> vistos = new HashSet<>();
        int contador = 1;
        boolean encabezado = false;

        for (ErrorLSSL error : Repositorio.listaErrores) {
            if (esParen.containsKey(error)) continue;

            String llave = error.getLine() + "|" + error.toString();
            if (vistos.contains(llave)) continue;

            if (!encabezado) {
                consola.append("--- ERRORES DE ESTRUCTURA Y LÓGICA ---\n\n");
                encabezado = true;
            }
            consola.append(String.format("[Error %d]%n", contador++));
            consola.append("  Línea: " + error.getLine() + ", Columna: " + error.getColumn() + "\n");
            consola.append("  Detalle: " + error.toString() + "\n");
            consola.append(DIVISOR);
            vistos.add(llave);
        }
    }

    private static void reportarErroresParentesis(JTextArea consola) {
        consola.append("--- ERRORES DE PARÉNTESIS ---\n\n");
        for (ErrorLSSL error : FiltroParentesis.erroresEncontrados) {
            consola.append("  Línea: " + error.getLine() + ", Columna: " + error.getColumn() + "\n");
            consola.append("  ➤ " + error.toString() + "\n");
            consola.append(DIVISOR);
        }
    }

    // ── Tabla de símbolos ────────────────────────────────────────────────

    private static void mostrarTablaSimbolos(JTable tabla) {
        DefaultTableModel modelo = (DefaultTableModel) tabla.getModel();
        modelo.setRowCount(0);
        for (Simbolo s : Repositorio.tablaSimbolos.values()) {
            modelo.addRow(new Object[]{
                s.getIdent(), s.getTipoDato(), s.getValor(), s.getVarConstParam()
            });
        }
    }
}
