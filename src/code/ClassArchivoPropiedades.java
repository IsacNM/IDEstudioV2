package code;

import java.io.*;
import java.util.Properties;

public class ClassArchivoPropiedades {

    private final String RUTA = System.getProperty("user.dir") + "/config.properties";

    public Properties LeerPropiedades() {
        Properties config = new Properties();
        try (InputStream configInput = new FileInputStream(RUTA)) {
            config.load(configInput);
            return config;
        } catch (IOException e) {
            // Si no existe el archivo, devolvemos un Properties vacío
            return new Properties();
        }
    }

    public boolean EscribirPropiedades(Properties propiedades) {
        try (OutputStream configOutput = new FileOutputStream(RUTA)) {
            propiedades.store(configOutput, "<------ Archivo de Configuraciones ------>");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
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
