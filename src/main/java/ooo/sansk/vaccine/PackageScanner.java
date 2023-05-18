package ooo.sansk.vaccine;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PackageScanner {
    private static final String CLASS_FILE_SUFFIX = ".class";

    private PackageScanner() {
    }

    /**
     * Private helper method
     *
     * @param directory   The directory to start with
     * @param packageName The package name to search for. Will be needed for getting the
     *                    Class object.
     * @param classes     if a file isn't loaded but still is in the directory
     * @throws ClassNotFoundException if a child class could not be loaded
     */
    private static void checkDirectory(File directory, String packageName, ArrayList<Class<?>> classes)
            throws ClassNotFoundException {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }
        final String[] files = directory.list();
        if (files == null) {
            return;
        }

        for (final String file : files) {
            if (file.endsWith(CLASS_FILE_SUFFIX)) {
                try {
                    classes.add(Class.forName("%s.%s".formatted(packageName, file.substring(0, file.length() - 6))));
                } catch (final NoClassDefFoundError e) {
                    // do nothing. this class hasn't been found by the
                    // loader, and we don't care.
                }
            } else {
                File tmpDirectory = new File(directory, file);
                if (tmpDirectory.isDirectory()) {
                    checkDirectory(tmpDirectory, packageName + "." + file, classes);
                }
            }
        }
    }

    /**
     * Private helper method.
     *
     * @param connection the connection to the jar
     * @param packageName   the package name to search for
     * @param classes    the current ArrayList of all classes. This method will simply
     *                   add new classes.
     * @throws ClassNotFoundException if a file isn't loaded but still is in the jar file
     * @throws IOException            if it can't correctly read from the jar file.
     */
    private static void checkJarFile(JarURLConnection connection, String packageName, ArrayList<Class<?>> classes)
            throws ClassNotFoundException, IOException {
        final JarFile jarFile = connection.getJarFile();
        final Enumeration<JarEntry> entries = jarFile.entries();
        String name;

        for (JarEntry jarEntry; entries.hasMoreElements() && ((jarEntry = entries.nextElement()) != null); ) {
            name = jarEntry.getName();

            if (!name.contains(CLASS_FILE_SUFFIX)) {
                continue;
            }
            name = name.substring(0, name.length() - 6).replace('/', '.');

            if (!name.contains(packageName)) {
                continue;
            }
            classes.add(Class.forName(name));
        }
    }

    /**
     * Attempts to list all the classes in the specified package as determined
     * by the context class loader
     *
     * @param packageName the package name to search
     * @return a list of classes that exist within that package
     * @throws ClassNotFoundException if something went wrong
     */
    public static List<Class<?>> getClassesForPackage(ClassLoader classLoader, String packageName) throws ClassNotFoundException {
        final var classes = new ArrayList<Class<?>>();

        try {
            final var resources = classLoader.getResources(packageName.replace('.', '/'));

            for (URL url; resources.hasMoreElements() && ((url = resources.nextElement()) != null); ) {
                final var connection = url.openConnection();
                if (connection instanceof JarURLConnection urlConnection) {
                    checkJarFile(urlConnection, packageName, classes);
                } else {
                    checkDirectory(new File(URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8)), packageName, classes);
                }
            }
        } catch (final NullPointerException nullPointerException) {
            throw new ClassNotFoundException(packageName + " does not appear to be a valid package (Null pointer exception)", nullPointerException);
        } catch (final IOException ioException) {
            throw new ClassNotFoundException("IOException was thrown when trying to get all resources for " + packageName, ioException);
        }

        return classes;
    }
}
