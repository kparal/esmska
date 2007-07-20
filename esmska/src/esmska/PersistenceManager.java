/*
 * PersistenceManager.java
 *
 * Created on 19. červenec 2007, 20:55
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 * @author ripper
 */
public class PersistenceManager {
    private static PersistenceManager persistenceManager;
    private static final File PROGRAM_DIR =
            new File(System.getProperty("user.home"), ".esmska");
    private static final File CONFIG_FILE = new File(PROGRAM_DIR, "config.xml");
    private static ConfigBean config;
    
    /** Creates a new instance of PersistenceManager */
    private PersistenceManager() throws IOException {
        boolean ok = true;
        if (!PROGRAM_DIR.exists())
            ok = PROGRAM_DIR.mkdir();
        if (!ok)
            throw new IOException("Can't create program dir");
        if (!(PROGRAM_DIR.canWrite() && PROGRAM_DIR.canExecute()))
            throw new IOException("Can't write or execute the program dir");
    }
    
    /** Get PersistenceManager */
    public static PersistenceManager getPersistenceManager() throws IOException {
        if (persistenceManager == null)
            persistenceManager = new PersistenceManager();
        return persistenceManager;
    }
    
    public ConfigBean getConfig() {
        if (config == null)
            config = new ConfigBean();
        return config;
    }
    
    /** Save program configuration */
    public void saveConfig() throws IOException {
        if (config == null)
            return;
        CONFIG_FILE.createNewFile();
        XMLEncoder xmlEncoder = new XMLEncoder(
                new BufferedOutputStream(new FileOutputStream(CONFIG_FILE)));
        xmlEncoder.writeObject(config);
        xmlEncoder.close();
    }
    
    public ConfigBean loadConfig() throws IOException {
        if (CONFIG_FILE.exists()) {
            XMLDecoder xmlDecoder = new XMLDecoder(
                    new BufferedInputStream(new FileInputStream(CONFIG_FILE)));
            ConfigBean config = (ConfigBean) xmlDecoder.readObject();
            xmlDecoder.close();
            this.config = config;
        } else {
            config = new ConfigBean();
        }
        return config;
    }
    
}
