package eu.multisensor.services;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

import javax.servlet.ServletContext;

import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;
import edu.upf.taln.nif.annotation.SentenceAnnotation;
import edu.upf.taln.nif.annotation.TextAnnotation;
import eu.multisensor.containers.ConceptContainer;
import eu.multisensor.containers.IConceptContainer;
import eu.multisensor.containers.SentenceContainer;
import eu.multisensor.containers.Span;
import eu.multisensor.containers.TermContainer;
import eu.multisensor.containers.TokenContainer;
import eu.multisensor.utils.NIFSentence;
import eu.multisensor.utils.SolrUtils;
import eu.multisensor.utils.SolrUtils.AnalysisInfo;
import eu.multisensor.utils.SolrUtils.TfIdf;

public class OldConceptExtractor {
    
    private final static Logger log = LoggerFactory.getLogger(OldConceptExtractor.class);

    private static final double TF_IDF_THRESHOLD = 0;
    
    static class TermCandidates {
        HashMap<TermContainer, ArrayList<TermContainer>> termMap = new HashMap<TermContainer, ArrayList<TermContainer>>();
        StringBuilder candidateStr = new StringBuilder();
    }
    
    static class CorpusFrequencies {
        HashMap<String, Integer> frequencies = new HashMap<String, Integer>();
        int totalStoredDocs;
    }

    private HashMap<UseCaseEnum, CorpusFrequencies> storedFrequencies;
    
    private SentenceDetectorME sentenceDetector;
    private TokenizerME tokenizer;
    private POSTaggerME posTagger;
    private ChunkerME chunker;

    public void init(ServletContext context) throws InvalidFormatException, IOException, URISyntaxException {
        
        InputStream sentenceIS;
        InputStream tokenIS;
        InputStream posIS;
        InputStream chunkerIS;
        
        String basePath;
        if (context != null) {
            basePath = context.getRealPath("/WEB-INF");
            if (basePath == null) {
                throw new FileNotFoundException("WEB-INF directory not found!");
            }
            
        } else {
            basePath = "src/main/webapp/WEB-INF";
        }
        
        sentenceIS = new FileInputStream(basePath + "/models/en-sent.bin");
        tokenIS = new FileInputStream(basePath + "/models/en-token.bin");
        posIS = new FileInputStream(basePath + "/models/en-pos-maxent.bin");
        chunkerIS = new FileInputStream(basePath + "/models/en-chunker.bin");
        
        SentenceModel sentenceModel = new SentenceModel(sentenceIS);
        sentenceDetector = new SentenceDetectorME(sentenceModel);
        
        TokenizerModel tm = new TokenizerModel(tokenIS);
        tokenizer = new TokenizerME(tm);
        
        POSModel pm = new POSModel(posIS);
        posTagger = new POSTaggerME(pm);
        
        ChunkerModel chunkerModel = new ChunkerModel(chunkerIS);
        chunker = new ChunkerME(chunkerModel);
        
        storedFrequencies = new HashMap<UseCaseEnum, CorpusFrequencies>();
        storedFrequencies.put(UseCaseEnum.UC1_1, loadFrequencies(basePath + "/stored_freq_ep.txt"));
        log.info("UC1.1 frequencies loaded! Total docs: " + storedFrequencies.get(UseCaseEnum.UC1_1).totalStoredDocs);
        
        storedFrequencies.put(UseCaseEnum.UC1_2, loadFrequencies(basePath + "/stored_freq_ha2.txt"));
        log.info("UC1.2 frequencies loaded! Total docs: " + storedFrequencies.get(UseCaseEnum.UC1_2).totalStoredDocs);
        
        storedFrequencies.put(UseCaseEnum.UC2, loadFrequencies(basePath + "/stored_freq_d.txt"));
        log.info("UC2 frequencies loaded! Total docs: " + storedFrequencies.get(UseCaseEnum.UC2).totalStoredDocs);
    }

    private CorpusFrequencies loadFrequencies(String filepath) throws IOException {
        
        CorpusFrequencies cf = new CorpusFrequencies();
        
        CSVReader reader = new CSVReader(new FileReader(filepath), '\t');
        
        String [] nextLine;
        nextLine = reader.readNext();
        cf.totalStoredDocs = Integer.parseInt(nextLine[0]);
        
        while ((nextLine = reader.readNext()) != null) {
            
            String term = nextLine[0];
            int docsWithTerm = Integer.parseInt(nextLine[1]);
                        
            cf.frequencies.put(term, docsWithTerm);
        }
        
        reader.close();
        
        return cf;
    }
    public IConceptContainer processWholeText(String text, boolean useLinguatec) throws Exception {
    	// TODO
       // return processWholeText(text, UseCase.UC1_2, useLinguatec);
        return processWholeText(text,UseCase.getUseCase("en" ,"UC1_2") ,useLinguatec);
   }
    
