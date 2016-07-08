/**
 * 
 */
package eu.multisensor.chunking;


import java.util.LinkedList;
import java.util.ListIterator;
import edu.upf.taln.TokenInfo;
/**
 * @author Joan Codina
 * 
 *         This class contains the chunking information corresponding to a node
 *         and ways to compare them
 * 
 *         A chunk is composed by a central node and a set of sorted surrounding
 *         of dependent elements From a chunk different information can be
 *         obtained a) the central node b) the basic chunk d) if it contains an
 *         external reference (a number between parenthesis) e) the list of
 *         embedded chunks. always sorted.
 * 
 *         it includes a way to find related chunks. Two chunks are related if
 *         the central node is the same Are more related if they have the same
 *         external reference and then it finds he maximal embedded chunks that
 *         is similar.
 * 
 */
public class ChunkerNode {

	private String nodeContent;
	private int nodeNumber;
	private String nodePOS;
	private String nodeLemma;
	private String nodeMorpho;
	private String nodeDep;
	private TokenInfo node;
	private Boolean indefinite;
	private LinkedList<ChunkerSubNode> detNodes;
	private LinkedList<ChunkerSubNode> prevNodes;
	private LinkedList<ChunkerSubNode> nextNodes;
	private LinkedList<ChunkerSubNode> externalReferences;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		;
	}

	/**
	 * a chunker node with its data
	 * 
	 * @param nodeNumber
	 * @param nodeContent
	 * @param nodeLemma
	 * @param nodePOS
	 * @param nodeMorpho
	 * @param nodeDep
	 */
	public ChunkerNode(int nodeNumber, TokenInfo node, String nodeContent,
			String nodeLemma, String nodePOS, String nodeMorpho, String nodeDep) {
		this.nodeContent = nodeContent;
		this.nodeNumber = nodeNumber;
		this.nodePOS = nodePOS;
		this.nodeLemma = nodeLemma;
		this.indefinite=true;
		if (nodeMorpho ==null) 
		          this.nodeMorpho ="";
		     else this.nodeMorpho = nodeMorpho;
		this.nodeDep = nodeDep;
		this.setIndefinite(false);
		this.nextNodes = new LinkedList<ChunkerSubNode>();
		this.prevNodes = new LinkedList<ChunkerSubNode>();
		this.setDetNodes(new LinkedList<ChunkerSubNode>());
		this.node = node;
		this.externalReferences = new LinkedList<ChunkerSubNode>();
	}

	public Boolean getIndefinite() {
		return indefinite;
	}

	public void setIndefinite(Boolean indefinite) { 
		// once is false (definite) it will not change if an indefinite appears..
		this.indefinite = this.indefinite && indefinite;
	}


	public int getNodeNumber() {
		return this.nodeNumber;
	}

	public String getToken() {
		return this.nodeContent;
	}

	public String getLemma() {
		return this.nodeLemma;
	}

	public String getPOS() {
		return this.nodePOS;
	}

	public String getMorpho() {
		return this.nodeMorpho;
	}

	public String getDep() {
		return this.nodeDep;
	}

	/**
	 * all the subchunks must be appended in the same order as they appear in
	 * the text
	 * 
	 * @param text
	 * @param type
	 */
	public void addPrevEnd(ChunkerSubNode node) {
		prevNodes.add(node);
	}
	/**
	 * all the subchunks must be appended in the same order as they appear in
	 * the text
	 * 
	 * @param text
	 * @param type
	 */
	public void addPrev(ChunkerSubNode node) {
		prevNodes.addFirst(node);
	}
	public void addPrevAll(LinkedList<ChunkerSubNode> prevNodes) {
		// TODO Auto-generated method stub
		for (ChunkerSubNode node:prevNodes){
			this.prevNodes.add(node);
		}
	}
	
	public void addDET(ChunkerSubNode node) {
		getDetNodes().addFirst(node);
	}


	/**
	 * 
	 * @param text
	 * @param type
	 */
	public void addNext(ChunkerSubNode node) {
		nextNodes.add(node);
	}
	public void addNextAll(LinkedList<ChunkerSubNode> nextNodes) {
		// TODO Auto-generated method stub
		for (ChunkerSubNode node:nextNodes){
			this.nextNodes.add(node);
		}
	}
	
	public void addExt(ChunkerSubNode node) {
		externalReferences.add(node);
	}

	public String toString() {
		StringBuffer text = new StringBuffer(nodeContent);
		StringBuffer ltext = new StringBuffer(nodeLemma);
		StringBuffer result = new StringBuffer("\n");
		result.append("chunk: -" + text.toString() + "-  at pos:"
				+ this.nodeNumber + "  of type :" + this.nodeDep + "\n");
		if (!this.externalReferences.isEmpty()) {
			result.append("extra info: ");
			ListIterator<ChunkerSubNode> nodeItr = externalReferences
					.listIterator();
			while (nodeItr.hasNext()) {
				ChunkerSubNode subNode = (ChunkerSubNode) (nodeItr.next());
				result.append("--" + subNode.content + "--");
			}
			result.append("\n");
		}
		// append prev elements
		ListIterator<ChunkerSubNode> nodeItr = getDetNodes().listIterator();
		while (nodeItr.hasNext()) {
			ChunkerSubNode subNode = (ChunkerSubNode) (nodeItr.next());
			text.insert(0, subNode.content + " ");
			result.append("\t" + text.toString() + "\n");
		}
		result.append("\t" + text.toString() + "\n");
		nodeItr = prevNodes.listIterator();
		while (nodeItr.hasNext()) {
			ChunkerSubNode subNode = (ChunkerSubNode) (nodeItr.next());
			text.insert(0, subNode.content + " ");
			result.append("\t" + text.toString() + "\n");
		}

		nodeItr = nextNodes.listIterator();
		while (nodeItr.hasNext()) {
			ChunkerSubNode subNode = (ChunkerSubNode) (nodeItr.next());
			text.append(" " + subNode.content);
			result.append("\t" + text.toString() + "\n");
		}
		return result.toString();
	}

	/**
	 * Intellignet copy
	 * 
	 * @param origin
	 */
	/*
	public void merge(ChunkerNode origin) {
		ListIterator<ChunkerSubNode> nodeItr = origin.prevNodes.listIterator();
		while (nodeItr.hasNext()) {
			this.addPrev((ChunkerSubNode) (nodeItr.next()));
		}
		nodeItr = origin.nextNodes.listIterator();
		while (nodeItr.hasNext()) {
			this.addNext((ChunkerSubNode) (nodeItr.next()));
		}
	}
    */
	/**
	 * 
	 * @return a list of text chunks that can be generated from the current
	 *         chunk
	 */
	public LinkedList<String> getAllChunks() {

		LinkedList<String> chunks = new LinkedList<String>();
		StringBuffer text = new StringBuffer(nodeContent);
		chunks.add(text.toString());
		// append prev elements
		ListIterator<ChunkerSubNode> nodeItr = prevNodes.listIterator();
		while (nodeItr.hasNext()) {
			ChunkerSubNode subNode = (ChunkerSubNode) (nodeItr.next());
			text.insert(0, subNode.content + " ");
			chunks.add(text.toString());
		}

		nodeItr = nextNodes.listIterator();
		while (nodeItr.hasNext()) {
			ChunkerSubNode subNode = (ChunkerSubNode) (nodeItr.next());
			text.append(" " + subNode.content);
			chunks.add(text.toString());
		}
		return chunks;
	}

	public String getInfo() {
		StringBuffer result = new StringBuffer("");
		if (!this.externalReferences.isEmpty()) {
			ListIterator<ChunkerSubNode> nodeItr = externalReferences
					.listIterator();
			while (nodeItr.hasNext()) {
				ChunkerSubNode subNode = (ChunkerSubNode) (nodeItr.next());
				result.append(" " + subNode.content + " ");
			}
		}
		return result.toString();
	}

	/**
	 * It writes the chunks into the gate document. Basically what it does is to
	 * represent the info of the chunks into a gate document using the following
	 * format a notation of C_N for the basic chunk and then C_0 for the full
	 * chunk C_1 for the next embeded into that one.
	 * 
	 * 
	 * C_Last will incorporate all the information about the embeded chunks.
	 * 
	 * @param doc
	 *            gate document
	 * 
	 */

	public Chunk getChunk() {
		Chunk ck=new Chunk(nodeNumber,nodeContent,nodeLemma);
    	StringBuffer text=new StringBuffer(nodeContent); 
    	StringBuffer ltext=new StringBuffer(nodeLemma); 
    	StringBuffer treeText=new StringBuffer(" ("+ nodePOS +" " + nodeContent+") "); 
   	
    	String root=text.toString();
    	String root_lemma=this.nodeLemma;
    	//ck.startToken=this.node.getId();
    	//ck.endToken=this.node.getId();
   	    ck.root=root;
		ck.rootLemma=root_lemma;
  	    ck.info= getInfo();
   	    if (this.indefinite) {
   	    	ck.indefinite=true;
   	    }
   	    else {
   	    	ck.indefinite=false;
   	    }
   	   	int NodeNum=0;
    	// all previous nodes are a single one. NC_Base
    	// NC_1 NC_2 .... NC_last requires the post components.
    	// then NC_Full includes the determinant....
    	int LastNodeNum=+nextNodes.size();
 
    	//if (prevNodes.size()>0){
    	//	LastNodeNum++;
     	// append prev elements
 	     ck.base=new Chunk(nodeNumber,root,root_lemma);
 		 ck.base.root=root;
		 ck.base.rootLemma=root_lemma;
  	     ListIterator<ChunkerSubNode> nodeItr =  prevNodes.listIterator();
		 while(nodeItr.hasNext())
		    {
			 ChunkerSubNode subNode=nodeItr.next();
		     Chunk subChunk=subNode.chunk.getChunk();
			 ck.add(subChunk);
			 ck.base.add(subChunk);
		    }
			// NodeNum++;
			// String name="NC_"+NodeNum;
				 //ck.base.text= text.toString();
			 //ck.base.textLemma=ltext.toString();
			 //ck.base.endToken=ck.endToken;
             //ck.base.startToken=ck.startToken;
		 		 
    	// }
    	nodeItr =  nextNodes.listIterator();
		while(nodeItr.hasNext())
		    {
			 ChunkerSubNode subNode=nodeItr.next();
		     Chunk subChunk=subNode.chunk.getChunk();
			 ck.add(subChunk);
		    }       
         return ck;

	}

	public LinkedList<ChunkerSubNode> getDetNodes() {
		return detNodes;
	}

	public void setDetNodes(LinkedList<ChunkerSubNode> detNodes) {
		this.detNodes = detNodes;
	}

	public LinkedList<ChunkerSubNode> getPrevNodes() {
		return prevNodes;
	}


	public LinkedList<ChunkerSubNode> getNextNodes() {
		return nextNodes;
	}






	
}
