/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gnu.io;

import com.sun.jna.Platform;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author drphrozen
 */
public class Native {

    private static String nativeLibraryPath = null;
    private static boolean unpacked;
    private static final String libraryName = "rxtxSerial";

    public static void load() {
        try {
            System.loadLibrary(libraryName);
            nativeLibraryPath = libraryName;
        } catch (UnsatisfiedLinkError e) {
            loadNativeLibraryFromJar();
        }
    }

    /**
     * Attempts to load the native library resource from the filesystem,
     * extracting the JNA stub library from jna.jar if not already available.
     */
    private static void loadNativeLibraryFromJar() {
        String libname = System.mapLibraryName(libraryName);
        String arch = System.getProperty("os.arch");
        String name = System.getProperty("os.name");
        String resourceName = getNativeLibraryResourcePath(Platform.getOSType(), arch, name) + "/" + libname;
        URL url = CommPortIdentifier.class.getResource(resourceName);

        // Add an ugly hack for OpenJDK (soylatte) - JNI libs use the usual
        // .dylib extension
        if (url == null && Platform.isMac()
                && resourceName.endsWith(".dylib")) {
            resourceName = resourceName.substring(0, resourceName.lastIndexOf(".dylib")) + ".jnilib";
            url = CommPortIdentifier.class.getResource(resourceName);
        }
        if (url == null) {
            throw new UnsatisfiedLinkError(libraryName + " (" + resourceName
                    + ") not found in resource path");
        }

        File lib = null;
        if (url.getProtocol().toLowerCase().equals("file")) {
            try {
                lib = new File(new URI(url.toString()));
            } catch (URISyntaxException e) {
                lib = new File(url.getPath());
            }
            if (!lib.exists()) {
                throw new Error("File URL " + url + " could not be properly decoded");
            }
        } else {
            InputStream is = Native.class.getResourceAsStream(resourceName);
            if (is == null) {
                throw new Error("Can't obtain " + libraryName + " InputStream");
            }

            FileOutputStream fos = null;
            try {
                // Suffix is required on windows, or library fails to load
                // Let Java pick the suffix, except on windows, to avoid
                // problems with Web Start.
                lib = File.createTempFile("rxtx", Platform.isWindows() ? ".dll" : null);
                lib.deleteOnExit();
                ClassLoader cl = Native.class.getClassLoader();
                if (Platform.deleteNativeLibraryAfterVMExit()
                        && (cl == null
                        || cl.equals(ClassLoader.getSystemClassLoader()))) {
                    Runtime.getRuntime().addShutdownHook(new DeleteNativeLibrary(lib));
                }
                fos = new FileOutputStream(lib);
                int count;
                byte[] buf = new byte[1024];
                while ((count = is.read(buf, 0, buf.length)) > 0) {
                    fos.write(buf, 0, count);
                }
            } catch (IOException e) {
                throw new Error("Failed to create temporary file for " + libraryName + " library: " + e);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                }
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                    }
                }
            }
            unpacked = true;
        }
        System.load(lib.getAbsolutePath());
        nativeLibraryPath = lib.getAbsolutePath();
    }

    private static String getNativeLibraryResourcePath(int osType, String arch, String name) {
        String osPrefix;
        arch = arch.toLowerCase();
        switch (osType) {
            case Platform.WINDOWS:
                if ("i386".equals(arch)) {
                    arch = "x86";
                }
                osPrefix = "win32-" + arch;
                break;
            case Platform.MAC:
                osPrefix = "darwin";
                break;
            case Platform.LINUX:
                if ("x86".equals(arch)) {
                    arch = "i386";
                } else if ("x86_64".equals(arch)) {
                    arch = "amd64";
                }
                osPrefix = "linux-" + arch;
                break;
            case Platform.SOLARIS:
                osPrefix = "sunos-" + arch;
                break;
            default:
                osPrefix = name.toLowerCase();
                if ("x86".equals(arch)) {
                    arch = "i386";
                }
                if ("x86_64".equals(arch)) {
                    arch = "amd64";
                }
                if ("powerpc".equals(arch)) {
                    arch = "ppc";
                }
                int space = osPrefix.indexOf(" ");
                if (space != -1) {
                    osPrefix = osPrefix.substring(0, space);
                }
                osPrefix += "-" + arch;
                break;
        }
        return "/gnu/io/" + osPrefix;
    }

    /** For internal use only. */
    private static class DeleteNativeLibrary extends Thread {

        private final File file;

        public DeleteNativeLibrary(File file) {
            this.file = file;
        }

        public void run() {
            // If we can't force an unload/delete, spawn a new process
            // to do so
            if (!deleteNativeLibrary()) {
                try {
                    Runtime.getRuntime().exec(new String[]{
                                System.getProperty("java.home") + "/bin/java",
                                "-cp", System.getProperty("java.class.path"),
                                getClass().getName(),
                                file.getAbsolutePath(),});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public static void main(String[] args) {
            if (args.length == 1) {
                File file = new File(args[0]);
                if (file.exists()) {
                    long start = System.currentTimeMillis();
                    while (!file.delete() && file.exists()) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                        }
                        if (System.currentTimeMillis() - start > 5000) {
                            System.err.println("Could not remove temp file: "
                                    + file.getAbsolutePath());
                            break;
                        }
                    }
                }
            }
            System.exit(0);
        }
    }
    /** Ensure our unpacked native library gets cleaned up if this class gets
    garbage-collected.
     */
    private static final Object finalizer = new Object() {

        protected void finalize() {
            deleteNativeLibrary();
        }
    };

    /** Remove any unpacked native library.  Forcing the class loader to
    unload it first is required on Windows, since the temporary native
    library can't be deleted until the native library is unloaded.  Any
    deferred execution we might install at this point would prevent the
    Native class and its class loader from being GC'd, so we instead force
    the native library unload just a little bit prematurely.
     */
    private static boolean deleteNativeLibrary() {
        String path = nativeLibraryPath;
        if (path == null || !unpacked) {
            return true;
        }
        File flib = new File(path);
        if (flib.delete()) {
            nativeLibraryPath = null;
            unpacked = false;
            return true;
        }
        // Reach into the bowels of ClassLoader to force the native
        // library to unload
        // NOTE: this may cause a failure when freeing com.sun.jna.Memory
        // after the library has been unloaded, see
        // https://jna.dev.java.net/issues/show_bug.cgi?id=157
        try {
            ClassLoader cl = Native.class.getClassLoader();
            Field f = ClassLoader.class.getDeclaredField("nativeLibraries");
            f.setAccessible(true);
            List libs = (List) f.get(cl);
            for (Iterator i = libs.iterator(); i.hasNext();) {
                Object lib = i.next();
                f = lib.getClass().getDeclaredField("name");
                f.setAccessible(true);
                String name = (String) f.get(lib);
                if (name.equals(path) || name.indexOf(path) != -1
                        || name.equals(flib.getCanonicalPath())) {
                    Method m = lib.getClass().getDeclaredMethod("finalize", new Class[0]);
                    m.setAccessible(true);
                    m.invoke(lib, new Object[0]);
                    nativeLibraryPath = null;
                    if (unpacked) {
                        if (flib.exists()) {
                            if (flib.delete()) {
                                unpacked = false;
                                return true;
                            }
                            return false;
                        }
                    }
                    return true;
                }
            }
        } catch (Exception e) {
        }
        return false;
    }
}
