/*
 * PersistenceManager.java
 *
 * Created on 19. červenec 2007, 20:55
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska.persistence;

import java.beans.IntrospectionException;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import esmska.data.Config;
import esmska.data.Contact;
import esmska.data.Contacts;
import esmska.data.History;
import esmska.data.Keyring;
import esmska.data.Operators;
import esmska.data.Queue;
import esmska.data.SMS;
import esmska.operators.Operator;
import esmska.utils.OSType;
import org.apache.commons.lang.StringUtils;

/** Load and store settings and data
 *
 * @author ripper
 */
public class PersistenceManager {
    private static final Logger logger = Logger.getLogger(PersistenceManager.class.getName());
    private static PersistenceManager persistenceManager;
    
    private static final String USER_DIRNAME = "esmska";
    private static final String OPERATOR_DIRNAME = "operators";
    private static final String CONFIG_FILENAME = "settings.xml";
    private static final String CONTACTS_FILENAME = "contacts.csv";
    private static final String QUEUE_FILENAME = "queue.csv";
    private static final String HISTORY_FILENAME = "history.csv";
    private static final String KEYRING_FILENAME = "keyring.csv";
    private static final String LOCK_FILENAME = "running.lock";
    private static final String OPERATOR_RESOURCE = "/esmska/operators/scripts";
    
    private static File userDir =
            new File(System.getProperty("user.home") + File.separator + ".config",
            USER_DIRNAME);
    private static File operatorDir = new File(OPERATOR_DIRNAME);
    private static File configFile = new File(userDir, CONFIG_FILENAME);
    private static File contactsFile = new File(userDir, CONTACTS_FILENAME);
    private static File queueFile = new File(userDir, QUEUE_FILENAME);
    private static File historyFile = new File(userDir, HISTORY_FILENAME);
    private static File keyringFile = new File(userDir, KEYRING_FILENAME);
    private static File lockFile = new File(userDir, LOCK_FILENAME);
    
    private static boolean customPathSet;
    private FileLock lock;
    
    /** Creates a new instance of PersistenceManager */
    private PersistenceManager() throws IOException {
        //adjust program dir according to operating system
        if (!customPathSet) {
            String path;
            
            switch (OSType.detect()) {
                case LINUX:
                    path = System.getenv("XDG_CONFIG_HOME");
                    break;
                case MAC_OS_X:
                    path = System.getProperty("user.home") + "/Library/Application Support";
                    break;
                case WINDOWS:
                    path = System.getenv("APPDATA");
                    break;
                default:
                    path = System.getenv("XDG_CONFIG_HOME");
                    break;
            }
            
            if (StringUtils.isNotEmpty(path)) {
                setUserDir(path + File.separator + USER_DIRNAME);
            }
        }
        
        //create program dir if necessary
        if (!userDir.exists() && !userDir.mkdirs()) {
            throw new IOException("Can't create program dir '" + userDir.getAbsolutePath() + "'");
        }
        if (!(userDir.canWrite() && userDir.canExecute())) {
            throw new IOException("Can't write or execute the program dir '" + userDir.getAbsolutePath() + "'");
        }
    }
    
    /** Set user directory to custom path */
    public static void setUserDir(String path) {
        if (persistenceManager != null) {
            throw new IllegalStateException("Persistence manager already exists");
        }
        logger.fine("Setting new userdir path: " + path);

        userDir = new File(path);
        configFile = new File(userDir, CONFIG_FILENAME);
        contactsFile = new File(userDir, CONTACTS_FILENAME);
        queueFile = new File(userDir, QUEUE_FILENAME);
        historyFile = new File(userDir, HISTORY_FILENAME);
        keyringFile = new File(userDir, KEYRING_FILENAME);
        lockFile = new File(userDir, LOCK_FILENAME);
        customPathSet = true;
    }
    
    /** Get user configuration dir */
    public static File getUserDir() {
        return userDir;
    }
    
    /** Get PersistenceManager */
    public static PersistenceManager getInstance() throws IOException {
        if (persistenceManager == null) {
            persistenceManager = new PersistenceManager();
        }
        return persistenceManager;
    }
    
    /** Save program configuration */
    public void saveConfig() throws IOException {
        logger.fine("Saving config...");
        //store current program version into config
        Config.getInstance().setVersion(Config.getLatestVersion());
        
        File temp = createTempFile();
        XMLEncoder xmlEncoder = new XMLEncoder(
                new BufferedOutputStream(new FileOutputStream(temp)));
        xmlEncoder.writeObject(Config.getInstance());
        xmlEncoder.close();
        moveFileSafely(temp, configFile);
        logger.finer("Saved config into file: " + configFile.getAbsolutePath());
    }
    
    /** Load program configuration */
    public void loadConfig() throws IOException {
        logger.fine("Loading config...");
        if (configFile.exists()) {
            XMLDecoder xmlDecoder = new XMLDecoder(
                    new BufferedInputStream(new FileInputStream(configFile)));
            Config newConfig = (Config) xmlDecoder.readObject();
            xmlDecoder.close();
            if (newConfig != null) {
                Config.setSharedInstance(newConfig);
            }
        }
    }
    
