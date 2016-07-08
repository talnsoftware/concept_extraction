package eu.multisensor.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;

public class SolrUtils {

    public static class TfIdf {

        private String term;
        private Double score;
        private Integer termFrequency;
        private Integer documentFrequency;

        public TfIdf(String term, Double score, Integer tf, Integer df) {
            this.setTerm(term);
            this.setScore(score);
            this.setTermFrequency(tf);
            this.setDocumentFrequency(df);
        }

        public TfIdf(String term, int occ, int docsWithTerm, int totDocTerms, int totDocs) {
            this.setTerm(term);
            this.setScore(getValue(occ, docsWithTerm, totDocTerms, totDocs));
            this.setTermFrequency(occ);
            this.setDocumentFrequency(docsWithTerm);
        }
        
        public Double getValue(Number occ, Number docsWithTerms, Number totDocTerms, Number totDocs){
            double tf = occ.floatValue() / (Float.MIN_VALUE + totDocTerms.floatValue());
            double idf = Math.log10(totDocs.floatValue() / (Float.MIN_VALUE + docsWithTerms.floatValue()));
            return (tf * idf);
        }

        public String getTerm() {
            return term;
        }

        public void setTerm(String term) {
            this.term = term;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }

        public Integer getTermFrequency() {
            return termFrequency;
        }

        public void setTermFrequency(Integer tf) {
            this.termFrequency = tf;
        }

        public Integer getDocumentFrequency() {
            return documentFrequency;
        }

        public void setDocumentFrequency(Integer df) {
            this.documentFrequency = df;
        }

    }

    public static class AnalysisInfo {
        public StringBuilder tokens = new StringBuilder();
        public StringBuilder lemmas = new StringBuilder();
        List<String> bnIds = new ArrayList<String>();
        List<String> vnClasses = new ArrayList<String>();
        ArrayList<String> frames = new ArrayList<String>();
        ArrayList<String> lemmaFrames = new ArrayList<String>();
        ArrayList<String> lemmaDep = new ArrayList<String>();
    }

    public static final String SOLR_URL = "http://10.80.27.67:8080/solr/multisensor_ha";
    
    /*
     * http://localhost:8080/solr/multisensor_press/tvrh?q=bnId:%22bn:00046926n%22&wt=json&indent=true&tv.all=true&fl=bnId
     */
    
    public static SolrClient getClient() {
        return new HttpSolrClient(SOLR_URL);
    }

    public static boolean solrExistsDoc(SolrClient solrCli, String docId) throws IOException, SolrServerException {
        
        SolrQuery query = new SolrQuery("id:" + docId).setRows(0);
        long count = solrCli.query(query).getResults().getNumFound();
        return count > 0;
    }

    public static List<Count> solrGetIDF(SolrClient solrCli, String field) throws IOException, SolrServerException {
        
        SolrQuery query = new SolrQuery("*:*").setRows(0);
        query.addFacetField(field);
        
        QueryResponse response = solrCli.query(query);
        return response.getFacetField(field).getValues();
    }

    public static Map<String, TfIdf> solrGetTF_IDF(SolrClient solrCli, String docId, String field) throws IOException, SolrServerException {
        
        SolrQuery query = new SolrQuery("id:" + docId);
        query.setRequestHandler("/tvrh");
        query.setParam("tv.tf_idf", true);
        query.setParam("tv.tf", true);
        query.setParam("tv.df", true);
        
        QueryResponse response = solrCli.query(query);
        NamedList<Object> solrResponse = response.getResponse();
        
        Iterator<Entry<String, Object>> termVectors =  ((NamedList) solrResponse.get("termVectors")).iterator();
        
        Map<String, TfIdf> tfidfMap = new HashMap<String, TfIdf>();
        while(termVectors.hasNext()){
            Entry<String, Object> docTermVector = termVectors.next();
            for(Iterator<Entry<String, Object>> fi = ((NamedList)docTermVector.getValue()).iterator(); fi.hasNext(); ){
                Entry<String, Object> fieldEntry = fi.next();
                if(fieldEntry.getKey().equals(field)){
                    for(Iterator<Entry<String, Object>> tvInfoIt = ((NamedList)fieldEntry.getValue()).iterator(); tvInfoIt.hasNext(); ){
                        Entry<String, Object> tvInfo = tvInfoIt.next();
                        NamedList tv = (NamedList) tvInfo.getValue();
                        
                        TfIdf score = new TfIdf(tvInfo.getKey(), (Double) tv.get("tf-idf"), (Integer) tv.get("tf"), (Integer) tv.get("df"));
                        tfidfMap.put(tvInfo.getKey(), score);
                        
                        //System.out.println("Vector Info: " + tvInfo.getKey() + " tf: " + tv.get("tf") + " df: " + tv.get("df") + " tf-idf: " + tv.get("tf-idf"));
                    }
                }       
            }
        }
        
        return tfidfMap;
    }
    
    public static void add2Solr(SolrClient solrCli, String docId, AnalysisInfo info) throws IOException, SolrServerException {
        
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("id", docId);
        
        doc.addField("token", info.tokens);
        doc.addField("lemma", info.lemmas);
        doc.addField("bnId", info.bnIds);
        doc.addField("vnClass", info.vnClasses);

        solrCli.add(doc);
        solrCli.commit();
    }

    public static void removeDoc(SolrClient solrCli, String docId) throws SolrServerException, IOException {
        solrCli.deleteByQuery("id:"+docId);
        solrCli.commit();
    }
}
