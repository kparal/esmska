/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.persistence;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.Validate;

/** Manager for taking care of backups of user configuration files.
 *
 * @author ripper
 */
public class BackupManager {
    private static final Logger logger = Logger.getLogger(BackupManager.class.getName());
    private final File backupRoot;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);


    /** Constructor.
     *
     * @param backupRoot Directory under which the backups will be created and managed.
     * Not null.
     */
    public BackupManager(File backupRoot) {
        Validate.notNull(backupRoot);
        this.backupRoot = backupRoot;
    }

    /** Back up selected files to backup directory. A new subdirectory in a format
     * of yyyy-mm-dd will be created for them.
     *
     * @param files files to back up. Not null. Non-existent files are ignored.
     * @param overwrite if backup should overwrite already existing subdirectory
     * (i.e. backup from today)
     * @return true if files were backed up; false if they weren't (e.g. backup
     * already existed and wasn't overwritten)
     */
    public boolean backupFiles(Collection<File> files, boolean overwrite) throws IOException {
        Validate.notNull(files);

        String today = dateFormat.format(new Date());
        File backupDir = new File(backupRoot, today);

        if (backupDir.exists()) {
            if (!overwrite) {
                logger.fine("Backup already exists in '" + backupDir.getAbsolutePath() +
                        "', skipping backup");
                return false;
            }
        } else {
            FileUtils.forceMkdir(backupDir);
        }

        //copy files
        for (File file : files) {
            if (!file.exists()) {
                //ignore non-existent files
                continue;
            }
            FileUtils.copyFileToDirectory(file, backupDir);
        }

        logger.fine("Files backed up to '" + backupDir.getAbsolutePath() + "'");
        return true;
    }

    /** Clean backup directory from old backup subdirs.
     *
     * @param backupsPreserved how many old backups should be preserved. All other
     * (older) backups will be deleted. Negative integer is converted to 0.
     */
    public void removeOldBackups(int backupsPreserved) throws IOException {
        if (backupsPreserved < 0) {
            backupsPreserved = 0;
        }

        File[] subdirs = backupRoot.listFiles();
        HashMap<Date, File> dateFiles = new HashMap<Date, File>();
        for (File dir : subdirs) {
            if (!dir.isDirectory()) {
                continue;
            }
            try {
                Date date = dateFormat.parse(dir.getName());
                dateFiles.put(date, dir);
            } catch (ParseException ex) {
                logger.log(Level.WARNING, "Unknown unparsable dir found in backups folder: '" +
                        dir.getAbsolutePath() + "'", ex);
                //just skip it
            }
        }

        //sort directories by date
        ArrayList<Date> dates = new ArrayList<Date>(dateFiles.keySet());
        Collections.sort(dates);
        Collections.reverse(dates);

        //delete old directories
        if (dates.size() > backupsPreserved) {
            Date[] datesArray = dates.toArray(new Date[]{});
            Date[] toRemove = Arrays.copyOfRange(datesArray, backupsPreserved, datesArray.length);

            for (Date d : toRemove) {
                File dir = dateFiles.get(d);
                FileUtils.deleteDirectory(dir);
            }
        }
    }
}
