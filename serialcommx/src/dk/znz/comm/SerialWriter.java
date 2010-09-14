/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package dk.znz.comm;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.charset.Charset;

/**
 *
 * @author Esben
 */
public class SerialWriter {
    private OutputStream dest;
    private Charset charset;

    public SerialWriter(OutputStream dest, Charset charset) {
        this.dest = dest;
        this.charset = charset;
    }

    public void send(byte[] bytes) throws IOException {
        dest.write(bytes);
    }

    public String send(String str) throws IOException {
        byte[] bytes = str.getBytes(charset);
        send(bytes);
        return new String(bytes, charset);
    }
}
