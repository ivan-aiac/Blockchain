package blockchain;

import blockchain.utils.CryptoUtils;
import blockchain.utils.StringUtil;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

public class Block implements Serializable {

    private static final long serialVersionUID = 2392606367124566105L;
    private final long id;
    private final String creatorName;
    private final long creatorId;
    private byte[] creatorSign;
    private final long timeStamp;
    private final String previousBlockHash;
    private final long magicNumber;
    private final String hash;
    private List<Transaction> transactions;
    private final long generationTimeSeconds;

    public Block(long id, String previousBlockHash, int n, String creatorName, long creatorId) {
        this.id = id;
        this.timeStamp = Instant.now().toEpochMilli();
        this.previousBlockHash = previousBlockHash;
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String hash;
        long magicNumber;
        Pattern zeroPattern = Pattern.compile(String.format(BlockChain.ZERO_REGEX, n));
        Instant start = Instant.now();
        do {
            magicNumber = rng.nextLong();
            hash = StringUtil.applySha256(Arrays.toString(new Object[]{id, timeStamp, previousBlockHash, magicNumber}));
        } while (!zeroPattern.matcher(hash).matches());
        generationTimeSeconds = Duration.between(start, Instant.now()).toSeconds();
        this.hash = hash;
        this.magicNumber = magicNumber;
        this.creatorName = creatorName;
        this.creatorId = creatorId;
    }

    public void sign(PrivateKey privateKey) {
        try {
            creatorSign = CryptoUtils.signData(privateKey, creatorName.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            creatorSign = null;
        }
    }

    public String getCreatorName() {
        return creatorName;
    }

    public long getCreatorId() {
        return creatorId;
    }

    public byte[] getCreatorSign() {
        return creatorSign;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public long getMagicNumber() {
        return magicNumber;
    }

    public long getId() {
        return id;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public String getPreviousBlockHash() {
        return previousBlockHash;
    }

    public String getHash() {
        return hash;
    }

    public long getGenerationTimeSeconds() {
        return generationTimeSeconds;
    }

}
