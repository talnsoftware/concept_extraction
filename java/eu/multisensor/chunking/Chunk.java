package eu.multisensor.chunking;


import java.util.Iterator;
import java.util.TreeSet;

import edu.upf.taln.TokenInfo;
import eu.multisensor.utils.BasicOpenNLPChunker.ChunkAnnotation;

public class Chunk implements Comparable<Object> {
    
    private int startToken;
    private int endToken;
    
    public String root;
    public String rootLemma;
   
    private String text;
    private String textLemma;
    
    public String info;
   
    public boolean indefinite;
   
    public Chunk base;
   
    private TokenInfo[] tokens;
    private TreeSet<Integer> tokenSet;
   
    private int sentence;
    
    public ChunkAnnotation annotation;
  
   
    public TokenInfo[] getTokensList() {
		return tokens;
	}
	
    public Chunk(int number, String token, String lemma) {
        
        tokens = null;
        tokenSet = new TreeSet<Integer>();
        tokenSet.add(number);
        
        startToken = number;
        endToken = number;
        
        text = token;
        textLemma = lemma;
    }

    public void addNode(int number, String token, String lemma) {
        if (number < startToken) {
            startToken = number;
            text = token + text;
        }
        
        if (number > endToken) {
            endToken = number;
            text = text + token;
        }
        tokenSet.add(number);
    }
    
    public void add(Chunk member) {
        
        if (member.startToken < startToken) {
            startToken = member.startToken;
            text = member.text + text;
        }
        
        if (member.endToken > endToken) {
            endToken = member.endToken;
            text = text + member.text;
        }
        tokenSet.addAll(member.tokenSet);
    }

	public String getRoot() {
		return root;
	}
	
	public void setRoot(String root) {
		this.root = root;
	}
	
	public String getRootLemma() {
		return rootLemma;
	}
	
	public void setRootLemma(String rootLemma) {
		this.rootLemma = rootLemma;
	}
	
	public String getText() {
		return text;
	}
	
	public void setText(String text) {
		this.text = text;
	}
	
	public String getTextLemma() {
		return textLemma;
	}
	
	public void setTextLemma(String textLemma) {
		this.textLemma = textLemma;
	}
	
	public int getSentence() {
		return sentence;
	}
	
    public ChunkAnnotation setTokensList(TokenInfo[] tokens) {

    	// count the number of real tokens
        Iterator<Integer> tokenIt = tokenSet.iterator();
        int numberTokens=0;
        while (tokenIt.hasNext()) {
            int t = tokenIt.next();
        	numberTokens+= tokens[t].getLemma().split("_").length;
        }
    	
 
    	this.tokens = new TokenInfo[numberTokens];
        
        int i = 0;
        text = "";
        textLemma = "";
        String sep = "";
        String lSep = "";

        tokenIt = tokenSet.iterator();
        while (tokenIt.hasNext()) {
            int t = tokenIt.next();
        	String[] splits= tokens[t].getLemma().split("_");
        	String[] forms= tokens[t].getForm().split(" ");
            for (int s=0; s<splits.length; s++){
	            TokenInfo Token = new TokenInfo();
	            Token.setLemma(splits[s]);
	            Token.setForm(forms[s]);
	            Token.setPOS(tokens[t].getPOS());
	            this.tokens[i++] =Token;
	            text += sep + forms[s];
	            textLemma += lSep + splits[s];
	            sep = " ";
	            lSep = "_";
            }
        }
        
        // TODO: Los indices son así porque al construirse el array tokens empieza en 1 como el conll
        int start = tokens[1].getStartOffset();
        int end = tokens[i].getEndOffset();
        
        if (base != null) {
            base.setTokensList(tokens);
        }

        ChunkAnnotation chunkAnn = new ChunkAnnotation(start, end);
        chunkAnn.chunk = this;
        
        return chunkAnn;
    }
	public void setSentence(int i) {
		sentence=i;
	}
	
    @Override
    public int compareTo(Object o) {

        double res = this.text.compareTo(((Chunk) o).text);
        if (res > 0) {
            return 1;
        } else if (res < 0){
            return -1;
        }
        
        /* same text */
        res = this.startToken - ((Chunk) o).startToken;
        if (res < 0) {
            return 1;
        } else if (res > 0) {
            return -1;
        }
        return 0;
    }


}
