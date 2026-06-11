public enum Priority {
    VERY_URGENT(1, "Foarte Urgent"),
    URGENT(2, "Urgent"),
    NORMAL(3, "Normal"),
    LOW(4, "Scazut");

    public int value;
    public String label;
    Priority(int value, String label)
    {
        this.value = value;
        this.label = label;
    }
}
