package view;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.swing.DefaultListModel;
import javax.swing.JTextPane;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import code.editor.ArchivoPropiedades;
import code.editor.EditorTema;
import code.editor.LanguageKeywords;

public class JConfig extends javax.swing.JDialog {

    private static final java.util.logging.Logger LOG =
            java.util.logging.Logger.getLogger(JConfig.class.getName());

    private javax.swing.text.JTextComponent textPane;
    public String fuenteSeleccionada = null;
    public int tamañoSeleccionado = -1;
    public int estiloSeleccionado = -1;
    public Color colorSeleccionado = null;
    private JTextPane editorActual;
    private String idiomaSeleccionado = "es"; // valor por defecto
    public static String rutaPorDefecto = "";
    public static String rutaEnsamblador = "";
    public static String rutaEmuladorDOS = "";
    private Map<String, Color> temaColores; // <--- Mapa para guardar colores por tema
    private String temaSeleccionado = "General"; // <--- Opción seleccionada actualmente

    /** Tabbed donde están los editores; se usa para aplicar el tema a
     *  todos al guardar. Puede ser null si JConfig se invoca sin él. */
    private javax.swing.JTabbedPane tabbedEditores;

    public JConfig(java.awt.Frame parent, boolean modal,
                   javax.swing.text.JTextComponent textPane) {
        this(parent, modal, textPane, null);
    }

    public JConfig(java.awt.Frame parent, boolean modal,
                   javax.swing.text.JTextComponent textPane,
                   javax.swing.JTabbedPane tabbedEditores) {
        super(parent, modal);
        this.textPane = textPane;
        this.tabbedEditores = tabbedEditores;

        this.temaColores = new HashMap<>();
        inicializarTemaColores();

        initComponents();

        cargarFuentes();    // ← llena la lista de fuentes
        cargarEstilos();    // ← llena la lista de estilos
        cargarTamaños();    // ← llena la lista de tamaños
        cargarTema();

        // Cargar configuración persistida (idioma, fuente y rutas) ANTES de
        // registrar listeners para evitar que el evento de selección dispare
        // efectos secundarios sobre valores aún no cargados.
        cargarConfiguracionPersistida();

        configurarListenersDeVista();

        copiarDocumentoConEstilos(textPane, txtEjemplo);

        // Listeners de idioma se registran en configurarListenersDeVista().

        Color colorInicial = temaColores.getOrDefault(temaSeleccionado, Color.BLACK);
        jColorChooser1.setColor(colorInicial);
        txtEjemplo.setForeground(colorInicial);

        // Aplicar el idioma cargado a los textos del propio diálogo.
        actualizarTextosDeJConfig(idiomaSeleccionado);

        // Al final del constructor JConfig
// Llamada inicial para que se coloree al abrir la ventana
        actualizarPreviewSintaxis();
    }

    /** Lee {@code ~/.idestudio/config.properties} y refleja los valores
     *  guardados en los controles del diálogo (idioma, fuente, rutas de
     *  trabajo / ensamblador / emulador). Es seguro llamarla aunque el
     *  archivo no exista todavía: simplemente no hace nada. */
    private void cargarConfiguracionPersistida() {
        Properties prop = new ArchivoPropiedades().LeerPropiedades();

        // 1. Idioma — selecciona el radio correspondiente.
        if (prop != null) {
            String idiomaGuardado = prop.getProperty("idioma");
            if (idiomaGuardado != null && !idiomaGuardado.isBlank()) {
                idiomaSeleccionado = idiomaGuardado.trim().toLowerCase();
            }
        }
        if ("en".equals(idiomaSeleccionado)) {
            Ingles.setSelected(true);
        } else {
            Español.setSelected(true);
        }

        // 2. Fuente actual — preselecciona en la lista y muéstrala en el
        //    campo "MostrarFuente" para que se vea de inmediato.
        String familia = (textPane != null && textPane.getFont() != null)
                ? textPane.getFont().getFamily() : null;
        if (familia != null) {
            fuenteSeleccionada = familia;
            MostrarFuente.setText(familia);
            listaFuentes.setSelectedValue(familia, true);
        }

        // 3. Rutas de la pestaña Compilación.
        if (prop != null) {
            String r1 = prop.getProperty("rutaTrabajo", rutaPorDefecto);
            String r2 = prop.getProperty("rutaEnsamblador", rutaEnsamblador);
            String r3 = prop.getProperty("rutaEmuladorDOS", rutaEmuladorDOS);
            if (r1 != null && !r1.isBlank()) {
                rutaPorDefecto = r1;
                jTextField1.setText(r1);
            } else if (!rutaPorDefecto.isEmpty()) {
                jTextField1.setText(rutaPorDefecto);
            }
            if (r2 != null && !r2.isBlank()) {
                rutaEnsamblador = r2;
                jTextField2.setText(r2);
            }
            if (r3 != null && !r3.isBlank()) {
                rutaEmuladorDOS = r3;
                jTextField3.setText(r3);
            }
        } else if (!rutaPorDefecto.isEmpty()) {
            jTextField1.setText(rutaPorDefecto);
        }
    }

