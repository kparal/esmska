/*
 * PersistenceManager.java
 *
 * Created on 19. červenec 2007, 20:55
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska.persistence;

import esmska.data.Config;
import esmska.data.Contact;
import esmska.data.History;
import esmska.data.SMS;
import esmska.operators.Operator;
import java.beans.IntrospectionException;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;

/** Load and store settings and data
 *
 * @author ripper
 */
public class PersistenceManager {
    private static final Logger logger = Logger.getLogger(PersistenceManager.class.getName());
    private static PersistenceManager persistenceManager;
    
    private static final String PROGRAM_DIRNAME = "esmska";
    private static final String OPERATOR_DIRNAME = "operators";
    private static final String CONFIG_FILENAME = "settings.xml";
    private static final String CONTACTS_FILENAME = "contacts.csv";
    private static final String QUEUE_FILENAME = "queue.csv";
    private static final String HISTORY_FILENAME = "history.csv";
    private static File PROGRAM_DIR =
            new File(System.getProperty("user.home") + File.separator + ".config",
            PROGRAM_DIRNAME);
    private static File OPERATOR_DIR = new File(OPERATOR_DIRNAME);
    private static File CONFIG_FILE = new File(PROGRAM_DIR, CONFIG_FILENAME);
    private static File CONTACTS_FILE = new File(PROGRAM_DIR, CONTACTS_FILENAME);
    private static File QUEUE_FILE = new File(PROGRAM_DIR, QUEUE_FILENAME);
    private static File HISTORY_FILE = new File(PROGRAM_DIR, HISTORY_FILENAME);
    
    private static Config config = new Config();
    private static TreeSet<Contact> contacts = new TreeSet<Contact>();
    private static List<SMS> queue = Collections.synchronizedList(new ArrayList<SMS>());
    private static History history = new History();
    private static TreeSet<Operator> operators = new TreeSet<Operator>();
    
    private static boolean customPathSet;
    
    /** Creates a new instance of PersistenceManager */
    private PersistenceManager() throws IOException {
        //adjust program dir according to operating system
        if (!customPathSet) {
            String path = System.getenv("XDG_CONFIG_HOME");
            if ((path == null || path.equals("")) &&
                    System.getProperty("os.name").toLowerCase().contains("windows")) {
                path = System.getenv("APPDATA");
            }
            if (path != null && !path.equals("")) {
                setProgramDir(path + File.separator + PROGRAM_DIRNAME);
            }
        }
        
        //create program dir if necessary
        boolean ok = true;
        if (!PROGRAM_DIR.exists())
            ok = PROGRAM_DIR.mkdirs();
        if (!ok)
            throw new IOException("Can't create program dir");
        if (!(PROGRAM_DIR.canWrite() && PROGRAM_DIR.canExecute()))
            throw new IOException("Can't write or execute the program dir");
    }
    
