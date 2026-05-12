package view;

import code.editor.Edition;
import code.editor.Time;
import code.editor.File;
import code.GestorCompilador;
import code.editor.Position;
import javax.swing.ImageIcon;
import javax.swing.table.DefaultTableModel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

public class IDE extends javax.swing.JFrame implements Runnable {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(IDE.class.getName());

    public IDE() {
        initComponents();
        setIconImage(new ImageIcon(getClass().getResource("/imgs/iconApp.jpg")).getImage());

        // Hilo del reloj: daemon (muere automáticamente al cerrar la JVM)
        // y se interrumpe en formWindowClosing (ver run()).
        hiloReloj = new Thread(this, "reloj-IDE");
        hiloReloj.setDaemon(true);
        hiloReloj.start();

        File.newFile(jTabbed, positionLabel);

        // Aplicar el idioma persistido al arrancar (si existe) para que la
        // ventana refleje la última preferencia del usuario.
        java.util.Properties prop = new code.editor.ArchivoPropiedades().LeerPropiedades();
        if (prop != null) {
            String idiomaGuardado = prop.getProperty("idioma");
            if (idiomaGuardado != null && !idiomaGuardado.isBlank()) {
                actualizarIdioma(idiomaGuardado.trim().toLowerCase());
            }
        }
    }

    private Thread hiloReloj;

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jFileChooser = new javax.swing.JFileChooser();
        jPopup = new javax.swing.JPopupMenu();
        Copiar = new javax.swing.JMenuItem();
        Cortar = new javax.swing.JMenuItem();
        Pegar = new javax.swing.JMenuItem();
        background = new javax.swing.JPanel();
        positionLabel = new javax.swing.JLabel();
        timeLabel = new javax.swing.JLabel();
        separator = new javax.swing.JSeparator();
        iconBar = new javax.swing.JPanel();
        ajustes1 = new javax.swing.JButton();
        ajustes2 = new javax.swing.JButton();
        ajustes3 = new javax.swing.JButton();
        ajustes4 = new javax.swing.JButton();
        ajustes5 = new javax.swing.JButton();
        ajustes6 = new javax.swing.JButton();
        ajustes7 = new javax.swing.JButton();
        ajustes8 = new javax.swing.JButton();
        ajustes9 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jTabbed = new javax.swing.JTabbedPane();
        jLabel1 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jTabbedPane2 = new javax.swing.JTabbedPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        TablaTokens = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tablaSimbolos = new javax.swing.JTable();
        MenuBar = new javax.swing.JMenuBar();
        archivo = new javax.swing.JMenu();
        jmenuNuevo = new javax.swing.JMenuItem();
        jmenuAbrir = new javax.swing.JMenuItem();
        jmenuGuardar = new javax.swing.JMenuItem();
        jmenuGuardarTodo = new javax.swing.JMenuItem();
        jmenuGuardarComo = new javax.swing.JMenuItem();
        jmenuCerrarProyecto = new javax.swing.JMenuItem();
        jmenuCerrarTodo = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jmenuSalir = new javax.swing.JMenuItem();
        Edicion = new javax.swing.JMenu();
        jmenuCopiar = new javax.swing.JMenuItem();
        jmenuCortar = new javax.swing.JMenuItem();
        jmenuPegar = new javax.swing.JMenuItem();
        Mcompilar = new javax.swing.JMenu();
        jmenuCompilar = new javax.swing.JMenuItem();
        jmenuCompilaryCorrer = new javax.swing.JMenuItem();
        Herramientas = new javax.swing.JMenu();
        jmenuConf = new javax.swing.JMenuItem();
        Info = new javax.swing.JMenu();

        Copiar.setText("Copiar");
        Copiar.addActionListener(this::CopiarActionPerformed);
        jPopup.add(Copiar);

        Cortar.setText("Cortar");
        Cortar.addActionListener(this::CortarActionPerformed);
        jPopup.add(Cortar);

