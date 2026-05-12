package code.editor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Archivo de configuración persistido por usuario en
 * {@code $HOME/.idestudio/config.properties} (no portable a propósito —
 * cada usuario tiene su propia configuración independiente del directorio
 * desde el que se lance la aplicación).
 *
 * Si el directorio no existe se crea automáticamente; si el archivo no
 * existe, {@link #LeerPropiedades()} devuelve un {@link Properties} vacío
 * (sin error).
 */
public class ArchivoPropiedades {

    private static final Logger LOG = Logger.getLogger(ArchivoPropiedades.class.getName());

    /** Carpeta del usuario donde vive el .properties. */
    public static final Path DIR_CONFIG = Paths.get(
            System.getProperty("user.home"), ".idestudio");

    /** Ruta absoluta del archivo de propiedades por usuario. */
    public static final Path RUTA_CONFIG = DIR_CONFIG.resolve("config.properties");

    private final String RUTA = RUTA_CONFIG.toString();

    public Properties LeerPropiedades() {
        Properties config = new Properties();
        if (!Files.exists(RUTA_CONFIG)) {
            return config;   // archivo aún no existe → defaults
        }
        try (InputStream in = new FileInputStream(RUTA)) {
            config.load(in);
            return config;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "No se pudo leer " + RUTA, e);
            return new Properties();
        }
    }

    public boolean EscribirPropiedades(Properties propiedades) {
        try {
            // Crea el directorio ~/.idestudio si aún no existe
            if (!Files.exists(DIR_CONFIG)) {
                Files.createDirectories(DIR_CONFIG);
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "No se pudo crear el directorio " + DIR_CONFIG, e);
            return false;
        }
        try (OutputStream out = new FileOutputStream(RUTA)) {
            propiedades.store(out, "Configuración de IDEstudio (por usuario)");
            return true;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "No se pudo escribir " + RUTA, e);
            return false;
        }
    }

    public String LeerPropiedad(String llave) {
        Properties config = LeerPropiedades();
        return config.getProperty(llave);
    }

    public boolean EscribirPropiedad(String llave, String valor) {
        Properties config = LeerPropiedades();
        config.setProperty(llave, valor);
        return EscribirPropiedades(config);
    }
}
