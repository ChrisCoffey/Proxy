/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.proxy.widget;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.Toast;

import com.proxy.R;
import com.proxy.app.adapter.ImagePagerAdapter;

import static com.proxy.util.ViewUtils.dpToPx;

/**
 * To be used with ViewPager to provide a tab indicator component which give constant feedback as to
 * the user's scroll progress.
 * <p/>
 * To use the component, simply add it to your view hierarchy. Then in your {@link
 * android.app.Activity} or {@link android.support.v4.app.Fragment} call {@link
 * #setViewPager(android.support.v4.view.ViewPager)} providing it the ViewPager this layout is being
 * used for.
 * <p/>
 * The colors can be customized in two ways. The first and simplest is to provide an array of colors
 * via {@link #setSelectedIndicatorColors(int...)} and {@link #setDividerColors(int...)}. The
 * alternative is via the {@link TabColorizer} interface which provides you complete control over
 * which color is used for any individual position.
 * <p/>
 * The views used as tabs can be customized by calling {@link #setCustomTabView(int, int)},
 * providing the layout ID of your custom layout.
 */
@SuppressWarnings("unused")
public class SlidingTabLayout extends HorizontalScrollView {

    private static final int TITLE_OFFSET_DIPS = 24;
    private static final int TAB_VIEW_PADDING_DIPS = 16;
    private static final int TAB_VIEW_TEXT_SIZE_SP = 12;
    private final SlidingTabStrip _tabStrip;
    private final int _tabSize;
    private int _titleOffset;

    private int _tabViewLayoutId;
    private int _tabViewImageViewId;

    private ViewPager _viewPager;
    private OnPageChangeListener _viewPagerPageChangeListener;
    private Rect _layoutRect;

    /**
     * Constructor.
     *
     * @param context activity context
     */
    public SlidingTabLayout(Context context) {
        this(context, null);
    }

    /**
     * Constructor.
     *
     * @param context activity context
     * @param attrs   app attributes
     */
    public SlidingTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructor.
     *
     * @param context  activity context
     * @param attrs    app attributes
     * @param defStyle styles
     */
    public SlidingTabLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // Disable the Scroll Bar
        setHorizontalScrollBarEnabled(false);
        // Make sure that the Tab Strips fills this View
        setFillViewport(true);

        _titleOffset = (int) (TITLE_OFFSET_DIPS * getResources().getDisplayMetrics().density);

        _tabStrip = new SlidingTabStrip(context);
        addView(_tabStrip, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        _tabSize = (int) dpToPx(getResources(), R.dimen.common_rect_small);
    }

    /**
     * Set the custom {@link TabColorizer} to be used.
     * <p/>
     * If you only require simple custmisation then you can use {@link
     * #setSelectedIndicatorColors(int...)} and {@link #setDividerColors(int...)} to achieve similar
     * effects.
     *
     * @param tabColorizer colorizer to set
     */
    public void setCustomTabColorizer(TabColorizer tabColorizer) {
        _tabStrip.setCustomTabColorizer(tabColorizer);
    }

    /**
     * Sets the colors to be used for indicating the selected tab. These colors are treated as a
     * circular array. Providing one color will mean that all tabs are indicated with the same
     * color.
     *
     * @param colors indicator color
     */
    public void setSelectedIndicatorColors(int... colors) {
        _tabStrip.setSelectedIndicatorColors(colors);
    }

    /**
     * Sets the colors to be used for tab dividers. These colors are treated as a circular array.
     * Providing one color will mean that all tabs are indicated with the same color.
     *
     * @param colors divider color
     */
    public void setDividerColors(int... colors) {
        _tabStrip.setDividerColors(colors);
    }

    /**
     * Set the {@link OnPageChangeListener}. When using {@link
     * SlidingTabLayout} you are required to set any {@link android.support.v4.view.ViewPager
     * .OnPageChangeListener} through this method. This is so that the layout can update it's scroll
     * position correctly.
     *
     * @param listener to set
     * @see android.support.v4.view.ViewPager#setOnPageChangeListener
     */
    public void setOnPageChangeListener(OnPageChangeListener listener) {
        _viewPagerPageChangeListener = listener;
    }

    /**
     * Set the custom layout to be inflated for the tab views.
     *
     * @param layoutResId Layout id to be inflated
     * @param imageViewId id of the {@link android.widget.ImageView} in the inflated view
     */
    public void setCustomTabView(int layoutResId, int imageViewId) {
        _tabViewLayoutId = layoutResId;
        _tabViewImageViewId = imageViewId;
    }

    /**
     * Sets the associated view pager. Note that the assumption here is that the pager content
     * (number of tabs and tab titles) does not change after this call has been made.
     *
     * @param viewPager to set
     */
    public void setViewPager(ViewPager viewPager) {
        _tabStrip.removeAllViews();

        _viewPager = viewPager;
        if (viewPager != null) {
            viewPager.setOnPageChangeListener(new InternalViewPagerListener());
            populateTabStrip();
        }
    }

