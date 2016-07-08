package eu.multisensor.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class LinguatecNERServiceClient 
{
    private static final String nerServiceURL = "http://services.linguatec.org/rest/ner/recognize";
    
    public static String sendText(String inText, String inLanguage) throws ClientProtocolException, IOException
    {
        return sendText(inText, inLanguage, "http://example.org/");
    }
    
    public static String sendText(String inText, String inLanguage, String document_url) throws ClientProtocolException, IOException
    {
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(nerServiceURL);
        
        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        urlParameters.add(new BasicNameValuePair("language", inLanguage));
        urlParameters.add(new BasicNameValuePair("text", inText));
        urlParameters.add(new BasicNameValuePair("document_url", document_url));
        
        post.setEntity(new UrlEncodedFormEntity(urlParameters));
        HttpResponse response = client.execute(post);
        HttpEntity entity = response.getEntity();
        
        return EntityUtils.toString(entity);
    }
}
