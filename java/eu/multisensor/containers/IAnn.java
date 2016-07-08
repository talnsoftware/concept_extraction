package eu.multisensor.containers;

public interface IAnn extends Comparable<IAnn>
{
    public String getText();
    public Span getSpan();
    public String getLabel();
}