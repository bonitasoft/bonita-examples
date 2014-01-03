package org.bonitasoft.example.spring;

import java.io.File;
import java.util.List;

public class FileRemover {

    public FileRemover(final List<String> files) {
        super();
        if (files != null && !files.isEmpty()) {
            for (final String filePath : files) {
                final String resolvedFile = resolveSystemProperties(filePath);
                final File fileToDelete = new File(resolvedFile);
                final boolean deleted = fileToDelete.delete();
                if (deleted) {
                    System.err.println("File '" + resolvedFile + "' has been successfully deleted");
                } else {
                    System.err.println("File '" + resolvedFile + "' has NOT been deleted");
                }
            }
        }
    }

    private String resolveSystemProperties(final String originalString) {
        String s = new String(originalString);
        String resolvedFile = "";
        while (s.contains("${")) {
            final String sysPropName = s.substring(s.indexOf("${") + 2, s.indexOf("}"));
            final String syspropValue = System.getProperty(sysPropName);
            resolvedFile += s.substring(0, s.indexOf("$"));
            resolvedFile += syspropValue;
            s = s.substring(s.indexOf("}") + 1);
        }
        resolvedFile += s;
        return resolvedFile;
    }


}
