package code.semantico;

public class AnalizadorSemantico {

    public static void construirTablaSimbolos() {
        TablaSimbolos.construir();
    }

    public static void procesarAsignaciones() {
        ProcesadorAsignaciones.procesar();
    }

    public static void validarSentenciasLeer() {
        ProcesadorAsignaciones.validarSentenciasLeer();
    }

    public static void validarSemantica() {
        AuxSemantico.validarVariablesNoDeclaradas(Repositorio.listaErrores);
        AuxSemantico.validarVariablesNoInicializadas(Repositorio.listaErrores);
        AuxSemantico.validarTiposEnAsignaciones(Repositorio.listaErrores);
        AuxSemantico.validarTiposEnOperaciones(Repositorio.listaErrores);
        AuxSemantico.validarCondicionesBooleanas(Repositorio.listaErrores);
        AuxSemantico.validarTiposEnComparaciones(Repositorio.listaErrores);
    }

}
