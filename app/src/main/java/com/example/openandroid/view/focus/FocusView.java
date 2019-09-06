package com.example.openandroid.view.focus;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.example.openandroid.R;


/**
 * Focus view.
 */
public class FocusView extends RotateLayout implements IFocusView {
    private ImageView mFocusRing;
    private RelativeLayout mExpandView;
    private Runnable mDisappear = new Disappear();
    private Runnable mEndAction = new EndAction();
    private static final int SCALING_UP_TIME = 1000;
    private static final int SCALING_DOWN_STAY_TIME = 2000;
    private static final int SCALING_DOWN_TIME = 200;
    private static final int DISAPPEAR_TIMEOUT = 200;
    private IFocusView.FocusViewState mState = FocusViewState.STATE_IDLE;
    private RectF mPreviewRect = new RectF();
    private int mFocusViewX;
    private int mFocusViewY;
    private boolean mIsExpandViewRightOfFocusRing = true;

    /**
     * The constructor.
     *
     * @param context The Context the view is running in, through which it can access the current
     *                theme, resources, etc.
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    public FocusView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mFocusRing = (ImageView) findViewById(R.id.focus_ring);
        mExpandView = (RelativeLayout) findViewById(R.id.expand_view);
    }

    @Override
    public boolean isPassiveFocusRunning() {
        return mState == FocusViewState.STATE_PASSIVE_FOCUSING;
    }

    private boolean isExpandViewOutOfDisplay() {
        int previewLeft = (int) mPreviewRect.left;
        int previewRight = (int) mPreviewRect.right;
        int previewTop = (int) mPreviewRect.top;
        int previewBottom = (int) mPreviewRect.bottom;

        switch (getOrientation()) {
            case 0:
                return mFocusViewX + (mFocusRing.getWidth() / 2 + mExpandView.getWidth()) >
                        previewRight;
            case 90:
                return mFocusViewY - (mFocusRing.getWidth() / 2 + mExpandView.getWidth()) <
                        previewTop;
            case 180:
                return mFocusViewX - (mFocusRing.getWidth() / 2 + mExpandView.getWidth()) <
                        previewLeft;
            case 270:
                return mFocusViewY + (mFocusRing.getWidth() / 2 + mExpandView.getWidth()) >
                        previewBottom;
            default:
                return false;
        }
    }

    @Override
    protected void onLayout(boolean change, int left, int top, int right, int bottom) {
        super.onLayout(change, left, top, right, bottom);
        @SuppressLint("DrawAllocation")
        RelativeLayout.LayoutParams layoutParams =
                new RelativeLayout.LayoutParams(mExpandView.getLayoutParams());
        //Change expand view position if it is out of display.
        if (isExpandViewOutOfDisplay()) {
            if (mIsExpandViewRightOfFocusRing) {
                layoutParams.addRule(RelativeLayout.LEFT_OF, R.id.focus_ring);
                layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
                mExpandView.setLayoutParams(layoutParams);
                mExpandView.postInvalidate();
                mIsExpandViewRightOfFocusRing = false;
            }
            return;
        }

        if (!mIsExpandViewRightOfFocusRing) {
            layoutParams.addRule(RelativeLayout.RIGHT_OF, R.id.focus_ring);
            layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
            mExpandView.setLayoutParams(layoutParams);
            mExpandView.postInvalidate();
            mIsExpandViewRightOfFocusRing = true;
            return;
        }
    }

    @Override
    public boolean isActiveFocusRunning() {
        return (mState == FocusViewState.STATE_ACTIVE_FOCUSING);
    }

    @Override
    public void startPassiveFocus() {
        if (mState != FocusViewState.STATE_IDLE || getHandler() == null) {
            return;
        }
        getHandler().removeCallbacks(mDisappear);
        setContentDescription("continue focus");
        mFocusRing.setVisibility(VISIBLE);
        mExpandView.setVisibility(INVISIBLE);
        mFocusRing.setImageDrawable(this.getResources().getDrawable(R.drawable.ic_continue_focus));
        setVisibility(VISIBLE);
        animate().withLayer().setDuration(SCALING_UP_TIME).scaleX(1.2f).scaleY(1.2f).alpha(1.0f);
        mState = FocusViewState.STATE_PASSIVE_FOCUSING;
    }

    @Override
    public void startActiveFocus() {
        Log.d("wangchao_focus","startActiveFocus------------------->");
        if (mState != FocusViewState.STATE_IDLE || getHandler() == null) {
            return;
        }
        getHandler().removeCallbacks(mDisappear);
        setContentDescription("touch focus");
        mExpandView.setVisibility(VISIBLE);
        mFocusRing.setVisibility(VISIBLE);
        mFocusRing.setImageDrawable(this.getResources().getDrawable(R.drawable.ic_touch_focus));
        setVisibility(VISIBLE);
        animate().withLayer().setDuration(SCALING_UP_TIME).scaleX(1.2f).scaleY(1.2f).alpha(1.0f);
        mState = FocusViewState.STATE_ACTIVE_FOCUSING;
    }

    @Override
    public void stopFocusAnimations() {
        Log.d("stopFocusAnimations","stopFocusAnimations---------------------------->="+isPassiveFocusRunning());
        if (isPassiveFocusRunning()) {
            animate().withLayer().setDuration(SCALING_DOWN_TIME).scaleX(1f).scaleY(1f) .withEndAction(mEndAction);
        } else if (isActiveFocusRunning()) {
            mState = FocusViewState.STATE_ACTIVE_FOCUSED;
            animate().withLayer().setDuration(SCALING_DOWN_TIME).scaleX(1f).scaleY(1f);
            postDelayed(new ActiveFocusEndAction(), SCALING_DOWN_STAY_TIME);
        }
    }

    @Override
    public void setFocusLocation(float viewX, float viewY) {
        mFocusViewX = (int) viewX;
        mFocusViewY = (int) viewY;
    }

    @Override
    public void centerFocusLocation() {

    }

    protected void setPreviewRect(RectF previewRect) {
      mPreviewRect = previewRect;
    }

    protected FocusViewState getFocusState() {
        return mState;
    }

    protected void setFocusState(FocusViewState state) {
        mState = state;
    }

    protected void clearFocusUi() {
        lowlightFocusView();
        setVisibility(INVISIBLE);
        mState = FocusViewState.STATE_IDLE;
    }

    protected void highlightFocusView() {
        animate().withLayer().alpha(1.0f);
    }

    protected void lowlightFocusView() {
        postDelayed(new ActiveFocusEndAction(), SCALING_DOWN_STAY_TIME);
    }

    private class EndAction implements Runnable {
        @Override
        public void run() {
            // Keep the focus indicator for some time.
            postDelayed(mDisappear, DISAPPEAR_TIMEOUT);
        }
    }
    /**
     * Action of active focus.
     */
    private class ActiveFocusEndAction implements Runnable {
        @Override
        public void run() {
            animate().withLayer().alpha(0.5f);
        }
    }
    private class Disappear implements Runnable {
        @Override
        public void run() {
            clearFocusUi();
        }
    }

}
