package eu.multisensor.chunking;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.FieldAnalysisRequest;
import org.apache.solr.client.solrj.response.AnalysisResponseBase.AnalysisPhase;
import org.apache.solr.client.solrj.response.FieldAnalysisResponse;
import org.apache.solr.client.solrj.response.FieldAnalysisResponse.Analysis;
import org.apache.solr.client.solrj.util.ClientUtils;


// there is a conflict between these:
import edu.upf.taln.TokenInfo;
//import org.apache.solr.client.solrj.response.AnalysisResponseBase.TokenInfo;
/**
 * @author joan
 *
 */
public class ConceptScoring {

	private final static Logger log = LoggerFactory.getLogger(ConceptScoring.class);

	private static final String fieldType = "analyzed_stems";
	   
    private Map<String,TermInfo> terms;
	private Map<String,String> candidates;

    private int maxFreq;
    private int maxWordLength;

    private UseCaseSolrClient useCaseClient;

    private boolean includesDoc;

    private double domDocs;
    private double refDocs;

    public ConceptScoring(UseCaseSolrClient client, boolean includesDoc)
            throws SolrServerException, IOException {
                
        this.terms = new HashMap<String, TermInfo>();
        this.candidates = new HashMap<String, String>();
        
        this.useCaseClient = client;
        this.domDocs = useCaseClient.getUseCaseTotalDocs();
        this.refDocs = useCaseClient.getReferenceTotalDocs();
        
        this.includesDoc = includesDoc;
        
        this.maxWordLength = 0;
    }

	
	/**
	 * this is a recursive function to computed the nested frequency
	 * The nested frequency indicates in how many bigger multiwords a (multi)word can be found
	 * If a belongs to a_b a_c and a_d of size 2  and to a_b_e and a_b_f 
	 * finding a_ we find the freq of a_b and a_c and a_d but not the one of level 3 
	 * maybe we have the some of level 3 but not all of them 
	 * imagine we have a_b_e but not a_b_f or a_c_*
	 * 
	 * If a_b_e belongs to a bigger multiword but is not in the doucument then maybe is not so important
	 * then we count that a_b_e belongs to a 1 unit  a_b_e_* with freq the nestedFreq
	 * if we have a_b then if the freq a_b_* is bigger than the parents of a_b_ then we add the rem freq to 1 to a_b_*.
	 * 
	 * @param size
	 */
	
	
	private void computeNestedValues(int size) {
		if (size==0) return;
		for (TermInfo t:terms.values()){
			if (t.numWords !=size)continue;
			// at this point all the parents have been processed 
			t.globalFreq=t.domFreq+t.refFreq;
			int longer=1;
			if (t.longerParents==0 && t.upperFreq>0){
				longer=2;
				t.longerParents=1;
				t.parentFreq=t.upperFreq;
			} else if (t.parentFreq<t.upperFreq) {
				t.parentFreq=t.upperFreq;
				t.longerParents++;
			}
			// propagate
			// to all right childs and grand-childs
			// but only to left grand-childs from  right childs 
			propagate(t,longer,t.left,false);
			propagate(t,longer,t.right,true);
		}
		computeNestedValues(size-1);
	}
	
	private void propagate(TermInfo t,int longer, TermInfo son, boolean right) {
		if (son==null) return;
		if (son.reference.equals("the evolution fresh greek yogurt"))
			System.out.println(t.reference);
		son.longerParents+=longer;
		son.parentFreq+=t.globalFreq;
		if (!right){
			propagate(t,longer,son.left,false);
		}
		propagate(t,longer, son.right,true);
				
	}

	public ArrayList<TermInfo> getSubstrings(TermInfo tI) {
		return getSubstrings(tI,Boolean.FALSE);
	}
	
	private ArrayList<TermInfo> getSubstrings(TermInfo tI,Boolean right) {
		// sorted or not... some of them can be original
		ArrayList<TermInfo> t=new ArrayList<TermInfo>();
		if (tI.left!=null  && !right){
			t.add(tI.left );
			t.addAll(getSubstrings(tI.left,Boolean.FALSE));
		}
		
		if (tI.right!=null){
			t.add(tI.right);
			t.addAll(getSubstrings(tI.right,Boolean.TRUE));
		}
		
		return t;
	}
	

