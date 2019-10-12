import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class MaxFeeTxHandler {
    
    private UTXOPool pool;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code pool}. This should make a copy of pool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public MaxFeeTxHandler(UTXOPool pool) {
        // IMPLEMENT THIS
        this.pool = new UTXOPool(pool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        // IMPLEMENT THIS
        // ArrayList<Transaction.Output> outs = tx.getOutputs();
        UTXOPool uniqueUtxos = new UTXOPool();
         // ArrayList<Transaction.Input> ins = tx.getInputs();
       // ArrayList<UTXO> valid = pool.getAllUTXO();

        double total = 0;
        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input in = tx.getInput(i);
        // for (Transaction.Input in : ins) {
            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
            if (!pool.contains(utxo)) {
                return false;
            }
             if (uniqueUtxos.contains(utxo)) return false;
           Transaction.Output fout = pool.getTxOutput(utxo);
            if (false==Crypto.verifySignature(fout.address, tx.getRawDataToSign(i), in.signature)) {
                return false;
            }
             uniqueUtxos.addUTXO(utxo, fout);
           total += fout.value;
        }

        double spend = 0;
        for (Transaction.Output ot : tx.getOutputs()) {
            if (ot.value < 0) return false;
            spend += ot.value;
        }

        if(total < spend )return false;

        
        return true;
    }

    private double calcTxFees(Transaction tx) {
        double sumInputs = 0;
        double sumOutputs = 0;
        for (Transaction.Input in : tx.getInputs()) {
            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
            // if (!pool.contains(utxo) || !isValidTx(tx)) continue;
            Transaction.Output txOutput = pool.getTxOutput(utxo);
            sumInputs += txOutput.value;
        }
        for (Transaction.Output out : tx.getOutputs()) {
            sumOutputs += out.value;
        }
        return sumInputs - sumOutputs;
    }

    

    public class subNode{
      public  UTXO utxo0 = null;
      public  ArrayList<pathNode>  childs = new ArrayList<pathNode>();
      pathNode container = null;
      boolean used = false;
    }
    
    public class pathNode {
        // // it's either a tx to process or utxo in original pool
        // Transaction transa = null;
        
        boolean isOriginal = false;
        double fee=0;
        boolean handled = false;

        Transaction matched = null;

        ArrayList<subNode> parents = new ArrayList<subNode>();//, childs = new ArrayList<pathNode>();
        ArrayList<subNode> brothers = new ArrayList<subNode>();//, childs = new ArrayList<pathNode>();
    }

