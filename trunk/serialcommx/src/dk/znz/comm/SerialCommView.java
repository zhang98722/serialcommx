/*
 * SerialCommView.java
 */
package dk.znz.comm;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Enumeration;
import java.util.prefs.Preferences;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * The application's main frame.
 */
public class SerialCommView extends FrameView {

    private SerialWriter m_serialWriter = null;
    private Charset m_charset;
    private Preferences m_preferences;
    private final File m_settingsFile = new File("settings.xml");
    private SerialSetting m_serialSetting;

    public SerialCommView(SingleFrameApplication app) {
        super(app);

        initComponents();

        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String) (evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer) (evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });

        // Loading program settings..
        try {
            Preferences.importPreferences(new BufferedInputStream(new FileInputStream(m_settingsFile)));
        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(this.getComponent(), "Error Loading", "Could not load \"" + m_settingsFile.getName() +"\".", JOptionPane.ERROR_MESSAGE);
            Logger.getLogger(SerialCommView.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
//        } catch (IOException ex) {
//        } catch (InvalidPreferencesFormatException ex) {
            JOptionPane.showMessageDialog(this.getComponent(), "Error Loading", "There was a problem parsing the settings file \"" + m_settingsFile.getName() +"\".\n\nError description:\n" + ex.getMessage(), JOptionPane.ERROR_MESSAGE);
            Logger.getLogger(SerialCommView.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            m_preferences = Preferences.userNodeForPackage(SettingsDialog.class);
            m_serialSetting = new SerialSetting(m_preferences);
        }
    }

    private DefaultComboBoxModel getParityModel() {
        EnumModelEntry[] objects = new EnumModelEntry[] {
            new EnumModelEntry(SerialPort.PARITY_NONE, "None"),
            new EnumModelEntry(SerialPort.PARITY_ODD, "Odd"),
            new EnumModelEntry(SerialPort.PARITY_EVEN, "Even"),
            new EnumModelEntry(SerialPort.PARITY_MARK, "Mark"),
            new EnumModelEntry(SerialPort.PARITY_SPACE, "Space")
        };
        return new DefaultComboBoxModel(objects);
    }

    private DefaultComboBoxModel getDataBitsModel() {
        EnumModelEntry[] objects = new EnumModelEntry[] {
            new EnumModelEntry(SerialPort.DATABITS_5, "5 data bits"),
            new EnumModelEntry(SerialPort.DATABITS_6, "6 data bits"),
            new EnumModelEntry(SerialPort.DATABITS_7, "7 data bits"),
            new EnumModelEntry(SerialPort.DATABITS_8, "8 data bits")
        };
        return new DefaultComboBoxModel(objects);
    }

    private DefaultComboBoxModel getStopBitsModel() {
        EnumModelEntry[] objects = new EnumModelEntry[] {
            new EnumModelEntry(SerialPort.STOPBITS_1, "1 stop bit"),
            new EnumModelEntry(SerialPort.STOPBITS_1_5, "1.5 stop bits"),
            new EnumModelEntry(SerialPort.STOPBITS_2, "2 stop bits")
        };
        return new DefaultComboBoxModel(objects);
    }

    private DefaultComboBoxModel getFlowcontrolModel() {
        EnumModelEntry[] objects = new EnumModelEntry[] {
            new EnumModelEntry(SerialPort.FLOWCONTROL_NONE, "None"),
                    new EnumModelEntry(SerialPort.FLOWCONTROL_RTSCTS_IN, "RTSCTS_IN"),
                    new EnumModelEntry(SerialPort.FLOWCONTROL_RTSCTS_OUT, "RTSCTS_OUT"),
                    new EnumModelEntry(SerialPort.FLOWCONTROL_XONXOFF_IN, "XONXOFF_IN"),
                    new EnumModelEntry(SerialPort.FLOWCONTROL_XONXOFF_OUT, "XONXOFF_OUT")
        };
        return new DefaultComboBoxModel(objects);
    }
    
    private DefaultComboBoxModel getCharsetModel() {
        return new DefaultComboBoxModel(Charset.availableCharsets().keySet().toArray(new String[0]));
    }

    @Action
    private void connect() {
        InputStream in = null;
        try {
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(m_serialSetting.getPort());
            if (portIdentifier.isCurrentlyOwned()) {
                System.out.println("Error: Port is currently in use");
            } else {
                CommPort commPort = portIdentifier.open(m_serialSetting.getPort(), 2000);
                if (commPort instanceof SerialPort) {
                    SerialPort serialPort = (SerialPort) commPort;
                    serialPort.setSerialPortParams(
                            m_serialSetting.getBaudRate(),
                            m_serialSetting.getDataBits(),
                            m_serialSetting.getStopBits(),
                            m_serialSetting.getParity()
                            );
                    in = serialPort.getInputStream();
                    SerialReader reader = new SerialReader(in, outputTextArea);
                    m_charset = Charset.forName(charsetComboBox.getSelectedItem().toString());
                    m_serialWriter = new SerialWriter(serialPort.getOutputStream(), m_charset);
                    serialPort.addEventListener(reader);
                    serialPort.notifyOnDataAvailable(true);
                } else {
                    System.out.println("Error: Only serial ports are handled by this example.");
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this.getFrame(), ex.getMessage(), "Error while connecting..", JOptionPane.ERROR_MESSAGE);
            Logger.getLogger(SerialCommView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void updateCommPorts(DefaultComboBoxModel modelToUpdate) {
        Enumeration<CommPortIdentifier> e = CommPortIdentifier.getPortIdentifiers();
        CommPortIdentifier[] commPortIdentifiers = Collections.list(e).toArray(new CommPortIdentifier[0]);
        modelToUpdate.removeAllElements();
        for (int i = 0; i < commPortIdentifiers.length; i++) {
            CommPortIdentifier commPortIdentifier = commPortIdentifiers[i];
            modelToUpdate.addElement(commPortIdentifier.getName());
        }
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = SerialCommApp.getApplication().getMainFrame();
            aboutBox = new SerialCommAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        SerialCommApp.getApplication().show(aboutBox);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        comPortComboBox = new javax.swing.JComboBox();
        baudRateComboBox = new javax.swing.JComboBox();
        jScrollPane1 = new javax.swing.JScrollPane();
        outputTextArea = new javax.swing.JTextArea();
        connectButton = new javax.swing.JButton();
        dataBitsComboBox = new javax.swing.JComboBox();
        parityComboBox = new javax.swing.JComboBox();
        stopBitsComboBox = new javax.swing.JComboBox();
        flowControlComboBox = new javax.swing.JComboBox();
        inputTextField = new javax.swing.JTextField();
        sendButton = new javax.swing.JButton();
        charsetComboBox = new javax.swing.JComboBox();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();

        mainPanel.setName("mainPanel"); // NOI18N

        comPortComboBox.setEditable(true);
        comPortComboBox.setModel(new DefaultComboBoxModel());
        comPortComboBox.setName("comPortComboBox"); // NOI18N
        updateCommPorts((DefaultComboBoxModel)comPortComboBox.getModel());
        comPortComboBox.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
                comPortComboBoxPopupMenuWillBecomeVisible(evt);
            }
        });

        baudRateComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "600", "1200", "2400", "4800", "9600", "14400", "19200", "28800", "38400", "56000", "57600", "115200", "128000", "256000" }));
        baudRateComboBox.setName("baudRateComboBox"); // NOI18N

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        outputTextArea.setColumns(20);
        outputTextArea.setRows(5);
        outputTextArea.setName("outputTextArea"); // NOI18N
        jScrollPane1.setViewportView(outputTextArea);

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(dk.znz.comm.SerialCommApp.class).getContext().getResourceMap(SerialCommView.class);
        connectButton.setText(resourceMap.getString("connectButton.text")); // NOI18N
        connectButton.setName("connectButton"); // NOI18N
        connectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectButtonActionPerformed(evt);
            }
        });

        dataBitsComboBox.setModel(getDataBitsModel());
        dataBitsComboBox.setName("dataBitsComboBox"); // NOI18N

        parityComboBox.setModel(getParityModel());
        parityComboBox.setName("parityComboBox"); // NOI18N

        stopBitsComboBox.setModel(getStopBitsModel());
        stopBitsComboBox.setName("stopBitsComboBox"); // NOI18N

        flowControlComboBox.setModel(getFlowcontrolModel());
        flowControlComboBox.setName("flowControlComboBox"); // NOI18N

        inputTextField.setName("inputTextField"); // NOI18N

        sendButton.setText(resourceMap.getString("sendButton.text")); // NOI18N
        sendButton.setName("sendButton"); // NOI18N
        sendButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendButtonActionPerformed(evt);
            }
        });

        charsetComboBox.setModel(getCharsetModel());
        charsetComboBox.setName("charsetComboBox"); // NOI18N

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 425, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, mainPanelLayout.createSequentialGroup()
                        .addComponent(connectButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comPortComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(baudRateComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(dataBitsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(parityComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(stopBitsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(flowControlComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(charsetComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(inputTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 362, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sendButton)))
                .addContainerGap())
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(connectButton)
                    .addComponent(comPortComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(baudRateComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(dataBitsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(parityComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(stopBitsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(flowControlComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(charsetComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 174, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sendButton)
                    .addComponent(inputTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(dk.znz.comm.SerialCommApp.class).getContext().getActionMap(SerialCommView.class, this);
        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 445, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 275, Short.MAX_VALUE)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3))
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

    private void comPortComboBoxPopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_comPortComboBoxPopupMenuWillBecomeVisible
        // FIXME
        //updateCommPorts((DefaultComboBoxModel)comPortComboBox.getModel());
    }//GEN-LAST:event_comPortComboBoxPopupMenuWillBecomeVisible

    private void connectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectButtonActionPerformed
        connect();
    }//GEN-LAST:event_connectButtonActionPerformed

    private void sendButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendButtonActionPerformed
        try {
            String output = m_serialWriter.send(inputTextField.getText());
            outputTextArea.append(output);
        } catch (IOException ex) {
            Logger.getLogger(SerialCommView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_sendButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox baudRateComboBox;
    private javax.swing.JComboBox charsetComboBox;
    private javax.swing.JComboBox comPortComboBox;
    private javax.swing.JButton connectButton;
    private javax.swing.JComboBox dataBitsComboBox;
    private javax.swing.JComboBox flowControlComboBox;
    private javax.swing.JTextField inputTextField;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JTextArea outputTextArea;
    private javax.swing.JComboBox parityComboBox;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JButton sendButton;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JComboBox stopBitsComboBox;
    // End of variables declaration//GEN-END:variables
    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;
    private JDialog aboutBox;
}


