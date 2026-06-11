package code;

import code.intermedio.Generador3D;
import code.intermedio.GeneradorCodigoIntermedio;
import code.lexico.AnalizadorLexico;
import code.semantico.AnalizadorSemantico;
import code.semantico.Repositorio;
import code.semantico.TokenTipo;
import code.sintactico.AnalizadorSintactico;
import compilerTools.ErrorLSSL;
import compilerTools.Token;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Driver CLI para probar el compilador desde la terminal.
 *
 * <pre>
 * Uso:
 *   java code.TestCompiler [--strict] &lt;archivo.id&gt;
 *
 *   --strict   Aborta antes de generar el .3d si hay cualquier error
 *              (léxico, sintáctico, semántico o de paréntesis).
 *              Por defecto se generan los .3d aunque haya errores
 *              menores, lo cual es útil para depuración pero puede
 *              producir salida parcial o ruido.
 *
 * Códigos de salida:
 *   0 - éxito sin errores
 *   1 - abortado por --strict ante errores
 *   2 - argumentos inválidos
 *   3 - excepción inesperada
 * </pre>
 */
public class TestCompiler {
    public static void main(String[] args) {
        boolean strict = false;
        String filePath = null;

        for (String a : args) {
            if ("--strict".equals(a)) {
                strict = true;
            } else if (filePath == null) {
                filePath = a;
            }
        }

        if (filePath == null) {
            System.out.println("Uso: java code.TestCompiler [--strict] <archivo.id>");
            System.out.println("  --strict   Aborta antes de generar el .3d si hay errores.");
            System.exit(2);
        }

        try {
            String content = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);

            // 1. Limpiar
            Repositorio.limpiar();

            // 2. Léxico
            int erroresLexicos = 0;
            try (StringReader sr = new StringReader(content)) {
                AnalizadorLexico lexer = new AnalizadorLexico(sr);
                try {
                    Token t;
                    while ((t = lexer.yylex()) != null) {
                        if (!TokenTipo.ERROR.equals(t.getLexicalComp())) {
                            Repositorio.listaTokens.add(t);
                        } else {
                            System.err.println("Error léxico en línea " + t.getLine()
                                    + ", columna " + t.getColumn() + ": '" + t.getLexeme() + "'");
                            erroresLexicos++;
                        }
                    }
                } finally {
                    try { lexer.yyclose(); } catch (Exception ignore) { /* sr ya cerrado */ }
                }
            }

            // 3. Sintáctico
            AnalizadorSintactico sint = new AnalizadorSintactico(Repositorio.listaTokens);
            sint.ejecutar();

            // En --strict, abortar antes del semántico si ya hay errores
            if (strict && (erroresLexicos > 0 || !Repositorio.listaErrores.isEmpty())) {
                imprimirYAbortar("antes de fase semántica",
                        erroresLexicos, Repositorio.listaErrores, 0);
            }

            int erroresAntesSemantico = Repositorio.listaErrores.size();

            // 4. Semántico
            AnalizadorSemantico.construirTablaSimbolos();
            AnalizadorSemantico.procesarAsignaciones();
            AnalizadorSemantico.validarSentenciasLeer();
            AnalizadorSemantico.validarSemantica();

            int erroresSemanticos = Repositorio.listaErrores.size() - erroresAntesSemantico;

            // En --strict, abortar también si la fase semántica añadió errores
            if (strict && (erroresLexicos > 0 || !Repositorio.listaErrores.isEmpty())) {
                imprimirYAbortar("antes de generar el .3d",
                        erroresLexicos, Repositorio.listaErrores, 0);
            }

            // Reporte resumen (modo no-strict): listar errores pero seguir
            if (!strict && (erroresLexicos > 0 || !Repositorio.listaErrores.isEmpty())) {
                System.out.println();
                System.out.println("[!] Errores presentes (compilación continúa por compatibilidad):");
                if (erroresLexicos > 0) {
                    System.out.println("    léxicos:    " + erroresLexicos);
                }
                if (Repositorio.listaErrores.size() > 0) {
                    System.out.println("    estructura: " + (Repositorio.listaErrores.size() - erroresSemanticos));
                    if (erroresSemanticos > 0) {
                        System.out.println("    semánticos: " + erroresSemanticos);
                    }
                }
                System.out.println("    Use --strict para abortar ante errores.");
            }

            // 5. Intermedio
            Generador3D gen3D = new Generador3D();
            gen3D.inicializar();
            gen3D.crearArchivo(filePath);
            GeneradorCodigoIntermedio generador = new GeneradorCodigoIntermedio(gen3D);
            generador.generar(TokenTipo.FIN);
            gen3D.cerrar();

            System.out.println("Código intermedio generado exitosamente para: " + filePath);

            String ruta3D = Generador3D.obtenerRuta3D(filePath);
            System.out.println("Contenido del archivo .3d:");
            System.out.println("-----------------------------------------");
            System.out.println(Files.readString(Paths.get(ruta3D), StandardCharsets.UTF_8));
            System.out.println("-----------------------------------------");

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(3);
        }
    }

    private static void imprimirYAbortar(String fase, int lexicos,
            java.util.List<ErrorLSSL> errores, int dummy) {
        System.err.println();
        System.err.println("[--strict] Abortando " + fase + ":");
        if (lexicos > 0) System.err.println("  errores léxicos: " + lexicos);
        for (ErrorLSSL e : errores) {
            System.err.println("  línea " + e.getLine()
                    + ", col " + e.getColumn() + ": " + e.toString());
        }
        System.exit(1);
    }
}
