import java.util.ArrayList;
import java.util.HashMap;

/* Block Chain should maintain only limited block nodes to satisfy the functions
   You should not have the all the blocks added to the block chain in memory 
   as it would overflow memory
 */
public class BlockChain {
    public static final int CUT_OFF_AGE = 10;

    private TransactionPool txPool;
    private BlockNode maxHeightNode;
    private HashMap<ByteArrayWrapper, BlockNode> blockToNode;

    // all information required in handling a block in block chain
    private class BlockNode {
        public Block b;
        public BlockNode parent;
        public ArrayList<BlockNode> children;
        public int height;
        // utxo pool for making a new block on top of this block
        private UTXOPool uPool;

        public BlockNode(Block b, BlockNode parent, UTXOPool uPool) {
            this.b = b;
            this.parent = parent;
            children = new ArrayList<BlockNode>();
            this.uPool = uPool;
            if (parent != null) {
                height = parent.height + 1;
                parent.children.add(this);
            } else {
                height = 1;
            }
        }

        public UTXOPool getUTXOPoolCopy() {
            return new UTXOPool(uPool);
        }
    }

    /* create an empty block chain with just a genesis block.
     * Assume genesis block is a valid block
     */
    public BlockChain(Block genesisBlock) {
        UTXOPool utxoPool = new UTXOPool();
        Transaction coinbase = genesisBlock.getCoinbase();
        utxoPool.addUTXO(new UTXO(coinbase.getHash(), 0), coinbase.getOutput(0));

        maxHeightNode = new BlockNode(genesisBlock, null, utxoPool);
        txPool = new TransactionPool();
        blockToNode = new HashMap<ByteArrayWrapper, BlockNode>();

        blockToNode.put(new ByteArrayWrapper(genesisBlock.getHash()), maxHeightNode);
    }

    /* Get the maximum height block
     */
    public Block getMaxHeightBlock() {
        return maxHeightNode.b;
    }

    /* Get the UTXOPool for mining a new block on top of
     * max height block
     */
    public UTXOPool getMaxHeightUTXOPool() {
        return maxHeightNode.getUTXOPoolCopy();
    }

    /* Get the transaction pool to mine a new block
     */
    public TransactionPool getTransactionPool() {
        return new TransactionPool(txPool);
    }

    /* Add a block to block chain if it is valid.
     * For validity, all transactions should be valid
     * and block should be at height > (maxHeight - CUT_OFF_AGE).
     * For example, you can try creating a new block over genesis block
     * (block height 2) if blockChain height is <= CUT_OFF_AGE + 1.
     * As soon as height > CUT_OFF_AGE + 1, you cannot create a new block at height 2.
     * Return true of block is successfully added
     */
    public boolean addBlock(Block b) {
        // Genesis block?
        if (b.getPrevBlockHash() == null)
            return false;

        // Check height
        BlockNode prevNode = blockToNode.get(new ByteArrayWrapper(b.getPrevBlockHash()));
        if (prevNode == null || prevNode.height + 1 <= maxHeightNode.height - CUT_OFF_AGE)
            return false;

        // Process transactions
        UTXOPool utxoPool = prevNode.getUTXOPoolCopy();
        TxHandler txHandler = new TxHandler(utxoPool);
        for (Transaction tx : b.getTransactions()) {
            // Check transaction validity
            if (!txHandler.isValidTx(tx))
                return false;

            // Remove all UTXO corresponding to tx inputs from utxoPool.
            for (int i = 0; i < tx.numInputs(); i++) {
                UTXO utxo = new UTXO(tx.getInput(i).prevTxHash, tx.getInput(i).outputIndex);
                utxoPool.removeUTXO(utxo);
            }

            // Add all tx outputs to utxoPool.
            for (int i = 0; i < tx.numOutputs(); i++) {
                UTXO utxo = new UTXO(tx.getHash(), i);
                utxoPool.addUTXO(utxo, tx.getOutput(i));
            }

            // Update txPool
            txPool.removeTransaction(tx.getHash());
        }

        BlockNode newNode = new BlockNode(b, prevNode, utxoPool);
        // Update maxHeightNode
        if (newNode.height > maxHeightNode.height)
            maxHeightNode = newNode;

        // Update utxoPool with b's coinbase.
        utxoPool.addUTXO(new UTXO(b.getCoinbase().getHash(), 0), b.getCoinbase().getOutput(0));

        // Update map
        blockToNode.put(new ByteArrayWrapper(b.getHash()), newNode);
        return true;
    }

    /* Add a transaction in transaction pool
     */
    public void addTransaction(Transaction tx) {
        txPool.addTransaction(tx);
    }
}