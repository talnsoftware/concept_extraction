package eu.multisensor.utils;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
/*
import it.uniroma1.lcl.babelfy.commons.BabelfyToken;
import it.uniroma1.lcl.babelfy.commons.PosTag;
import it.uniroma1.lcl.babelfy.commons.annotation.SemanticAnnotation;
import it.uniroma1.lcl.babelfy.core.Babelfy;
*/
//import it.uniroma1.lcl.babelnet.BabelNet;
//import it.uniroma1.lcl.babelnet.BabelSynset;
import edu.upf.taln.babelnet_api.babelnet.BabelNet;
import edu.upf.taln.babelnet_api.babelnet.BabelSynset;
import it.uniroma1.lcl.jlt.util.Language;

public class BabelfyAccessor {
//    private BabelNet bnInstance;
//    private Babelfy bfy;
//
//
//    public BabelfyAccessor(BabelNet bnInstance) {
//        this.bnInstance = bnInstance;
//        this.bfy = new Babelfy();        
//    }
//    
//    public List<SemanticAnnotation> getSemAnn(String sentence, Language lang) {
//       return this.bfy.babelfy(sentence, lang);
//    }
//
//    public List<SemanticAnnotation> getSemAnn(List<BabelfyToken> sentenceTokens, Language lang) {
//        return this.bfy.babelfy(sentenceTokens, lang);
//    }
//    
//    public List<BabelSynset> getSynsets(String sentence, Language lang) throws IOException {
//       return semAnn2Synsets(this.bfy.babelfy(sentence, lang));
//    }
//
//    public List<BabelSynset> getSynsets(List<BabelfyToken> sentenceTokens, Language lang) throws IOException {
//        return semAnn2Synsets(this.bfy.babelfy(sentenceTokens, lang));
//    }
//
//    private List<BabelSynset> semAnn2Synsets(List<SemanticAnnotation> semAnnList) throws IOException {
//        ArrayList<BabelSynset> synsetList = new ArrayList<BabelSynset>();
//        for(SemanticAnnotation ann : semAnnList) {
//            synsetList.add(this.getSynset(ann.getBabelSynsetID()));
//        }
//            
//        return synsetList;
//    }
//   
//    public BabelSynset getSynset(String synId) throws IOException {
//        return this.bnInstance.getSynsetFromId(synId);
//    }
//    
//    public static void test() throws IOException {
//            
//        BabelfyAccessor ba = new BabelfyAccessor(BabelNet.getInstance());
//
//        // ------ Babelfy-ing a sentence
//            
//            String inputText = "Germany supports nuclear power.";        
//            List<SemanticAnnotation> bfyAnnotations = ba.getSemAnn(inputText, Language.EN);
//            
//            System.out.println("Babelfy-ing a sentence");
//            for (SemanticAnnotation annotation : bfyAnnotations) {                    
//                String frag = inputText.substring(annotation.getCharOffsetFragment().getStart(),annotation.getCharOffsetFragment().getEnd() + 1);
//                System.out.println("\n ---> "+ frag + ":");            
//                System.out.println("BN synset: "+annotation.getBabelSynsetID());            
//                //System.out.println("\t" + annotation.getBabelNetURL());
//                System.out.println("DBpedia:" + annotation.getDBpediaURL());
//                
//                /*
//                BabelSynsetID bsynID = new BabelSynsetID(annotation.getBabelSynsetID());
//                BabelSynset syn = bsynID.toBabelSynset();
//                /*/
//                BabelSynset syn = ba.getSynset(annotation.getBabelSynsetID());
//                //*/                                     
//                System.out.println("synset source: " + syn.getSynsetSource());
//                System.out.println("synset type: " + syn.getSynsetType());
//                System.out.println("synset pos: " + syn.getPOS());
//
//                /* BabelSynset 3.5
//                System.out.println("WN sense key: " + syn.getMainSense(Language.EN).getSensekey());
//                System.out.println("WN offset: " + syn.getMainSense(Language.EN).getWordNetOffset());
//                /*/
//                // BabelSynset 2.5
//                System.out.println("--------------------");
//                System.out.println("DBpedia from synset: " + syn.getDBPediaURIs(Language.EN));
//                System.out.println("WN senses: " + syn.getSenses(Language.EN));
//                System.out.println("WN offsets: " + syn.getWordNetOffsets());
//                //*/
//            }
//    
//    
//            // ------ Babelfy-ing a tokenised sentence
//            List<BabelfyToken> tokenList = new ArrayList<BabelfyToken>();
//            BabelfyToken token = new BabelfyToken("Germany", PosTag.NOUN);
//            tokenList.add(token);        
//            token = new BabelfyToken("supports", PosTag.VERB);
//            tokenList.add(token);
//            token = new BabelfyToken("nuclear", PosTag.NOUN);
//            tokenList.add(token);
//            token = new BabelfyToken("power", PosTag.NOUN);
//            tokenList.add(token);        
//            tokenList.add(BabelfyToken.EOS);                
//            // you can also construct a Token with args: word, lemma, PosTag and language
//            
//            System.out.println("Babelfy-ing a tokenised sentence");
//            bfyAnnotations = ba.getSemAnn(tokenList, Language.EN);
//            for (SemanticAnnotation annotation : bfyAnnotations) {     
//                
//                /*
//                BabelSynsetID bsynID = new BabelSynsetID(annotation.getBabelSynsetID());            
//                BabelSynset syn = bsynID.toBabelSynset();
//                /*/
//                BabelSynset syn = ba.getSynset(annotation.getBabelSynsetID());
//                //*/
//
//                //System.out.println("\n ---> "+ syn.getMainSense(Language.EN).getLemma() + ":");
//                System.out.println("\n ---> "+ syn.getMainSense() + ":");
//                System.out.println("BN synset: "+annotation.getBabelSynsetID());            
//                //System.out.println("\t" + annotation.getBabelNetURL());
//                System.out.println("DBpedia:" + annotation.getDBpediaURL());
//                System.out.println("synset source: "+syn.getSynsetSource());
//                System.out.println("synset source: "+syn.getSynsetSource());
//                
//                /*  BabelSynset 3.5
//                System.out.println("WN sense key: "+syn.getMainSense(Language.EN).getSensekey());
//                /*/
//                // BabelSynset 2.5
//                System.out.println("--------------------");
//                System.out.println("WN senses: " + syn.getSenses(Language.EN));
//                //*/
//
//            }
//    }
}