    public IConceptContainer processWholeText(String text, UseCase useCase, boolean useLinguatec) throws Exception {
        return processWholeText(text, useCase, useLinguatec, false);
    }

    public IConceptContainer processWholeText(String text, UseCase useCase, boolean useLinguatec, boolean useSolr) throws Exception {
        
        ConceptContainer container = new ConceptContainer(text);

        TermCandidates docCandidates;
        if (useLinguatec) {
            docCandidates = processWholeTextLinguatec(container);
            //docCandidates = processWholeTextStanfordLinguatec(container);
        } else {
            docCandidates = processWholeTextOpenNLP(container);
        }
        
        selectConcepts(container, docCandidates, useCase, useSolr);
        
        return container;
    }

    private void selectConcepts(ConceptContainer container, TermCandidates docCandidates, UseCase useCase, boolean useSolr) throws IOException, SolrServerException {

        Map<String, TfIdf> termList;
        if (useSolr) {
            AnalysisInfo info = new AnalysisInfo();
            String randId = "temp" + (new Random().nextInt());
            info.tokens = docCandidates.candidateStr;
            //info.lemmas = docCandidates.candidateStr;

            SolrClient solrCli = SolrUtils.getClient();
            SolrUtils.add2Solr(solrCli, randId, info);
            System.out.println(randId);
            
            termList = SolrUtils.solrGetTF_IDF(solrCli, randId, "token");
            SolrUtils.removeDoc(solrCli, randId);
            
        } else {
            termList = getStoredTfIdf(container, docCandidates, useCase);
        }
        
        
        for(SentenceContainer sentence : container.getSentences()) {
            List<TermContainer> concepts = sentence.getConcepts();
            for(int i = concepts.size()-1 ; i >= 0; i--) {
                
                TermContainer term = concepts.get(i);
                TermContainer best = null;
                Double bestScore = 0.0;
                
                ArrayList<TermContainer> candidates = docCandidates.termMap.get(term);
                for(TermContainer candidate : candidates) {

                    TfIdf tfidf = termList.get(candidate.getLabel());
                    if (tfidf != null) {
                        Double score = tfidf.getScore();
                        if (score == null || score < TF_IDF_THRESHOLD) {
                            //concepts.remove(i);
                            //do nothing
                            
                        } else if (score == 1.0) {
                            //handle 1.0 scores using df
                        
                        } else if (score > bestScore){
                            
                            //String label = candidate.getLabel() + "#" + String.format("%.3f", score);
                            String label = String.format("%.3f", score);
                            
                            /*
                            log.info("Retrieving babelnet synsets...");
                            if (bn == null) {
                                bn = BabelNet.getInstance(bnConfigFile);
                            }
                            List<BabelSynset> synsets = bn.getSynsets(Language.EN, candidate.getText().toLowerCase());
                            log.info("Retrieved!");
                            
                            
                            candidate.setSynsets(synsets);
                            if (!synsets.isEmpty()) {
                                BabelSynset firstSynset = synsets.get(0);
                                label += "#" + firstSynset.getMainSense();
                            }
                            */
                            candidate.setLabel(label);                        

                            bestScore = score;
                            best = candidate;
                        }
                    }
                }
                
                concepts.remove(i);
                if (best != null) {
                    concepts.add(i, best);
                }
            }
        }
    }

    private Map<String, TfIdf> getStoredTfIdf(ConceptContainer container, TermCandidates docCandidates, UseCase useCase) throws IOException {
        
        String text = container.getText();
        int tokenCount = container.getTokenCount();
        
        CorpusFrequencies cf = storedFrequencies.get(useCase);

        HashMap<String, TfIdf> candidateScores = new HashMap<String, TfIdf>();
        for (ArrayList<TermContainer> candidates : docCandidates.termMap.values()) {

            for (TermContainer candidate : candidates) {
                String term = candidate.getText();
                if (cf.frequencies.containsKey(term)
                        && !candidateScores.containsKey(term)) {
                
                    int occ = getSubstringCount(text, term);
                    int docsWithTerm = cf.frequencies.get(term);
                    int totDocTerms = tokenCount;
                    
                    TfIdf tf_idf = new TfIdf(term, occ, docsWithTerm, totDocTerms, cf.totalStoredDocs);
                    
                    candidateScores.put(term,  tf_idf);
                }
            }
        }
        
        return candidateScores;
     }

