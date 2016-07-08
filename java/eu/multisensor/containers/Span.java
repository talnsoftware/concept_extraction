package eu.multisensor.containers;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class Span implements ISpan {

    final Pair<Integer, Integer> pair;

    public Span(Integer start, Integer end) {
        this.pair = new ImmutablePair<Integer, Integer>(start, end);
    }

    public Integer getStart() {
        return this.pair.getLeft();
    }

    public Integer getEnd() {
        return this.pair.getRight();
    }
}
