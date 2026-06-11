/**
 * Implementarea structurii de date BTree pentru arhiva clinica
 */
public class BTree {
    private BTreeNode root;
    private int t; //gradul minim (t>=2)

    public BTree (int t)
    {
        BTreeNode x = new BTreeNode(t, true);
        x.n = 0;
        this.root = x;
        this.t = t;
    }

    /**
     * Metoda pentru compararea fiselor medicale, dupa nume apoi dupa id
     * @param r1
     * @param r2
     * @return
     */
    private int compareRecords(MedicalRecord r1, MedicalRecord r2)
    {
        int nameCompare = r1.name.toLowerCase().compareTo(r2.name.toLowerCase());
        if(nameCompare != 0){
            return nameCompare;
        }
        return Integer.compare(r1.id, r2.id);
    }

    /**
     * Modul de cautare exacta dupa nume
     * @param name
     * @return fisa medicala a pacientului
     */
    public MedicalRecord search(String name)
    {
        return bTreeSearch(root, name.toLowerCase());
    }

    /**
     * Cautarea recursiva in BTree pornind dintr un nod x
     * @param x
     * @param k
     * @return
     */
    private MedicalRecord bTreeSearch(BTreeNode x, String k)
    {
        int i = 0;
        //parcurgem cheile locale ale nodului atat timo cat sunt mai mici alfabetic
        while(i < x.n && k.compareTo(x.keys[i].name.toLowerCase()) > 0)
        {
            i = i + 1;
        }

        //am gasit cheia in nodul curent
        if(i < x.n && k.equals(x.keys[i].name.toLowerCase()))
        {
            return x.keys[i];
        }

        //ajungem la frunza si cheia nu a fost gasita
        else if(x.leaf){
            return null;
        }
        //pasul recursiv
        else return bTreeSearch(x.children[i], k);
    }

    /**
     * Insertie preventiva
     * @param k
     */
    public void insert(MedicalRecord k)
    {
        BTreeNode r = root;
        //daca radacina este plina crestem arborele in inaltime
        if(r.n == 2 * t - 1)
        {
            BTreeNode s = new BTreeNode(t, false);
            root = s;
            s.n = 0;
            s.children[0] = r; //vechea radacina devine primul fiu
            bTreeSplitChild(s, 0, r); //facem split la vechea radacina
            bTreeInsertNonfull(s, k); //inseram in nodul reechilibrat
        }
        else{
            bTreeInsertNonfull(r, k);
        }
    }

    /**
     * Inserare recursiva intr-un nod care este garantat ca nu e plin
     * @param x
     * @param k
     */
    private void bTreeInsertNonfull(BTreeNode x, MedicalRecord k)
    {
        int i = x.n -1;
        if(x.leaf){
            //daca nodul e frunza deplasam cheile la dreapta pentru a face loc noii inserari
            while(i >= 0 && compareRecords(k, x.keys[i]) <  0){
                x.keys[i+1] = x.keys[i];
                i = i - 1;
            }
            x.keys[i+1] = k;
            x.n = x.n + 1;
        }
        else{
            //daca e nod intern cautam fiul in care trebuie sa coboram
            while(i >= 0 && compareRecords(k, x.keys[i]) < 0){
                i = i - 1;
            }
            i = i + 1;

            //asigurare, facem split ianinte de a cobori daca fiul este plin
            if(x.children[i].n == 2 * t - 1){
                bTreeSplitChild(x ,i, x.children[i]);
                if(compareRecords(k, x.keys[i]) > 0)
                {
                    i= i + 1;
                }
            }
            bTreeInsertNonfull(x.children[i], k);
        }
    }

    /**
     * Splitul fiului plin y al parintelui x
     * @param x
     * @param i
     * @param y
     */
    private void bTreeSplitChild(BTreeNode x, int i, BTreeNode y)
    {
        BTreeNode z = new BTreeNode(t, y.leaf); //noul nod frate care preia jumatatea dreapta a lui y
        z.n = t - 1;

        //copiem ultimele t-1 chei in z
        for(int j = 0; j < t - 1; j++)
        {
            z.keys[j] = y.keys[j + t];
            y.keys[j + t] = null;
        }

        //daca y nu e frunza mutam si pointerii catre fii
        if(!y.leaf){
            for(int j = 0; j < t; j++)
            {
                z.children[j] = y.children[j + t];
                y.children[j + t] = null;
            }
        }

        y.n = t - 1; //ajustam dimensiunea

        //deplasam fii la dreapta pentru a face loc noului fiu z
        for(int j = x.n; j >= i+1; j--)
        {
            x.children[j + 1] = x.children[j];
        }
        x.children[i + 1] = z;

        //deplasam cheile lui x la dreapta pentru a face loc cheii care urca din y
        for(int j = x.n - 1; j >= i; j--)
        {
            x.keys[j+1] = x.keys[j];
        }
        x.keys[i] = y.keys[t-1]; //cheia din y urca in parintele x
        y.keys[t-1] = null;
        x.n = x.n + 1;
    }

