package ru.valera.flickrgallery;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Created by Valera on 06.09.2016.
 */
public class QueryPreferences {
    private static final String PREF_SEARCH_QUERY = "searchQuery"; // используется в качестве ключа для хранения запроса

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
}
