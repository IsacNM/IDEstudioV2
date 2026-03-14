package code.semantico;

public class Simbolo {
    private String ident;        
    private String tipoDato;      
    private String valor;         
    private String varConstParam; 

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