    /**
     * Create a default view to be used for tabs. This is called if a custom tab view is not set via
     * {@link #setCustomTabView(int, int)}.
     *
     * @param context this context
     * @return default textview
     */
    protected ImageView createDefaultTabView(Context context) {
        ImageView imageView = new ImageView(context);
        ViewCompat.setLayerType(imageView, ViewCompat.LAYER_TYPE_SOFTWARE, null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // If we're running on Honeycomb or newer, then we can use the Theme's
            // selectableItemBackground to ensure that the View has a pressed state
            TypedValue outValue = new TypedValue();
            getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground,
                outValue, true);
            imageView.setBackgroundResource(outValue.resourceId);
        }


        int padding = (int) (TAB_VIEW_PADDING_DIPS * getResources().getDisplayMetrics().density);
        imageView.setPadding(padding, padding, padding, padding);

        return imageView;
    }

    /**
     * Add tab strip.
     */
    private void populateTabStrip() {
        final ImagePagerAdapter adapter = (ImagePagerAdapter) _viewPager.getAdapter();
        final OnClickListener tabClickListener = new TabClickListener();

        for (int i = 0; i < adapter.getCount(); i++) {
            View tabView = null;
            ImageView tabImageView = null;

            if (_tabViewLayoutId != 0) {
                // If there is a custom tab view layout id set, try and inflate it
                tabView = LayoutInflater.from(getContext()).inflate(_tabViewLayoutId, _tabStrip,
                    false);
                tabImageView = (ImageView) tabView.findViewById(_tabViewImageViewId);
            }

            if (tabView == null) {
                tabView = createDefaultTabView(getContext());
            }

            if (tabImageView == null && ImageView.class.isInstance(tabView)) {
                tabImageView = (ImageView) tabView;
                tabImageView.setImageDrawable(adapter.getPageImage(i));
            }

            //This causes the TabView to be evenly distributed.
            tabView.setLayoutParams(new TableLayout.LayoutParams(0, _tabSize
                * (int) getResources().getDisplayMetrics().density, 1f));
            tabView.setOnClickListener(tabClickListener);
            tabView.setOnLongClickListener(getLongClickListener(adapter, i));

            _tabStrip.addView(tabView);
        }
    }


    /**
     * Add a long click listener to display content definition of a tab.
     *
     * @param adapter  ImageAdapter
     * @param position of object image description in a list
     * @return LongClickListener
     */
    private OnLongClickListener getLongClickListener(
        final ImagePagerAdapter adapter, final int position) {
        return new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(_viewPager.getContext(),
                    adapter.getImageDescription(position), Toast.LENGTH_SHORT).show();
                return false;
            }
        };
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (_viewPager != null) {
            scrollToTab(_viewPager.getCurrentItem(), 0);
        }
    }

    /**
     * Scroll to tab index and offset.
     *
     * @param tabIndex       position
     * @param positionOffset offset
     */
    private void scrollToTab(int tabIndex, int positionOffset) {
        final int tabStripChildCount = _tabStrip.getChildCount();
        if (tabStripChildCount == 0 || tabIndex < 0 || tabIndex >= tabStripChildCount) {
            return;
        }

        View selectedChild = _tabStrip.getChildAt(tabIndex);
        if (selectedChild != null) {
            int targetScrollX = selectedChild.getLeft() + positionOffset;

            if (tabIndex > 0 || positionOffset > 0) {
                // If we're not at the first child and are mid-scroll, make sure we obey the offset
                targetScrollX -= _titleOffset;
            }

            scrollTo(targetScrollX, 0);
        }
    }

    /**
     * Allows complete control over the colors drawn in the tab layout. Set with {@link
     * #setCustomTabColorizer(TabColorizer)}.
     */
    public interface TabColorizer {

        /**
         * @param position index
         * @return return the color of the indicator used when {@code position} is selected.
         */
        int getIndicatorColor(int position);

        /**
         * @param position index
         * @return return the color of the divider drawn to the right of {@code position}.
         */
        int getDividerColor(int position);

    }


    /**
     * Common ViewPageListener.
     */
    private class InternalViewPagerListener implements OnPageChangeListener {
        private int mScrollState;

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            int tabStripChildCount = _tabStrip.getChildCount();
            if ((tabStripChildCount == 0) || (position < 0) || (position >= tabStripChildCount)) {
                return;
            }

            _tabStrip.onViewPagerPageChanged(position, positionOffset);

            View selectedTitle = _tabStrip.getChildAt(position);
            int extraOffset = (selectedTitle != null)
                ? (int) (positionOffset * selectedTitle.getWidth())
                : 0;
            scrollToTab(position, extraOffset);

            if (_viewPagerPageChangeListener != null) {
                _viewPagerPageChangeListener.onPageScrolled(position, positionOffset,
                    positionOffsetPixels);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            mScrollState = state;

            if (_viewPagerPageChangeListener != null) {
                _viewPagerPageChangeListener.onPageScrollStateChanged(state);
            }
        }

        @Override
        public void onPageSelected(int position) {
            if (mScrollState == ViewPager.SCROLL_STATE_IDLE) {
                _tabStrip.onViewPagerPageChanged(position, 0f);
                scrollToTab(position, 0);
            }

            if (_viewPagerPageChangeListener != null) {
                _viewPagerPageChangeListener.onPageSelected(position);
            }
        }

    }

    /**
     * Tab clicklistener.
     */
    private class TabClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            for (int i = 0; i < _tabStrip.getChildCount(); i++) {
                if (v == _tabStrip.getChildAt(i)) {
                    _viewPager.setCurrentItem(i);
                    return;
                }
            }
        }
    }

}
