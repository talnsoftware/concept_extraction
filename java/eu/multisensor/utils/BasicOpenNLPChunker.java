package eu.multisensor.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.ling.tokensregex.PhraseTable.TokenList;
import edu.upf.taln.TokenInfo;
import eu.multisensor.chunking.Chunk;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.Span;

public class BasicOpenNLPChunker {

    private static class Annotation implements Comparable<Annotation>{
        public int start;
        public int end;       

        public Annotation(final int start, final int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public int compareTo(Annotation other) {
            return start-other.start + end-other.end;
        }
    }
    
    private static class TokenAnnotation extends Annotation {

        public TokenAnnotation(int start, int end) {
            super(start, end);
        }
    }

    public static class ChunkAnnotation extends Annotation {

        List<TokenAnnotation> tokens;
        public Chunk chunk; 
        public double score;

        public ChunkAnnotation(int start, int end) {
            super(start, end);
        }
    }

    private static class SentenceAnnotation extends Annotation {

        List<ChunkAnnotation> chunks;

        public SentenceAnnotation(int start, int end) {
            super(start, end);
        }
    }
    
    private SentenceDetectorME sentenceDetector;
    private TokenizerME tokenizer;
    private POSTaggerME posTagger;
    private ChunkerME chunker;
    
    private Set<String> excludedPOS = null;
    private Set<String> acceptedChunkTypes = null;
    
    public BasicOpenNLPChunker(InputStream sentenceIS, InputStream tokenIS, InputStream posIS, InputStream chunkerIS)
            throws InvalidFormatException, IOException {
        
        SentenceModel sentenceModel = new SentenceModel(sentenceIS);
        sentenceDetector = new SentenceDetectorME(sentenceModel);
        
        TokenizerModel tm = new TokenizerModel(tokenIS);
        tokenizer = new TokenizerME(tm);
        
        POSModel pm = new POSModel(posIS);
        posTagger = new POSTaggerME(pm);
        
        ChunkerModel chunkerModel = new ChunkerModel(chunkerIS);
        chunker = new ChunkerME(chunkerModel);
    }
    
    public void setExcludedPOSFilter(Set<String> filter) {
        this.excludedPOS = filter;
    }
    
    public void setAcceptedChunkTypeFilter(Set<String> filter) {
        this.acceptedChunkTypes = filter;
    }
    
    public List<SentenceAnnotation> chunkText(String text) {

        String[] sentences = sentenceDetector.sentDetect(text);
        
        int offset = 0;
        List<SentenceAnnotation> sentenceAnnotations = new ArrayList<SentenceAnnotation>();
        for (String sentence : sentences) {
            int start = text.indexOf(sentence, offset);
            List<ChunkAnnotation> chunks = chunkSentence(sentence, start);
            
            offset = start + sentence.length();
            SentenceAnnotation sentenceAnnotation = new SentenceAnnotation(start, offset);
            sentenceAnnotation.chunks = chunks;
            
            sentenceAnnotations.add(sentenceAnnotation);
        }
        return sentenceAnnotations;
    }

    public List<ChunkAnnotation> chunkSentence(String sentence, int offset) {
        String tokens[] = tokenizer.tokenize(sentence);
        return chunkTokens(sentence, offset, tokens);
    }

    public List<ChunkAnnotation> chunkTokens(String text, int offset, String[] tokens) {
        String tags[] = posTagger.tag(tokens);
        return chunkPOSTokens(text, offset, tokens, tags);
    }

    protected List<ChunkAnnotation> chunkPOSTokens(String text, int offset, String[] tokens, String[] tags) {
        
        TokenInfo[] tokenInfos = new TokenInfo[tokens.length];
        for (int idx = 0; idx < tokens.length; idx++) {
            String token = tokens[idx];
            
            TokenInfo tInfo = new TokenInfo();
            tInfo.setForm(token);
            tInfo.setLemma(token);
            
            tokenInfos[idx] = tInfo;
        }
        
        Span[] spans = chunker.chunkAsSpans(tokens, tags);
        
        List<ChunkAnnotation> chunkAnnotations = new ArrayList<ChunkAnnotation>();
        for (Span span : spans) {

            if (acceptedChunkTypes != null && acceptedChunkTypes.contains(span.getType())) {
                
                Chunk chunk = new Chunk(span.getStart(), "", "");

                int idx = span.getStart();
                while(idx < span.getEnd() && excludedPOS.contains(tags[idx])) {
                    idx++;
                }
                
                List<TokenAnnotation> tokenAnnotations = new ArrayList<TokenAnnotation>();
                while(idx < span.getEnd()) {
                    
                    if (excludedPOS.contains(tags[idx])) {
                        tokenAnnotations.clear();
                        break;
                    }
                    
                    String token = tokens[idx];
                    int start = text.indexOf(token, offset);
                    offset = start + token.length();
                    
                    tokenAnnotations.add(new TokenAnnotation(start, offset));

                    chunk.addNode(idx, tokens[idx], tokens[idx]);
                    chunk.setTokensList(tokenInfos);

                    idx++;
                }
                
                if (!tokenAnnotations.isEmpty()) {
                    int start = tokenAnnotations.get(0).start;
                    ChunkAnnotation chunkAnn = new ChunkAnnotation(start, offset);
                    chunkAnn.tokens = tokenAnnotations;
                    chunkAnn.chunk = chunk;
                    
                    chunk.annotation = chunkAnn;
                    
                    chunkAnnotations.add(chunkAnn);
                }
            }
        }
        return chunkAnnotations;
    }
}
