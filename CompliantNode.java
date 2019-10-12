import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Set;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {

   int nofr;

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        nofr=numRounds;
    }

    public void setFollowees(boolean[] followees) {
        
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        if (stx==null) {
            stx = new HashSet<Transaction>();
        }
        for (Transaction x : pendingTransactions) {
            stx.add(x);
        }
    }

    static Set<Transaction>  stx = null;
     Set<Transaction> empt = new HashSet<Transaction>();
    int round=0;
    public Set<Transaction> sendToFollowers() {
        round++;
        if (round<nofr) {
            return empt;
        }
        return stx;
   }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        
    }
}
