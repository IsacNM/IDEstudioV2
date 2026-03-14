package code.semantico;

public enum ErrorSemantico {
    VAR_NO_DECLARADA(101),
    VAR_NO_INICIALIZADA(102),
    TIPOS_INCOMPATIBLES(103),
    OPERACION_INVALIDA(104),
    CONDICION_NO_BOOLEANA(105),
    COMPARACION_INVALIDA(106);

    public final int id;

    ErrorSemantico(int id) {
        this.id = id;
    }
}
