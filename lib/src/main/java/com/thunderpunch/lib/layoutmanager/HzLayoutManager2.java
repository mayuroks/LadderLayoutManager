package com.thunderpunch.lib.layoutmanager;

import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO Step 1 return layout params
 * TODO Step 2 define constructor
 * TODO Step 3 no child to show remove and recycler all VIEWs
 * TODO Step 4 setup child view size based on orientation
 */
public class HzLayoutManager2 extends RecyclerView.LayoutManager {
    private static final String TAG = "HzLayoutManager2";
    private static final int INVALIDATE_SCROLL_OFFSET = Integer.MAX_VALUE;

    // TODO give random integers
    public static final int VERTICAL = 1;
    public static final int HORIZONTAL = 0;

    private static final float DEFAULT_CHILD_LAYOUT_OFFSET = 0.2f;

    private float mItemHeightWidthRatio;
    private float mScale;
    private int mOrientation;
    private Interpolator mInterpolator;

    // TODO write a function to setup child peek size
    // Peeksize is cards that are behind, how much region should be exposed
    private int mChildPeekSize;
    private int mChildPeekSizeInput;

    private static final String CHILD_WIDTH = "childWidth";
    private static final String CHILD_HEIGHT = "childHeight";
    private Map<String, Integer> mChildXYParams;