    /** @deprecated usar {@link LanguageKeywords#all()}. */
    @Deprecated
    public static final String[] palabrasReservadas = LanguageKeywords.all();

    String[] tokens = {
        // Bloques
        "{", "}",
        // Comentarios
        "-#", "#[", "]#",
        // Operadores aritméticos
        "++", "--", "**", "//", "%%",
        // Operadores relacionales
        ">>", "<<", "::", ":!", ">>=", "<<=",
        // Operadores lógicos
        "-y-", "-o-", "~",
        // Asignación
        ":=", "+=+", "-=-", "*=*", "/=/",
        // Unarios
        "++>", "--<",
        // Puntuación
        ";", "'", "\"",
        // Estructuras de control
        "si", "sino",
        "suich", "caso", "rompe", "defecto",
        // Ciclos
        "isac", "diego", "repite", "hasta",
        // E/S
        "mostrar", "leer",
        // Declaración
        "var", "const",
        // Tipos
        "entero", "decimal", "texto", "car", "logico", "corto",
        // Booleanos
        "cierto", "falso"
    };

    private void inicializarTemaColores() {
        // Defaults centralizados en EditorTema
        temaColores.putAll(EditorTema.COLORES_DEFAULT);

        // Override con lo persistido por usuario (si existe)
        Properties prop = new ArchivoPropiedades().LeerPropiedades();
        if (prop == null) return;
        for (String categoria : EditorTema.COLORES_DEFAULT.keySet()) {
            temaColores.put(categoria, EditorTema.leerColor(prop, categoria));
        }
    }

