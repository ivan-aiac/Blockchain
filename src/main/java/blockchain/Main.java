package blockchain;

import blockchain.user.Miner;
import blockchain.user.User;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final int MINER_THREADS = 10;
    private static final int NUMBER_OF_BLOCKS = 15;
    private static final List<String> USERS =
            List.of("Nick", "Bob", "Alice", "Anna", "John", "Erick", "FastFood", "PcParts", "CarShop", "ClothesStore");
    private static long id = 1;
    private static final BlockChain blockChain = BlockChain.getInstance();

    public static void main(String[] args) {
        ExecutorService minerExecutor = Executors.newFixedThreadPool(MINER_THREADS);
        ExecutorService spenderExecutor = Executors.newFixedThreadPool(MINER_THREADS + USERS.size());
        List<Miner> miners = createMiners();
        List<User> users = createUsers();
        for (Miner miner : miners) {
            minerExecutor.submit(() -> {
                while (blockChain.getNextBlockId() <= NUMBER_OF_BLOCKS) {
                    try {
                        miner.mineBlock();
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            });
        }
        users.addAll(miners);
        for (User user : users) {
            spenderExecutor.submit(() -> {
                ThreadLocalRandom rng = ThreadLocalRandom.current();
                while (blockChain.getNextBlockId() <= NUMBER_OF_BLOCKS) {
                    try {
                        if (rng.nextFloat() <= 0.05) {
                            user.spendVC(rng.nextLong(1, 101), blockChain.getRandomUserId(user.getId()));
                            Thread.sleep(100);
                        }
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            });
        }
        minerExecutor.shutdown();
        spenderExecutor.shutdown();
        try {
            minerExecutor.awaitTermination(1, TimeUnit.SECONDS);
            spenderExecutor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    private static List<Miner> createMiners() {
        List<Miner> miners = new ArrayList<>();
        for (int i = 0; i < MINER_THREADS; i++, id++) {
            try {
                Miner m = new Miner(i);
                blockChain.registerUser(m);
                miners.add(m);
            } catch (NoSuchAlgorithmException e) {
                System.out.println(e.getMessage());
            }
        }
        return miners;
    }

    private static List<User> createUsers() {
        List<User> users = new ArrayList<>();
        for (String user: USERS) {
            try {
                User u = new User(id++, user);
                blockChain.registerUser(u);
                users.add(u);
            } catch (NoSuchAlgorithmException e) {
                System.out.println(e.getMessage());
            }
        }
        return users;
    }
}
    