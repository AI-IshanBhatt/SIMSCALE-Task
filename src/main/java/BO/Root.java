package BO;

import java.util.ArrayList;

public class Root {

    private final String service;
    private final String start;
    private final String end;
    private final String span;

    private final ArrayList<Root> calls;

    Root(String service, String start, String end, String span, ArrayList<Root> calls) {
        this.service = service;
        this.start = start;
        this.end = end;
        this.span = span;
        this.calls = calls;
    }

    public String getService() {
        return service;
    }

    public String getStart() {
        return start;
    }

    public String getEnd() {
        return end;
    }

    public ArrayList<Root> getCalls() {
        return calls;
    }

    public String getSpan() {
        return span;
    }
}
