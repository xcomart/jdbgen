/*
 * MIT License
 * 
 * Copyright (c) 2020 Dennis Soungjin Park
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package comart.utils;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import comart.tools.jdbgen.types.HasIcon;
import comart.tools.jdbgen.types.HasTitle;
import comart.utils.tuple.Pair;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;
import javax.imageio.ImageIO;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import jiconfont.IconCode;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.ObjectUtils;

/**
 * The Swing helpers of the application: look and feel switching, icons, the
 * standard dialogs and a handful of table and file chooser conveniences.
 *
 * <p>Two registries are kept so that a look and feel or font size change can be
 * applied to what is already on screen: every component an icon was put on with
 * {@link #addIcon(JComponent, IconCode)}, and every frame registered with
 * {@link #registerFrame(java.awt.Container)}. Resolved icons are cached by their
 * path and the cache is dropped whenever the look and feel changes.</p>
 *
 * <p>The dialogs may be called from a background thread: they are run on the
 * Event Dispatch Thread and block until the user answered.</p>
 */
@Slf4j
public class UIUtils {
    /** size the font icons are built in, taken from the look and feel. */
    private static int fontSize = 14;
    /** colour the font icons are built in, taken from the look and feel. */
    private static Color color = null;
    /** every component a font icon was put on, and the glyph it carries. */
    private static final Set<Pair<JComponent, IconCode>> items = new HashSet();
    /** the windows to update when the look and feel changes. */
    private static final Set<Container> frames = new HashSet<>();
    /** resolved icons by their configured path, dropped on a theme change. */
    private static final Map<String, Icon> cachedIcon = new HashMap<>();
    /** defaults of the look and feel in effect. */
    private static UIDefaults uiDefaults = null;

    /** makes the Font Awesome glyphs available to the icon font builder. */
    static {
        IconFontSwing.register(FontAwesome.getIconFont());
    }

    /**
     * this class only holds <code>static</code> methods.
     */
    public UIUtils() {
    }