    // TODO change variable name to is
    private boolean mCheckedChildSize;
    private int mScrollOffset = INVALIDATE_SCROLL_OFFSET;
    private int mChildCount;
    private boolean mReverse;
    private ChildDecorateHelper mDecorateHelper;
    private int mMaxItemLayoutCount;
    private int UNLIMITED = 0;

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        // TODO Step 1 return layout params
        return new RecyclerView.LayoutParams(getChildWidth(), getChildHeight());
    }

    public HzLayoutManager2(float itemHeightWidthRatio, float scale, int orientation) {
        mItemHeightWidthRatio = itemHeightWidthRatio;
        mScale = scale;
        mOrientation = orientation;

        // setup child layout params
//        mChildSize = new int[2];

        mChildXYParams = new HashMap<>();

        // setup interpolator
        mInterpolator = new DecelerateInterpolator();
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        Log.i(TAG, "onLayoutChildren: called when layout out children views");

        if (state.getItemCount() == 0) {
            // TODO step 3 No child to show remove and recycler all VIEWs
            removeAndRecycleAllViews(recycler);
            return;
        }

        // TODO step 4 setup child view size based on orientation
        if (!mCheckedChildSize) {
//            if (mOrientation == VERTICAL) {
//                mChildSize[0] = getHorizontalSpace();
//                mChildSize[1] = (int) (mItemHeightWidthRatio * mChildSize[0]);
//            } else {
//                mChildSize[1] = getVerticalSpace();
//                mChildSize[0] = (int) (mChildSize[1] / mItemHeightWidthRatio);
//            }

            setChildSize(mOrientation);

            // TODO childPeekSize depends on mOrientation
            // refactor this
            mChildPeekSize = mChildPeekSizeInput == 0 ?
                    (int) (getChildSize(mOrientation) * DEFAULT_CHILD_LAYOUT_OFFSET) : mChildPeekSizeInput;
            Log.i(TAG, "onLayoutChildren: mChildPeekSize " + mChildPeekSize);

            mCheckedChildSize = true;
        }

        int itemCount = getItemCount();

        // TODO setup scroll offset, what happens when its not setup
        if (mReverse) { // figure out how reverse works
            mScrollOffset += (itemCount - mChildCount) * getChildSize(mOrientation);
        }

        mChildCount = itemCount;
        mScrollOffset = makeScrollOffsetWithinRange(mScrollOffset);
        fill(recycler);
    }

    // TODO fill()
    public void fill(RecyclerView.Recycler recycler) {
        Log.i(TAG, "fill: called");
        /*
         * This method is called when laying out children and
         * scrolling vertically or updating
         * */
        int bottomItemPosition = (int) Math.floor(mScrollOffset / getChildSize(mOrientation));//>=1

        int bottomItemVisibleSize = mScrollOffset % getChildSize(mOrientation);

        // offsetFactor [0,1)
        final float offsetFactor = mInterpolator.getInterpolation(
                bottomItemVisibleSize * 1.0f / getChildSize(mOrientation));

        Log.i(TAG, "fill: bottomItemPosition " + bottomItemPosition + " " + bottomItemVisibleSize + " " + offsetFactor + " " + getChildSize(mOrientation));
        final int recyclerViewSpace = mOrientation == VERTICAL ? getVerticalSpace() : getHorizontalSpace();

        ArrayList<ItemLayoutInfo> layoutInfos = new ArrayList<>();

        for (int i = bottomItemPosition - 1, j = 1, remainSpace = recyclerViewSpace - getChildSize(mOrientation); i >= 0; i--, j++) {
            Log.i(TAG, "forloop: " + remainSpace + " " + recyclerViewSpace);
            double maxOffset = mChildPeekSize * Math.pow(mScale, j);

            int childStart = (int) (remainSpace - offsetFactor * maxOffset);
            float layoutPercent = childStart * 1.0f / recyclerViewSpace;

            ItemLayoutInfo info
                    = new ItemLayoutInfo(childStart, offsetFactor, layoutPercent);

            layoutInfos.add(0, info);

            if (mMaxItemLayoutCount != UNLIMITED && j == mMaxItemLayoutCount - 1) {
                Log.i(TAG, "fill: mMaxItemLayoutCount != UNLIMITED && j == mMaxItemLayoutCount - 1");
                if (offsetFactor != 0) {
                    Log.i(TAG, "fill: offsetFactor != 0");
                    info.start = remainSpace;
                    info.positionoffsetFactor = 0;
                    info.layoutPercent = remainSpace / recyclerViewSpace;
                }
                break;
            }

            // Reduce remain space for next card
            remainSpace -= maxOffset;
            Log.i(TAG, "fill: offsetFactor " + offsetFactor + " remainSpace " + remainSpace + " maxOffset " + maxOffset);

            if (remainSpace <= 0) {
                Log.i(TAG, "fill: remainSpace <= 0");
                info.start = (int) (remainSpace + maxOffset);
                info.positionoffsetFactor = 0;
                info.layoutPercent = info.start / recyclerViewSpace;
                break;
            }
        }

        if (bottomItemPosition < mChildCount) {
            Log.i(TAG, "bottomItemPosition < mChildCount " + bottomItemPosition + " " + mChildCount);
            final int start = recyclerViewSpace - bottomItemVisibleSize;

            layoutInfos.add(new ItemLayoutInfo(start,
                    bottomItemVisibleSize * 1.0f / getChildSize(mOrientation),
                    start * 1.0f / recyclerViewSpace).setIsBottom());
        } else {
            Log.i(TAG, "bottomItemPosition >= mChildCount " + bottomItemPosition + " " + mChildCount);
            bottomItemPosition -= 1;
        }

        int layoutCount = layoutInfos.size();

        // Check if a child position is out of visible range
        final int startPos = bottomItemPosition - (layoutCount - 1);
        final int endPos = bottomItemPosition;

        final int childCount = getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View childView = getChildAt(i);
            int pos = convert2LayoutPosition(getPosition(childView));

            if (pos < startPos || pos > endPos) {
                Log.i(TAG, "recycling view " + pos + " " + endPos + " " + startPos);
                removeAndRecycleView(childView, recycler);
            }
        }
        detachAndScrapAttachedViews(recycler);

        for (int i = 0; i < layoutCount; i++) {
            fillChild(recycler.getViewForPosition(convert2AdapterPosition(startPos + i)), layoutInfos.get(i));
        }
    }

    // TODO fillChild()
    private void fillChild(View view, HzLayoutManager2.ItemLayoutInfo layoutInfo) {
        // TODO add the view
        addView(view);

        // TODO ???
        measureChildWithExactlySize(view);

        // TODO layout the view with decorated margins
        if (mOrientation == VERTICAL) {
            int left = getPaddingLeft();
            layoutDecoratedWithMargins(view, left, layoutInfo.start
                    , left + getChildWidth(), layoutInfo.start + getChildHeight());
        } else {
            int top = getPaddingTop();
            layoutDecoratedWithMargins(view, layoutInfo.start, top
                    , layoutInfo.start + getChildWidth(), top + getChildHeight());
        }

        if (mDecorateHelper != null) {
            mDecorateHelper.decorateChild(view, layoutInfo.positionoffsetFactor, layoutInfo.layoutPercent, layoutInfo.isBottom);
        }
    }

    @Override
    public boolean canScrollHorizontally() {
        return mOrientation == HORIZONTAL;
    }

    // TODO scrollHorizontallyBy()
    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int pendingScrollOffset = mScrollOffset + dx;
        mScrollOffset = makeScrollOffsetWithinRange(pendingScrollOffset);
        fill(recycler);
        return mScrollOffset - pendingScrollOffset + dx;
    }

    private int getVerticalSpace() {
        /* TODO
         * Play with params
         * what if only height is returned
         * */
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    private int getHorizontalSpace() {
        /* TODO
         * Play with params
         * what if only width is returned
         * */
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    private int makeScrollOffsetWithinRange(int scrollOffset) {
        /* TODO
         * What does this formula do
         * */
        Log.i(TAG, "makeScrollOffsetWithinRange: before " + scrollOffset);
        int offset = Math.max(getChildSize(mOrientation), scrollOffset);
        int childCountOffset = mChildCount * getChildSize(mOrientation);

        Log.i(TAG, "makeScrollOffsetWithinRange: after " + offset + " " + childCountOffset);
        return Math.min(offset, childCountOffset);
    }

    // TODO measureChildWithExactlySize
    private void measureChildWithExactlySize(View child) {
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();
        final int widthSpec = View.MeasureSpec.makeMeasureSpec(
                getChildWidth() - lp.leftMargin - lp.rightMargin, View.MeasureSpec.EXACTLY);
        final int heightSpec = View.MeasureSpec.makeMeasureSpec(
                getChildHeight() - lp.topMargin - lp.bottomMargin, View.MeasureSpec.EXACTLY);
        child.measure(widthSpec, heightSpec);
    }

    // TODO convert2LayoutPosition
    public int convert2LayoutPosition(int adapterPostion) {
        return mReverse ? mChildCount - 1 - adapterPostion : adapterPostion;
    }

    // TODO convert2AdapterPosition
    public int convert2AdapterPosition(int layoutPosition) {
        return mReverse ? mChildCount - 1 - layoutPosition : layoutPosition;
    }

    public void setChildPeekSize(int childPeekSize) {
        Log.i(TAG, "setChildPeekSize: called");
        mChildPeekSizeInput = childPeekSize;
        mCheckedChildSize = false;
        if (getChildCount() > 0) {
            requestLayout();
        }
    }

    public void setMaxItemLayoutCount(int count) {
        mMaxItemLayoutCount = Math.max(2, count);
        if (getChildCount() > 0) {
            requestLayout();
        }
    }

    public HzLayoutManager2 setChildDecorateHelper(HzLayoutManager2.ChildDecorateHelper layoutHelper) {
        mDecorateHelper = layoutHelper;
        return this;
    }

    // TODO ItemLayoutInfo
    private static class ItemLayoutInfo {
        float layoutPercent;
        float positionoffsetFactor;
        int start; // start of item
        boolean isBottom;

        ItemLayoutInfo(int start, float positionOffset, float layoutPercent) {
            this.start = start;
            this.positionoffsetFactor = positionOffset;
            this.layoutPercent = layoutPercent;
        }

        HzLayoutManager2.ItemLayoutInfo setIsBottom() {
            isBottom = true;
            return this;
        }
    }

    // TODO Item decorator
    public interface ChildDecorateHelper {
        /**
         * @param child
         * @param posoffsetFactor childview相对于自身起始位置的偏移量百分比范围[0，1)
         * @param layoutPercent   childview 在整个布局中的位置百分比
         * @param isBottom        childview 是否处于底部
         */
        void decorateChild(View child, float posoffsetFactor, float layoutPercent, boolean isBottom);
    }

    public static class DefaultChildDecorateHelper implements HzLayoutManager2.ChildDecorateHelper {
        private float mElevation;

        public DefaultChildDecorateHelper(float maxElevation) {
            mElevation = maxElevation;
        }

        @Override
        public void decorateChild(View child, float posoffsetFactor, float layoutPercent, boolean isBottom) {
            ViewCompat.setElevation(child, (float) (layoutPercent * mElevation * 0.7 + mElevation * 0.3));
        }
    }

    private int getChildSize(int orientation) {
        if (orientation == VERTICAL) {
            return mChildXYParams.get(CHILD_HEIGHT);
        } else {
            return mChildXYParams.get(CHILD_WIDTH);
        }
    }

    private int getChildWidth() {
        return mChildXYParams.get(CHILD_WIDTH);
    }

    private int getChildHeight() {
        return mChildXYParams.get(CHILD_HEIGHT);
    }

    private void setChildSize(int orientation) {
        int childHeight, childWidth;

        switch (orientation) {
            case VERTICAL:
                childWidth = getHorizontalSpace();
                childHeight = (int) (mItemHeightWidthRatio * childWidth);
                mChildXYParams.put(CHILD_WIDTH, childWidth);
                mChildXYParams.put(CHILD_HEIGHT, childHeight);
                break;

            case HORIZONTAL:
                childHeight = getVerticalSpace();
                childWidth = (int) (childHeight / mItemHeightWidthRatio);
                mChildXYParams.put(CHILD_WIDTH, childWidth);
                mChildXYParams.put(CHILD_HEIGHT, childHeight);
                break;
        }
    }
}
