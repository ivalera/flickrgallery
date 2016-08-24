package ru.valera.flickrgallery.ui.fragments;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

import ru.valera.flickrgallery.FlickrFetchr;
import ru.valera.flickrgallery.R;
import ru.valera.flickrgallery.ThumbnailDownloader;
import ru.valera.flickrgallery.model.GalleryItem;

/**
 * Created by Valera on 22.08.2016.
 */
public class PhotoGalleryFragment extends Fragment{

    private static final String TAG = "PhotoGalleryFragment";
    private static final int COL_WIDTH = 300;
    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    private int numPage = 1;

    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // удеживает фрагмент, чтобы поворот экрана не приводил к многократному порождению
        // новых объектов AsynkTask для загрузки JSON
        setRetainInstance(true);
        new FetchItemTask().execute();

        Handler responseHandler = new Handler();
        // создание нового потока
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloaderListener(new ThumbnailDownloader.ThumbnailDownloaderListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloader(PhotoHolder photoHolder, Bitmap bitmap) {
                Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                photoHolder.bindDrawable(drawable);
            }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");
    }

    // для прорисовки пользовательского интерфейса
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPhotoRecyclerView = (RecyclerView)v.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        // для обновления страниц по скролу после ста элементов
        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(!recyclerView.canScrollVertically(1)){
                    numPage++;
                    new FetchItemTask().execute();
                }
            }
        });
        // для динамичекого количества столбцов
        mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int numColumns = mPhotoRecyclerView.getWidth() / COL_WIDTH;
                        GridLayoutManager layoutManager = (GridLayoutManager)mPhotoRecyclerView.getLayoutManager();
                        layoutManager.setSpanCount(numColumns);
                    }
                }
        );

        setupAdapter();

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
        mThumbnailDownloader.clearCache();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");

    }
    // проверяет текущее сосотеяние модели (List<GalleryItem>)
    // и соответствующим образом настраиваем адаптер для RecyclerView
    private void setupAdapter(){
        // проверяем был ли присоединен фрагмент к активности
        if(isAdded()){
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    // ViewHolder класс хранящий ссылки на виджеты
    private class PhotoHolder extends RecyclerView.ViewHolder{
        //private TextView titleTextView;
        ImageView mItemImageView;

        public PhotoHolder(View itemView){
            super(itemView);
            //titleTextView = (TextView) itemView;
            mItemImageView = (ImageView)itemView
                    .findViewById(R.id.fragment_photo_gallery_image_view);
        }

        /*public void bindGalleryItem(GalleryItem item){
            titleTextView.setText(item.toString());
        }*/

        public void bindDrawable(Drawable drawable){
            mItemImageView.setImageDrawable(drawable);
        }
    }
    // Adapter
    public class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{
        private List<GalleryItem> mGalleryItems;
        private int lastBoundPosition;

        public PhotoAdapter(List<GalleryItem> galleryItems){
            mGalleryItems = galleryItems;
        }
        // Создание новых View и ViewHolder элемента списка, которые впоследствии могут переиспользоваться
        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            /*TextView textView = new TextView(getActivity());
            return new PhotoHolder(textView);*/
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, viewGroup, false);
            return new PhotoHolder(view);
        }
        // Заполнение виджетов View данными
        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            Bitmap bitmap = mThumbnailDownloader.getCachedImage(galleryItem.getUrl());

            if (bitmap == null) {
                Drawable drawable = getResources().getDrawable(R.drawable.loading_image);
                photoHolder.bindDrawable(drawable);
                mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());
            } else {
                Log.i(TAG, "Loaded image from cache");
                photoHolder.bindDrawable(new BitmapDrawable(getResources(), bitmap));
            }

            //Now preload the previous and next 10 images
            preloadAdjacentImages(position);

            lastBoundPosition = position;
            Log.i(TAG,"Last bound position is " + Integer.toString(lastBoundPosition));
        }

        private void preloadAdjacentImages(int position) {
            final int imageBufferSize = 10; //Number of images before & after position to cache

            //Set the Indexes for the images to preload
            int startIndex = Math.max(position - imageBufferSize, 0); //Starting index must be >= 0
            int endIndex = Math.min(position + imageBufferSize, mGalleryItems.size() - 1); //Ending index must be <= number of galleryItems - 1

            //Loop over mGallery items using our index bounds
            for (int i = startIndex; i <= endIndex; i++) {
                //We don't need to preload the "current" item, as it is being
                //displayed already.
                if (i == position) continue;

                String url = mGalleryItems.get(i).getUrl();
                mThumbnailDownloader.preloadImage(url);
            }
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    public class FetchItemTask extends AsyncTask<Void, Void, List<GalleryItem>> {

        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            return new FlickrFetchr().fetchItems(numPage);
        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
            mItems = galleryItems;
            setupAdapter();
        }
    }
}