    // Función auxiliar para resaltar el preview con el color del tema
    private void actualizarPreviewSintaxis() {
        // Si el componente no está listo, salimos
        if (txtEjemplo == null || temaColores == null) {
            return;
        }

        javax.swing.text.StyledDocument doc = txtEjemplo.getStyledDocument();
        String texto;
        try {
            texto = doc.getText(0, doc.getLength());
        } catch (javax.swing.text.BadLocationException e) {
            return;
        }

        // 1. Resetear todo al color "General" o "Fondo"
        SimpleAttributeSet attrsGeneral = new SimpleAttributeSet();
        Color cGeneral = temaColores.getOrDefault("General", Color.BLACK);
        StyleConstants.setForeground(attrsGeneral, cGeneral);
        doc.setCharacterAttributes(0, doc.getLength(), attrsGeneral, true);

        // Si quieres cambiar el fondo del cuadro también:
        if (temaColores.containsKey("Fondo")) {
            txtEjemplo.setBackground(temaColores.get("Fondo"));
        }

        // 2. Definir patrones (Mismos que en tu interfaz principal)
        // Palabras Reservadas
        String[] reservadas = {
            "inicio", "fin", "si", "sino", "suich", "caso", "rompe",
            "defecto", "mostrar", "leer", "var", "const", "entero",
            "decimal", "texto", "cierto", "falso" // Agrega las que uses en el ejemplo
        };

        // 3. Recorrer el texto y aplicar colores (Simulación rápida del parser)
        int i = 0;
        while (i < texto.length()) {
            char c = texto.charAt(i);

            // --- Comentarios (#) ---
            if (c == '#') {
                int fin = texto.indexOf('\n', i);
                if (fin == -1) {
                    fin = texto.length();
                }
                aplicarEstilo(doc, i, fin - i, temaColores.getOrDefault("Comentarios", Color.GRAY));
                i = fin;
                continue;
            }

            // --- Cadenas (" ") ---
            if (c == '"') {
                int fin = texto.indexOf('"', i + 1);
                if (fin != -1) {
                    aplicarEstilo(doc, i, (fin + 1) - i, temaColores.getOrDefault("Cadenas", Color.BLUE));
                    i = fin + 1;
                    continue;
                }
            }

            // --- Números ---
            if (Character.isDigit(c)) {
                int inicio = i;
                while (i < texto.length() && (Character.isDigit(texto.charAt(i)) || texto.charAt(i) == '.')) {
                    i++;
                }
                // Decidir si es entero o flotante (lógica simple)
                Color cNum = temaColores.getOrDefault("Números enteros", Color.RED);
                if (texto.substring(inicio, i).contains(".")) {
                    cNum = temaColores.getOrDefault("Números flotantes", Color.MAGENTA);
                }
                aplicarEstilo(doc, inicio, i - inicio, cNum);
                continue;
            }

            // --- Palabras / Identificadores ---
            if (Character.isLetter(c)) {
                int inicio = i;
                while (i < texto.length() && Character.isLetterOrDigit(texto.charAt(i))) {
                    i++;
                }
                String palabra = texto.substring(inicio, i);

                // Checar si es reservada
                boolean esReservada = false;
                for (String res : reservadas) {
                    if (res.equals(palabra)) {
                        esReservada = true;
                        break;
                    }
                }

                if (esReservada) {
                    aplicarEstilo(doc, inicio, i - inicio, temaColores.getOrDefault("Palabras reservadas", Color.ORANGE));
                } else {
                    aplicarEstilo(doc, inicio, i - inicio, temaColores.getOrDefault("Identificadores", Color.BLACK));
                }
                continue;
            }

            // --- Signos de puntuación ---
            if (":;(){}[],.".indexOf(c) != -1) {
                aplicarEstilo(doc, i, 1, temaColores.getOrDefault("Signos de puntuación", Color.BLACK));
            }

            i++;
        }
    }

// Helper pequeño para no repetir código
    private void aplicarEstilo(javax.swing.text.StyledDocument doc, int inicio, int longi, Color c) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, c != null ? c : Color.BLACK);
        doc.setCharacterAttributes(inicio, longi, attrs, false);
    }

    private void configurarListenersDeVista() {

        // Fuente
        listaFuentes.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                fuenteSeleccionada = listaFuentes.getSelectedValue();
                if (fuenteSeleccionada != null) {
                    // Reflejar en el campo "MostrarFuente" qué fuente está
                    // seleccionada (antes mostraba siempre "jTextField1").
                    MostrarFuente.setText(fuenteSeleccionada);
                }
                actualizarPreview();
            }
        });

        // Tamaño
        listaTamaños.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                try {
                    tamañoSeleccionado = Integer.parseInt(listaTamaños.getSelectedValue());
                    actualizarPreview();
                } catch (NumberFormatException ex) {
                    tamañoSeleccionado = -1;
                }
            }
        });

        // Estilo
        listaEstilos.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int index = listaEstilos.getSelectedIndex();
                estiloSeleccionado = switch (index) {
                    case 1 ->
                        Font.BOLD;
                    case 2 ->
                        Font.ITALIC;
                    case 3 ->
                        Font.BOLD | Font.ITALIC;
                    default ->
                        Font.PLAIN;
                };
                actualizarPreview();
            }
        });

        Español.addActionListener(e -> {
            cambiarIdioma("es");
        });

        Ingles.addActionListener(e -> {
            cambiarIdioma("en");
        });

        // jButton3 (Buscar emulador DOS) no tiene listener generado por
        // NetBeans, lo registramos aquí para evitar tocar initComponents().
        jButton3.addActionListener(e -> {
            javax.swing.JFileChooser selector = new javax.swing.JFileChooser();
            selector.setFileSelectionMode(javax.swing.JFileChooser.FILES_AND_DIRECTORIES);
            boolean es = "es".equals(idiomaSeleccionado);
            selector.setDialogTitle(es ? "Selecciona la ruta del emulador DOS"
                                       : "Select the DOS emulator path");
            if (selector.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
                rutaEmuladorDOS = selector.getSelectedFile().getAbsolutePath();
                jTextField3.setText(rutaEmuladorDOS);
            }
        });

        jColorChooser1.getSelectionModel().addChangeListener(e -> {
            Color nuevoColor = jColorChooser1.getColor();

            // Obtenemos qué categoría está seleccionada en la lista (ej: "Cadenas")
            String categoria = listaTema.getSelectedValue();

            if (categoria != null) {
                // 1. Guardamos el color en el mapa
                temaColores.put(categoria, nuevoColor);

                // 2. ¡IMPORTANTE! Llamamos a la función que repinta todo
                actualizarPreviewSintaxis();
            }
        });

        // Iniciar Preview con valores actuales
        txtEjemplo.setText(textPane.getText());
        txtEjemplo.setFont(textPane.getFont());
        txtEjemplo.setForeground(textPane.getForeground());

    }

    private void cambiarIdioma(String idioma) {
        // 1. Guardar selección temporal en la clase JConfig
        this.idiomaSeleccionado = idioma;

        // 2. Buscar la ventana principal de forma segura
        java.awt.Window parent = javax.swing.SwingUtilities.getWindowAncestor(this);
        if (parent instanceof IDE principal) {
            principal.actualizarIdioma(idioma);
            // Actualizar también los textos de este diálogo (JConfig) si quieres que cambie al momento
            actualizarTextosDeJConfig(idioma);
        }
    }

    private void actualizarTextosDeJConfig(String idioma) {
        boolean es = idioma.equals("es");

        // Título de la ventana
        setTitle(es ? "Configuración" : "Settings");

        // Botones de acción
        Aceptar.setText(es ? "Aceptar" : "OK");
        Cancelar.setText(es ? "Cancelar" : "Cancel");

        // Etiquetas de la pestaña Fuentes
        Tam.setText(es ? "Tamaño" : "Size");
        jLabel1.setText(es ? "Estilo" : "Style");
        jLabel2.setText(es ? "Color" : "Color");
        jLabel3.setText(es ? "Fuente:" : "Font:");

        // Etiquetas de la pestaña Compilación
        jLabel4.setText(es ? "Ruta de trabajo" : "Working path");
        jLabel5.setText(es ? "Ruta de ensamblador" : "Assembler path");
        jLabel6.setText(es ? "Ruta de emulador DOS" : "DOS emulator path");
        String txtBuscar = es ? "Buscar" : "Browse";
        jButton1.setText(txtBuscar);
        jButton2.setText(txtBuscar);
        jButton3.setText(txtBuscar);

        // Pestañas
        if (jTabbedPane1.getTabCount() >= 2) {
            jTabbedPane1.setTitleAt(0, es ? "Fuentes" : "Fonts");
            jTabbedPane1.setTitleAt(1, es ? "Compilación" : "Compilation");
        }
    }

    private void cargarEstilos() {
        DefaultListModel<String> modelo = new DefaultListModel<>();
        modelo.addElement("Plain");
        modelo.addElement("Bold");
        modelo.addElement("Italic");
        modelo.addElement("Bold Italic");
        listaEstilos.setModel(modelo);
    }

    private void cargarTamaños() {
        DefaultListModel<String> modeloTamaños = new DefaultListModel<>();
        int[] tamanos = {8, 9, 10, 11, 12, 13, 14, 15, 16, 18, 20, 22, 24, 28, 32, 36, 48, 72};
        for (int t : tamanos) {
            modeloTamaños.addElement(String.valueOf(t));
        }
        listaTamaños.setModel(modeloTamaños);
    }

    private void cargarFuentes() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fuentes = ge.getAvailableFontFamilyNames();
        listaFuentes.setListData(fuentes);
    }

    private void aplicarCambiosAlEditor() {
        if (editorActual == null) {
            return;
        }

        // Construir la nueva fuente
        Font nuevaFuente = new Font(
                fuenteSeleccionada != null ? fuenteSeleccionada : editorActual.getFont().getFamily(),
                estiloSeleccionado != -1 ? estiloSeleccionado : editorActual.getFont().getStyle(),
                tamañoSeleccionado != -1 ? tamañoSeleccionado : editorActual.getFont().getSize()
        );

        editorActual.setFont(nuevaFuente);

        // Color si se cambió
        if (colorSeleccionado != null) {
            editorActual.setForeground(colorSeleccionado);
        }
    }

    private void actualizarPreview() {
        Font nueva = new Font(
                fuenteSeleccionada != null ? fuenteSeleccionada : textPane.getFont().getFamily(),
                estiloSeleccionado != -1 ? estiloSeleccionado : textPane.getFont().getStyle(),
                tamañoSeleccionado != -1 ? tamañoSeleccionado : textPane.getFont().getSize()
        );

        txtEjemplo.setFont(nueva);

        if (txtEjemplo != null && colorSeleccionado != null) {
            txtEjemplo.setForeground(colorSeleccionado); // solo preview
        }
    }
