/**
 * Copyright 2017 Bo Song
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bosong.expandablelayout;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AbsListView;
import android.widget.LinearLayout;

/**
 * Created by bosong on 4/17/2017.
 */

public class ExpandableLayout extends LinearLayout{
    private static final String TAG = "ExpandableLayout";
    private final int ANIMATION_DURATION = 300;

    /**
     *  Height in collapsed state
     *  收缩起来的高度
     */
    private int mCollapsedHeight;
    /**
     *  Height in expanded state
     *  展开的高度
     */
    private int mExpandedHeight;
    /**
     *  Used in OnMeasure method
     */
    private int mCurrentHeight = -1;
    /**
     *  current state: true -> collapsed
     *  标记当前的状态 true -> 收缩
     */
    private boolean mCollapsed = true;
    /**
     *  The parent which can scroll (RecyclerView, ListView)
     *  可滚动的父布局
     */
    private ViewGroup mScrolledParent;
    /**
     *  Last visible view in collapsed state
     *  收缩后最下面的View，通过这个view计算得到收缩后的高度
     */
    private View mLastView;
    private View mAnchorView;
    /**
     *  Offset for collapsed height
     *  收缩起来高度的偏移
     */
    private int mCollapsedOffset;
    /**
     * Offset for expanded height
     *
     */
    private int mExpandedOffset;
    private boolean mExpandWithScroll; // true to scroll parent if expanded content exceeds parent's bottom edge
    private boolean mCollapseWithScroll; // true to scroll to ExpandableLayout's head
    private int mExpandScrollOffset;
    private int mCollapseScrollOffset;
    private boolean mAnimating;

    private ExpandCollapseAnimation mAnimation;

    private OnExpandListener mOnExpandListener;

    public ExpandableLayout(Context context) {
        this(context, null);
    }

    public ExpandableLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);

        mAnimation = new ExpandCollapseAnimation();
        mAnimation.setFillAfter(true);
        mAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // clear animation here to avoid repeated applyTransformation() calls
                clearAnimation();

                if (mOnExpandListener != null) {
                    mOnExpandListener.onExpand(!mCollapsed);
                }

                if (mScrolledParent != null) {
                    int scrollDistanceY = 0;
                    if (mExpandWithScroll && !mCollapsed && expandShouldScrollParent()) {
                        scrollDistanceY = getDescendantBottom((ViewGroup) mScrolledParent.getParent(), ExpandableLayout.this)
                                - mScrolledParent.getBottom() + mExpandScrollOffset;
                    } else if (mCollapseWithScroll && collapsedShouldScrollParent()) {
                        scrollDistanceY = getDescendantTop((ViewGroup) mScrolledParent.getParent(), ExpandableLayout.this)
                                - mScrolledParent.getTop() - mCollapseScrollOffset;
                    }

                    if (mScrolledParent instanceof RecyclerView) {
                        RecyclerView recyclerView = (RecyclerView) mScrolledParent;
                        recyclerView.smoothScrollBy(0, scrollDistanceY);
                    } else if (mScrolledParent instanceof AbsListView) {
                        AbsListView listView = (AbsListView) mScrolledParent;
                        listView.smoothScrollBy(scrollDistanceY, ANIMATION_DURATION);
                    }
                }

                mAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mScrolledParent = findScrolledParent();
    }

    /**
     * Set a View, we will collapse views under this view in the screen.
     * @param view Last view in collapsed state
     */
    public void setCollapsedEdgeView(View view){
        if(view == null || view.equals(mLastView)){
            return;
        }
        mLastView = view;

        requestLayout();
        invalidate();
    }

    public void setCollapsedEdgeView(View view, int collapsedOffset) {
        if(view == null) {
            return;
        }
        mCollapsedOffset = collapsedOffset;
        setCollapsedEdgeView(view);
    }

    // Not Supported
