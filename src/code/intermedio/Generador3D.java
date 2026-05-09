package code.intermedio;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Clase adaptada del docente para generar el archivo de código intermedio .3d.
 * Gestiona contadores de temporales (T1, T2...) y etiquetas (L1, L2...).
 */
public class Generador3D {

    private static final Logger LOG = Logger.getLogger(Generador3D.class.getName());

    private int contadorTemp = 0;
    private int contadorEtiq = 0;
    private PrintStream archivo3D = System.out;

    /** Abre (o crea) el archivo .3d reemplazando la extensión .id */
    public void crearArchivo(String rutaArchivoFuente) {
        String ruta3D = rutaArchivoFuente.replaceAll("(?i)\\.id$", ".3d");
        if (ruta3D.equals(rutaArchivoFuente)) {
            ruta3D = rutaArchivoFuente + ".3d";
        }
        try {
            archivo3D = new PrintStream(new FileOutputStream(ruta3D), false, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "No se pudo crear el archivo .3d en " + ruta3D, e);
        }
    }

    /**
     * Genera una instrucción de código intermedio y la escribe en el archivo.
     * Operaciones soportadas: ASIGNACION, LEER, MOSTRAR, :=,
     *   +, -, *, /, %, &+, IF, GOTO, LABEL
     */
    public void gc(String operacion, String operando1, String operando2, String resultado) {
        switch (operacion) {
            case "ASIGNACION":
                archivo3D.println(resultado + ":=" + operando1 + ";");
                break;
            case "LEER":
                archivo3D.println("LEER " + resultado + ";");
                break;
            case "MOSTRAR":
                archivo3D.println("MOSTRAR " + resultado + ";");
                break;
            case "+":
            case "-":
            case "*":
            case "/":
            case "%":
            case "&+":
                archivo3D.println(resultado + ":=" + operando1 + operacion + operando2 + ";");
                break;
            case ":=":
                archivo3D.println(resultado + ":=" + operando1 + ";");
                break;
            case "IF":
                archivo3D.println("if " + operando1 + " then GOTO " + resultado + ";");
                break;
            case "GOTO":
                archivo3D.println("GOTO " + resultado + ";");
                break;
            case "LABEL":
                archivo3D.println(resultado + ":");
                break;
            default:
                System.err.println("[Generador3D] Operación desconocida: " + operacion);
        }
    }

    /** Genera un nuevo nombre de variable temporal (T1, T2, ...) */
    public String nuevaTemp() {
        contadorTemp++;
        return "T" + contadorTemp;
    }

    /** Genera una nueva etiqueta de salto (L1, L2, ...) */
    public String nuevaEtiq() {
        contadorEtiq++;
        return "L" + contadorEtiq;
    }

    /** Reinicia los contadores (llamar antes de cada compilación) */
    public void inicializar() {
        contadorTemp = 0;
        contadorEtiq = 0;
    }

    /** Cierra el archivo de salida */
    public void cerrar() {
        if (archivo3D != System.out) {
            archivo3D.close();
        }
    }

    /** Devuelve la ruta donde se genera el .3d (para mostrar al usuario) */
    public static String obtenerRuta3D(String rutaFuente) {
        String ruta = rutaFuente.replaceAll("(?i)\\.id$", ".3d");
        return ruta.equals(rutaFuente) ? rutaFuente + ".3d" : ruta;
    }
}
