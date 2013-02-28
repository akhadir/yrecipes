package com.example.mymind;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

public class DataFilter {
	public Hashtable <String,Hashtable<String,String>> data = new Hashtable<String,Hashtable<String,String>>();
	public DataFilter(){}
	public DataFilter(String d){
		this.setData(d);
	}
	public DataFilter getObject(String d) {
		this.setData(d);
		return this;
	}
	
	public Hashtable<String, Hashtable<String, String>> getData() {
		return data;
	}
	
	public void setData(Hashtable<String, Hashtable<String, String>> data) {
		this.data = data;
	}
	public void setData(String d) {
		JSONObject jsonFirst;
		try {
			jsonFirst = new JSONObject(d);
			@SuppressWarnings("unchecked")
			Iterator<String> itrMain =  (Iterator<String>) jsonFirst.keys();
			while (itrMain.hasNext()) {
				String main = itrMain.next();
				JSONObject jobj = (JSONObject) jsonFirst.get(main);
				@SuppressWarnings("unchecked")
				Iterator<String> itr =  (Iterator<String>) jobj.keys();
				Hashtable<String,String> value = new Hashtable<String,String>();
				while (itr.hasNext()) {
					String key = (String) itr.next();
					String val = (String) jobj.get(key);
					value.put(key, val);
				}
				data.put(main, value);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	public Hashtable<String,String> get(String key) {
		return data.get(key);
	}
	public void put(String key, Hashtable<String,String> dat) {
		data.put(key, dat);
	}
	
	public Enumeration<String> keys(){
		return data.keys();
	}
	public int size() {
		return data.size();
	}
	
	@Override
	public String toString() {
		Enumeration<String> e = this.data.keys();
		String output = "{";
		Boolean firstMain = true;
		while (e.hasMoreElements()) {
			String key = e.nextElement();
			Hashtable<String,String> value = this.data.get(key);
			if (!firstMain) {
				output += ",";
			}
			output += "\"" + key+ "\":{";
			Enumeration<String> ekeys = value.keys();
			firstMain = false;
			Boolean firstSub = true;
			while (ekeys.hasMoreElements()) {
				if (!firstSub) {
					output += ",";
				}
				String dkey = ekeys.nextElement();
				String dValue = value.get(dkey);
				output += "\"" + dkey + "\":\"" + dValue + "\"";
				firstSub = false;
			}
			output += "}";
		}
		output += "}";
		return output;
	}
	
}
