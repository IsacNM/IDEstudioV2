package code.editor;

import java.util.Calendar;
import java.util.TimeZone;

public class Time {

    /** Bandera global de idioma para componentes del editor que deben
     *  re-renderizar texto periódicamente (reloj, etiqueta de posición).
     *  Se actualiza desde {@code IDE#actualizarIdioma(String)}. */
    public static volatile boolean idiomaEs = true;

    public static String calcularHora() {
        Calendar calendario = Calendar.getInstance();
        calendario.setTimeZone(TimeZone.getTimeZone("America/Mazatlan"));

        int horas    = calendario.get(Calendar.HOUR_OF_DAY);
        int minutos  = calendario.get(Calendar.MINUTE);
        int segundos = calendario.get(Calendar.SECOND);
        String prefijo = idiomaEs ? "Hora actual" : "Current time";
        return String.format("%s: %02d:%02d:%02d", prefijo, horas, minutos, segundos);
    }
}
