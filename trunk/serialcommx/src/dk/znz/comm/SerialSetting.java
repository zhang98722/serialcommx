package dk.znz.comm;

import gnu.io.SerialPort;
import java.io.Serializable;
import java.util.prefs.Preferences;

/**
 *
 * @author Esben
 */
public class SerialSetting implements Serializable {
    private static final long serialVersionUID = 6792253358427640124L;

    private String port;
    private int baudRate;
    private int dataBits;
    private int parity;
    private int stopBits;
    private int flowControl;

    private String append;
    private boolean echo;

    public SerialSetting(String port, int baudRate, int dataBits, int parity, int stopBits, int flowControl, String append, boolean echo) {
        this.port = port;
        this.baudRate = baudRate;
        this.dataBits = dataBits;
        this.parity = parity;
        this.stopBits = stopBits;
        this.flowControl = flowControl;
        
        this.append = append;
        this.echo = echo;
    }

    /**
     * Default constructor, creates a default serial setting.
     * Baud rate:    115200
     * Data bits:         8
     * Parity:         none
     * Stop bits:         1
     * Flow control:   none
     *
     * Append:         \r\n
     * Echo:           true
     */
    public SerialSetting() {
        // FIXME: default port should be the first from the comm port list
        // CommPortIdentifier.getPortIdentifiers()
        port = "";
        baudRate = 115200;
        dataBits = SerialPort.DATABITS_8;
        parity = SerialPort.PARITY_NONE;
        stopBits = SerialPort.STOPBITS_1;
        flowControl = SerialPort.FLOWCONTROL_NONE;

        append = "\r\n";
        echo = true;
    }

    /**
     * Default constructor, creates a default serial setting.
     * Baud rate:    115200
     * Data bits:         8
     * Parity:         none
     * Stop bits:         1
     * Flow control:   none
     *
     * Append:         \r\n
     * Echo:           true
     *
     * @param preferences The preferences node which contains the settings.
     */
    public SerialSetting(Preferences preferences) {
        port = preferences.get("CONNECTION_PORT", "");
        baudRate = preferences.getInt("CONNECTION_BAUD_RATE", 115200);
        dataBits = preferences.getInt("CONNECTION_DATA_BITS", SerialPort.DATABITS_8);
        parity = preferences.getInt("CONNECTION_PARITY", SerialPort.PARITY_NONE);
        stopBits = preferences.getInt("CONNECTION_STOP_BITS", SerialPort.STOPBITS_1);
        flowControl = preferences.getInt("CONNECTION_FLOW_CONTROL", SerialPort.FLOWCONTROL_NONE);

        append = preferences.get("OPTION_NEWLINE", "\r\n");
        echo = preferences.getBoolean("OPTION_ECHO", true);
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }

    public int getDataBits() {
        return dataBits;
    }

    public void setDataBits(int dataBits) {
        this.dataBits = dataBits;
    }

    public int getParity() {
        return parity;
    }

    public void setParity(int parity) {
        this.parity = parity;
    }

    public int getStopBits() {
        return stopBits;
    }

    public void setStopBits(int stopBits) {
        this.stopBits = stopBits;
    }

    public int getFlowControl() {
        return flowControl;
    }

    public void setFlowControl(int flowControl) {
        this.flowControl = flowControl;
    }

    public String getAppend() {
        return append;
    }

    public void setAppend(String append) {
        this.append = append;
    }

    public boolean isEcho() {
        return echo;
    }

    public void setEcho(boolean echo) {
        this.echo = echo;
    }
}
