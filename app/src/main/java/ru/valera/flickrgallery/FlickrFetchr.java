package ru.valera.flickrgallery;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.valera.flickrgallery.model.GalleryItem;

/**
 * Created by Valera on 22.08.2016.
 */

// получатель
public class FlickrFetchr {

    private static final String TAG = "FlickrFetchr";
    private static final String API__KEY = "16f62924b491f9482441731f7a89e6e1";

    private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static final Uri ENDPOINT = Uri
            .parse("https://api.flickr.com/services/rest/")
            .buildUpon() // urlbuilder для пострения полного URL-адреса для API-запроса
            .appendQueryParameter("api_key", API__KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();

    public byte[] getUrlBytes(String urlSpec) throws IOException {

        URL url = new URL(urlSpec);
        // создане подключения к URL
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() +
                        ": with " +
                        urlSpec);
            }

            int byteRead = 0;
            byte[] buffer = new byte[1024];
            // вызываем read пока в подключении не кончатся данные
            while ((byteRead = in.read(buffer)) > 0) {
                // out выдает массив байтов
                out.write(buffer, 0, byteRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    public String getUrlsString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    // для получения последних фотографий
    public List<GalleryItem> fetchRecentPhotos(){
        String url = buildUrl(FETCH_RECENTS_METHOD, null);
        return downloadGalleryItems(url);
    }

    // для поиска фотографий
    public List<GalleryItem> searchPhotos(String query){
        String url = buildUrl(SEARCH_METHOD, query);
        return downloadGalleryItems(url);
    }

    // параметр page для страниц
    private List<GalleryItem> downloadGalleryItems(String url) {
        List<GalleryItem> items = new ArrayList<>();
        try {
            String jsonString = getUrlsString(url);
            Log.i(TAG, "Received JSON: " + jsonString);
            // JSONObject разбирает переданную строку JSON и строит иерархию объетков,
            // соответствующую исходному тектсу JSON
            JSONObject jsonBody = new JSONObject(jsonString);
            //parseItems(items, jsonBody);
            items = parseGsonItems(jsonBody);
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items", ioe);
        } catch (JSONException je) {
            Log.e(TAG, "Failed to parse JSON", je);

        }
        return items;
    }

    // для построения URL-адреса по значениям метода и запроса
    private String buildUrl(String method, String query){
        Uri.Builder uriBuilder = ENDPOINT.buildUpon()
                .appendQueryParameter("method", method);
        if(method.equals(SEARCH_METHOD)){
            uriBuilder.appendQueryParameter("text", query);
        }
        return uriBuilder.build().toString();
    }

    private void parseItems(List<GalleryItem> items, JSONObject jsonBody)
            throws IOException, JSONException{

        // методы getJSONObject и getJSONArray для пермещени по иерархии JSONObject
        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");

        for(int i = 0; i < photoJsonArray.length(); i++){
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);

            GalleryItem item = new GalleryItem();

            item.setId(photoJsonObject.getString("id"));
            item.setCaption(photoJsonObject.getString("title"));

            if(!photoJsonObject.has("url_s")){
                continue;
            }

            item.setUrl(photoJsonObject.getString("url_s"));
            items.add(item);
        }
    }
    // Задание Gson parse
    private List<GalleryItem> parseGsonItems(JSONObject jsonBody)
            throws JSONException {
        Gson gson = new GsonBuilder().create();
        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");
        return Arrays.asList(gson.fromJson(photoJsonArray.toString(), GalleryItem[].class));
    }
}