//    /**
//     * A view occupy space in ExpandableLayout
//     * @param view
//     */
//    public void setAnchorView(View view) {
//        mAnchorView = view;
//    }

    public void setOnExpandListener(OnExpandListener listener) {
        mOnExpandListener = listener;
    }

    public void setExpandWithScroll(boolean scroll){
        mExpandWithScroll = scroll;
    }

    /**
     * Scroll the parent(if parent is RecyclerView or AbsListView) when expanded view beyonds the view-port
     * @param scroll
     * @param offset
     */
    public void setExpandWithScroll(boolean scroll, int offset) {
        setExpandWithScroll(scroll);
        mExpandScrollOffset = offset;
    }

    public void setCollapseWithScroll(boolean scroll) {
        mCollapseWithScroll = scroll;
    }

    public void setCollapseWithScroll(boolean scroll, int offset) {
        setCollapseWithScroll(scroll);
        mCollapseScrollOffset = offset;
    }

    public void initState(boolean collapsed){
        if(collapsed){ // init as collapsed
            collapse(false);
        }else{
            expand(false);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if(mCollapsedHeight == 0) {
            int width = getMeasuredWidth();
            int height = getMeasuredHeight();
            layout(0, 0, width, height);

            int collapsedHeight = getDescendantBottom(ExpandableLayout.this, mLastView) + mCollapsedOffset;
            if(collapsedHeight > 0 && mCollapsedHeight != collapsedHeight) {
                Log.d(TAG, "collapsedHeight ->>>>> " + collapsedHeight);
                mCollapsedHeight = collapsedHeight;
                mCurrentHeight = collapsedHeight;
            }
        }

        mExpandedHeight = getMeasuredHeight();
        if(mCurrentHeight >= 0 && mExpandedHeight != mCurrentHeight) {
            int width = getMeasuredWidth();
            setMeasuredDimension(width, mCurrentHeight);
            Log.d(TAG, "current height ->>>>>> " + mCurrentHeight);
        }
    }

    public void toggle(boolean withAnimation){
        if(mAnimating) {
            return;
        }
        if (mCollapsed) { // need expand
            expand(withAnimation);
        } else { // need collapsed
            collapse(withAnimation);
        }
    }

    public void toggleWithAnimation() {
        toggle(true);
    }

    /**
     * Toggle with offsets
     * @param expandedOffset Add offset to expanded height
     * @param collapsedOffset Add offset to collapsed height
     */
    public void toggleWithOffset(int expandedOffset, int collapsedOffset){
        mExpandedOffset = expandedOffset;
        mCollapsedOffset = collapsedOffset;
        mExpandedHeight += mExpandedOffset;
        mCollapsedHeight += mCollapsedOffset;
        toggle(true);
    }

    public void expand(boolean withAnimation) {
        if(withAnimation && mAnimation != null) {
            clearAnimation();
            mAnimation.setData(getHeight(), mExpandedHeight);
            startAnimation(mAnimation);
        } else {
            mCurrentHeight = mExpandedHeight;
            requestLayout();
        }
        mCollapsed = false;
    }

    public void collapse(boolean withAnimation) {
        if(withAnimation && mAnimation != null) {
            clearAnimation();
            mAnimation.setData(getHeight(), mCollapsedHeight);
            startAnimation(mAnimation);
        } else {
            mCurrentHeight = mCollapsedHeight;
            requestLayout();
        }
        mCollapsed = true;
    }

    private boolean expandShouldScrollParent() {
        if(mScrolledParent != null) {
            ViewGroup parent = (ViewGroup) mScrolledParent.getParent();
            if(getDescendantTop(parent, this) + mExpandedHeight > mScrolledParent.getBottom()){
                return true;
            }
        }
        return false;
    }

    private boolean collapsedShouldScrollParent(){
        if(mScrolledParent != null) {
            ViewGroup parent = (ViewGroup) mScrolledParent.getParent();
            if(getDescendantBottom(parent, this) - mCollapsedHeight < mScrolledParent.getTop()){
                return true;
            }
        }
        return false;
    }

    private ViewGroup findScrolledParent(){
        ViewParent parent= getParent();
        while (parent!=null){
            if((parent instanceof RecyclerView || parent instanceof AbsListView)) {
                return (ViewGroup)parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    /**
     * get descendant's bottom relative to parent
     * @param view
     * @return
     */
    private int getDescendantBottom(ViewGroup parent, View view){
        if(parent == null || view == null){
            return 0;
        }
        int height = 0;
        if(view.getLayoutParams() instanceof MarginLayoutParams){
            MarginLayoutParams lp = (MarginLayoutParams) view.getLayoutParams();
            height = lp.bottomMargin;
        }

        height += view.getMeasuredHeight();
        return height + getDescendantTop(parent, view);
    }

    /**
     * get descendant's top relative to parent
     * @param parent
     * @param view
     * @return
     */
    private int getDescendantTop(ViewGroup parent, View view) {
        if(parent == null || view == null || view.getParent() == null){
            return 0;
        }
        if(view.getParent() instanceof View) {
            int top = view.getTop();
            if(view.getParent() == parent){
                return top;
            }else{
                return top + getDescendantTop(parent, (View) view.getParent());
            }
        } else { // view.getParent() is a ViewRootImpl instance means that parent does not contain this view
            throw new RuntimeException("view must be included in the parent");
        }
    }

    private class ExpandCollapseAnimation extends Animation {
        private int mStartHeight;
        private int mEndHeight;

        public ExpandCollapseAnimation() {
            setDuration(ANIMATION_DURATION);
        }

        public ExpandCollapseAnimation(int startHeight, int endHeight) {
            setData(startHeight, endHeight);
            setDuration(ANIMATION_DURATION);
        }

        public void setData(int startHeight, int endHeight) {
            mStartHeight = startHeight;
            mEndHeight = endHeight;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            int newHeight = (int) ((mEndHeight - mStartHeight) * interpolatedTime + mStartHeight);

            int heightOffset = 0;

            if(mOnExpandListener != null) {
                heightOffset = mOnExpandListener.onAnimating(!mCollapsed, interpolatedTime);
            }

            mCurrentHeight = newHeight + heightOffset;

            requestLayout();
        }
    }

    interface OnExpandListener{
        void onExpand(boolean expand);

        /**
         *
         * @param expanding true -> expanding
         * @param interpolatedTime The value of the normalized time (0.0 to 1.0)
         *        after it has been run through the interpolation function.
         * @return Return value will add to current height
         */
        int onAnimating(boolean expanding, float interpolatedTime);
    }
}