package ru.valera.flickrgallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Valera on 24.08.2016.
 */
public class ThumbnailDownloader<T> extends HandlerThread{

    private static final String TAG = "ThumbnailDownloader";
    // для идентификатора сообщений
    private static final int MESSAGE_DOWNLOAD = 0;
    // отвечает за постановку в очередь запросов на загрузку в фоновом потоке
    // так же отвечает за обработку сообщений запросов на загрузку при извлечении их из очеред
    private Handler mRequestHandler;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();
    private Handler mResponseHandler;
    private ThumbnailDownloaderListener<T> mThumbnailDownloaderListener;

    public interface ThumbnailDownloaderListener<T>{
        // будет вызвано, через некоторое время, когда изображение загружно
        // и готово к добавлению в пользовательский интерфейс
        void onThumbnailDownloader(T target, Bitmap thumbnail);
    }

    public void setThumbnailDownloaderListener(ThumbnailDownloaderListener<T> listener){
        mThumbnailDownloaderListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler){
        super(TAG);
        mResponseHandler = responseHandler;
    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler(){
            // проверяем тип сообщения
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == MESSAGE_DOWNLOAD){
                    T target = (T) msg.obj;
                    Log.i(TAG, "Got a request for URl: " + mRequestMap.get(target));
                    handleRequest(target);
                }
            }
        };
    }

    public void queueThumbnail(T target, String url){
        Log.i(TAG, "Got a URL: " + url);

        if(url == null){
            mRequestMap.remove(target);
        }else {
            mRequestMap.put(target, url);
            // во избежании создания новых объектов Message, работаем с методом obtainMessage
            // использующим общий пул объектов
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget();
        }
    }

    // удаление всеx запросов из очереди
    public void clearQueue(){
        mResponseHandler.removeMessages(MESSAGE_DOWNLOAD);
    }

    private void handleRequest(final T target){
        try {
            final String url = mRequestMap.get(target);
            if(url == null){
                return;
            }
            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            // для построения растрового изображения с массивом байтов
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0 , bitmapBytes.length);
            Log.i(TAG, "Bitmap created");
            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    // эта проверка гарантирует, что каждый объект PhotoHolder получит правильное
                    // изображение, даже если за прошедшее время был сделан другой запрос
                    if (mRequestMap.get(target) != url) {
                        return;
                    }
                    mRequestMap.remove(target);
                    mThumbnailDownloaderListener.onThumbnailDownloader(target, bitmap);
                }
            });
        }catch (IOException ioe){
            Log.e(TAG, "Error downloading image", ioe);
        }
    }
}
