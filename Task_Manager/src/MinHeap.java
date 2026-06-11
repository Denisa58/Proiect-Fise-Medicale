/**
 * Implementarea structurii de date Min-Heap
 * Foloseste un vector pentru a reprezenta un arbore binar
 */
public class MinHeap {
    private MedicalRecord[] heap; // Vectorul in care stocam referintele fiselor pacientilor
    private int size;// Numarul curent de pacienti aflati in asteptare
    private int capacity; // Capacitatea totala maxima alocata initial pentru vector

    /**
     * Constructor pentru initializarea cozii cu o anumita capacitate
     * @param capacity
     */
    public MinHeap(int capacity) {
        this.capacity = capacity;
        this.heap = new MedicalRecord[capacity];
        this.size = 0;
    }

    /**
     * Adauga un pacient nou in coada
     * @param record
     */
    public void insert(MedicalRecord record) {
        //daca vectorul devine plin ii dublam dimensiunea
        if(size >= capacity) {
            resize();
        }
        heap[size] = record;
        //facem heapify up pana cand pacientul isi gaseste locul in lista
        siftUp(size);
        size++;
    }

    /**
     * Metoda utilizata pentru redimensionarea memoriei alocate vectorului
     */
    private void resize(){
        this.capacity = this.capacity * 2;
        MedicalRecord[] newHeap = new MedicalRecord[capacity];
        System.arraycopy(heap,0,newHeap,0,size);
        this.heap = newHeap;
    }

    /**
     * Reordonare(Heapify Up)
     * Compara nodul curent cu parintele sau si le interschimba daca ordinea e gresita
     * @param index
     */
    private void siftUp(int index) {
        while(index > 0){
            int parentIndex = (index - 1) / 2;
            if(heap[index].priority.value < heap[parentIndex].priority.value) {
                swap(index, parentIndex);
                index = parentIndex;
            } else {
                break;
            }
        }
    }

    /**
     * Extrage si elimina pacientul cu cea mai mare urgenta medicala(aflat laindexul 0)
     * @return
     */
    public MedicalRecord extractMin(){
        if(size == 0) return null;

        MedicalRecord root = heap[0];
        heap[0] = heap[size-1];
        size--;
        siftDown(0);
        return root;
    }

    /**
     * Reordonare(Heapify Down)
     * oboara un element pe drumul fiilor mai urgenti pentru a restabili proprietatea de min-heap
     * @param index
     */
    private void siftDown(int index) {
        int minIndex = index;
        int left = 2 * index + 1; //fiul stang
        int right = 2 * index + 2; //fiul drept

        if(left < size && heap[left].priority.value < heap[minIndex].priority.value) {
            minIndex = left;
        }
        if(right < size && heap[right].priority.value < heap[minIndex].priority.value) {
            minIndex = right;
        }
        // Daca nodul parinte nu mai este cel mai urgent, realizam interschimbarea cu cel mai mic dintre fii
        if(index != minIndex) {
            swap(index, minIndex);
            siftDown(minIndex);
        }
    }

    /**
     * Metoda pentru interschimbarea a doua elemete
     * @param i
     * @param j
     */
    private void swap(int i, int j) {
        MedicalRecord t = heap[i];
        heap[i] = heap[j];
        heap[j] = t;
    }

    public boolean isEmpty(){
        return size == 0;
    }

    /**
     * Cauta un pacient dupa ID si il elimina
     * @param id
     * @return
     */
    public boolean stergeDupaId(int id) {
        int indexGasit = -1;

        //cautare liniara pentru a identifica unde se afla ID-ul in vector
        for (int i = 0; i < size; i++) {
            if (heap[i].id == id) {
                indexGasit = i;
                break;
            }
        }

        if (indexGasit == -1) return false;
        //inlocuim elementul sters cu ultimul element din vector
        heap[indexGasit] = heap[size - 1];
        size--;

        //verificam proprietatile
        if (indexGasit < size) {
            siftDown(indexGasit);
            siftUp(indexGasit);
        }

        return true;
    }

    /**
     * Metoda folosita de server pentru a trimite lista de pacienti catre tabelul din interfata grafică
     * @return
     */
    public MedicalRecord[] getHeapElements() {
        MedicalRecord[] activeList = new MedicalRecord[size];
        System.arraycopy(heap, 0, activeList, 0, size);
        return activeList;
    }
}