    /** Save contacts */
    public void saveContacts() throws IOException {
        logger.fine("Saving contacts...");
        File temp = createTempFile();
        ExportManager.exportContacts(Contacts.getInstance().getAll(), temp);
        moveFileSafely(temp, contactsFile);
        logger.finer("Saved contacts into file: " + contactsFile.getAbsolutePath());
    }
       
    /** Load contacts */
    public void loadContacts() throws Exception {
        logger.fine("Loading contacts...");
        if (contactsFile.exists()) {
            ArrayList<Contact> newContacts = ImportManager.importContacts(contactsFile,
                    ContactParser.ContactType.ESMSKA_FILE);
            ContinuousSaveManager.disableContacts();
            Contacts.getInstance().clear();
            Contacts.getInstance().addAll(newContacts);
            ContinuousSaveManager.enableContacts();
        }
    }
    
    /** Save sms queue */
    public void saveQueue() throws IOException {
        logger.fine("Saving queue...");
        File temp = createTempFile();
        ExportManager.exportQueue(Queue.getInstance().getAll(), temp);
        moveFileSafely(temp, queueFile);
        logger.finer("Saved queue into file: " + queueFile.getAbsolutePath());
    }
    
    /** Load sms queue */
    public void loadQueue() throws IOException {
        logger.fine("Loading queue");
        if (queueFile.exists()) {
            ArrayList<SMS> newQueue = ImportManager.importQueue(queueFile);
            ContinuousSaveManager.disableQueue();
            Queue.getInstance().clear();
            Queue.getInstance().addAll(newQueue);
            ContinuousSaveManager.enableQueue();
        }
    }
    
    /** Save sms history */
    public void saveHistory() throws IOException {
        logger.fine("Saving history...");
        File temp = createTempFile();
        ExportManager.exportHistory(History.getInstance().getRecords(), temp);
        moveFileSafely(temp, historyFile);
        logger.finer("Saved history into file: " + historyFile.getAbsolutePath());
    }
    
    /** Load sms history */
    public void loadHistory() throws Exception {
        logger.fine("Loading history...");
        if (historyFile.exists()) {
            ArrayList<History.Record> records = ImportManager.importHistory(historyFile);
            ContinuousSaveManager.disableHistory();
            History.getInstance().clearRecords();
            History.getInstance().addRecords(records);
            ContinuousSaveManager.enableHistory();
        }
    }
    
    /** Save keyring. */
    public void saveKeyring() throws Exception {
        logger.fine("Saving keyring...");
        File temp = createTempFile();
        ExportManager.exportKeyring(Keyring.getInstance(), temp);
        moveFileSafely(temp, keyringFile);
        logger.finer("Saved keyring into file: " + keyringFile.getAbsolutePath());
    }
    
    /** Load keyring. */
    public void loadKeyring() throws Exception {
        logger.fine("Loading keyring...");
        if (keyringFile.exists()) {
            ContinuousSaveManager.disableKeyring();
            Keyring.getInstance().clearKeys();
            ImportManager.importKeyring(keyringFile);
            ContinuousSaveManager.enableKeyring();
        }
    }
    
    /** Load operators
     * @throws IOException When there is problem accessing operator directory or files
     * @throws IntrospectionException When current JRE does not support JavaScript execution
     */
    public void loadOperators() throws IOException, IntrospectionException {
        logger.fine("Loading operators...");
        TreeSet<Operator> newOperators = new TreeSet<Operator>();
        if (operatorDir.exists()) {
            newOperators = ImportManager.importOperators(operatorDir);
        } else if (PersistenceManager.class.getResource(OPERATOR_RESOURCE) != null) {
            newOperators = ImportManager.importOperators(OPERATOR_RESOURCE);
        } else {
            throw new IOException("Could not find operator directory '" +
                    operatorDir.getAbsolutePath() + "' nor jar operator resource '" +
                    OPERATOR_RESOURCE + "'");
        }
        Operators.getInstance().clear();
        Operators.getInstance().addAll(newOperators);
    }
    
    /** Checks if this is the first instance of the program.
     * Manages instances by using an exclusive lock on a file.
     * @return true if this is the first instance run; false otherwise
     */
    public boolean isFirstInstance() {
        try {
            FileOutputStream out = new FileOutputStream(lockFile);
            FileChannel channel = out.getChannel();
            lock = channel.tryLock();
            if (lock == null) {
                return false;
            }
            lockFile.deleteOnExit();
        } catch (Throwable t) {
            logger.log(Level.INFO, "Program lock could not be obtained", t);
            return false;
        }
        return true;
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
        if (!file.exists()) {
            return null;
        }
        String backupName = file.getAbsolutePath() + "~";
        File backup = new File(backupName);
        FileUtils.copyFile(file, backup);
        file.delete();
        return backup;
    }
}
