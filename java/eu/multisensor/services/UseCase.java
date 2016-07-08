package eu.multisensor.services;

import java.util.HashMap;
import java.util.Map;


public class UseCase {
	private static  String SOLR_URL = "http://ipatdoc.taln.upf.edu:8080/multisensor/use_cases_";
	//private static  String SOLR_URL = "http://10.80.29.5:8080/multisensor/use_cases_";

	private String name;
	private String lang;	
	private String id;
	private String url;
	private String filterQuery;
	public UseCase(String name,String lang, String id,String filterQuery) {
		this.name = name;
		this.lang=lang;
		this.id=id;
		this.url = SOLR_URL+lang;
		this.filterQuery = filterQuery;
	}

	public String getUrl() {
		return url;
	}
	


	public String getLang() {
		return lang;
	}


	public String getId() {
		return id;
	}



	private static HashMap<String, UseCase> useCases= new HashMap<String,UseCase>(){ 
		{
			put("UC1_2_en",new UseCase("UC1-2 1- Energy Policy",
					"en",
					"UC1_2",
					"useCase:UC1_2"));
			put("UC1_en",new UseCase("UC1 2- Household Appliances",
					"en",
					"UC1",
					"useCase:UC1"));
			put("UC2_en",new UseCase("UC2 3- Yoghurt industry",
					"en",
					"UC2",
					"useCase:UC2"));
			put("ref_en",new UseCase("reference corpus",
					"en",
					"ref",
					"useCase:ref"));
			put("UC1_2_fr",new UseCase("UC1-2 - Energy Policy",
					"fr",
					"UC1_2",
					"useCase:UC1_2"));
			put("UC1_fr",new UseCase("UC1 - Household Appliances",
					"fr",
					"UC1",
					"useCase:UC1"));
			put("UC2_fr",new UseCase("UC2 - Yoghurt industry",
					"fr",
					"UC2",
					"useCase:UC2"));
			put("ref_fr",new UseCase("reference corpus",
					"fr",
					"ref",
					"useCase:ref"));
			put("UC1_2_es",new UseCase("UC1-2 - Energy Policy",
					"es",
					"UC1_2",
					"useCase:UC1_2"));
			put("UC1_es",new UseCase("UC1 - Household Appliances",
					"es",
					"UC1",
					"useCase:UC1"));
			put("UC2_es",new UseCase("UC2 - Yoghurt industry",
					"es",
					"UC2",
					"useCase:UC2"));
			put("ref_es",new UseCase("reference corpus",
					"es",
					"ref",
					"useCase:ref"));
			put("UC1_2_de",new UseCase("UC1-2 - Energy Policy",
					"de",
					"UC1_2",
					"useCase:UC1_2"));
			put("UC1_de",new UseCase("UC1 - Household Appliances",
					"de",
					"UC1",
					"useCase:UC1"));
			put("UC2_de",new UseCase("UC2 - Yoghurt industry",
					"de",
					"UC2",
					"useCase:UC2"));
			put("ref_de",new UseCase("reference corpus",
					"de",
					"ref",
					"useCase:ref"));
		}
	};
	
	public static UseCase getUseCase(String lang, String caseName){
		return useCases.get(caseName+"_"+lang);
	}
}