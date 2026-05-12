                           #Clase proporcionada por el docente,adaptar a nuestro codigo
public int posSigToken(Production tokens,int posicionActual,String tokenReferencia){
    int y=posicionActual;
       while (!tokens.lexicalCompRank(y).equals(tokenReferencia))
           y++;
     return(y+1);  
   }
  public void GeneracionCodigoIntermedio(String tokenFinal)
   {
        String varTemp;
        ArrayList<String> expArit=new ArrayList<>();
    while (!tokens.lexicalCompRank(posicionTokenActual).equals(tokenFinal))
     {
       switch (tokens.lexicalCompRank(posicionTokenActual))
       {
           case "T_VAR"://analizar de forma ciclica todas las posibles declaracion que tengan un valor asignado                        
                         posicionTokenActual++;  //ubicar el primer identificador
                       while(!tokens.lexicalCompRank(posicionTokenActual).equals("T_BEGIN"))
                       {
                        if (tokens.lexicalCompRank(posicionTokenActual+1).equals("T_ASIGNAR"))
                         //T_ASIGNAR,20,"",x
                         generador3D.gc("T_ASIGNAR", tokens.lexemeRank(posicionTokenActual+2), "", tokens.lexemeRank(posicionTokenActual));
                        posicionTokenActual=posSigToken(tokens,posicionTokenActual,"T_PUNTOCOMA");
                       } 
                         break;
           case "T_WRITE":
           case "T_WRITELN":
           case "T_READ":
           case "T_READLN":generador3D.gc(tokens.lexicalCompRank(posicionTokenActual), "", "", tokens.lexemeRank(posicionTokenActual+2));
                         posicionTokenActual=posSigToken(tokens,posicionTokenActual,"T_PUNTOCOMA");
                         break;
           case "T_BEGIN":posicionTokenActual++;
                        break;
           case "T_END":posicionTokenActual++;
                      break;
           case "T_IDENTIFICADOR":if (tokens.lexicalCompRank(posicionTokenActual+1).equals("T_ASIGNAR"))
                                   {
                                    if  (!tokens.lexicalCompRank(posicionTokenActual+3).equals("T_PUNTOCOMA"))
                                     { //recuperar expresion aritmetica
                                      while (!tokens.lexicalCompRank(posicionTokenActual).equals("T_PUNTOCOMA"))
                                       {
                                        expArit.add(tokens.lexemeRank(posicionTokenActual));
                                        posicionTokenActual++;
                                       }
                                      //convertir a posfija
                                      ArrayList posfija=notacion.infijaPostfija(expArit);
                                      //Generar codigo 3D de la expArit
                                      posfija3D(posfija);
                                      //moverse al siguiente token del punto y coma
                                      posicionTokenActual++;
                                     }
                                    else
                                    {//asignacion simple
                                     generador3D.gc(":=",tokens.lexemeRank(posicionTokenActual+2),"",tokens.lexemeRank(posicionTokenActual));  
                                     posicionTokenActual=posSigToken(tokens,posicionTokenActual,"T_PUNTOCOMA");
                                    }   
                                   }  
                                  break;
           case "T_IF":String etiqTempVerdaderoIf=generador3D.nuevaEtiq();
                       String expCondicionalIf="("; 
                       //if (x>10) then
                       posicionTokenActual+=2;
                       while (!tokens.lexicalCompRank(posicionTokenActual).equals("T_PARCIERRA"))
                       {
                        expCondicionalIf=expCondicionalIf.concat(tokens.lexemeRank(posicionTokenActual));
                        posicionTokenActual++;
                       }
                        //agrega a expCondicionalIf el parentesis que cierra
                        expCondicionalIf=expCondicionalIf.concat(tokens.lexemeRank(posicionTokenActual));
                       //expCodicional   (x>10)
                        generador3D.gc("IF", expCondicionalIf, "", etiqTempVerdaderoIf);
                        String etiqTempFalsoIf=generador3D.nuevaEtiq();
                        generador3D.gc("GOTO", "", "", etiqTempFalsoIf);
                        generador3D.gc("LABEL", "", "", etiqTempVerdaderoIf);
                        //mover la posicion actual del token hasta despues del begin
                        posicionTokenActual=posSigToken(tokens,posicionTokenActual,"T_BEGIN");
                        //guardar código intermedio del bloque de intrucciones
                        GeneracionCodigoIntermedio("T_END");
                        generador3D.gc("LABEL", "", "", etiqTempFalsoIf);
                       break; 
           case "T_WHILE":String etiqTempRegreso=generador3D.nuevaEtiq();
                          String etiqTempVerdaderoWhile=generador3D.nuevaEtiq();
                        String expCondicionalWhile="("; 
                        posicionTokenActual+=2;
                       while (!tokens.lexicalCompRank(posicionTokenActual).equals("T_PARCIERRA"))
                       {
                        expCondicionalWhile=expCondicionalWhile.concat(tokens.lexemeRank(posicionTokenActual));
                        posicionTokenActual++;
                       }
                       //agrega a expCondicionalWhile el parentesis que cierra
                       expCondicionalWhile=expCondicionalWhile.concat(tokens.lexemeRank(posicionTokenActual));
                       generador3D.gc("LABEL", "", "", etiqTempRegreso);
                       generador3D.gc("IF", expCondicionalWhile, "", etiqTempVerdaderoWhile);
                       String etiqTempFalsoWhile=generador3D.nuevaEtiq();
                       generador3D.gc("GOTO", "", "", etiqTempFalsoWhile);
                       generador3D.gc("LABEL", "", "", etiqTempVerdaderoWhile); 
                       posicionTokenActual=posSigToken(tokens,posicionTokenActual,"T_BEGIN");
                       //guardar código intermedio del bloque de intrucciones
                        GeneracionCodigoIntermedio("T_END");
                        generador3D.gc("GOTO", "", "", etiqTempRegreso);
                        generador3D.gc("LABEL", "", "", etiqTempFalsoWhile);
                         break;            
           case "T_PUNTOCOMA":posicionTokenActual++;
                            break;
           case "T_PUNTO":break;                 
       }
    }
   }



   Otra clase para implementar en mi codigo para la generacion del archivo 3d.
   package idepascual;

