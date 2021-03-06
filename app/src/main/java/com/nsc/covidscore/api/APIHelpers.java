package com.nsc.covidscore.api;

import com.nsc.covidscore.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class APIHelpers {
    /**
     * Handle the String response for a Volley request and pass it to a callback function
     * @param response the string response returned from the Volley request
     * @param cb the callback method used to access the response data after its been processed
     */
    public static void handleStringResponse(String response, VolleyStringCallback cb) {
        try {
            JSONArray jsonArray = new JSONArray(response);
            String countyPopulation = jsonArray.getJSONArray(1).getString(1);
            cb.getString(countyPopulation);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handle the JSON response for a Volley request and pass it to a callback function
     * @param type the type of response expected: COUNTY, COUNTY_HISTORICAL, PROVINCE, or COUNTY_HISTORICAL
     * @param response the string response returned from the Volley request
     * @param county the county name
     * @param state the state name
     * @param cb the callback method used to access the response data after its been processed
     */
    public static void handleJsonResponse(
            String type, String response, String county, String state, VolleyJsonCallback cb) {
        try {
            switch (type) {
                case Constants.COUNTY: {
                    JSONArray counties = new JSONArray(response);
                    if (counties.length() > 1) {
                        for (int i = 0; i < counties.length(); i++) {
                            JSONObject jsonObject = counties.getJSONObject(i);
                            String stateName = jsonObject.optString(Constants.PROVINCE);
                            if (state.equalsIgnoreCase(stateName.toLowerCase())) {
                                cb.getJsonData(jsonObject);
                                break;
                            }
                        }
                    } else {
                        cb.getJsonData(counties.getJSONObject(0));
                    }
                    break;
                }
                case Constants.COUNTY_HISTORICAL: {
                    JSONArray counties = new JSONArray(response);
                    for (int i = 0; i < counties.length(); i++) {
                        JSONObject jsonObject = counties.getJSONObject(i);
                        String countyName = jsonObject.optString(Constants.COUNTY);
                        if (countyName.equalsIgnoreCase(county)) {
                            cb.getJsonData(jsonObject);
                            break;
                        }
                    }
                    break;
                }
                case Constants.PROVINCE:
                    JSONObject jsonObject = new JSONObject(response);
                    String stateName = jsonObject.optString(Constants.STATE);
                    if (stateName.equalsIgnoreCase(state)) {
                        cb.getJsonData(jsonObject);
                    }
                    break;
                case Constants.COUNTRY_HISTORICAL:
                    cb.getJsonData(new JSONObject(response));
                    break;
                default:
                    throw new JSONException(Constants.ERROR_STATE_COUNTY);
            }
        } catch (JSONException | IOException e) {
            cb.getJsonException(e);
            e.printStackTrace();
        }
    }
}
