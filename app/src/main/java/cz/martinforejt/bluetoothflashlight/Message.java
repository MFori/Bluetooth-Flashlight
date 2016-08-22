package cz.martinforejt.bluetoothflashlight;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Martin Forejt on 21.08.2016.
 * forejt.martin97@gmail.com
 */
public class Message {

    public static final int TYPE_INIT = 0;
    public static final int TYPE_ACCEPT = 200;
    public static final int TYPE_END = 1;
    public static final int TYPE_LIGHT = 2;

    public static final int LIGHT_01 = 2;

    public static String create(int type, Map<String, String> params) {
        switch (type) {
            case TYPE_INIT:
                return initMessage(params);
            case TYPE_END:
                return endMessage();
            case TYPE_LIGHT:
                return light(params);
            case TYPE_ACCEPT:
                return accept(params);
        }

        return "";
    }

    public static String create(int type) {
        switch (type) {
            case TYPE_END:
                return endMessage();
            case TYPE_LIGHT:
                Map<String, String> params = new HashMap<>();
                params.put("light_type", String.valueOf(LIGHT_01));
                return light(params);
            case TYPE_ACCEPT:
                Map<String, String> params_a = new HashMap<>();
                params_a.put("has_flash", "0");
                return accept(params_a);
        }

        return "";
    }

    /**
     * @param params Map
     * @return String
     */
    private static String light(Map<String, String> params) {
        JSONObject json = new JSONObject();
        if (params.containsKey("light_type")) {
            try {
                json.put("type", TYPE_LIGHT);
                json.put("light_type", params.get("light_type"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return json.toString();
    }

    /**
     * @param params Map
     * @return String
     */
    private static String initMessage(Map<String, String> params) {
        JSONObject json = new JSONObject();
        try {
            json.put("type", TYPE_INIT);
            if (params.containsKey("both")) json.put("both", params.get("both"));
            if (params.containsKey("has_flash")) json.put("has_flash", params.get("has_flash"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json.toString();
    }

    /**
     * @param params Map
     * @return String
     */
    private static String accept(Map<String, String> params) {
        JSONObject json = new JSONObject();
        try {
            json.put("type", TYPE_ACCEPT);
            if (params.containsKey("has_flash")) json.put("has_flash", params.get("has_flash"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json.toString();
    }

    /**
     * @return String
     */
    private static String endMessage() {
        JSONObject json = new JSONObject();

        try {
            json.put("type", TYPE_END);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json.toString();
    }
}
