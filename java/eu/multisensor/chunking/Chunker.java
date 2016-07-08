/*
 *  
 *   TALN UPF- Joan Codina
 *   Chunker based on dependency 
 */

package eu.multisensor.chunking;

import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.upf.taln.CoNLL2009Reader;
import edu.upf.taln.TokenInfo;
import eu.multisensor.utils.BasicOpenNLPChunker.ChunkAnnotation;

/**
 * Joan Codina
 * This class is the implementation of a chunker based  dependency relations.
 */

public class Chunker {

	private final static Logger log = LoggerFactory.getLogger(Chunker.class);

	private ChunkerExtractor chunker = null;

    public Chunker(String rulesPath) {
             this.chunker = new ChunkerExtractor(rulesPath);
    }

    public List<ChunkAnnotation> annotateSentence(TokenInfo[] tokens) throws Exception {
        
        List<ChunkerNode> chunkerNodes = chunker.extractChunks(tokens);

        ListIterator<ChunkerNode> nodeItr = chunkerNodes.listIterator();
        List<ChunkAnnotation> chunkAnns = new ArrayList<ChunkAnnotation>();
        while (nodeItr.hasNext()) {
            
            ChunkerNode chunkNode = nodeItr.next();
            
            Chunk chunk = chunkNode.getChunk();
            ChunkAnnotation chunkAnn = chunk.setTokensList(tokens);
            chunk.annotation = chunkAnn;
            
            chunkAnns.add(chunkAnn);
        }

        return chunkAnns;
    }
}