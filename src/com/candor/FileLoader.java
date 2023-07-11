package com.candor;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class FileLoader {
    private Map<String, byte[]> files;
    private String web_page_path;

    private void loadFile(String path) {
        InputStream FileInputStream = null;
        try {
            FileInputStream fis = new FileInputStream(web_page_path + path);
            files.put(path, fis.readAllBytes());
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            if (FileInputStream != null) {
                try {
                    FileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public byte[] getFile(String path) {
        if (!files.containsKey(path)) {
            loadFile(path);
        }
        return files.get(path);
    }

    public FileLoader(String _web_page_path) {
        web_page_path = _web_page_path;
        files = new HashMap<>();
    }
}