    /**
     * switch to the look and feel <code>className</code> names, falling back to
     * the system look and feel when it cannot be installed. The icon cache is
     * dropped, the icon colour and font size are taken from the new defaults,
     * every registered icon is rebuilt and every registered frame is updated,
     * so the change is visible right away.
     *
     * @param className
     *            fully qualified name of the <code>LookAndFeel</code> class.
     */
    @SuppressWarnings("UseSpecificCatch")
    public static void setLAF(String className) {
        boolean hasSet = false;
        
        cachedIcon.clear();

        try {
            UIManager.setLookAndFeel(className);
            hasSet = true;
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
        }

        if (!hasSet) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                log.error(e.getLocalizedMessage(), e);
            }
        }
        
        uiDefaults = UIManager.getLookAndFeelDefaults();

        color = uiDefaults.getColor("Button.foreground");
        fontSize = uiDefaults.getFont("Button.font").getSize();
        items.forEach((t) -> {
            addIconPrivate((JComponent)t.getFirst(), (IconCode)t.getSecond());
        });
        frames.forEach((f) -> {
            try {
                SwingUtilities.updateComponentTreeUI(f);
            } catch (Exception e) {
                log.warn("cannot update component tree of {}: {}",
                        f.getClass().getName(), e.getLocalizedMessage());
            }
        });
    }

    /**
     * run <code>task</code> on the Event Dispatch Thread and return its result.
     * Several callers live on SwingWorker background threads or on the main
     * thread, and Swing dialogs must not be created off the EDT.
     *
     * @param task
     *            the work to do on the EDT.
     * @param <T>
     *            result type of the task.
     * @return whatever the task returned.
     * @throws RuntimeException
     *             wrapping anything the task threw, and the interruption of the
     *             calling thread.
     */
    @SuppressWarnings("unchecked")
    private static <T> T onEdt(Callable<T> task) {
        if (SwingUtilities.isEventDispatchThread()) {
            try {
                return task.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        final Object[] res = new Object[1];
        final Exception[] err = new Exception[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    res[0] = task.call();
                } catch (Exception e) {
                    err[0] = e;
                }
            });
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ie);
        } catch (InvocationTargetException ite) {
            throw new RuntimeException(ite.getCause());
        }
        if (err[0] != null)
            throw new RuntimeException(err[0]);
        return (T)res[0];
    }

    /**
     * scale an image to icon size, which is 1.2 times the current font size in
     * both directions, smoothly.
     *
     * @param image
     *            the image to scale.
     * @return the scaled image.
     */
    public static Image resize(Image image) {
        return image.getScaledInstance((int)(fontSize * 1.2), (int)(fontSize * 1.2), 4);
    }

    /**
     * switch to the FlatLaf dark theme, telling macOS to use its dark
     * appearance for the window decorations as well.
     */
    public static void setFlatDarkLaf() {
        setLAF(FlatDarkLaf.class.getName());
        System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua");
    }

    /**
     * switch to the FlatLaf light theme, telling macOS to use its light
     * appearance for the window decorations as well.
     */
    public static void setFlatLightLaf() {
        setLAF(FlatLightLaf.class.getName());
        System.setProperty("apple.awt.application.appearance", "NSAppearanceNameAqua");
    }

    /**
     * remember a window so that its component tree is updated on the next
     * {@link #setLAF(String)}.
     *
     * @param frame
     *            the window or container to keep in step with the look and
     *            feel.
     */
    public static void registerFrame(Container frame) {
        frames.add(frame);
    }

    /**
     * rebuild every registered icon after the user interface font changed. The
     * new size is read from the current <code>Button.font</code> default.
     *
     * @param size
     *            not used; the size in effect is taken from the look and feel
     *            defaults.
     */
    public static void fontSizeChanged(int size) {
        fontSize = UIManager.getFont("Button.font").getSize();
        items.forEach((t) -> {
            addIconPrivate((JComponent)t.getFirst(), (IconCode)t.getSecond());
        });
    }

    /**
     * give a component a font icon and a label. Both are set reflectively, so
     * a component without <code>setText</code> or <code>setIcon</code> is left
     * as it is.
     *
     * @param button
     *            the component to decorate.
     * @param code
     *            the icon font glyph.
     * @param text
     *            the label to set.
     */
    public static void applyIcon(JComponent button, IconCode code, String text) {
        try {
            Method setText = button.getClass().getMethod("setText", new Class[]{String.class});
            setText.invoke(button, text);
        } catch(Throwable ignored) {}
        addIcon(button, code);
    }

    /**
     * turn a component into an icon only button: the label is cleared and, for
     * a <code>JButton</code>, the margin is tightened to five pixels.
     *
     * @param button
     *            the component to decorate.
     * @param code
     *            the icon font glyph.
     */
    public static void applyIcon(JComponent button, IconCode code) {
        applyIcon(button, code, "");
        if (button instanceof JButton) {
            ((JButton)button).setMargin(new Insets(5, 5, 5, 5));
        }
    }

    /**
     * build the glyph in the current icon size and colour and hand it to
     * <code>setIcon</code>, reflectively so that anything having such a method
     * can be used. A component without one is left as it is.
     */
    private static void addIconPrivate(JComponent button, IconCode code) {
        try {
            Method setIcon = button.getClass().getMethod("setIcon", new Class[]{Icon.class});
            setIcon.invoke(button, IconFontSwing.buildIcon(code, (float)(fontSize), color));
        } catch (Exception ignored) {}

    }

    /**
     * put a font icon on a component and remember the pair, so that the icon is
     * rebuilt whenever the look and feel or the font size changes.
     *
     * @param button
     *            the component to decorate.
     * @param code
     *            the icon font glyph.
     */
    public static void addIcon(JComponent button, IconCode code) {
        addIconPrivate(button, code);
        items.add(new Pair(button, code));
    }
    
    /**
     * make a table keep what is being typed when it loses the focus, instead of
     * discarding the cell editor.
     *
     * @param table
     *            the table to configure.
     */
    public static void setCommitOnLostFocus(JTable table) {
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    }
    
    /**
     * The icon a configured icon path names, resolved once and cached
     * afterwards.
     *
     * <p>Four prefixes are understood, and anything else is taken for a file
     * path resolved with {@link AppDirs#resolvePath(String)}:</p>
     * <ul>
     *   <li><code>stock:</code> - an image below <code>/icons/</code> of the
     *       class path</li>
     *   <li><code>http</code> - an image downloaded from that address</li>
     *   <li><code>fa:</code> - a Font Awesome glyph, by its name</li>
     *   <li><code>color:</code> - a filled circle in the named
     *       {@link Color} constant</li>
     * </ul>
     *
     * @param path
     *            the configured icon path. An empty one selects the generic
     *            icon.
     * @return the icon, or the generic icon when it cannot be read.
     */
    @SuppressWarnings({"null", "UseSpecificCatch"})
    public static synchronized Icon getIcon(String path) {
        Icon res = null;
        if (!cachedIcon.containsKey(path)) {
            boolean isStock = path.toLowerCase().startsWith("stock:");
            boolean isUrl = path.toLowerCase().startsWith("http");
            boolean isFA = path.toLowerCase().startsWith("fa:");
            boolean isCol = path.toLowerCase().startsWith("color:");
            String npath = path;
            if (isStock) {
                npath = "/icons/" + path.substring(6);
            }

            try {
                if (isUrl) {
                    OkHttpClient client = HttpUtils.getClient();
                    Request req = new Request.Builder().url(path).build();
                    try (Response response = client.newCall(req).execute();
                            InputStream is = response.body().byteStream()) {
                        res = new ImageIcon(resize(ImageIO.read(is)));
                    }
                } else if (isFA) {
                    IconCode code = FontAwesome.valueOf(npath.substring(3).toUpperCase());
                    res = IconFontSwing.buildIcon( code, (float)fontSize, color);
                } else if (isCol) {
                    String colName = path.substring(6);
                    Color col = (Color)Color.class.getDeclaredField(colName.toUpperCase()).get(null);
                    res = IconFontSwing.buildIcon( FontAwesome.CIRCLE, (float)(fontSize * 1.2), col);
                } else {
                    boolean isBlank = StrUtils.isEmpty(path);
                    if (isBlank)
                        npath = "/icons/generic.png";
                    try (InputStream is = (isStock || isBlank)
                            ? UIUtils.class.getResourceAsStream(npath)
                            : new FileInputStream(AppDirs.resolvePath(path))) {
                        res = new ImageIcon(resize(ImageIO.read((InputStream)is)));
                    }
                }
            } catch (Exception e) {
                if (!"/icons/generic.png".equals(npath)) {
                    log.info("Icon not found. Falling back to use default icon.");
                    res = getIcon("stock:generic.png");
                } else {
                    log.error("cannot read default icon. installation may corrupted.", e);
                }
            }

            if (res != null) {
                cachedIcon.put(path, res);
            }
        } else {
            res = (Icon)cachedIcon.get(path);
        }

        return (Icon)res;
    }
    
    /**
     * renders a list element through the {@link HasTitle} - and optionally
     * {@link HasIcon} - view a function returns for it.
     */
    private static class MyListCellRenderer<T> extends DefaultListCellRenderer {
        /** maps an element of the list to what is to be shown for it. */
        private final Function<T,HasTitle> func;

        /**
         * @param func
         *            maps an element to its title carrier, may return
         *            <code>null</code>.
         */
        public MyListCellRenderer(Function<T,HasTitle> func) {
            this.func = func;
        }
        
        /**
         * label the cell with the title, and the icon where there is one, of
         * the view <code>func</code> returns for the element.
         *
         * @return the configured label.
         */
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            HasTitle lb = func.apply((T)value);
            if (lb != null) {
                if (lb instanceof HasIcon) {
                    HasIcon hi = (HasIcon)lb;
                    String icon = hi.getIcon();
                    if (!StrUtils.isEmpty(icon)) {
                        label.setIcon(UIUtils.getIcon(hi.getIcon()));
                    }
                }
                label.setText(lb.getTitle());
            }
            return label;
        }
        
    }
    
    /**
     * a list renderer that shows the title, and the icon where there is one, of
     * whatever <code>func</code> makes of an element.
     *
     * @param func
     *            maps a list element to its title carrier. It may return
     *            <code>null</code>, which leaves the default rendering alone.
     * @param <T>
     *            element type of the list.
     * @return the renderer.
     */
    public static <T> ListCellRenderer<T> getListCellRenderer(Function<T,HasTitle> func) {
        return (ListCellRenderer<T>) new MyListCellRenderer<>(func);
    }
    
    /**
     * keep an empty row at the end of an editable table, so that a new entry
     * can always be typed without pressing an "add" button. A row is appended
     * whenever the last one holds anything.
     *
     * @param table
     *            the table, whose model has to be a
     *            <code>DefaultTableModel</code>.
     */
    public static void applyTableEdit(JTable table) {
        DefaultTableModel tmodel = (DefaultTableModel)table.getModel();
        tmodel.addTableModelListener(e -> {
            int ridx = tmodel.getRowCount() - 1;
            boolean needToAdd = ridx < 0;

            if (!needToAdd) {
                for(int i = 0; i < tmodel.getColumnCount(); ++i) {
                    if (ObjectUtils.isNotEmpty(tmodel.getValueAt(ridx, i))) {
                        needToAdd = true;
                        break;
                    }
                }
            }

            if (needToAdd) {
                EventQueue.invokeLater(() -> {
                    tmodel.addRow(new String[tmodel.getColumnCount()]);
                });
            }
        });
    }
    
    /**
     * show an error dialog and log the message. May be called from any thread.
     *
     * @param parent
     *            the component the dialog is centred on, may be
     *            <code>null</code>.
     * @param message
     *            the text to show.
     */
    public static void error(Component parent, String message) {
        onEdt(() -> {
            JOptionPane.showMessageDialog(parent, message,
                    I18n.t("common.dialog.error.title"), JOptionPane.ERROR_MESSAGE);
            return null;
        });
        log.warn(message);
    }

    /**
     * show an information dialog and log the message. May be called from any
     * thread.
     *
     * @param parent
     *            the component the dialog is centred on, may be
     *            <code>null</code>.
     * @param message
     *            the text to show.
     */
    public static void info(Component parent, String message) {
        onEdt(() -> {
            JOptionPane.showMessageDialog(parent, message,
                    I18n.t("common.dialog.info.title"), JOptionPane.INFORMATION_MESSAGE);
            return null;
        });
        log.info(message);
    }

    /**
     * ask the user an OK/Cancel question and log the answer. May be called from
     * any thread; it blocks until the dialog is answered.
     *
     * @param parent
     *            the component the dialog is centred on, may be
     *            <code>null</code>.
     * @param title
     *            title of the dialog.
     * @param message
     *            the question to ask.
     * @return the user confirmed or not.
     */
    public static boolean confirm(Component parent, String title, String message) {
        boolean res = onEdt(() -> JOptionPane.showConfirmDialog(
                parent, message, title, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION);
        log.info("{}: {}", message, res);
        return res;
    }
    
    /**
     * the password prompt: one field, plus a confirmation field when a password
     * is being set for the first time. The dialog is repeated until both fields
     * match or the user cancels.
     */
    private static class PasswordPanel extends JPanel {
        /** what the user entered, <code>null</code> when they cancelled. */
        private final String password;

        /**
         * build the panel and run the dialog to its end, leaving the answer in
         * {@link #password}.
         *
         * @param prompt
         *            title of the dialog.
         * @param isFirst
         *            add a confirmation field and insist that both entries
         *            match.
         */
        private PasswordPanel(String prompt, boolean isFirst) {
            super(new FlowLayout());
            JPasswordField pwdField = new JPasswordField(20);
            add(new JLabel(I18n.t("common.password.label"), null, JLabel.LEADING));
            add(pwdField);
            JPasswordField confirmField = null;
            if (isFirst){
                add(new JLabel(I18n.t("common.password.confirm.label"), null, JLabel.LEADING));
                confirmField = new JPasswordField(20);
                add(confirmField);
            }
            JOptionPane joptionPane = new JOptionPane(this, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
            boolean responseOK = false;
            boolean isSame = true;
            do {
                // getValue() is null when the dialog is dismissed with the window
                // close button, so compare null-safely
                responseOK = Integer.valueOf(JOptionPane.OK_OPTION)
                        .equals(configure(joptionPane, prompt, pwdField));
                if (isFirst) {
                    String pass = String.valueOf(pwdField.getPassword());
                    String conf = String.valueOf(confirmField.getPassword());
                    isSame = pass.equals(conf);
                    if (!isSame)
                        UIUtils.error(null, I18n.t("common.password.mismatch"));
                }
            } while (responseOK && !isSame);
            this.password = responseOK ? String.valueOf(pwdField.getPassword()) : null;
        }

        /**
         * prompt for a password.
         *
         * @param message
         *            title of the dialog.
         * @param isFirst
         *            ask for a confirmation of the password as well.
         * @return the password, or <code>null</code> when the user cancelled.
         */
        public static String getPassword(String message, boolean isFirst){
            return new PasswordPanel(message, isFirst).password;
        }

        /**
         * show the dialog once and dispose of it afterwards.
         *
         * @param jOptionPane
         *            the option pane holding this panel.
         * @param prompt
         *            title of the dialog.
         * @param pwdField
         *            the field to put the caret in.
         * @return the button the user pressed, <code>null</code> when the
         *         dialog was closed with the window button.
         */
        private Object configure(JOptionPane jOptionPane, String prompt, JPasswordField pwdField) {
            JDialog jDialog = promptDialog(prompt, jOptionPane, pwdField);
            Object result = jOptionPane.getValue();
            jDialog.dispatchEvent(new WindowEvent(jDialog, WindowEvent.WINDOW_CLOSING));
            jDialog.dispose();
            return result;
        }

        /**
         * show the modal dialog, putting the focus into the password field
         * every time the window gains it.
         *
         * @param message
         *            title of the dialog.
         * @param jOptionPane
         *            the option pane to build the dialog from.
         * @param pwdField
         *            the field to focus.
         * @return the dialog, which has been closed by the time this returns.
         */
        private JDialog promptDialog(String message, JOptionPane jOptionPane, JComponent pwdField) {
            JDialog dialog = jOptionPane.createDialog(message);
            dialog.addWindowFocusListener(new WindowAdapter() {
                @Override
                public void windowGainedFocus(WindowEvent e) {
                    pwdField.requestFocusInWindow();
                }
            });
            dialog.setVisible(true);
            return dialog;
        }
    }

    /**
     * ask the user for the master password. May be called from any thread; it
     * blocks until the dialog is answered.
     *
     * @param message
     *            title of the dialog.
     * @param isFirst
     *            a password is being set for the first time, so ask for it
     *            twice and insist that both entries match.
     * @return the password, or <code>null</code> when the user cancelled.
     */
    public static String password(String message, boolean isFirst) {
        return onEdt(() -> PasswordPanel.getPassword(message, isFirst));
    }
    
    /**
     * run a task in the background while a loading animation covers
     * <code>parent</code> as its glass pane. Returns as soon as the task was
     * started. When the glass pane cannot be installed the task is still run,
     * on a plain thread, because it is expected to block; a failure of the task
     * itself is logged.
     *
     * @param parent
     *            the window to cover, which has to have a
     *            <code>setGlassPane</code> method.
     * @param worker
     *            the task to run off the Event Dispatch Thread.
     */
    public static void loading(Window parent, Runnable worker) {
        try {
            JComponent gpanel = new JLabel(new ImageIcon(
                    AppDirs.installResourceFile("resource/loading.gif").getAbsolutePath()));
            Method m = parent.getClass().getMethod("setGlassPane", Component.class);
            m.invoke(parent, gpanel);
//            parent.setGlassPane(gpanel);
            SwingWorker<Boolean,String> sworker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    worker.run();
                    return true;
                }

                @Override
                protected void done() {
                    gpanel.setVisible(false);
                    try {
                        get();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("background task interrupted", ie);
                    } catch (Exception e) {
                        log.error("background task failed", e);
                    }
                }
            };
            gpanel.setVisible(true);
            sworker.execute();
        } catch (Exception ex) {
            log.error("cannot show loading glass pane, running the task directly.", ex);
            // must not run on the EDT - the task is expected to block
            new Thread(worker).start();
        }
    }
    
    /**
     * ask for a file, starting in the working directory and storing the result
     * relative to a known directory where possible.
     *
     * @param parent
     *            the component the chooser is centred on.
     * @return the chosen path, or <code>null</code> when the user cancelled.
     */
    public static String openFileDlg(Component parent) {
        return openFileDlg(parent, "", true);
    }
    
    /**
     * ask for a file of any type.
     *
     * @param parent
     *            the component the chooser is centred on.
     * @param startPath
     *            the directory to start in.
     * @param relative
     *            store the result relative to the user data directory or the
     *            installation when it lies below one of them, see
     *            {@link AppDirs#relativize(String)}.
     * @return the chosen path, or <code>null</code> when the user cancelled.
     */
    public static String openFileDlg(Component parent, String startPath, boolean relative) {
        return openFileDlg(parent, startPath, relative, null, null);
    }
    
    /**
     * ask for a file, optionally offering an extension filter.
     *
     * @param parent
     *            the component the chooser is centred on.
     * @param startPath
     *            the directory to start in.
     * @param relative
     *            store the result relative to the user data directory or the
     *            installation when it lies below one of them, see
     *            {@link AppDirs#relativize(String)}.
     * @param fileTypeName
     *            label of the extension filter. No filter is added unless both
     *            this and <code>fileTypes</code> are given.
     * @param fileTypes
     *            the extensions the filter accepts, without a leading dot.
     * @return the chosen path, or <code>null</code> when the user cancelled.
     */
    public static String openFileDlg(Component parent, String startPath, boolean relative, String fileTypeName, String []fileTypes) {
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File(startPath));
        if (fileTypeName != null && fileTypes != null) {
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                fileTypeName, fileTypes);
            fc.addChoosableFileFilter(filter);
        }
        if (fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            String fpath = fc.getSelectedFile().getAbsolutePath();
            // a template or an icon below the user data or the installation
            // directory is stored relative to it, see AppDirs.resolve() - which
            // is what reads it back
            return relative ? AppDirs.relativize(fpath) : fpath;
        } else {
            return null;
        }
    }

    /**
     * ask for an image file, offering the image formats the icons may be in.
     *
     * @param parent
     *            the component the chooser is centred on.
     * @param startPath
     *            the directory to start in.
     * @return the chosen path, stored relative to a known directory where
     *         possible, or <code>null</code> when the user cancelled.
     */
    public static String openIconDlg(Component parent, String startPath) {
        return openFileDlg(parent, startPath, true, I18n.t("common.filechooser.imageFilter"),
                new String[]{"jpg", "jpeg", "tiff", "tif", "gif", "png", "ico"});
    }
    
    /**
     * ask for a directory, starting in the working directory and storing the
     * result relative to a known directory where possible.
     *
     * @param parent
     *            the component the chooser is centred on.
     * @return the chosen path, or <code>null</code> when the user cancelled.
     */
    public static String openDirDlg(Component parent) {
        return openDirDlg(parent, "", true);
    }
    
    /**
     * ask for a directory.
     *
     * @param parent
     *            the component the chooser is centred on.
     * @param startPath
     *            the directory to start in.
     * @param relative
     *            store the result relative to the user data directory or the
     *            installation when it lies below one of them, see
     *            {@link AppDirs#relativize(String)}.
     * @return the chosen path, or <code>null</code> when the user cancelled.
     */
    public static String openDirDlg(Component parent, String startPath, boolean relative) {
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File(startPath));
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            String fpath = fc.getSelectedFile().getAbsolutePath();
            // a directory below the user data or the installation directory is
            // stored relative to it, the same way openFileDlg() does it - it is
            // AppDirs.resolve() that reads it back, so relativizing against the
            // working directory instead would name a different directory on the
            // next start
            return relative ? AppDirs.relativize(fpath) : fpath;
        } else {
            return null;
        }
    }

    /**
     * read a two column key/value table into a map, keeping the order of the
     * rows and skipping every row whose key or value is empty.
     *
     * @param model
     *            the table model, column 0 holding the keys and column 1 the
     *            values.
     * @return the entries of the table.
     */
    public static Map<String, String> applyTableToMap(TableModel model) {
        Map<String, String> props = new LinkedHashMap<>();
        for (int i=0; i<model.getRowCount(); i++) {
            String k = (String)model.getValueAt(i, 0);
            String v = (String)model.getValueAt(i, 1);
            if (!StrUtils.isEmpty(k) && !StrUtils.isEmpty(v))
                props.put(k, v);
        }
        return props;
    }
    /**
     * {@link #tableSetLastEmpty(TableModel, int)} for a table whose key column
     * is the first one.
     *
     * @param model
     *            the table model to append an empty row to.
     */
    public static void tableSetLastEmpty(TableModel model) {
        tableSetLastEmpty(model, 0);
    }
    
    /**
     * append an empty row to a key/value table when its last row is filled in,
     * so that there is always a row to type a new entry into. The columns
     * before <code>stCol</code> are check box columns and are added as
     * <code>Boolean.FALSE</code>.
     *
     * @param model
     *            the table model, which has to be a
     *            <code>DefaultTableModel</code>.
     * @param stCol
     *            index of the key column; the value column is the next one.
     */
    public static void tableSetLastEmpty(TableModel model, int stCol) {
        boolean lastEmpty = false;
        for (int i=0; i<model.getRowCount(); i++) {
            String k = (String)model.getValueAt(i, stCol);
            String v = (String)model.getValueAt(i, stCol+1);
            if (!StrUtils.isEmpty(k) && !StrUtils.isEmpty(v))
                lastEmpty = false;
            else
                lastEmpty = true;
        }
        if (!lastEmpty) {
            ArrayList<Object> arr = new ArrayList<>();
            arr.add(""); arr.add("");
            for (int i=0; i<stCol; i++)
                arr.add(0, Boolean.FALSE);
            
            ((DefaultTableModel)model).addRow(arr.toArray());
        }
    }
    
    /**
     * check that a text field or text area was filled in, telling the user
     * which one is missing when it was not. Anything else passes.
     *
     * @param parent
     *            the component an error dialog is centred on.
     * @param target
     *            the input to check. It is named in the error message by its
     *            tool tip, or by its component name when it has none.
     * @return the input is filled in or not.
     */
    public static boolean checkNotEmpty(Component parent, JComponent target) {
        boolean isOk = true;
        if (target instanceof JTextField) {
            isOk = !StrUtils.isEmpty(((JTextField) target).getText());
        } else if (target instanceof JTextArea) {
            isOk = !StrUtils.isEmpty(((JTextArea) target).getText());
        }
        if (!isOk) {
            String name = target.getToolTipText();
            if (name == null)
                name = target.getName();
            UIUtils.error(parent, I18n.t("common.validation.required", name));
        }
        return isOk;
    }
    
    /**
     * check several inputs in order, stopping at the first empty one.
     *
     * @param parent
     *            the component an error dialog is centred on.
     * @param targets
     *            the inputs to check.
     * @return every input is filled in or not.
     */
    public static boolean checkNotEmpty(Component parent, JComponent[] targets) {
        for (JComponent t:targets) {
            if (!checkNotEmpty(parent, t))
                return false;
        }
        return true;
    }
    
    /**
     * set the tool tip of the template table to the name, the template file and
     * the output of the row the mouse is over, and clear it outside of the
     * rows.
     *
     * @param tabTemplates
     *            the template table.
     * @param baseidx
     *            index of the name column; the template file and the output are
     *            the two columns after it.
     * @param evt
     *            the mouse event the tool tip is asked for.
     */
    public static void templateTooltip(JTable tabTemplates, int baseidx, MouseEvent evt) {
        Point p = evt.getPoint();
        int row = tabTemplates.rowAtPoint(p);
        if (row < 0) {
            tabTemplates.setToolTipText(null);
            return;
        }
        String name = (String)tabTemplates.getValueAt(row, baseidx);
        String tfile = (String)tabTemplates.getValueAt(row, baseidx+1);
        String tout = (String)tabTemplates.getValueAt(row, baseidx+2);
        tabTemplates.setToolTipText(I18n.t("common.template.tooltip", name, tfile, tout));
    }
    
    /**
     * make a button open the documentation of the icon paths.
     *
     * @param btn
     *            the help button.
     */
    public static void iconHelpAction(JButton btn) {
        btn.addActionListener(e -> PlatformUtils.openDoc("icons.md"));
    }

    /**
     * make a button open the template reference documentation.
     *
     * @param btn
     *            the help button.
     */
    public static void templateHelpAction(JButton btn) {
        btn.addActionListener(e -> PlatformUtils.openDoc("template-reference.md"));
    }
    
    /**
     * make a button square, as high as the component it sits next to.
     *
     * @param ref
     *            the component whose height is taken.
     * @param btn
     *            the button to size.
     */
    public static void fitButton(JComponent ref, JButton btn) {
        int size = ref.getHeight();
        btn.setSize(size, size);
    }
    
    /**
     * give a window the application icon. A failure to read the icon is logged
     * and the window keeps the default one.
     *
     * @param wnd
     *            the window to decorate.
     */
    public static void setApplicationIcon(Window wnd) {
        try {
            wnd.setIconImage(ImageIO.read(AppDirs.installResourceFile("resource/icon.png")));
        } catch (IOException ex) {
            log.error(ex.getLocalizedMessage(), ex);
        }
    }
}
