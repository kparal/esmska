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
import esmska.data.Operator;
import esmska.utils.RuntimeUtils;
import java.text.MessageFormat;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

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
    private static final String GLOBAL_CONFIG_FILENAME = "esmska.conf";
    private static final String CONTACTS_FILENAME = "contacts.csv";
    private static final String QUEUE_FILENAME = "queue.csv";
    private static final String HISTORY_FILENAME = "history.csv";
    private static final String KEYRING_FILENAME = "keyring.csv";
    private static final String LOCK_FILENAME = "running.lock";
    private static final String OPERATOR_RESOURCE = "/esmska/operators/scripts";
    
    private static File configDir =
            new File(System.getProperty("user.home") + File.separator + ".config",
            PROGRAM_DIRNAME);
    private static File dataDir =
            new File(System.getProperty("user.home") + File.separator + ".local" +
            File.separator + "share", PROGRAM_DIRNAME);

    private static File globalOperatorDir = new File(OPERATOR_DIRNAME);
    private static File localOperatorDir = new File(dataDir, OPERATOR_DIRNAME);
    private static File configFile = new File(configDir, CONFIG_FILENAME);
    private static File globalConfigFile = new File(GLOBAL_CONFIG_FILENAME);
    private static File contactsFile = new File(configDir, CONTACTS_FILENAME);
    private static File queueFile = new File(configDir, QUEUE_FILENAME);
    private static File historyFile = new File(configDir, HISTORY_FILENAME);
    private static File keyringFile = new File(configDir, KEYRING_FILENAME);
    private static File lockFile = new File(configDir, LOCK_FILENAME);
    
    private static boolean customPathSet;
    private FileLock lock;
    
    /** Creates a new instance of PersistenceManager */
    private PersistenceManager() throws IOException {
        //adjust program dir according to operating system
        if (!customPathSet) {
            String confDir, datDir;
            
            switch (RuntimeUtils.detectOS()) {
                case MAC_OS_X:
                    confDir = System.getProperty("user.home") + "/Library/Application Support";
                    datDir = confDir;
                    break;
                case WINDOWS:
                    confDir = System.getenv("APPDATA");
                    datDir = confDir;
                    break;
                case LINUX:
                default:
                    confDir = System.getenv("XDG_CONFIG_HOME");
                    datDir = System.getenv("XDG_DATA_HOME");
                    break;
            }
            
            if (StringUtils.isNotEmpty(confDir)) {
                setConfigDir(confDir + File.separator + PROGRAM_DIRNAME);
            }
            if (StringUtils.isNotEmpty(datDir)) {
                setDataDir(datDir + File.separator + PROGRAM_DIRNAME);
            }
        }
        
        //create config dir if necessary
        if (!configDir.exists() && !configDir.mkdirs()) {
            throw new IOException("Can't create config dir '" + configDir.getAbsolutePath() + "'");
        }
        if (!(canWrite(configDir) && configDir.canExecute())) {
            throw new IOException("Can't write or execute the config dir '" + configDir.getAbsolutePath() + "'");
        }
        //create data dir if necessary
        if (!dataDir.exists() && !dataDir.mkdirs()) {
            throw new IOException("Can't create data dir '" + dataDir.getAbsolutePath() + "'");
        }
        if (!(canWrite(dataDir) && dataDir.canExecute())) {
            throw new IOException("Can't write or execute the data dir '" + dataDir.getAbsolutePath() + "'");
        }
        //create local operators dir
        if (!localOperatorDir.exists() && !localOperatorDir.mkdirs()) {
            throw new IOException("Can't create local operator dir '" + localOperatorDir.getAbsolutePath() + "'");
        }
    }
    
    /** Set config directory */
    private static void setConfigDir(String path) {
        if (persistenceManager != null) {
            throw new IllegalStateException("Persistence manager already exists");
        }
        logger.fine("Setting new config dir path: " + path);

        configDir = new File(path);
        configFile = new File(configDir, CONFIG_FILENAME);
        contactsFile = new File(configDir, CONTACTS_FILENAME);
        queueFile = new File(configDir, QUEUE_FILENAME);
        historyFile = new File(configDir, HISTORY_FILENAME);
        keyringFile = new File(configDir, KEYRING_FILENAME);
        lockFile = new File(configDir, LOCK_FILENAME);
    }
    
    /** Get configuration directory */
    public static File getConfigDir() {
        return configDir;
    }

    /** Set data directory */
    private static void setDataDir(String path) {
        if (persistenceManager != null) {
            throw new IllegalStateException("Persistence manager already exists");
        }
        logger.fine("Setting new data dir path: " + path);

        dataDir = new File(path);
        localOperatorDir = new File(dataDir, OPERATOR_DIRNAME);
    }

    /** Get data directory */
    public static File getDataDir() {
        return dataDir;
    }

    /** Set custom directories */
    public static void setCustomDirs(String configDir, String dataDir) {
        setConfigDir(configDir);
        setDataDir(dataDir);
        customPathSet = true;
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
        FileOutputStream out = new FileOutputStream(temp);
        XMLEncoder xmlEncoder = new XMLEncoder(new BufferedOutputStream(out));
        xmlEncoder.writeObject(Config.getInstance());
        xmlEncoder.flush();
        out.getChannel().force(false);
        xmlEncoder.close();

        moveFileSafely(temp, configFile);
        logger.finer("Saved config into file: " + configFile.getAbsolutePath());
    }
    
    /** Load program configuration */
    public void loadConfig() throws IOException {
        //system-wide config
        if (globalConfigFile.exists()) {
            logger.fine("Loading global config...");
            try {
                ImportManager.importGlobalConfig(globalConfigFile);
            } catch (Exception ex) {
                //don't stop here, continue to load local config
                logger.log(Level.WARNING, "Failed to load global configuration: "
                        + globalConfigFile, ex);
            }
        }

        //per-user config
        if (configFile.exists()) {
            logger.fine("Loading local config...");
            XMLDecoder xmlDecoder = new XMLDecoder(
                    new BufferedInputStream(new FileInputStream(configFile)));
            Config newConfig = (Config) xmlDecoder.readObject();
            xmlDecoder.close();
            if (newConfig != null) {
                Config.setSharedInstance(newConfig);
            }
        } else {
            //set new config, to apply changes made through global config
            Config.setSharedInstance(new Config());
        }
    }
    
    /** Save contacts */
    public void saveContacts() throws IOException {
        logger.fine("Saving contacts...");

        File temp = createTempFile();
        FileOutputStream out = new FileOutputStream(temp);
        ExportManager.exportContacts(Contacts.getInstance().getAll(), out);
        out.flush();
        out.getChannel().force(false);
        out.close();

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
        FileOutputStream out = new FileOutputStream(temp);
        ExportManager.exportQueue(Queue.getInstance().getAll(), out);
        out.flush();
        out.getChannel().force(false);
        out.close();

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
        FileOutputStream out = new FileOutputStream(temp);
        ExportManager.exportHistory(History.getInstance().getRecords(), out);
        out.flush();
        out.getChannel().force(false);
        out.close();

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
        FileOutputStream out = new FileOutputStream(temp);
        ExportManager.exportKeyring(Keyring.getInstance(), out);
        out.flush();
        out.getChannel().force(false);
        out.close();
        
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
        ArrayList<Operator> globalOperators = new ArrayList<Operator>();
        TreeSet<Operator> localOperators = new TreeSet<Operator>();
        //global operators
        if (globalOperatorDir.exists()) {
            globalOperators = new ArrayList<Operator>(ImportManager.importOperators(globalOperatorDir));
        } else if (PersistenceManager.class.getResource(OPERATOR_RESOURCE) != null) {
            globalOperators = new ArrayList<Operator>(ImportManager.importOperators(OPERATOR_RESOURCE));
        } else {
            throw new IOException("Could not find operator directory '" +
                    globalOperatorDir.getAbsolutePath() + "' nor jar operator resource '" +
                    OPERATOR_RESOURCE + "'");
        }
        //local operators
        if (localOperatorDir.exists()) {
            localOperators = ImportManager.importOperators(localOperatorDir);
        }
        //replace old global versions with new local ones
        for (Operator op : localOperators) {
            int index = globalOperators.indexOf(op);
            if (index >= 0) {
                Operator globOp = globalOperators.get(index);
                if (op.getVersion().compareTo(globOp.getVersion()) > 0) {
                    globalOperators.set(index, op);
                    logger.finer("Local operator " + op.getName() + " is newer, replacing global one.");
                } else {
                    logger.finer("Local operator " + op.getName() + " is not newer than global one, skipping.");
                }
            } else {
                globalOperators.add(op);
                logger.finer("Local operator " + op.getName() + " is additional to global ones, adding to operator list.");
            }
        }
        //load it
        Operators.getInstance().clear();
        Operators.getInstance().addAll(globalOperators);
    }

    /** Save new operator to file. New or updated operator is saved in global operator
     * directory (if there are sufficient permissions), otherwise in local operator
     * directory.
     *
     * @param scriptName name of the operator/script (without suffix), not null nor empty
     * @param scriptContents contents of the operator script file, not null nor empty
     * @param icon operator icon, may be null
     */
    public void saveOperator(String scriptName, String scriptContents, byte[] icon) throws IOException {
        Validate.notEmpty(scriptName);
        Validate.notEmpty(scriptContents);

        logger.fine("Saving operator...");

        File temp = createTempFile();
        FileOutputStream out = new FileOutputStream(temp);

        File iconTemp = null;
        FileOutputStream iconOut = null;
        if (icon != null) {
            iconTemp = createTempFile();
            iconOut = new FileOutputStream(iconTemp);
        }

        //export the operator
        ExportManager.exportOperator(scriptContents, icon, out, iconOut);

        out.flush();
        out.getChannel().force(false);
        out.close();

        if (icon != null) {
            iconOut.flush();
            iconOut.getChannel().force(false);
            iconOut.close();
        }

        //move script file to correct location
        File scriptFileGlobal = new File(globalOperatorDir, scriptName + ".operator");
        File scriptFileLocal = new File(localOperatorDir, scriptName + ".operator");
        if (canWrite(globalOperatorDir) && (!scriptFileGlobal.exists() || canWrite(scriptFileGlobal))) {
            //first try global dir
            moveFileSafely(temp, scriptFileGlobal);
            logger.finer("Saved operator script into file: " + scriptFileGlobal.getAbsolutePath());
        } else if (canWrite(localOperatorDir) && (!scriptFileLocal.exists() || canWrite(scriptFileLocal))) {
            //second try local dir
            moveFileSafely(temp, scriptFileLocal);
            logger.finer("Saved operator script into file: " + scriptFileLocal.getAbsolutePath());
        } else {
            //report error
            throw new IOException(MessageFormat.format("Could not save operator " +
                    "{0} to ''{1}'' nor to ''{2}'' - no write permissions?", scriptName,
                    scriptFileGlobal, scriptFileLocal));
        }

        //move icon file to correct location
        if (icon != null) {
            File iconFileGlobal = new File(globalOperatorDir, scriptName + ".png");
            File iconFileLocal = new File(localOperatorDir, scriptName + ".png");
            if (canWrite(globalOperatorDir) && (!iconFileGlobal.exists() || canWrite(iconFileGlobal))) {
                //first try global dir
                moveFileSafely(iconTemp, iconFileGlobal);
                logger.finer("Saved operator icon into file: " + iconFileGlobal.getAbsolutePath());
            } else if (canWrite(localOperatorDir) && (!iconFileLocal.exists() || canWrite(iconFileLocal))) {
                //second try local dir
                moveFileSafely(iconTemp, iconFileLocal);
                logger.finer("Saved operator icon into file: " + iconFileLocal.getAbsolutePath());
            } else {
                //report error
                throw new IOException(MessageFormat.format("Could not save operator icon " +
                        "{0} to '{1}' nor to '{2}' - no write permissions?", scriptName,
                        iconFileGlobal, iconFileLocal));
            }
        }
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
     * @param srcFile source file, not null
     * @param destFile destination file, not null
     */
    private void moveFileSafely(File srcFile, File destFile) throws IOException {
        Validate.notNull(srcFile);
        Validate.notNull(destFile);

        File backup = backupFile(destFile);
        try {
            FileUtils.moveFile(srcFile, destFile);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Moving of " + srcFile.getAbsolutePath() + " to " +
                    destFile.getAbsolutePath() + " failed, trying to restore from backup", ex);
            FileUtils.deleteQuietly(destFile);
            //backup may be null if the destination file didn't exist
            if (backup != null) {
                FileUtils.moveFile(backup, destFile);
            }
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

    /** Test if it is possible to write to a certain file/directory.
     * It doesn't have to exist. This method is available because of Java bug
     * on Windows which does not check permissions in File.canWrite() but only
     * read-only bit (<a href="http://www.velocityreviews.com/forums/t303199-java-reporting-incorrect-directory-permission-under-windows-xp.html">reference1</a>,
     * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4939819>referece2</a>).
     * @param file File, existing or not existing; not null
     * @return true if file can be written, false if not
     */
    public static boolean canWrite(File file) {
        Validate.notNull(file);

        if (!RuntimeUtils.isWindows()) {
            //on POSIX systems file.canWrite() works
            return file.canWrite();
        }

        //file.canWrite() does not work on Windows
        boolean success = false;
        try {
            if (file.exists()) {
                if (file.isDirectory()) {
                    //try to create file inside directory
                    String name = "writeTest.esmska";
                    File f = new File(file, name);
                    while (f.exists()) {
                        name = name + ".1";
                        f = new File(file, name);
                    }
                    f.createNewFile();
                    success = f.delete();
                } else {
                    //try to open file for writing
                    FileOutputStream out = new FileOutputStream(file);
                    out.close();
                    success = true;
                }
            } else {
                //try to create and delete the file
                FileUtils.touch(file);
                success = file.delete();
            }
        } catch (Exception ex) {
            //be quiet, don't report anything
            success = false;
        }
        return success;
    }
}
