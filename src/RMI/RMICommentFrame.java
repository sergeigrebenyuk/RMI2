/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package RMI;


import java.awt.BorderLayout;
import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.undo.UndoManager;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Document;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 *
 * @author sg
 */

public class RMICommentFrame extends javax.swing.JFrame {

    private RMI rmi;
    // undo and redo
    //private Document editorPaneDocument;
    protected UndoHandler undoHandler = new UndoHandler();
    protected UndoManager undoManager = new UndoManager();
    private UndoAction undoAction = null;
    private RedoAction redoAction = null;
    
   
    protected class UndoHandler implements UndoableEditListener{
        public void undoableEditHappened(UndoableEditEvent e)
        {
          undoManager.addEdit(e.getEdit());
          undoAction.update();
          redoAction.update();
        }
}
 
class UndoAction extends AbstractAction
{
  private UndoManager manager;
  public UndoAction(UndoManager manager) {
    super("Undo");
    this.manager = manager;
    setEnabled(false);
  }
 
  public void actionPerformed(ActionEvent e)
  {
    try
    {
      undoManager.undo();
    }
    catch (CannotUndoException ex)
    {
      // TODO deal with this
      //ex.printStackTrace();
    }
    update();
    redoAction.update();
  }
 
  protected void update()
  {
    if (undoManager.canUndo())
    {
      setEnabled(true);
      putValue(Action.NAME, undoManager.getUndoPresentationName());
    }
    else
    {
      setEnabled(false);
      putValue(Action.NAME, "Undo");
    }
  }
}
 
class RedoAction extends AbstractAction
{
    private UndoManager manager;
    public RedoAction(UndoManager manager) 
    {
        super("Redo");
        this.manager = manager;
        setEnabled(false);
    }
 
  public void actionPerformed(ActionEvent e)
  {
    try
    {
      undoManager.redo();
    }
    catch (CannotRedoException ex)
    {
      // TODO deal with this
      ex.printStackTrace();
    }
    update();
    undoAction.update();
  }
 
  protected void update()
  {
    if (undoManager.canRedo())
    {
      setEnabled(true);
      putValue(Action.NAME, undoManager.getRedoPresentationName());
    }
    else
    {
      setEnabled(false);
      putValue(Action.NAME, "Redo");
    }
  }
}
    
    public void SetContext(RMI _rmi) {
        rmi = _rmi;
    }
    public RMICommentFrame() {
       initComponents();
        sDescription.getDocument().addUndoableEditListener(undoHandler);
        KeyStroke undoKeystroke = KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK);
        KeyStroke redoKeystroke = KeyStroke.getKeyStroke(KeyEvent.VK_Y, Event.CTRL_MASK);

        undoAction = new UndoAction(undoManager);
        sDescription.getInputMap().put(undoKeystroke, "undoKeystroke");
        sDescription.getActionMap().put("undoKeystroke", undoAction);

        redoAction = new RedoAction(undoManager);
        sDescription.getInputMap().put(redoKeystroke, "redoKeystroke");
        sDescription.getActionMap().put("redoKeystroke", redoAction);
        
        
    }
       
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bClose = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        sDescription = new javax.swing.JTextPane();
        buttonLoad = new javax.swing.JButton();
        buttonTemplate = new javax.swing.JButton();

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        bClose.setText("Close");
        bClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bCloseActionPerformed(evt);
            }
        });

        sDescription.setBackground(new java.awt.Color(255, 255, 204));
        sDescription.setFont(new java.awt.Font("Lucida Sans Typewriter", 0, 18)); // NOI18N
        sDescription.setForeground(new java.awt.Color(0, 51, 102));
        sDescription.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                sDescriptionFocusLost(evt);
            }
        });
        sDescription.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                sDescriptionKeyReleased(evt);
            }
        });
        jScrollPane2.setViewportView(sDescription);
        sDescription.getAccessibleContext().setAccessibleParent(sDescription);

        buttonLoad.setText("Load...");
        buttonLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonLoadActionPerformed(evt);
            }
        });

        buttonTemplate.setText("Template");
        buttonTemplate.setActionCommand("");
        buttonTemplate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonTemplateActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(buttonLoad)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonTemplate)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 311, Short.MAX_VALUE)
                .addComponent(bClose)
                .addContainerGap())
            .addComponent(jScrollPane2)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 536, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(bClose, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(9, 9, 9)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(buttonLoad)
                            .addComponent(buttonTemplate))))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void bCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bCloseActionPerformed
        this.setVisible(false);
    }//GEN-LAST:event_bCloseActionPerformed

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
      sDescription.setText(rmi.sDescription);
    }//GEN-LAST:event_formWindowOpened

    private void sDescriptionKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_sDescriptionKeyReleased
        
        
    }//GEN-LAST:event_sDescriptionKeyReleased

    private void buttonLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonLoadActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_buttonLoadActionPerformed

    private void buttonTemplateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonTemplateActionPerformed
       
    }//GEN-LAST:event_buttonTemplateActionPerformed

    private void sDescriptionFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_sDescriptionFocusLost
       try {
            // Save comment to file
            if (rmi.bExperimentIsSetUp)
            {   rmi.sDescription = sDescription.getText();
              //  rmi.fDescFileChannel.truncate(0);
            rmi.fDescFileWriter = new PrintWriter(rmi.fDescFile);   
            rmi.fDescFileWriter.print(rmi.sDescription.trim());        
            rmi.fDescFileWriter.flush();
            rmi.fDescFileWriter.close();
            
            }
        } catch (IOException ex) {
            Logger.getLogger(RMICommentFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_sDescriptionFocusLost
 
    /**
     * @param args the command line arguments
     */
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
            java.util.logging.Logger.getLogger(RMICommentFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(RMICommentFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(RMICommentFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(RMICommentFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new RMICommentFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bClose;
    private javax.swing.JButton buttonLoad;
    private javax.swing.JButton buttonTemplate;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextPane sDescription;
    // End of variables declaration//GEN-END:variables

    void updateContent() {
        sDescription.setText(rmi.sDescription);
    }
}