    private int getSubstringCount(String text, String term) {
        int count = 0;
        int idx = text.indexOf(term, 0);
        while(idx >= 0) {
            count++;
            idx = text.indexOf(term, idx+term.length());
        }
        return count;
    }

    public TermCandidates processWholeTextLinguatec(ConceptContainer container) throws Exception {
        
        String text = container.getText();
                
        String response = LinguatecNERServiceClient.sendText(text, "en");
        ConceptExtractionRDF rdf = new ConceptExtractionRDF(response, text);
        TreeSet<SentenceAnnotation> sentences = rdf.getSentencesAndTokens(false);
        LinkedHashMap<Pair<Integer, Integer>, String> entities = new LinkedHashMap<Pair<Integer,Integer>, String>(); //rdf.getEntityAnnotations();
        
        TermCandidates docCandidates = new TermCandidates();
        for (SentenceAnnotation sentence : sentences)
        {
            int start = sentence.offsetBegin;
            Span sentSpan = new Span(start, sentence.offsetEnd);

            SentenceContainer sentenceContainer = new SentenceContainer(sentence.anchor, sentSpan, "");
            Collection<String> toks = new ArrayList<>();
            for (TextAnnotation token : sentence.tokens)
            {
                toks.add(token.anchor);
            }
            sentenceContainer.addTokens(text, toks, start);
            
            container.addSentence(sentenceContainer);
            
            int offset = 0;
            List<TokenContainer> tokenConts = sentenceContainer.getTokens();
            for(Pair<Integer, Integer> entitySpan : entities.keySet()) {
                
                if (entitySpan.getLeft() > sentSpan.getStart()
                        && entitySpan.getRight() < sentSpan.getEnd()) {
                    
                    String neText = entities.get(entitySpan);

                    Integer startTokenIdx = null;
                    Integer endTokenIdx = null;
                    while(offset < tokenConts.size()
                            && endTokenIdx == null) {
                        
                        TokenContainer tokenCont = tokenConts.get(offset);
                        if (startTokenIdx == null && neText.startsWith(tokenCont.getText())) {
                            startTokenIdx = offset;
                        }
                        
                        if (startTokenIdx != null && neText.endsWith(tokenCont.getText())) {
                            endTokenIdx = offset+1;
                        }
                        
                        offset++;
                    }

                    if (startTokenIdx == null || endTokenIdx == null) {
                        log.error("No token span found for NE: '" + neText + "'. It'll be discarded. ");
                        
                    } else {
                        Span neSpan = new Span(entitySpan.getLeft(), entitySpan.getRight());
                        Span neTokenSpan = new Span(startTokenIdx, endTokenIdx);
    
                        TermContainer ne = new TermContainer(neText, neText.replace(" ", "_"), neSpan, neTokenSpan);
                        sentenceContainer.addNE(ne);
                    }
                }
            }
            
            TermCandidates candidates = extractTermCandidates(text, tokenConts, entities);
            
            docCandidates.candidateStr.append(candidates.candidateStr);
            for(TermContainer term : candidates.termMap.keySet()) {
                sentenceContainer.addConcept(term);
                docCandidates.termMap.put(term, candidates.termMap.get(term));
            }
        }
        
        return docCandidates;
    }
    
    public TermCandidates processWholeTextOpenNLP(ConceptContainer container) throws Exception {

        String text = container.getText();
        HashMap<Pair<Integer, Integer>, String> entities = new HashMap<Pair<Integer, Integer>, String>();
        
        String[] sentences = sentenceDetector.sentDetect(text);
        
        int end = 0;
        TermCandidates docCandidates = new TermCandidates();
        for (String sentence : sentences) {
            
            List<String> tokens = Arrays.asList(tokenizer.tokenize(sentence));
            
            int start = text.indexOf(sentence, end);
            end = start + sentence.length();

            Span sentSpan = new Span(start, end);
            SentenceContainer sentenceContainer = new SentenceContainer(sentence, sentSpan, "");
            sentenceContainer.addTokens(text, tokens, start);
            
            container.addSentence(sentenceContainer);
            
            TermCandidates candidates = extractTermCandidates(text, sentenceContainer.getTokens(), entities);
            
            docCandidates.candidateStr.append(candidates.candidateStr);
            for(TermContainer term : candidates.termMap.keySet()) {
                sentenceContainer.addConcept(term);
                docCandidates.termMap.put(term, candidates.termMap.get(term));
            }
        }
        
        return docCandidates;
    }

    public ConceptContainer process(String text, List<NIFSentence> nifSentences, UseCase useCase, boolean useSolr) throws Exception {
        return process(text, nifSentences, useCase, new HashMap<Pair<Integer, Integer>, String>(), useSolr);
    }
    