void populate(ArrayList<Transaction> inArray, HashSet<pathNode> graph){
    boolean addnode = false;
    ArrayList<Transaction> leftover = new ArrayList<Transaction>();
    for (Transaction tx : inArray) {
        if (isValidTx(tx)) {
            addnode = true;
            double fe = calcTxFees(tx);
            
            pathNode node = new pathNode();
            node.fee = fe;
            node.matched=tx;

            for (int i = 0; i < tx.numOutputs(); i++) {
                Transaction.Output out = tx.getOutput(i);
                UTXO utxo = new UTXO(tx.getHash(), i);
                pool.addUTXO(utxo, out);

                subNode sub = new subNode();
                sub.utxo0 = utxo;
                sub.container = node;
                node.brothers.add(sub);
            }

            for (Transaction.Input in : tx.getInputs()) {
                UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
                for (pathNode fnode : graph) {
                    boolean found = false;
                    for (subNode sub : fnode.brothers) {
                        if (sub.utxo0.equals(utxo)) {
                            found=true;
                            sub.childs.add(node);
                            node.parents.add(sub);
                            break;
                        }
                    }
                    if (found) {
                        break;
                    }
                    
                }
            }
            graph.add(node);
        }
        else{
            leftover.add(tx);
        }
    }
    if (addnode && leftover.size()>0) {
        populate(leftover, graph);
    }
}

    void maxhandle(ArrayList<Transaction> inArray, ArrayList<Transaction> outArray){
        HashSet<pathNode> graph = new HashSet<pathNode>();
        HashMap<Transaction, pathNode> pairs = new HashMap<Transaction, pathNode>();
        {
            for (UTXO utx : pool.getAllUTXO()) {
                pathNode node = new pathNode();
                node.isOriginal=true;
                node.handled=true;

                subNode sub1 = new subNode();
                sub1.container = node;
                sub1.utxo0 = utx;
                node.brothers.add(sub1);

                graph.add(node);
            }

            UTXOPool backup = new UTXOPool(pool);
            populate(inArray, graph);
            for (pathNode node : graph) {
                if(node.matched != null)pairs.put(node.matched, node);
            }
            pool = new UTXOPool(backup);
        }

        handle_simples(pairs, outArray);
        handle_conflict(pairs, outArray);
    }

    void handleOne(Transaction tx, ArrayList<Transaction> outArray){
            outArray.add(tx);

            for (Transaction.Input in : tx.getInputs()) {
                UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
                pool.removeUTXO(utxo);
            }
            for (int i = 0; i < tx.numOutputs(); i++) {
                Transaction.Output out = tx.getOutput(i);
                UTXO utxo = new UTXO(tx.getHash(), i);
                pool.addUTXO(utxo, out);
            }
    }

    void handle_simples(HashMap<Transaction, pathNode> pairs, ArrayList<Transaction> outArray){
            Set<Transaction> possi = pairs.keySet();
            ArrayList<Transaction> ftemp = new ArrayList<Transaction>();

            for (Transaction tx : possi) {
                boolean isSimple = true;
                pathNode node = pairs.get(tx);

                for (subNode pa : node.parents) {
                    if (pa.container.handled==false || pa.childs.size()>1) {
                        isSimple=false;
                        break;
                    }
                }

                if (isSimple) {
                    node.handled=true;
                    ftemp.add(tx);
                    handleOne(tx, outArray);
                }
            }

            for (Transaction tx : ftemp) {
                pairs.remove(tx);
            }
            if (ftemp.size()>0) {
                handle_simples(pairs, outArray);
            }
    }

    void handle_conflict(HashMap<Transaction, pathNode> pairs, ArrayList<Transaction> outArray){
            doconf(pairs);
            for (pathNode nn : maxFeePath) {
                nn.handled=true;
                handleOne(nn.matched,outArray);
            }
    }

    ArrayList<pathNode> tryingPath = new ArrayList<pathNode>();
    // HashMap<ArrayList<pathNode>, Number> pathCosts = new HashMap<ArrayList<pathNode>, Number>();
    double maxFee = -1;
        ArrayList<pathNode> maxFeePath = new ArrayList<pathNode>();

    void doconf(HashMap<Transaction, pathNode> pairs){
            Set<Transaction> possi = pairs.keySet();
        ArrayList<pathNode> ftemp = new ArrayList<pathNode>();

            for (Transaction tx : possi) {
                boolean ready = true;
                pathNode node = pairs.get(tx);

                for (subNode pa : node.parents) {
                    if (pa.container.handled==false || pa.used) {
                        ready=false;
                        break;
                    }
                }

                if (ready) {
                    ftemp.add(node);
                }
            }

            if (ftemp.size()==0) {
                double fee = 0;
                for (pathNode nn : tryingPath) {
                    fee += nn.fee;
                }
                if (fee > maxFee) {
                    maxFee = fee;
                    maxFeePath = new ArrayList<pathNode>(tryingPath);
                }
                return;
            }

            for (pathNode node : ftemp) {
                node.handled=true;
                for (subNode pa : node.parents) {
                    pa.used=true;
                }
                tryingPath.add(node);

                doconf(pairs);

                node.handled=false;
                for (subNode pa : node.parents) {
                    pa.used=false;
                }
                tryingPath.remove(tryingPath.size()-1);
            }
    }

    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        ArrayList<Transaction> ret = new ArrayList<Transaction>();
        ArrayList<Transaction> txs = new ArrayList<Transaction>(Arrays.asList(possibleTxs));

        maxhandle(txs, ret);

        return ret.toArray(new Transaction[ret.size()]);
        
    }

}
