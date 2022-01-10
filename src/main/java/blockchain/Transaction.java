package blockchain;

import blockchain.utils.CryptoUtils;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;

public class Transaction implements Serializable {

    private static final long serialVersionUID = 1464099182453904322L;
    private final long id;
    private final long fromUser;
    private final long toUser;
    private final long amount;
    private byte[] signature;

    public Transaction(long id, long fromUser, long toUser, long amount) {
        this.id = id;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.amount = amount;
    }

    public long getId() {
        return id;
    }

    public long getSenderId() {
        return fromUser;
    }

    public long getAmount() {
        return amount;
    }

    public long getRecipientId() {
        return toUser;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void sign(PrivateKey privateKey) {
        try {
            signature = CryptoUtils.signData(privateKey, getTransactionBytes());
        } catch (Exception e) {
            System.out.println(e.getMessage());
            signature = null;
        }
    }

    public byte[] getTransactionBytes() {
        return String.format("Id:%dFrom:%dTo:%dVC:%d",id, fromUser, toUser, amount).getBytes(StandardCharsets.UTF_8);
    }
}
