package esmska.transfer;

import org.apache.commons.lang.Validate;

/** Manager of different security image resolvers.
 *
 * @author ripper
 */
public class ImageCodeManager {
    private static ImageCodeResolver resolver = new DefaultImageCodeResolver();

    /** Get current preferred security image resolver. By default the
     * {@link DefaultImageCodeResolver} is returned.
     */
    public synchronized static ImageCodeResolver getResolver() {
        return resolver;
    }

    /** Set a security image resolver which should be used from now on */
    public synchronized static void setResolver(ImageCodeResolver resolver) {
        Validate.notNull(resolver);
        ImageCodeManager.resolver = resolver;
    }
}
