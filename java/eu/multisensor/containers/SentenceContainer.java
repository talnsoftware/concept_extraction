package eu.multisensor.containers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SentenceContainer extends BaseAnnContainer implements ISent {

    private List<TokenContainer> tokens = new ArrayList<TokenContainer>();
    private List<TermContainer> NEs = new ArrayList<TermContainer>();
    private List<TermContainer> concepts = new ArrayList<TermContainer>();

    public SentenceContainer(String text, Span span, String label) {
        super(text, label, span);
    }

    public void addToken(TokenContainer token) {
        this.tokens.add(token);
    }
    
    @Override
    public List<TokenContainer> getTokens() {
        return this.tokens;
    }

    public void addNE(TermContainer NE) {
        this.NEs.add(NE);
    }

    @Override
    public List<TermContainer> getNEs() {
        return this.NEs;
    }

    public void addConcept(TermContainer concept) {
        this.concepts.add(concept);
    }

    @Override
    public List<TermContainer> getConcepts() {
        return this.concepts;
    }

    public void addTokens(String text, Collection<String> strTokens, int offset) {
        
        for(String strToken : strTokens) {
            
            int start = text.indexOf(strToken, offset);
            offset = start + strToken.length();
            
            Span tokenSpan = new Span(start, offset);
            TokenContainer token = new TokenContainer(strToken, strToken, tokenSpan);
            this.tokens.add(token);
        }
    }

}
