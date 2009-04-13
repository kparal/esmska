/*
 * ImportVCardTransferHandler.java
 */
package esmska.gui.dnd;

import esmska.gui.ImportFrame;
import esmska.gui.MainFrame;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.TransferHandler;

/**
 * Transfer handler for importing vCard (vcard, vcf) files to Esmska.
 *
 * @author  Marian Bouček
 * @version 1.0
 */
public class ImportVCardTransferHandler extends TransferHandler {

    private static final Logger logger = Logger.getLogger(ImportVCardTransferHandler.class.getName());
    private static DataFlavor uriListFlavor; //flavor for dropping files in Gnome or KDE

    static {
        try {
            uriListFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
        } catch (ClassNotFoundException ex) {
            assert false : "Can't happen";
            ex.printStackTrace();
        }
    }

    @Override
    public boolean canImport(TransferSupport support) {
        if (!support.isDrop()) {
            return false;
        }

        if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor) &&
                !support.isDataFlavorSupported(uriListFlavor)) {
            return false;
        }

        support.setDropAction(TransferHandler.COPY);
        return true;
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }

        if (!isVCard(support)) {
            return false;
        }

        List<File> files = getInputFiles(support);
        String fileName = files.get(0).getAbsolutePath();

        ImportFrame importFrame = new ImportFrame();
        importFrame.setLocationRelativeTo(MainFrame.getInstance());
        importFrame.importVCardFile(fileName);
        importFrame.setVisible(true);

        return true;
    }

    /** Check if transfer support contains exactly one vCard file */
    private boolean isVCard(TransferSupport support) {
        List<File> files = getInputFiles(support);
        if (files.size() != 1) {
            return false;
        }

        String fileName = files.get(0).getAbsolutePath();
        String lower = fileName.toLowerCase();

        boolean supported = lower.endsWith("vcard") || lower.endsWith("vcf");
        return supported;
    }

    @SuppressWarnings("unchecked")
    /** Get list of files from transfer support */
    private List<File> getInputFiles(TransferSupport support) {
        try {
            if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                Object data = support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                return (List<File>) data;
            } else if (support.isDataFlavorSupported(uriListFlavor)) {
                Object data = support.getTransferable().getTransferData(uriListFlavor);
                return textURIListToFileList((String) data);
            }
        } catch (Exception ex) {
            logger.log(Level.INFO, "Problem getting dropped files", ex);
        }
        return Collections.emptyList();
    }

    /** Convert list of URIs to list of files */
    private static List<File> textURIListToFileList(String data) {
        List<File> list = new ArrayList<File>(1);
        for (StringTokenizer st = new StringTokenizer(data, "\r\n");
                st.hasMoreTokens();) {
            String s = st.nextToken();
            if (s.startsWith("#")) {
                // the line is a comment (as per the RFC 2483)
                continue;
            }
            try {
                URI uri = new URI(s);
                File file = new File(uri);
                list.add(file);
            } catch (java.net.URISyntaxException e) {
                // malformed URI
            } catch (IllegalArgumentException e) {
                // the URI is not a valid 'file:' URI
            }
        }
        return list;
    }
}
