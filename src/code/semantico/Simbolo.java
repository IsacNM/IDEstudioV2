package code.semantico;

public class Simbolo {
    private String ident;          // Nombre de la variable
    private String tipoDato;       // TIPO_ENTERO, TIPO_DECIMAL, etc.
    private String valor;          // Valor inicial
    private String varConstParam;  // "var" o "const"

 public String getIdent() {
        return ident;
}
 
  public void setIdent(String ident) {
        this.ident = ident;
    }
    
    public String getTipoDato() {
        return tipoDato;
    }
    
    public void setTipoDato(String tipoDato) {
        this.tipoDato = tipoDato;
    }
    
    public String getValor() {
        return valor;
    }
    
    public void setValor(String valor) {
        this.valor = valor;
    }
    
    public String getVarConstParam() {
        return varConstParam;
    }
    
    public void setVarConstParam(String varConstParam) {
        this.varConstParam = varConstParam;
    }
}