    /** Set program dir to custom path */
    public static void setProgramDir(String path) {
        if (persistenceManager != null)
            throw new IllegalStateException("Persistence manager already exists");
        
        PROGRAM_DIR = new File(path);
        OPERATOR_DIR = new File(PROGRAM_DIR, OPERATOR_DIRNAME);
        CONFIG_FILE = new File(PROGRAM_DIR, CONFIG_FILENAME);
        CONTACTS_FILE = new File(PROGRAM_DIR, CONTACTS_FILENAME);
        QUEUE_FILE = new File(PROGRAM_DIR, QUEUE_FILENAME);
        HISTORY_FILE = new File(PROGRAM_DIR, HISTORY_FILENAME);
        customPathSet = true;
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
    public static TreeSet<Contact> getContacs() {
        return contacts;
    }
    
    /** return queue */
    public static List<SMS> getQueue() {
        return queue;
    }
    
    /** return history */
    public static History getHistory() {
        return history;
    }
    
    /** return operators */
    public static TreeSet<Operator> getOperators() {
        return operators;
    }
    
    /** Save program configuration */
    public void saveConfig() throws IOException {
        //store current program version into config
        config.setVersion(Config.getLatestVersion());
        
        File temp = createTempFile();
        XMLEncoder xmlEncoder = new XMLEncoder(
                new BufferedOutputStream(new FileOutputStream(temp)));
        xmlEncoder.writeObject(config);
        xmlEncoder.close();
        moveFileSafely(temp, CONFIG_FILE);
    }
    
    /** Load program configuration */
    public void loadConfig() throws IOException {
        if (CONFIG_FILE.exists()) {
            XMLDecoder xmlDecoder = new XMLDecoder(
                    new BufferedInputStream(new FileInputStream(CONFIG_FILE)));
            Config config = (Config) xmlDecoder.readObject();
            xmlDecoder.close();
            if (config != null)
                this.config = config;
        }
    }
    
    /** Save contacts */
    public void saveContacts() throws IOException {
        File temp = createTempFile();
        ExportManager.exportContacts(contacts, temp);
        moveFileSafely(temp, CONTACTS_FILE);
    }
       
    /** Load contacts */
    public void loadContacts() throws Exception {
        if (CONTACTS_FILE.exists()) {
            ArrayList<Contact> newContacts = ImportManager.importContacts(CONTACTS_FILE,
                    ContactParser.ContactType.ESMSKA_FILE);
            contacts.clear();
            contacts.addAll(newContacts);
        }
    }
    
    /** Save sms queue */
    public void saveQueue() throws IOException {
        File temp = createTempFile();
        ExportManager.exportQueue(queue, temp);
        moveFileSafely(temp, QUEUE_FILE);
    }
    
    /** Load sms queue */
    public void loadQueue() throws IOException {
        if (QUEUE_FILE.exists()) {
            ArrayList<SMS> newQueue = ImportManager.importQueue(QUEUE_FILE);
            queue.clear();
            queue.addAll(newQueue);
        }
    }
    
    /** Save sms history */
    public void saveHistory() throws IOException {
        File temp = createTempFile();
        ExportManager.exportHistory(history.getRecords(), temp);
        moveFileSafely(temp, HISTORY_FILE);
    }
    
    /** Load sms history */
    public void loadHistory() throws Exception {
        if (HISTORY_FILE.exists()) {
            ArrayList<History.Record> records = ImportManager.importHistory(HISTORY_FILE);
            history.clearRecords();
            history.addRecords(records);
        }
    }
    
    /** Load operators
     * @throws IOException When there is problem accessing operator directory or files
     * @throws IntrospectionException When current JRE does not support JavaScript execution
     */
    public void loadOperators() throws IOException, IntrospectionException {
        if (OPERATOR_DIR.exists()) {
            TreeSet<Operator> newOperators = ImportManager.importOperators(OPERATOR_DIR);
            operators.clear();
            operators.addAll(newOperators);
        } else {
            throw new IOException("Operators directory doesn't exist.");
        }
    }

    /** Moves file from srcFile to destFile safely (using backup of destFile).
     * If move fails, exception is thrown and attempt to restore destFile from
     * backup is made.
     */
    private void moveFileSafely(File srcFile, File destFile) throws IOException {
        File backup = backupFile(destFile);
        try {
            FileUtils.moveFile(srcFile, destFile);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Moving of " + srcFile.getAbsolutePath() + " to " +
                    destFile.getAbsolutePath() + " failed, trying to restore from backup");
            FileUtils.deleteQuietly(destFile);
            FileUtils.moveFile(backup, destFile);
            throw ex;
        }
        FileUtils.deleteQuietly(backup);
    }

    
    /** Create temp file and return it. */
    private File createTempFile() throws IOException {
        return File.createTempFile("esmska", null);
    }
    
    /** Copies original file to backup file with same filename, but ending with "~".
     * DELETES original file!
     * @return newly created backup file, or null if original file doesn't exist
     */
    private File backupFile(File file) throws IOException {
        if (!file.exists())
            return null;
        String backupName = file.getAbsolutePath() + "~";
        File backup = new File(backupName);
        FileUtils.copyFile(file, backup);
        file.delete();
        return backup;
    }
}
