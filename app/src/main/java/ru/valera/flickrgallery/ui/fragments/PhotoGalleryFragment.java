package ru.valera.flickrgallery.ui.fragments;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import ru.valera.flickrgallery.FlickrFetchr;
import ru.valera.flickrgallery.QueryPreferences;
import ru.valera.flickrgallery.R;
import ru.valera.flickrgallery.model.GalleryItem;

/**
 * Created by Valera on 22.08.2016.
 */
public class PhotoGalleryFragment extends Fragment{

    private static final String TAG = "PhotoGalleryFragment";
    private static final int COL_WIDTH = 360;
    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    // !
    //private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private int columnResize;

    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // удеживает фрагмент, чтобы поворот экрана не приводил к многократному порождению
        // новых объектов AsynkTask для загрузки JSON
        setRetainInstance(true);
        // чтобы зарегестрировать фрагмент для получения обратных вызовов меню
        setHasOptionsMenu(true);
        updateItems();

        // !
       /* Handler responseHandler = new Handler();
        // создание нового потока для загрузки изображения
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloaderListener(new ThumbnailDownloader.ThumbnailDownloaderListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloader(PhotoHolder photoHolder, Bitmap bitmap) {
                Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                photoHolder.bindDrawable(drawable);
            }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();*/
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
                    new FetchItemTask().execute();
                }
            }
        });
        // для динамичекого количества столбцов
        mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        columnResize = mPhotoRecyclerView.getWidth() / 3;
                        Log.e("Width", String.valueOf(columnResize));
                        GridLayoutManager layoutManager = (GridLayoutManager)mPhotoRecyclerView.getLayoutManager();
                        layoutManager.setSpanCount(3);
                    }
                }
        );

        setupAdapter();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // !
        //mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // !
        //mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");

    }

    // меню
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.fragment_photo_gallery, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView(); // получаем SearchView

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            // выполняется при отправке запроса пользователем
            @Override
            public boolean onQueryTextSubmit(String s) {
                Log.d(TAG, "QueryTextSubmit:" + s);
                QueryPreferences.setStoreQuery(getActivity(), s);
                updateItems();
                return true;
            }

            // вызывается при любом изменении текста в текстовои поле SearchView 490
            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(TAG, "QueryTextChange:" + s);
                return false;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoreQuery(getActivity(), null);
                updateItems();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateItems(){
        new FetchItemTask().execute();
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
        ImageView mItemImageView;

        public PhotoHolder(View itemView){
            super(itemView);
            mItemImageView = (ImageView)itemView
                    .findViewById(R.id.fragment_photo_gallery_image_view);
            mItemImageView.getLayoutParams().height = columnResize;
        }

        // Заргузчик изображения Picasso
        public void bindGalleryItem(GalleryItem galleryItem){
            Picasso.with(getActivity()).
                    load(galleryItem.getUrl())
                    .placeholder(R.drawable.loading_image)
                    .into(mItemImageView);
        }

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
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, viewGroup, false);
            return new PhotoHolder(view);
        }
        // Заполнение виджетов View данными
        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            photoHolder.bindGalleryItem(galleryItem);

            // !
           /* Drawable placeholder = getResources().getDrawable(R.drawable.loading_image);
            photoHolder.bindDrawable(placeholder);*/

            //mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());

            lastBoundPosition = position;
            Log.i(TAG,"Last bound position is " + Integer.toString(lastBoundPosition));
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    public class FetchItemTask extends AsyncTask<Void, Void, List<GalleryItem>> {

        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            String query = "robot";

            if(query == null){
                return new FlickrFetchr().fetchRecentPhotos();
            }else {
                return new FlickrFetchr().searchPhotos(query);
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
            mItems = galleryItems;
            setupAdapter();
        }
    }
}
