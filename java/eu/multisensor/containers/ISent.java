package eu.multisensor.containers;

import java.util.List;

public interface ISent extends IAnn
{
    public List<TokenContainer> getTokens();
    public List<TermContainer> getNEs();
    public List<TermContainer> getConcepts();
}