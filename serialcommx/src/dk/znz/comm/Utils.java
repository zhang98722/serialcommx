package dk.znz.comm;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;
import gnu.io.CommPortIdentifier;
import gnu.io.RXTXCommDriver;

/** Simple example of JNA interface mapping and usage. */
public class Utils {

    // kernel32.dll uses the __stdcall calling convention (check the function
    // declaration for "WINAPI" or "PASCAL"), so extend StdCallLibrary
    // Most C libraries will just extend com.sun.jna.Library,
    private interface Kernel32 extends StdCallLibrary {
        // Method declarations, constant and structure definitions go here

        Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class);
        // Optional: wraps every call to the native library in a
        // synchronized block, limiting native calls to one at a time
        Kernel32 SYNC_INSTANCE = (Kernel32) Native.synchronizedLibrary(INSTANCE);

        int QueryDosDeviceA(String lpDeviceName, byte[] lpTargetPath, int ucchMax);
    }

    /**
     * lookup a dos name for a given device name
     * @param name the name of the device to lookup
     * @return only the device name is returned eg. "\Device\ProlificSerial0" returns "ProlificSerial0"
     */
    public static String QueryDosDevice(String name) {
        Kernel32 lib = Kernel32.INSTANCE;
        byte[] buffer = new byte[65536];
        int result = lib.QueryDosDeviceA(name, buffer, buffer.length);
        if(result != 0) {
            String deviceInfo = Native.toString(buffer);
            return (deviceInfo.startsWith("\\Device\\") && deviceInfo.length() > "\\Device\\".length() ? deviceInfo.substring("\\Device\\".length()) : deviceInfo);
        } else {
            int errorNumber = Native.getLastError();
            switch(errorNumber) {
                case 2:
                    return "N/A";
                default:
                    System.err.println("QueryDosDevice(" + name + "): " + errorNumber);
                    return "N/A";
            }
        }
    }
}
