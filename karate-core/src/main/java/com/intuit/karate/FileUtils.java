package com.intuit.karate;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.intuit.karate.Script.eval;

/**
 *
 * @author pthomas3
 */
public class FileUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class); 
    
    private FileUtils() {
        // only static methods
    }
    
    public static final boolean isClassPath(String text) {
        return text.startsWith("classpath:");
    }   
    
    public static final boolean isJsonFile(String text) {
        return text.endsWith(".json");
    }

    public static final boolean isJavaScriptFile(String text) {
        return text.endsWith(".js");
    }

    public static final boolean isXmlFile(String text) {
        return text.endsWith(".xml");
    }

    public static final boolean isTextFile(String text) {
        return text.endsWith(".txt");
    }

    public static ScriptValue readFile(String text, ScriptContext context) {
        text = StringUtils.trim(text);
        int pos = text.indexOf(':');
        String fileName = pos == -1 ? text : text.substring(pos + 1);
        fileName = StringUtils.trim(fileName);
        if (isJsonFile(text) || isXmlFile(text) || isJavaScriptFile(text)) {
            String contents = readFileAsString(fileName, isClassPath(text), context);
            return eval(contents, context);
        } else if (isTextFile(text)) {
            String contents = readFileAsString(fileName, isClassPath(text), context);
            return new ScriptValue(contents);
        } else {
            InputStream is = getFileStream(fileName, isClassPath(text), context);
            return new ScriptValue(is);
        }        
    }
    
    public static String readFileAsString(String path, boolean classpath, ScriptContext context) {
        InputStream is = getFileStream(path, classpath, context);
        try {
            return IOUtils.toString(is, "utf-8");
        } catch (Exception e) {
            String message = String.format("could not read file: %s, classpath: %s", path, classpath);
            logger.error(message);
            throw new RuntimeException(message, e);
        }
    } 
    
    public static InputStream getFileStream(String path, boolean classpath, ScriptContext context) {
        if (classpath) {
            return context.fileClassLoader.getResourceAsStream(path);
        }
        String fullPath = context.featureDir + File.separator + path;
        try {
            InputStream is = org.apache.commons.io.FileUtils.openInputStream(new File(fullPath));
            logger.debug("loaded file from: {} - {}: {}", fullPath, path, is);
            return is;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getFileReaderFunction() {
        return "function(path) {\n"
                + "  var FileUtils = Java.type('com.intuit.karate.FileUtils');\n"
                + "  return FileUtils.readFile(path, " + Script.VAR_CONTEXT + ").value;\n"
                + "}";
    } 
    
    public static File getDirContaining(Class clazz) {
        String resourcePath = clazz.getResource(clazz.getSimpleName() + ".class").getFile();
        return new File(resourcePath).getParentFile();
    }
    
    private static URL toFileUrl(String path) {
        path = StringUtils.trim(path);
        File file = new File(path);        
        try {
            return file.getAbsoluteFile().toURI().toURL();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static ClassLoader createClassLoader(String ... paths) {
        List<URL> urls = new ArrayList<>(paths.length);
        for (String path : paths) {
            urls.add(toFileUrl(path));
        }
        return new URLClassLoader(urls.toArray(new URL[]{}));       
    }    
    
}
