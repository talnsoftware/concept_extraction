/**
 * 
 */
package eu.multisensor.chunking;

import java.util.ListIterator;

import eu.multisensor.chunking.ChunkerExtractor.ElemType;

/**
 * @author Joan Codina
 *
 */
class ChunkerSubNode   {
	public ElemType rulesType;
    public String nodeType;
    public String content; 
    public String lcontent; //lemma content
    public StringBuffer subTree; 	

    public int start; // Pos, token where the sub-chunk starts
    public int end;  // Pos token where the sub-chunk ends
    public ChunkerNode chunk;
    
    public ChunkerSubNode(ElemType rulesType, String nodeType, String content,String lcontent,int start, int end,ChunkerNode Chunk,String subTree){
    	this.rulesType=rulesType;
    	this.nodeType=nodeType;
    	this.content=content;
    	this.lcontent=lcontent;
    	this.start=start;
    	this.end=end;
    	this.chunk=Chunk;
    	this.subTree=new StringBuffer(subTree);
    }
    

	public ChunkerSubNode(ChunkerSubNode next) {
		// TODO Auto-generated constructor stub
    	this.rulesType=next.rulesType;
    	this.nodeType=next.nodeType;
    	this.content=next.content;
    	this.lcontent=next.lcontent;
    	this.start=next.start;
    	this.end=next.end;
    	this.chunk=next.chunk;
    	this.subTree=new StringBuffer(next.subTree);
	}
    	/**
    	 * integrates two nodes into one. Subtree is pending
    	 * 
    	 * @param origin The origin mode was the previous head now becomes part of the new Chunk
    	 */
    	public void integrate(ChunkerSubNode origin) {
    		// first the one to integrate is before or after?
    	if (end<origin.end) { // origin is after.
    			end=origin.end; 
    			chunk.addNext(new ChunkerSubNode(origin));
    			content+=" "+origin.content;
    		   lcontent+="_"+origin.lcontent;
    	} else {
    		if (origin.start< start) 
    			start=origin.start;
    		chunk.addPrevEnd(new ChunkerSubNode(origin));
    		content=origin.content+" "+content;
    		lcontent=origin.lcontent+"_"+lcontent;
    	}
    		ListIterator<ChunkerSubNode> nodeItr = origin.chunk.getDetNodes().listIterator();
    		while (nodeItr.hasNext()) {
    			this.chunk.addDET(new ChunkerSubNode(nodeItr.next()));
    		}
 
    	}
       	/**
    	 * integrates two nodes into one. Subtree is pending
    	 * 
    	 * @param origin The origin mode was the previous head now becomes part of the new Chunk
    	 */
    	public void integrateInfo(ChunkerSubNode origin) {
    		// first the one to integrate is before or after?
    	if (end<origin.end) { // origin is after.
    			end=origin.end; 
     			// content+=" "+origin.content;
    	} else {
    		if (origin.start< start) 
    			start=origin.start;
    		// content=origin.content+" "+content;
    	}
            this.chunk.addExt(new ChunkerSubNode(origin));
    		ListIterator<ChunkerSubNode> nodeItr = origin.chunk.getDetNodes().listIterator();
    		while (nodeItr.hasNext()) {
    			this.chunk.addDET(new ChunkerSubNode(nodeItr.next()));
    		}
     		nodeItr = origin.chunk.getPrevNodes().listIterator();
     		while (nodeItr.hasNext()) {
     			this.chunk.addPrevEnd(new ChunkerSubNode(nodeItr.next()));
     		}
    		nodeItr = origin.chunk.getNextNodes().listIterator();
     		while (nodeItr.hasNext()) {
     			this.chunk.addNext(new ChunkerSubNode(nodeItr.next()));
     		}

    	}


}