    //stergere
    public void delete(String name) {
        if (name == null || name.trim().isEmpty()) return;
        bTreeDelete(this.root, name.toLowerCase());

        if (this.root.n == 0 && !this.root.leaf) {
            this.root = this.root.children[0];
        }
    }

    /**
     * Stergere recursiva din nodul x
     * @param x
     * @param k
     */
    private void bTreeDelete(BTreeNode x, String k) {
        int idx = 0;

        // Găsim indexul comparând exclusiv alfabetic cheile (la fel ca la search)
        while (idx < x.n && k.compareTo(x.keys[idx].name.toLowerCase()) > 0) {
            idx++;
        }

        // Cazul 1 și 2: Cheia k corespunde numelui din nodul curent
        if (idx < x.n && k.equals(x.keys[idx].name.toLowerCase())) {
            if (x.leaf) {
                // Cazul 1: k este într-o frunză -> ștergere directă prin translatare la stânga
                for (int i = idx + 1; i < x.n; i++) {
                    x.keys[i - 1] = x.keys[i];
                }
                x.keys[x.n - 1] = null;
                x.n--;
            } else {
                // Cazul 2: k este într-un nod intern
                BTreeNode y = x.children[idx]; // fiul precedent
                BTreeNode z = x.children[idx + 1]; // fiul succesor

                if (y.n >= t) {
                    // Cazul 2a: Fiul precedent are destule chei (>= t)
                    MedicalRecord pred = getInorderPredecessor(y);
                    x.keys[idx] = pred;
                    bTreeDelete(y, pred.name.toLowerCase());
                } else if (z.n >= t) {
                    // Cazul 2b: Fiul succesor are destule chei (>= t)
                    MedicalRecord succ = getInorderSuccessor(z);
                    x.keys[idx] = succ;
                    bTreeDelete(z, succ.name.toLowerCase());
                } else {
                    // Cazul 2c: Ambii fii au doar t-1 chei -> fuzionăm k și z în y
                    mergeNodes(x, idx, y, z);
                    bTreeDelete(y, k);
                }
            }
        } else {
            // Cazul 3: Cheia k nu este în nodul curent x
            if (x.leaf) {
                return; // Cheia nu există în arbore
            }

            boolean isLastChild = (idx == x.n);

            // Asigurarea preventivă top-down: nodul în care coborâm trebuie să aibă >= t chei
            if (x.children[idx].n < t) {
                fillChildNode(x, idx);
            }

            //ajustam indexul in cazul in care ultimul fiu a fost fuzionat
            if (isLastChild && idx > x.n) {
                bTreeDelete(x.children[idx - 1], k);
            } else {
                bTreeDelete(x.children[idx], k);
            }
        }
    }

    private MedicalRecord getInorderPredecessor(BTreeNode node) {
        BTreeNode i = node;
        while (!i.leaf) {
            i = i.children[i.n];
        }
        return i.keys[i.n - 1];
    }

    private MedicalRecord getInorderSuccessor(BTreeNode node) {
        BTreeNode i = node;
        while (!i.leaf) {
            i = i.children[0];
        }
        return i.keys[0];
    }

    /**
     * Asigura ca un nod fiu are cel putin t chei inainte de a cobori in el
     * @param parent
     * @param idx
     */
    private void fillChildNode(BTreeNode parent, int idx) {
        // Împrumutăm de la fratele stâng dacă acesta are minim t chei
        if (idx != 0 && parent.children[idx - 1].n >= t) {
            borrowFromLeft(parent, idx);
            // Împrumutăm de la fratele drept dacă acesta are minim t chei
        } else if (idx != parent.n && parent.children[idx + 1].n >= t) {
            borrowFromRight(parent, idx);

        }
        //Daca niciun frate nu are sa i dea chei facem fuziune
        else {
            if (idx != parent.n) {
                mergeNodes(parent, idx, parent.children[idx], parent.children[idx + 1]);
            } else {
                mergeNodes(parent, idx - 1, parent.children[idx - 1], parent.children[idx]);
            }
        }
    }

