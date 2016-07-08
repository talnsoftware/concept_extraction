package eu.multisensor.chunking;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.ListIterator;

import edu.upf.taln.TokenInfo;




public class ChunkerExtractor {
	public enum ElemType {
		HEAD,
		IHEAD, // independent head: an independent head does not belong to the upper chunker. 
		NUMHEAD, // Numeric head; a numeric head tries to solve the problem where a cardinal (CD) is the head of the chunk
				// because of parsing problems. It acts as a head but like the verbs is reversed
		PART,		
		VERB,		 // Some verbs belong to a cluster, 
		HEAD_PART,  // it can be header or part depending on where it is...
		HEAD_VERB,  // it is the header of a verb chunk
		PENDING_PART, // The item itself may not belong to the chunk, but its decendants do. , which 
		INFO,  // like a number inside brackets
		EXCLUDE,  // excludes its pending elements
		DET,    //determinant
		IDET,    // indefinite determinant.
		INUM    // indefinite NUMBER.
	};
	
	public enum AppliesType {
		EVERYWHERE, BEFORE, AFTER,
		DAFTER, // after in a definite   
		OUTSIDE,INFO	
	};
		
/**
 * 	
 * @author joan Codina
 * A chunker rule is a set of conditions that have to be meet to be head, part or info
 * A rule is composed by a set of continions that all of them have to met.
 * Each condition can be null (not taken into account) or a RE 
 * In a rule there is a single condition for each kind of linguistic information
 *  
 * A  rule can include a single dependency type, and then a set of POS, these are expressed
 * as a Regular expression, like *, (JJ)|(NC) , N*, [ANJ]* 
 * 
 * Or rule can also include elments of Lemma or morpho analysis in the same way.
 *
 * The rules have a context, a context can be inside a Chunk, outside it *
 * All the rules are applied in by order, only one rule is fired every time  
 * A rule can indicate that nothing has to be done, meaning that the process stops there for that token
 * 
 * The tokens are processed folowing the dependency tree, and the sons of each node are processed 
 * from left to rignt..
 * 
 * Each rule is composed of the following components
 * 
 * Place where it applies: everywhere, beforeNucleos, AfterNucleus,Info, 
 * Current token information (Word, lemma, POS, Depen)
 * Previous token in the sequence (POS, Depen)
 * Next token in the sequence (POS, Depen)
 * Action to perform: New, Append (when appended, all the subtree is appended)
 * 
 */
public class ChunkerRule {


	public String Lemma;
	public String Token;
	public String POS;
	public String Morpho;
	public String Depen;
	
	public String Prev_lemma;
	public String Prev_POS;
	public String Prev_Depen;
	
	public String Next_lemma;
	public String Next_POS;
	public String Next_Depen;
	public ElemType Type;
	public AppliesType Applies;
	public static final int RULESIZE = 13;
	/**
	 * 
	 * @param S Tab separated string containing the rule in a conll format.
	 * dash means no rule, 
	 */
	public ChunkerRule(String S) throws IllegalArgumentException {
		String[] params=S.split("\t");
		if (params.length != RULESIZE) throw new IllegalArgumentException("The number of elements in a rule should be " + RULESIZE + " when reading \n"+S);
		int i=0;
		for (i=0;i<RULESIZE;i++) params[i]=params[i].trim();
		i=0;
        if (params[i].contentEquals("*")) Token=null; 		else Token      =params[i]; i++;
        if (params[i].contentEquals("*")) Lemma=null; 		else Lemma      =params[i]; i++;
        if (params[i].contentEquals("*")) POS=null; 		else POS        =params[i]; i++;
        if (params[i].contentEquals("*")) Morpho=null; 		else Morpho     =params[i]; i++;
        if (params[i].contentEquals("*")) Depen=null; 		else Depen      =params[i]; i++;
        if (params[i].contentEquals("*")) Prev_lemma=null; 	else Prev_lemma =params[i]; i++;
        if (params[i].contentEquals("*")) Prev_POS=null; 	else Prev_POS   =params[i]; i++;
        if (params[i].contentEquals("*")) Prev_Depen=null; 	else Prev_Depen =params[i]; i++;
        if (params[i].contentEquals("*")) Next_lemma=null; 	else Next_lemma =params[i]; i++;
        if (params[i].contentEquals("*")) Next_POS=null; 	else Next_POS   =params[i]; i++;
        if (params[i].contentEquals("*")) Next_Depen=null; 	else Next_Depen =params[i]; i++;
        // read teh type
        try {
        	this.Type= ElemType.valueOf( params[i].toUpperCase());i++;
        }
        catch (IllegalArgumentException ex){
        	 throw new IllegalArgumentException("The element  "+ params[i] +" does not belong to the enum Type" );
        }
        try {
        	this.Applies=AppliesType.valueOf( params[i].toUpperCase());i++;
        }
        catch (IllegalArgumentException ex){
        	 throw new IllegalArgumentException("The element  "+ params[i] +" does not belong to the enum Applies" );
        }
      

	
	}
	
