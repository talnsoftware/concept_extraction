package eu.multisensor.chunking;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;

import eu.multisensor.services.UseCaseEnum;

public class UseCaseSolrClient {

    String USE_CASE_FIELD = "useCase";
    
    String USE_CASE_REFERENCE_KEY = "ref";
    // Joan seems to have mismatched the keys and it's fixed here
    String USE_CASE_UC1_1_KEY = "UC1_2";
    String USE_CASE_UC1_2_KEY = "UC1";
    String USE_CASE_UC2_KEY = "UC2";
    
    public String url;

    HttpSolrClient solrCli;

    private String useCaseId;
    
    public UseCaseSolrClient(String url, UseCaseEnum useCase) {

        this.url = url;
        this.solrCli = new HttpSolrClient(url);
        
        switch(useCase) {
            case UC1_1:
                this.useCaseId = USE_CASE_UC1_1_KEY;
                break;
    
            case UC1_2:
                this.useCaseId = USE_CASE_UC1_2_KEY;
                break;
    
            case UC2:
                this.useCaseId = USE_CASE_UC2_KEY;
                break;
    
            default:
                // TODO: throw exception?
                useCaseId = null;
                break;
        }
    }

    public void save(ArrayList<TermInfo> termList, String docId) throws Exception {
        
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("id", docId);  
        
        String terms = "";
        String lterms = "";
        for (TermInfo termInfo : termList){
            
            Set<Chunk> chunks = termInfo.variants;
            for(Chunk chunk : chunks){
                //System.out.println(chunk.text+"---"+chunk.textLemma+"---"+chunk.base.text+"---"+chunk.base.textLemma);
                terms += " " + chunk.base.getText().replace(" ", "_ _");
                lterms += " " + chunk.base.getTextLemma().replace("_", "_ _");
            }
            //System.out.println(terms +"\n"+lterms); return;
        }
        
        doc.addField("id", docId);          
        doc.addField("term", terms);
        doc.addField("term_lemma", lterms);
        doc.addField("useCase", useCaseId);
        
        this.solrCli.add(doc);
        this.solrCli.commit(); 
    }

    public long getReferenceTotalDocs() throws SolrServerException, IOException {
        return getReferenceFreq("*:*");
    }
    
    public long getReferenceFreq(String queryStr) throws SolrServerException, IOException {
        return getCount(USE_CASE_REFERENCE_KEY, queryStr);
    }

    public long getUseCaseTotalDocs() throws SolrServerException, IOException {
        return getUseCaseFreq("*:*");
    }
    
    public long getUseCaseFreq(String queryStr) throws SolrServerException, IOException {
        return getCount(this.useCaseId, queryStr);
    }
    
    private long getCount(String useCaseId, String queryStr) throws SolrServerException, IOException {

        SolrQuery q = new SolrQuery(queryStr);
        q.setFilterQueries(USE_CASE_FIELD + ":" + useCaseId);
        q.setRows(0); // don't actually request any data
        
        return solrCli.query(q).getResults().getNumFound();
    }

}
