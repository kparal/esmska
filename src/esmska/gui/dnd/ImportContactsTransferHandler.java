/*
 * ImportContactsTransferHandler.java
 */
package esmska.gui.dnd;

import esmska.Context;
import esmska.data.Contact;
import esmska.gui.ImportFrame;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.TransferHandler;
import org.apache.commons.lang.StringUtils;

/**
 * Transfer handler for importing contacts to Esmska.
 * Allows to drop vCard (vcard, vcf) files or string with contact name/number.
 *
 * @author  Marian Bouček
 * @version 1.0
 */
public class ImportContactsTransferHandler extends TransferHandler {

    private static final Logger logger = Logger.getLogger(ImportContactsTransferHandler.class.getName());
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
                !support.isDataFlavorSupported(uriListFlavor) &&
                !support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            return false;
        }

        support.setDropAction(TransferHandler.COPY);
        return true;
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            logger.warning("Can't import contacts data by drag&drop, unsupported format.");
            return false;
        }

        try {
            //we must try uriListFlavor before stringFlavor, because stringFlavor
            //appears to be subset of it
            if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ||
                    support.isDataFlavorSupported(uriListFlavor)) {
                //dropped a file

                if (!isVCard(support)) {
                    logger.warning("Can't import contacts data by drag&drop, not a vCard file.");
                    return false;
                }

                List<File> files = getInputFiles(support);
                String fileName = files.get(0).getAbsolutePath();

                ImportFrame importFrame = new ImportFrame();
                importFrame.setLocationRelativeTo(Context.mainFrame);
                importFrame.importVCardFile(fileName);
                importFrame.setVisible(true);
            } else if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                //dropped a string

                String data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);

                //decide if name or number
                String name = null, number = null;
                StringUtils.trim(data);
                if (data == null || data.length() > 100 || data.contains("\n")) {
                    //no data, too long or not valid
                    logger.warning("Can't import contacts data by drag&drop, not a valid string.");
                    return false;
                }
                String pNumber = Contact.parseNumber(data);
                Matcher matcher = Pattern.compile("[+0-9]").matcher(data);
                int numbers = 0;
                while (matcher.find()) {
                    numbers++;
                }
                if (pNumber != null && numbers > data.length() / 2) {
                    //at least 50% chars were numbers, it's a number
                    number = pNumber;
                } else {
                    //it's name
                    name = data;
                }

<<<<<<< HEAD
                Contact skeleton = new Contact(name, number, null,"fff");
=======
                Contact skeleton = new Contact(name, number, null);
>>>>>>> origin/work
                Context.mainFrame.getContactPanel().showAddContactDialog(skeleton);
            } else {
                String msg = "Unknown supported DnD flavor: " + Arrays.toString(support.getDataFlavors());
                assert false : msg;
                logger.warning(msg);
            }
        } catch (Exception ex) {
            logger.log(Level.INFO, "Problem getting dropped data", ex);
            return false;
        }
        return true;
    }

    /** Check if transfer support contains exactly one vCard file */
    private boolean isVCard(TransferSupport support)
            throws UnsupportedFlavorException, IOException {
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
    private List<File> getInputFiles(TransferSupport support) 
            throws UnsupportedFlavorException, IOException {
        if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            Object data = support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            return (List<File>) data;
        } else if (support.isDataFlavorSupported(uriListFlavor)) {
            Object data = support.getTransferable().getTransferData(uriListFlavor);
            return textURIListToFileList((String) data);
        } else {
            return Collections.emptyList();
        }
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