    public Boolean matches(String ref,String value){
    	if (ref==null ) return true;
    	return value.matches(ref);		
    }
	
}
	
	
	private LinkedList<ChunkerNode> chunks;
    private LinkedList<ChunkerRule> rules;

	public ChunkerExtractor(String fileRules) {

		rules=new  LinkedList<ChunkerRule>();
		InputStream    fis;
		BufferedReader br;
		String         line;

		try {
			fis = new FileInputStream(fileRules);
		br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
		while ((line = br.readLine()) != null) {
		    if (!line.startsWith("#"))
			  rules.add(new ChunkerRule(line));
		}

		// Done with the file
		br.close();
/* Old ruleset	
 * 	// rules.add(new ChunkerRule("escriure la regla"));
 
		// rules.add(new ChunkerRule("Token\tLema\tPos\t\t\t\t\t"));
     // rules.add(new ChunkerRule("Token \t Lema \t Pos \t Morpho \t Depen \t P_lemma \t P_pos \t P_Depen \t N_lemma \t N_pos \t N_Dp \t type \t applies"));		
		rules.add(new ChunkerRule("*     \t*     \t(NN|NNS|NNP)\t* \t  NMOD\t*       \t *     \t *       \t *       \t (NN|NNS) \t *    \t PART \t BEFORE"));
		rules.add(new ChunkerRule("*     \t*     \t(NN|NNS|NNP)\t* \t  NMOD\t*       \t *     \t *       \t *       \t *     \t *    \t PART \t BEFORE"));				
		rules.add(new ChunkerRule("*     \t*     \t(NN|NNS|PRP)\t* \t (SBJ|OBJ) \t*  \t *     \t *       \t *       \t *     \t *   \t HEAD_VERB \t BEFORE"));		
		rules.add(new ChunkerRule("*     \t*     \t (NNP|NN|NNS)\t* \t*     \t *       \t *     \t *       \t *       \t *     \t *    \t HEAD \t EVERYWHERE "));
		rules.add(new ChunkerRule("*     \t*\t(JJ|VBG|VBN|NNP|NNPS|CD)\t*\tNMOD\t*       \t *     \t *       \t *       \t *     \t *    \t PART \t BEFORE"));
//to take care of said when wrongly tagged
		rules.add(new ChunkerRule("*     \t*\t(VBD)\t*\tNMOD\t*       \t *     \t *       \t *       \t *     \t *    \t PART \t BEFORE"));
		rules.add(new ChunkerRule("*     \t a|an \t DT  \t  *     \t NMOD  \t*        \t *     \t *       \t *       \t *     \t *    \t IDET \t BEFORE"));
		rules.add(new ChunkerRule("*     \t *    \t DT  \t  *     \t NMOD  \t*        \t *     \t *       \t *       \t *     \t *    \t DET \t BEFORE"));
		rules.add(new ChunkerRule("*     \t *    \t(CD|LS)\t  *   \t (PRN|APPO|NMOD)\t *     \t *     \t *       \t *       \t *     \t *    \t INFO \t AFTER"));
		rules.add(new ChunkerRule("*     \t wherein\tWRB \t  *     \t *     \t *       \t *     \t *       \t *       \t *     \t *    \t IHEAD \t DAFTER"));
		rules.add(new ChunkerRule("*     \t which \tWDT \t  *     \t *     \t *       \t *     \t *       \t *       \t *     \t *    \t IHEAD \t DAFTER"));
		rules.add(new ChunkerRule("*     \t wherein \tWRB \t  *     \t *     \t ,       \t *     \t *       \t *       \t *     \t *    \t PART  \t AFTER"));
		rules.add(new ChunkerRule("*     \t which \t WDT \t  *     \t *     \t ,       \t *     \t *       \t *       \t *     \t *    \t PART \t AFTER"));

		rules.add(new ChunkerRule("*     \t wherein\tWRB \t  *     \t *     \t ,       \t *     \t *       \t *       \t *     \t *    \t HEAD  \t EVERYWHERE"));
		rules.add(new ChunkerRule("*     \t which \t WDT \t  *     \t *     \t ,       \t *     \t *       \t *       \t *     \t *    \t HEAD \t EVERYWHERE"));
		
		// this rule is not correct ..... 
//		rules.add(new ChunkerRule("*     \t *    \t IN  \t  *     \t(NMOD|PMOD)\t*    \t *     \t *       \t *       \t *    \t  *    \t HEAD \t DAFTER"));
		rules.add(new ChunkerRule("*     \t *    \t IN  \t  *     \t(NMOD|PMOD)\t*    \t *     \t *       \t *       \t *   \t * \t PART \t AFTER"));
		rules.add(new ChunkerRule("*     \t *    \t IN  \t  *     \t(ADV|OBJ)\t*      \t *     \t *       \t *       \t NN    \t PMOD \t PART \t AFTER"));
		rules.add(new ChunkerRule("*     \t *    \t IN  \t  *     \tPRP    \t *       \t *     \t *       \t *       \t VBG   \t PMOD \t PART \t AFTER"));
  		rules.add(new ChunkerRule("*     \t *    \t(VBG|VBD)\t*   \tAPPO   \t *   \t *     \t *       \t *       \t *     \t *    \t PART \t AFTER"));
  	// joan "a system capable of... ducts located in the hull" 
  		rules.add(new ChunkerRule("*     \t *    \tJJ \t*   \tAPPO       \t [^,]*   \t *     \t *       \t *       \t *     \t *    \t PART \t AFTER"));
  		// joan "a sail batten" 
  		rules.add(new ChunkerRule("*     \t *    \tVBN \t*   \tAPPO       \t [^,]*   \t *     \t *       \t *       \t *     \t *    \t PART \t AFTER"));
// joan wind heeling load (from 0be18)
  		rules.add(new ChunkerRule("*     \t *    \t(VBG|VBD|VBN)\t* \tAPPO \t *      \t *     \t *       \t *       \t *     \t *    \t PART \t BEFORE"));
// a CD is only the head of chunk if then t contains a NN as NMOD or PMOD
		rules.add(new ChunkerRule("*     \t*     \t CD    \t* \t*     \t *       \t *     \t *       \t *       \t *     \t *    \t NUMHEAD \t EVERYWHERE "));

  		// this is a fallback option but dangerous
 // 		rules.add(new ChunkerRule("*     \t*     \t (VBG)   \t* \t*     \t *       \t *     \t *       \t *       \t *     \t *    \t VERB \t EVERYWHERE "));
// 		rules.add(new ChunkerRule("*     \t *    \t *   \t  *     \t *     \t *       \t *     \t *       \t *       \t *     \t *    \t *    \t   *"));
//		rules.add(new ChunkerRule("*     \t *    \t *   \t  *     \t *     \t *       \t *     \t *       \t *       \t *     \t *    \t *    \t   *"));
//		rules.add(new ChunkerRule("*     \t *    \t *   \t  *     \t *     \t *       \t *     \t *       \t *       \t *     \t *    \t *    \t   *"));
*/
		
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("error while reading the rules chunker file");
		}
			
	}

