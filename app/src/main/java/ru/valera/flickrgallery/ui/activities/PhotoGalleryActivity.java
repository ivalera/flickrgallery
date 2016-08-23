package ru.valera.flickrgallery.ui.activities;

import android.support.v4.app.Fragment;

import ru.valera.flickrgallery.ui.fragments.PhotoGalleryFragment;

public class PhotoGalleryActivity extends SingleFragmentActivity {

    @Override
    public Fragment createFragment(){
        return PhotoGalleryFragment.newInstance();
    }
}
