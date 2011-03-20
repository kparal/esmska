
package esmska.gui;

import esmska.data.SMS;
import esmska.transfer.ImageCodeResolver;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/** GUI implementation of image resolving. Displays the image to the user and
 * asks him to provide the security code.
 *
 * @author ripper
 */
public class GUIImageCodeResolver implements ImageCodeResolver {
    private static final Logger logger = Logger.getLogger(GUIImageCodeResolver.class.getName());

    /** This method <b>must not</b> be called from EDT. For description see
     * {@link ImageCodeResolver#resolveImageCode(esmska.data.SMS)}
     */
    @Override
    public boolean resolveImageCode(final SMS sms) {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Called from EDT");
        }
        Validate.notNull(sms);

        //if there is no image and no hint, than there is nothing to recognize
        if (sms.getImage() == null && StringUtils.isEmpty(sms.getImageHint())) {
            sms.setImageCode(null);
            return false;
        }

        //we will use semaphore to wait for result in sending thread
        final Semaphore semaphore = new Semaphore(0);

        try {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    GatewayMessageFrame gmf = GatewayMessageFrame.getInstance();
                    gmf.addImageCodeMsg(sms, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            semaphore.release();
                        }
                    });
                }
            });

            //wait for result by blocking current thread
            semaphore.acquire();

            return StringUtils.isNotEmpty(sms.getImageCode());
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Resolving image code interrupted", ex);
            return false;
        }

    }

}
