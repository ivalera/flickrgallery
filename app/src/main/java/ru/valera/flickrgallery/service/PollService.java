package ru.valera.flickrgallery.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.util.List;

import ru.valera.flickrgallery.FlickrFetchr;
import ru.valera.flickrgallery.QueryPreferences;
import ru.valera.flickrgallery.R;
import ru.valera.flickrgallery.model.GalleryItem;
import ru.valera.flickrgallery.ui.activities.PhotoGalleryActivity;

/**
 * Created by Valera on 09.09.2016.
 */
public class PollService extends IntentService {

    public static final String TAG = "PollService";

    public static final int POLL_INTERVAL = 1000 * 60; // 60 секунд

    public static Intent newIntent(Context context){
        return new Intent(context, PollService.class);
    }

    // отложенное выполнене
    public static void setServiceAlarm(Context context, boolean isOn){
        Intent intent = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, intent, 0);
        // отправляет интенты самостоятельно
        AlarmManager alarmManager = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);
        if(isOn){
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime(), POLL_INTERVAL, pi);
        }else {
            alarmManager.cancel(pi);
            pi.cancel();
        }
    }

    public static boolean isServiceAlarmOn(Context context){
        Intent intent = PollService.newIntent(context);
        PendingIntent pi = PendingIntent
                .getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE);
        return pi != null;
    }

    public PollService(){
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(!isNetworkAvailableAndConnected()){
            return;
        }
        //Log.i(TAG, "Received an intent: " + intent);
        // чтение текущего запроса и идентификатора последнего результата
        String query = QueryPreferences.getStoreQuery(this);
        String lastResultId = QueryPreferences.getLastResultId(this);

        List<GalleryItem> items;

        // загрузка последнего набора результатов
        if(query == null){
            items = new FlickrFetchr().fetchRecentPhotos();
        }else {
            items = new FlickrFetchr().searchPhotos(query);
        }

        // если набор не пуст
        if(items.size() == 0){
            return;
        }

        // получить первый результат
        String resultId = items.get(0).getId();
        // проверяем отличается ли идентификатор первого результата от последнего
        if(resultId.equals(lastResultId)){
            Log.i(TAG, "Got an old result:" + resultId);
        }else {
            Log.i(TAG, "Got a new result: " + resultId);
        }

        Resources resources = getResources();
        Intent i = PhotoGalleryActivity.newIntent(this);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

        Notification notification = new Notification.Builder(this)
                .setTicker(resources.getString(R.string.new_pictures_title)) // текст бегущей строки
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setContentTitle(resources.getString(R.string.new_pictures_title))
                .setContentText(resources.getString(R.string.new_pictures_text))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();

        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);
        notificationManager.notify(0, notification);

        // сохраняем первый результат
        QueryPreferences.setLastResultId(this, resultId);
    }

    private boolean isNetworkAvailableAndConnected(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        // проверка на доступность интернета для фоновой службы
        boolean isNetworkAvailable = cm.getActiveNetworkInfo() !=null;
        // проверяется полноценное подключение к интернету
        boolean isNetworkConnected = isNetworkAvailable && cm.getActiveNetworkInfo().isConnected();
        return isNetworkConnected;
    }
}
