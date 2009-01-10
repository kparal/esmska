/*
 * CommandLineParser.java
 *
 * Created on 23. září 2007, 14:21
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package esmska;

import esmska.data.Config;
import esmska.utils.L10N;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/** Parses program arguments from command line
 *
 * @author ripper
 */
@SuppressWarnings("static-access")
public class CommandLineParser {
    private static final ResourceBundle l10n = L10N.l10nBundle;
    
    private static final Options options = new Options();
    private static final Option help = new Option("h", "help", false, 
            l10n.getString("CommandLineParser.show_this_help"));
    private static final Option portable = new Option("p", "portable", false,
            l10n.getString("CommandLineParser.enable_portable_mode"));
    private static final Option config = OptionBuilder.withArgName(
            l10n.getString("CommandLineParser.path")).hasArg().
            withDescription(l10n.getString("CommandLineParser.set_user_path")).
            withLongOpt("config").create("c");
    private static final Option debugNetwork = new Option(null, "debug-network", false,
            l10n.getString("CommandLineParser.debugNetwork"));
    private static final Option debugNetworkFull = new Option(null, "debug-network-full", false,
            l10n.getString("CommandLineParser.debugNetworkFull"));
    private static final Option version = new Option(null, "version", false,
            l10n.getString("CommandLineParser.version"));
    private static final Option debug = new Option(null, "debug", false,
            l10n.getString("CommandLineParser.debug"));

    static {
        OptionGroup configGroup = new OptionGroup();
        configGroup.addOption(portable);
        configGroup.addOption(config);

        options.addOption(help);
        options.addOptionGroup(configGroup);
        options.addOption(debugNetwork);
        options.addOption(debugNetworkFull);
        options.addOption(version);
        options.addOption(debug);
    }
    private boolean isPortable;
    private String configPath;

    /** Parse command line arguments
     * @return true, if arguments' syntax was ok, false otherwise
     */
    public boolean parseArgs(String[] args) {
        try {
            PosixParser parser = new PosixParser();
            CommandLine cmd = parser.parse(options, args);
            List<Option> opts = Arrays.asList(cmd.getOptions());
            
            if (opts.contains(help)) {
                printUsage();
                System.exit(0);
            }
            if (opts.contains(portable)) {
                isPortable = true;
            }
            if (opts.contains(config)) {
                configPath = config.getValue();
            }
            if (opts.contains(debugNetwork)) {
                System.setProperty("org.apache.commons.logging.Log",
                        "org.apache.commons.logging.impl.SimpleLog");
                System.setProperty("org.apache.commons.logging.simplelog.showdatetime", 
                        "true");
                System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header",
                        "debug");
                System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient",
                        "debug");
            }
            if (opts.contains(debugNetworkFull)) {
                System.setProperty("org.apache.commons.logging.Log",
                        "org.apache.commons.logging.impl.SimpleLog");
                System.setProperty("org.apache.commons.logging.simplelog.showdatetime",
                        "true");
                System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", 
                        "debug");
                System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", 
                        "debug");
            }
            if (opts.contains(version)) {
                System.out.println("Esmska " + Config.getLatestVersion());
                System.exit(0);
            }
            if (opts.contains(debug)) {
                Logger mainLogger = Logger.getLogger("esmska");
                mainLogger.setLevel(Level.ALL);
                
                ConsoleHandler console = new ConsoleHandler();
                console.setLevel(Level.ALL);
                mainLogger.addHandler(console);
                mainLogger.setUseParentHandlers(false);
            }

        } catch (ParseException ex) {
            System.err.println(
                    MessageFormat.format(l10n.getString("CommandLineParser.invalid_option"), ex.getMessage()));
            printUsage();
            return false;
        }

        return true;
    }

    /** Whether portable mode is enabled or disabled */
    public boolean isPortable() {
        return isPortable;
    }

    /** User custom path to configuration files */
    public String getConfigPath() {
        return configPath;
    }

    /** Print usage help */
    private static void printUsage() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setSyntaxPrefix(l10n.getString("CommandLineParser.usage") + " ");
        formatter.printHelp("java -jar esmska.jar [" + l10n.getString("CommandLineParser.options") + "]",
                "\n" + l10n.getString("CommandLineParser.available_options"), options, null);
    }
}
