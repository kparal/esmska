package esmska.data;

/** This class contains user-configurable gateway properties.
 */
public class GatewayConfig {

    private String signature;
    private boolean receipt;

    /** Get assigned Signature name. */
    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    /** Whether to ask for delivery receipt.*/
    public boolean isReceipt() {
        return receipt;
    }

    public void setReceipt(boolean receipt) {
        this.receipt = receipt;
    }
}
