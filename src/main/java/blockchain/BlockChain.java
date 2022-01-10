package blockchain;

import blockchain.user.Miner;
import blockchain.user.User;
import blockchain.utils.CryptoUtils;
import blockchain.utils.SerializationUtils;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

public class BlockChain implements Serializable {

    private static final BlockChain instance = new BlockChain();
    private static final String FILE_NAME = "block.chain";
    private static final long INITIAL_ID = 1;
    private static final int INITIAL_N = 0;
    private static final String INITIAL_HASH = "0";
    public static final String ZERO_REGEX = "0{%d}.+";
    private static final long serialVersionUID = -5981475679654647069L;
    private long nextBlockId;
    private String previousBlockHash;
    private final List<Block> blocks;
    private final List<Transaction> transactions;
    private int numberOfZeros;
    private Pattern zeroPattern;
    private final ReentrantReadWriteLock lock;
    private final Lock readLock;
    private final Lock writeLock;
    private final Lock transactionsLock;
    private final Condition addingBlockCondition;
    private long maxTransactionId;
    private long currentTransactionId;
    private final Map<Long, User> users;
    private long lastUserId;

    private BlockChain() {
        BlockChain blockChain = null;
        users = new Hashtable<>();
        blocks = new ArrayList<>();
        transactions = new ArrayList<>();
        /*File file = new File(FILE_NAME);
        if (file.isFile()) {
            try {
                blockChain = loadFromFile();
            } catch (Exception e) {
                users.clear();
                System.out.println(e.getMessage());
            }
        }*/
        if (blockChain != null) {
            nextBlockId = blockChain.nextBlockId;
            previousBlockHash = blockChain.previousBlockHash;
            blocks.addAll(blockChain.blocks);
            transactions.addAll(blockChain.transactions);
            maxTransactionId = blockChain.maxTransactionId;
            currentTransactionId = blockChain.currentTransactionId;
            lastUserId = blockChain.lastUserId;
            numberOfZeros = blockChain.numberOfZeros;
        } else {
            previousBlockHash = INITIAL_HASH;
            nextBlockId = INITIAL_ID;
            numberOfZeros = INITIAL_N;
        }
        updateZeroPattern();
        lock = new ReentrantReadWriteLock();
        readLock = lock.readLock();
        writeLock = lock.writeLock();
        transactionsLock = new ReentrantLock();
        addingBlockCondition = transactionsLock.newCondition();
    }

    private BlockChain loadFromFile() throws Exception {
        BlockChain blockChain = (BlockChain) SerializationUtils.deserialize(FILE_NAME);
        users.putAll(blockChain.users);
        for (Block block : blockChain.blocks) {
            if (!isBlockValid(block)) {
                throw new RuntimeException("Corrupted File: Contains invalid blocks.");
            }
        }
        for (Transaction transaction : blockChain.transactions) {
            if (!isTransactionValid(transaction)) {
                throw new RuntimeException("Corrupted File: Contains invalid transactions.");
            }
        }
        return blockChain;
    }

    public void registerUser(User user) {
        users.put(user.getId(), user);
        lastUserId = user.getId();
    }

    public long getRandomUserId(long id) {
        readLock.lock();
        try {
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            User randomUser;
            do {
                randomUser = users.get(rng.nextLong(lastUserId));
            } while (randomUser == null || id == randomUser.getId());
            return randomUser.getId();
        } finally {
            readLock.unlock();
        }
    }

    public long getUserVC(User user) {
        readLock.lock();
        try {
            long id = user.getId();

            long minedBlocksVC = user instanceof Miner ? 100L * blocks.stream()
                    .filter(b -> b.getCreatorId() == id)
                    .count() : 0L;
            return minedBlocksVC + blocks.stream()
                    .flatMap(b -> b.getTransactions().stream())
                    .filter(t -> id == t.getSenderId() || id == t.getRecipientId())
                    .map(t -> t.getRecipientId() == id ? t.getAmount() : -t.getAmount())
                    .reduce(100L, Long::sum);
        } finally {
            readLock.unlock();
        }
    }

    public long getNextTransactionId() {
        transactionsLock.lock();
        try {
            while (lock.isWriteLocked()) {
                addingBlockCondition.await();
            }
            return currentTransactionId++;
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
            return -1;
        } finally {
            transactionsLock.unlock();
        }
    }

    public void addTransaction(Transaction transaction) {
        readLock.lock();
        transactionsLock.lock();
        try {
            if (isTransactionValid(transaction)) {
                while (lock.isWriteLocked()) {
                    addingBlockCondition.await();
                }
                transactions.add(transaction);
            }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        } finally {
            transactionsLock.unlock();
            readLock.unlock();
        }
    }