import java.io.FileOutputStream;
import java.io.PrintStream;

public class Generador3D {
   public int contadorTemp;
   public int contadorEtiq;
   public PrintStream archivo3D = System.out;


        public void crearArchivoCodigo3D(String rutaArchivo,String nombreArchivo)
         {
          String intermedio=rutaArchivo+nombreArchivo;
          intermedio=intermedio.replace(".jsn", ".3D");      
           try {
            archivo3D = new PrintStream(new FileOutputStream(intermedio));
           } 
           catch (Exception e) {
            e.printStackTrace();
           }    
          }
    
	public void gc(String operacion, String operando1, String operando2, String resultado) {
		switch(operacion) {                      //x:=20
                        case "T_ASIGNAR":archivo3D.println( resultado+":="+operando1+";");  
                                     break; 
                        case "T_READ":
                        case "T_READLN":
			case "T_WRITE":
                        case "T_WRITELN":    
			         archivo3D.println(operacion+" " + resultado + ";");
				break;
                        case "+":
                        case "-":
                        case "*":
                        case "/":
                                 archivo3D.println(resultado+":="+operando1+operacion+operando2);
                                 break;
                        case ":=":archivo3D.println(resultado+":="+operando1);
                                  break;
                        case "IF": archivo3D.println("if "+operando1+" then GOTO "+resultado);
                                   break;
                        case "GOTO":archivo3D.println("GOTO "+resultado);
                                    break;
                        case "LABEL":archivo3D.println(resultado+":");
                                     break;            
			default:
				System.err.println("Error en la generación de código");
		}       
	}
        
	public String nuevaTemp() {
            contadorTemp++;
		return "T" + contadorTemp;
	}

	public String nuevaEtiq() {
            contadorEtiq++;
		return "L" + contadorEtiq;
	}
        
        public void inicializarTempEtiq(){
          contadorTemp=0;
          contadorEtiq=0;  
        }
        
