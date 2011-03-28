package esmska.integration;

import java.io.File;

/** Integration for MS Windows.
 *
 * @author ripper
 */
public class WindowsIntegration extends IntegrationAdapter {

    @Override
    public File getConfigDir(File defaultConfigDir) {
        String configDir = System.getenv("APPDATA");
        return new File(configDir);
    }

    @Override
    public File getDataDir(File defaultDataDir) {
        String dataDir = System.getenv("APPDATA");
        return new File(dataDir);
    }

}