    public ConceptContainer process(String text, List<NIFSentence> nifSentences, UseCase useCase, Map<Pair<Integer, Integer>, String> entities, boolean useSolr) throws Exception {
        
        log.info("Creating container...");
        ConceptContainer container = new ConceptContainer(text);

        log.info("Searching candidates...");
        TermCandidates docCandidates = this.processSentences(container, nifSentences, entities);
        
        log.info("Selecting concepts...");
        selectConcepts(container, docCandidates, useCase, useSolr);

        return container;
    }

    public TermCandidates processSentences(ConceptContainer container, List<NIFSentence> nifSentences, Map<Pair<Integer, Integer>, String> entities) throws Exception {

        String text = container.getText();
                
        int end = 0;
        TermCandidates docCandidates = new TermCandidates();
        for (NIFSentence nifSentence : nifSentences) {
            String sentence = nifSentence.text;
            String[] words = nifSentence.getTokenArray();
            
            List<String> tokens = Arrays.asList(words);
            
            int start = text.indexOf(sentence, end);
            end = start + sentence.length();

            Span sentSpan = new Span(start, end);
            SentenceContainer sentenceContainer = new SentenceContainer(sentence, sentSpan, "");
            sentenceContainer.addTokens(text, tokens, start);
            
            container.addSentence(sentenceContainer);
            
            TermCandidates candidates = extractTermCandidates(text, sentenceContainer.getTokens(), entities);
            
            docCandidates.candidateStr.append(candidates.candidateStr);
            for(TermContainer term : candidates.termMap.keySet()) {
                sentenceContainer.addConcept(term);
                docCandidates.termMap.put(term, candidates.termMap.get(term));
            }
        }
        
        return docCandidates;
    }
    
    /*
    public TermCandidates processWholeTextStanfordLinguatec(ConceptContainer container) throws Exception {
        
        String text = container.getText();
                
        String response = LinguatecNERServiceClient.sendText(text, "en");
        ConceptExtractionRDF rdf = new ConceptExtractionRDF(response, text);
        LinkedHashMap<Pair<Integer, Integer>, LinkedHashMap<Pair<Integer, Integer>, String>> sentences = rdf.getSentencesAndTokens();
        LinkedHashMap<Pair<Integer, Integer>, String> entities = rdf.getEntityAnnotations();

        return processWholeTextStanford(container, entities);
    }

    public TermCandidates processWholeTextStanford(ConceptContainer container, HashMap<Pair<Integer, Integer>, String> entities) throws Exception {
        
        String text = container.getText(); 
        
        Properties props = new Properties(); 
        props.put("annotators", "tokenize, ssplit, pos, lemma"); 
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props, false);
        Annotation document = pipeline.process(text);  
    
        int end = 0;
        TermCandidates docCandidates = new TermCandidates();
        for(CoreMap sentenceAnn: document.get(SentencesAnnotation.class)) {
            
            String sentence = sentenceAnn.get(TextAnnotation.class);
            
            int start = text.indexOf(sentence, end);
            end = start + sentence.length();

            Span sentSpan = new Span(start, end);
            SentenceContainer sentenceContainer = new SentenceContainer(sentence, sentSpan, "");

            int offset = start;
            List<String> words = new ArrayList<String>();
            List<String> lemmas = new ArrayList<String>();
            for(CoreLabel token: sentenceAnn.get(TokensAnnotation.class))
            {       
                String word = token.get(TextAnnotation.class);      
                String lemma = token.get(LemmaAnnotation.class);
                words.add(word);
                lemmas.add(lemma);
                
                int startWord = text.indexOf(word, offset);
                offset = startWord + word.length();
                
                Span tokenSpan = new Span(startWord, offset);
                TokenContainer tokenCont = new TokenContainer(word, lemma, tokenSpan);
                sentenceContainer.addToken(tokenCont);
            }
            
            container.addSentence(sentenceContainer);
            
            TermCandidates candidates = extractTermCandidates(text, sentenceContainer.getTokens(), entities);
            
            docCandidates.candidateStr.append(candidates.candidateStr);
            for(TermContainer term : candidates.termMap.keySet()) {
                sentenceContainer.addConcept(term);
                docCandidates.termMap.put(term, candidates.termMap.get(term));
            }
        }
        
        return docCandidates;
    }
    
    //*/
    
