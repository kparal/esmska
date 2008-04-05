/*
 * CommandLineParser.java
 *
 * Created on 23. září 2007, 14:21
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package esmska;

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

    private static final Options options = new Options();
    private static final Option help = new Option("h", "help", false, "Zobrazí tuto nápovědu");
    private static final Option portable = new Option("p", "portable", false, "Zapnutí přenosného módu - " +
            "zeptá se na umístění uživatelského adresáře. Nelze použít s -c.");
    private static final Option config = OptionBuilder.withArgName("cesta").hasArg().
            withDescription("Nastavení cesty k uživatelskému adresáři. Nelze použít s -p.").
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
            System.err.println("Neplatná volba na příkazovém řádku! ('" + ex.getMessage() + "')");
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
        formatter.setSyntaxPrefix("Použití: ");
        formatter.printHelp("java -jar esmska.jar [VOLBY]", "\nDostupné volby:", options, null);
    }
}