        public void cerrarArchivoCodigo3D(){
            archivo3D.close();
        }
}
otro codigo para implementar a nuestro codigo:
private void recuperarSimbolos(){
        HashMap<String,String> valoresCompatibles=new HashMap<>();
        valoresCompatibles.put("BYTE","0");
        valoresCompatibles.put("INTEGER","0");
        valoresCompatibles.put("REAL","0.0");
        valoresCompatibles.put("STRING","");

     for (Production id:Repositorio.idDeclaracionesGlobales)  
     {
         //recupera lexamas
         //System.out.println(id.lexemeRank(0)); 
         //System.out.println(id.lexemeRank(0, -1));
//         var
//          cad:string;
//          edad,nControl,edad:byte;
//          saludo:String;
//          cad:integer;
      //ANALIZAR TODOS LOS IDENTIFICADORES QUE APAREZCAN EN id
      //SI EL IDENTIFICADOR NO FUE ENCONTRADO EN LA TABLA SIMBOLOS, AGREGARLO
      //SI EL IDENTIFICADOR FUE ENCONTRADO EN LA TABLA SIMBOLOS, MARCAR EL ERROR  
        // Repositorio.gestionErrores.add(new ErrorLSSL(1,"Error Semántico {}: variable "+id.lexemeRank(0)+"redeclarada",id,true)); 
:
:
:
UNA VEZ QUE YA NO HAYA ERRORES QUE AGREGAR DESDE LA CREACION DE LA TABLA DE SIMBOLOS, AHORA SI SE EJECUTA EL SEMANTICO....
 
 
private void analisisSemantico()
    {
        //RECUPERACION DE ASIGNACIONES
     for (Production asignacion:Repositorio.idAsignaciones)  
     {
         //System.out.println(asignacion.lexemeRank(0,-1));
         //System.out.println(asignacion.lexicalCompRank(0, -1));
         //VERIFICAR TODO LO CORRESPONDIENTE A LOS VALORES E IDENTIFICADORES QUE SE USEN EN LAS ASIGNACIONES 
        //Y SI FUERA EL CASO AGREGAR LOS ERRORES CORRESPONDIENTES AL ARRAYLIST DE ERRORES 
                          // Repositorio.gestionErrores.add(new ErrorLSSL(1,"Error Semántico {}: .......
        //QUE AL TERMINAR EL SEMANTICO SE DEBEN MOSTRAR
     }   
        //RECUPERACION DE SENTENCIAS WRITELN
      for (Production writeln:Repositorio.idWriteln)  
       {
        //writeln.lexemeRank(0,-1);
        //writeln.lexicalCompRank(0,-1);
        //VERIFICAR TODO LO CORRESPONDIENTE A LOS VALORES E IDENTIFICADORES QUE SE USEN EN LA SENTENCIA WRITELN  
        //Y SI FUERA EL CASO AGREGAR LOS ERRORES CORRESPONDIENTES AL ARRAYLIST DE ERRORES 
                          // Repositorio.gestionErrores.add(new ErrorLSSL(1,"Error Semántico {}: .......
        //QUE AL TERMINAR EL SEMANTICO SE DEBEN MOSTRAR
       }
      //RECUPERACION DE SENTENCIAS READLN
      for (Production readln:Repositorio.idReadln)  
       {
        //readln.lexemeRank(0,-1);
        //readln.lexicalCompRank(0,-1);
        //VERIFICAR TODO LO CORRESPONDIENTE A LOS VALORES E IDENTIFICADORES QUE SE USEN EN LA SENTENCIA READLN  
        //Y SI FUERA EL CASO AGREGAR LOS ERRORES CORRESPONDIENTES AL ARRAYLIST DE ERRORES
                       // Repositorio.gestionErrores.add(new ErrorLSSL(1,"Error Semántico {}: ....... 
        //QUE AL TERMINAR EL SEMANTICO SE DEBEN MOSTRAR
       }
      //AGREGAR LOS CICLOS FOR PARA RECORRER TODAS LAS VARIABLES QUE RECUPERAN LAS REGLAS DONDE SE USAN VALORES E IDENTIFICADORES
      //ANALIZAR LAS REGLAS RECUPERADAS Y SI FUERA EL CASO AGREGAR LOS ERRORES CORRESPONDIENTES AL ARRAYLIST DE ERRORES
                 // Repositorio.gestionErrores.add(new ErrorLSSL(1,"Error Semántico {}: .......
      //QUE AL TERMINAR EL SEMANTICO SE DEBEN MOSTRAR
    }