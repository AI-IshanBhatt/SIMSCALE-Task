package BO;

public class Trace {

    private final String id;
    private final Root root;

    public Trace(String id, Root root) {
        this.id = id;
        this.root = root;
    }

    public String getId() {
        return id;
    }

    public Root getRoot() {
        return root;
    }
}
