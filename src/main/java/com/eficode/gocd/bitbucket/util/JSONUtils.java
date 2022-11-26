package com.eficode.gocd.bitbucket.util;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class JSONUtils {
    public static Object fromJSON(String json) {
        return new GsonBuilder().create().fromJson(json, Object.class);
    }

    public static <T> T fromJSON(String json, Class<T> type) {
        return new GsonBuilder().create().fromJson(json, type);
    }

    public static <T> T fromJSON(String json, TypeToken<T> typeToken) {
        return new GsonBuilder().create().fromJson(json, typeToken.getType());
    }

    public static String toJSON(Object object) {
        return new GsonBuilder().create().toJson(object);
    }
}
