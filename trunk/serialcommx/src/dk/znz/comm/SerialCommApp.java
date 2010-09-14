/*
 * SerialCommApp.java
 */
package dk.znz.comm;

import gnu.io.CommPortIdentifier;
import java.util.Enumeration;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The main class of the application.
 */
public class SerialCommApp extends SingleFrameApplication {

    /**
     * At startup create and show the main frame of the application.
     */
    @Override
    protected void startup() {
        show(new SerialCommView(this));
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override
    protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of SerialCommApp
     */
    public static SerialCommApp getApplication() {
        return Application.getInstance(SerialCommApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        //launch(SerialCommApp.class, args);
        for (Enumeration e = CommPortIdentifier.getPortIdentifiers(); e.hasMoreElements();) {
            CommPortIdentifier commPortIdentifier = (CommPortIdentifier)e.nextElement();
            System.out.println(commPortIdentifier.getName());
        }
    }
}
