/**
 * Clasa reprezinta structura de date a unei fise medicale
 */
public class MedicalRecord {
    public int id;
    public String name;
    public String diagnosis;
    public String symptoms;
    public Priority priority;

    public MedicalRecord(int id, String name, String diagnosis, String symptoms,Priority priority)
    {
        this.id = id;
        this.name = name;
        this.diagnosis = diagnosis;
        this.symptoms = symptoms;
        this.priority = priority;
    }

    @Override
    public String toString(){
        return "ID Fisa: " + id + "/nPacient: " + name + "/nDiagnostic: " + diagnosis + " [" + priority.label + "]/n";
    }

}
