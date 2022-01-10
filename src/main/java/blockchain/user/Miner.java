package blockchain.user;

import blockchain.Block;

import java.security.NoSuchAlgorithmException;

public class Miner extends User {

    public Miner(int id) throws NoSuchAlgorithmException{
        super(id, String.format("%s%d", "miner", id));
    }

    public void mineBlock() {
        Block block = new Block(blockChain.getNextBlockId(), blockChain.getPreviousBlockHash(), blockChain.getNumberOfZeros(), name, id);
        block.sign(keyPair.getPrivate());
        blockChain.addBlock(block);
    }

}
