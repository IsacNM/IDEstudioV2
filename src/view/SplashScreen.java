package view;

import javax.swing.ImageIcon;
import javax.swing.UIManager;


public class SplashScreen extends javax.swing.JFrame {

    public SplashScreen() {
        initComponents();
        setIconImage(new ImageIcon(getClass().getResource("/imgs/iconApp.jpg")).getImage());

    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        BackgroundP = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        barraprogreso = new javax.swing.JProgressBar();
        jLabel1 = new javax.swing.JLabel();
        imagenfondo = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("IDEstudio");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        BackgroundP.setPreferredSize(new java.awt.Dimension(900, 430));
        BackgroundP.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 48)); // NOI18N
        jLabel2.setText("IDEstudio");
        BackgroundP.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 20, 320, 80));

        barraprogreso.setIndeterminate(true);
        barraprogreso.setStringPainted(true);
        BackgroundP.add(barraprogreso, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 390, 900, 20));

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imgs/image-removebg-preview.png"))); // NOI18N
        BackgroundP.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(370, 30, 210, 180));

        imagenfondo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imgs/carga.jpg"))); // NOI18N
        BackgroundP.add(imagenfondo, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, 400));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BackgroundP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(BackgroundP, javax.swing.GroupLayout.PREFERRED_SIZE, 406, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 6, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
 final int delay = 5; // velocidad del llenado
    javax.swing.Timer timer = new javax.swing.Timer(delay, null);

    timer.addActionListener(e -> {
        int value = barraprogreso.getValue() + 1;
        barraprogreso.setValue(value);

        if (value >= 100) {
            timer.stop();
            new IDE().setVisible(true); // abre tu ventana principal
            dispose(); // cierra la pantalla de carga
        }
    });

    timer.start();
    }//GEN-LAST:event_formWindowOpened

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // TODO add your handling code here:
    }//GEN-LAST:event_formWindowClosing

    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(SplashScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(SplashScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(SplashScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(SplashScreen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new SplashScreen().setVisible(true);
try {
            UIManager.setLookAndFeel("com.jtattoo.plaf.luna.LunaLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }
            }
        });
    }

    /*
    com.jtattoo.plaf.acryl.AcrylLookAndFeel
com.jtattoo.plaf.aero.AeroLookAndFeel
com.jtattoo.plaf.aluminium.AluminiumLookAndFeel
com.jtattoo.plaf.bernstein.BernsteinLookAndFeel
com.jtattoo.plaf.fast.FastLookAndFeel
com.jtattoo.plaf.graphite.GraphiteLookAndFeel
com.jtattoo.plaf.hifi.HiFiLookAndFeel
com.jtattoo.plaf.luna.LunaLookAndFeel
com.jtattoo.plaf.mcwin.McWinLookAndFeel
com.jtattoo.plaf.mint.MintLookAndFeel
com.jtattoo.plaf.noire.NoireLookAndFeel
com.jtattoo.plaf.smart.SmartLookAndFeel
com.jtattoo.plaf.texture.TextureLookAndFeel
    */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel BackgroundP;
    private javax.swing.JProgressBar barraprogreso;
    private javax.swing.JLabel imagenfondo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    // End of variables declaration//GEN-END:variables
}
