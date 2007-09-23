/*
 * CommandLineParser.java
 *
 * Created on 23. září 2007, 14:21
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/** Parses program arguments from command line
 *
 * @author ripper
 */
public class CommandLineParser {
    private boolean portable;
    private String configPath;
    
    /** Creates a new instance of CommandLineParser */
    public CommandLineParser() {
    }
    
    
    /** Parse command line arguments
     * @return true, if arguments' syntax was ok, false otherwise
     */
    public boolean parseArgs(String[] args) {
        List<String> arguments = Arrays.asList(args);
        
        for (Iterator it = arguments.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if (arg.equals("-h") || arg.equals("--help")) {
                printUsage();
                System.exit(1);
            } else if (arg.equals("-p") || arg.equals("--portable")) {
                portable = true;
            } else if (arg.equals("-c") || arg.equals("--config")) {
                if (!it.hasNext()) {
                    System.err.println("Chybí cesta!");
                    printUsage();
                    System.exit(1);
                }
                configPath = (String) it.next();
            } else {
                System.err.println("Neznámá volba '" + arg + "'!");
                printUsage();
                return false;
            }
        }
        
        return true;
    }
    
    public boolean isPortable() {
        return portable;
    }
    
    public String getConfigPath() {
        return configPath;
    }
    
    /** Print usage help */
    private static void printUsage() {
        String usage =
                "Použití: java -jar esmska.jar [VOLBY]\n" +
                "\n" +
                "Dostupné volby:\n" +
                "   -h, --help                  Zobrazí tuto nápovědu\n" +
                "   -p, --portable              Zapnutí přenosného módu - " +
                "zeptá se na umístění konfiguračních souborů\n" +
                "   -c, --config <cesta>        Nastavení cesty ke konfiguračním souborům";
        System.out.println(usage);
    }
}
