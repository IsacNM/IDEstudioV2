package code.semantico;

import code.semantico.Simbolo;
import compilerTools.Token;
import compilerTools.ErrorLSSL;
import compilerTools.Production;
import java.util.ArrayList;
import java.util.HashMap;
public class Repositorio {
    
    public static ArrayList<Token> listaTokens = new ArrayList<>();
    public static ArrayList<ErrorLSSL> listaErrores = new ArrayList<>();
    public static HashMap<String, Simbolo> tablaSimbolos = new HashMap<>();
    public static ArrayList<Production> idDeclaraciones = new ArrayList<>();
    public static ArrayList<String> erroresSemanticos = new ArrayList<>();

    public static void limpiar() {
        listaTokens.clear();
        listaErrores.clear();
        tablaSimbolos.clear(); 
        idDeclaraciones.clear();
        erroresSemanticos.clear(); // ← AGREGAR

    }
    
}