//
//    private void aplicarColorSoloSeleccion(JTextPane editor, Color color) {
//        var doc = editor.getStyledDocument();
//
//        var attrs = new javax.swing.text.SimpleAttributeSet();
//        javax.swing.text.StyleConstants.setForeground(attrs, color);
//
//        int ini = editor.getSelectionStart();
//        int fin = editor.getSelectionEnd();
//
//        if (ini == fin) {
//            return; // nada seleccionado
//        }
//        doc.setCharacterAttributes(ini, fin - ini, attrs, false);
//    }
//    

    private void cargarTema() {
        DefaultListModel<String> modelo = new DefaultListModel<>();
        modelo.addElement("General");
        modelo.addElement("Palabras reservadas");
        modelo.addElement("Comentarios");
        modelo.addElement("Cadenas");
        modelo.addElement("Identificadores");
        modelo.addElement("Números enteros");
        modelo.addElement("Números flotantes");
        modelo.addElement("Signos de puntuación");
        listaTema.setModel(modelo);
    }

    private void copiarDocumentoConEstilos(javax.swing.text.JTextComponent origenAny, JTextPane destino) {
        // Si el origen no es JTextPane (p.ej. RSyntaxTextArea), copiamos
        // sólo el texto plano al panel de ejemplo.
        if (!(origenAny instanceof JTextPane)) {
            try {
                destino.setStyledDocument(new javax.swing.text.DefaultStyledDocument());
                if (origenAny != null) {
                    destino.setText(origenAny.getText());
                }
            } catch (Exception e) {
                LOG.log(java.util.logging.Level.WARNING,
                        "Error al copiar texto al panel de ejemplo", e);
            }
            return;
        }
        JTextPane origen = (JTextPane) origenAny;
        try {
            destino.setStyledDocument(new javax.swing.text.DefaultStyledDocument());

            var docOrigen = origen.getStyledDocument();
            var docDestino = destino.getStyledDocument();

            for (int i = 0; i < docOrigen.getLength(); i++) {
                var elemento = docOrigen.getCharacterElement(i);
                var atributos = elemento.getAttributes();

                String letra = docOrigen.getText(i, 1);
                docDestino.insertString(docDestino.getLength(), letra, atributos);
            }

        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING,
                    "Error al copiar documento con estilos", e);
        }
    }

    private void guardarConfiguracion() {
        // Aplicar fuente, estilo y tamaño al JTextPane principal
        Font nueva = new Font(
                fuenteSeleccionada != null ? fuenteSeleccionada : textPane.getFont().getFamily(),
                estiloSeleccionado != -1 ? estiloSeleccionado : textPane.getFont().getStyle(),
                tamañoSeleccionado != -1 ? tamañoSeleccionado : textPane.getFont().getSize()
        );
        textPane.setFont(nueva);

        // Guardar propiedades en el archivo de configuración
        ArchivoPropiedades archivo = new ArchivoPropiedades();
        Properties prop = archivo.LeerPropiedades();
        if (prop == null) {
            prop = new Properties(); // si no existe, crear uno nuevo
        }

        for (Map.Entry<String, Color> entry : temaColores.entrySet()) {
            prop.setProperty(EditorTema.llaveColor(entry.getKey()),
                             EditorTema.aHex(entry.getValue()));
        }

        prop.setProperty("idioma", idiomaSeleccionado);
        prop.setProperty("fuente", nueva.getFamily());
        prop.setProperty("estilo", String.valueOf(nueva.getStyle()));
        prop.setProperty("tamaño", String.valueOf(nueva.getSize()));

        // Persistir rutas de la pestaña Compilación. Se toman directamente
        // del campo de texto (no sólo de la variable estática) para que
        // funcionen tanto si el usuario las eligió con "Buscar" como si las
        // tecleó manualmente.
        rutaPorDefecto   = jTextField1.getText() == null ? "" : jTextField1.getText();
        rutaEnsamblador  = jTextField2.getText() == null ? "" : jTextField2.getText();
        rutaEmuladorDOS  = jTextField3.getText() == null ? "" : jTextField3.getText();
        prop.setProperty("rutaTrabajo",     rutaPorDefecto);
        prop.setProperty("rutaEnsamblador", rutaEnsamblador);
        prop.setProperty("rutaEmuladorDOS", rutaEmuladorDOS);

        archivo.EscribirPropiedades(prop);
    }


    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        FuentePestaña = new javax.swing.JPanel();
        jColorChooser1 = new javax.swing.JColorChooser();
        Aceptar = new javax.swing.JButton();
        Cancelar = new javax.swing.JButton();
        Ejemplo = new javax.swing.JPanel();
        ScrollpEjemplo = new javax.swing.JScrollPane();
        txtEjemplo = new javax.swing.JTextPane();
        Idioma = new javax.swing.JPanel();
        Español = new javax.swing.JRadioButton();
        Ingles = new javax.swing.JRadioButton();
        Tam = new javax.swing.JLabel();
        Tamaño = new javax.swing.JPanel();
        jScrollPane8 = new javax.swing.JScrollPane();
        listaTamaños = new javax.swing.JList<>();
        jLabel1 = new javax.swing.JLabel();
        Estilo = new javax.swing.JPanel();
        jScrollPane7 = new javax.swing.JScrollPane();
        listaEstilos = new javax.swing.JList<>();
        MostrarFuente = new javax.swing.JTextField();
        FuentePanel = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        listaFuentes = new javax.swing.JList<>();
        ColorPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        listaTema = new javax.swing.JList<>();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jTextField1 = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();

        buttonGroup1.add(Español);
        buttonGroup1.add(Ingles);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jColorChooser1.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        Aceptar.setText("Aceptar");
        Aceptar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AceptarActionPerformed(evt);
            }
        });

        Cancelar.setText("Cancelar");
        Cancelar.addActionListener(e -> dispose());

        Ejemplo.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        ScrollpEjemplo.setViewportView(txtEjemplo);

        javax.swing.GroupLayout EjemploLayout = new javax.swing.GroupLayout(Ejemplo);
        Ejemplo.setLayout(EjemploLayout);
        EjemploLayout.setHorizontalGroup(
            EjemploLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(EjemploLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ScrollpEjemplo, javax.swing.GroupLayout.DEFAULT_SIZE, 593, Short.MAX_VALUE)
                .addContainerGap())
        );
        EjemploLayout.setVerticalGroup(
            EjemploLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(EjemploLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ScrollpEjemplo, javax.swing.GroupLayout.DEFAULT_SIZE, 269, Short.MAX_VALUE)
                .addContainerGap())
        );

        Idioma.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        Español.setText("Español");

        Ingles.setText("Ingles");

        javax.swing.GroupLayout IdiomaLayout = new javax.swing.GroupLayout(Idioma);
        Idioma.setLayout(IdiomaLayout);
        IdiomaLayout.setHorizontalGroup(
            IdiomaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(IdiomaLayout.createSequentialGroup()
                .addGap(1, 1, 1)
                .addGroup(IdiomaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Ingles)
                    .addComponent(Español, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(19, Short.MAX_VALUE))
        );
        IdiomaLayout.setVerticalGroup(
            IdiomaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(IdiomaLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(Español)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 15, Short.MAX_VALUE)
                .addComponent(Ingles)
                .addGap(28, 28, 28))
        );

        Tam.setText("Tamaño");

        Tamaño.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        listaTamaños.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane8.setViewportView(listaTamaños);

        javax.swing.GroupLayout TamañoLayout = new javax.swing.GroupLayout(Tamaño);
        Tamaño.setLayout(TamañoLayout);
        TamañoLayout.setHorizontalGroup(
            TamañoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(TamañoLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane8, javax.swing.GroupLayout.DEFAULT_SIZE, 71, Short.MAX_VALUE)
                .addContainerGap())
        );
        TamañoLayout.setVerticalGroup(
            TamañoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, TamañoLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jScrollPane8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jLabel1.setText("Estilo");

        Estilo.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        listaEstilos.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane7.setViewportView(listaEstilos);

        javax.swing.GroupLayout EstiloLayout = new javax.swing.GroupLayout(Estilo);
        Estilo.setLayout(EstiloLayout);
        EstiloLayout.setHorizontalGroup(
            EstiloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(EstiloLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 122, Short.MAX_VALUE)
                .addContainerGap())
        );
        EstiloLayout.setVerticalGroup(
            EstiloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(EstiloLayout.createSequentialGroup()
                .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        MostrarFuente.setText("jTextField1");

        FuentePanel.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        listaFuentes.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane6.setViewportView(listaFuentes);

        javax.swing.GroupLayout FuentePanelLayout = new javax.swing.GroupLayout(FuentePanel);
        FuentePanel.setLayout(FuentePanelLayout);
        FuentePanelLayout.setHorizontalGroup(
            FuentePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(FuentePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane6)
                .addContainerGap())
        );
        FuentePanelLayout.setVerticalGroup(
            FuentePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(FuentePanelLayout.createSequentialGroup()
                .addComponent(jScrollPane6)
                .addContainerGap())
        );

        ColorPanel.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        listaTema.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane1.setViewportView(listaTema);

        javax.swing.GroupLayout ColorPanelLayout = new javax.swing.GroupLayout(ColorPanel);
        ColorPanel.setLayout(ColorPanelLayout);
        ColorPanelLayout.setHorizontalGroup(
            ColorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        ColorPanelLayout.setVerticalGroup(
            ColorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING)
        );

        jLabel2.setFont(new java.awt.Font("Segoe UI", 3, 12)); // NOI18N
        jLabel2.setText("Color");

        jLabel3.setText("Fuente:");

        javax.swing.GroupLayout FuentePestañaLayout = new javax.swing.GroupLayout(FuentePestaña);
        FuentePestaña.setLayout(FuentePestañaLayout);
        FuentePestañaLayout.setHorizontalGroup(
            FuentePestañaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(FuentePestañaLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(FuentePestañaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(FuentePestañaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(FuentePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(MostrarFuente, javax.swing.GroupLayout.DEFAULT_SIZE, 282, Short.MAX_VALUE)
                        .addComponent(ColorPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(FuentePestañaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(FuentePestañaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, FuentePestañaLayout.createSequentialGroup()
                            .addComponent(Aceptar)
                            .addGap(18, 18, 18)
                            .addComponent(Cancelar)
                            .addGap(26, 26, 26))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, FuentePestañaLayout.createSequentialGroup()
                            .addComponent(jColorChooser1, javax.swing.GroupLayout.PREFERRED_SIZE, 628, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addContainerGap()))
                    .addGroup(FuentePestañaLayout.createSequentialGroup()
                        .addGroup(FuentePestañaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(FuentePestañaLayout.createSequentialGroup()
                                .addGroup(FuentePestañaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(Estilo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(FuentePestañaLayout.createSequentialGroup()
                                        .addGap(8, 8, 8)
                                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(34, 34, 34)
                                .addGroup(FuentePestañaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(FuentePestañaLayout.createSequentialGroup()
                                        .addComponent(Tamaño, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(36, 36, 36)
                                        .addComponent(Idioma, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(Tam, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addComponent(Ejemplo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
            .addGroup(FuentePestañaLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        FuentePestañaLayout.setVerticalGroup(
            FuentePestañaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, FuentePestañaLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(FuentePestañaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Tam)
                    .addComponent(jLabel1)
                    .addComponent(jLabel3))
                .addGap(1, 1, 1)
                .addGroup(FuentePestañaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(FuentePestañaLayout.createSequentialGroup()
                        .addGroup(FuentePestañaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(Tamaño, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(Estilo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(23, 23, 23)
                        .addComponent(Ejemplo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(FuentePestañaLayout.createSequentialGroup()
                        .addComponent(Idioma, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(FuentePestañaLayout.createSequentialGroup()
                        .addComponent(MostrarFuente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(FuentePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGap(2, 2, 2)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(FuentePestañaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(ColorPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jColorChooser1, javax.swing.GroupLayout.PREFERRED_SIZE, 271, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(FuentePestañaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Cancelar)
                    .addComponent(Aceptar))
                .addGap(11, 11, 11))
        );

        jTabbedPane1.addTab("Fuentes", FuentePestaña);

        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        jLabel4.setText("Ruta de trabajo");

        jLabel5.setText("Ruta de ensamblador");

        jTextField2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField2ActionPerformed(evt);
            }
        });

        jLabel6.setText("Ruta de emulador DOS");

        jTextField3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField3ActionPerformed(evt);
            }
        });

        jButton1.setText("Buscar");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("Buscar");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setText("Buscar");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField2, javax.swing.GroupLayout.DEFAULT_SIZE, 740, Short.MAX_VALUE)
                    .addComponent(jTextField1)
                    .addComponent(jTextField3))
                .addGap(24, 24, 24)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(40, 49, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(54, 54, 54)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton2))
                .addGap(18, 18, 18)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1))
                .addGap(18, 18, 18)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton3))
                .addContainerGap(590, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Compilacion", jPanel2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void AceptarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AceptarActionPerformed
        // 1) Persistir la configuración a ~/.idestudio/config.properties
        guardarConfiguracion();

        // 2) Aplicar el tema recién guardado a TODOS los editores abiertos
        //    (no solo al activo). Si no se pasó tabbedEditores, se omite.
        if (tabbedEditores != null) {
            java.util.Properties prop = new code.editor.ArchivoPropiedades().LeerPropiedades();
            for (int i = 0; i < tabbedEditores.getTabCount(); i++) {
                java.awt.Component c = tabbedEditores.getComponentAt(i);
                if (c instanceof org.fife.ui.rtextarea.RTextScrollPane) {
                    org.fife.ui.rtextarea.RTextScrollPane sp =
                            (org.fife.ui.rtextarea.RTextScrollPane) c;
                    java.awt.Component v = sp.getViewport().getView();
                    if (v instanceof org.fife.ui.rsyntaxtextarea.RSyntaxTextArea) {
                        code.editor.EditorTema.aplicar(
                                (org.fife.ui.rsyntaxtextarea.RSyntaxTextArea) v, prop);
                    }
                }
            }
        }

        dispose();
    }//GEN-LAST:event_AceptarActionPerformed

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void jTextField2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField2ActionPerformed

    private void jTextField3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField3ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // Selector para la ruta del ensamblador. Acepta carpeta o archivo
        // ejecutable; el usuario decide.
        javax.swing.JFileChooser selector = new javax.swing.JFileChooser();
        selector.setFileSelectionMode(javax.swing.JFileChooser.FILES_AND_DIRECTORIES);
        boolean es = "es".equals(idiomaSeleccionado);
        selector.setDialogTitle(es ? "Selecciona la ruta del ensamblador"
                                   : "Select the assembler path");
        if (selector.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            rutaEnsamblador = selector.getSelectedFile().getAbsolutePath();
            jTextField2.setText(rutaEnsamblador);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        javax.swing.JFileChooser selector = new javax.swing.JFileChooser();

        // Configurar para que SOLO deje seleccionar CARPETAS, no archivos
        selector.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);

        // Opcional: poner título (localizado al idioma actual del diálogo)
        boolean es = "es".equals(idiomaSeleccionado);
        selector.setDialogTitle(es ? "Selecciona la carpeta por defecto para guardar"
                                   : "Select the default working folder");

        // Mostrar el diálogo
        int resultado = selector.showOpenDialog(this);

        // Si el usuario da clic en "Aceptar" / "Open"
        if (resultado == javax.swing.JFileChooser.APPROVE_OPTION) {
            java.io.File carpetaSeleccionada = selector.getSelectedFile();

            // 1. Guardar la ruta en la variable estática
            rutaPorDefecto = carpetaSeleccionada.getAbsolutePath();

            // 2. Mostrarla en el campo de texto
            jTextField1.setText(rutaPorDefecto);

            System.out.println("Nueva ruta definida: " + rutaPorDefecto);
        }


    }//GEN-LAST:event_jButton2ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Aceptar;
    private javax.swing.JButton Cancelar;
    private javax.swing.JPanel ColorPanel;
    private javax.swing.JPanel Ejemplo;
    private javax.swing.JRadioButton Español;
    private javax.swing.JPanel Estilo;
    private javax.swing.JPanel FuentePanel;
    private javax.swing.JPanel FuentePestaña;
    private javax.swing.JPanel Idioma;
    private javax.swing.JRadioButton Ingles;
    private javax.swing.JTextField MostrarFuente;
    private javax.swing.JScrollPane ScrollpEjemplo;
    private javax.swing.JLabel Tam;
    private javax.swing.JPanel Tamaño;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JColorChooser jColorChooser1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JTabbedPane jTabbedPane1;
    public static javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JList<String> listaEstilos;
    private javax.swing.JList<String> listaFuentes;
    private javax.swing.JList<String> listaTamaños;
    private javax.swing.JList<String> listaTema;
    private javax.swing.JTextPane txtEjemplo;
    // End of variables declaration//GEN-END:variables
}
