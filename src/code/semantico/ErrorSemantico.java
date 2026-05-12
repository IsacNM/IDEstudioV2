package code.semantico;

/**
 * Catálogo de tipos de error reportados por el analizador semántico.
 * Cada entrada tiene un ID estable que viaja en {@code ErrorLSSL.id} y
 * puede usarse para filtrar/clasificar los errores en la GUI.
 *
 * <pre>
 * Rangos:
 *   100-109 → semántica de variables y tipos
 *   110-119 → estructura sintáctica detectada por filtros (paréntesis)
 *   200-299 → reservado para errores estructurales reportados por
 *             AnalizadorSintactico antes del Grammar
 * </pre>
 */
public enum ErrorSemantico {
    VAR_NO_DECLARADA              (101),
    VAR_NO_INICIALIZADA           (102),
    TIPOS_INCOMPATIBLES           (103),
    OPERACION_INVALIDA            (104),
    CONDICION_NO_BOOLEANA         (105),
    COMPARACION_INVALIDA          (106),
    VAR_DUPLICADA                 (107),
    CONST_REASIGNADA              (108),
    CONST_SIN_VALOR               (109),
    PAREN_DESEQUILIBRADO_ABIERTOS (110),
    PAREN_DESEQUILIBRADO_CERRADOS (111);

    public final int id;

    ErrorSemantico(int id) {
        this.id = id;
    }
}