	/**
	 * 
	 * @param words
	 * @param candidate string with the original text, only for the top one (the original one)
	 * @param right if your are the right substring your left substrings will be redundant, so some actions may be skipped. 
	 * @param level c-value needs to know in how many supper terms in embedded, level will help to do it. 
	 * @param FreqParents c-value also needs the freq of the upper elements in the list....
	 * @return
	 * @throws SolrServerException 
	 * @throws IOException 
	 */
	public TermInfo generate_substrings(TermInfo parent, TokenInfo[] words, Chunk candidate, Boolean right) throws SolrServerException, IOException {

			String reference = getString(words);
			TermInfo result = terms.get(reference);
			
			if (result != null) {// already there with their subwords
				if (candidate != null) {
				    result.addVariant(candidate); //and frequency
				} else {
				    result.addVariant(reference);
				}
				return result;
			}
			
			//  (tI==null)
			// the reference not in the system
			// find in solr the freq and everything
			
			// match as term or as part of a upper term?
			// on domain corpus
			String ref=	getSolrString(words);
			String concatRef = ref.trim().replace(" ", "_ _");
			String query = "term_lemma:(\""+ref+"\" \""+concatRef+"\" \"_"+concatRef+"\"  \"_"+concatRef+"\"_ \""+concatRef+"_\")";
			long domFreq = this.useCaseClient.getUseCaseFreq(query);
			
			if (!this.includesDoc) {
			    domFreq += 1.0;
			}
			
			// on ref corpus
			long refFreq = this.useCaseClient.getReferenceFreq(query);
            // TODO: If every document belongs to a given domain (with many different domains) and only
			// to a domain then the reference corpus should be all the documents.
			
			// as nested into other terms
			// compute the nested freq   ws_term:(*_dairy *_dairy_* dairy_*)
			// it can only be done in domains where terms have already been detected
			// it can not be done without previous term detection
			query = "term_lemma:(\"_"+concatRef+"\"  \"_"+concatRef+"_\" \""+concatRef+"_\")";
			
			// without filtering that is... all domains
            // TODO: What?? It was filtering it!  
			long nestf = this.useCaseClient.getReferenceFreq(query);
			
			if (candidate != null){
				result = new TermInfo(reference, 1, domFreq, refFreq, nestf, candidate, words.length);
			} else {
				result = new TermInfo(reference, 1, domFreq, refFreq, nestf, reference, words.length);
			}
			//String reference, int docFreq, double domFreq, long refFreq,
			// long upperFreq, String variant, boolean original,int words
			if (maxWordLength < words.length) {
			    maxWordLength = words.length ;
			}
			terms.put(reference, result);
			
			// should be reference also reference for itself?
			// if not there?
			candidates.put(reference, reference);		    
 
		    
	        if (words.length == 1) {
	        	return result;
	        } else {
		            if (!right){
		            	 if (words[words.length-2].getLemma().equalsIgnoreCase("of")){
		            		 if (words.length==2) return result;
		            		 result.left=generate_substrings(result,Arrays.copyOfRange(words, 0, words.length-2),null,false);
		            	 } else { 
		            		 result.left=generate_substrings(result,Arrays.copyOfRange(words, 0, words.length-1),null,false); 
		            	 }
		            } else {
		            	// left should exist:and its parent's.left.right 
		            	// left has to be processced before right and be depth first (as it is) 
		            	result.left = parent.left.right;
		            }
		           	 if (words[1].getLemma().equalsIgnoreCase("of")){
		           		 if (words.length==2) return result;
		        		 result.left=generate_substrings(result,Arrays.copyOfRange(words, 2, words.length),null,false);
		           	 }else { 
		        		 result.left=generate_substrings(result,Arrays.copyOfRange(words, 1, words.length),null,false);    
			        }
	        }
	        return result;
	}
	
    private static void processRecSubstrings(String[] words) {
        processRecSubstrings(words, true);
    }
    
	private static void processRecSubstrings(String[] words, boolean reduceLeft) {
        
        System.out.println(StringUtils.join(words, " "));
	    if (words.length > 1) {
	        processRecSubstrings(Arrays.copyOfRange(words, 0, words.length-1), false);
	        
	        if (reduceLeft) {
	            processRecSubstrings(Arrays.copyOfRange(words, 1, words.length), true);
	        }
	    }
	}
    
    private static void processIterSubstrings(String[] words) {

        int i = 0;
        while (i < words.length) {
            int j = words.length;
            while (j != i) {
                System.out.println(StringUtils.join(Arrays.copyOfRange(words, i, j), " "));
                j--;
            }
            i++;
        }
    }
	
