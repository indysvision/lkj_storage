package com.indyvision.phrasefrenzy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.servlet.http.*;
/*
 ?action=insert&target="db_name"&type=who&content="text"
 ?action=get&target="db_name"&filter=abcd&use=who&content="Magdalena"

 [] = default if missing specific

 for insert:
 action
 - insert
 target
 - <db_name>
 type
 - how_subject	= a	
 - [who]			= b
 - what			= c
 - how_action	= d
 - why			= e
 - when			= f
 - where			= g
 content
 - <text_to_add>
 returnGet
 - true
 - [false]
 format
 - json
 - [text]

 for get:	
 action
 - get_random
 filter
 - bc
 - absdefg
 - [abcd]
 - ...
 use
 - [none]
 - how_subject	= a	
 - who			= b
 - what			= c
 - how_action	= d
 - why			= e
 - when			= f
 - where			= g
 content
 - <text_to_use>
 format
 - json
 - [text]
 */

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilter;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.labs.repackaged.org.json.JSONArray;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;

// TODO: Auto-generated Javadoc
/**
 * The Class PhraseServlet.
 */
@SuppressWarnings("serial")
public class PhraseServlet extends HttpServlet {

	Key dbKey;
	String action;
	String targetName;
	String type;
	String content;
	String filter;
	String use;
	String format;
	String returnGet;
	DatastoreService datastore;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse)
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		resp.setContentType("text/plain");

		action = req.getParameter("action");
		targetName = req.getParameter("target");
		type = req.getParameter("type");
		content = req.getParameter("content");
		filter = req.getParameter("filter");
		use = req.getParameter("use");
		format = req.getParameter("format");
		returnGet = req.getParameter("returnGet");

		if (action == null)
		{
			resp.getWriter().println("Wrong params");
			return;
		}
		if (action.equalsIgnoreCase("insert"))
		{
			if (!checkInsertParams())
			{
				resp.getWriter().println("Wrong params");
				return;
			}

			resp.getWriter().println(processInsert(targetName, type, content, returnGet, format));
		} else if (action.equalsIgnoreCase("get"))
		{
			if (!checkGetParams())
			{
				resp.getWriter().println("Wrong params");
				return;
			}

			resp.getWriter().println(processGet(targetName, filter, use, content, format));
		} else
		{
			resp.getWriter().println("Wrong params");
			return;
		}

	}

	/**
	 * Process get.
	 * 
	 * @param targetName
	 *            the target name
	 * @param filter
	 *            the filter
	 * @param use
	 *            the use
	 * @param content
	 *            the content
	 * @param format
	 *            the format
	 * @return the string result
	 */
	private String processGet(String targetName, String filter, String use, String content, String format)
	{
		// initialize params
		if (filter == null || filter.trim().equals(""))
		{
			filter = "abcd";
		}
		if (use == null || use.trim().equals(""))
		{
			use = "none";
		}
		if (format == null || format.trim().equals(""))
		{
			format = "text";
		}

		initStorage();
		boolean isUsedSomething = (use != null && !use.equalsIgnoreCase(""));
		String toPassOn = null;

		ArrayList<String> types = new ArrayList<String>();
		if (filter.contains("a"))
		{
			if (!(isUsedSomething && use.equalsIgnoreCase("how_subject")))
				types.add("how_subject");
			else
				toPassOn = formatEntity("how_subject", content);
		}
		if (filter.contains("b"))
		{
			if (!(isUsedSomething && use.equalsIgnoreCase("who")))
				types.add("who");
			else
				toPassOn = formatEntity("who", content);
		}
		if (filter.contains("c"))
		{
			if (!(isUsedSomething && use.equalsIgnoreCase("what")))
				types.add("what");
			else
				toPassOn = formatEntity("what", content);
		}
		if (filter.contains("d"))
		{
			if (!(isUsedSomething && use.equalsIgnoreCase("how_action")))
				types.add("how_action");
			else
				toPassOn = formatEntity("how_action", content);
		}
		if (filter.contains("e"))
		{
			if (!(isUsedSomething && use.equalsIgnoreCase("why")))
				types.add("why");
			else
				toPassOn = formatEntity("why", content);
		}
		if (filter.contains("f"))
		{
			if (!(isUsedSomething && use.equalsIgnoreCase("when")))
				types.add("when");
			else
				toPassOn = formatEntity("when", content);
		}
		if (filter.contains("g"))
		{
			if (!(isUsedSomething && use.equalsIgnoreCase("where")))
				types.add("where");
			else
				toPassOn = formatEntity("where", content);
		}

		return getFromStorage(targetName, types, toPassOn);
	}

	private String formatEntity(String type, String content)
	{
		if (format == null || format.trim().equals("") || format.equalsIgnoreCase("text"))
		{
			return "0 - <" + type + "> " + content + "\n";
		} else
		{
			JSONObject object = new JSONObject();
			try
			{
				object.put("type", type.replaceAll("\"", "").replaceAll("\\\"", ""));
				object.put("content", content.replaceAll("\"", "").replaceAll("\\\"", ""));
			} catch (JSONException e)
			{
				e.printStackTrace();
				return "{'error', 'json transformation failed'}";
			}

			return object.toString();
		}
	}

	/**
	 * Process insert.
	 * 
	 * @param targetName
	 *            the target name
	 * @param type
	 *            the type
	 * @param content
	 *            the content
	 * @param returnGet
	 *            the return get
	 * @param format
	 *            the format
	 * @return the string
	 */
	private String processInsert(String targetName, String type, String content, String returnGet, String format)
	{
		// initialize params
		if (type == null || type.trim().equals(""))
		{
			type = "who";
		}
		if (returnGet == null || returnGet.trim().equals(""))
		{
			returnGet = "false";
		}
		if (format == null || format.trim().equals(""))
		{
			format = "text";
		}

		initStorage();
		saveToStorage(targetName, type, content);
		if (returnGet.equalsIgnoreCase("true"))
		{
			if (format.equalsIgnoreCase("json"))
			{
				return null;
			} else
			{
				return null;
			}
		} else
		{
			return "Word added";
		}

	}

	/**
	 * Check get params.
	 * 
	 * @return true, if successful
	 */
	private boolean checkGetParams()
	{
		return (action != null && targetName != null);
	}

	/**
	 * Check insert params.
	 * 
	 * @return true, if successful
	 */
	private boolean checkInsertParams()
	{
		return (action != null && targetName != null && content != null);
	}

	private void initStorage()
	{
		datastore = DatastoreServiceFactory.getDatastoreService();
		dbKey = KeyFactory.createKey("PhraseStorage", targetName);
	}

	/**
	 * Gets the from storage.
	 * 
	 * @param targetName
	 *            the target name
	 * @param type
	 *            the type
	 * @return the from storage
	 */
	private String getFromStorage(String targetName, ArrayList<String> types, String toPassOn)
	{
		StringBuilder result = new StringBuilder();

		HashMap<String, String> resultsJson = new HashMap<>();

		if (toPassOn != null && !toPassOn.equals(""))
		{
			result.append(toPassOn);
		}

		int i = 1;
		for (String currentType : types)
		{
			Query query = new Query("Data", dbKey).addSort("rand_id",
					(new Random().nextBoolean()) ? Query.SortDirection.DESCENDING : Query.SortDirection.ASCENDING);

			Filter filter1 = new Query.FilterPredicate("type", FilterOperator.EQUAL, currentType);
			Filter filter2 = new Query.FilterPredicate("rand_id",
					(new Random().nextBoolean()) ? FilterOperator.LESS_THAN : FilterOperator.GREATER_THAN_OR_EQUAL,
					(new Random()).nextFloat());

			Filter compositeFilter = new CompositeFilter(CompositeFilterOperator.AND, Arrays.asList(filter1, filter2));
			query.setFilter(compositeFilter);

			List<Entity> records = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(15));
			Entity res = null;

			if (records.size() > 0)
			{
				res = records.get((new Random()).nextInt(records.size()));
			}

			if (records.size() == 0)
			{
				records = datastore.prepare(
						new Query("Data", dbKey).addSort(
								"rand_id",
								(new Random().nextBoolean()) ? Query.SortDirection.DESCENDING
										: Query.SortDirection.ASCENDING).setFilter(filter1)).asList(
						FetchOptions.Builder.withLimit(1));
				if (records.size() > 0)
					res = records.get(0);
			}

			if (res == null)
			{
				result.append(i++ + " - <" + currentType + "> N/A\n");
				resultsJson.put(currentType, null);
				continue;
			}

			// if (format != null && format.equalsIgnoreCase("json"))
			// {
			// return getJson(res, toPassOn);
			// }

			result.append(i++ + " - <" + currentType + "> " + res.getProperty("content") + "\n");
			resultsJson.put(currentType, (String) res.getProperty("content"));
		}

		if (format != null && format.equalsIgnoreCase("json"))
		{
			return getJson(resultsJson, parseTextToJson(toPassOn));
		} else
		{
			return result.toString();
		}
	}

	private JSONObject parseTextToJson(String inputObj)
	{
		if (inputObj == null)
			return null;
		try
		{
			return new JSONObject(inputObj);
		} catch (JSONException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Gets the json.
	 * 
	 * @param records
	 *            the records
	 * @return the json
	 */
	private String getJson(HashMap<String, String> records, JSONObject extra)
	{
		JSONObject resultJson = new JSONObject();
		try
		{
			if (records.size() == 0)
			{
				resultJson.put("error", "no records found");
				return resultJson.toString();
			}
			JSONArray jsonArray = new JSONArray();
			int i = 1;
			for (String key : records.keySet())
			{
				JSONObject object1 = new JSONObject();
				object1.put("type", key.replaceAll("\"", "").replaceAll("\\\"", ""));
				if (records.get(key) == null)
				{
					object1.put("content", "N/A");
				} else
				{
					object1.put("content", records.get(key).replaceAll("\"", "").replaceAll("\\\"", ""));
				}
				jsonArray.put(object1);
			}
			if (extra != null)
			{
				jsonArray.put(extra);
			}
			resultJson.put("results", jsonArray);
		} catch (JSONException e)
		{
			e.printStackTrace();
			return "{'error', 'json transformation failed'}";
		}

		return resultJson.toString();
	}

	/**
	 * Save to storage.
	 * 
	 * @param targetName
	 *            the target name
	 * @param type
	 *            the type
	 * @param content
	 *            the content
	 * @return true, if successful
	 */
	private boolean saveToStorage(String targetName, String type, String content)
	{
		Date date = new Date();
		Entity dataToSave = new Entity("Data", dbKey);

		Random rand = new Random();
		dataToSave.setProperty("rand_id", rand.nextFloat());
		dataToSave.setProperty("date", date);
		dataToSave.setProperty("content", content);
		dataToSave.setProperty("type", type);

		datastore.put(dataToSave);
		return true;
	}

}
