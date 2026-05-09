package code.semantico;

import compilerTools.ErrorLSSL;
import compilerTools.Token;
import java.util.ArrayList;
import java.util.HashMap;

public class Repositorio {

    public static final ArrayList<Token> listaTokens = new ArrayList<>();
    public static final ArrayList<Token> listaTokensFiltrados = new ArrayList<>();
    public static final ArrayList<ErrorLSSL> listaErrores = new ArrayList<>();
    public static final HashMap<String, Simbolo> tablaSimbolos = new HashMap<>();

    public static void limpiar() {
        listaTokens.clear();
        listaTokensFiltrados.clear();
        listaErrores.clear();
        tablaSimbolos.clear();
        FiltroParentesis.erroresEncontrados.clear();
    }
}