    private TermCandidates extractTermCandidates(String text, List<TokenContainer> tokens, Map<Pair<Integer, Integer>, String> entities)
            throws Exception {
        
        String[] words = new String[tokens.size()];
        for(int idx=0; idx < tokens.size(); idx++) {
            words[idx] = tokens.get(idx).getLabel();
        }
        
        //posTags are the parts of speech of every word in the sentence (The chunker needs this info of course)
        String[] posTags = posTagger.tag(words);
        
        //chunks are the start end "spans" indices to the chunks in the words array
        opennlp.tools.util.Span[] chunks = chunker.chunkAsSpans(words, posTags);
        
        TermCandidates candidates = new TermCandidates();
        for (int i = 0; i < chunks.length; i++) {
            
            if (chunks[i].getType().equals("NP")) {
                
                opennlp.tools.util.Span span = chunks[i];
                List<TokenContainer> spanTokens = tokens.subList(span.getStart(), span.getEnd());
                
                Integer startText = spanTokens.get(0).getSpan().getStart();
                Integer endText = spanTokens.get(spanTokens.size()-1).getSpan().getEnd();
                Span textSpan = new Span(startText, endText);
                
                if (!isOverlap(textSpan, entities, false)) {
                    
                    TermContainer term = new TermContainer("", "", textSpan, null);

                    String[] postTagArray = Arrays.copyOfRange(posTags, span.getStart(), span.getEnd());
                    
                    Integer offset = textSpan.getStart();
                    ArrayList<TermContainer> termList = new ArrayList<TermContainer>();
                    for(int idx = span.getStart(); idx < span.getEnd(); idx++) {
                        
                        while(idx < span.getEnd() && isStopWord(words, postTagArray, idx-span.getStart())) { 
                            idx++;
                        }

                        if (idx < span.getEnd()) {
                            
                            String npChunk = StringUtils.join(Arrays.copyOfRange(words, idx, span.getEnd()), "_").toLowerCase();
                            if (!npChunk.isEmpty()) {
                                
                                offset = text.indexOf(tokens.get(idx).getText(), offset);
                                String npText = text.substring(offset, textSpan.getEnd());
                                Span npSpan = new Span(offset, textSpan.getEnd());
                                Span tokenSpan = new Span(idx, span.getEnd());
                                
                                TermContainer subTerm = new TermContainer(npText, npChunk, npSpan, tokenSpan);
                                termList.add(subTerm);

                                candidates.candidateStr.append(" " + npChunk);
                            }
                        }
                    }
                    candidates.termMap.put(term, termList);
                }
            }
        }
        return candidates;
    }

    private boolean isStopWord(String[] words, String[] postTagArray, int idx) {

        //boolean isStopword = stopwords.contains(words[idx].toLowerCase());
        boolean isStopword = !(postTagArray[idx].startsWith("N")
                            || postTagArray[idx].startsWith("V")
                            || postTagArray[idx].startsWith("J")
                            || postTagArray[idx].startsWith("R"));
        return isStopword;
    }
     private boolean isOverlap(Span span, Map<Pair<Integer, Integer>, String> annotMap,
                boolean strict) {
        return isOverlap(span.getStart(), span.getEnd(), annotMap, strict);
    }
    
    private boolean isOverlap(int start, int end, Map<Pair<Integer, Integer>, String> annotMap,
                boolean strict) {

        for (Pair<Integer, Integer> offset : annotMap.keySet()) {
            
            Integer annotStart = offset.getLeft();
            Integer annotEnd = offset.getRight();
            
            if (start < annotEnd && end > annotStart) {
                return true;
            }
        }
        
        return false;
    }
    /*
    public Collection<? extends BabelNetAnnotation> babelfySentence(NIFSentence nifSentence, HashMap<Pair<Integer, Integer>, String> entities) throws IOException {
        
        String sentence = nifSentence.text;
        BabelfyAccessor ba = new BabelfyAccessor(this.bn);
        
        List<SemanticAnnotation> semAnnList = ba.getSemAnn(sentence, Language.EN);

        List<BabelNetAnnotation> nps = new ArrayList<BabelNetAnnotation>();
        for(SemanticAnnotation ann : semAnnList) {
            
            CharOffsetFragment offset = ann.getCharOffsetFragment();
            int startIdx = offset.getStart();
            int endIdx = offset.getEnd();
            
            BabelSynset synset = ba.getSynset(ann.getBabelSynsetID());
            if (!isOverlap(startIdx, endIdx, entities, false)) {

                BabelNetAnnotation annot = new BabelNetAnnotation();
                annot.text = sentence.substring(startIdx, endIdx);
                annot.sentence = sentence;
                annot.start = startIdx;
                annot.end = endIdx;
                
                annot.synsets = Arrays.asList(synset);
              
                nps.add(annot);
            }
        }
        
        return nps;
    }*/
}
