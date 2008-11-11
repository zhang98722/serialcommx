/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package dk.znz.comm;

import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextArea;

/**
 *
 * @author Esben
 */
public class SerialReader implements SerialPortEventListener {

    private InputStream src;
    private JTextArea dest;
    private byte[] buffer = new byte[1024];

    public SerialReader(InputStream src, JTextArea dest) {
        this.src = src;
        this.dest = dest;
    }
    
    public void serialEvent(SerialPortEvent ev) {
        try {
            int length = src.read(buffer);
            dest.append(new String(buffer, 0, length, Charset.forName("US-ASCII")));
        } catch (IOException ex) {
            Logger.getLogger(SerialReader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
