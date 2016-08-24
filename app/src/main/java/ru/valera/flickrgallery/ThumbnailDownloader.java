package ru.valera.flickrgallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.util.LruCache;
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
    private final static int MESSAGE_PRELOAD = 1;
    // отвечает за постановку в очередь запросов на загрузку в фоновом потоке
    // так же отвечает за обработку сообщений запросов на загрузку при извлечении их из очеред
    private Handler mRequestHandler;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();
    private Handler mResponseHandler;
    private ThumbnailDownloaderListener<T> mThumbnailDownloaderListener;
    private LruCache<String, Bitmap> mLruCache;

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
        mLruCache = new LruCache<String, Bitmap>(16384);
    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler(){
            // проверяем тип сообщения
            @Override
            public void handleMessage(Message msg) {
                switch(msg.what) {
                    case MESSAGE_DOWNLOAD:
                        //Image needed to be displayed on screen now.
                        //obj is a ViewHolder (a PhotoHolder to be specific)
                        //We need to download/cache the image (or pull from cache), and then send it to the responseHandler
                        T target = (T) msg.obj;
                        handleRequest(target);
                        break;
                    case MESSAGE_PRELOAD:
                        //A request to preload an image for future use was created.
                        //obj is a string (with a url to the image)
                        //we just need to download it and put it in cache (if it's not there already)
                        String url = (String) msg.obj;
                        downloadImage(url);
                        break;
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

    public void preloadImage(String url) {
        mRequestHandler.obtainMessage(MESSAGE_PRELOAD, url).sendToTarget();
    }

    // удаление всеx запросов из очереди
    public void clearQueue(){
        mResponseHandler.removeMessages(MESSAGE_DOWNLOAD);
    }

    public void clearCache() {
        mLruCache.evictAll();
    }

    public Bitmap getCachedImage(String url) {
        return mLruCache.get(url);
    }

    private void handleRequest(final T target) {
        final String url = mRequestMap.get(target);
        final Bitmap bitmap;
        if (url == null) {
            return;
        }
        bitmap = downloadImage(url);
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
    }

    private Bitmap downloadImage(String url) {
        Bitmap bitmap;

        if (url == null) //whoops!
            return null;

        //If the image is already in cache, no need to download it, just return it.
        bitmap = mLruCache.get(url);
        if (bitmap != null)
            return bitmap;

        //download and cache the image. Then return it in case it's needed right away.
        try {
            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            mLruCache.put(url, bitmap);
            Log.i(TAG, "Downloaded & cached image: " + url);
            return bitmap;
        } catch (IOException ex) {
            Log.e(TAG, "Error downloading image.", ex);
            return null;
        }
    }
}
