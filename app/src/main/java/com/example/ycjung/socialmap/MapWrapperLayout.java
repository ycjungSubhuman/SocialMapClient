package com.example.ycjung.socialmap;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * Created by ycjung on 16. 4. 23.
 */
public class MapWrapperLayout extends FrameLayout {
    private OnDragListener mOnDragListener;
    private OnTouchUpListener mOnTouchUpListener;

    public MapWrapperLayout(Context context) {
        super(context);
    }

    public interface OnDragListener {
        public void onDrag(MotionEvent motionEvent);
    }
    public interface OnTouchUpListener {
        public void onTouchUp(MotionEvent motionEvent);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mOnDragListener != null) {
            mOnDragListener.onDrag(ev);
        }
        if (mOnTouchUpListener != null
                && (ev.getAction()&MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            mOnTouchUpListener.onTouchUp(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    public void setOnDragListener(OnDragListener mOnDragListener) {
        this.mOnDragListener = mOnDragListener;
    }
    public void setOnTouchUpListener(OnTouchUpListener mOnTouchUpListener) {
        this.mOnTouchUpListener = mOnTouchUpListener;
    }

}
