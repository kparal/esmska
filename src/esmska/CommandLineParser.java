package esmska;

import esmska.data.Config;
import esmska.utils.L10N;
import esmska.utils.LogSupport;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.StringUtils;

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
    private static final Option version = new Option(null, "version", false,
            l10n.getString("CommandLineParser.version"));
    private static final String debugDesc = MessageFormat.format(
            l10n.getString("CommandLineParser.debug"),
            "standard", "network", "full");
    private static final Option debug = OptionBuilder.
            withArgName(l10n.getString("CommandLineParser.debugMode")).
            hasOptionalArg().withDescription(debugDesc).withLongOpt("debug").
            create();

    static {
        OptionGroup configGroup = new OptionGroup();
        configGroup.addOption(portable);
        configGroup.addOption(config);

        options.addOption(help);
        options.addOptionGroup(configGroup);
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
                configPath = opts.get(opts.indexOf(config)).getValue();
            }
            if (opts.contains(version)) {
                System.out.println("Esmska " + Config.getLatestVersion());
                System.exit(0);
            }
            if (opts.contains(debug)) {
                String debugMode = opts.get(opts.indexOf(debug)).getValue();

                //in debug mode always print everything on console
                LogSupport.getConsoleHandler().setLevel(Level.ALL);

                //enable httpclient debug, restrict program debug
                if (StringUtils.equals(debugMode, "network")) {
                    LogSupport.enableHttpClientLogging();
                    LogSupport.getEsmskaLogger().setLevel(Level.INFO);
                }
                //enable httpclient and full program debug (with web content)
                if (StringUtils.equals(debugMode, "full")) {
                    LogSupport.enableHttpClientLogging();
                    LogSupport.getEsmskaLogger().setLevel(Level.ALL);
                }
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
