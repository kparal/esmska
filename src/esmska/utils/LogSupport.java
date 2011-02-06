/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.apache.commons.lang.Validate;

/** Support class for configuring logging capabilities
 *
 * @author ripper
 */
public class LogSupport {

    private static final Logger logger = Logger.getLogger(LogSupport.class.getName());
    private static final Logger esmskaLogger = Logger.getLogger("esmska");
    private static final Logger httpclientLogger = Logger.getLogger("org.apache.commons.httpclient");
    private static final Logger httpclientWireLogger = Logger.getLogger("httpclient.wire.header");
    private static final Logger[] loggers = new Logger[]{esmskaLogger,
        httpclientLogger, httpclientWireLogger};
    private static ConsoleHandler consoleHandler = null;
    private static FileHandler fileHandler = null;
    private static final ArrayList<LogRecord> logBuffer = new ArrayList<LogRecord>();
    private static final BufferHandler bufferHandler = new BufferHandler();

    /** Initialize logging. Enables console handler. Use at startup. */
    public static void init() {
        esmskaLogger.setLevel(Level.FINER);
        esmskaLogger.setUseParentHandlers(false);

        consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.INFO);

        for (Logger log : loggers) {
            log.addHandler(consoleHandler);
        }
    }

    /** Initialize logfile file handler. Republishes any previously stored
     * records into it.
     * @param logOutput where to log; not null
     */
    public static void initFileHandler(File logOutput) throws IOException {
        Validate.notNull(logOutput);
        
        String pattern = logOutput.getAbsolutePath().replaceAll("%", "%%");
        fileHandler = new FileHandler(pattern, true);
        fileHandler.setLevel(Level.ALL);
        fileHandler.setFormatter(new SimpleFormatter());
        fileHandler.setEncoding("UTF-8");

        for (Logger log : loggers) {
            log.addHandler(fileHandler);
        }

        for (LogRecord record : logBuffer) {
            fileHandler.publish(record);
        }

        logger.fine("Started logging into " + logOutput.getAbsolutePath());
    }

    /** Whether to rememeber log records (useful for later inicialization of
     * file handler) or not (and erase old ones).
     */
    public static void storeRecords(boolean store) {
        if (store) {
            for (Logger log : loggers) {
                log.addHandler(bufferHandler);
            }
        } else {
            for (Logger log : loggers) {
                log.removeHandler(bufferHandler);
            }
            logBuffer.clear();
        }
    }

    /** Get main program logger */
    public static Logger getEsmskaLogger() {
        return esmskaLogger;
    }

    /** Get handler for writing logs into console */
    public static ConsoleHandler getConsoleHandler() {
        return consoleHandler;
    }

    /** Get handler for writing logs into file */
    public static FileHandler getFileHandler() {
        return fileHandler;
    }

    /** Enable debug messages from HttpClient library */
    public static void enableHttpClientLogging() {
        httpclientLogger.setLevel(Level.FINE);
        httpclientWireLogger.setLevel(Level.FINE);
    }

    /** Fake handler to remember all published messages */
    private static class BufferHandler extends Handler {

        public BufferHandler() {
            setLevel(Level.ALL);
        }

        @Override
        public void publish(LogRecord record) {
            logBuffer.add(record);
        }

        @Override
        public void flush() {
            return;
        }

        @Override
        public void close() throws SecurityException {
            return;
        }
    }
}
