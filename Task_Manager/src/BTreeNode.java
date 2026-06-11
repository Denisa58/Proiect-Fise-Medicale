/**
 * Clasa reprezintă un nod individual din structura arborelui B-Tree
 */
public class BTreeNode {
    public MedicalRecord[] keys; //tablou care stochează fișele medicale
    public BTreeNode[] children; //tablou de pointeri ctre nodurile fii
    public int n;
    public boolean leaf;

    public BTreeNode(int t, boolean leaf)
    {
        this.leaf = leaf;
        this.keys = new MedicalRecord[2 * t - 1];
        this.children = new BTreeNode[2 * t];
        this.n = 0;
    }
}
