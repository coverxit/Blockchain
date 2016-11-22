import java.util.HashSet;

public class TxHandler {

    private UTXOPool utxoPool = null;
    /* Creates a public ledger whose current UTXOPool (collection of unspent
     * transaction outputs) is utxoPool. This should make a defensive copy of
     * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

	/* Returns true if 
     * (1) all outputs claimed by tx are in the current UTXO pool,
	 * (2) the signatures on each input of tx are valid, 
	 * (3) no UTXO is claimed multiple times by tx, 
	 * (4) all of tx’s output values are non-negative, and
	 * (5) the sum of tx’s input values is greater than or equal to the sum of   
	        its output values;
	   and false otherwise.
	 */

    public boolean isValidTx(Transaction tx) {
        // We only deal with incoinbase transactions.
        if (!tx.isCoinbase()) {
            HashSet<UTXO> claimedUTXOs = new HashSet<UTXO>();
            double inputSum = 0, outputSum = 0;

            for (int i = 0; i < tx.numInputs(); i++) {
                Transaction.Input in = tx.getInput(i);
                UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);

                // (1) and (3)
                if (!utxoPool.contains(utxo) || claimedUTXOs.contains(utxo))
                    return false;
                else
                {
                    // (3)
                    claimedUTXOs.add(utxo);

                    // (2)
                    Transaction.Output prevOutput = utxoPool.getTxOutput(utxo);
                    if (!prevOutput.address.verifySignature(tx.getRawDataToSign(i), in.signature))
                        return false;

                    // (5)
                    inputSum += prevOutput.value;
                }
            }

            // (4)
            for (Transaction.Output out : tx.getOutputs()) {
                if (out.value < 0)
                    return false;

                outputSum += out.value;
            }

            // (5)
            if (inputSum < outputSum)
                return false;
        }

        return true;
    }

    /* Handles each epoch by receiving an unordered array of proposed
     * transactions, checking each transaction for correctness,
     * returning a mutually valid array of accepted transactions,
     * and updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        TransactionPool txPool = new TransactionPool();

        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx)) {
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

                txPool.addTransaction(tx);
            }
        }

        return txPool.getTransactions().toArray(new Transaction[txPool.getTransactions().size()]);
    }

} 