package cn.youngkaaa.yviewpager;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.support.v4.view.AbsSavedState;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.KeyEventCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.view.accessibility.AccessibilityRecordCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.FocusFinder;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Interpolator;
import android.widget.Scroller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class YViewPager extends ViewGroup {
    private static final String TAG = "YViewPager";
    private static final boolean DEBUG = false;
    private static final boolean USE_CACHE = false;
    private static final int DEFAULT_OFFSCREEN_PAGES = 1;
    private static final int MAX_SETTLE_DURATION = 600; // ms
    private static final int MIN_DISTANCE_FOR_FLING = 25; // dips
    private static final int DEFAULT_GUTTER_SIZE = 16; // dips
    private static final int MIN_FLING_VELOCITY = 400; // dips

    private static final int[] LAYOUT_ATTRS = new int[]{
            android.R.attr.layout_gravity
    };
    private static final int DEFAULT_OFFSCREEN_SCROLL_COUNT = 500;

    /**
     * Used to track what the expected number of items in the adapter should be.
     * If the app changes this when we don't expect it, we'll throw a big obnoxious exception.
     */
    private int mExpectedAdapterCount;


    private int mDestY;
    private int mVelocityY = 100;
    private boolean isCirculatory = false;
    private int mAdapterCirculatoryCount = DEFAULT_OFFSCREEN_SCROLL_COUNT;
    private boolean isVertical = false;
    private int mCurrentStartPos = 0;

    static class ItemInfo {
        Object object;
        int position;
        boolean scrolling;
        float widthFactor;
        float offset;

        @Override
        public String toString() {
            return "ItemInfo{" +
                    "object=" + object +
                    ", position=" + position +
                    ", scrolling=" + scrolling +
                    ", widthFactor=" + widthFactor +
                    ", offset=" + offset +
                    '}';
        }
    }

    private static final Comparator<ItemInfo> COMPARATOR = new Comparator<ItemInfo>() {
        @Override
        public int compare(YViewPager.ItemInfo lhs, YViewPager.ItemInfo rhs) {
            return lhs.position - rhs.position;
        }
    };

    private static final Interpolator sInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    private final ArrayList<ItemInfo> mItems = new ArrayList<ItemInfo>();
    private final YViewPager.ItemInfo mTempItem = new YViewPager.ItemInfo();
    private final Rect mTempRect = new Rect();
    private YPagerAdapter mAdapter;
    private int mCurItem;   // Index of currently displayed page.
    private int mRestoredCurItem = -1;
    private Parcelable mRestoredAdapterState = null;
    private ClassLoader mRestoredClassLoader = null;
    private Scroller mScroller;
    private boolean mIsScrollStarted;
    private PagerObserver mObserver;
    private int mPageMargin;
    private Drawable mMarginDrawable;

    //for horizontal
    private int mTopPageBounds;
    private int mBottomPageBounds;

    //for vertical
    private int mLeftPageBounds;
    private int mRightPageBounds;

    // Offsets of the first and last items, if known.
    // Set during population, used to determine if we are at the beginning
    // or end of the pager data set during touch scrolling.
    private float mFirstOffset = -Float.MAX_VALUE;
    private float mLastOffset = Float.MAX_VALUE;

    private int mChildWidthMeasureSpec;
    private int mChildHeightMeasureSpec;
    private boolean mInLayout;

    private boolean mScrollingCacheEnabled;

    private boolean mPopulatePending;
    private int mOffscreenPageLimit = DEFAULT_OFFSCREEN_PAGES;

    private boolean mIsBeingDragged;
    private boolean mIsUnableToDrag;
    private int mDefaultGutterSize;
    private int mGutterSize;
    private int mTouchSlop;
    /**
     * Position of the last motion event.
     */
    private float mLastMotionX;
    private float mLastMotionY;
    private float mInitialMotionX;
    private float mInitialMotionY;
    /**
     * ID of the active pointer. This is used to retain consistency during
     * drags/flings if multiple pointers are used.
     */
    private int mActivePointerId = INVALID_POINTER;
    /**
     * Sentinel value for no current active pointer.
     * Used by {@link #mActivePointerId}.
     */
    private static final int INVALID_POINTER = -1;

    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private int mFlingDistance;
    private int mCloseEnough;

    // If the pager is at least this close to its final position, complete the scroll
    // on touch down and let the user interact with the content inside instead of
    // "catching" the flinging pager.
    private static final int CLOSE_ENOUGH = 2; // dp

    private boolean mFakeDragging;
    private long mFakeDragBeginTime;

    private EdgeEffectCompat mLeftEdge;
    private EdgeEffectCompat mRightEdge;
    private EdgeEffectCompat mTopEdge;
    private EdgeEffectCompat mBottomEdge;

    private boolean mFirstLayout = true;
    private boolean mNeedCalculatePageOffsets = false;
    private boolean mCalledSuper;
    private int mDecorChildCount;

    private List<OnPageChangeListener> mOnPageChangeListeners;
    private OnPageChangeListener mOnPageChangeListener;
    private OnPageChangeListener mInternalPageChangeListener;
    private List<OnAdapterChangeListener> mAdapterChangeListeners;
    private YViewPager.PageTransformer mPageTransformer;
    private Method mSetChildrenDrawingOrderEnabled;

    private static final int DRAW_ORDER_DEFAULT = 0;
    private static final int DRAW_ORDER_FORWARD = 1;
    private static final int DRAW_ORDER_REVERSE = 2;
    private int mDrawingOrder;
    private ArrayList<View> mDrawingOrderedChildren;
    private static final ViewPositionComparator sPositionComparator = new ViewPositionComparator();

    public static final int VERTICAL = 0;
    public static final int HORIZONTAL = 1;

    /**
     * Indicates that the pager is in an idle, settled state. The current page
     * is fully in view and no animation is in progress.
     */
    public static final int SCROLL_STATE_IDLE = 0;

    /**
     * Indicates that the pager is currently being dragged by the user.
     */
    public static final int SCROLL_STATE_DRAGGING = 1;

    /**
     * Indicates that the pager is in the process of settling to a final position.
     */
    public static final int SCROLL_STATE_SETTLING = 2;


    private final Runnable mEndScrollRunnable = new Runnable() {
        @Override
        public void run() {
            setScrollState(SCROLL_STATE_IDLE);
            if (isVertical) {
                populateVertical();
            } else {
                populateHorizontal();
            }
        }
    };

    private int mScrollState = SCROLL_STATE_IDLE;


    /**
     * Callback interface for responding to changing state of the selected page.
     */
    public interface OnPageChangeListener {

        /**
         * This method will be invoked when the current page is scrolled, either as part
         * of a programmatically initiated smooth scroll or a user initiated touch scroll.
         *
         * @param position             Position index of the first page currently being displayed.
         *                             Page position+1 will be visible if positionOffset is nonzero.
         * @param positionOffset       Value from [0, 1) indicating the offset from the page at position.
         * @param positionOffsetPixels Value in pixels indicating the offset from position.
         */
        void onPageScrolled(int position, float positionOffset, int positionOffsetPixels);

        /**
         * This method will be invoked when a new page becomes selected. Animation is not
         * necessarily complete.
         *
         * @param position Position index of the new selected page.
         */
        void onPageSelected(int position);

        /**
         * Called when the scroll state changes. Useful for discovering when the user
         * begins dragging, when the pager is automatically settling to the current page,
         * or when it is fully stopped/idle.
         *
         * @param state The new scroll state.
         * @see YViewPager#SCROLL_STATE_IDLE
         * @see YViewPager#SCROLL_STATE_DRAGGING
         * @see YViewPager#SCROLL_STATE_SETTLING
         */
        void onPageScrollStateChanged(int state);
    }

    /**
     * Simple implementation of the {@link YViewPager.OnPageChangeListener} interface with stub
     * implementations of each method. Extend this if you do not intend to override
     * every method of {@link YViewPager.OnPageChangeListener}.
     */
    public static class SimpleOnPageChangeListener implements YViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // This space for rent
        }

        @Override
        public void onPageSelected(int position) {
            // This space for rent
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // This space for rent
        }
    }


    public interface PageTransformer {
        /**
         * Apply a property transformation to the given page.
         *
         * @param page     Apply the transformation to this page
         * @param position Position of page relative to the current front-and-center
         *                 position of the pager. 0 is front and center. 1 is one full
         *                 page position to the right, and -1 is one page position to the left.
         */
        void transformPage(View page, float position);
    }

    /**
     * Callback interface for responding to adapter changes.
     */
    public interface OnAdapterChangeListener {
        /**
         * Called when the adapter for the given view pager has changed.
         */
        void onAdapterChanged(YViewPager viewPager,
                              YPagerAdapter oldAdapter, YPagerAdapter newAdapter);
    }


    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    public @interface DecorView {
    }

    public YViewPager(Context context) {
        super(context);
        initStyle(context, null);
        initViewPager();
    }

    public YViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        initStyle(context, attrs);
        initViewPager();
    }

    private void initStyle(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.YViewPager);
        int direction = array.getInt(R.styleable.YViewPager_orientation, HORIZONTAL);
        isCirculatory = array.getBoolean(R.styleable.YViewPager_circulatory, false);
        mAdapterCirculatoryCount = array.getInt(R.styleable.YViewPager_circulationCount, DEFAULT_OFFSCREEN_SCROLL_COUNT);
        mCurrentStartPos = mAdapterCirculatoryCount / 2;

        if (direction == HORIZONTAL) {
            isVertical = false;
        } else if (direction == VERTICAL) {
            isVertical = true;
        }
        array.recycle();
    }

    void initViewPager() {
        setWillNotDraw(false);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setFocusable(true);
        final Context context = getContext();
        mScroller = new Scroller(context, sInterpolator);
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        final float density = context.getResources().getDisplayMetrics().density;

        mTouchSlop = configuration.getScaledPagingTouchSlop();
        mMinimumVelocity = (int) (MIN_FLING_VELOCITY * density);
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mLeftEdge = new EdgeEffectCompat(context);
        mRightEdge = new EdgeEffectCompat(context);
        mTopEdge = new EdgeEffectCompat(context);
        mBottomEdge = new EdgeEffectCompat(context);

        mFlingDistance = (int) (MIN_DISTANCE_FOR_FLING * density);
        mCloseEnough = (int) (CLOSE_ENOUGH * density);
        mDefaultGutterSize = (int) (DEFAULT_GUTTER_SIZE * density);

        ViewCompat.setAccessibilityDelegate(this, new YViewPager.MyAccessibilityDelegate());

        if (ViewCompat.getImportantForAccessibility(this)
                == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            ViewCompat.setImportantForAccessibility(this,
                    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }

        ViewCompat.setOnApplyWindowInsetsListener(this,
                new android.support.v4.view.OnApplyWindowInsetsListener() {
                    private final Rect mTempRect = new Rect();

                    @Override
                    public WindowInsetsCompat onApplyWindowInsets(final View v,
                                                                  final WindowInsetsCompat originalInsets) {
                        // First let the ViewPager itself try and consume them...
                        final WindowInsetsCompat applied =
                                ViewCompat.onApplyWindowInsets(v, originalInsets);
                        if (applied.isConsumed()) {
                            // If the ViewPager consumed all insets, return now
                            return applied;
                        }

                        // Now we'll manually dispatch the insets to our children. Since ViewPager
                        // children are always full-height, we do not want to use the standard
                        // ViewGroup dispatchApplyWindowInsets since if child 0 consumes them,
                        // the rest of the children will not receive any insets. To workaround this
                        // we manually dispatch the applied insets, not allowing children to
                        // consume them from each other. We do however keep track of any insets
                        // which are consumed, returning the union of our children's consumption
                        final Rect res = mTempRect;
                        res.left = applied.getSystemWindowInsetLeft();
                        res.top = applied.getSystemWindowInsetTop();
                        res.right = applied.getSystemWindowInsetRight();
                        res.bottom = applied.getSystemWindowInsetBottom();

                        for (int i = 0, count = getChildCount(); i < count; i++) {
                            final WindowInsetsCompat childInsets = ViewCompat
                                    .dispatchApplyWindowInsets(getChildAt(i), applied);
                            // Now keep track of any consumed by tracking each dimension's min
                            // value
                            res.left = Math.min(childInsets.getSystemWindowInsetLeft(),
                                    res.left);
                            res.top = Math.min(childInsets.getSystemWindowInsetTop(),
                                    res.top);
                            res.right = Math.min(childInsets.getSystemWindowInsetRight(),
                                    res.right);
                            res.bottom = Math.min(childInsets.getSystemWindowInsetBottom(),
                                    res.bottom);
                        }

                        // Now return a new WindowInsets, using the consumed window insets
                        return applied.replaceSystemWindowInsets(
                                res.left, res.top, res.right, res.bottom);
                    }
                });
    }


    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(mEndScrollRunnable);
        // To be on the safe side, abort the scroller
        if ((mScroller != null) && !mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
        super.onDetachedFromWindow();
    }

    private void setScrollState(int newState) {
        if (mScrollState == newState) {
            return;
        }

        mScrollState = newState;
        if (mPageTransformer != null) {
            // PageTransformers can do complex things that benefit from hardware layers.
            enableLayers(newState != SCROLL_STATE_IDLE);
        }
        dispatchOnScrollStateChanged(newState);
    }

    /**
     * Set a PagerAdapter that will supply views for this pager as needed.
     *
     * @param adapter Adapter to use
     */
    public void setAdapter(YPagerAdapter adapter) {
        Log.d(TAG, "setAdapter() in");
        if (mAdapter != null) {
            mAdapter.setViewPagerObserver(null);
            mAdapter.startUpdate(this);
            for (int i = 0; i < mItems.size(); i++) {
                final ItemInfo ii = mItems.get(i);
                mAdapter.destroyItem(this, ii.position, ii.object);
            }
            mAdapter.finishUpdate(this);
            mItems.clear();
            removeNonDecorViews();
//            mCurItem = 0;
            mCurItem = isCirculatory ? getAdapterCount() / 2 : 0;
            Log.d(TAG, "setAdapter() mCurItem1=>" + mCurItem);
            scrollTo(0, 0);
        }

        final YPagerAdapter oldAdapter = mAdapter;
        mAdapter = adapter;
        mExpectedAdapterCount = 0;
        if (mAdapter != null) {
            if (mObserver == null) {
                mObserver = new PagerObserver();
            }
            mAdapter.setViewPagerObserver(mObserver);
            mPopulatePending = false;
            final boolean wasFirstLayout = mFirstLayout;
            mFirstLayout = true;
            mExpectedAdapterCount = getAdapterCount();
//            mExpectedAdapterCount = mAdapter.getCount();
            mCurItem = isCirculatory ? getAdapterCount() / 2 : 0;
            Log.d(TAG, "setAdapter() mCurItem2=>" + mCurItem);
            if (mRestoredCurItem >= 0) {
                mAdapter.restoreState(mRestoredAdapterState, mRestoredClassLoader);
                if (isVertical) {
                    setCurrentItemInternalVertical(mRestoredCurItem, false, true);
                } else {
                    setCurrentItemInternalHorizontal(mRestoredCurItem, false, true);
                }
                mRestoredCurItem = -1;
                mRestoredAdapterState = null;
                mRestoredClassLoader = null;
            } else if (!wasFirstLayout) {
                if (isVertical) {
                    populateVertical();
                } else {
                    populateHorizontal();
                }
            } else {
                requestLayout();
            }
        }

        // Dispatch the change to any listeners
        if (mAdapterChangeListeners != null && !mAdapterChangeListeners.isEmpty()) {
            for (int i = 0, count = mAdapterChangeListeners.size(); i < count; i++) {
                mAdapterChangeListeners.get(i).onAdapterChanged(this, oldAdapter, adapter);
            }
        }
    }

    private int getAdapterCount() {
        if (isCirculatory) {
            return mAdapterCirculatoryCount;
        } else {
            return mAdapter.getCount();
        }
    }

    private void removeNonDecorViews() {
        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (!lp.isDecor) {
                removeViewAt(i);
                i--;
            }
        }
    }

    /**
     * Retrieve the current adapter supplying pages.
     *
     * @return The currently registered PagerAdapter
     */
    public YPagerAdapter getAdapter() {
        return mAdapter;
    }

    /**
     * Add a listener that will be invoked whenever the adapter for this ViewPager changes.
     *
     * @param listener listener to add
     */
    public void addOnAdapterChangeListener(YViewPager.OnAdapterChangeListener listener) {
        if (mAdapterChangeListeners == null) {
            mAdapterChangeListeners = new ArrayList<>();
        }
        mAdapterChangeListeners.add(listener);
    }

    /**
     * Remove a listener that was previously added via
     * {@link #addOnAdapterChangeListener(YViewPager.OnAdapterChangeListener)}.
     *
     * @param listener listener to remove
     */
    public void removeOnAdapterChangeListener(YViewPager.OnAdapterChangeListener listener) {
        if (mAdapterChangeListeners != null) {
            mAdapterChangeListeners.remove(listener);
        }
    }


    private int getClientWidth() {
        return getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
    }

    private int getClientHeight() {
        return getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
    }

    /**
     * Set the currently selected page. If the ViewPager has already been through its first
     * layout with its current adapter there will be a smooth animated transition between
     * the current item and the specified item.
     *
     * @param item Item index to select
     */
    public void setCurrentItem(int item) {
        mPopulatePending = false;
        if (item >= mAdapter.getCount()) {
            throw new IllegalArgumentException("adapter's count is" + mAdapter.getCount() + ",but you set the position" +
                    "is:" + item + ",it bigger than the max count");
        } else if (item < 0) {
            throw new IllegalArgumentException("you set the position" +
                    "is:" + item + ",it less than 0");
        }
        int newPos = 0;
        if (mCurrentStartPos == 0) {  //init status
            newPos = isCirculatory ? getAdapterCount() / 2 + item : item;
        } else {   //already scrolled status
            newPos = isCirculatory ? mCurrentStartPos + item : item;
        }
        if (isVertical) {
            setCurrentItemInternalVertical(newPos, !mFirstLayout, false);
        } else {
            setCurrentItemInternalHorizontal(newPos, !mFirstLayout, false);
        }
    }

    /**
     * Set the currently selected page.
     *
     * @param item         Item index to select
     * @param smoothScroll True to smoothly scroll to the new item, false to transition immediately
     */
    public void setCurrentItem(int item, boolean smoothScroll) {
        mPopulatePending = false;
        if (item >= mAdapter.getCount()) {
            throw new IllegalArgumentException("adapter's count is:" + mAdapter.getCount() + ",but you set the position" +
                    "is:" + item + ",it bigger than the max count");
        } else if (item < 0) {
            throw new IllegalArgumentException("you set the position" +
                    "is:" + item + ",it less than 0");
        }
        int newPos = 0;
        if (mCurrentStartPos == 0) {  //init status
            newPos = isCirculatory ? getAdapterCount() / 2 + item : item;
        } else {   //already scrolled status
            newPos = isCirculatory ? mCurrentStartPos + item : item;
        }
        if (isVertical) {
            setCurrentItemInternalVertical(newPos, smoothScroll, false);
        } else {
            setCurrentItemInternalHorizontal(newPos, smoothScroll, false);
        }
    }

    public int getCurrentItem() {
        int finalPos = isCirculatory ? mCurItem % mAdapter.getCount() : mCurItem;
        return finalPos;
    }

    void setCurrentItemInternalHorizontal(int item, boolean smoothScroll, boolean always) {
        setCurrentItemInternalHorizontal(item, smoothScroll, always, 0);
    }

    void setCurrentItemInternalVertical(int item, boolean smoothScroll, boolean always) {
        setCurrentItemInternalVertical(item, smoothScroll, always, 0);
    }

    void setCurrentItemInternalHorizontal(int item, boolean smoothScroll, boolean always, int velocity) {
        if (mAdapter == null || getAdapterCount() <= 0) {
            setScrollingCacheEnabled(false);
            return;
        }
        if (!always && mCurItem == item && mItems.size() != 0) {
            setScrollingCacheEnabled(false);
            return;
        }

        if (item < 0) {
            item = 0;
        } else if (item >= getAdapterCount()) {
            item = getAdapterCount() - 1;
        }
        final int pageLimit = mOffscreenPageLimit;
        if (item > (mCurItem + pageLimit) || item < (mCurItem - pageLimit)) {
            // We are doing a jump by more than one page.  To avoid
            // glitches, we want to keep all current pages in the view
            // until the scroll ends.
            for (int i = 0; i < mItems.size(); i++) {
                mItems.get(i).scrolling = true;
            }
        }
        final boolean dispatchSelected = mCurItem != item;

        if (mFirstLayout) {
            // We don't have any idea how big we are yet and shouldn't have any pages either.
            // Just set things up and let the pending layout handle things.
            mCurItem = item;
            if (dispatchSelected) {
                dispatchOnPageSelected(item);
            }
            requestLayout();
        } else {
            populateHorizontal(item);
            scrollToItemHorizontal(item, smoothScroll, velocity, dispatchSelected);
        }
    }

    void setCurrentItemInternalVertical(int item, boolean smoothScroll, boolean always, int velocity) {
        if (mAdapter == null || getAdapterCount() <= 0) {
            setScrollingCacheEnabled(false);
            return;
        }
        if (!always && mCurItem == item && mItems.size() != 0) {
            setScrollingCacheEnabled(false);
            return;
        }

        if (item < 0) {
            item = 0;
        } else if (item >= getAdapterCount()) {
            item = getAdapterCount() - 1;
        }
        final int pageLimit = mOffscreenPageLimit;
        if (item > (mCurItem + pageLimit) || item < (mCurItem - pageLimit)) {
            // We are doing a jump by more than one page.  To avoid
            // glitches, we want to keep all current pages in the view
            // until the scroll ends.
            for (int i = 0; i < mItems.size(); i++) {
                mItems.get(i).scrolling = true;
            }
        }
        final boolean dispatchSelected = mCurItem != item;

        if (mFirstLayout) {
            // We don't have any idea how big we are yet and shouldn't have any pages either.
            // Just set things up and let the pending layout handle things.
            mCurItem = item;
            if (dispatchSelected) {
                dispatchOnPageSelected(item);
            }
            requestLayout();
        } else {
            populateVertical(item);
            scrollToItemVertical(item, smoothScroll, velocity, dispatchSelected);
        }
    }


    private void scrollToItemHorizontal(int item, boolean smoothScroll, int velocity,
                                        boolean dispatchSelected) {
        final ItemInfo curInfo = infoForPosition(item);
        int destX = 0;
        if (curInfo != null) {
            final int width = getClientWidth();
            destX = (int) (width * Math.max(mFirstOffset,
                    Math.min(curInfo.offset, mLastOffset)));
        }

        if (smoothScroll) {
            smoothScrollToHorizontal(destX, 0, velocity);
            if (dispatchSelected) {
                dispatchOnPageSelected(item);
            }
        } else {
            if (dispatchSelected) {
                dispatchOnPageSelected(item);
            }
            completeScrollHorizontal(false);
            scrollTo(destX, 0);
            pageScrolledHorizontal(destX);
        }
    }

    private void scrollToItemVertical(int item, boolean smoothScroll, int velocity,
                                      boolean dispatchSelected) {
        final ItemInfo curInfo = infoForPosition(item);
        int destY = 0;
        if (curInfo != null) {
            final int height = getClientHeight();
            destY = (int) (height * Math.max(mFirstOffset,
                    Math.min(curInfo.offset, mLastOffset)));
            mDestY = destY;
        }

        if (smoothScroll) {
            smoothScrollToVertical(0, destY, velocity);
            if (dispatchSelected) {
                dispatchOnPageSelected(item);
            }
        } else {
            if (dispatchSelected) {
                dispatchOnPageSelected(item);
            }
            completeScrollVertical(false);
            scrollTo(0, destY);
            pageScrolledVertical(destY);
        }
    }

    /**
     * Set a listener that will be invoked whenever the page changes or is incrementally
     * scrolled. See {@link YViewPager.OnPageChangeListener}.
     *
     * @param listener Listener to set
     * @deprecated Use {@link #addOnPageChangeListener(YViewPager.OnPageChangeListener)}
     * and {@link #removeOnPageChangeListener(YViewPager.OnPageChangeListener)} instead.
     */
    @Deprecated
    public void setOnPageChangeListener(YViewPager.OnPageChangeListener listener) {
        mOnPageChangeListener = listener;
    }


    public void addOnPageChangeListener(YViewPager.OnPageChangeListener listener) {
        if (mOnPageChangeListeners == null) {
            mOnPageChangeListeners = new ArrayList<>();
        }
        mOnPageChangeListeners.add(listener);
    }

    /**
     * Remove a listener that was previously added via
     * {@link #addOnPageChangeListener(YViewPager.OnPageChangeListener)}.
     *
     * @param listener listener to remove
     */
    public void removeOnPageChangeListener(YViewPager.OnPageChangeListener listener) {
        if (mOnPageChangeListeners != null) {
            mOnPageChangeListeners.remove(listener);
        }
    }

    /**
     * Remove all listeners that are notified of any changes in scroll state or position.
     */
    public void clearOnPageChangeListeners() {
        if (mOnPageChangeListeners != null) {
            mOnPageChangeListeners.clear();
        }
    }


    public void setPageTransformer(boolean reverseDrawingOrder, YViewPager.PageTransformer transformer) {
        if (Build.VERSION.SDK_INT >= 11) {
            final boolean hasTransformer = transformer != null;
            final boolean needsPopulate = hasTransformer != (mPageTransformer != null);
            mPageTransformer = transformer;
            setChildrenDrawingOrderEnabledCompat(hasTransformer);
            if (hasTransformer) {
                mDrawingOrder = reverseDrawingOrder ? DRAW_ORDER_REVERSE : DRAW_ORDER_FORWARD;
            } else {
                mDrawingOrder = DRAW_ORDER_DEFAULT;
            }
            if (!isVertical && needsPopulate) {
                populateHorizontal();
            } else if (isVertical && needsPopulate) {
                populateVertical();
            }
        }
    }

    void setChildrenDrawingOrderEnabledCompat(boolean enable) {
        if (Build.VERSION.SDK_INT >= 7) {
            if (mSetChildrenDrawingOrderEnabled == null) {
                try {
                    mSetChildrenDrawingOrderEnabled = ViewGroup.class.getDeclaredMethod(
                            "setChildrenDrawingOrderEnabled", new Class[]{Boolean.TYPE});
                } catch (NoSuchMethodException e) {
                    Log.e(TAG, "Can't find setChildrenDrawingOrderEnabled", e);
                }
            }
            try {
                mSetChildrenDrawingOrderEnabled.invoke(this, enable);
            } catch (Exception e) {
                Log.e(TAG, "Error changing children drawing order", e);
            }
        }
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        final int index = mDrawingOrder == DRAW_ORDER_REVERSE ? childCount - 1 - i : i;
        final int result =
                ((YViewPager.LayoutParams) mDrawingOrderedChildren.get(index).getLayoutParams()).childIndex;
        return result;
    }

    /**
     * Set a separate OnPageChangeListener for internal use by the support library.
     *
     * @param listener Listener to set
     * @return The old listener that was set, if any.
     */
    YViewPager.OnPageChangeListener setInternalPageChangeListener(YViewPager.OnPageChangeListener listener) {
        YViewPager.OnPageChangeListener oldListener = mInternalPageChangeListener;
        mInternalPageChangeListener = listener;
        return oldListener;
    }

    /**
     * Returns the number of pages that will be retained to either side of the
     * current page in the view hierarchy in an idle state. Defaults to 1.
     *
     * @return How many pages will be kept offscreen on either side
     * @see #setOffscreenPageLimit(int)
     */
    public int getOffscreenPageLimit() {
        return mOffscreenPageLimit;
    }


    public void setOffscreenPageLimit(int limit) {
        if (limit < DEFAULT_OFFSCREEN_PAGES) {
            Log.w(TAG, "Requested offscreen page limit " + limit + " too small; defaulting to "
                    + DEFAULT_OFFSCREEN_PAGES);
            limit = DEFAULT_OFFSCREEN_PAGES;
        }
        if (limit != mOffscreenPageLimit) {
            mOffscreenPageLimit = limit;
            if (isVertical) {
                populateVertical();
            } else {
                populateHorizontal();
            }
        }
    }

    /**
     * Set the margin between pages.
     *
     * @param marginPixels Distance between adjacent pages in pixels
     * @see #getPageMargin()
     * @see #setPageMarginDrawable(Drawable)
     * @see #setPageMarginDrawable(int)
     */
    public void setPageMargin(int marginPixels) {
        final int oldMargin = mPageMargin;
        mPageMargin = marginPixels;

        final int width = getWidth();
        if (isVertical) {
            recomputeScrollPositionVertical(getHeight(), getHeight(), marginPixels, oldMargin);
        } else {
            recomputeScrollPositionHorizontal(width, width, marginPixels, oldMargin);
        }

        requestLayout();
    }

    /**
     * Return the margin between pages.
     *
     * @return The size of the margin in pixels
     */
    public int getPageMargin() {
        return mPageMargin;
    }

    /**
     * Set a drawable that will be used to fill the margin between pages.
     *
     * @param d Drawable to display between pages
     */
    public void setPageMarginDrawable(Drawable d) {
        mMarginDrawable = d;
        if (d != null) refreshDrawableState();
        setWillNotDraw(d == null);
        invalidate();
    }

    /**
     * Set a drawable that will be used to fill the margin between pages.
     *
     * @param resId Resource ID of a drawable to display between pages
     */
    public void setPageMarginDrawable(int resId) {
        setPageMarginDrawable(getContext().getResources().getDrawable(resId));
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == mMarginDrawable;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        final Drawable d = mMarginDrawable;
        if (d != null && d.isStateful()) {
            d.setState(getDrawableState());
        }
    }

    // We want the duration of the page snap animation to be influenced by the distance that
    // the screen has to travel, however, we don't want this duration to be effected in a
    // purely linear fashion. Instead, we use this method to moderate the effect that the distance
    // of travel has on the overall snap duration.
    float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5f; // center the values about 0.
        f *= 0.3f * Math.PI / 2.0f;
        return (float) Math.sin(f);
    }

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param x the number of pixels to scroll by on the X axis
     * @param y the number of pixels to scroll by on the Y axis
     */
