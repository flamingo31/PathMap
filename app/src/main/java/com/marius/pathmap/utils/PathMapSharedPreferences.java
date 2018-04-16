package com.marius.pathmap.utils;

import android.content.Context;
import android.content.SharedPreferences;


public class PathMapSharedPreferences {
    private static final String SHARED_PREFS_NAME = "path_map_preferences";
    private static final String TRACKING_STATE = "tracking_state";

    private static PathMapSharedPreferences instance;
    private SharedPreferences sharedPreferences;


    public static synchronized PathMapSharedPreferences getInstance(Context context) {
        if (instance == null) {
            instance = new PathMapSharedPreferences(context);
        }

        return instance;
    }

    private PathMapSharedPreferences(Context context) {
        instance = this;
        sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void delete(String key) {
        if (sharedPreferences.contains(key)) {
            getEditor().remove(key).apply();
        }
    }

    public void save(String key, Object value) {
        SharedPreferences.Editor editor = getEditor();
        if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        } else if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Enum) {
            editor.putString(key, value.toString());
        } else if (value != null) {
            throw new RuntimeException("Attempting to save non-supported preference");
        }

        editor.apply();
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) sharedPreferences.getAll().get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defValue) {
        T returnValue = (T) sharedPreferences.getAll().get(key);
        return returnValue == null ? defValue : returnValue;
    }

    public boolean has(String key) {
        return sharedPreferences.contains(key);
    }


    public void saveTrackingState(boolean trackingState) {
        save(TRACKING_STATE, trackingState);
    }

    public void removeTrackingState() {
        delete(TRACKING_STATE);

    }

    public boolean getTrackingState() {
        if (hasTrackingState()) {
            boolean state = get(TRACKING_STATE);
            return state;
        } else {
            return false;
        }
    }

    public boolean hasTrackingState() {
        return has(TRACKING_STATE);
    }

    public void clearAllData(){
        SharedPreferences.Editor editor = getEditor();
        editor.clear().apply();
    }

    private SharedPreferences.Editor getEditor() {
        return sharedPreferences.edit();
    }
}