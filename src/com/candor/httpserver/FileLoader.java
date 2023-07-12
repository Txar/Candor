package com.candor.httpserver;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;
import org.json.*;

public class FileLoader {
    public static final String directory_config_file_name = "directory.json";
    private Map<String, SimpleEntry<byte[], Boolean>> files;
    private Map<String, JSONObject> directories;
    private String web_page_path;
    private String config_path;
    public JSONObject config;

    public String fileType(String path) {
        return path.substring(path.lastIndexOf(".") + 1);
    }

    private boolean loadFile(String path) {
        return loadFile(path, false, false);
    }

    private boolean loadFile(String path, boolean isPathAbsolute, boolean isHidden) {
        InputStream FileInputStream = null;
        String p = path;
        boolean success = false;
        if (!isPathAbsolute) {
            p = web_page_path + File.separator + path;
        }
        try {
            FileInputStream fis = new FileInputStream(p);
            File f = new File(p);

            if (isHidden || f.getName().equals(directory_config_file_name)) {
                files.put(path, new SimpleEntry(fis.readAllBytes(), false));
            }
            else {
                files.put(path, new SimpleEntry(fis.readAllBytes(), isDirectoryAccessible(f.getParent())));
            }
            fis.close();
            success = true;

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
        return success;
    }

    private void loadDirectoryConfig(String path) {
        String p = path;
        boolean success = false;
        while (p.startsWith(web_page_path + File.separator + "public") && !success) {
            try {
                //getFile(p + File.separator + directory_config_file_name);
                loadFile(p.substring(web_page_path.length()) + File.separator + directory_config_file_name);
                JSONObject jo = new JSONObject(new String(files.get(p.substring(web_page_path.length()) + File.separator + directory_config_file_name).getKey()));
                directories.put(path, jo);
                success = true;
            } catch (Exception e) {
                System.out.println(e);
            }
            if (!success) {
                try {
                    String splitter = File.separator.replace("\\","\\\\");
                    String[] folders = p.split(splitter);

                    p = p.substring(0, p.length() - folders[folders.length - 1].length() - File.separator.length());
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }
    }

    private boolean isDirectoryAccessible(String path) {
        if (!directories.containsKey(path)) {
            loadDirectoryConfig(path);
        }
        return (boolean) directories.get(path).get("accessible");
    }

    public byte[] getFile(String path) {
        return getFile(path, false, false);
    }

    public byte[] getFile(String path, boolean isPathAbsolute, boolean isHidden) {
        if (!files.containsKey(path)) {
            loadFile(path, isPathAbsolute, isHidden);
        }
        return files.get(path).getKey();
    }

    public boolean isAccessible(String path) {
        boolean success = true;
        if (!files.containsKey(path)) {
            success = loadFile(path);
        }
        if (success) {
            return files.get(path).getValue();
        }
        else {
            return false;
        }
    }

    public FileLoader(String _web_page_path, String _config_path) {
        web_page_path = _web_page_path;
        files = new HashMap<>();
        directories = new HashMap<>();
        config_path = _config_path;
        config = new JSONObject(new String(getFile(config_path, false, true)));
    }
}