//    void smoothScrollTo(int x, int y) {
//        smoothScrollTo(x, y, 0);
//    }

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param x        the number of pixels to scroll by on the X axis
     * @param y        the number of pixels to scroll by on the Y axis
     * @param velocity the velocity associated with a fling, if applicable. (0 otherwise)
     */
    void smoothScrollToHorizontal(int x, int y, int velocity) {
        if (getChildCount() == 0) {
            // Nothing to do.
            setScrollingCacheEnabled(false);
            return;
        }
        int sx;
        boolean wasScrolling = (mScroller != null) && !mScroller.isFinished();
        if (wasScrolling) {
            // We're in the middle of a previously initiated scrolling. Check to see
            // whether that scrolling has actually started (if we always call getStartX
            // we can get a stale value from the scroller if it hadn't yet had its first
            // computeScrollOffset call) to decide what is the current scrolling position.

            sx = mIsScrollStarted ? mScroller.getCurrX() : mScroller.getStartX();
            // And abort the current scrolling.

            mScroller.abortAnimation();
            setScrollingCacheEnabled(false);
        } else {
            sx = getScrollX();
        }

        int sy = getScrollY();
        int dx = x - sx;
        int dy = y - sy;
        if (dx == 0 && dy == 0) {
            // TODO: 2017/2/11  completeScroll
            completeScrollHorizontal(false);
            populateHorizontal();
            setScrollState(SCROLL_STATE_IDLE);
            return;
        }

        setScrollingCacheEnabled(true);
        setScrollState(SCROLL_STATE_SETTLING);

        final int width = getClientWidth();
        final int halfWidth = width / 2;
        final float distanceRatio = Math.min(1f, 1.0f * Math.abs(dx) / width);
        final float distance = halfWidth + halfWidth
                * distanceInfluenceForSnapDuration(distanceRatio);
        int duration;
        velocity = Math.abs(velocity);
        if (velocity > 0) {
            duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
        } else {
            final float pageWidth = width * mAdapter.getPageWidth(mCurItem);
            final float pageDelta = (float) Math.abs(dx) / (pageWidth + mPageMargin);
            duration = (int) ((pageDelta + 1) * 100);
        }
        duration = Math.min(duration, MAX_SETTLE_DURATION);

        // Reset the "scroll started" flag. It will be flipped to true in all places
        // where we call computeScrollOffset().
        mIsScrollStarted = false;
        mScroller.startScroll(sx, sy, dx, dy, duration);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    void smoothScrollToVertical(int x, int y, int velocity) {
        if (getChildCount() == 0) {
            // Nothing to do.
            setScrollingCacheEnabled(false);
            return;
        }
        int sy;
        boolean wasScrolling = (mScroller != null) && !mScroller.isFinished();
        if (wasScrolling) {
            // We're in the middle of a previously initiated scrolling. Check to see
            // whether that scrolling has actually started (if we always call getStartX
            // we can get a stale value from the scroller if it hadn't yet had its first
            // computeScrollOffset call) to decide what is the current scrolling position.

            sy = mIsScrollStarted ? mScroller.getCurrY() : mScroller.getStartY();

            // And abort the current scrolling.

            mScroller.abortAnimation();
            setScrollingCacheEnabled(false);
        } else {
            //getScrollX() http://www.bubuko.com/infodetail-916594.html
            sy = getScrollY();
        }

        int sx = getScrollX();
        int dx = x - sx;
        int dy = y - sy;
        if (dx == 0 && dy == 0) {
            // TODO: 2017/2/11  completeScroll
            completeScrollVertical(false);
            populateVertical();
            setScrollState(SCROLL_STATE_IDLE);
            return;
        }

        setScrollingCacheEnabled(true);
        setScrollState(SCROLL_STATE_SETTLING);

        final int height = getClientHeight();
        final int halfHeight = height / 2;
        final float distanceRatio = Math.min(1f, 1.0f * Math.abs(dy) / height);
        final float distance = halfHeight + halfHeight
                * distanceInfluenceForSnapDuration(distanceRatio);

        int duration;
        velocity = Math.abs(velocity);
        if (velocity > 0) {
            duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
        } else {
            final float pageHeight = height * mAdapter.getPageWidth(mCurItem);
            final float pageDelta = (float) Math.abs(dy) / (pageHeight + mPageMargin);
            duration = (int) ((pageDelta + 1) * 100);
        }
        duration = Math.min(duration, MAX_SETTLE_DURATION);

        // Reset the "scroll started" flag. It will be flipped to true in all places
        // where we call computeScrollOffset().
        mIsScrollStarted = false;
        mScroller.startScroll(sx, sy, dx, dy, duration);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    private int mTotalNum = Integer.MAX_VALUE;

    ItemInfo addNewItem(int position, int index) {
        int newPos = position % mAdapter.getCount();
        Log.d(TAG, "addNewItem newPos=>" + newPos + ",position=>" + position);
        if (newPos == 0) {
            mCurrentStartPos = position;
            Log.d(TAG, "addNewItem mCurrentStartPos=>" + mCurrentStartPos);
        }
        ItemInfo ii = new ItemInfo();
        ii.position = position;
        ii.object = mAdapter.instantiateItem(this, newPos);
        ii.widthFactor = mAdapter.getPageWidth(newPos);
        if (index < 0 || index >= mItems.size()) {
            mItems.add(ii);
        } else {
            mItems.add(index, ii);
        }
        return ii;
    }

    void dataSetChanged() {
        // This method only gets called if our observer is attached, so mAdapter is non-null.

        final int adapterCount = getAdapterCount();
        mExpectedAdapterCount = adapterCount;
        boolean needPopulate = mItems.size() < mOffscreenPageLimit * 2 + 1
                && mItems.size() < adapterCount;
        int newCurrItem = mCurItem;

        boolean isUpdating = false;
        for (int i = 0; i < mItems.size(); i++) {
            final YViewPager.ItemInfo ii = mItems.get(i);
            final int newPos = mAdapter.getItemPosition(ii.object);

            if (newPos == YPagerAdapter.POSITION_UNCHANGED) {
                continue;
            }

            if (newPos == YPagerAdapter.POSITION_NONE) {
                mItems.remove(i);
                i--;

                if (!isUpdating) {
                    mAdapter.startUpdate(this);
                    isUpdating = true;
                }

                mAdapter.destroyItem(this, ii.position, ii.object);
                needPopulate = true;

                if (mCurItem == ii.position) {
                    // Keep the current item in the valid range
                    newCurrItem = Math.max(0, Math.min(mCurItem, adapterCount - 1));
                    needPopulate = true;
                }
                continue;
            }

            if (ii.position != newPos) {
                if (ii.position == mCurItem) {
                    // Our current item changed position. Follow it.
                    newCurrItem = newPos;
                }

                ii.position = newPos;
                needPopulate = true;
            }
        }

        if (isUpdating) {
            mAdapter.finishUpdate(this);
        }

        Collections.sort(mItems, COMPARATOR);

        if (needPopulate) {
            // Reset our known page widths; populate will recompute them.
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                final YViewPager.LayoutParams lp = (YViewPager.LayoutParams) child.getLayoutParams();
                if (!lp.isDecor) {
                    lp.widthFactor = 0.f;
                }
            }
            if (isVertical) {
                setCurrentItemInternalVertical(newCurrItem, false, true);
            } else {
                setCurrentItemInternalHorizontal(newCurrItem, false, true);
            }
            requestLayout();
        }
    }

    void populateHorizontal() {
        populateHorizontal(mCurItem);
    }

    void populateVertical() {
        populateVertical(mCurItem);
    }


    void populateHorizontal(int newCurrentItem) {
        ItemInfo oldCurInfo = null;
        if (mCurItem != newCurrentItem) {
            oldCurInfo = infoForPosition(mCurItem);
            mCurItem = newCurrentItem;
        }

        if (mAdapter == null) {
            sortChildDrawingOrder();
            return;
        }

        // Bail now if we are waiting to populate.  This is to hold off
        // on creating views from the time the user releases their finger to
        // fling to a new position until we have finished the scroll to
        // that position, avoiding glitches from happening at that point.
        if (mPopulatePending) {
            if (DEBUG) Log.i(TAG, "populate is pending, skipping for now...");
            sortChildDrawingOrder();
            return;
        }

        // Also, don't populate until we are attached to a window.  This is to
        // avoid trying to populate before we have restored our view hierarchy
        // state and conflicting with what is restored.
        if (getWindowToken() == null) {
            return;
        }

        mAdapter.startUpdate(this);
        final int pageLimit = mOffscreenPageLimit;

        //calculate the startPos,and the for-calculating while do when pos>=startPos
        int startPos = Math.max(0, mCurItem - pageLimit);
        final int N = getAdapterCount();
        final int endPos = Math.min(N - 1, mCurItem + pageLimit);
        if (N != mExpectedAdapterCount) {
            String resName;
            try {
                resName = getResources().getResourceName(getId());
            } catch (Resources.NotFoundException e) {
                resName = Integer.toHexString(getId());
            }
            throw new IllegalStateException("The application's PagerAdapter changed the adapter's"
                    + " contents without calling PagerAdapter#notifyDataSetChanged!"
                    + " Expected adapter item count: " + mExpectedAdapterCount + ", found: " + N
                    + " Pager id: " + resName
                    + " Pager class: " + getClass()
                    + " Problematic adapter: " + mAdapter.getClass());
        }

        // Locate the currently focused item or add it if needed.
        int curIndex = -1;
        ItemInfo curItem = null;
        for (curIndex = 0; curIndex < mItems.size(); curIndex++) {
            final ItemInfo ii = mItems.get(curIndex);
            if (ii.position >= mCurItem) {
                if (ii.position == mCurItem) {
                    curItem = ii;
                }
                break;
            }
        }

        //when we doesn't find the cached-item from the mItems
        //this means that doesn't cached this page,so we add it to mItems
        if (curItem == null && N > 0) {
            Log.d(TAG, "populateHorizontal() mCurItem=>" + mCurItem);
            curItem = addNewItem(mCurItem, curIndex);
        }

        // Fill 3x the available width or up to the number of offscreen
        // pages requested to either side, whichever is larger.
        // If we have no current item we have no work to do.
        if (curItem != null) {
            float extraWidthLeft = 0.f;
            //curIndex is the pos of the current page.
            //we let it -1 means that we calculate from this page to it's left pages
            int itemIndex = curIndex - 1;
            ItemInfo ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
            final int clientWidth = getClientWidth();

            //normally the code : 2.f-curItem.widthFactor may equals the value of 1.f
            //and the (float) getPaddingLeft() / (float) clientWidth always is 0.f
            //so the leftWidthNeeded always is 1.0,which means that we at least calculate one page from current page to left
            final float leftWidthNeeded = clientWidth <= 0 ? 0 :
                    2.f - curItem.widthFactor + (float) getPaddingLeft() / (float) clientWidth;

            for (int pos = mCurItem - 1; pos >= 0; pos--) {
                if (extraWidthLeft >= leftWidthNeeded && pos < startPos) {
                    if (ii == null) {
                        break;
                    }

                    if (pos == ii.position && !ii.scrolling) {
                        mItems.remove(itemIndex);
                        mAdapter.destroyItem(this, pos, ii.object);
                        itemIndex--;
                        curIndex--;
                        ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
                    }
                } else if (ii != null && pos == ii.position) {
                    extraWidthLeft += ii.widthFactor;
                    itemIndex--;
                    ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
                } else {
                    ii = addNewItem(pos, itemIndex + 1);
                    extraWidthLeft += ii.widthFactor;
                    curIndex++;
                    ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
                }
            }

            float extraWidthRight = curItem.widthFactor;
            itemIndex = curIndex + 1;
            if (extraWidthRight < 2.f) {
                ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
                final float rightWidthNeeded = clientWidth <= 0 ? 0 :
                        (float) getPaddingRight() / (float) clientWidth + 2.f;

                for (int pos = mCurItem + 1; pos < N; pos++) {

                    if (extraWidthRight >= rightWidthNeeded && pos > endPos) {
                        if (ii == null) {
                            break;
                        }
                        if (pos == ii.position && !ii.scrolling) {
                            mItems.remove(itemIndex);
                            mAdapter.destroyItem(this, pos, ii.object);
                            ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
                        }
                    } else if (ii != null && pos == ii.position) {
                        extraWidthRight += ii.widthFactor;
                        itemIndex++;
                        ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
                    } else {
                        ii = addNewItem(pos, itemIndex);
                        itemIndex++;
                        extraWidthRight += ii.widthFactor;
                        ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
                    }
                }
            }
            calculatePageOffsetsHorizontal(curItem, curIndex, oldCurInfo);
        }

        if (DEBUG) {
            Log.i(TAG, "Current page list:");
            for (int i = 0; i < mItems.size(); i++) {
                Log.i(TAG, "#" + i + ": page " + mItems.get(i).position);
            }
        }

        //Called to inform the adapter of which item is currently considered to
        //be the "primary", that is the one show to the user as the current page.
        mAdapter.setPrimaryItem(this, mCurItem, curItem != null ? curItem.object : null);

        //Called when the a change in the shown pages has been completed.  At this
        //point you must ensure that all of the pages have actually been added or
        //removed from the container as appropriate.
        mAdapter.finishUpdate(this);

        // Check width measurement of current pages and drawing sort order.
        // Update LayoutParams as needed.
        //
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            lp.childIndex = i;
            if (!lp.isDecor && lp.widthFactor == 0.f) {
                // 0 means requery the adapter for this, it doesn't have a valid width.
                final ItemInfo ii = infoForChild(child);
                if (ii != null) {
                    lp.widthFactor = ii.widthFactor;
                    lp.position = ii.position;
                }
            }
        }

        sortChildDrawingOrder();

        if (hasFocus()) {
            View currentFocused = findFocus();
            ItemInfo ii = currentFocused != null ? infoForAnyChild(currentFocused) : null;
            if (ii == null || ii.position != mCurItem) {
                for (int i = 0; i < getChildCount(); i++) {
                    View child = getChildAt(i);
                    ii = infoForChild(child);
                    if (ii != null && ii.position == mCurItem) {
                        if (child.requestFocus(View.FOCUS_FORWARD)) {
                            break;
                        }
                    }
                }
            }
        }
    }

    void populateVertical(int newCurrentItem) {
        ItemInfo oldCurInfo = null;
        if (mCurItem != newCurrentItem) {
            oldCurInfo = infoForPosition(mCurItem);
            mCurItem = newCurrentItem;
        }

        if (mAdapter == null) {
            sortChildDrawingOrder();
            return;
        }

        // Bail now if we are waiting to populate.  This is to hold off
        // on creating views from the time the user releases their finger to
        // fling to a new position until we have finished the scroll to
        // that position, avoiding glitches from happening at that point.
        if (mPopulatePending) {
            if (DEBUG) Log.i(TAG, "populate is pending, skipping for now...");
            sortChildDrawingOrder();
            return;
        }

        // Also, don't populate until we are attached to a window.  This is to
        // avoid trying to populate before we have restored our view hierarchy
        // state and conflicting with what is restored.
        if (getWindowToken() == null) {
            return;
        }

        mAdapter.startUpdate(this);

        final int pageLimit = mOffscreenPageLimit;
        final int startPos = Math.max(0, mCurItem - pageLimit);

        final int N = getAdapterCount();
        final int endPos = Math.min(N - 1, mCurItem + pageLimit);
        if (N != mExpectedAdapterCount) {
            String resName;
            try {
                resName = getResources().getResourceName(getId());
            } catch (Resources.NotFoundException e) {
                resName = Integer.toHexString(getId());
            }
            throw new IllegalStateException("The application's PagerAdapter changed the adapter's"
                    + " contents without calling PagerAdapter#notifyDataSetChanged!"
                    + " Expected adapter item count: " + mExpectedAdapterCount + ", found: " + N
                    + " Pager id: " + resName
                    + " Pager class: " + getClass()
                    + " Problematic adapter: " + mAdapter.getClass());
        }

        // Locate the currently focused item or add it if needed.
        int curIndex = -1;
        ItemInfo curItem = null;
        for (curIndex = 0; curIndex < mItems.size(); curIndex++) {
            final ItemInfo ii = mItems.get(curIndex);
            if (ii.position >= mCurItem) {
                if (ii.position == mCurItem) {
                    curItem = ii;
                }
                break;
            }
        }

        if (curItem == null && N > 0) {
            curItem = addNewItem(mCurItem, curIndex);
        }

        // Fill 3x the available width or up to the number of offscreen
        // pages requested to either side, whichever is larger.
        // If we have no current item we have no work to do.
        if (curItem != null) {
            float extraHeightTop = 0.f;
            int itemIndex = curIndex - 1;
            ItemInfo ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
            final int clientHeight = getClientHeight();

            final float topHeightNeeded = clientHeight <= 0 ? 0 :
                    2.f - curItem.widthFactor + (float) getPaddingTop() / (float) clientHeight;

            for (int pos = mCurItem - 1; pos >= 0; pos--) {
                if (extraHeightTop >= topHeightNeeded && pos < startPos) {
                    if (ii == null) {
                        break;
                    }

                    if (pos == ii.position && !ii.scrolling) {
                        mItems.remove(itemIndex);
                        mAdapter.destroyItem(this, pos, ii.object);
                        itemIndex--;
                        curIndex--;
                        ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
                    }
                } else if (ii != null && pos == ii.position) {
                    extraHeightTop += ii.widthFactor;
                    itemIndex--;
                    ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
                } else {
                    ii = addNewItem(pos, itemIndex + 1);
                    extraHeightTop += ii.widthFactor;
                    curIndex++;
                    ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
                }
            }

            float extraHeightBottom = curItem.widthFactor;
            itemIndex = curIndex + 1;
            if (extraHeightBottom < 2.f) {
                ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
                final float bottomHeightNeeded = clientHeight <= 0 ? 0 :
                        (float) getPaddingBottom() / (float) clientHeight + 2.f;

                for (int pos = mCurItem + 1; pos < N; pos++) {
                    if (extraHeightBottom >= bottomHeightNeeded && pos > endPos) {
                        if (ii == null) {
                            break;
                        }
                        if (pos == ii.position && !ii.scrolling) {
                            mItems.remove(itemIndex);
                            mAdapter.destroyItem(this, pos, ii.object);
                            ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
                        }
                    } else if (ii != null && pos == ii.position) {
                        extraHeightBottom += ii.widthFactor;
                        itemIndex++;
                        ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
                    } else {
                        ii = addNewItem(pos, itemIndex);
                        itemIndex++;
                        extraHeightBottom += ii.widthFactor;
                        ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
                    }
                }
            }
            calculatePageOffsetsVertical(curItem, curIndex, oldCurInfo);
        }

        if (DEBUG) {
            Log.i(TAG, "Current page list:");
            for (int i = 0; i < mItems.size(); i++) {
                Log.i(TAG, "#" + i + ": page " + mItems.get(i).position);
            }
        }
        mAdapter.setPrimaryItem(this, mCurItem, curItem != null ? curItem.object : null);
        mAdapter.finishUpdate(this);
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            lp.childIndex = i;
            if (!lp.isDecor && lp.widthFactor == 0.f) {
                final ItemInfo ii = infoForChild(child);
                if (ii != null) {
                    lp.widthFactor = ii.widthFactor;
                    lp.position = ii.position;
                }
            }
        }

        sortChildDrawingOrder();

        if (hasFocus()) {
            View currentFocused = findFocus();
            ItemInfo ii = currentFocused != null ? infoForAnyChild(currentFocused) : null;
            if (ii == null || ii.position != mCurItem) {
                for (int i = 0; i < getChildCount(); i++) {
                    View child = getChildAt(i);
                    ii = infoForChild(child);
                    if (ii != null && ii.position == mCurItem) {
                        if (child.requestFocus(View.FOCUS_FORWARD)) {
                            break;
                        }
                    }
                }
            }
        }
    }


    private void sortChildDrawingOrder() {
        if (mDrawingOrder != DRAW_ORDER_DEFAULT) {
            if (mDrawingOrderedChildren == null) {
                mDrawingOrderedChildren = new ArrayList<View>();
            } else {
                mDrawingOrderedChildren.clear();
            }
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                mDrawingOrderedChildren.add(child);
            }
            Collections.sort(mDrawingOrderedChildren, sPositionComparator);
        }
    }


    private void calculatePageOffsetsHorizontal(ItemInfo curItem, int curIndex, ItemInfo oldCurInfo) {
        final int N = getAdapterCount();
        final int width = getClientWidth();
        final float marginOffset = width > 0 ? (float) mPageMargin / width : 0;

        // Fix up offsets for later layout.
        if (oldCurInfo != null) {
            final int oldCurPosition = oldCurInfo.position;
            // Base offsets off of oldCurInfo.
            if (oldCurPosition < curItem.position) {
                int itemIndex = 0;
                ItemInfo ii = null;
                float offset = oldCurInfo.offset + oldCurInfo.widthFactor + marginOffset;
                for (int pos = oldCurPosition + 1; pos <= curItem.position && itemIndex < mItems.size(); pos++) {
                    ii = mItems.get(itemIndex);
                    while (pos > ii.position && itemIndex < mItems.size() - 1) {
                        itemIndex++;
                        ii = mItems.get(itemIndex);
                    }

                    while (pos < ii.position) {
                        // We don't have an item populated for this,
                        // ask the adapter for an offset.
                        offset += mAdapter.getPageWidth(pos) + marginOffset;
                        pos++;
                    }
                    ii.offset = offset;
                    offset += ii.widthFactor + marginOffset;
                }
            } else if (oldCurPosition > curItem.position) {
                int itemIndex = mItems.size() - 1;
                ItemInfo ii = null;
                float offset = oldCurInfo.offset;
                for (int pos = oldCurPosition - 1; pos >= curItem.position && itemIndex >= 0; pos--) {
                    ii = mItems.get(itemIndex);
                    while (pos < ii.position && itemIndex > 0) {
                        itemIndex--;
                        ii = mItems.get(itemIndex);
                    }
                    while (pos > ii.position) {
                        // We don't have an item populated for this,
                        // ask the adapter for an offset.
                        offset -= mAdapter.getPageWidth(pos) + marginOffset;
                        pos--;
                    }
                    offset -= ii.widthFactor + marginOffset;
                    ii.offset = offset;
                }
            }
        }

        // Base all offsets off of curItem.
        final int itemCount = mItems.size();
        float offset = curItem.offset;
        int pos = curItem.position - 1;
        mFirstOffset = curItem.position == 0 ? curItem.offset : -Float.MAX_VALUE;
        mLastOffset = curItem.position == N - 1
                ? curItem.offset + curItem.widthFactor - 1 : Float.MAX_VALUE;


        for (int i = curIndex - 1; i >= 0; i--, pos--) {
            final ItemInfo ii = mItems.get(i);
            // TODO: 2017/2/16
            while (pos > ii.position) {
                offset -= mAdapter.getPageWidth(pos--) + marginOffset;
            }
            offset -= ii.widthFactor + marginOffset;
            ii.offset = offset;

            if (ii.position == 0) {
                mFirstOffset = offset;
            }
        }
        offset = curItem.offset + curItem.widthFactor + marginOffset;
        pos = curItem.position + 1;
        for (int i = curIndex + 1; i < itemCount; i++, pos++) {
            final ItemInfo ii = mItems.get(i);
            while (pos < ii.position) {
                offset += mAdapter.getPageWidth(pos++) + marginOffset;
            }
            if (ii.position == N - 1) {
                mLastOffset = offset + ii.widthFactor - 1;
            }

            ii.offset = offset;
            offset += ii.widthFactor + marginOffset;
        }

        mNeedCalculatePageOffsets = false;
    }

    private void calculatePageOffsetsVertical(ItemInfo curItem, int curIndex, ItemInfo oldCurInfo) {
        final int N = getAdapterCount();
        final int height = getClientHeight();
        final float marginOffset = height > 0 ? (float) mPageMargin / height : 0;

        // Fix up offsets for later layout.
        if (oldCurInfo != null) {
            final int oldCurPosition = oldCurInfo.position;
            if (oldCurPosition < curItem.position) {
                int itemIndex = 0;
                ItemInfo ii = null;
                float offset = oldCurInfo.offset + oldCurInfo.widthFactor + marginOffset;
                for (int pos = oldCurPosition + 1; pos <= curItem.position && itemIndex < mItems.size(); pos++) {
                    ii = mItems.get(itemIndex);
                    while (pos > ii.position && itemIndex < mItems.size() - 1) {
                        itemIndex++;
                        ii = mItems.get(itemIndex);
                    }

                    while (pos < ii.position) {
                        // We don't have an item populated for this,
                        // ask the adapter for an offset.
                        offset += mAdapter.getPageWidth(pos) + marginOffset;
                        pos++;
                    }
                    ii.offset = offset;
                    offset += ii.widthFactor + marginOffset;
                }
            } else if (oldCurPosition > curItem.position) {
                int itemIndex = mItems.size() - 1;
                ItemInfo ii = null;
                float offset = oldCurInfo.offset;
                for (int pos = oldCurPosition - 1; pos >= curItem.position && itemIndex >= 0; pos--) {
                    ii = mItems.get(itemIndex);
                    while (pos < ii.position && itemIndex > 0) {
                        itemIndex--;
                        ii = mItems.get(itemIndex);
                    }
                    while (pos > ii.position) {
                        // We don't have an item populated for this,
                        // ask the adapter for an offset.
                        offset -= mAdapter.getPageWidth(pos) + marginOffset;
                        pos--;
                    }
                    offset -= ii.widthFactor + marginOffset;
                    ii.offset = offset;
                }
            }
        }

        // Base all offsets off of curItem.
        final int itemCount = mItems.size();
        float offset = curItem.offset;
        int pos = curItem.position - 1;
        mFirstOffset = curItem.position == 0 ? curItem.offset : -Float.MAX_VALUE;
        mLastOffset = curItem.position == N - 1
                ? curItem.offset + curItem.widthFactor - 1 : Float.MAX_VALUE;
        for (int i = curIndex - 1; i >= 0; i--, pos--) {
            final ItemInfo ii = mItems.get(i);
            // TODO: 2017/2/16
            while (pos > ii.position) {
                offset -= mAdapter.getPageWidth(pos--) + marginOffset;
            }
            offset -= ii.widthFactor + marginOffset;
            ii.offset = offset;
            if (ii.position == 0) {
                mFirstOffset = offset;
            }
        }
        offset = curItem.offset + curItem.widthFactor + marginOffset;
        pos = curItem.position + 1;
        for (int i = curIndex + 1; i < itemCount; i++, pos++) {
            final ItemInfo ii = mItems.get(i);
            while (pos < ii.position) {
                offset += mAdapter.getPageWidth(pos++) + marginOffset;
            }
            if (ii.position == N - 1) {
                mLastOffset = offset + ii.widthFactor - 1;
            }

            ii.offset = offset;
            offset += ii.widthFactor + marginOffset;
        }

        mNeedCalculatePageOffsets = false;
    }

    /**
     * This is the persistent state that is saved by ViewPager.  Only needed
     * if you are creating a sublass of ViewPager that must save its own
     * state, in which case it should implement a subclass of this which
     * contains that state.
     */
    public static class SavedState extends AbsSavedState {
        int position;
        Parcelable adapterState;
        ClassLoader loader;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(position);
            out.writeParcelable(adapterState, flags);
        }

        @Override
        public String toString() {
            return "FragmentPager.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " position=" + position + "}";
        }

        public static final Creator<SavedState> CREATOR = ParcelableCompat.newCreator(
                new ParcelableCompatCreatorCallbacks<SavedState>() {
                    @Override
                    public YViewPager.SavedState createFromParcel(Parcel in, ClassLoader loader) {
                        return new YViewPager.SavedState(in, loader);
                    }

                    @Override
                    public YViewPager.SavedState[] newArray(int size) {
                        return new YViewPager.SavedState[size];
                    }
                });

        SavedState(Parcel in, ClassLoader loader) {
            super(in, loader);
            if (loader == null) {
                loader = getClass().getClassLoader();
            }
            position = in.readInt();
            adapterState = in.readParcelable(loader);
            this.loader = loader;
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        YViewPager.SavedState ss = new YViewPager.SavedState(superState);
        ss.position = mCurItem;
        if (mAdapter != null) {
            ss.adapterState = mAdapter.saveState();
        }
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof YViewPager.SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        YViewPager.SavedState ss = (YViewPager.SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        if (mAdapter != null) {
            mAdapter.restoreState(ss.adapterState, ss.loader);
            if (isVertical) {
                setCurrentItemInternalVertical(ss.position, false, true);
            } else {
                setCurrentItemInternalHorizontal(ss.position, false, true);
            }
        } else {
            mRestoredCurItem = ss.position;
            mRestoredAdapterState = ss.adapterState;
            mRestoredClassLoader = ss.loader;
        }
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (!checkLayoutParams(params)) {
            params = generateLayoutParams(params);
        }
        final YViewPager.LayoutParams lp = (YViewPager.LayoutParams) params;
        // Any views added via inflation should be classed as part of the decor
        lp.isDecor |= isDecorView(child);
        if (mInLayout) {
            if (lp != null && lp.isDecor) {
                throw new IllegalStateException("Cannot add pager decor view during layout");
            }
            lp.needsMeasure = true;
            addViewInLayout(child, index, params);
        } else {
            ViewParent parent = child.getParent();
            if (parent != null) {
                ((ViewGroup) parent).removeView(child);
            }
            super.addView(child, index, params);
        }

        if (USE_CACHE) {
            if (child.getVisibility() != GONE) {
                child.setDrawingCacheEnabled(mScrollingCacheEnabled);
            } else {
                child.setDrawingCacheEnabled(false);
            }
        }
    }

    private static boolean isDecorView(View view) {
        Class<?> clazz = view.getClass();
        return clazz.getAnnotation(YViewPager.DecorView.class) != null;
    }

    @Override
    public void removeView(View view) {
        if (mInLayout) {
            removeViewInLayout(view);
        } else {
            super.removeView(view);
        }
    }


    ItemInfo infoForChild(View child) {
        for (int i = 0; i < mItems.size(); i++) {
            ItemInfo ii = mItems.get(i);
            if (mAdapter.isViewFromObject(child, ii.object)) {
                return ii;
            }
        }
        return null;
    }

    ItemInfo infoForAnyChild(View child) {
        ViewParent parent;
        while ((parent = child.getParent()) != this) {
            if (parent == null || !(parent instanceof View)) {
                return null;
            }
            child = (View) parent;
        }
        return infoForChild(child);
    }


    ItemInfo infoForPosition(int position) {
        for (int i = 0; i < mItems.size(); i++) {
            ItemInfo ii = mItems.get(i);
            if (ii.position == position) {
                return ii;
            }
        }
        return null;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFirstLayout = true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // For simple implementation, our internal size is always 0.
        // We depend on the container to specify the layout size of
        // our view.  We can't really know what it is since we will be
        // adding and removing different arbitrary views and do not
        // want the layout to change as this happens.


        setMeasuredDimension(getDefaultSize(0, widthMeasureSpec),
                getDefaultSize(0, heightMeasureSpec));

        final int measuredWidth = getMeasuredWidth();

        final int maxGutterSize = measuredWidth / 10;
        mGutterSize = Math.min(maxGutterSize, mDefaultGutterSize);

        int childWidthSize = measuredWidth - getPaddingLeft() - getPaddingRight();
        int childHeightSize = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();

        /*
         * Make sure all children have been properly measured. Decor views first.
         * Right now we cheat and make this less complicated by assuming decor
         * views won't intersect. We will pin to edges based on gravity.
         */
        int size = getChildCount();
        for (int i = 0; i < size; ++i) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (YViewPager.LayoutParams) child.getLayoutParams();
                if (lp != null && lp.isDecor) {
                    final int hgrav = lp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                    final int vgrav = lp.gravity & Gravity.VERTICAL_GRAVITY_MASK;
                    int widthMode = MeasureSpec.AT_MOST;
                    int heightMode = MeasureSpec.AT_MOST;
                    boolean consumeVertical = vgrav == Gravity.TOP || vgrav == Gravity.BOTTOM;
                    boolean consumeHorizontal = hgrav == Gravity.LEFT || hgrav == Gravity.RIGHT;
                    if (consumeVertical) {
                        widthMode = MeasureSpec.EXACTLY;
                    } else if (consumeHorizontal) {
                        heightMode = MeasureSpec.EXACTLY;
                    }
                    int widthSize = childWidthSize;
                    int heightSize = childHeightSize;

                    //Can be one of the constants FILL_PARENT (replaced by MATCH_PARENT
                    //in API Level 8) or WRAP_CONTENT, or an exact size.
                    if (lp.width != LayoutParams.WRAP_CONTENT) {
                        widthMode = MeasureSpec.EXACTLY;
                        if (lp.width != LayoutParams.MATCH_PARENT) {
                            widthSize = lp.width;
                        }
                    }
                    if (lp.height != YViewPager.LayoutParams.WRAP_CONTENT) {
                        heightMode = MeasureSpec.EXACTLY;
                        if (lp.height != YViewPager.LayoutParams.MATCH_PARENT) {
                            heightSize = lp.height;
                        }
                    }
                    final int widthSpec = MeasureSpec.makeMeasureSpec(widthSize, widthMode);
                    final int heightSpec = MeasureSpec.makeMeasureSpec(heightSize, heightMode);
                    child.measure(widthSpec, heightSpec);

                    if (consumeVertical) {
                        childHeightSize -= child.getMeasuredHeight();
                    } else if (consumeHorizontal) {
                        childWidthSize -= child.getMeasuredWidth();
                    }
                }
            }
        }

        mChildWidthMeasureSpec = MeasureSpec.makeMeasureSpec(childWidthSize, MeasureSpec.EXACTLY);
        mChildHeightMeasureSpec = MeasureSpec.makeMeasureSpec(childHeightSize, MeasureSpec.EXACTLY);

        // Make sure we have created all fragments that we need to have shown.
        mInLayout = true;
        if (isVertical) {
            populateVertical();
        } else {
            populateHorizontal();
        }
        mInLayout = false;

        size = getChildCount();
        for (int i = 0; i < size; ++i) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (!isVertical && (lp == null || !lp.isDecor)) {
                    final int widthSpec = MeasureSpec.makeMeasureSpec(
                            (int) (childWidthSize * lp.widthFactor), MeasureSpec.EXACTLY);
                    child.measure(widthSpec, mChildHeightMeasureSpec);
                } else if (isVertical && (lp == null || !lp.isDecor)) {
                    final int heightSpec = MeasureSpec.makeMeasureSpec(
                            (int) (childHeightSize * lp.widthFactor), MeasureSpec.EXACTLY);
                    child.measure(mChildWidthMeasureSpec, heightSpec);
                }
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Make sure scroll position is set correctly.
        if (isVertical && h != oldh) {
            recomputeScrollPositionVertical(h, oldh, mPageMargin, mPageMargin);
        } else if (!isVertical && w != oldw) {
            recomputeScrollPositionHorizontal(w, oldw, mPageMargin, mPageMargin);
        }
    }

    private void recomputeScrollPositionHorizontal(int width, int oldWidth, int margin, int oldMargin) {
        if (oldWidth > 0 && !mItems.isEmpty()) {
            if (!mScroller.isFinished()) {
                mScroller.setFinalX(getCurrentItem() * getClientWidth());
            } else {
                final int widthWithMargin = width - getPaddingLeft() - getPaddingRight() + margin;
                final int oldWidthWithMargin = oldWidth - getPaddingLeft() - getPaddingRight()
                        + oldMargin;
                final int xpos = getScrollX();
                final float pageOffset = (float) xpos / oldWidthWithMargin;
                final int newOffsetPixels = (int) (pageOffset * widthWithMargin);

                scrollTo(newOffsetPixels, getScrollY());
            }
        } else {
            final YViewPager.ItemInfo ii = infoForPosition(mCurItem);
            final float scrollOffset = ii != null ? Math.min(ii.offset, mLastOffset) : 0;
            final int scrollPos =
                    (int) (scrollOffset * (width - getPaddingLeft() - getPaddingRight()));
            if (scrollPos != getScrollX()) {
                completeScrollHorizontal(false);
                scrollTo(scrollPos, getScrollY());
            }
        }
    }

    private void recomputeScrollPositionVertical(int height, int oldHeight, int margin, int oldMargin) {
        if (oldHeight > 0 && !mItems.isEmpty()) {
            if (!mScroller.isFinished()) {
                mScroller.setFinalY(getCurrentItem() * getClientHeight());
            } else {
                final int heightWithMargin = height - getPaddingTop() - getPaddingBottom() + margin;
                final int oldHeightWithMargin = oldHeight - getPaddingTop() - getPaddingBottom()
                        + oldMargin;
                final int ypos = getScrollY();
                final float pageOffset = (float) ypos / oldHeightWithMargin;
                final int newOffsetPixels = (int) (pageOffset * heightWithMargin);

                scrollTo(getScrollX(), newOffsetPixels);
            }
        } else {
            final ItemInfo ii = infoForPosition(mCurItem);
            final float scrollOffset = ii != null ? Math.min(ii.offset, mLastOffset) : 0;
            final int scrollPos =
                    (int) (scrollOffset * (height - getPaddingTop() - getPaddingBottom()));
            if (scrollPos != getScrollY()) {
                completeScrollVertical(false);
                scrollTo(getScrollX(), scrollPos);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int count = getChildCount();
        int width = r - l;
        int height = b - t;
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        // TODO: 2017/2/10
        final int scrollX = getScrollX();
        final int scrollY = getScrollY();

        int decorCount = 0;

        // First pass - decor views. We need to do this in two passes so that
        // we have the proper offsets for non-decor views later.
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final YViewPager.LayoutParams lp = (YViewPager.LayoutParams) child.getLayoutParams();
                int childLeft = 0;
                int childTop = 0;
                if (lp.isDecor) {
                    final int hgrav = lp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                    final int vgrav = lp.gravity & Gravity.VERTICAL_GRAVITY_MASK;

                    switch (hgrav) {
                        default:
                            childLeft = paddingLeft;
                            break;
                        case Gravity.LEFT:
                            childLeft = paddingLeft;
                            paddingLeft += child.getMeasuredWidth();
                            break;
                        case Gravity.CENTER_HORIZONTAL:
                            childLeft = Math.max((width - child.getMeasuredWidth()) / 2,
                                    paddingLeft);
                            break;
                        case Gravity.RIGHT:
                            childLeft = width - paddingRight - child.getMeasuredWidth();
                            paddingRight += child.getMeasuredWidth();
                            break;
                    }
                    switch (vgrav) {
                        default:
                            childTop = paddingTop;
                            break;
                        case Gravity.TOP:
                            childTop = paddingTop;
                            paddingTop += child.getMeasuredHeight();
                            break;
                        case Gravity.CENTER_VERTICAL:
                            childTop = Math.max((height - child.getMeasuredHeight()) / 2,
                                    paddingTop);
                            break;
                        case Gravity.BOTTOM:
                            childTop = height - paddingBottom - child.getMeasuredHeight();
                            paddingBottom += child.getMeasuredHeight();
                            break;
                    }
                    if (isVertical) {
                        childTop += scrollY;
                    } else {
                        childLeft += scrollX;
                    }
                    child.layout(childLeft, childTop,
                            childLeft + child.getMeasuredWidth(),
                            childTop + child.getMeasuredHeight());

                    decorCount++;
                }
            }
        }

        final int childWidth = width - paddingLeft - paddingRight;
        final int childHeight = height - paddingTop - paddingBottom;
        // Page views. Do this once we have the right padding offsets from above.
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final YViewPager.LayoutParams lp = (YViewPager.LayoutParams) child.getLayoutParams();
                // TODO: 2017/2/10  ItemInfo
                ItemInfo ii;
                if (!lp.isDecor && (ii = infoForChild(child)) != null) {
                    // TODO: 2017/2/10    ii.offset
                    int loff = (int) (childWidth * ii.offset);
                    int toff = (int) (childHeight * ii.offset);
                    int childLeft = !isVertical ? paddingLeft + loff : paddingLeft;
                    int childTop = !isVertical ? paddingTop : paddingTop + toff;
                    if (lp.needsMeasure) {
                        // This was added during layout and needs measurement.
                        // Do it now that we know what we're working with.

                        lp.needsMeasure = false;

                        int widthSpec;
                        int heightSpec;
                        if (!isVertical) {
                            widthSpec = MeasureSpec.makeMeasureSpec(
                                    (int) (childWidth * lp.widthFactor),
                                    MeasureSpec.EXACTLY);
                            heightSpec = MeasureSpec.makeMeasureSpec(
                                    (int) (height - paddingTop - paddingBottom),
                                    MeasureSpec.EXACTLY);
                        } else {
                            widthSpec = MeasureSpec.makeMeasureSpec(
                                    (int) (width * lp.widthFactor),
                                    MeasureSpec.EXACTLY);
                            heightSpec = MeasureSpec.makeMeasureSpec(
                                    (int) (childHeight - paddingTop - paddingBottom),
                                    MeasureSpec.EXACTLY);
                        }
                        child.measure(widthSpec, heightSpec);
                    }
                    if (DEBUG) {
                        Log.v(TAG, "Positioning #" + i + " " + child + " f=" + ii.object
                                + ":" + childLeft + "," + childTop + " " + child.getMeasuredWidth()
                                + "x" + child.getMeasuredHeight());
                    }
                    child.layout(childLeft, childTop,
                            childLeft + child.getMeasuredWidth(),
                            childTop + child.getMeasuredHeight());
                }
            }
        }
        if (!isVertical) {
            mTopPageBounds = paddingTop;
            mBottomPageBounds = height - paddingBottom;
        } else {
            mLeftPageBounds = paddingLeft;
            mRightPageBounds = width - paddingRight;
        }
        mDecorChildCount = decorCount;


        if (isVertical && mFirstLayout) {
            scrollToItemVertical(mCurItem, false, 0, false);
        } else if (!isVertical && mFirstLayout) {
            scrollToItemHorizontal(mCurItem, false, 0, false);
        }
        mFirstLayout = false;
    }


    @Override
    public void computeScroll() {
        mIsScrollStarted = true;
        if (!mScroller.isFinished() && mScroller.computeScrollOffset()) {
            int oldX = getScrollX();
            int oldY = getScrollY();
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();
            if (oldY != y || oldX != x) {
                scrollTo(x, y);

                if (!isVertical && !pageScrolledHorizontal(x)) {
                    mScroller.abortAnimation();
                    scrollTo(0, y);
                } else if (isVertical && pageScrolledVertical(y)) {
                    mScroller.abortAnimation();
//                    scrollTo(x, mDestY);
                    smoothScrollToVertical(x, mDestY, mVelocityY);
                }
            }

            // Keep on drawing until the animation has finished.
            ViewCompat.postInvalidateOnAnimation(this);
            return;
        }

        // Done with scroll, clean up state.
        if (isVertical) {
            completeScrollVertical(true);
        } else {
            completeScrollHorizontal(true);
        }
    }


    private boolean pageScrolledHorizontal(int xpos) {
        if (mItems.size() == 0) {
            if (mFirstLayout) {
                // If we haven't been laid out yet, we probably just haven't been populated yet.
                // Let's skip this call since it doesn't make sense in this state
                return false;
            }

            mCalledSuper = false;

            // TODO: 2017/2/11
            onPageScrolledHorizontal(0, 0, 0);
            if (!mCalledSuper) {
                throw new IllegalStateException(
                        "onPageScrolled did not call superclass implementation");
            }
            return false;
        }
        // TODO: 2017/2/11
        final ItemInfo ii = infoForCurrentScrollPositionHorizontal();

        final int width = getClientWidth();
        final int widthWithMargin = width + mPageMargin;
        final float marginOffset = (float) mPageMargin / width;

        final int currentPage = ii.position;
        final float pageOffset = (((float) xpos / width) - ii.offset)
                / (ii.widthFactor + marginOffset);
        final int offsetPixels = (int) (pageOffset * widthWithMargin);
        mCalledSuper = false;
        onPageScrolledHorizontal(currentPage, pageOffset, offsetPixels);
        if (!mCalledSuper) {
            throw new IllegalStateException(
                    "onPageScrolled did not call superclass implementation");
        }
        return true;
    }

    public void setDirection(int direction) {
        if (direction == HORIZONTAL && isVertical) {
            isVertical = false;
        } else if (direction == VERTICAL && !isVertical) {
            isVertical = true;
        }
        requestLayout();
    }


    private boolean pageScrolledVertical(int ypos) {
        if (mItems.size() == 0) {
            if (mFirstLayout) {
                // If we haven't been laid out yet, we probably just haven't been populated yet.
                // Let's skip this call since it doesn't make sense in this state
                return false;
            }
            mCalledSuper = false;

            onPageScrolledVertical(0, 0, 0);

            if (!mCalledSuper) {
                throw new IllegalStateException(
                        "onPageScrolled did not call superclass implementation");
            }
            return false;
        }
        // TODO: 2017/2/11
        final ItemInfo ii = infoForCurrentScrollPositionVertical();

        final int height = getClientHeight();
        final int heightWithMargin = height + mPageMargin;
        final float marginOffset = (float) mPageMargin / height;

        final int currentPage = ii.position;

        final float pageOffset = (((float) ypos / height) - ii.offset)
                / (ii.widthFactor + marginOffset);
        final int offsetPixels = (int) (pageOffset * heightWithMargin);
        mCalledSuper = false;
        onPageScrolledVertical(currentPage, pageOffset, offsetPixels);
        if (!mCalledSuper) {
            throw new IllegalStateException(
                    "onPageScrolled did not call superclass implementation");
        }
        return true;
    }

    /**
     * This method will be invoked when the current page is scrolled, either as part
     * of a programmatically initiated smooth scroll or a user initiated touch scroll.
     * If you override this method you must call through to the superclass implementation
     * (e.g. super.onPageScrolled(position, offset, offsetPixels)) before onPageScrolled
     * returns.
     *
     * @param position     Position index of the first page currently being displayed.
     *                     Page position+1 will be visible if positionOffset is nonzero.
     * @param offset       Value from [0, 1) indicating the offset from the page at position.
     * @param offsetPixels Value in pixels indicating the offset from position.
     */

    protected void onPageScrolledHorizontal(int position, float offset, int offsetPixels) {
        // Offset any decor views if needed - keep them on-screen at all times.

        if (mDecorChildCount > 0) {
            final int scrollX = getScrollX();
            int paddingLeft = getPaddingLeft();
            int paddingRight = getPaddingRight();
            final int width = getWidth();
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                final LayoutParams lp = (YViewPager.LayoutParams) child.getLayoutParams();
                if (!lp.isDecor) continue;

                final int hgrav = lp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                int childLeft = 0;
                switch (hgrav) {
                    default:
                        childLeft = paddingLeft;
                        break;
                    case Gravity.LEFT:
                        childLeft = paddingLeft;
                        paddingLeft += child.getWidth();
                        break;
                    case Gravity.CENTER_HORIZONTAL:
                        childLeft = Math.max((width - child.getMeasuredWidth()) / 2,
                                paddingLeft);
                        break;
                    case Gravity.RIGHT:
                        childLeft = width - paddingRight - child.getMeasuredWidth();
                        paddingRight += child.getMeasuredWidth();
                        break;
                }
                childLeft += scrollX;
                final int childOffset = childLeft - child.getLeft();
                if (childOffset != 0) {
                    child.offsetLeftAndRight(childOffset);
                }
            }
        }

        dispatchOnPageScrolled(position, offset, offsetPixels);

        if (mPageTransformer != null) {
            final int scrollX = getScrollX();
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.isDecor) continue;
                // TODO: 2017/2/11
                final float transformPos = (float) (child.getLeft() - scrollX) / getClientWidth();
                mPageTransformer.transformPage(child, transformPos);
            }
        }
        mCalledSuper = true;
    }

    /**
     * This method will be invoked when the current page is scrolled, either as part
     * of a programmatically initiated smooth scroll or a user initiated touch scroll.
     * If you override this method you must call through to the superclass implementation
     * (e.g. super.onPageScrolled(position, offset, offsetPixels)) before onPageScrolled
     * returns.
     *
     * @param position     Position index of the first page currently being displayed.
     *                     Page position+1 will be visible if positionOffset is nonzero.
     * @param offset       Value from [0, 1) indicating the offset from the page at position.
     * @param offsetPixels Value in pixels indicating the offset from position.
     */

    protected void onPageScrolledVertical(int position, float offset, int offsetPixels) {
        // Offset any decor views if needed - keep them on-screen at all times.

        if (mDecorChildCount > 0) {
            final int scrollY = getScrollY();
            int paddingTop = getPaddingTop();
            int paddingBottom = getPaddingBottom();
            final int height = getHeight();
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                final YViewPager.LayoutParams lp = (YViewPager.LayoutParams) child.getLayoutParams();
                if (!lp.isDecor) continue;

                final int vgrav = lp.gravity & Gravity.VERTICAL_GRAVITY_MASK;
                int childTop = 0;
                switch (vgrav) {
                    default:
                        childTop = paddingTop;
                        break;
                    case Gravity.TOP:
                        childTop = paddingTop;
                        paddingTop += child.getHeight();
                        break;
                    case Gravity.CENTER_VERTICAL:
                        childTop = Math.max((height - child.getMeasuredHeight()) / 2,
                                paddingTop);
                        break;
                    case Gravity.BOTTOM:
                        childTop = height - paddingTop - child.getMeasuredHeight();
                        paddingTop += child.getMeasuredHeight();
                        break;
                }
                childTop += scrollY;

                final int childOffset = childTop - child.getTop();
                if (childOffset != 0) {
                    child.offsetTopAndBottom(childOffset);
                }
            }
        }

        dispatchOnPageScrolled(position, offset, offsetPixels);

        if (mPageTransformer != null) {
            final int scrollY = getScrollY();
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.isDecor) continue;
                final float transformPos = (float) (child.getTop() - scrollY) / getClientHeight();
                mPageTransformer.transformPage(child, transformPos);
            }
        }
        mCalledSuper = true;
    }


    private void dispatchOnPageScrolled(int position, float offset, int offsetPixels) {
        int newPosition=isCirculatory?position%mAdapter.getCount():position;
        if (mOnPageChangeListener != null) {
            mOnPageChangeListener.onPageScrolled(newPosition, offset, offsetPixels);
        }
        if (mOnPageChangeListeners != null) {
            for (int i = 0, z = mOnPageChangeListeners.size(); i < z; i++) {
                OnPageChangeListener listener = mOnPageChangeListeners.get(i);
                if (listener != null) {
                    listener.onPageScrolled(newPosition, offset, offsetPixels);
                }
            }
        }
        if (mInternalPageChangeListener != null) {
            mInternalPageChangeListener.onPageScrolled(newPosition, offset, offsetPixels);
        }
    }

    private void dispatchOnPageSelected(int position) {
        int newPosition=isCirculatory?position%mAdapter.getCount():position;
        if (mOnPageChangeListener != null) {
            mOnPageChangeListener.onPageSelected(newPosition);
        }
        if (mOnPageChangeListeners != null) {
            for (int i = 0, z = mOnPageChangeListeners.size(); i < z; i++) {
                OnPageChangeListener listener = mOnPageChangeListeners.get(i);
                if (listener != null) {
                    listener.onPageSelected(newPosition);
                }
            }
        }
        if (mInternalPageChangeListener != null) {
            mInternalPageChangeListener.onPageSelected(newPosition);
        }
    }

    private void dispatchOnScrollStateChanged(int state) {
        if (mOnPageChangeListener != null) {
            mOnPageChangeListener.onPageScrollStateChanged(state);
        }
        if (mOnPageChangeListeners != null) {
            for (int i = 0, z = mOnPageChangeListeners.size(); i < z; i++) {
                YViewPager.OnPageChangeListener listener = mOnPageChangeListeners.get(i);
                if (listener != null) {
                    listener.onPageScrollStateChanged(state);
                }
            }
        }
        if (mInternalPageChangeListener != null) {
            mInternalPageChangeListener.onPageScrollStateChanged(state);
        }
    }


    private void completeScrollHorizontal(boolean postEvents) {
        boolean needPopulate = mScrollState == SCROLL_STATE_SETTLING;
        if (needPopulate) {
            setScrollingCacheEnabled(false);
            boolean wasScrolling = !mScroller.isFinished();
            if (wasScrolling) {
                mScroller.abortAnimation();

                int oldX = getScrollX();
                int oldY = getScrollY();
                int x = mScroller.getCurrX();
                int y = mScroller.getCurrY();
                if (oldX != x || oldY != y) {
                    scrollTo(x, y);
                    if (x != oldX) {
                        pageScrolledHorizontal(x);
                    }
                }
            }
        }
        mPopulatePending = false;

        for (int i = 0; i < mItems.size(); i++) {
            ItemInfo ii = mItems.get(i);
            if (ii.scrolling) {
                needPopulate = true;
                ii.scrolling = false;
            }
        }
        if (needPopulate) {
            if (postEvents) {
                ViewCompat.postOnAnimation(this, mEndScrollRunnable);
            } else {
                mEndScrollRunnable.run();
            }
        }
    }

    private void completeScrollVertical(boolean postEvents) {
        boolean needPopulate = mScrollState == SCROLL_STATE_SETTLING;
        if (needPopulate) {
            // Done with scroll, no longer want to cache view drawing.
            setScrollingCacheEnabled(false);
            boolean wasScrolling = !mScroller.isFinished();
            if (wasScrolling) {
                mScroller.abortAnimation();

                int oldX = getScrollX();
                int oldY = getScrollY();
                int x = mScroller.getCurrX();
                int y = mScroller.getCurrY();
                if (oldX != x || oldY != y) {
                    scrollTo(x, y);
                    if (y != oldY) {
                        pageScrolledVertical(y);
                    }
                }
            }
        }
        mPopulatePending = false;

        for (int i = 0; i < mItems.size(); i++) {
            ItemInfo ii = mItems.get(i);
            if (ii.scrolling) {
                needPopulate = true;
                ii.scrolling = false;
            }
        }
        if (needPopulate) {
            if (postEvents) {
                ViewCompat.postOnAnimation(this, mEndScrollRunnable);
            } else {
                mEndScrollRunnable.run();
            }
        }
    }

    private boolean isGutterDragHorizontal(float x, float dx) {
        return (x < mGutterSize && dx > 0) || (x > getWidth() - mGutterSize && dx < 0);
    }

    private boolean isGutterDragVertical(float y, float dy) {
        return (y < mGutterSize && dy > 0) || (y > getHeight() - mGutterSize && dy < 0);
    }

    private void enableLayers(boolean enable) {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final int layerType = enable
                    ? ViewCompat.LAYER_TYPE_HARDWARE : ViewCompat.LAYER_TYPE_NONE;
            ViewCompat.setLayerType(getChildAt(i), layerType, null);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onMotionEvent will be called and we do the actual
         * scrolling there.
         */

        final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;

        // Always take care of the touch gesture being complete.
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            // Release the drag.
            if (DEBUG) Log.v(TAG, "Intercept done!");
            resetTouch();
            return false;
        }

        // Nothing more to do here if we have decided whether or not we
        // are dragging.
        if (action != MotionEvent.ACTION_DOWN) {
            if (mIsBeingDragged) {
                if (DEBUG) Log.v(TAG, "Intercept returning true!");
                return true;
            }
            if (mIsUnableToDrag) {
                if (DEBUG) Log.v(TAG, "Intercept returning false!");
                return false;
            }
        }

        switch (action) {
            case MotionEvent.ACTION_MOVE: {
                /*
                 * mIsBeingDragged == false, otherwise the shortcut would have caught it. Check
                 * whether the user has moved far enough from his original down touch.
                 */

                /*
                * Locally do absolute value. mLastMotionY is set to the y value
                * of the down event.
                */

                final int activePointerId = mActivePointerId;
                if (activePointerId == INVALID_POINTER) {
                    // If we don't have a valid id, the touch down wasn't on content.
                    break;
                }
                //
                final int pointerIndex = ev.findPointerIndex(activePointerId);
                final float x = ev.getX(pointerIndex);
                final float dx = isVertical ? x - mInitialMotionX : x - mLastMotionX;
                final float xDiff = Math.abs(dx);
                final float y = ev.getY(pointerIndex);

                final float dy = isVertical ? y - mLastMotionY : y - mInitialMotionY;
                final float yDiff = Math.abs(dy);

                if (DEBUG) Log.v(TAG, "Moved x to " + x + "," + y + " diff=" + xDiff + "," + yDiff);

                if (!isVertical && dx != 0 && !isGutterDragHorizontal(mLastMotionX, dx)
                        && canScrollHorizontal(this, false, (int) dx, (int) x, (int) y)) {
                    // Nested view has scrollable area under this point. Let it be handled there.
                    mLastMotionX = x;
                    mLastMotionY = y;
                    mIsUnableToDrag = true;
                    return false;
                } else if (isVertical && dy != 0 && isGutterDragVertical(mLastMotionY, dy)
                        && canScrollVertical(this, false, (int) dy, (int) x, (int) y)) {
                    mLastMotionX = x;
                    mLastMotionY = y;
                    mIsUnableToDrag = true;
                    return false;
                }

                if (!isVertical && xDiff > mTouchSlop && xDiff * 0.5f > yDiff) {
                    mIsBeingDragged = true;
                    requestParentDisallowInterceptTouchEvent(true);
                    setScrollState(SCROLL_STATE_DRAGGING);
                    mLastMotionX = dx > 0
                            ? mInitialMotionX + mTouchSlop : mInitialMotionX - mTouchSlop;
                    mLastMotionY = y;
                    setScrollingCacheEnabled(true);
                } else if (!isVertical && yDiff > mTouchSlop) {
                    // The finger has moved enough in the vertical
                    // direction to be counted as a drag...  abort
                    // any attempt to drag horizontally, to work correctly
                    // with children that have scrolling containers.
                    mIsUnableToDrag = true;
                } else if (isVertical && yDiff > mTouchSlop && yDiff * 0.5f > xDiff) {
                    mIsBeingDragged = true;
                    requestParentDisallowInterceptTouchEvent(true);
                    setScrollState(SCROLL_STATE_DRAGGING);
                    mLastMotionY = dy > 0
                            ? mInitialMotionY + mTouchSlop : mInitialMotionY - mTouchSlop;
                    mLastMotionX = x;
                    setScrollingCacheEnabled(true);
                } else if (isVertical && xDiff > mTouchSlop) {
                    mIsUnableToDrag = true;
                }

                if (mIsBeingDragged) {
                    // Scroll to follow the motion event
                    if (isVertical && performDragVertical(y)) {
                        ViewCompat.postInvalidateOnAnimation(this);
                    } else if (!isVertical && performDragHorizontal(x)) {
                        ViewCompat.postInvalidateOnAnimation(this);
                    }
                }
                break;
            }

            case MotionEvent.ACTION_DOWN: {
                /*
                 * Remember location of down touch.
                 * ACTION_DOWN always refers to pointer index 0.
                 */
                mLastMotionX = mInitialMotionX = ev.getX();
                mLastMotionY = mInitialMotionY = ev.getY();
                mActivePointerId = ev.getPointerId(0);
                mIsUnableToDrag = false;
                mIsScrollStarted = true;

                mScroller.computeScrollOffset();
                if (!isVertical && mScrollState == SCROLL_STATE_SETTLING
                        && Math.abs(mScroller.getFinalX() - mScroller.getCurrX()) > mCloseEnough) {
                    // Let the user 'catch' the pager as it animates.
                    mScroller.abortAnimation();
                    //
                    mPopulatePending = false;
                    if (isVertical) {
                        populateVertical();
                    } else {
                        populateHorizontal();
                    }
                    mIsBeingDragged = true;
                    requestParentDisallowInterceptTouchEvent(true);
                    setScrollState(SCROLL_STATE_DRAGGING);
                } else if (!isVertical) {
                    completeScrollHorizontal(false);
                    mIsBeingDragged = false;
                } else if (isVertical && mScrollState == SCROLL_STATE_SETTLING
                        && Math.abs(mScroller.getFinalY() - mScroller.getCurrY()) > mCloseEnough) {
                    // Let the user 'catch' the pager as it animates.
                    mScroller.abortAnimation();
                    //
                    mPopulatePending = false;
                    if (isVertical) {
                        populateVertical();
                    } else {
                        populateHorizontal();
                    }
                    mIsBeingDragged = true;
                    requestParentDisallowInterceptTouchEvent(true);
                    setScrollState(SCROLL_STATE_DRAGGING);
                } else if (isVertical) {
                    completeScrollVertical(false);
                    mIsBeingDragged = false;
                }

                if (DEBUG) {
                    Log.v(TAG, "Down at " + mLastMotionX + "," + mLastMotionY
                            + " mIsBeingDragged=" + mIsBeingDragged
                            + "mIsUnableToDrag=" + mIsUnableToDrag);
                }
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                if (!isVertical) {
                    onSecondaryPointerUpHorizontal(ev);
                } else {
                    onSecondaryPointerUpVertical(ev);
                }
                break;
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        /*
         * The only time we want to intercept motion events is if we are in the
         * drag mode.
         */
        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mFakeDragging) {
            // A fake drag is in progress already, ignore this real one
            // but still eat the touch events.
            // (It is likely that the user is multi-touching the screen.)
            return true;
        }

        if (ev.getAction() == MotionEvent.ACTION_DOWN && ev.getEdgeFlags() != 0) {
            // Don't handle edge touches immediately -- they may actually belong to one of our
            // descendants.
            return false;
        }

        if (mAdapter == null || getAdapterCount() == 0) {
            // Nothing to present or scroll; nothing to touch.
            return false;
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        final int action = ev.getAction();
        boolean needsInvalidate = false;

        switch (action & MotionEventCompat.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                mScroller.abortAnimation();
                mPopulatePending = false;
                if (isVertical) {
                    populateVertical();
                } else {
                    populateHorizontal();
                }
                // Remember where the motion event started
                mLastMotionX = mInitialMotionX = ev.getX();
                mLastMotionY = mInitialMotionY = ev.getY();
                mActivePointerId = ev.getPointerId(0);
                break;
            }
            case MotionEvent.ACTION_MOVE:
                if (!mIsBeingDragged) {
                    final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                    if (pointerIndex == -1) {
                        // A child has consumed some touch events and put us into an inconsistent
                        // state.
                        needsInvalidate = resetTouch();
                        break;
                    }
                    final float x = ev.getX(pointerIndex);
                    final float xDiff = Math.abs(x - mLastMotionX);
                    final float y = ev.getY(pointerIndex);
                    final float yDiff = Math.abs(y - mLastMotionY);
                    if (!isVertical && xDiff > mTouchSlop && xDiff > yDiff) {
                        mIsBeingDragged = true;
                        requestParentDisallowInterceptTouchEvent(true);
                        mLastMotionX = x - mInitialMotionX > 0 ? mInitialMotionX + mTouchSlop :
                                mInitialMotionX - mTouchSlop;
                        mLastMotionY = y;
                        setScrollState(SCROLL_STATE_DRAGGING);
                        setScrollingCacheEnabled(true);

                        // Disallow Parent Intercept, just in case
                        ViewParent parent = getParent();
                        if (parent != null) {
                            parent.requestDisallowInterceptTouchEvent(true);
                        }
                    } else if (isVertical && yDiff > mTouchSlop && yDiff > xDiff) {
                        mIsBeingDragged = true;
                        requestParentDisallowInterceptTouchEvent(true);

                        mLastMotionY = y - mInitialMotionY > 0 ? mInitialMotionY + mTouchSlop :
                                mInitialMotionY - mTouchSlop;
                        mLastMotionX = x;
                        setScrollState(SCROLL_STATE_DRAGGING);
                        setScrollingCacheEnabled(true);

                        // Disallow Parent Intercept, just in case
                        ViewParent parent = getParent();
                        if (parent != null) {
                            parent.requestDisallowInterceptTouchEvent(true);
                        }
                    }
                }
                // Not else! Note that mIsBeingDragged can be set above.
                if (!isVertical && mIsBeingDragged) {
                    // Scroll to follow the motion event
                    final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                    final float x = ev.getX(activePointerIndex);
                    needsInvalidate |= performDragHorizontal(x);
                } else if (isVertical && mIsBeingDragged) {
                    // Scroll to follow the motion event
                    final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                    final float y = ev.getY(activePointerIndex);
                    needsInvalidate |= performDragVertical(y);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    //initialVelocityX for horizontal
                    int initialVelocityX = (int) VelocityTrackerCompat.getXVelocity(
                            velocityTracker, mActivePointerId);
                    //initialVelocityY for vertical
                    int initialVelocityY = (int) VelocityTrackerCompat.getYVelocity(
                            velocityTracker, mActivePointerId);
                    mVelocityY = initialVelocityY;
                    mPopulatePending = true;
                    final int width = getClientWidth();
                    final int height = getClientHeight();

                    final int scrollX = getScrollX();
                    final int scrollY = getScrollY();

                    final ItemInfo iiHorizontal = infoForCurrentScrollPositionHorizontal();
                    final ItemInfo iiVertical = infoForCurrentScrollPositionVertical();

                    final float marginOffsetHorizontal = (float) mPageMargin / width;
                    final float marginOffsetVertical = (float) mPageMargin / height;
                    final int currentPageHorizontal = iiHorizontal.position;
                    final int currentPageVertical = iiVertical.position;

                    final float pageOffsetHorizontal = (((float) scrollX / width) - iiHorizontal.offset)
                            / (iiHorizontal.widthFactor + marginOffsetHorizontal);
                    final float pageOffsetVertical = (((float) scrollY / height) - iiVertical.offset)
                            / (iiVertical.widthFactor + marginOffsetVertical);

                    final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                    final float x = ev.getX(activePointerIndex);
                    final float y = ev.getY(activePointerIndex);

                    final int totalDeltaHorizontal = (int) (x - mInitialMotionX);
                    final int totalDeltaVertical = (int) (y - mInitialMotionY);
                    int nextPage = isVertical ? determineTargetPageVertical(currentPageVertical, pageOffsetVertical, initialVelocityY, totalDeltaVertical)
                            : determineTargetPageHorizontal(currentPageHorizontal, pageOffsetHorizontal, initialVelocityX, totalDeltaHorizontal);
                    if (!isVertical) {
                        setCurrentItemInternalHorizontal(nextPage, true, true, initialVelocityX);
                    } else {
                        //initialVelocityY>0 ==> swipe down ELSE swipe up
                        setCurrentItemInternalVertical(nextPage, true, true, initialVelocityY);
                    }
                    needsInvalidate = resetTouch();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (!isVertical && mIsBeingDragged) {
                    scrollToItemHorizontal(mCurItem, true, 0, false);
                } else if (isVertical && mIsBeingDragged) {
                    scrollToItemVertical(mCurItem, true, 0, false);
                }
                needsInvalidate = resetTouch();
                break;
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                final float x = ev.getX(index);
                final float y = ev.getY(index);
                if (!isVertical) {
                    mLastMotionX = x;
                } else {
                    mLastMotionY = y;
                }
                mActivePointerId = ev.getPointerId(index);
                break;
            }
            case MotionEventCompat.ACTION_POINTER_UP:
                if (!isVertical) {
                    onSecondaryPointerUpHorizontal(ev);
                    mLastMotionX = ev.getX(ev.findPointerIndex(mActivePointerId));
                } else {
                    onSecondaryPointerUpVertical(ev);
                    mLastMotionY = ev.getY(ev.findPointerIndex(mActivePointerId));
                }
                break;
        }
        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
        return true;
    }

    private boolean resetTouch() {
        boolean needsInvalidate;
        mActivePointerId = INVALID_POINTER;
        endDrag();

        needsInvalidate = isVertical ? mTopEdge.onRelease() | mBottomEdge.onRelease() :
                mLeftEdge.onRelease() | mRightEdge.onRelease();
        return needsInvalidate;
    }

    private void requestParentDisallowInterceptTouchEvent(boolean disallowIntercept) {
        final ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }

    private boolean performDragHorizontal(float x) {
        boolean needsInvalidate = false;
        final float deltaX = mLastMotionX - x;
        mLastMotionX = x;

        float oldScrollX = getScrollX();
        float scrollX = oldScrollX + deltaX;
        final int width = getClientWidth();

        float leftBound = width * mFirstOffset;
        float rightBound = width * mLastOffset;

        boolean leftAbsolute = true;
        boolean rightAbsolute = true;

        final ItemInfo firstItem = mItems.get(0);
        final ItemInfo lastItem = mItems.get(mItems.size() - 1);
        if (firstItem.position != 0) {
            leftAbsolute = false;
            leftBound = firstItem.offset * width;
        }

        if (lastItem.position != getAdapterCount() - 1) {
            rightAbsolute = false;
            rightBound = lastItem.offset * width;
        }

        if (scrollX < leftBound) {
            if (leftAbsolute) {
                float over = leftBound - scrollX;
                needsInvalidate = mLeftEdge.onPull(Math.abs(over) / width);
            }
            scrollX = leftBound;
        } else if (scrollX > rightBound) {
            if (rightAbsolute) {
                float over = scrollX - rightBound;
                needsInvalidate = mRightEdge.onPull(Math.abs(over) / width);
            }
            scrollX = rightBound;
        }
        // Don't lose the rounded component
        mLastMotionX += scrollX - (int) scrollX;
        scrollTo((int) scrollX, getScrollY());
        pageScrolledHorizontal((int) scrollX);
        return needsInvalidate;
    }

    private boolean performDragVertical(float y) {
        boolean needsInvalidate = false;

        final float deltaY = mLastMotionY - y;
        mLastMotionY = y;

        float oldScrollY = getScrollY();
        float scrollY = oldScrollY + deltaY;
        final int height = getClientHeight();

        float topBound = height * mFirstOffset;
        float bottomBound = height * mLastOffset;

        boolean topAbsolute = true;
        boolean bottomAbsolute = true;

        final ItemInfo firstItem = mItems.get(0);
        final ItemInfo lastItem = mItems.get(mItems.size() - 1);
        if (firstItem.position != 0) {
            topAbsolute = false;
            topBound = firstItem.offset * height;
        }

        if (lastItem.position != getAdapterCount() - 1) {
            bottomAbsolute = false;
            bottomBound = lastItem.offset * height;
        }

        if (scrollY < topBound) {
            if (topAbsolute) {
                float over = topBound - scrollY;
                needsInvalidate = mTopEdge.onPull(Math.abs(over) / height);
            }
            scrollY = topBound;
        } else if (scrollY > bottomBound) {
            if (bottomAbsolute) {
                float over = scrollY - bottomBound;
                needsInvalidate = mBottomEdge.onPull(Math.abs(over) / height);
            }
            scrollY = bottomBound;
        }
        // Don't lose the rounded component
        mLastMotionY += scrollY - (int) scrollY;
        scrollTo(getScrollX(), (int) scrollY);
        pageScrolledVertical((int) scrollY);
        return needsInvalidate;
    }

    /**
     * @return Info about the page at the current scroll position.
     * This can be synthetic for a missing middle page; the 'object' field can be null.
     */

    private ItemInfo infoForCurrentScrollPositionHorizontal() {
        final int width = getClientWidth();
        final float scrollOffset = width > 0 ? (float) getScrollX() / width : 0;
        final float marginOffset = width > 0 ? (float) mPageMargin / width : 0;

        int lastPos = -1;
        float lastOffset = 0.f;
        float lastWidth = 0.f;
        boolean first = true;

        ItemInfo lastItem = null;
        for (int i = 0; i < mItems.size(); i++) {
            ItemInfo ii = mItems.get(i);
            float offset;


            if (!first && ii.position != lastPos + 1) {
                // Create a synthetic item for a missing page.
                ii = mTempItem;
                ii.offset = lastOffset + lastWidth + marginOffset;
                ii.position = lastPos + 1;
                ii.widthFactor = mAdapter.getPageWidth(ii.position);
                i--;
            }
            offset = ii.offset;

            final float leftBound = offset;

            final float rightBound = offset + ii.widthFactor + marginOffset;

            if (first || scrollOffset >= leftBound) {
                if (scrollOffset < rightBound || i == mItems.size() - 1) {
                    return ii;
                }
            } else {
                return lastItem;
            }

            first = false;
            lastPos = ii.position;
            lastOffset = offset;
            lastWidth = ii.widthFactor;
            lastItem = ii;

        }
        return lastItem;
    }

    private ItemInfo infoForCurrentScrollPositionVertical() {
        final int height = getClientHeight();

        final float scrollOffset = height > 0 ? (float) getScrollY() / height : 0;
        final float marginOffset = height > 0 ? (float) mPageMargin / height : 0;

        int lastPos = -1;
        float lastOffset = 0.f;
        float lastHeight = 0.f;
        boolean first = true;

        ItemInfo lastItem = null;
        for (int i = 0; i < mItems.size(); i++) {
            ItemInfo ii = mItems.get(i);
            float offset;


            if (!first && ii.position != lastPos + 1) {
                // Create a synthetic item for a missing page.
                ii = mTempItem;
                ii.offset = lastOffset + lastHeight + marginOffset;
                ii.position = lastPos + 1;
                ii.widthFactor = mAdapter.getPageWidth(ii.position);
                i--;
            }

            offset = ii.offset;

            final float topBound = offset;

            final float bottomBound = offset + ii.widthFactor + marginOffset;

            if (first || scrollOffset >= topBound) {
                if (scrollOffset < bottomBound || i == mItems.size() - 1) {
                    return ii;
                }
            } else {
                return lastItem;
            }

            first = false;
            lastPos = ii.position;
            lastOffset = offset;
            lastHeight = ii.widthFactor;
            lastItem = ii;
        }
        return lastItem;
    }

    private int determineTargetPageHorizontal(int currentPage, float pageOffset, int velocity, int deltaX) {
        int targetPage;
        if (Math.abs(deltaX) > mFlingDistance && Math.abs(velocity) > mMinimumVelocity) {
            targetPage = velocity > 0 ? currentPage : currentPage + 1;
        } else {
            final float truncator = currentPage >= mCurItem ? 0.4f : 0.6f;
            targetPage = currentPage + (int) (pageOffset + truncator);
        }
        if (mItems.size() > 0) {
            final YViewPager.ItemInfo firstItem = mItems.get(0);
            final YViewPager.ItemInfo lastItem = mItems.get(mItems.size() - 1);
            // Only let the user target pages we have items for
            targetPage = Math.max(firstItem.position, Math.min(targetPage, lastItem.position));
        }
        return targetPage;
    }

    private int determineTargetPageVertical(int currentPage, float pageOffset, int velocity, int deltaY) {
        int targetPage;
        if (Math.abs(deltaY) > mFlingDistance && Math.abs(velocity) > mMinimumVelocity) {
            targetPage = velocity > 0 ? currentPage : currentPage + 1;
        } else {
            final float truncator = currentPage >= mCurItem ? 0.4f : 0.6f;
            targetPage = currentPage + (int) (pageOffset + truncator);
        }
        if (mItems.size() > 0) {
            final ItemInfo firstItem = mItems.get(0);
            final ItemInfo lastItem = mItems.get(mItems.size() - 1);
            // Only let the user target pages we have items for
            targetPage = Math.max(firstItem.position, Math.min(targetPage, lastItem.position));
        }
        return targetPage;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        boolean needsInvalidate = false;

        final int overScrollMode = getOverScrollMode();
        if (overScrollMode == View.OVER_SCROLL_ALWAYS
                || (overScrollMode == View.OVER_SCROLL_IF_CONTENT_SCROLLS
                && mAdapter != null && getAdapterCount() > 1)) {
            if (!isVertical && !mLeftEdge.isFinished()) {
                final int restoreCount = canvas.save();
                final int height = getHeight() - getPaddingTop() - getPaddingBottom();
                final int width = getWidth();
                canvas.rotate(270);
                canvas.translate(-height + getPaddingTop(), mFirstOffset * width);
                mLeftEdge.setSize(height, width);
                needsInvalidate |= mLeftEdge.draw(canvas);
                canvas.restoreToCount(restoreCount);
            } else if (isVertical && !mTopEdge.isFinished()) {
                final int restoreCount = canvas.save();
                final int height = getHeight();
                final int width = getWidth() - getPaddingLeft() - getPaddingRight();
                canvas.translate(getPaddingLeft(), mFirstOffset * height);
                mTopEdge.setSize(width, height);
                needsInvalidate |= mTopEdge.draw(canvas);
                canvas.restoreToCount(restoreCount);
            }
            if (!isVertical && !mRightEdge.isFinished()) {
                final int restoreCount = canvas.save();
                final int width = getWidth();
                final int height = getHeight() - getPaddingTop() - getPaddingBottom();

                canvas.rotate(90);
                canvas.translate(-getPaddingTop(), -(mLastOffset + 1) * width);
                mRightEdge.setSize(height, width);
                needsInvalidate |= mRightEdge.draw(canvas);
                canvas.restoreToCount(restoreCount);
            } else if (isVertical && !mBottomEdge.isFinished()) {
                final int restoreCount = canvas.save();
                final int width = getWidth() - getPaddingLeft() - getPaddingRight();
                final int height = getHeight();
                canvas.rotate(180, width, 0);
                canvas.translate(width - getPaddingLeft(), -(mLastOffset + 1) * height);
                mBottomEdge.setSize(width, height);
                needsInvalidate |= mBottomEdge.draw(canvas);
                canvas.restoreToCount(restoreCount);
            }
        } else if (!isVertical) {
            mLeftEdge.finish();
            mRightEdge.finish();
        } else if (isVertical) {
            mTopEdge.finish();
            mBottomEdge.finish();
        }

        if (needsInvalidate) {
            // Keep animating
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the margin drawable between pages if needed.
        if (mPageMargin > 0 && mMarginDrawable != null && mItems.size() > 0 && mAdapter != null) {
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();

            final int width = getWidth();
            final int height = getHeight();

            final float marginOffsetHorizontal = (float) mPageMargin / width;
            final float marginOffsetVertical = (float) mPageMargin / height;

            int itemIndex = 0;
            ItemInfo ii = mItems.get(0);
            float offset = ii.offset;
            final int itemCount = mItems.size();
            final int firstPos = ii.position;
            final int lastPos = mItems.get(itemCount - 1).position;
            for (int pos = firstPos; pos < lastPos; pos++) {
                while (pos > ii.position && itemIndex < itemCount) {
                    ii = mItems.get(++itemIndex);
                }

                float drawAt = 0;
                if (!isVertical && pos == ii.position) {
                    drawAt = (ii.offset + ii.widthFactor) * width;
                    offset = ii.offset + ii.widthFactor + marginOffsetHorizontal;
                } else if (!isVertical) {
                    float widthFactor = mAdapter.getPageWidth(pos);
                    drawAt = (offset + widthFactor) * width;
                    offset += widthFactor + marginOffsetHorizontal;
                } else if (isVertical && pos == ii.position) {
                    drawAt = (ii.offset + ii.widthFactor) * height;
                    offset = ii.offset + ii.widthFactor + marginOffsetVertical;
                } else if (isVertical) {
                    float widthFactor = mAdapter.getPageWidth(pos);
                    drawAt = (offset + widthFactor) * height;
                    offset += widthFactor + marginOffsetVertical;
                }

                if (!isVertical && drawAt + mPageMargin > scrollX) {
                    mMarginDrawable.setBounds(Math.round(drawAt), mTopPageBounds,
                            Math.round(drawAt + mPageMargin), mBottomPageBounds);
                    mMarginDrawable.draw(canvas);
                } else if (isVertical && drawAt + mPageMargin > scrollY) {
                    mMarginDrawable.setBounds(Math.round(drawAt), mTopPageBounds,
                            Math.round(drawAt + mPageMargin), mRightPageBounds);
                    mMarginDrawable.draw(canvas);
                }

                if (!isVertical && drawAt > scrollX + width) {
                    break; // No more visible, no sense in continuing
                } else if (isVertical && drawAt > scrollY + height) {
                    break;
                }
            }
        }
    }


    public boolean beginFakeDrag() {
        if (mIsBeingDragged) {
            return false;
        }
        mFakeDragging = true;
        setScrollState(SCROLL_STATE_DRAGGING);
        mInitialMotionX = mLastMotionX = 0;
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
        final long time = SystemClock.uptimeMillis();
        final MotionEvent ev = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, 0, 0, 0);
        mVelocityTracker.addMovement(ev);
        ev.recycle();
        mFakeDragBeginTime = time;
        return true;
    }

    /**
     * End a fake drag of the pager.
     *
     * @see #beginFakeDrag()
     * @see #fakeDragBy(float)
     */
