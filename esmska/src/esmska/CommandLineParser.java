/*
 * CommandLineParser.java
 *
 * Created on 23. září 2007, 14:21
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package esmska;

import esmska.utils.L10N;
import java.text.MessageFormat;
import java.util.ResourceBundle;
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
    private static final Option help = new Option("h", "help", false, l10n.getString("CommandLineParser.show_this_help"));
    private static final Option portable = new Option("p", "portable", false, l10n.getString("CommandLineParser.enable_portable_mode"));
    private static final Option config = OptionBuilder.withArgName(l10n.getString("CommandLineParser.path")).hasArg().
            withDescription(l10n.getString("CommandLineParser.set_user_path")).
            withLongOpt("config").create("c");
    private static final OptionGroup configGroup = new OptionGroup();

    static {
        configGroup.addOption(portable);
        configGroup.addOption(config);

        options.addOption(help);
        options.addOptionGroup(configGroup);
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

            if (cmd.hasOption(help.getOpt())) {
                printUsage();
                System.exit(0);
            }
            if (cmd.hasOption(portable.getOpt())) {
                isPortable = true;
            }
            if (cmd.hasOption(config.getOpt())) {
                configPath = config.getValue();
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
        formatter.setSyntaxPrefix(l10n.getString("CommandLineParser.usage"));
        formatter.printHelp(l10n.getString("CommandLineParser.basic_usage"), 
                "\n" + l10n.getString("CommandLineParser.available_options"), options, null);
    }
}
