/*
 * PersistenceManager.java
 *
 * Created on 19. červenec 2007, 20:55
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package persistence;

import esmska.*;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/** Load and store settings and data
 *
 * @author ripper
 */
public class PersistenceManager {
    private static PersistenceManager persistenceManager;
    private static final String PROGRAM_DIRNAME = ".esmska";
    private static final String CONFIG_FILENAME = "nastaveni.xml";
    private static final String CONTACTS_FILENAME = "kontakty.xml";
    private static File PROGRAM_DIR =
            new File(System.getProperty("user.home"), PROGRAM_DIRNAME);
    private static File CONFIG_FILE = new File(PROGRAM_DIR, CONFIG_FILENAME);
    private static File CONTACTS_FILE = new File(PROGRAM_DIR, CONTACTS_FILENAME);
    private static Config config = new Config();
    private static ContactsBean contacts = new ContactsBean();
    
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
    
    public static void setProgramDir(String path) {
        if (persistenceManager != null)
            throw new IllegalStateException("Persistence manager already exists");
        
        PROGRAM_DIR = new File(path);
        CONFIG_FILE = new File(PROGRAM_DIR, CONFIG_FILENAME);
        CONTACTS_FILE = new File(PROGRAM_DIR, CONTACTS_FILENAME);
    }
    
    /** Get PersistenceManager */
    public static PersistenceManager getInstance() throws IOException {
        if (persistenceManager == null)
            persistenceManager = new PersistenceManager();
        return persistenceManager;
    }
    
    /** return config */
    public static Config getConfig() {
        return config;
    }
    
    /** return contacts */
    public static ContactsBean getContacs() {
        return contacts;
    }
    
    /** Save program configuration */
    public void saveConfig() throws IOException {
        CONFIG_FILE.createNewFile();
        XMLEncoder xmlEncoder = new XMLEncoder(
                new BufferedOutputStream(new FileOutputStream(CONFIG_FILE)));
        xmlEncoder.writeObject(config);
        xmlEncoder.close();
    }
    
    /** Load program configuration */
    public Config loadConfig() throws IOException {
        if (CONFIG_FILE.exists()) {
            XMLDecoder xmlDecoder = new XMLDecoder(
                    new BufferedInputStream(new FileInputStream(CONFIG_FILE)));
            Config config = (Config) xmlDecoder.readObject();
            xmlDecoder.close();
            if (config != null)
                this.config = config;
        }
        return config;
    }
    
    /** Save contacts */
    public void saveContacts() throws IOException {
        CONTACTS_FILE.createNewFile();
        XMLEncoder xmlEncoder = new XMLEncoder(
                new BufferedOutputStream(new FileOutputStream(CONTACTS_FILE)));
        xmlEncoder.writeObject(contacts);
        xmlEncoder.close();
    }
    
    /** Load contacts */
    public ContactsBean loadContacts() throws IOException {
        if (CONTACTS_FILE.exists()) {
            XMLDecoder xmlDecoder = new XMLDecoder(
                    new BufferedInputStream(new FileInputStream(CONTACTS_FILE)));
            ContactsBean contacts = (ContactsBean) xmlDecoder.readObject();
            xmlDecoder.close();
            if (contacts != null)
                this.contacts = contacts;
        }
        return contacts;
    }
}
