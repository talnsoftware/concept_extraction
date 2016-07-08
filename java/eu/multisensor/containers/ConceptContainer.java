package eu.multisensor.containers;

import java.util.ArrayList;
import java.util.List;

public class ConceptContainer implements IConceptContainer {

    private List<SentenceContainer> sentences = new ArrayList<SentenceContainer>();
    private String text;
    private int tokenCount;

    public ConceptContainer(String text) {
        super();
        this.text = text;
    }
    
    public void addSentence(SentenceContainer sentence) {
        this.sentences.add(sentence);
        tokenCount += sentence.getTokens().size();
    }
    
    @Override
    public List<SentenceContainer> getSentences() {
        return this.sentences;
    }

    @Override
    public String getText() {
        return this.text;
    }

    public int getTokenCount() {
        return tokenCount;
    }
}
