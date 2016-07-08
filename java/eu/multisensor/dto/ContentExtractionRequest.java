/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.multisensor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 *  Request to extract contents from the text of a document annotated with named entities and the deep syntactic
 *  parses of its sentences. Annotations follows the NIF 2.0 ontology and are serialized in RDF/JSON.
 * @author gerard
 */
public class ContentExtractionRequest
{
	@JsonProperty
	public final  String text = "";	
}
