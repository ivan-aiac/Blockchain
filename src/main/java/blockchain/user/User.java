package blockchain.user;

import blockchain.BlockChain;
import blockchain.Transaction;
import blockchain.utils.CryptoUtils;

import java.io.Serializable;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

public class User implements Serializable {
    private static final int KEY_LENGTH = 1024;
    private static final long serialVersionUID = -3714273561403539716L;
    protected final BlockChain blockChain;
    protected final long id;
    protected final String name;
    protected final KeyPair keyPair;

    public User(long id, String name) throws NoSuchAlgorithmException {
        this.id = id;
        blockChain = BlockChain.getInstance();
        this.name = name;
        keyPair = CryptoUtils.generateKeys(KEY_LENGTH);
        blockChain.registerUser(this);
    }

    public long getVC() {
        return blockChain.getUserVC(this);
    }
    
    public byte[] getEncodedPublicKey() {
        return keyPair.getPublic().getEncoded();
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void spendVC(long amount, long recipientId) {
        long transactionId = blockChain.getNextTransactionId();
        Transaction transaction = new Transaction(transactionId, id, recipientId, amount);
        transaction.sign(keyPair.getPrivate());
        blockChain.addTransaction(transaction);
    }
}
