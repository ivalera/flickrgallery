package ru.valera.flickrgallery;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Created by Valera on 06.09.2016.
 */
public class QueryPreferences {
    private static final String PREF_SEARCH_QUERY = "searchQuery"; // используется в качестве ключа для хранения запроса
    private static final String PREF_LATS_RESULT_ID = "lastResultId";

    // возвращает занчание запроса, хранящиеся в общих настройках
    public static String getStoreQuery(Context context){
        // определяет возвращаемое значение по умолчанию,
        // которое должно возвращатся при отсутствии записи ключем PREF_SEARCH_QUERY
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_SEARCH_QUERY, null);
    }

    public static void setStoreQuery(Context context, String query){
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_SEARCH_QUERY, query)
                .apply();
    }

    public static String getLastResultId(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_LATS_RESULT_ID ,null);
    }

    public static void setLastResultId(Context context, String lastResultId){
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_LATS_RESULT_ID ,lastResultId)
                .apply();
    }
}