//    public void endFakeDrag() {
//        if (!mFakeDragging) {
//            throw new IllegalStateException("No fake drag in progress. Call beginFakeDrag first.");
//        }
//
//        if (mAdapter != null) {
//            final VelocityTracker velocityTracker = mVelocityTracker;
//            velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
//            int initialVelocity = (int) VelocityTrackerCompat.getXVelocity(
//                    velocityTracker, mActivePointerId);
//            mPopulatePending = true;
//            final int width = getClientWidth();
//            final int scrollX = getScrollX();
//            final YViewPager.ItemInfo ii = infoForCurrentScrollPosition();
//            final int currentPage = ii.position;
//            final float pageOffset = (((float) scrollX / width) - ii.offset) / ii.widthFactor;
//            final int totalDelta = (int) (mLastMotionX - mInitialMotionX);
//            int nextPage = determineTargetPage(currentPage, pageOffset, initialVelocity,
//                    totalDelta);
//            setCurrentItemInternal(nextPage, true, true, initialVelocity);
//        }
//        endDrag();
//
//        mFakeDragging = false;
//    }

    /**
     * Fake drag by an offset in pixels. You must have called {@link #beginFakeDrag()} first.
     *
     * @param xOffset Offset in pixels to drag by.
     * @see #beginFakeDrag()
     * @see #endFakeDrag()
     */