        Pegar.setText("Pegar");
        Pegar.addActionListener(this::PegarActionPerformed);
        jPopup.add(Pegar);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("IDEstudio");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setIconImages(null);
        setResizable(false);
        setSize(new java.awt.Dimension(1036, 910));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        background.setBackground(new java.awt.Color(204, 204, 204));
        background.setForeground(new java.awt.Color(255, 255, 255));

        positionLabel.setFont(new java.awt.Font("Ebrima", 0, 14)); // NOI18N
        positionLabel.setText("Columna: 1, Reglón: 1.");

        timeLabel.setFont(new java.awt.Font("Ebrima", 0, 14)); // NOI18N
        timeLabel.setText("Hora actual: 02:50 P.M");

        ajustes1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imgs/archivo-nuevo.png"))); // NOI18N
        ajustes1.setBorderPainted(false);
        ajustes1.setFocusPainted(false);
        ajustes1.addActionListener(this::ajustes1ActionPerformed);

        ajustes2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imgs/folder-open_10470361.png"))); // NOI18N
        ajustes2.setBorderPainted(false);
        ajustes2.setFocusPainted(false);
        ajustes2.addActionListener(this::ajustes2ActionPerformed);

        ajustes3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imgs/duplicate_3917512.png"))); // NOI18N
        ajustes3.setBorderPainted(false);
        ajustes3.setFocusPainted(false);
        ajustes3.addActionListener(this::ajustes3ActionPerformed);

        ajustes4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imgs/cut_13801285.png"))); // NOI18N
        ajustes4.setBorderPainted(false);
        ajustes4.setFocusPainted(false);
        ajustes4.addActionListener(this::ajustes4ActionPerformed);

        ajustes5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imgs/Custom-Icon-Design-Mono-General-2-Paste.24.png"))); // NOI18N
        ajustes5.setBorderPainted(false);
        ajustes5.setFocusPainted(false);
        ajustes5.addActionListener(this::ajustes5ActionPerformed);

        ajustes6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imgs/play.png"))); // NOI18N
        ajustes6.setBorderPainted(false);
        ajustes6.setFocusPainted(false);
        ajustes6.addActionListener(this::ajustes6ActionPerformed);

        ajustes7.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imgs/cancel.png"))); // NOI18N
        ajustes7.setBorderPainted(false);
        ajustes7.setFocusPainted(false);
        ajustes7.addActionListener(this::ajustes7ActionPerformed);

        ajustes8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imgs/diskette.png"))); // NOI18N
        ajustes8.setBorderPainted(false);
        ajustes8.setFocusPainted(false);
        ajustes8.addActionListener(this::ajustes8ActionPerformed);

        ajustes9.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imgs/setting.png"))); // NOI18N
        ajustes9.setBorderPainted(false);
        ajustes9.setFocusPainted(false);
        ajustes9.addActionListener(this::ajustes9ActionPerformed);

