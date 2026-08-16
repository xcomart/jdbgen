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
import java.awt.Frame;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import net.miginfocom.swing.MigLayout;

/**
 * undecorated progress dialog for long running background tasks such as the
 * driver download of the maven explorer. The dialog shows a progress bar and
 * a log area which are fed by an attached <code>Worker</code>, and hides
 * itself as soon as that worker is done.
 *
 * @author comart
 */
public class ProcessProgress extends JDialog {
    
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
    public ProcessProgress(Frame parent, boolean modal, Worker worker) {
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
     * create the components of the dialog and lay them out. The progress bar
     * sits above the log area, both fill the width of the dialog and only the
     * log area grows with it.
     */
    private void initComponents() {
        progStatus = new JProgressBar();
        jScrollPane1 = new JScrollPane();
        txtProcessLog = new JTextArea();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        txtProcessLog.setColumns(20);
        txtProcessLog.setRows(5);
        jScrollPane1.setViewportView(txtProcessLog);

        // bar of a fixed height on top, log area filling the rest. The size of
        // the log area is the one the form was designed with.
        getContentPane().setLayout(new MigLayout(
                "insets dialog, fill, wrap 1", "[grow]", "[][grow]"));
        getContentPane().add(progStatus, "growx, h 33!");
        getContentPane().add(jScrollPane1, "grow, push, w :481:, h :265:");

        pack();
    }

    /** the bar showing how far the attached worker has come. */
    private JProgressBar progStatus;
    /** scroll pane around the log area. */
    private JScrollPane jScrollPane1;
    /** log area collecting the lines published by the attached worker. */
    private JTextArea txtProcessLog;
}
