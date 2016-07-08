package eu.multisensor.chunking;

import java.util.Set;
import java.util.TreeSet;



public class TermInfo implements Comparable<Object> {
    
	private static final double log2 = Math.log(2); 
	
	public String reference;
	public int numWords;
	public int freq;  // occurrences in document 
	public double tf; // tf 
	public double domScore; // prob of being a term specific to a domain
	public double cScore; // prob of a multword to be a term
	public double domProb; // probability to find that term in a document of the  domain corpus 
	public double refProb;// probability to find that term in a document of the  ref corpus (including the domain)
	public double idf;   // idf in the domain
	public double refIdf; // idf in the reference corpus
	public double tfIdf;
	public double tfIdf2;
	public Set<Chunk> variants; // vairants of the same string with only stemming or lemmatizer changes
	public Set<String> variantsB; // vairants not in the original document
	public double longerParents; // number of longer candidate terms. 
	// longerParents and nested freq Can be computed at the end, by levels: first longer words, then decreasing length one by one.  
	public double domFreq;// specific domain frequency
	public double refFreq; // general domain frequency
	public double globalFreq; // includes the general and the specific domain (it depends on how search is done)
	public double upperFreq; // freq as part of bigger NP's as found in the index 
	public double parentFreq; // sum of the freq's of the parents found 
	public 	TermInfo left;  
	public	TermInfo right;

    public boolean selected = false;		


	
	public TermInfo(String reference, int docFreq, double domFreq, long refFreq, long upperFreq, String variant,int words ) {
		this.reference = reference;
		this.domFreq = domFreq;
		this.refFreq = refFreq;
		this.freq = docFreq;
		this.tf = 0.0;
		domScore=0.0;
		variants=new TreeSet<Chunk>();
		variantsB=new TreeSet<String>();
		variantsB.add(variant);
		// subterms obtained after removing the last/first word, they can be obviously null
		// with recursive search all substrings are obtained
		left=null;
		right=null;	
		upperFreq=0;
	    longerParents=0;
	    numWords=words;
	}
	
    public TermInfo(String reference, int docFreq, double domFreq, long refFreq, long upperFreq, Chunk variant, int words) {
        
        this.reference = reference;
        this.domFreq = domFreq;
        this.refFreq = refFreq;
        this.freq = docFreq;
        this.tf = 0.0;
        domScore = 0.0;
        variants = new TreeSet<Chunk>();
        variantsB = new TreeSet<String>();
        variants.add(variant);
        // subterms obtained after removing the last/first word, they can be
        // obviously null
        // with recursive search all substrings are obtained
        left = null;
        right = null;
        upperFreq = 0;
        longerParents = 0;
        numWords = words;
    }
	
    public void computeScore(int totDocTerms, double domDocs, double refDocs, double maxFreq) {
        
        tf = 0.5 + 0.5 * (double) freq / (double) maxFreq;
        // Roberto's
        // tf = occ.floatValue() / (Float.MIN_VALUE + totDocTerms.floatValue());
        
        tf = freq / (Double.MIN_VALUE + totDocTerms);
        
        idf = Math.log10(domDocs / (Float.MIN_VALUE + domFreq));
        refIdf = Math.log10(refDocs / (Float.MIN_VALUE + refFreq));
        
        tfIdf = (tf * idf);
        tfIdf2 = (tf * refIdf);
        
        // prob
        // domProb = Math.log(2.0+domFreq)/Math.log(domDocs);
        // refProb = Math.log(2.0+refFreq)/Math.log(refDocs);
        // refProb = Math.log((2+refDocs)/(refFreq + 1))/ Math.log(refDocs+2);
        domProb = (domFreq) / (domDocs);
        refProb = (1 + refFreq) / (1 + refDocs);
        
        domScore = tf * Math.sqrt(domProb * (1 - refProb)); // all between 0 and 1
        domScore = Math.max(0, (domProb - refProb) / domProb);
        domScore = refIdf / idf;
        
        cScore = Math.log(numWords) / log2 * (globalFreq); // without +1 all
                                                           // single word terms
                                                           // have 0 c-domScore
                                                           // should be domFreq?
        
        if (longerParents > 0) {
            // compute the part of the nested terms or each children add the children.
            cScore = cScore - (1 / longerParents) * parentFreq;
        }
        // cScore = cScore/(globalFreq+1);
    }

	public void addVariant(String variant){
		variantsB.add(variant);
		freq++;
	}

	public void addVariant(Chunk chunkVariant) {
		variants.add(chunkVariant);
		freq++;
	}
	
	@Override
	public int compareTo(Object o) {
		double res= ((TermInfo)o).cScore * ((TermInfo)o).domScore - this.cScore*  this.domScore;			
		if (res>0.0) return 1;
		else if (res<0.0) return -1;
		else  { //equal value...then check the domain score
			res= ((TermInfo)o).domScore - this.domScore;			
			if (res>0.0) return 1;
			else if (res<0.0) return -1;		
		}
		return 0;//both values are equals.
	}

	public boolean isOriginal() {
		 return !variants.isEmpty();
	}

}   