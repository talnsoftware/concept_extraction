package eu.multisensor.services;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import opennlp.tools.util.InvalidFormatException;

import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.upf.taln.TokenInfo;
import edu.upf.taln.nif.annotation.SentenceAnnotation;
import edu.upf.taln.nif.annotation.TextAnnotation;
import eu.multisensor.chunking.Chunk;
import eu.multisensor.chunking.Chunker;
import eu.multisensor.chunking.ConceptScoring;
import eu.multisensor.chunking.TermInfo;
import eu.multisensor.chunking.UseCaseSolrClient;
import eu.multisensor.utils.BasicOpenNLPChunker;
import eu.multisensor.utils.BasicOpenNLPChunker.ChunkAnnotation;

public class ConceptExtractor {
    
    private final static Logger log = LoggerFactory.getLogger(ConceptExtractor.class);

    private BasicOpenNLPChunker basicChunker;

    private Chunker depChunker;

    public void init(String basePath) throws InvalidFormatException, IOException, URISyntaxException {
                
        init(basePath, "en");
    }

    private void init(String basePath, String lang) throws InvalidFormatException, IOException {
        
        InputStream sentenceIS;
        InputStream tokenIS;
        InputStream posIS;
        InputStream chunkerIS;
        
        sentenceIS = new FileInputStream(basePath + "/models/en-sent.bin");
        tokenIS = new FileInputStream(basePath + "/models/en-token.bin");
        posIS = new FileInputStream(basePath + "/models/en-pos-maxent.bin");
        chunkerIS = new FileInputStream(basePath + "/models/en-chunker.bin");
        
        basicChunker = new BasicOpenNLPChunker(sentenceIS, tokenIS, posIS, chunkerIS);
        
        HashSet<String> accepted = new HashSet<String>();
        accepted.add("NP");
        
        HashSet<String> excludedPOS = new HashSet<String>();
        excludedPOS.add("DT");
        excludedPOS.add("PDT");
        excludedPOS.add("CD");
        excludedPOS.add("CC");
        excludedPOS.add("IN");
        
        basicChunker.setAcceptedChunkTypeFilter(accepted);
        basicChunker.setExcludedPOSFilter(excludedPOS);
        
        String rulesPath = basePath + "/chunker_" + lang + ".rules";
        depChunker = new Chunker(rulesPath);
        
    }

    public Set<ChunkAnnotation> processBasic(String text, TreeSet<SentenceAnnotation> sentences, UseCaseSolrClient client)
            throws Exception {
        
        log.info("Chunking...");
        List<ChunkAnnotation> chunkAnns = new ArrayList<ChunkAnnotation>();
        for (SentenceAnnotation sentenceAnn : sentences) {
            
            chunkAnns.addAll(basicChunker.chunkSentence(text, sentenceAnn.offsetBegin));
        }
        
        return extractTerms(client, chunkAnns);
    }

    public Set<ChunkAnnotation> processWithDeps(String text, TreeMap<TextAnnotation, TokenInfo[]> depInfo, UseCaseSolrClient client)
            throws Exception {
        
        log.info("Chunking...");
        List<ChunkAnnotation> chunkAnns = new ArrayList<ChunkAnnotation>();
        for (TextAnnotation sentence: depInfo.keySet()){
            try {
                TokenInfo[] sentenceTokens = depInfo.get(sentence);
                //log.info(sentence.anchor);
                List<ChunkAnnotation> annots = depChunker.annotateSentence(sentenceTokens);
                //log.info(sentence.anchor);
                // log.info("Chunks: " + annots.size()+"");
                
                chunkAnns.addAll(annots);
                
            } catch (Exception e) {
                log.info(sentence.anchor);
                throw e;
            }
            
        }
        
        return extractTerms(client, chunkAnns);
    }

    private Set<ChunkAnnotation> extractTerms(UseCaseSolrClient client, List<ChunkAnnotation> chunks)
            throws SolrServerException, IOException, Exception {
        
        log.info("Scoring preprocess...");
        ConceptScoring scorer = new ConceptScoring(client, false);

        for (ChunkAnnotation chunkAnn : chunks) {
        	// log.info("chunk: " +chunkAnn.chunk.getTextLemma() );
            scorer.addCandidate(chunkAnn.chunk);
        }
        
        log.info("Computing scores...");
        ArrayList<TermInfo> terms = scorer.computeScores();
        log.info("Terms: " + terms.size());

        /*
        log.info("Filtering terms...");
        Set<ChunkAnnotation> annotations = new TreeSet<ChunkAnnotation>();
        for (TermInfo term : terms) {
            
            filterTerm(annotations, term);
            
            ArrayList<TermInfo> subTerms = scorer.getSubstrings(term);
            for (TermInfo subTerm : subTerms){
                filterTerm(annotations, subTerm);
            }
        }
     
        for (TermInfo term : terms) {
        		System.out.println("term: " +term.reference +" \t c-value:"+term.cScore +"\t  domScore:"+term.domScore   );
             }  
         */
        List<TermInfo> selectedTerms;
        if (terms.size() > 12) {
            selectedTerms = terms.subList(0, 12);
        } else {
            selectedTerms = terms;
        }
       
        Set<ChunkAnnotation> annotations = new TreeSet<ChunkAnnotation>();
        for (TermInfo term : selectedTerms) {
    		System.out.println("term: " +term.reference +" \t c-value:"+term.cScore +"\t  domScore:"+term.domScore   );
            for (Chunk chunk : term.variants) {
                ChunkAnnotation selectedAnn = chunk.annotation;
                selectedAnn.score = term.domScore;
        		System.out.println("selected: " +selectedAnn.chunk.getText()  + selectedAnn.score);
        		boolean res = annotations.add(selectedAnn);
            }
        }
        return annotations;
    }

    private void filterTerm(Set<ChunkAnnotation> annotations, TermInfo term) {
        
        if (term.domScore > 1.2 &&
                (term.cScore > 15.0 ||
                 term.freq > 4.0 ||
                 term.globalFreq > 6.0)) {
            
            // anotar
            log.info("Adding term variants");
            for (Chunk chunk : term.variants) {
                annotations.add(chunk.annotation);
            }
            
        } else if (term.domScore > 1.2) {
            log.info("Discarded by domScore");
            
        } else {
            log.info("Discarded by others");
        }
    }
}
