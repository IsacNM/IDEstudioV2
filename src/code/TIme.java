package code;

import java.util.Calendar;
import java.util.TimeZone;

public class TIme {
        public static String calcularHora() {
        Calendar calendario = Calendar.getInstance();
        calendario.setTimeZone(TimeZone.getTimeZone("America/Mazatlan"));

        int horas = calendario.get(Calendar.HOUR);
        int minutos = calendario.get(Calendar.MINUTE);
        int segundos = calendario.get(Calendar.SECOND);
        return String.format("Hora actual: %02d:%02d:%02d", horas, minutos, segundos);
    }
    
}
