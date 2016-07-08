package eu.multisensor.containers;

public class BaseAnnContainer implements IAnn {

    private String text;
    private Span span;
    private String label;

    public BaseAnnContainer(String text, String label, Span span) {
        super();
        this.text = text;
        this.span = span;
        this.label = label;
    }

    @Override
    public String getText() {
        return this.text;
    }

    @Override
    public Span getSpan() {
        return this.span;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return this.label;
    }
    
    @Override
    public int compareTo(IAnn o) {
        return span.getStart() - o.getSpan().getStart();
    }
}
