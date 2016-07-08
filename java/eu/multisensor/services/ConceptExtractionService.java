/**
 * Copyright (c) 2014, everis All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */
package eu.multisensor.services;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import opennlp.tools.util.InvalidFormatException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.upf.taln.TokenInfo;
import edu.upf.taln.nif.annotation.TextAnnotation;
import eu.multisensor.chunking.UseCaseSolrClient;
import eu.multisensor.dto.MSWorkBook;
import eu.multisensor.utils.BasicOpenNLPChunker.ChunkAnnotation;

/**
 *  Developed as part of WP2
 * @author roberto
 */
@Path("contentextractionservice")
public class ConceptExtractionService implements ServletContextListener
{
    final static Logger log = LoggerFactory.getLogger(ConceptExtractionService.class);

    public static Map<String, String> lang2Solr = new HashMap<String, String>();
    static {
        lang2Solr.put("en", "http://ipatdoc.taln.upf.edu:8080/multisensor/use_cases_en/");
        lang2Solr.put("es", "http://ipatdoc.taln.upf.edu:8080/multisensor/use_cases_es/");
        lang2Solr.put("fr", "http://ipatdoc.taln.upf.edu:8080/multisensor/use_cases_fr/");
        lang2Solr.put("de", "http://ipatdoc.taln.upf.edu:8080/multisensor/use_cases_de/");
    }

    static ConceptExtractor extractor;
    
    private Exception exception = null;
    
    @Override
    public void contextInitialized(ServletContextEvent ctxEvent) {
        
        try {
            init(ctxEvent.getServletContext());
            
        } catch (Exception e) {
            log.error("ConceptExtractionService could not be initialized!", e);
            exception = e;
        }
    }

    public void init(ServletContext context) throws InvalidFormatException, IOException, URISyntaxException {
        log.info("Initializing ContextExtractionService...");
        
        String basePath;
        String solr_url;
        if (context != null) {
            basePath = context.getRealPath("/WEB-INF");
            if (basePath == null) {
                throw new FileNotFoundException("WEB-INF directory not found!");
            }
            solr_url = context.getInitParameter("solr_url");
            
        } else {
            basePath = "src/main/webapp/WEB-INF";
            solr_url = "http://ipatdoc.taln.upf.edu:8080/multisensor/";
        }
        
        lang2Solr.put("en", solr_url + "use_cases_en");
        lang2Solr.put("es", solr_url + "use_cases_es");
        lang2Solr.put("fr", solr_url + "use_cases_fr");
        lang2Solr.put("de", solr_url + "use_cases_de");
        
      
        extractor = new ConceptExtractor();
        extractor.init(basePath);
        
        log.info("ContextExtractionService initialized!");
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        
    }
    
    /**
     * Method to request an extractive summary from one or more documents.
     * @param inRequest documents annotated using NIF 2.0 and in RDF/JSON format.
     * @return set of RDF triples using the ontologies of the semantic repository and serialized in RDF/JSON format.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public MSWorkBook getContents(MSWorkBook inRequest) throws Exception
    {
        log.info("Processing request for document " + inRequest.id);

        if (exception == null) {
            
            try {
                
                UseCaseEnum useCase = UseCaseEnum.fromString(inRequest.use_case);
                
                String solrUrl = lang2Solr.get(inRequest.language);
                if (solrUrl == null) {
                    throw new Exception("Language " + inRequest.language + " not supported yet...");
                }
                
                UseCaseSolrClient client = new UseCaseSolrClient(solrUrl, useCase);

                String inJsonLD = inRequest.rdf.get(0);
                DependencyParsingRDF parserRDF = new DependencyParsingRDF(inJsonLD, inRequest.body);
                /*
                String surfaceConlls = parserRDF.readParses(true, true, false, "", true);
                
                InputStream conllStream = new ByteArrayInputStream(surfaceConlls.getBytes());
                List<CoNLL2009Reader> surfaceReaders = CoNLL2009Reader.readTreeBankFromFile(conllStream);
                */
                
                TreeMap<TextAnnotation, TokenInfo[]> depInfo = parserRDF.getDependencyInfo();
                
                Set<ChunkAnnotation> conceptAnnotations;
                /*
                //if (surfaceReaders.isEmpty() || surfaceReaders.get(0).getDeprel(1).equals("_")) {
                if (false) {
                    log.info("No dependencies found!");

                    log.info("Extracting sentences using basic chunker...");
                    TreeSet<SentenceAnnotation> sentences = parserRDF.getSentencesAndTokens(false);
                    
                    conceptAnnotations = extractor.processBasic(inRequest.body, sentences, client);

                } else {
                */
                    //log.info("Dependencies found!");

                    log.info("Extracting sentences using dependencies..."+inRequest.body);
                    conceptAnnotations = extractor.processWithDeps(inRequest.body, depInfo, client);
                    log.info(conceptAnnotations.size() + " concepts found...");
                //}
                
                // Update RDF model
                ConceptExtractionRDF rdf = new ConceptExtractionRDF(inJsonLD, inRequest.body);
                rdf.updateRDFModel(conceptAnnotations);
    
                // Serialize model into JSON-LD and pack into response
                String outJsonLD = rdf.getModelAsJSONLD();
                inRequest.rdf.clear();
                inRequest.rdf.add(outJsonLD);
                
                log.info("Request for document " + inRequest.id + " successfully processed.");

            } catch (Exception e) {
                log.error("Failed to process request " + inRequest.id, e);
            }
            
        } else {
            log.error("Failed to process request " + inRequest.id + " because of initialization failure.", exception);
        }

        return inRequest;
    }

}
