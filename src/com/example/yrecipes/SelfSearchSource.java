package com.example.yrecipes;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SelfSearchSource {
    final static String URL = "http://recipes.search.yahoo.com/search";
    //final static String URL = "http://localhost:8090/s.php";
    protected String genURL;
	public Hashtable<Integer,Object> searchData = new Hashtable<Integer,Object>();
	public Hashtable<String,Object> filterData = new Hashtable<String,Object>();
	public Hashtable<String,ArrayList<String>> infilterData = new Hashtable<String,ArrayList<String>>();
	String keyword;
	int page;
	Document baseNode;
	public void getSource(String keyword, int page, Hashtable<String,ArrayList<String>> filters){
		
		this.keyword = keyword;
		this.page = page;
		infilterData = filters;
		genURL = genURL();
		try {
			baseNode = Jsoup.connect(genURL).userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/534.55.3 (KHTML, like Gecko) Version/5.1.3 Safari/534.53.10").get();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
	}
	public Hashtable<String,Object> getFilterData() {
		Hashtable<String,Object> output = new Hashtable<String,Object>();
		Hashtable<String,String> sdata = new Hashtable<String,String>();
		//String[] filters = {"holiday", "ingredient_w", "ingredient_wo", "mealtype", "collection", "rating", "totaltime", "sources"};
		sdata = this.getFilters("refiner_totaltime");
		output.put("totaltime", sdata);
		sdata = this.getFilters("refiner_ingredient_w");
		output.put("ingredient_w", sdata);
		sdata = this.getFilters("refiner_ingredient_wo");
		output.put("ingredient_wo", sdata);
		sdata = this.getFilters("refiner_mealtype");
		output.put("mealtype", sdata);
		sdata = this.getFilters("refiner_collection");
		output.put("collection", sdata);
		sdata = this.getFilters("refiner_rating");
		output.put("rating", sdata);
		sdata = this.getFilters("refiner_sources");
		output.put("sources", sdata);
		sdata = this.getFilters("refiner_totaltime");
		output.put("holiday", sdata);
		return output;
	}
	
	Hashtable<String,String> getFilters(String nodeId) {
		Hashtable<String,String> sdata = new Hashtable<String,String>();
		Elements ttFilterNodes = baseNode.select("#doc #bd-wrap #bd #sidebar .bd #" + nodeId + " .default_show");
        for (Iterator<Element> iterator = ttFilterNodes.iterator(); iterator.hasNext();) {
        	String key = "", value = "";
            Element liElement = (Element) iterator.next();
            if (liElement.getElementsByTag("a") != null && liElement.getElementsByTag("a").size() > 0) {
            	Element link = liElement.getElementsByTag("a").get(0);
            	key = link.text().toString();
            } else {
            	Element activeNode = liElement.getElementsByClass("active").get(0);
            	key = activeNode.text().toString();
            }
            if (liElement.getElementsByClass("refiner_count") != null && liElement.getElementsByClass("refiner_count").size() > 0) {
            	Element countNode = liElement.getElementsByClass("refiner_count").get(0);
            	value = countNode.text().toString().replace("(", "").replace(")","");
            }
    		sdata.put(key, value);
        }
		return sdata;
	}
	
	public Hashtable<Integer,Object> getRecipeData() {
		Hashtable<Integer,Object> output = new Hashtable<Integer,Object>();
		Hashtable<String,String> sdata;
		int i = 0;
		//Element baseNodeE = baseNode.select("body").first();
		String img;
		Elements baseNodes = baseNode.select("#doc #bd-wrap #bd #results #cols #left #main #web ol .recipe-article");
        for (Iterator<Element> iterator = baseNodes.iterator(); iterator.hasNext();) {
        	sdata = new Hashtable<String,String>();
            Element divElement = (Element) iterator.next();
            Elements imgElements = divElement.select("img");
            if (imgElements != null && imgElements.size() > 0) {
            	img = imgElements.get(0).attr("data-src");
	            int ind = img.indexOf("http", 2);
	            if(-1 != ind) {
	            	img = img.substring(ind);
	            }
            } else {
            	img = "http://illpop.com/img_illust/food/t_rice_m28.jpg";
            }
            sdata.put("image", img);
            
            Elements titleLink = divElement.select(".yschttl");
            String title = titleLink.get(0).text().toString().replace("<b>", "").replace("</b>",	"").replace("\n", "");
            String url = titleLink.get(0).attr("href");
            sdata.put("title", title);
            int ind = url.indexOf("http", 2);
            if(-1 != ind) {
            	url = URLDecoder.decode(url.substring(ind));
            }
            sdata.put("URL", url);
            
            Elements totalTimeNode = divElement.select(".totaltime .attr-content");
            String totaltime ="";
            if(totalTimeNode !=null && totalTimeNode.size() > 0) {
            	totaltime = totalTimeNode.get(0).text().toString().replace("</span>", "").replace("<span class=\"num\">", "").replace("\n", "");
            }
            sdata.put("duration", totaltime);
            sdata.put("rating", "");
            Elements ingredientsNode = divElement.select(".ingredients .attr-content");
            String ingredients ="";
            if (ingredientsNode !=null && ingredientsNode.size() > 0) {
            	ingredients = ingredientsNode.get(0).text().toString();
            }
            sdata.put("ingredients", ingredients);
            output.put(i++, sdata);
        }
        return output;
	}
	public String genURL() {
		String url = URL + "?p=" + URLEncoder.encode(keyword);
		if (page > 1) {
			Integer startVal = ((page - 1) * 10) + 1; 
			url += "&b=" + startVal.toString(); 
		}
		if (infilterData != null && infilterData.size() > 0) {
			Enumeration<String> e = infilterData.keys();
			String key;
			ArrayList<String> valArray;
			String val;
			while (e.hasMoreElements()) {
				key = e.nextElement();
				valArray = infilterData.get(key);
				if (valArray.size() <= 0) {
					continue;
				}
				val = valArray.get(0);
				if (key.equals("totaltime")) {
					int indexOf = val.toString().indexOf(" ");
					Integer value = Integer.parseInt(val.toString().substring(0, indexOf));
					value *= 60;
					url += "&limttim=" + value.toString();
				} else if (key.equals("mealtype")) {
					url += "&limmeal=" + URLEncoder.encode(val.toString());
				} else if (key.equals("collection")) {
					url += "&limcoll=" + URLEncoder.encode(val.toString());
				} else if (key.equals("rating")) {
					url += "&limrate=" + URLEncoder.encode(val.toString());
				} else if (key.equals("holiday")) {
					url += "&limholi=" + URLEncoder.encode(val.toString());
				} else if (key.equals("sources")) {
					url += "&provider=" + URLEncoder.encode(val.toString());
				} else if (key.equals("ingredient_w")) {
					int len = valArray.size();
					for (int i = 0; i < len; i++) {
						url += "&limincing=" + URLEncoder.encode(valArray.get(i));
					}
				}  else if (key.equals("ingredient_wo")) {
					int len = valArray.size();
					for (int i = 0; i < len; i++) {
						url += "&limexcing=" + URLEncoder.encode(valArray.get(i));
					}
				} 
				
			}
		}
		return url;
	}
}