        javax.swing.GroupLayout iconBarLayout = new javax.swing.GroupLayout(iconBar);
        iconBar.setLayout(iconBarLayout);
        iconBarLayout.setHorizontalGroup(
            iconBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(iconBarLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ajustes1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ajustes2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ajustes8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ajustes3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ajustes4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ajustes5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ajustes9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ajustes6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ajustes7)
                .addContainerGap(999, Short.MAX_VALUE))
        );
        iconBarLayout.setVerticalGroup(
            iconBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
            .addComponent(ajustes1)
            .addComponent(ajustes2)
            .addComponent(ajustes3)
            .addComponent(ajustes4)
            .addComponent(ajustes5)
            .addComponent(ajustes6)
            .addComponent(ajustes7)
            .addComponent(ajustes8)
            .addComponent(ajustes9)
        );

        jPanel1.setEnabled(false);

        jTabbed.addChangeListener(this::jTabbedStateChanged);

        jLabel1.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imgs/archivo-nuevo.png"))); // NOI18N
        jLabel1.setText("Genere un nuevo archivo para continuar");
        jLabel1.setToolTipText("");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbed, javax.swing.GroupLayout.DEFAULT_SIZE, 622, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 622, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbed, javax.swing.GroupLayout.DEFAULT_SIZE, 456, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 456, Short.MAX_VALUE))
        );

        TablaTokens.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "Token", "Lexema", "[Renglón, Columna]"
            }
        ));
        TablaTokens.setName("Tabla de tokens"); // NOI18N
        jScrollPane1.setViewportView(TablaTokens);

        jTabbedPane2.addTab("Salida de información", jScrollPane1);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane2)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane2)
        );

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane2.setViewportView(jTextArea1);

        jTabbedPane1.addTab("Resultado de compilación", jScrollPane2);

        tablaSimbolos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Identificador", "Tipo", "Valor", "Categoria"
            }
        ));
        jScrollPane3.setViewportView(tablaSimbolos);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 669, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(0, 4, Short.MAX_VALUE)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 247, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 5, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jTabbedPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout backgroundLayout = new javax.swing.GroupLayout(background);
        background.setLayout(backgroundLayout);
        backgroundLayout.setHorizontalGroup(
            backgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(separator, javax.swing.GroupLayout.PREFERRED_SIZE, 1044, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(iconBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGroup(backgroundLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(backgroundLayout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(backgroundLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(positionLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 192, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(timeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        backgroundLayout.setVerticalGroup(
            backgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundLayout.createSequentialGroup()
                .addComponent(iconBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(positionLabel)
                    .addComponent(timeLabel))
                .addGap(71, 71, 71)
                .addComponent(separator, javax.swing.GroupLayout.PREFERRED_SIZE, 9, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        archivo.setText("Archivo");
        archivo.setName("archivo"); // NOI18N

        jmenuNuevo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jmenuNuevo.setText("Nuevo");
        archivo.add(jmenuNuevo);

        jmenuAbrir.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        jmenuAbrir.setText("Abrir");
        jmenuAbrir.addActionListener(this::jmenuAbrirActionPerformed);
        archivo.add(jmenuAbrir);

        jmenuGuardar.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jmenuGuardar.setText("Guardar");
        jmenuGuardar.addActionListener(this::jmenuGuardarActionPerformed);
        archivo.add(jmenuGuardar);

        jmenuGuardarTodo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jmenuGuardarTodo.setText("Guardar Todo");
        jmenuGuardarTodo.addActionListener(this::jmenuGuardarTodoActionPerformed);
        archivo.add(jmenuGuardarTodo);

        jmenuGuardarComo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jmenuGuardarComo.setText("Guardar como..");
        jmenuGuardarComo.addActionListener(this::jmenuGuardarComoActionPerformed);
        archivo.add(jmenuGuardarComo);

        jmenuCerrarProyecto.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jmenuCerrarProyecto.setText("Cerrar Proyecto");
        jmenuCerrarProyecto.addActionListener(this::jmenuCerrarProyectoActionPerformed);
        archivo.add(jmenuCerrarProyecto);

        jmenuCerrarTodo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_4, java.awt.event.InputEvent.ALT_DOWN_MASK));
        jmenuCerrarTodo.setText("Cerrar Todo");
        jmenuCerrarTodo.addActionListener(this::jmenuCerrarTodoActionPerformed);
        archivo.add(jmenuCerrarTodo);
        archivo.add(jSeparator1);

        jmenuSalir.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jmenuSalir.setText("Salir");
        jmenuSalir.addActionListener(this::jmenuSalirActionPerformed);
        archivo.add(jmenuSalir);

        MenuBar.add(archivo);

        Edicion.setText("Edicion");

        jmenuCopiar.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jmenuCopiar.setText("Copiar");
        jmenuCopiar.addActionListener(this::jmenuCopiarActionPerformed);
        Edicion.add(jmenuCopiar);

        jmenuCortar.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jmenuCortar.setText("Cortar");
        jmenuCortar.addActionListener(this::jmenuCortarActionPerformed);
        Edicion.add(jmenuCortar);

        jmenuPegar.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jmenuPegar.setText("Pegar");
        jmenuPegar.addActionListener(this::jmenuPegarActionPerformed);
        Edicion.add(jmenuPegar);

        MenuBar.add(Edicion);

        Mcompilar.setText("Compilar");

        jmenuCompilar.setText("Compilar");
        jmenuCompilar.addActionListener(this::jmenuCompilarActionPerformed);
        Mcompilar.add(jmenuCompilar);

        jmenuCompilaryCorrer.setText("Compilar y Correr");
        jmenuCompilaryCorrer.addActionListener(this::jmenuCompilaryCorrerActionPerformed);
        Mcompilar.add(jmenuCompilaryCorrer);

        MenuBar.add(Mcompilar);

        Herramientas.setText("Herramientas");

        jmenuConf.setText("Configuracion");
        jmenuConf.addActionListener(this::jmenuConfActionPerformed);
        Herramientas.add(jmenuConf);

        MenuBar.add(Herramientas);

        Info.setText("Acerca de..");
        MenuBar.add(Info);

        setJMenuBar(MenuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(background, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(background, javax.swing.GroupLayout.PREFERRED_SIZE, 796, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void ajustes1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ajustes1ActionPerformed
        // TODO add your handling code here:
        File.newFile(jTabbed, positionLabel);
    }//GEN-LAST:event_ajustes1ActionPerformed

    private void ajustes7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ajustes7ActionPerformed
        // TODO add your handling code here:
        File.closeFile(jTabbed, jFileChooser);
    }//GEN-LAST:event_ajustes7ActionPerformed

    private void ajustes8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ajustes8ActionPerformed
        // TODO add your handling code here:
        File.saveFile(jTabbed, jFileChooser);
    }//GEN-LAST:event_ajustes8ActionPerformed

    private void jmenuGuardarComoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenuGuardarComoActionPerformed
        // TODO add your handling code here:
        File.saveAsFile(jTabbed, jFileChooser);
    }//GEN-LAST:event_jmenuGuardarComoActionPerformed

    private void jmenuGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenuGuardarActionPerformed
        // TODO add your handling code here:
        File.saveFile(jTabbed, jFileChooser);
    }//GEN-LAST:event_jmenuGuardarActionPerformed

    private void jmenuGuardarTodoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenuGuardarTodoActionPerformed
        // TODO add your handling code here:
        File.saveAllFiles(jTabbed, jFileChooser);
    }//GEN-LAST:event_jmenuGuardarTodoActionPerformed

    private void jmenuCerrarProyectoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenuCerrarProyectoActionPerformed
        // TODO add your handling code here:
        File.closeFile(jTabbed, jFileChooser);
    }//GEN-LAST:event_jmenuCerrarProyectoActionPerformed

    private void jmenuCerrarTodoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenuCerrarTodoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jmenuCerrarTodoActionPerformed

    private void jmenuSalirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenuSalirActionPerformed
        // TODO add your handling code here:
        File.exitApp(jTabbed, jFileChooser);
    }//GEN-LAST:event_jmenuSalirActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        if (hiloReloj != null) {
            hiloReloj.interrupt();
        }
        File.exitApp(jTabbed, jFileChooser);
    }//GEN-LAST:event_formWindowClosing

    private void ajustes2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ajustes2ActionPerformed
        // TODO add your handling code here:
        File.openFile(jTabbed, jFileChooser, positionLabel);
    }//GEN-LAST:event_ajustes2ActionPerformed

    private void jmenuAbrirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenuAbrirActionPerformed
        // TODO add your handling code here:
        File.openFile(jTabbed, jFileChooser, positionLabel);
    }//GEN-LAST:event_jmenuAbrirActionPerformed

    private void jmenuCopiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenuCopiarActionPerformed
        // TODO add your handling code here:
        Edition.copiar(jTabbed);
    }//GEN-LAST:event_jmenuCopiarActionPerformed

    private void jmenuCortarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenuCortarActionPerformed
        // TODO add your handling code here:
        Edition.cortar(jTabbed);
    }//GEN-LAST:event_jmenuCortarActionPerformed

    private void jmenuPegarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenuPegarActionPerformed
        // TODO add your handling code here:
        Edition.pegar(jTabbed);
    }//GEN-LAST:event_jmenuPegarActionPerformed

    private void ajustes3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ajustes3ActionPerformed
        // TODO add your handling code here:
        Edition.copiar(jTabbed);
    }//GEN-LAST:event_ajustes3ActionPerformed

    private void ajustes4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ajustes4ActionPerformed
        // TODO add your handling code here:
        Edition.cortar(jTabbed);
    }//GEN-LAST:event_ajustes4ActionPerformed

    private void ajustes5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ajustes5ActionPerformed
        // TODO add your handling code here:
        Edition.pegar(jTabbed);
    }//GEN-LAST:event_ajustes5ActionPerformed

    private void PegarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PegarActionPerformed
        // TODO add your handling code here:
        Edition.pegar(jTabbed);
    }//GEN-LAST:event_PegarActionPerformed

    private void CortarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CortarActionPerformed
        // TODO add your handling code here:
        Edition.cortar(jTabbed);
    }//GEN-LAST:event_CortarActionPerformed

    private void CopiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CopiarActionPerformed
        // TODO add your handling code here:
        Edition.copiar(jTabbed);
    }//GEN-LAST:event_CopiarActionPerformed

    private void jTabbedStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jTabbedStateChanged
        RSyntaxTextArea currentArea = File.getTextAreaActual(jTabbed);
        if (currentArea != null) {
            Position.actualizarPosicionPuntero(currentArea, this.positionLabel);
        }
    }//GEN-LAST:event_jTabbedStateChanged

    private void jmenuConfActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenuConfActionPerformed
        // Abre el diálogo de configuración modal. Al pulsar Aceptar persiste
        // la config (~/.idestudio/config.properties) y aplica el tema a
        // todos los editores abiertos.
        JConfig dialogo = new JConfig(this, true, File.getTextAreaActual(jTabbed), jTabbed);
        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);
    }//GEN-LAST:event_jmenuConfActionPerformed

    private void ajustes9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ajustes9ActionPerformed
        // (placeholder: el botón de ajustes adicional aún no tiene acción)
        JConfig dialogo = new JConfig(this, true, File.getTextAreaActual(jTabbed), jTabbed);
        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);
    }//GEN-LAST:event_ajustes9ActionPerformed

    private void ajustes6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ajustes6ActionPerformed
        // TODO add your handling code here:
        GestorCompilador.ejecutarCompilacion(jTabbed, (DefaultTableModel) TablaTokens.getModel(), jTextArea1, tablaSimbolos);

    }//GEN-LAST:event_ajustes6ActionPerformed

    private void jmenuCompilarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenuCompilarActionPerformed
        // TODO add your handling code here:

        GestorCompilador.ejecutarCompilacion(jTabbed, (DefaultTableModel) TablaTokens.getModel(), jTextArea1, tablaSimbolos);

    }//GEN-LAST:event_jmenuCompilarActionPerformed

    private void jmenuCompilaryCorrerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenuCompilaryCorrerActionPerformed
        // Compilar y mostrar el código intermedio generado en la consola.
        // Antes este botón pintaba la línea 5 hardcodeada como error
        // (resto de prueba). Ahora delega en el GestorCompilador (que se
        // encarga de pintar las líneas reales con error y emitir el .3d)
        // y, si la compilación fue limpia, vuelca el .3d a la consola.
        GestorCompilador.ejecutarCompilacion(
                jTabbed, (DefaultTableModel) TablaTokens.getModel(),
                jTextArea1, tablaSimbolos);

        String rutaArchivo = File.getRutaArchivo(jTabbed, jTabbed.getSelectedIndex());
        if (rutaArchivo == null) {
            return;
        }

        String ruta3D = code.intermedio.Generador3D.obtenerRuta3D(rutaArchivo);
        java.nio.file.Path p = java.nio.file.Paths.get(ruta3D);
        if (!java.nio.file.Files.exists(p)) {
            return;
        }
        try {
            String contenido = java.nio.file.Files.readString(
                    p, java.nio.charset.StandardCharsets.UTF_8);
            jTextArea1.append("\n--- EJECUCIÓN: contenido de " + p.getFileName() + " ---\n");
            jTextArea1.append(contenido);
            jTextArea1.append("\n--- fin ejecución ---\n");
        } catch (java.io.IOException ioe) {
            logger.log(java.util.logging.Level.WARNING,
                    "No se pudo leer el archivo .3d generado", ioe);
        }
    }//GEN-LAST:event_jmenuCompilaryCorrerActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem Copiar;
    private javax.swing.JMenuItem Cortar;
    private javax.swing.JMenu Edicion;
    private javax.swing.JMenu Herramientas;
    private javax.swing.JMenu Info;
    private javax.swing.JMenu Mcompilar;
    private javax.swing.JMenuBar MenuBar;
    private javax.swing.JMenuItem Pegar;
    private javax.swing.JTable TablaTokens;
    private javax.swing.JButton ajustes1;
    private javax.swing.JButton ajustes2;
    private javax.swing.JButton ajustes3;
    private javax.swing.JButton ajustes4;
    private javax.swing.JButton ajustes5;
    private javax.swing.JButton ajustes6;
    private javax.swing.JButton ajustes7;
    private javax.swing.JButton ajustes8;
    private javax.swing.JButton ajustes9;
    private javax.swing.JMenu archivo;
    private javax.swing.JPanel background;
    private javax.swing.JPanel iconBar;
    private javax.swing.JFileChooser jFileChooser;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    public javax.swing.JPopupMenu jPopup;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JTabbedPane jTabbed;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JMenuItem jmenuAbrir;
    private javax.swing.JMenuItem jmenuCerrarProyecto;
    private javax.swing.JMenuItem jmenuCerrarTodo;
    private javax.swing.JMenuItem jmenuCompilar;
    private javax.swing.JMenuItem jmenuCompilaryCorrer;
    private javax.swing.JMenuItem jmenuConf;
    private javax.swing.JMenuItem jmenuCopiar;
    private javax.swing.JMenuItem jmenuCortar;
    private javax.swing.JMenuItem jmenuGuardar;
    private javax.swing.JMenuItem jmenuGuardarComo;
    private javax.swing.JMenuItem jmenuGuardarTodo;
    private javax.swing.JMenuItem jmenuNuevo;
    private javax.swing.JMenuItem jmenuPegar;
    private javax.swing.JMenuItem jmenuSalir;
    public javax.swing.JLabel positionLabel;
    private javax.swing.JSeparator separator;
    private javax.swing.JTable tablaSimbolos;
    private javax.swing.JLabel timeLabel;
    // End of variables declaration//GEN-END:variables

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            String hora = Time.calcularHora();
            javax.swing.SwingUtilities.invokeLater(() -> timeLabel.setText(hora));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();   // restaurar flag y salir
                break;
            }
        }
    }

    public void actualizarIdioma(String idioma) {
        boolean es = idioma.equals("es");

        // Bandera global usada por Time/Position para localizar el reloj y
        // la etiqueta de columna/renglón cuyas actualizaciones ocurren fuera
        // de este método (hilo de reloj, listener de caret).
        code.editor.Time.idiomaEs = es;

        // --- Ventana Principal ---
        setTitle(es ? "IDEstudio" : "IDEstudio");

        // --- Menú Archivo ---
        archivo.setText(es ? "Archivo" : "File");
        jmenuNuevo.setText(es ? "Nuevo" : "New");
        jmenuAbrir.setText(es ? "Abrir" : "Open");
        jmenuGuardar.setText(es ? "Guardar" : "Save");
        jmenuGuardarTodo.setText(es ? "Guardar Todo" : "Save All");
        jmenuGuardarComo.setText(es ? "Guardar como.." : "Save as..");
        jmenuCerrarProyecto.setText(es ? "Cerrar Proyecto" : "Close Project");
        jmenuCerrarTodo.setText(es ? "Cerrar Todo" : "Close All");
        jmenuSalir.setText(es ? "Salir" : "Exit");

        // --- Menú Edición (y Popup) ---
        Edicion.setText(es ? "Edición" : "Edit");
        String txtCopiar = es ? "Copiar" : "Copy";
        String txtCortar = es ? "Cortar" : "Cut";
        String txtPegar = es ? "Pegar" : "Paste";

        jmenuCopiar.setText(txtCopiar);
        jmenuCortar.setText(txtCortar);
        jmenuPegar.setText(txtPegar);

        // Actualizar también el JPopupMenu
        Copiar.setText(txtCopiar);
        Cortar.setText(txtCortar);
        Pegar.setText(txtPegar);

        // --- Menú Compilar ---
        Mcompilar.setText(es ? "Compilar" : "Compile");
        jmenuCompilar.setText(es ? "Compilar" : "Compile");
        jmenuCompilaryCorrer.setText(es ? "Compilar y Correr" : "Compile and Run");

        // --- Menú Herramientas e Info ---
        Herramientas.setText(es ? "Herramientas" : "Tools");
        jmenuConf.setText(es ? "Configuración" : "Settings");
        Info.setText(es ? "Acerca de.." : "About..");

        // --- Labels de Estado (Textos iniciales) ---
        jLabel1.setText(es ? "Genere un nuevo archivo para continuar" : "Generate a new file to continue");
        positionLabel.setText(es ? "Columna: 1, Renglón: 1." : "Column: 1, Line: 1.");
        // Forzar refresco inmediato del reloj sin esperar al siguiente tick.
        timeLabel.setText(code.editor.Time.calcularHora());

        // --- Tooltips de la barra de iconos (ajustes) ---
        ajustes1.setToolTipText(es ? "Nuevo Archivo" : "New File");
        ajustes2.setToolTipText(es ? "Abrir Archivo" : "Open File");
        ajustes8.setToolTipText(es ? "Guardar" : "Save");
        ajustes3.setToolTipText(es ? "Copiar" : "Copy");
        ajustes4.setToolTipText(es ? "Cortar" : "Cut");
        ajustes5.setToolTipText(es ? "Pegar" : "Paste");
        ajustes9.setToolTipText(es ? "Configuración" : "Settings");
        ajustes6.setToolTipText(es ? "Compilar y Correr" : "Compile and Run");
        ajustes7.setToolTipText(es ? "Detener" : "Stop");

        // --- Pestañas de paneles inferiores ---
        if (jTabbedPane2.getTabCount() > 0) {
            jTabbedPane2.setTitleAt(0, es ? "Salida de información" : "Information output");
        }
        if (jTabbedPane1.getTabCount() > 0) {
            jTabbedPane1.setTitleAt(0, es ? "Resultado de compilación" : "Compilation result");
        }

        // --- Tabla de Tokens ---
        String[] cabecera = es
                ? new String[]{"Token", "Lexema", "[Renglón, Columna]"}
                : new String[]{"Token", "Lexeme", "[Line, Column]"};

        // Conservar el contenido actual de la tabla al cambiar las cabeceras.
        javax.swing.table.DefaultTableModel modeloActual
                = (javax.swing.table.DefaultTableModel) TablaTokens.getModel();
        Object[][] datos = new Object[modeloActual.getRowCount()][cabecera.length];
        for (int r = 0; r < modeloActual.getRowCount(); r++) {
            for (int c = 0; c < Math.min(cabecera.length, modeloActual.getColumnCount()); c++) {
                datos[r][c] = modeloActual.getValueAt(r, c);
            }
        }
        TablaTokens.setModel(new javax.swing.table.DefaultTableModel(datos, cabecera));
        TablaTokens.setName(es ? "Tabla de tokens" : "Token table");

        // --- Tabla de Símbolos (Identificadores) ---
        String[] cabeceraSimbolos = es
                ? new String[]{"Identificador", "Tipo", "Valor", "Categoría"}
                : new String[]{"Identifier", "Type", "Value", "Category"};
        javax.swing.table.DefaultTableModel modSim
                = (javax.swing.table.DefaultTableModel) tablaSimbolos.getModel();
        Object[][] datosSim = new Object[modSim.getRowCount()][cabeceraSimbolos.length];
        for (int r = 0; r < modSim.getRowCount(); r++) {
            for (int c = 0; c < Math.min(cabeceraSimbolos.length, modSim.getColumnCount()); c++) {
                datosSim[r][c] = modSim.getValueAt(r, c);
            }
        }
        tablaSimbolos.setModel(new javax.swing.table.DefaultTableModel(datosSim, cabeceraSimbolos));
    }
}