    private boolean isTransactionValid(Transaction transaction) {
        long id = transaction.getId();
        // Id Check
        if (transactions.stream().noneMatch(t -> t.getId() < maxTransactionId || id == t.getId())) {
            User user = users.get(transaction.getSenderId());
            if (user != null) {
                // Signature Check
                try {
                    if(CryptoUtils.verifySignature(user.getEncodedPublicKey(), transaction.getSignature(), transaction.getTransactionBytes())) {
                        return getUserVC(user) >= transaction.getAmount();
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }
        return false;
    }

    public void addBlock(Block block) {
        writeLock.lock();
        transactionsLock.lock();
        try {
            if (isBlockValid(block)) {
                if (transactions.isEmpty()) {
                    block.setTransactions(List.of());
                } else {
                    maxTransactionId = currentTransactionId;
                    block.setTransactions(List.copyOf(transactions));
                    transactions.clear();
                }
                nextBlockId++;
                previousBlockHash = block.getHash();
                int adjustment = adjustNumberOfZeros(block.getGenerationTimeSeconds());
                if (adjustment != 0) {
                    updateZeroPattern();
                }
                numberOfZeros += adjustment;
                blocks.add(block);
                try {
                    SerializationUtils.serialize(this, FILE_NAME);
                } catch (IOException e) {
                    System.out.println("Serialize :" + e.getMessage());
                }
                printBlock(block, adjustment);
            }
        } finally {
            addingBlockCondition.signal();
            transactionsLock.unlock();
            writeLock.unlock();
        }
    }

    public long getNextBlockId() {
        readLock.lock();
        try {
            return nextBlockId;
        } finally {
            readLock.unlock();
        }
    }

    public int getNumberOfZeros() {
        readLock.lock();
        try {
            return numberOfZeros;
        } finally {
            readLock.unlock();
        }
    }

    public String getPreviousBlockHash() {
        readLock.lock();
        try {
            return previousBlockHash;
        } finally {
            readLock.unlock();
        }
    }

    public static BlockChain getInstance() {
        return instance;
    }

    private boolean isBlockValid(Block block) {
        User user = users.get(block.getCreatorId());
        boolean isSignatureValid = false;
        if (user != null) {
            try {
                isSignatureValid = CryptoUtils.verifySignature(user.getEncodedPublicKey(), block.getCreatorSign(), block.getCreatorName().getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        return Objects.equals(previousBlockHash, block.getPreviousBlockHash())
                && zeroPattern.matcher(block.getHash()).matches()
                && isSignatureValid;
    }

    private int adjustNumberOfZeros(long blockGenTime) {
        if (blockGenTime < 1 && numberOfZeros < 4) {
            return 1;
        } else if (blockGenTime > 1 || numberOfZeros >= 4){
            return -1;
        } else {
            return 0;
        }
    }

    private void updateZeroPattern() {
        zeroPattern = Pattern.compile(String.format(ZERO_REGEX, numberOfZeros));
    }

    public void printBlock(Block block, int adjustment) {
        StringBuilder sb = new StringBuilder();
        String ls = System.lineSeparator();
        sb.append("Block:").append(ls);
        sb.append("Created by ").append(block.getCreatorName()).append(ls);
        sb.append(block.getCreatorName()).append(" gets 100 VC").append(ls);
        sb.append("Id: ").append(block.getId()).append(ls);
        sb.append("Timestamp: ").append(block.getTimeStamp()).append(ls);
        sb.append("Magic number: ").append(block.getMagicNumber()).append(ls);
        sb.append("Hash of the previous block:").append(ls).append(block.getPreviousBlockHash()).append(ls);
        sb.append("Hash of the block:").append(ls).append(block.getHash()).append(ls);
        sb.append("Block data: ");
        if (block.getTransactions().isEmpty()) {
            sb.append("No Transactions").append(ls);
        } else {
            sb.append(ls);
            block.getTransactions().forEach(t -> {
                String from = users.get(t.getSenderId()).getName();
                String to = users.get(t.getRecipientId()).getName();
                sb.append(String.format("%s sent %d VC to %s", from, t.getAmount(), to)).append(ls);
            });
        }
        sb.append("Block was generating for ").append(block.getGenerationTimeSeconds()).append(" seconds").append(ls);
        switch (adjustment) {
            case 0:
                sb.append("N stays the same").append(ls);
                break;
            case 1:
                sb.append("N was increased to ").append(numberOfZeros).append(ls);
                break;
            case -1:
                sb.append("N was decreased by 1").append(ls);
        }
        System.out.println(sb);
    }

}
