package com.candor.girder.file;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FileLoader {
    private Map<String, byte[]> files;

    public String fileType(String path) {
        return path.substring(path.lastIndexOf(".") + 1);
    }

    private boolean loadFile(String path) {
        java.io.FileInputStream fis = null;
        boolean success = false;

        try {
            fis = new FileInputStream(path);
            files.put(path, fis.readAllBytes());
            fis.close();
            success = true;

        } catch (Exception e) {
            System.out.println(e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return success;
    }

    public byte[] getFile(String path) {
        if (!files.containsKey(path)) {
            loadFile(path);
        }
        return files.get(path);
    }

    public FileLoader() {
        files = new HashMap<>();
    }
}
