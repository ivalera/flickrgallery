package ru.valera.flickrgallery.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import ru.valera.flickrgallery.ui.fragments.PhotoGalleryFragment;

public class PhotoGalleryActivity extends SingleFragmentActivity {

    public static Intent newIntent(Context context){
        return new Intent(context, PhotoGalleryActivity.class);
    }

    @Override
    public Fragment createFragment(){
        return PhotoGalleryFragment.newInstance();
    }
}
