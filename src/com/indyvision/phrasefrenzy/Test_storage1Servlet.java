package com.indyvision.phrasefrenzy;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.labs.repackaged.org.json.JSONArray;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;

@SuppressWarnings("serial")
public class Test_storage1Servlet extends HttpServlet {
	Key dbKey;
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		resp.setContentType("text/plain");
		String targetName = req.getParameter("target");
		String keyName = req.getParameter("key");
		String formatName = req.getParameter("format");
		
//		String userAgent = req.getHeader("X-AppEngine-City");

		if (targetName !=null ){
			if (keyName == null){
			    if (formatName == null){	    	
					resp.getWriter().println("getting all data for " + targetName);
			    }
				resp.getWriter().println(getFromStorage(targetName, null, formatName));
			}
			else{
			    if (formatName == null){
					resp.getWriter().println("getting data for " + targetName + " with key " + keyName);
			    }
				resp.getWriter().println(getFromStorage(targetName, keyName, formatName));
			}
		}
		else{
			resp.getWriter().println("Wrong params");
		}
	}
	
	private boolean saveToStorage(String target, String toSave, String keyToSave){
        if (target == null || target.equalsIgnoreCase("")){
        	return false;
        }
        if (toSave == null || toSave.equalsIgnoreCase("")){
        	return false;
        }
        dbKey = KeyFactory.createKey("GeneralStorage", target);
        Date date = new Date();
        Entity dataToSave = new Entity("Data", dbKey);
        
        dataToSave.setProperty("date", date);
        dataToSave.setProperty("content", toSave);
        if (keyToSave != null && !keyToSave.equalsIgnoreCase("")){
            dataToSave.setProperty("extraKey", keyToSave);
        }

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        datastore.put(dataToSave);
        return true;
	}
	
	private String getFromStorage(String targetDomain, String targetKey, String format){
	    StringBuilder result = new StringBuilder();
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        dbKey = KeyFactory.createKey("GeneralStorage", targetDomain);
		Query query = new Query("Data", dbKey).addSort("date", Query.SortDirection.DESCENDING);

	    if (targetKey != null && !targetKey.equalsIgnoreCase("")){
		    query.setFilter(new Query.FilterPredicate("extraKey", FilterOperator.EQUAL, targetKey));
	    }
	    List<Entity> records = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(15));
    
	    if (format != null && format.equalsIgnoreCase("json")){	    	
	    	return getJson(records);
	    }
	    
	    if (records.size() == 0){
	    	return "N/A";
	    }
	    int i = 1;
	    for (Entity ent : records){
	    	result.append(i + " - <" +ent.getProperty("date") + "> " + ent.getProperty("content") + "\n");
	    }
	    if (result.equals("")){
	    	return "N/A";
	    }
	    else{
		    return result.toString();
	    }
	}

	private String getJson(List<Entity> records){
		JSONObject object = new JSONObject();
		  try {
			    if (records.size() == 0){
				    object.put("error", "no records found");
			    	return object.toString();
			    }
			    JSONArray jsonArray = new JSONArray();
			    int i = 1;
			    for (Entity ent : records){
					JSONObject object1 = new JSONObject();
				    object1.put("date", ent.getProperty("date"));
				    object1.put("content", ent.getProperty("content"));
				    jsonArray.put(object1);
			    }
			    object.put("results", jsonArray);
			    object.put("results_size", records.size());
  
		  } catch (JSONException e) {
		    e.printStackTrace();
		    return "{'error', 'json transformation failed'}";
		  }
		return object.toString();
	}
	
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		String target = req.getParameter("target");
		String data = req.getParameter("data");
		String optionalKey = req.getParameter("key");
		
		if (target != null && data != null){
			resp.setContentType("text/plain");
			resp.getWriter().println("Saving values in " + target + " - <" + data + ">");
			if (saveToStorage(target, data, optionalKey)){
				resp.getWriter().println("Done");
			}
			else{
				resp.getWriter().println("Failed");
			}
		}
		else{
			resp.setContentType("text/plain");
			resp.getWriter().println("Incorrect parameters");
		}
	}
}