    public LinkedList<ChunkerNode> extractChunks(TokenInfo[] sortedTokens)
            throws Exception {
        
        this.chunks = new LinkedList<ChunkerNode>();
        if (sortedTokens.length < 2) {
            return chunks;
        }
        // convert SentenceData into a tree
        LinkedList<Integer>[] tree = generateTree(sortedTokens);
        // traverse the tree.
        // if (tree[0].size()!=1 )
        // System.out.println("sentence wiht more than one root "+
        // tree[0].size());
        for (int roots = 0; roots < tree[0].size(); roots++) {
            int root = tree[0].get(roots);
            ChunkerNode RootNode = new ChunkerNode(root, sortedTokens[root],
                    sortedTokens[root].getForm(),
                    sortedTokens[root].getLemma(), sortedTokens[root].getPOS(),
                    "", // TODO get morpho, is not used.. so
                    sortedTokens[root].getDeprel());

            extractNodeChunks(tree, sortedTokens, 0, true, false, null, RootNode, null, null);
        }
        return chunks;
        /*
         * 
         * ListIterator<ChunkerNode> nodeItr = this.chunks.listIterator(); while
         * (nodeItr.hasNext()) { nodeItr.next().writeChunks(annotations); }
         */
    }

	public String toString(){
		StringBuffer result=new StringBuffer();
		ListIterator<ChunkerNode> nodeItr = this.chunks.listIterator();
		while (nodeItr.hasNext()) {
			result.append(nodeItr.next().toString());
		}		
		return result.toString();
	}
	/** it checks rule by rule if the current chunk satisfies some of the rules if so then it returns the type
	 * @param Prev The previous node
	 * @param Curr The current node
	 * @param Next  The next node
	 * @return the type of the current node, if no rule is satisfied then returns EXCLUDE 
	 */
	private ElemType CheckRules(ChunkerNode Prev,ChunkerNode Curr,ChunkerNode Next,Boolean Previous,Boolean definite){
		ListIterator<ChunkerRule> nodeItr = this.rules.listIterator();
		while (nodeItr.hasNext()) {
			ChunkerRule R=nodeItr.next();
			if ((R.Applies == AppliesType.BEFORE ) && (! Previous)) continue;
			if ((R.Applies == AppliesType.AFTER ) && ( Previous)) continue;
			if ((R.Applies == AppliesType.DAFTER ) && ( ( Previous) || !definite) ) continue;
			if (! R.matches(R.Token,Curr.getToken())) continue;
			if (! R.matches(R.Lemma,Curr.getLemma())) continue;
			if (! R.matches(R.Morpho,Curr.getMorpho())) continue;
			if (! R.matches(R.POS,Curr.getPOS())) continue;
			if (! R.matches(R.Depen,Curr.getDep())) continue;
			if (Next==null) {
					if (R.Next_lemma != null) continue; 
					if (R.Next_POS != null) continue; 
					if (R.Next_Depen != null) continue; 
			} else {
				if (! R.matches(R.Next_lemma,Next.getLemma())) continue;
				if (! R.matches(R.Next_POS,Next.getPOS())) continue;			
				if (! R.matches(R.Next_Depen,Next.getDep())) continue;
			}
			if (Prev==null) {
				// the !R.matches is for these cases where the match is "not containing...".
				if (R.Prev_lemma != null && ! R.matches(R.Prev_lemma,"") ) continue; 
				if (R.Prev_POS != null && ! R.matches(R.Prev_POS,"") ) continue; 
				if (R.Prev_Depen != null && ! R.matches(R.Prev_Depen,"") ) continue; 
			} else {
				if (! R.matches(R.Prev_lemma,Prev.getLemma())) continue;
				if (! R.matches(R.Prev_POS,Prev.getPOS())) continue;			
				if (! R.matches(R.Prev_Depen,Prev.getDep())) continue;
			}			
			// then it matches the rule
			return R.Type;
		}
		return ElemType.EXCLUDE;
	}
	/**
	 * This function returns the text under the node, and if there are chunks
	 * then the list of chunks that has been collecting
	 * 
	 * @param Node
	 *            the current node
	 * @param tree
	 *            the full tree
	 * @param instance
	 *            The full sentence
	 * @return returns a linked lists of texts, the first text is the text under
	 *         the node, the remaining list are the chunks under that node.
	 * @throws Exception
	 */
	public ChunkerSubNode extractNodeChunks(LinkedList<Integer>[] tree,
			TokenInfo[] instance, int level, Boolean Pos,Boolean Definite,ChunkerNode Prev,ChunkerNode Curr,ChunkerNode Next,ElemType Head ) throws Exception {
		if (level>instance.length) throw new Exception("sentence has a cycle");
		// get the list of subnodes
		int Node=Curr.getNodeNumber();
		Boolean subnumHead=false; // indicates that is a part of a numhead (like an of) and propagates the numhead property.
		ListIterator<Integer> nodeItr = tree[Node].listIterator();
		StringBuffer text = new StringBuffer("");
		StringBuffer ltext = new StringBuffer("");
		
		ElemType nodeType=CheckRules(Prev,Curr,Next,Pos,Definite);
		String currentStr=instance[Node].getForm();
		String currentLemma= instance[Node].getLemma();

	    ChunkerNode current = new ChunkerNode(Node,instance[Node],currentStr,currentLemma,				
				instance[Node].getPOS(),
				"", //TODO get morpho, is not used.. so 
				instance[Node].getDeprel());
		// if its VERB or NUMHEAD, we create the chunk ... but we don't add it, and  we wait for the HEAD_VERB 
		ChunkerSubNode currentNode=new ChunkerSubNode(nodeType,
				instance[Node].getDeprel(),				
				currentStr,currentLemma,
				Node,Node,current,
				" ("+instance[Node].getDeprel()+" "+ currentStr+") ");
		
	    Boolean isChunk =  (nodeType== ElemType.HEAD||  nodeType== ElemType.HEAD_PART   || nodeType== ElemType.HEAD_VERB );
	    if (Head == ElemType.NUMHEAD 
	    		&& nodeType== ElemType.PART && instance[Node].getPOS().contentEquals("IN") ){ 
	    	// numhead type part and is IN or before... to distinguish between the first of the batteries and the batt 600
	    	isChunk =true;
	    	nodeType= ElemType.NUMHEAD;
	    	subnumHead=true;
	    } else if (Head == ElemType.NUMHEAD 
	    		&& nodeType== ElemType.PART && 	Pos ){ // numhead to distinguish between the first of the batteries and the batt 600
	    	isChunk =true;
	    	// nodeType= ElemType.NUMHEAD; do not propagate
	    	subnumHead=true;
	    } 
	    
	    else if  ( nodeType== ElemType.NUMHEAD){
    		isChunk=true;
    		// but it has not been added
    		 //System.out.println(" -- NUMHEAD...  " + (String) instance[Node].getFeatures().get("string"));
    	} else if (isChunk) {
    		if(Head != ElemType.NUMHEAD) chunks.add(current);
    		// it adds current node to the list of chunks but content is still pending
		}

		
		Boolean Previous = true;
		ChunkerNode PrevNode=null; // the next node in the senquence
		ChunkerNode ActNode=null;
		ChunkerNode NextNode=null;
	    if (!nodeItr.hasNext()) {
			// if the node does not have children, then is a single word,
			// return the single word
			// but it could be a chunk
			return currentNode;
		} else  { //		if (nodeItr.hasNext()) prepare the iteration, 
				// set next node to first node, so in the iteration, next will be actual node and
			    // prev will be null, and will start the iteration
			int N_Node= (int) (nodeItr.next());
			NextNode=new ChunkerNode(N_Node,instance[N_Node],
					instance[N_Node].getForm(),
					instance[N_Node].getLemma(),
					instance[N_Node].getPOS(),
					"", //TODO get morpho, is not used.. so 
					instance[N_Node].getDeprel());
		}
		
        //  for each sub node
		while (nodeItr.hasNext() || NextNode!=null) {
			PrevNode=ActNode;
			ActNode=NextNode;	
			if (nodeItr.hasNext()){//set the next node
				int N_Node = (int) (nodeItr.next());
				NextNode=new ChunkerNode(N_Node,instance[N_Node],
						instance[N_Node].getForm(),
						instance[N_Node].getLemma(),
						instance[N_Node].getPOS(),
						"", //TODO get morpho, is not used.. so 
						instance[N_Node].getDeprel());
			} else {
				NextNode=null;
			}
			// gets the subnode (which can be a full sub tree)		
			int cNode=ActNode.getNodeNumber();
			// if child node is non projective: when its node number is higher than the grandfather. 
			ChunkerSubNode subNodeCnk = extractNodeChunks(tree, instance, level+1,(cNode < Node),! current.getIndefinite(), PrevNode,ActNode,NextNode,nodeType);
			// depending on the child we decide what to do with the info obtained
			// if it's info, we just add it as info.
			if ( level>0) {
			
				 int father=instance[cNode].getHead();
				 int grandFather=instance[father].getHead();
			     //  System.out.println(level+" this is "+cNode+" "+ActNode.getToken()+ " father  "+currentStr+" "+father + "  grandpa "+ grandFather);
				 if ( (cNode <  grandFather) &&  (grandFather < father)) continue;
				 if ( (cNode >  grandFather) &&  (grandFather > father)) continue;
					
			}
			// if it's info it's added as info
			if (isChunk && subNodeCnk.rulesType == ElemType.INFO){
				current.addExt(subNodeCnk);
				continue;
			}

			// strange cases with numhead where head and part are reversed...
			if ((subNodeCnk.rulesType == ElemType.HEAD_PART ||subNodeCnk.rulesType == ElemType.HEAD ||subNodeCnk.rulesType == ElemType.PART)  && nodeType== ElemType.NUMHEAD){
				// the head is a number, but is a wrong one....
				// they must be interchanged....
				// so the current head 
				/* 
				 * FeatureMap chunkMap = Factory.newFeatureMap();
				 annotationsCopy.add(instance[Node].getStartNode().getOffset(), 
						 instance[Node].getEndNode().getOffset(),
					"Number",chunkMap);   
				*/
				//System.out.println( subNodeCnk.chunk.toString() );
				subNodeCnk.integrateInfo(currentNode);
				// System.out.println( subNodeCnk.chunk.toString() );
				current=subNodeCnk.chunk;
				currentNode=subNodeCnk;
				if (!subnumHead ) chunks.add(current);
				isChunk=true;
				text=new StringBuffer(currentNode.content);
				ltext=new StringBuffer(currentNode.lcontent);
		    	//if ( nodeType== ElemType.NUMHEAD)  System.out.println(" -- NUMHEAD...Member  " );
		    	nodeType= ElemType.HEAD;
		    	continue;
			}
			if (subNodeCnk.rulesType == ElemType.HEAD_PART
					|| subNodeCnk.rulesType == ElemType.PART) {
				if (Previous) {
					current.addPrev(subNodeCnk);
				} else {
					current.addNext(subNodeCnk);
				}
				continue;
			}
		   // When the node is a chunk some dependent elemEnts are special like determinants and numbering.
			if (isChunk) {
			     if (subNodeCnk.rulesType == ElemType.IDET){
					    //	System.out.println(" -- IDET... inserted" );
						current.addDET(subNodeCnk);
						current.setIndefinite(true);
				}else if (subNodeCnk.rulesType == ElemType.INUM) {
				    //	System.out.println(" -- IDET... inserted" );
					current.addPrev(subNodeCnk);
					current.setIndefinite(true); 
				} else if (subNodeCnk.rulesType == ElemType.DET){
					    //	System.out.println(" -- DET... inserted" );
						current.addDET(subNodeCnk);
						current.setIndefinite(false);
				}else if (subNodeCnk.rulesType == ElemType.INFO){
					current.addExt(subNodeCnk);
				}else if (subNodeCnk.rulesType == ElemType.HEAD_VERB && nodeType== ElemType.VERB){
					// the head is a verb, but is a wrong one....
					// they must be interchanged....
					// so the current head 				
					ChunkerNode NewCurrent=subNodeCnk.chunk;
					NewCurrent.addNext(currentNode);
					current=NewCurrent;
				} else if (subNodeCnk.rulesType == ElemType.PENDING_PART){
					;
					if (Previous) {
						current.addPrevAll(subNodeCnk.chunk.getNextNodes());
						current.addPrevAll(subNodeCnk.chunk.getPrevNodes());
						
					} else {
						current.addNextAll(subNodeCnk.chunk.getNextNodes());
						current.addNextAll(subNodeCnk.chunk.getPrevNodes());
					}
				}		

	
			}

		}

		return currentNode;
	}

	public LinkedList<Integer>[] generateTree(TokenInfo[] instance) {
		LinkedList<Integer>[] tree = new LinkedList[instance.length];
		for (int i = 0; i < instance.length; i++) {
			tree[i] = new LinkedList<Integer>();
		}
		// TODO remember 0 is root 
		for (int i = 1; i < instance.length; i++) {
			tree[(Integer) instance[i].getHead()].addLast(new Integer(i));
			// System.out.print(instance[i].getForm() + "#" + instance[i].getPOS()+" ");
		}
		System.out.println();
		return tree;
	}
}
