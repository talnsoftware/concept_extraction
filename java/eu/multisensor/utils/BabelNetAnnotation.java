package eu.multisensor.utils;

//import it.uniroma1.lcl.babelnet.BabelSynset;

import java.util.List;

import edu.upf.taln.babelnet_api.babelnet.BabelSynset;

public class BabelNetAnnotation {

    public List<BabelSynset> synsets;
    public String sentence;
    public int end;
    public int start;
    public String text;
    
    @Override
    public String toString() {
        return "(" + start + "," + end + ")=" + text;
    }
}