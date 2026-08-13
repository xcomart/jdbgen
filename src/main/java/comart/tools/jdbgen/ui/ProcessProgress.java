/*
 * The MIT License
 *
 * Copyright 2024 comart.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package comart.tools.jdbgen.ui;

import comart.utils.UIUtils;
import java.util.List;
import javax.swing.SwingWorker;

/**
 * undecorated progress dialog for long running background tasks such as the
 * driver download of the maven explorer. The dialog shows a progress bar and
 * a log area which are fed by an attached <code>Worker</code>, and hides
 * itself as soon as that worker is done.
 *
 * @author comart
 */
public class ProcessProgress extends javax.swing.JDialog {
    
    /**
     * background task driving a <code>ProcessProgress</code> dialog.
     * Subclasses only have to implement <code>doInBackground()</code>, report
     * their progress with <code>setProgress(int)</code> and their log lines
     * with <code>publish(String...)</code>, and return whether the task
     * succeeded. The dialog is wired to the worker by the
     * <code>ProcessProgress</code> constructor.
     */
    public static abstract class Worker extends SwingWorker<Boolean, String> {
        /** dialog fed by this worker, assigned by the
         * <code>ProcessProgress</code> constructor. */
        ProcessProgress parent = null;
        
        /**
         * create a worker which is not attached to a dialog yet. The dialog
         * is assigned when the worker is passed to the
         * <code>ProcessProgress</code> constructor.
         */
        public Worker() {
            
        }
        
        /**
         * publish the log lines produced since the last call on the event
         * dispatch thread. The progress bar is set to the current progress
         * value, every chunk is appended as one line to the log area and the
         * caret is moved to the end of the text.
         *
         * @param chunks
         *            log lines published by <code>doInBackground()</code>.
         */
        @Override
        protected void process(List<String> chunks) {
            parent.progStatus.setValue(getProgress());
            for (String chunk:chunks)
                parent.txtProcessLog.append(chunk+"\n");
            int last = parent.txtProcessLog.getText().length();
            parent.txtProcessLog.setSelectionStart(last);
            parent.txtProcessLog.setSelectionEnd(last);
        }

        /**
         * store the result of the task in the dialog and hide it. The value
         * returned by <code>doInBackground()</code> is written to the
         * <code>result</code> field of the dialog, <code>false</code> is
         * stored when the task failed with an exception.
         */
        @Override
        protected void done() {
            boolean bStatus = false;
            try {
                bStatus = get();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            parent.result = bStatus;
            parent.setVisible(false);
        } 
    }
    
    /**
     * outcome of the attached worker, valid once the dialog has been hidden.
     * <code>true</code> when the worker returned <code>true</code>,
     * <code>false</code> when it failed or when it has not run yet.
     */
    public boolean result = false;
    
    /** task started by <code>start()</code>, <code>null</code> when the dialog
     * was created without a worker. */
    private Worker worker = null;

    /**
     * Creates new form ProcessProgress
     * <p>
     * The dialog is created undecorated, centered on <code>parent</code> and
     * attached to <code>worker</code> so that the worker can feed the
     * progress bar and the log area. The worker is not started here, call
     * <code>start()</code> for that.
     *
     * @param parent
     *            frame the dialog belongs to and is centered on.
     * @param modal
     *            <code>true</code> to create a modal dialog.
     * @param worker
     *            background task to be attached, may be <code>null</code> in
     *            which case the dialog shows no progress at all.
     */
    @SuppressWarnings("LeakingThisInConstructor")
    public ProcessProgress(java.awt.Frame parent, boolean modal, Worker worker) {
        super(parent, modal);
        setUndecorated( true );
        initComponents();
        
        setLocationRelativeTo(parent);
        
        if (worker != null) {
            this.worker = worker;
            worker.parent = this;
        }
        UIUtils.setApplicationIcon(this);
        this.pack();
    }
    
    /**
     * start the attached background task. Nothing happens when no worker was
     * given to the constructor. Call this before the dialog is made visible,
     * as a modal dialog blocks the caller until the worker is done.
     */
    public void start() {
        if (worker != null)
            worker.execute();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        progStatus = new javax.swing.JProgressBar();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtProcessLog = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        txtProcessLog.setColumns(20);
        txtProcessLog.setRows(5);
        jScrollPane1.setViewportView(txtProcessLog);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 481, Short.MAX_VALUE)
                    .addComponent(progStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(progStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 265, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * stand alone entry point which shows this dialog on its own, used to
     * preview the form during development. The dialog is created without a
     * worker, the Nimbus look and feel is selected when available and the
     * virtual machine is terminated once the dialog is closed.
     *
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
            java.util.logging.Logger.getLogger(ProcessProgress.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ProcessProgress.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ProcessProgress.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ProcessProgress.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                ProcessProgress dialog = new ProcessProgress(new javax.swing.JFrame(), true, null);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JProgressBar progStatus;
    private javax.swing.JTextArea txtProcessLog;
    // End of variables declaration//GEN-END:variables
}