//    public void fakeDragBy(float xOffset) {
//        if (!mFakeDragging) {
//            throw new IllegalStateException("No fake drag in progress. Call beginFakeDrag first.");
//        }
//
//        if (mAdapter == null) {
//            return;
//        }
//
//        mLastMotionX += xOffset;
//
//        float oldScrollX = getScrollX();
//        float scrollX = oldScrollX - xOffset;
//        final int width = getClientWidth();
//
//        float leftBound = width * mFirstOffset;
//        float rightBound = width * mLastOffset;
//
//        final YViewPager.ItemInfo firstItem = mItems.get(0);
//        final YViewPager.ItemInfo lastItem = mItems.get(mItems.size() - 1);
//        if (firstItem.position != 0) {
//            leftBound = firstItem.offset * width;
//        }
//        if (lastItem.position != mAdapter.getCount() - 1) {
//            rightBound = lastItem.offset * width;
//        }
//
//        if (scrollX < leftBound) {
//            scrollX = leftBound;
//        } else if (scrollX > rightBound) {
//            scrollX = rightBound;
//        }
//        // Don't lose the rounded component
//        mLastMotionX += scrollX - (int) scrollX;
//        scrollTo((int) scrollX, getScrollY());
//        pageScrolled((int) scrollX);
//
//        // Synthesize an event for the VelocityTracker.
//        final long time = SystemClock.uptimeMillis();
//        final MotionEvent ev = MotionEvent.obtain(mFakeDragBeginTime, time, MotionEvent.ACTION_MOVE,
//                mLastMotionX, 0, 0);
//        mVelocityTracker.addMovement(ev);
//        ev.recycle();
//    }

    /**
     * Returns true if a fake drag is in progress.
     *
     * @return true if currently in a fake drag, false otherwise.
     * @see #beginFakeDrag()
     */
    public boolean isFakeDragging() {
        return mFakeDragging;
    }

    private void onSecondaryPointerUpHorizontal(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionX = ev.getX(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
        }
    }

    private void onSecondaryPointerUpVertical(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionY = ev.getY(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
        }
    }

    private void endDrag() {
        mIsBeingDragged = false;
        mIsUnableToDrag = false;

        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private void setScrollingCacheEnabled(boolean enabled) {
        if (mScrollingCacheEnabled != enabled) {
            mScrollingCacheEnabled = enabled;
            if (USE_CACHE) {
                final int size = getChildCount();
                for (int i = 0; i < size; ++i) {
                    final View child = getChildAt(i);
                    if (child.getVisibility() != GONE) {
                        child.setDrawingCacheEnabled(enabled);
                    }
                }
            }
        }
    }

    /**
     * Check if this ViewPager can be scrolled horizontally in a certain direction.
     *
     * @param direction Negative to check scrolling left, positive to check scrolling right.
     * @return Whether this ViewPager can be scrolled in the specified direction. It will always
     * return false if the specified direction is 0.
     */
    public boolean canScrollHorizontally(int direction) {
        if (mAdapter == null) {
            return false;
        }

        final int width = getClientWidth();
        final int scrollX = getScrollX();
        if (direction < 0) {
            return (scrollX > (int) (width * mFirstOffset));
        } else if (direction > 0) {
            return (scrollX < (int) (width * mLastOffset));
        } else {
            return false;
        }
    }

    /**
     * Tests scrollability within child views of v given a delta of dx.
     *
     * @param v      View to test for horizontal scrollability
     * @param checkV Whether the view v passed should itself be checked for scrollability (true),
     *               or just its children (false).
     * @param dx     Delta scrolled in pixels
     * @param x      X coordinate of the active touch point
     * @param y      Y coordinate of the active touch point
     * @return true if child views of v can be scrolled by delta of dx.
     */
    protected boolean canScrollHorizontal(View v, boolean checkV, int dx, int x, int y) {
        if (v instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) v;
            final int scrollX = v.getScrollX();
            final int scrollY = v.getScrollY();
            final int count = group.getChildCount();
            for (int i = count - 1; i >= 0; i--) {
                // TODO: Add versioned support here for transformed views.
                final View child = group.getChildAt(i);
                if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight()
                        && y + scrollY >= child.getTop() && y + scrollY < child.getBottom()
                        && canScrollHorizontal(child, true, dx, x + scrollX - child.getLeft(),
                        y + scrollY - child.getTop())) {
                    return true;
                }
            }
        }
        return checkV && ViewCompat.canScrollHorizontally(v, -dx);
    }

    /**
     * Tests scrollability within child views of v given a delta of dx.
     *
     * @param v      View to test for horizontal scrollability
     * @param checkV Whether the view v passed should itself be checked for scrollability (true),
     *               or just its children (false).
     * @param dy     Delta scrolled in pixels
     * @param x      X coordinate of the active touch point
     * @param y      Y coordinate of the active touch point
     * @return true if child views of v can be scrolled by delta of dx.
     */
    protected boolean canScrollVertical(View v, boolean checkV, int dy, int x, int y) {
        if (v instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) v;
            final int scrollX = v.getScrollX();
            final int scrollY = v.getScrollY();
            final int count = group.getChildCount();
            // Count backwards - let topmost views consume scroll distance first.
            for (int i = count - 1; i >= 0; i--) {
                // TODO: Add versioned support here for transformed views.
                // This will not work for transformed views in Honeycomb+
                final View child = group.getChildAt(i);

                if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight()
                        && y + scrollY >= child.getTop() && y + scrollY < child.getBottom()
                        && canScrollVertical(child, true, dy, x + scrollX - child.getLeft(),
                        y + scrollY - child.getTop())) {
                    return true;
                }
            }
        }
        return checkV && ViewCompat.canScrollVertically(v, -dy);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Let the focused view and/or our descendants get the key first
        return super.dispatchKeyEvent(event) || executeKeyEvent(event);
    }

    /**
     * You can call this function yourself to have the scroll view perform
     * scrolling from a key event, just as if the event had been dispatched to
     * it by the view hierarchy.
     *
     * @param event The key event to execute.
     * @return Return true if the event was handled, else false.
     */
    public boolean executeKeyEvent(KeyEvent event) {
        boolean handled = false;
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    handled = arrowScroll(FOCUS_LEFT);
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    handled = arrowScroll(FOCUS_RIGHT);
                    break;
                case KeyEvent.KEYCODE_TAB:
                    if (Build.VERSION.SDK_INT >= 11) {
                        // The focus finder had a bug handling FOCUS_FORWARD and FOCUS_BACKWARD
                        // before Android 3.0. Ignore the tab key on those devices.
                        if (KeyEventCompat.hasNoModifiers(event)) {
                            handled = arrowScroll(FOCUS_FORWARD);
                        } else if (KeyEventCompat.hasModifiers(event, KeyEvent.META_SHIFT_ON)) {
                            handled = arrowScroll(FOCUS_BACKWARD);
                        }
                    }
                    break;
            }
        }
        return handled;
    }

    /**
     * Handle scrolling in response to a left or right arrow click.
     *
     * @param direction The direction corresponding to the arrow key that was pressed. It should be
     *                  either {@link View#FOCUS_LEFT} or {@link View#FOCUS_RIGHT}.
     * @return Whether the scrolling was handled successfully.
     */
    public boolean arrowScroll(int direction) {
        View currentFocused = findFocus();
        if (currentFocused == this) {
            currentFocused = null;
        } else if (currentFocused != null) {
            boolean isChild = false;
            for (ViewParent parent = currentFocused.getParent(); parent instanceof ViewGroup;
                 parent = parent.getParent()) {
                if (parent == this) {
                    isChild = true;
                    break;
                }
            }
            if (!isChild) {
                // This would cause the focus search down below to fail in fun ways.
                final StringBuilder sb = new StringBuilder();
                sb.append(currentFocused.getClass().getSimpleName());
                for (ViewParent parent = currentFocused.getParent(); parent instanceof ViewGroup;
                     parent = parent.getParent()) {
                    sb.append(" => ").append(parent.getClass().getSimpleName());
                }
                Log.e(TAG, "arrowScroll tried to find focus based on non-child "
                        + "current focused view " + sb.toString());
                currentFocused = null;
            }
        }

        boolean handled = false;

        View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused,
                direction);
        if (nextFocused != null && nextFocused != currentFocused) {
            if (direction == View.FOCUS_LEFT) {
                // If there is nothing to the left, or this is causing us to
                // jump to the right, then what we really want to do is page left.
                final int nextLeft = getChildRectInPagerCoordinates(mTempRect, nextFocused).left;
                final int currLeft = getChildRectInPagerCoordinates(mTempRect, currentFocused).left;
                if (currentFocused != null && nextLeft >= currLeft) {
                    handled = pageLeft();
                } else {
                    handled = nextFocused.requestFocus();
                }
            } else if (direction == View.FOCUS_RIGHT) {
                // If there is nothing to the right, or this is causing us to
                // jump to the left, then what we really want to do is page right.
                final int nextLeft = getChildRectInPagerCoordinates(mTempRect, nextFocused).left;
                final int currLeft = getChildRectInPagerCoordinates(mTempRect, currentFocused).left;
                if (currentFocused != null && nextLeft <= currLeft) {
                    handled = pageRight();
                } else {
                    handled = nextFocused.requestFocus();
                }
            }
        } else if (direction == FOCUS_LEFT || direction == FOCUS_BACKWARD) {
            // Trying to move left and nothing there; try to page.
            handled = pageLeft();
        } else if (direction == FOCUS_RIGHT || direction == FOCUS_FORWARD) {
            // Trying to move right and nothing there; try to page.
            handled = pageRight();
        }
        if (handled) {
            playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction));
        }
        return handled;
    }

    private Rect getChildRectInPagerCoordinates(Rect outRect, View child) {
        if (outRect == null) {
            outRect = new Rect();
        }
        if (child == null) {
            outRect.set(0, 0, 0, 0);
            return outRect;
        }
        outRect.left = child.getLeft();
        outRect.right = child.getRight();
        outRect.top = child.getTop();
        outRect.bottom = child.getBottom();

        ViewParent parent = child.getParent();
        while (parent instanceof ViewGroup && parent != this) {
            final ViewGroup group = (ViewGroup) parent;
            outRect.left += group.getLeft();
            outRect.right += group.getRight();
            outRect.top += group.getTop();
            outRect.bottom += group.getBottom();

            parent = group.getParent();
        }
        return outRect;
    }

    boolean pageLeft() {
        if (mCurItem > 0) {
            setCurrentItem(mCurItem - 1, true);
            return true;
        }
        return false;
    }

    boolean pageRight() {
        if (mAdapter != null && mCurItem < (getAdapterCount() - 1)) {
            setCurrentItem(mCurItem + 1, true);
            return true;
        }
        return false;
    }

    /**
     * We only want the current page that is being shown to be focusable.
     */
    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        final int focusableCount = views.size();

        final int descendantFocusability = getDescendantFocusability();

        if (descendantFocusability != FOCUS_BLOCK_DESCENDANTS) {
            for (int i = 0; i < getChildCount(); i++) {
                final View child = getChildAt(i);
                if (child.getVisibility() == VISIBLE) {
                    YViewPager.ItemInfo ii = infoForChild(child);
                    if (ii != null && ii.position == mCurItem) {
                        child.addFocusables(views, direction, focusableMode);
                    }
                }
            }
        }

        // we add ourselves (if focusable) in all cases except for when we are
        // FOCUS_AFTER_DESCENDANTS and there are some descendants focusable.  this is
        // to avoid the focus search finding layouts when a more precise search
        // among the focusable children would be more interesting.
        if (descendantFocusability != FOCUS_AFTER_DESCENDANTS
                || (focusableCount == views.size())) { // No focusable descendants
            // Note that we can't call the superclass here, because it will
            // add all views in.  So we need to do the same thing View does.
            if (!isFocusable()) {
                return;
            }
            if ((focusableMode & FOCUSABLES_TOUCH_MODE) == FOCUSABLES_TOUCH_MODE
                    && isInTouchMode() && !isFocusableInTouchMode()) {
                return;
            }
            if (views != null) {
                views.add(this);
            }
        }
    }

    /**
     * We only want the current page that is being shown to be touchable.
     */
    @Override
    public void addTouchables(ArrayList<View> views) {
        // Note that we don't call super.addTouchables(), which means that
        // we don't call View.addTouchables().  This is okay because a ViewPager
        // is itself not touchable.
        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == VISIBLE) {
                YViewPager.ItemInfo ii = infoForChild(child);
                if (ii != null && ii.position == mCurItem) {
                    child.addTouchables(views);
                }
            }
        }
    }

    /**
     * We only want the current page that is being shown to be focusable.
     */
    @Override
    protected boolean onRequestFocusInDescendants(int direction,
                                                  Rect previouslyFocusedRect) {
        int index;
        int increment;
        int end;
        int count = getChildCount();
        if ((direction & FOCUS_FORWARD) != 0) {
            index = 0;
            increment = 1;
            end = count;
        } else {
            index = count - 1;
            increment = -1;
            end = -1;
        }
        for (int i = index; i != end; i += increment) {
            View child = getChildAt(i);
            if (child.getVisibility() == VISIBLE) {
                YViewPager.ItemInfo ii = infoForChild(child);
                if (ii != null && ii.position == mCurItem) {
                    if (child.requestFocus(direction, previouslyFocusedRect)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        // Dispatch scroll events from this ViewPager.
        if (event.getEventType() == AccessibilityEventCompat.TYPE_VIEW_SCROLLED) {
            return super.dispatchPopulateAccessibilityEvent(event);
        }

        // Dispatch all other accessibility events from the current page.
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == VISIBLE) {
                final YViewPager.ItemInfo ii = infoForChild(child);
                if (ii != null && ii.position == mCurItem
                        && child.dispatchPopulateAccessibilityEvent(event)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new YViewPager.LayoutParams();
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return generateDefaultLayoutParams();
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof YViewPager.LayoutParams && super.checkLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new YViewPager.LayoutParams(getContext(), attrs);
    }

    class MyAccessibilityDelegate extends AccessibilityDelegateCompat {

        @Override
        public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(host, event);
            event.setClassName(YViewPager.class.getName());
            final AccessibilityRecordCompat recordCompat =
                    AccessibilityEventCompat.asRecord(event);
            recordCompat.setScrollable(canScroll());
            if (event.getEventType() == AccessibilityEventCompat.TYPE_VIEW_SCROLLED
                    && mAdapter != null) {
                recordCompat.setItemCount(getAdapterCount());
                recordCompat.setFromIndex(mCurItem);
                recordCompat.setToIndex(mCurItem);
            }
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            info.setClassName(YViewPager.class.getName());
            info.setScrollable(canScroll());
            if (canScrollHorizontally(1)) {
                info.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
            }
            if (canScrollHorizontally(-1)) {
                info.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD);
            }
        }

        @Override
        public boolean performAccessibilityAction(View host, int action, Bundle args) {
            if (super.performAccessibilityAction(host, action, args)) {
                return true;
            }
            switch (action) {
                case AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD: {
                    if (canScrollHorizontally(1)) {
                        setCurrentItem(mCurItem + 1);
                        return true;
                    }
                }
                return false;
                case AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD: {
                    if (canScrollHorizontally(-1)) {
                        setCurrentItem(mCurItem - 1);
                        return true;
                    }
                }
                return false;
            }
            return false;
        }

        private boolean canScroll() {
            return (mAdapter != null) && (getAdapterCount() > 1);
        }
    }

    private class PagerObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            dataSetChanged();
        }

        @Override
        public void onInvalidated() {
            dataSetChanged();
        }
    }

    /**
     * Layout parameters that should be supplied for views added to a
     * ViewPager.
     */
    public static class LayoutParams extends ViewGroup.LayoutParams {
        /**
         * true if this view is a decoration on the pager itself and not
         * a view supplied by the adapter.
         */
        public boolean isDecor;

        /**
         * Gravity setting for use on decor views only:
         * Where to position the view page within the overall ViewPager
         * container; constants are defined in {@link Gravity}.
         */
        public int gravity;

        /**
         * Width as a 0-1 multiplier of the measured pager width
         */
        float widthFactor = 0.f;

        /**
         * true if this view was added during layout and needs to be measured
         * before being positioned.
         */
        boolean needsMeasure;

        /**
         * Adapter position this view is for if !isDecor
         */
        int position;

        /**
         * Current child index within the ViewPager that this view occupies
         */
        int childIndex;

        public LayoutParams() {
            super(MATCH_PARENT, MATCH_PARENT);
        }

        public LayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);

            final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
            gravity = a.getInteger(0, Gravity.TOP);
            a.recycle();
        }
    }

    static class ViewPositionComparator implements Comparator<View> {
        @Override
        public int compare(View lhs, View rhs) {
            final LayoutParams llp = (LayoutParams) lhs.getLayoutParams();
            final LayoutParams rlp = (LayoutParams) rhs.getLayoutParams();
            if (llp.isDecor != rlp.isDecor) {
                return llp.isDecor ? 1 : -1;
            }
            return llp.position - rlp.position;
        }
    }
}

