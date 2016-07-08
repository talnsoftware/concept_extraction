package eu.multisensor.containers;

import java.util.List;

import edu.upf.taln.babelnet_api.babelnet.BabelSynset;

public class TermContainer extends BaseAnnContainer implements ITerm {

    private Span tokenSpan;
    private List<BabelSynset> synsets;

    public TermContainer(String text, String label, Span span, Span tokenSpan) {
        super(text, label, span);
        this.tokenSpan = tokenSpan;
    }

    @Override
    public Span getTokenSpan() {
        return this.tokenSpan;
    }

    public void setSynsets(List<BabelSynset> synsets) {
        this.synsets = synsets;
    }
    
    public List<BabelSynset> getSynsets() {
        return synsets;
    }
}
