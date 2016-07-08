package eu.multisensor.services;


import edu.upf.taln.nif.NIFWrapper;
import eu.multisensor.utils.BasicOpenNLPChunker.ChunkAnnotation;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import vocabularies.NIF;

import java.util.Collection;

/**
 * A wrapper around an RDF model containing NIF annotations.
 * 
 * @author Gerard Casamayor
 */
public class ConceptExtractionRDF extends NIFWrapper
{
	private static final URI taIdentRef = factory.createURI("http://www.w3.org/2005/11/its/rdf#taIdentRef");
	private static final URI taClassRef = factory.createURI("http://www.w3.org/2005/11/its/rdf#taClassRef");
	private static final URI specificConcept = factory.createURI("http://data.multisensorproject.eu/ontology#SpecificConcept");
	private static final URI genericConcept = factory.createURI("http://data.multisensorproject.eu/ontology#GenericConcept");
	private static final URI annotator = factory.createURI("http://data.multisensorproject.eu/");
	private static final Log log = LogFactory.getLog(ConceptExtractionRDF.class);

	public ConceptExtractionRDF(String inSerializedRDFModel, String inText) 
	{
		super(inSerializedRDFModel, inText);
	}	
	
	/**
	 * Extends an existing RDF model (containing NIF annotations of tokens, sentences and entities) with annotations of 
	 * additional entities and concepts disambiguated against BabelNet.
	 * @param inConceptAnnotations
	 */
	public void updateRDFModel(Collection<ChunkAnnotation> inConceptAnnotations)
	{
		for (ChunkAnnotation annotation : inConceptAnnotations)
		{
			Pair<Integer, Integer> offsets = Pair.of(annotation.start, annotation.end);
			// Find matching nif:Word or nif:Phrase annotation, or create a new one
			Resource nifAnnotation = null;
			if (this.nifWordAndPhraseAnnotations.containsKey(offsets))
			{
				nifAnnotation = this.nifWordAndPhraseAnnotations.get(offsets);
			}
			else
			{
				// If no Nif annotation exists, then the entity MUST be a multiword. Otherwise a word annotation would exist
				nifAnnotation = this.createAnnotation(annotation.start, annotation.end, annotation.chunk.getText(), NIF.CLASS_PHRASE);
			}

			if (this.model.contains(nifAnnotation, taClassRef, null))
			{
				// The word or phrase has a taClassRef statement already, so create a new class Annotation unit
				URI conceptAnnotationURI = factory.createURI(nifAnnotation.stringValue() + "-annot-concept");
				this.addStatement(nifAnnotation, NIF.OBJECT_PROPERTY_ANNOTATION_UNIT, conceptAnnotationURI);
				this.addStatement(conceptAnnotationURI, RDF.TYPE, NIF.CLASS_ANNOTATION_UNIT);
				this.addStatement(conceptAnnotationURI, taClassRef, specificConcept);
				this.addStatement(conceptAnnotationURI, NIF.OBJECT_PROPERTY_PROVENANCE, annotator); // using nif-ann generic property
				this.addStatement(conceptAnnotationURI, NIF.DATA_PROPERTY_CONFIDENCE, factory.createLiteral(annotation.score)); // using nif-ann generic property
			}
			else
			{
				// The word or phrase has no taClassRef statement, so add statement directly to it
				this.addStatement(nifAnnotation, taClassRef, specificConcept);
				this.addStatement(nifAnnotation, NIF.OBJECT_PROPERTY_TACLASSREF_PROVENANCE, annotator); // using nif-ann companion property for taClassRef
				this.addStatement(nifAnnotation, NIF.DATA_PROPERTY_TACLASSREF_CONFIDENCE, factory.createLiteral(annotation.score)); // using nif-ann companion property for taClassRef
			}
		}
	}
}