    public String getString(TokenInfo[] words) {
        String reference = "";
        String separator = "";
        for (int i = 0; i < words.length; i++) {
            reference += separator + words[i].getLemma();
            separator = " ";
        }
        return reference;
    }
    
	public String getSolrString (TokenInfo[] words){
		String reference="";
		String separator="";
		for (int i=0;i<words.length;i++){
			reference+=separator+ClientUtils.escapeQueryChars(words[i].getLemma());
			separator=" ";
		}
		return reference;
	}
	
	public String[] getStemsSolr (String termCandidate) throws SolrServerException, IOException{
		// find  the stem by means of a solr query
		FieldAnalysisRequest rq = new FieldAnalysisRequest();
		rq.addFieldType(fieldType);
		rq.setQuery(termCandidate);
		rq.setFieldValue(termCandidate);
		FieldAnalysisResponse result = rq.process(this.useCaseClient.solrCli);
		
		// extract term
		Analysis an = result.getFieldTypeAnalysis(fieldType);
		int phases = an.getQueryPhasesCount();
		
		AnalysisPhase anP = null;
        Iterator<AnalysisPhase> it = an.getQueryPhases().iterator();
		while(it.hasNext()) {
		    anP = it.next();
		}
		
		List<org.apache.solr.client.solrj.response.AnalysisResponseBase.TokenInfo> listT = anP.getTokens();

		int i=0;
        String[] words = new String[listT.size()];
		for (org.apache.solr.client.solrj.response.AnalysisResponseBase.TokenInfo T : listT){
			words[i++]=T.getText();
		}
		return words;
	}
	
    public String addCandidate(Chunk termCandidate) throws SolrServerException, IOException {
        // to split multiword tokens
    	
         // candidate should be transformed to stems, check if already found in document.
    	String candidate= termCandidate.getText();
        String reference = candidates.get(termCandidate.getText());
        if (reference == null) {
            
            // not yet in the document the term candiate
            TokenInfo[] words = termCandidate.getTokensList();
            
            // if stemmed version exists... that easy just add an new variant to the stem
            reference = getString(words);
            TermInfo tI = terms.get(reference);
            if (tI != null) {
                tI.addVariant(termCandidate); // and frequency
                
            } else {
                // get all substrings (including the proper one) and add them to
                // the list of term candidates
                tI = generate_substrings(null, words, termCandidate, Boolean.FALSE);
            }
        } else {
            TermInfo tI = terms.get(reference);
            tI.addVariant(termCandidate); // and frequency , true because is
                                          // original
        }
        return reference;
    }
	
    /**
     * computes the scores and returns a sorted list of term candidates, terms
     * are sorted by domScore
     * 
     * @return
     * @throws Exception
     */
    public ArrayList<TermInfo> computeScores() throws Exception {

        // find the max freq in document to get the tf
        computeNestedValues(maxWordLength);
        
        maxFreq = 0;
        for (TermInfo t : terms.values()) {
            if (t.freq > maxFreq) {
                maxFreq = t.freq;
            }
        }
        
        // compute the scores.
        for (TermInfo t : terms.values()) {
            t.computeScore(maxFreq, domDocs, refDocs, maxFreq);
        }

         ArrayList<TermInfo> a = new ArrayList<TermInfo>();
       // add the terms
        for (int size=maxWordLength;size>0;size--){
            for (TermInfo t:terms.values()){
                if (t.numWords !=size)continue;
                if (t.isOriginal() || !t.selected ) {a.add(t); t.selected=true;}
                // check the sons
                if(t.left!= null && t.right!=null){
                    if (t.left.cScore> t.right.cScore){
                        if (!t.left.selected) {a.add(t.left);t.left.selected=true;}
                    }else
                        if (!t.right.selected) {a.add(t.right);t.right.selected=true;}
                }
            }
        }
         Collections.sort(a);
         
        ArrayList<TermInfo> b=new ArrayList<TermInfo>();
        ArrayList<TermInfo> c=new ArrayList<TermInfo>();
        for (TermInfo tI :a){
            if ((tI.domScore<=0.8) || Math.max(tI.globalFreq, tI.freq )<2 || (tI.cScore<=2&& tI.numWords!=1) ) {
                c.add(tI);
                continue;
             }
            b.add(tI);
        }
        //System.out.println(b.size());
        //System.out.println(c.size());

        b.addAll(c);
        // System.out.println(b.size());
      
        return b;
    }	
}