    /**
     * Transfera o cheie de la fratele stang la fiul curent coborand o cheie din parinte
     * @param parent
     * @param idx
     */
    private void borrowFromLeft(BTreeNode parent, int idx) {
        BTreeNode child = parent.children[idx];
        BTreeNode sibling = parent.children[idx - 1];

        //mutam la dreapta pentru a face loc cheii din parinte
        for (int i = child.n - 1; i >= 0; i--) {
            child.keys[i + 1] = child.keys[i];
        }
        if (!child.leaf) {
            for (int i = child.n; i >= 0; i--) {
                child.children[i + 1] = child.children[i];
            }
        }

        child.keys[0] = parent.keys[idx - 1];

        if (!child.leaf) {
            child.children[0] = sibling.children[sibling.n];
            sibling.children[sibling.n] = null;
        }

        parent.keys[idx - 1] = sibling.keys[sibling.n - 1];
        sibling.keys[sibling.n - 1] = null;

        child.n++;
        sibling.n--;
    }

    /**
     * Transfera o cheie de la fratele drept la fiul curent coborand o cheie din parinte
     * @param parent
     * @param idx
     */
    private void borrowFromRight(BTreeNode parent, int idx) {
        BTreeNode child = parent.children[idx];
        BTreeNode sibling = parent.children[idx + 1];

        child.keys[child.n] = parent.keys[idx];

        if (!child.leaf) {
            child.children[child.n + 1] = sibling.children[0];
        }

        parent.keys[idx] = sibling.keys[0];

        //mutam la stanga elementele fratelui drept  din care s a imprumutat cheia
        for (int i = 1; i < sibling.n; i++) {
            sibling.keys[i - 1] = sibling.keys[i];
        }
        sibling.keys[sibling.n - 1] = null;

        if (!sibling.leaf) {
            for (int i = 1; i <= sibling.n; i++) {
                sibling.children[i - 1] = sibling.children[i];
            }
            sibling.children[sibling.n] = null;
        }
        child.n++;
        sibling.n--;
    }

    /**
     * Fuzioneaza nodul sibling in nodul child
     * @param parent
     * @param idx
     * @param child
     * @param sibling
     */
    private void mergeNodes(BTreeNode parent, int idx, BTreeNode child, BTreeNode sibling) {
        child.keys[t - 1] = parent.keys[idx];

        //copiem cheile din frate in continuarea fiului
        for (int i = 0; i < sibling.n; i++) {
            child.keys[i + t] = sibling.keys[i];
        }

        //copiem structura de pointeri de fii daca nodurile nu sunt frunze
        if (!child.leaf) {
            for (int i = 0; i <= sibling.n; i++) {
                child.children[i + t] = sibling.children[i];
            }
        }

        //mutam la stanga cheile din parinte pentru a acoperi golul lasat de cheia coborata
        for (int i = idx + 1; i < parent.n; i++) {
            parent.keys[i - 1] = parent.keys[i];
        }
        parent.keys[parent.n - 1] = null;

        //mutam pointerii de copii
        for (int i = idx + 2; i <= parent.n; i++) {
            parent.children[i - 1] = parent.children[i];
        }
        parent.children[parent.n] = null;

        child.n += sibling.n + 1;
        parent.n--;
    }

    /**
     * Parcurge recursiv tot B-Tree ul si aplica algoritmul KMP pe diagnostice si simptome
     * rezultatele sunt salvate sub forma de text json
     * @param node
     * @param query
     * @param jsonResult
     * @param count
     */
    public void searchWithKMP(BTreeNode node, String query, StringBuilder jsonResult, int[] count) {
        if (node == null) return;

        //parcurgem toate fisele medicale
        for (int i = 0; i < node.n; i++) {
            if (node.keys[i] != null) {
                boolean matchSymptoms = KMP.search(node.keys[i].symptoms, query);
                boolean matchDiagnosis = KMP.search(node.keys[i].diagnosis, query);

                //daca s-a gasit o potrivire a sablonului adaugam datele in json
                if (matchSymptoms || matchDiagnosis) {
                    if (count[0] > 0) {
                        jsonResult.append(",");
                    }

                    jsonResult.append("{")
                            .append("\"id\":").append(node.keys[i].id).append(",")
                            .append("\"nume\":\"").append(node.keys[i].name).append("\",")
                            .append("\"diagnostic\":\"").append(node.keys[i].diagnosis).append("\",")
                            .append("\"simptome\":\"").append(node.keys[i].symptoms).append("\",")
                            .append("\"prioritate\":\"").append(node.keys[i].priority.label).append("\"")
                            .append("}");

                    count[0]++;
                }
            }
        }

        // Dacă nodul nu este frunză, propagăm căutarea recursiv în toți fiii săi
        if (!node.leaf) {
            for (int i = 0; i <= node.n; i++) {
                searchWithKMP(node.children[i], query, jsonResult, count);
            }
        }
    }

    public BTreeNode getRoot()
    {
        return root;
    }
}