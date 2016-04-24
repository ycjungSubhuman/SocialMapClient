package com.example.ycjung.socialmap;


import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.SupportMapFragment;


/**
 * A simple {@link Fragment} subclass.
 */
public class DrawSupportMapFragment extends SupportMapFragment {
    public View mOriginalContentView;
    public MapWrapperLayout mMapWrapperLayout;


    public DrawSupportMapFragment() {
        // Required empty public constructor
    }


    @Override
    public View getView() {
        return mOriginalContentView;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mOriginalContentView = super.onCreateView(inflater, container, savedInstanceState);
        mMapWrapperLayout = new MapWrapperLayout(getActivity());
        mMapWrapperLayout.addView(mOriginalContentView);
        return mMapWrapperLayout;
    }

    public void setOnDragListener(MapWrapperLayout.OnDragListener onDragListener) {
        mMapWrapperLayout.setOnDragListener(onDragListener);
    }
    public void setOnTouchUpListener(MapWrapperLayout.OnTouchUpListener onTouchUpListener) {
        mMapWrapperLayout.setOnTouchUpListener(onTouchUpListener);
    }

}
