package cn.youngkaaa.yviewpagerdemo;

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
import android.support.annotation.CallSuper;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.support.v4.view.AbsSavedState;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.KeyEventCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.PagerAdapter;
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

/**
 * Layout manager that allows the user to flip left and right
 * through pages of data.  You supply an implementation of a
 * {PagerAdapter} to generate the pages that the view shows.
 * <p>
 * <p>ViewPager is most often used in conjunction with {@link android.app.Fragment},
 * which is a convenient way to supply and manage the lifecycle of each page.
 * There are standard adapters implemented for using fragments with the ViewPager,
 * which cover the most common use cases.  These are
 * { android.support.v4.app.FragmentYPagerAdapter} and
 * { android.support.v4.app.FragmentStatePPagerAdapter; each of these
 * classes have simple code showing how to build a full user interface
 * with them.
 * <p>
 * <p>Views which are annotated with the YViewPager1.DecorView annotation are treated as
 * part of the view pagers 'decor'. Each decor view's position can be controlled via
 * its {@code android:layout_gravity} attribute. For example:
 * <p>
 * <pre>
 * &lt;YViewPager1
 *     android:layout_width=&quot;match_parent&quot;
 *     android:layout_height=&quot;match_parent&quot;&gt;
 *
 *     &lt;android.support.v4.view.PagerTitleStrip
 *         android:layout_width=&quot;match_parent&quot;
 *         android:layout_height=&quot;wrap_content&quot;
 *         android:layout_gravity=&quot;top&quot; /&gt;
 *
 * &lt;/YViewPager1&gt;
 * </pre>
 * <p>
 * <p>For more information about how to use ViewPager, read <a
 * href="{@docRoot}training/implementing-navigation/lateral.html">Creating Swipe Views with
 * Tabs</a>.</p>
 * <p>
 * <p>Below is a more complicated example of ViewPager, using it in conjunction
 * with {@link android.app.ActionBar} tabs.  You can find other examples of using
 * ViewPager in the API 4+ Support Demos and API 13+ Support Demos sample code.
 * <p>
 * {@sample frameworks/support/samples/Support13Demos/src/com/example/android/supportv13/app/ActionBarTabsPager.java
 * complete}
 */

public class YViewPagerOrigin extends ViewGroup {
    private static final String TAG = "YViewPager1";
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

    /**
     * Used to track what the expected number of items in the adapter should be.
     * If the app changes this when we don't expect it, we'll throw a big obnoxious exception.
     */
    private int mExpectedAdapterCount;
    private int mDestY;
    private int mVelocityY = 100;

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

    private static final Comparator<YViewPagerOrigin.ItemInfo> COMPARATOR = new Comparator<YViewPagerOrigin.ItemInfo>() {
        @Override
        public int compare(YViewPagerOrigin.ItemInfo lhs, YViewPagerOrigin.ItemInfo rhs) {
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

    private final ArrayList<YViewPagerOrigin.ItemInfo> mItems = new ArrayList<YViewPagerOrigin.ItemInfo>();
    private final YViewPagerOrigin.ItemInfo mTempItem = new YViewPagerOrigin.ItemInfo();

    private final Rect mTempRect = new Rect();

    private PagerAdapter mAdapter;
    private int mCurItem;   // Index of currently displayed page.
    private int mRestoredCurItem = -1;
    private Parcelable mRestoredAdapterState = null;
    private ClassLoader mRestoredClassLoader = null;

    private Scroller mScroller;
    private boolean mIsScrollStarted;

    private YViewPagerOrigin.PagerObserver mObserver;

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
    private List<YViewPagerOrigin.OnAdapterChangeListener> mAdapterChangeListeners;
    private YViewPagerOrigin.PageTransformer mPageTransformer;
    private Method mSetChildrenDrawingOrderEnabled;

    private static final int DRAW_ORDER_DEFAULT = 0;
    private static final int DRAW_ORDER_FORWARD = 1;
    private static final int DRAW_ORDER_REVERSE = 2;
    private int mDrawingOrder;
    private ArrayList<View> mDrawingOrderedChildren;
    private static final ViewPositionComparator sPositionComparator = new ViewPositionComparator();

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


    private boolean isVertical = true;

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
         * @see YViewPagerOrigin#SCROLL_STATE_IDLE
         * @see YViewPagerOrigin#SCROLL_STATE_DRAGGING
         * @see YViewPagerOrigin#SCROLL_STATE_SETTLING
         */
        void onPageScrollStateChanged(int state);
    }

    /**
     * Simple implementation of the {@link YViewPagerOrigin.OnPageChangeListener} interface with stub
     * implementations of each method. Extend this if you do not intend to override
     * every method of {@link YViewPagerOrigin.OnPageChangeListener}.
     */
    public static class SimpleOnPageChangeListener implements YViewPagerOrigin.OnPageChangeListener {
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

    /**
     * A PageTransformer is invoked whenever a visible/attached page is scrolled.
     * This offers an opportunity for the application to apply a custom transformation
     * to the page views using animation properties.
     * <p>
     * <p>As property animation is only supported as of Android 3.0 and forward,
     * setting a PageTransformer on a ViewPager on earlier platform versions will
     * be ignored.</p>
     */
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
         *
         * @param viewPager  ViewPager where the adapter change has happened
         * @param oldAdapter the previously set adapter
         * @param newAdapter the newly set adapter
         */
        void onAdapterChanged(@NonNull YViewPagerOrigin viewPager,
                              @Nullable PagerAdapter oldAdapter, @Nullable PagerAdapter newAdapter);
    }

    /**
     * Annotation which allows marking of views to be decoration views when added to a view
     * pager.
     * <p>
     * <p>Views marked with this annotation can be added to the view pager with a layout resource.
     * An example being PagerTitleStrip.</p>
     * <p>
     * <p>You can also control whether a view is a decor view but setting
     * {@link YViewPagerOrigin.LayoutParams#isDecor} on the child's layout params.</p>
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    public @interface DecorView {
    }

    public YViewPagerOrigin(Context context) {
        super(context);
        initViewPager();
    }

    public YViewPagerOrigin(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViewPager();
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

        ViewCompat.setAccessibilityDelegate(this, new YViewPagerOrigin.MyAccessibilityDelegate());

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
    public void setAdapter(PagerAdapter adapter) {
        if (mAdapter != null) {
//            mAdapter.setViewPagerObserver(null);
            mAdapter.startUpdate(this);
            for (int i = 0; i < mItems.size(); i++) {
                final ItemInfo ii = mItems.get(i);
                mAdapter.destroyItem(this, ii.position, ii.object);
            }
            mAdapter.finishUpdate(this);
            mItems.clear();
            removeNonDecorViews();
            mCurItem = 0;
            scrollTo(0, 0);
        }

        final PagerAdapter oldAdapter = mAdapter;
        mAdapter = adapter;
        mExpectedAdapterCount = 0;

        if (mAdapter != null) {
            if (mObserver == null) {
                mObserver = new PagerObserver();
            }
//            mAdapter.setViewPagerObserver(mObserver);
            mPopulatePending = false;
            final boolean wasFirstLayout = mFirstLayout;
            mFirstLayout = true;
            mExpectedAdapterCount = mAdapter.getCount();
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
    public PagerAdapter getAdapter() {
        return mAdapter;
    }

    /**
     * Add a listener that will be invoked whenever the adapter for this ViewPager changes.
     *
     * @param listener listener to add
     */
    public void addOnAdapterChangeListener(@NonNull YViewPagerOrigin.OnAdapterChangeListener listener) {
        if (mAdapterChangeListeners == null) {
            mAdapterChangeListeners = new ArrayList<>();
        }
        mAdapterChangeListeners.add(listener);
    }

    /**
     * Remove a listener that was previously added via
     * {@link #addOnAdapterChangeListener(YViewPagerOrigin.OnAdapterChangeListener)}.
     *
     * @param listener listener to remove
     */
    public void removeOnAdapterChangeListener(@NonNull YViewPagerOrigin.OnAdapterChangeListener listener) {
        if (mAdapterChangeListeners != null) {
            mAdapterChangeListeners.remove(listener);
        }
    }

    /**
     * 将measuredWidth减去左右的padding 剩下的宽度就是 可用的宽度
     *
     * @return 可用的宽度
     */
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
        if (isVertical) {
            setCurrentItemInternalVertical(item, !mFirstLayout, false);
        } else {
            setCurrentItemInternalHorizontal(item, !mFirstLayout, false);
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
        if (isVertical) {
            setCurrentItemInternalVertical(item, smoothScroll, false);
        } else {
            setCurrentItemInternalHorizontal(item, smoothScroll, false);
        }
    }

    public int getCurrentItem() {
        return mCurItem;
    }

    void setCurrentItemInternalHorizontal(int item, boolean smoothScroll, boolean always) {
        setCurrentItemInternalHorizontal(item, smoothScroll, always, 0);
    }

    void setCurrentItemInternalVertical(int item, boolean smoothScroll, boolean always) {
        setCurrentItemInternalVertical(item, smoothScroll, always, 0);
    }

    void setCurrentItemInternalHorizontal(int item, boolean smoothScroll, boolean always, int velocity) {
        if (mAdapter == null || mAdapter.getCount() <= 0) {
            setScrollingCacheEnabled(false);
            return;
        }
        if (!always && mCurItem == item && mItems.size() != 0) {
            setScrollingCacheEnabled(false);
            return;
        }

        if (item < 0) {
            item = 0;
        } else if (item >= mAdapter.getCount()) {
            item = mAdapter.getCount() - 1;
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
        if (mAdapter == null || mAdapter.getCount() <= 0) {
            setScrollingCacheEnabled(false);
            return;
        }
        if (!always && mCurItem == item && mItems.size() != 0) {
            setScrollingCacheEnabled(false);
            return;
        }

        if (item < 0) {
            item = 0;
        } else if (item >= mAdapter.getCount()) {
            item = mAdapter.getCount() - 1;
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
        //使用传入的item (position)来拿到对应的ItemInfo 具体的infoForPosition()方法功能可以看该方法的注释
        final ItemInfo curInfo = infoForPosition(item);
        int destX = 0;
        //如果该item(position)对应的curInfo存在的话
        if (curInfo != null) {
            //getClientWidth()返回的是当前ViewPager的measuredWidth减去左右的padding值所剩下的child View可用宽度大小
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
        //使用传入的item (position)来拿到对应的ItemInfo 具体的infoForPosition()方法功能可以看该方法的注释
        final ItemInfo curInfo = infoForPosition(item);
        int destY = 0;
        //如果该item(position)对应的curInfo存在的话
        if (curInfo != null) {
            //getClientWidth()返回的是当前ViewPager的measuredWidth减去左右的padding值所剩下的child View可用宽度大小
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
     * scrolled. See {@link YViewPagerOrigin.OnPageChangeListener}.
     *
     * @param listener Listener to set
     * @deprecated Use {@link #addOnPageChangeListener(YViewPagerOrigin.OnPageChangeListener)}
     * and {@link #removeOnPageChangeListener(YViewPagerOrigin.OnPageChangeListener)} instead.
     */
    @Deprecated
    public void setOnPageChangeListener(YViewPagerOrigin.OnPageChangeListener listener) {
        mOnPageChangeListener = listener;
    }

    /**
     * Add a listener that will be invoked whenever the page changes or is incrementally
     * scrolled. See {@link YViewPagerOrigin.OnPageChangeListener}.
     * <p>
     * <p>Components that add a listener should take care to remove it when finished.
     * Other components that take ownership of a view may call {@link #clearOnPageChangeListeners()}
     * to remove all attached listeners.</p>
     *
     * @param listener listener to add
     */
    public void addOnPageChangeListener(YViewPagerOrigin.OnPageChangeListener listener) {
        if (mOnPageChangeListeners == null) {
            mOnPageChangeListeners = new ArrayList<>();
        }
        mOnPageChangeListeners.add(listener);
    }

    /**
     * Remove a listener that was previously added via
     * {@link #addOnPageChangeListener(YViewPagerOrigin.OnPageChangeListener)}.
     *
     * @param listener listener to remove
     */
    public void removeOnPageChangeListener(YViewPagerOrigin.OnPageChangeListener listener) {
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

    /**
     * Set a {@link YViewPagerOrigin.PageTransformer} that will be called for each attached page whenever
     * the scroll position is changed. This allows the application to apply custom property
     * transformations to each page, overriding the default sliding look and feel.
     * <p>
     * <p><em>Note:</em> Prior to Android 3.0 the property animation APIs did not exist.
     * As a result, setting a PageTransformer prior to Android 3.0 (API 11) will have no effect.</p>
     *
     * @param reverseDrawingOrder true if the supplied PageTransformer requires page views
     *                            to be drawn from last to first instead of first to last.
     * @param transformer         PageTransformer that will modify each page's animation properties
     */
    public void setPageTransformer(boolean reverseDrawingOrder, YViewPagerOrigin.PageTransformer transformer) {
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
                ((YViewPagerOrigin.LayoutParams) mDrawingOrderedChildren.get(index).getLayoutParams()).childIndex;
        return result;
    }

    /**
     * Set a separate OnPageChangeListener for internal use by the support library.
     *
     * @param listener Listener to set
     * @return The old listener that was set, if any.
     */
    YViewPagerOrigin.OnPageChangeListener setInternalPageChangeListener(YViewPagerOrigin.OnPageChangeListener listener) {
        YViewPagerOrigin.OnPageChangeListener oldListener = mInternalPageChangeListener;
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

    /**
     * Set the number of pages that should be retained to either side of the
     * current page in the view hierarchy in an idle state. Pages beyond this
     * limit will be recreated from the adapter when needed.
     * <p>
     * <p>This is offered as an optimization. If you know in advance the number
     * of pages you will need to support or have lazy-loading mechanisms in place
     * on your pages, tweaking this setting can have benefits in perceived smoothness
     * of paging animations and interaction. If you have a small number of pages (3-4)
     * that you can keep active all at once, less time will be spent in layout for
     * newly created view subtrees as the user pages back and forth.</p>
     * <p>
     * <p>You should keep this limit low, especially if your pages have complex layouts.
     * This setting defaults to 1.</p>
     *
     * @param limit How many pages will be kept offscreen in an idle state.
     */
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
    public void setPageMarginDrawable(@DrawableRes int resId) {
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
        //先判断是否存在子类 如果不存在就直接return掉
        if (getChildCount() == 0) {
            // Nothing to do.
            setScrollingCacheEnabled(false);
            return;
        }
        int sx;
        //是否正在滚动 true表示正在滑动    false表示已经滑动已经结束了
        boolean wasScrolling = (mScroller != null) && !mScroller.isFinished();
        //如果正在滑动的话
        if (wasScrolling) {
            // We're in the middle of a previously initiated scrolling. Check to see
            // whether that scrolling has actually started (if we always call getStartX
            // we can get a stale value from the scroller if it hadn't yet had its first
            // computeScrollOffset call) to decide what is the current scrolling position.

            //判断现在是否已经 开始滑动了  如果已经开始滑动则使用getCurrX()来获得当前滑动到的X坐标
            //否则的话就获得滑动开始时的x坐标
            sx = mIsScrollStarted ? mScroller.getCurrX() : mScroller.getStartX();
            // And abort the current scrolling.

            //如果当前正在滑动的话 就终止滑动动画(内部实现就是使用mCurrX=mFinalX,mCurrY=mFinalY,mFinished=true)
            mScroller.abortAnimation();
            setScrollingCacheEnabled(false);
        } else {
            //else进入的条件是滑动已经结束了
            //getScrollX() http://www.bubuko.com/infodetail-916594.html
            sx = getScrollX();
        }

        //getScrollY() http://www.bubuko.com/infodetail-916594.html
        int sy = getScrollY();
        //根据上面计算出来的sx,sy（即当前的x、y坐标） 然后与传入的目标x、y坐标 计算delta
        int dx = x - sx;
        int dy = y - sy;
        //如果计算出来的delta 都等于0 时就表示滑动结束了 更新状态为idle
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
        //先判断是否存在子类 如果不存在就直接return掉
        if (getChildCount() == 0) {
            // Nothing to do.
            setScrollingCacheEnabled(false);
            return;
        }
        int sy;
        //是否正在滚动 true表示正在滑动    false表示已经滑动已经结束了
        boolean wasScrolling = (mScroller != null) && !mScroller.isFinished();
        //如果正在滑动的话
        if (wasScrolling) {
            // We're in the middle of a previously initiated scrolling. Check to see
            // whether that scrolling has actually started (if we always call getStartX
            // we can get a stale value from the scroller if it hadn't yet had its first
            // computeScrollOffset call) to decide what is the current scrolling position.

            //判断现在是否已经 开始滑动了  如果已经开始滑动则使用getCurrX()来获得当前滑动到的X坐标
            //否则的话就获得滑动开始时的x坐标
            sy = mIsScrollStarted ? mScroller.getCurrY() : mScroller.getStartY();

            // And abort the current scrolling.

            //如果当前正在滑动的话 就终止滑动动画(内部实现就是使用mCurrX=mFinalX,mCurrY=mFinalY,mFinished=true)
            mScroller.abortAnimation();
            setScrollingCacheEnabled(false);
        } else {
            //else进入的条件是滑动已经结束了
            //getScrollX() http://www.bubuko.com/infodetail-916594.html
            sy = getScrollY();
        }

        int sx = getScrollX();
        //根据上面计算出来的sx,sy（即当前的x、y坐标） 然后与传入的目标x、y坐标
        int dx = x - sx;
        int dy = y - sy;
        //如果计算出来的delta 都等于0 时就表示滑动结束了 更新状态为idle
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

    /**
     * 根据传入的position来构造一个ItemInfo对象的实例  然后加入到mItems中去
     *
     * @param position
     * @param index
     * @return
     */
    ItemInfo addNewItem(int position, int index) {
        ItemInfo ii = new ItemInfo();
        ii.position = position;
        ii.object = mAdapter.instantiateItem(this, position);
        ii.widthFactor = mAdapter.getPageWidth(position);
        if (index < 0 || index >= mItems.size()) {
            mItems.add(ii);
        } else {
            mItems.add(index, ii);
        }
        return ii;
    }

    void dataSetChanged() {
        // This method only gets called if our observer is attached, so mAdapter is non-null.

        final int adapterCount = mAdapter.getCount();
        mExpectedAdapterCount = adapterCount;
        boolean needPopulate = mItems.size() < mOffscreenPageLimit * 2 + 1
                && mItems.size() < adapterCount;
        int newCurrItem = mCurItem;

        boolean isUpdating = false;
        for (int i = 0; i < mItems.size(); i++) {
            final YViewPagerOrigin.ItemInfo ii = mItems.get(i);
            final int newPos = mAdapter.getItemPosition(ii.object);

            if (newPos == PagerAdapter.POSITION_UNCHANGED) {
                continue;
            }

            if (newPos == PagerAdapter.POSITION_NONE) {
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
                final YViewPagerOrigin.LayoutParams lp = (YViewPagerOrigin.LayoutParams) child.getLayoutParams();
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
        // TODO: 2017/2/14
        ItemInfo oldCurInfo = null;
        //mCurItem指的是当前ViewPager是在那一页 第一页时则mCurItem=0....
        //newCurrentItem指的是当你滑动到另外一页时新的页面的index,比如你从第一页滑动到第二页时，newCurrentItem=1;
        if (mCurItem != newCurrentItem) {
            oldCurInfo = infoForPosition(mCurItem);
            mCurItem = newCurrentItem;
        }

        if (mAdapter == null) {
            sortChildDrawingOrder();
            return;
        }

        //如果mIsBeingDragged=true的话 mPopulatePending=true
        //而mIsBeingDragged是在onInterceptTouchEvent()中的ACTION_MOVE中当水平移动
        //的距离满足某种约束时赋值为true的。也就是当判定为水平移动时就拦截该事件让ViewPager来处理接下来的MOVE和UP事件

        //第二种情形仍然是在onInterceptTouchEvent()中的，但是与上面不同的是此次是在ACTION_DOWN事件中复制的
        //在ACTION_MOVE中处理的目的是但用户第一次滑动释放后 在ViewPager剩下的还没有页面还没有完成剩下的滑动之前用户
        //再次按下时 此时终止ViewPager的剩下滑动 而重新将用户的DOWN->MOVE->UP事件捕捉到 此时仍然是视为正在滑动（是滑动的开始）
        //所以仍然把mIsBeingDragged赋值为true

        //第三种是在onTouchEvent()中的MOVE事件中，此时此刻MOVE回调代表用户正在用手指拖动 所以mIsBeingDragged肯定又要赋值为true咯

        //所以在滑动的过程中 populate()方法是执行到这里就返回了  不往下继续执行了
        //这样做的目的是避免滑动过程中出现一些差错（下面的官方注释也说明了这一点）
        //然后在滑动完毕后 此时mCurItem 就是新的当前页面的index。（这里假如我们从0划到1时，即从第一页滑动到第二页），由于第一页创建时就
        // 已经往后缓存了一页（假设mOffScreenPageLimit=1），往前缓存了0页（因为当前页已经是第一页，再往前就没有了）所以往后滑动时是可以
        // 直接滑动调用缓存的。然后当你滑动到第一页后（即滑动切换完成后），此时mCurItem=1,此时再滑动完成会后再次调用populate(1);
        //然后开始执行缓存，即向左向右分别开始以mCurItem为基准缓存mOffScreenPageLimit页。所以执行完毕后mItems中就缓存了第0、1、2页了

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
        //从当前页的position开始往前计算startPos，并且确保startPos是大于0的。
        //比如刚开始时mCurItem=0,而pageLimit为默认的1，所以此时应保证startPos是0而不是-1
        final int startPos = Math.max(0, mCurItem - pageLimit);

        //这里你在实现PagerAdapter#getCount()方法时返回的是几这里就会获得几。
        final int N = mAdapter.getCount();
        //从当前页的position开始往后计算endPos，并且确保endPos是小于mAdapter.getCount()-1的
        //比如刚开始时mCurItem=0,而pageLimit为默认的1，所以此时endPos是1
        final int endPos = Math.min(N - 1, mCurItem + pageLimit);
        //计算完startPos和endPos的话，此次mItems中就会放入（缓存）ItemInfo.position在startPos-endPos之间的页面
        //防止用户在更新了数据源后没有调用notifyDataSetChanged()方法
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
        //在mItems中寻找当前页中已经有了的那个ItemInfo对象 从0开始  如果找到的话那么cueIndex就是指向那个的
        //找到的对象在mItems中的索引位置,curItem就是指向的那个找到的对象
        //如果第一次初始化ViewPager时，mItems是空的，所以此时该for循环不执行，只是将curIndex变成0而已，并且curItem也是null
        for (curIndex = 0; curIndex < mItems.size(); curIndex++) {
            final ItemInfo ii = mItems.get(curIndex);
            if (ii.position >= mCurItem) {
                if (ii.position == mCurItem) {
                    curItem = ii;
                }
                break;
            }
        }

        //当上面的for循环中没有找,即没有找到一个ItemInfo的对象，使得该对象的iteminfo.position=mCurItem.此时curItem=null
        //说明该页还没有创建，所以使用addNewItem()将其创建出来，并且加入到mItems中去
        if (curItem == null && N > 0) {
            //addNewItem()方法内部根据传入的两个参数来构造一个ItemInfo对象并且加入到mItems中去
            curItem = addNewItem(mCurItem, curIndex);
        }

        // Fill 3x the available width or up to the number of offscreen
        // pages requested to either side, whichever is larger.
        // If we have no current item we have no work to do.

        //执行到这里 curItem一般都不会是null
        if (curItem != null) {
            float extraWidthLeft = 0.f;
            //因为上面for循环执行完毕后curIndex=mItems.size()，要减去1不然下标超过最大会越界 （这种情况的条件是mItem不为空）
            //另一种情况：如果mItems.size()=0,那么此时curIndex=0,那么下面的itemIndex=0-1=-1;
            //因为这里先算的是往左边开始算。所以这里先往左挪一位
            int itemIndex = curIndex - 1;
            //如果当前页的左边还存在页面，那么就用ii指向它(当用户当前停留的页面不是第1页时，即curIndex不是0时,itemIndex>=0)，
            //如果左边不存在了就使用null来标记，当用户当前停留的页面是第1页时，即curIndex是0时，此时itemIndex=-1
            ItemInfo ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
            final int clientWidth = getClientWidth();

            // TODO: 2017/2/16 getPaddingLeft
            //经测试将 后面的 + (float) getPaddingLeft() / (float) clientWidth 去掉也能正常处理paddingLeft属性
            //这里使用2.f是因为curItem.widthFactor一般是1.0 然后减去后剩下1.0 刚好是下一页的widthOffset
            final float leftWidthNeeded = clientWidth <= 0 ? 0 :
                    2.f - curItem.widthFactor + (float) getPaddingLeft() / (float) clientWidth;

            //往左计算 具体的类似下面的往右计算  注释可以参照下面的for循环
            for (int pos = mCurItem - 1; pos >= 0; pos--) {
                if (extraWidthLeft >= leftWidthNeeded && pos < startPos) {
                    if (ii == null) {
                        break;
                    }

                    if (pos == ii.position && !ii.scrolling) {
                        mItems.remove(itemIndex);
                        mAdapter.destroyItem(this, pos, ii.object);
                        //唯一不同的是这里要itemIndex--，curIndex-- 删除掉一个以后后面的会往前顶上来，所以往后删除的话就不用处理itemIndex
                        //而往前删除的话是要修改索引的
                        itemIndex--;
                        //因为当前页的前面少了一个  所以当前页的索引也要往前挪一位
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
                    //当你往当前页的前面插入了一位后  当前页的索引就要往后挪一位
                    curIndex++;
                    ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
                }
            }

            //往下是开始往右计算了
            float extraWidthRight = curItem.widthFactor;
            //开始往右边计算 curIndex指向的是当前ViewPager的index，所以下面itemIndex就是当前的下一页
            itemIndex = curIndex + 1;
            //执行到这里extraWidthRight一般都小于等于1.0，因为widthFactor是受PagerAdapter#getPageWidth()的返回值影响的。
            //而PagerAdapter中一般要求getPageWidth()的返回是要在(0.f-1.f]之间的
            if (extraWidthRight < 2.f) {
                //如果mItems里面存在缓存的话就拿出来 没有的话ii=null 就addNew就行了
                ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
                final float rightWidthNeeded = clientWidth <= 0 ? 0 :
                        (float) getPaddingRight() / (float) clientWidth + 2.f;

                //从当前页开始往后遍历
                for (int pos = mCurItem + 1; pos < N; pos++) {
                    //注意这里要两个条件都满足 很多时候是第一个条件是满足了的 但是第二个不满足
                    //而影响第二个条件的因素就是setOffscreenPageLimit()方法
                    //两个条件都能满足的情况就表示已经向右计算完毕了 但是这种情况又分为两种 下面举例说明
                    //比如当前页是第3页，一共4页。offScreenPageLimit=1;所以此时endPos=4;
                    //此时第一次往右计算第四页时(pos=3+1=4)，下面的第一个条件不满足（1.0<2.0）然后假如在mItems中没有找到缓存的ItemInfo对象
                    //那么就进入下面的最后一个else中去执行addNewItem() 然后将itemIndex自增1(自增后itemIndex=5) 将extraWidthRight加上新new出
                    //来的ItemInfo.widthFactor，然后将ii=mItems.get(itemIndex)=null ,然后执行完后pos再自增1
                    //此时extraWidthRight=2.0 pos=5 下面两个条件都满足  进入后ii==null 表示已经到了最右边外面了或者该页还没有缓存到mItems中去 所以是null
                    //此时就直接break掉for循环

                    //上面第一种情况是在endPos之外没有缓存（pos不越界的情况下） 或者pos越界（超过mItems.size-1）
                    //那么第二种情况就是pos没有越界并且在endPos之外还在mItems中找到了缓存的对象 此时就要把找到的remove掉
                    //目的是使用保持mItems中缓存的是当前页的±mOffScreenPageLimit个ItemInfo对象
                    if (extraWidthRight >= rightWidthNeeded && pos > endPos) {
                        //上面分析的第一种情况
                        if (ii == null) {
                            break;
                        }
                        //上面分析的第二种情况
                        if (pos == ii.position && !ii.scrolling) {
                            mItems.remove(itemIndex);
                            mAdapter.destroyItem(this, pos, ii.object);
                            //因为已经remove掉了索引为itemIndex的对象，所以该索引后面的对象会补上来  所以此时直接使用itemIndex而不用s提前itemIndex++
                            //这里的目的是往后销毁掉所有的满足上面第二种情况的对象
                            ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
                        }
                    } else if (ii != null && pos == ii.position) {
                        //在mItems中拿到了以前缓存了的ItemInfo对象
                        //此时的做法就和下面的else中代码差不多  不同的是下面是要事先add一个新的 具体注释看下面的else中代码
                        extraWidthRight += ii.widthFactor;
                        itemIndex++;
                        ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
                    } else { //没有在mItems中拿到以前缓存存储的ItemInfo对象 那么就addNewItem()来add一个
                        //这里itemIndex的值仍然是上面curIndex + 1的结果，即当前页的下一页的index
                        ii = addNewItem(pos, itemIndex);
                        //然后自增itemIndex,指向当前页的下一页的下一页的index
                        itemIndex++;
                        //将当前页的下一页 的width 加入到 extraWidthRight中去 处理到这里就表示当前页的下一页处理完毕了
                        extraWidthRight += ii.widthFactor;
                        //处理完当前页的下一页，接着处理当前页的下一页的下一页
                        //下面的语句和上面for循环外面的套路一样 即：如果mItems里面存在缓存的话就拿出来 没有的话ii=null 就addNew就行了
                        ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
                    }
                }
            }
            //执行到这里就已经将mItems中处理好了，处理后的结果是保证在当前页的±mOffScreenPageLimit范围内都缓存(存储)了
            //ItemInfo对象，并且只存储了这些，其他的不在该范围的都被remove()掉和destroyItem()掉

            //假如你当前在第一页(position=0)，然后你调用了populate(1)切换到下一页
            //那么curItem就是position=1的，oldCurInfo就是position=0的那个
            //计算ItemInfo的offset属性
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
        //通知PagerAdapter 当前显示给用户的是position=mCurItem的页面
        mAdapter.setPrimaryItem(this, mCurItem, curItem != null ? curItem.object : null);

        //Called when the a change in the shown pages has been completed.  At this
        //point you must ensure that all of the pages have actually been added or
        //removed from the container as appropriate.
        //通知PagerAdapter 这里已经更新完毕了
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
                //正常情况下widthFactor都不应该是0
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
        //mCurItem指的是当前ViewPager是在那一页 第一页时则mCurItem=0....
        //newCurrentItem指的是当你滑动到另外一页时新的页面的index,比如你从第一页滑动到第二页时，newCurrentItem=1;
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
        //从当前页的position开始往前计算startPos，并且确保startPos是大于0的。
        //比如刚开始时mCurItem=0,而pageLimit为默认的1，所以此时应保证startPos是0而不是-1
        final int startPos = Math.max(0, mCurItem - pageLimit);

        //这里你在实现PagerAdapter#getCount()方法时返回的是几这里就会获得几。
        final int N = mAdapter.getCount();
        //从当前页的position开始往后计算endPos，并且确保endPos是小于mAdapter.getCount()-1的
        //比如刚开始时mCurItem=0,而pageLimit为默认的1，所以此时endPos是1
        final int endPos = Math.min(N - 1, mCurItem + pageLimit);
        //计算完startPos和endPos的话，此次mItems中就会放入（缓存）ItemInfo.position在startPos-endPos之间的页面
        //防止用户在更新了数据源后没有调用notifyDataSetChanged()方法
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
        //在mItems中寻找当前页中已经有了的那个ItemInfo对象 从0开始  如果找到的话那么cueIndex就是指向那个的
        //找到的对象在mItems中的索引位置,curItem就是指向的那个找到的对象
        //如果第一次初始化ViewPager时，mItems是空的，所以此时该for循环不执行，只是将curIndex变成0而已，并且curItem也是null
        for (curIndex = 0; curIndex < mItems.size(); curIndex++) {
            final ItemInfo ii = mItems.get(curIndex);
            if (ii.position >= mCurItem) {
                if (ii.position == mCurItem) {
                    curItem = ii;
                }
                break;
            }
        }

        //当上面的for循环中没有找,即没有找到一个ItemInfo的对象，使得该对象的iteminfo.position=mCurItem.此时curItem=null
        //说明该页还没有创建，所以使用addNewItem()将其创建出来，并且加入到mItems中去
        if (curItem == null && N > 0) {
            //addNewItem()方法内部根据传入的两个参数来构造一个ItemInfo对象并且加入到mItems中去
            curItem = addNewItem(mCurItem, curIndex);
        }

        // Fill 3x the available width or up to the number of offscreen
        // pages requested to either side, whichever is larger.
        // If we have no current item we have no work to do.

        //执行到这里 curItem一般都不会是null
        if (curItem != null) {
            float extraHeightTop = 0.f;
            int itemIndex = curIndex - 1;
            ItemInfo ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
            final int clientHeight = getClientHeight();

            final float topHeightNeeded = clientHeight <= 0 ? 0 :
                    2.f - curItem.widthFactor + (float) getPaddingTop() / (float) clientHeight;

            //往上计算 具体的类似下面的往右计算  注释可以参照下面的for循环
            for (int pos = mCurItem - 1; pos >= 0; pos--) {
                if (extraHeightTop >= topHeightNeeded && pos < startPos) {
                    if (ii == null) {
                        break;
                    }

                    if (pos == ii.position && !ii.scrolling) {
                        mItems.remove(itemIndex);
                        mAdapter.destroyItem(this, pos, ii.object);
                        //唯一不同的是这里要itemIndex--，curIndex-- 删除掉一个以后后面的会往前顶上来，所以往后删除的话就不用处理itemIndex
                        //而往前删除的话是要修改索引的
                        itemIndex--;
                        //因为当前页的前面少了一个  所以当前页的索引也要往前挪一位
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
                    //当你往当前页的前面插入了一位后  当前页的索引就要往后挪一位
                    curIndex++;
                    ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
                }
            }

            float extraHeightBottom = curItem.widthFactor;
            itemIndex = curIndex + 1;
            if (extraHeightBottom < 2.f) {
                //如果mItems里面存在缓存的话就拿出来 没有的话ii=null 就addNew就行了
                ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
                final float bottomHeightNeeded = clientHeight <= 0 ? 0 :
                        (float) getPaddingBottom() / (float) clientHeight + 2.f;

                //从当前页开始往后遍历
                for (int pos = mCurItem + 1; pos < N; pos++) {
                    if (extraHeightBottom >= bottomHeightNeeded && pos > endPos) {
                        //上面分析的第一种情况
                        if (ii == null) {
                            break;
                        }
                        //上面分析的第二种情况
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

    /**
     * 在populate()方法中的addNewItem()中，在创建新的ItemInfo时只是创建了position和object和widthFactor
     * 对于offset并没有赋值 所以此方法就是用来给offset赋值的
     * <p>
     * 对于有五页的ViewPager来说，五个ItemInfo分别对应的offset为：
     * 0: position=0,widthFactor=1.f, offset=0.f
     * 1: position=1,widthFactor=1.f, offset=1.f
     * 2: position=2,widthFactor=1.f, offset=2.f
     * 3: position=3,widthFactor=1.f, offset=3.f
     * 4: position=4,widthFactor=1.f, offset=4.f
     * <p>
     * 第一页的范围就是: [offset,widthFactor+offset+marginOffset]==>[0f,1f]
     * 第二页的范围就是: [offset,widthFactor+offset+marginOffset]==>[1f,2f]
     * 第三页的范围就是: [offset,widthFactor+offset+marginOffset]==>[2f,3f]
     * 第四页的范围就是: [offset,widthFactor+offset+marginOffset]==>[3f,4f]
     * 第五页的范围就是: [offset,widthFactor+offset+marginOffset]==>[4f,5f]
     * <p>
     * 其中marginOffset一般为0，除非你调用了{@link YViewPagerOrigin#setPageMargin(int)}方法 ，marginOffset=mPageMargin/clientWidth
     * 其中1f,2f...是该页的实际显示宽度除以clientWidth得到的，而clientWidth=measuredWidth-paddingLeft-paddingRight
     *
     * @param curItem    当前选中的curItem，即用户调用populate(index)中index对应的那个ItemInfo
     * @param curIndex   即用户调用populate(index)中的index
     * @param oldCurInfo 当populate(index)中的index!=mCurItem时，oldCurInfo=mItems.get(mCurItem)
     */
    private void calculatePageOffsetsHorizontal(ItemInfo curItem, int curIndex, ItemInfo oldCurInfo) {
        final int N = mAdapter.getCount();
        final int width = getClientWidth();
        final float marginOffset = width > 0 ? (float) mPageMargin / width : 0;

        // Fix up offsets for later layout.
        if (oldCurInfo != null) {
            final int oldCurPosition = oldCurInfo.position;
            // Base offsets off of oldCurInfo.
            //从往右切换 old.position<cur.position
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
        //先拿到当前ItemInfo的offset属性 一般来说当前ItemInfo的offset都不会是0（除了第一次初始化时position=0的offset本来就是0）
        //比如你现在curItem是第二个，即position=1，此时offset=1.0；因为在当position=0时由于mOffScreenPageLimit最小为1，所以
        //position=1 ItemInfo的在position=0时就已经被计算出来了，即offset=1.f
        float offset = curItem.offset;
        //pos要保持和下面的两个for循环中的i要一致所以在第一个for循环(往左计算offset)之前，先将
        //pos=curItem.position - 1 因为在下面的第一个for循环的起始条件中给i也是赋值为curItem.position - 1
        //在第一个for循环执行完毕有给pos赋值为curItem.position +1，同理在第二个for循环的初始条件中也给i赋相同的值curItem.position + 1
        int pos = curItem.position - 1;
        mFirstOffset = curItem.position == 0 ? curItem.offset : -Float.MAX_VALUE;
        mLastOffset = curItem.position == N - 1
                ? curItem.offset + curItem.widthFactor - 1 : Float.MAX_VALUE;

        // Previous pages
        //计算当前页的前几页,到底计算几页要根据当前的curItem来决定，另外还要看mItems的size();
        //而mItems的size()由mOffScreenPageLimit来决定；即：当mOffScreenPageLimit=1时，mItems的size<=2*mOffScreenPageLimit+1=3
        //所以此时往左最多计算1页(当当前页是非0之外的页时)，或者往左不计算了(当当前页是0时)
        // 当mOffScreenPageLimit=2时，mItems的size<=2*mOffScreenPageLimit+1=5
        //所以此时往左最多计算2页(当当前页是大于1之外的页时)，或者往左计算一页了(当当前页是1时)，或者往左不计算了（当当前页是0时）
        //....

        for (int i = curIndex - 1; i >= 0; i--, pos--) {
            final ItemInfo ii = mItems.get(i);
            //pos>li.position一般不会发生，一般情况下pos=ii.position
            // TODO: 2017/2/16
            while (pos > ii.position) {
                offset -= mAdapter.getPageWidth(pos--) + marginOffset;
            }
            //更新offset 当前的offset offset可以看做是当前页的开始位置，减去前一页的宽度总和就求
            // 出了前一页的offset，具体可以看本方法calculatePageOffsets()最前面的注释
            offset -= ii.widthFactor + marginOffset;
            //计算出来的offset设置给前一个ItemInfo,
            ii.offset = offset;

            //如果前一个ItemInfo就是最前面的position=0的那个ItemInfo，此时就把offset记录下来 存入全局变量mFirstOffset中去
            //同理还有mLastOffset,当然是对应position=N-1的那个ItemInfo咯
            if (ii.position == 0) {
                mFirstOffset = offset;
            }
        }
        //现在准备往后计算了 先做好准备工作，先把offset置为当前页的下一页的开始位置，即下一页的offset
        offset = curItem.offset + curItem.widthFactor + marginOffset;
        //然后把pos变成当前页position的下一位 这样是为了和上面offset的改变保持一致 并且和下面for循环的i的初始值保持一致
        pos = curItem.position + 1;
        // Next pages
        //开始往后计算 具体可以参考上面的for循环前面的注释  大致意思是一样的不过只是调换了方向而已
        for (int i = curIndex + 1; i < itemCount; i++, pos++) {
            final ItemInfo ii = mItems.get(i);
            while (pos < ii.position) {
                offset += mAdapter.getPageWidth(pos++) + marginOffset;
            }
            if (ii.position == N - 1) {
                mLastOffset = offset + ii.widthFactor - 1;
            }

            //在这里就直接赋值给下一页的offset，因为在for开始执行之前我们在外面就已经将下一页的开始offset计算好了
            //相当于do...while...语句  先do一个再while ...
            //上面的向左计算的相当于 while...语句，
            ii.offset = offset;
            //往后计算offset 这里光计算就行了 for循环的条件会去控制下一页是否要使用这里计算好的offset
            offset += ii.widthFactor + marginOffset;
        }

        mNeedCalculatePageOffsets = false;
    }

    private void calculatePageOffsetsVertical(ItemInfo curItem, int curIndex, ItemInfo oldCurInfo) {
        final int N = mAdapter.getCount();
        final int height = getClientHeight();
        final float marginOffset = height > 0 ? (float) mPageMargin / height : 0;

        // Fix up offsets for later layout.
        if (oldCurInfo != null) {
            final int oldCurPosition = oldCurInfo.position;
            // Base offsets off of oldCurInfo.
            //从往右切换 old.position<cur.position
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
            //pos>li.position一般不会发生，一般情况下pos=ii.position
            // TODO: 2017/2/16
            while (pos > ii.position) {
                offset -= mAdapter.getPageWidth(pos--) + marginOffset;
            }
            //更新offset 当前的offset offset可以看做是当前页的开始位置，减去前一页的宽度总和就求
            // 出了前一页的offset，具体可以看本方法calculatePageOffsets()最前面的注释
            offset -= ii.widthFactor + marginOffset;
            //计算出来的offset设置给前一个ItemInfo,
            ii.offset = offset;
            if (ii.position == 0) {
                mFirstOffset = offset;
            }
        }
        //现在准备往后计算了 先做好准备工作，先把offset置为当前页的下一页的开始位置，即下一页的offset
        offset = curItem.offset + curItem.widthFactor + marginOffset;
        //然后把pos变成当前页position的下一位 这样是为了和上面offset的改变保持一致 并且和下面for循环的i的初始值保持一致
        pos = curItem.position + 1;
        // Next pages
        //开始往后计算 具体可以参考上面的for循环前面的注释  大致意思是一样的不过只是调换了方向而已
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

        public static final Creator<YViewPagerOrigin.SavedState> CREATOR = ParcelableCompat.newCreator(
                new ParcelableCompatCreatorCallbacks<YViewPagerOrigin.SavedState>() {
                    @Override
                    public YViewPagerOrigin.SavedState createFromParcel(Parcel in, ClassLoader loader) {
                        return new YViewPagerOrigin.SavedState(in, loader);
                    }

                    @Override
                    public YViewPagerOrigin.SavedState[] newArray(int size) {
                        return new YViewPagerOrigin.SavedState[size];
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
        YViewPagerOrigin.SavedState ss = new YViewPagerOrigin.SavedState(superState);
        ss.position = mCurItem;
        if (mAdapter != null) {
            ss.adapterState = mAdapter.saveState();
        }
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof YViewPagerOrigin.SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        YViewPagerOrigin.SavedState ss = (YViewPagerOrigin.SavedState) state;
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
        final YViewPagerOrigin.LayoutParams lp = (YViewPagerOrigin.LayoutParams) params;
        // Any views added via inflation should be classed as part of the decor
        lp.isDecor |= isDecorView(child);
        if (mInLayout) {
            if (lp != null && lp.isDecor) {
                throw new IllegalStateException("Cannot add pager decor view during layout");
            }
            lp.needsMeasure = true;
            addViewInLayout(child, index, params);
        } else {
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

    private static boolean isDecorView(@NonNull View view) {
        Class<?> clazz = view.getClass();
        return clazz.getAnnotation(YViewPagerOrigin.DecorView.class) != null;
    }

    @Override
    public void removeView(View view) {
        if (mInLayout) {
            removeViewInLayout(view);
        } else {
            super.removeView(view);
        }
    }

    /**
     * 传入一个non-decor view  然后根据 PagerAdapter的isViewFromObject()方法（该方法一般被重写）来判断
     * 判断是通过遍历mItems对象(一个ItemInfo的List实例化对象)来判断是否该child是存在于mItems中
     * 当存在时返回对应的ItemInfo对象  反之返回null
     * <p>
     * 具体和 {@link YViewPagerOrigin#infoForPosition(int)} 方法功能类似
     *
     * @param child non-decor view
     * @return 不存在对应的ItemInfo返回null 反之返回对应的对象引用
     */
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

    /**
     * 该方法{@link YViewPagerOrigin#infoForChild(View)}方法功能差不多
     * {@link YViewPagerOrigin#infoForChild(View)}是传入一个non child View 来和ItemInfo.object来判断
     * 而本方法是使用position和ItemInfo.position来判断
     *
     * @param position
     * @return
     */
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

        //getDefaultSize() ：当传入的MeasureSpec mode为
        //                             1： UNSPECIFIED时：返回getDefaultSize()方法传入的第一个参数值
        //                             2： AT_MOST时：返回MeasureSpec的size
        //                             3： EXACTLY时：返回MeasureSpec的size
        //        所以当你给viewpager设置match_parent和wrap_content的效果都是一样的
        setMeasuredDimension(getDefaultSize(0, widthMeasureSpec),
                getDefaultSize(0, heightMeasureSpec));

        final int measuredWidth = getMeasuredWidth();

        // TODO: 2017/2/10    maxGutterSize意思
        final int maxGutterSize = measuredWidth / 10;
        //mDefaultGutterSize是 DEFAULT_GUTTER_SIZE * density 而 DEFAULT_GUTTER_SIZE是(16dp) 即16dp*屏幕密度
        mGutterSize = Math.min(maxGutterSize, mDefaultGutterSize);

        // Children are just made to fill our space.
        //正如上面注释所说的，viewpager中的子View的宽度和高度等于父布局viewpager的剩余空间的大小的
        //所以下面的childWidthSize、childHeightSize是将viewpager的测量后的大小减去其设置的左右padding
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
            //排除掉child状态为GONE的
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (YViewPagerOrigin.LayoutParams) child.getLayoutParams();
                //将decor和 normal 区分出来
                //todo decor这段暂时不看 因为decor我也没搞懂在viewpager中是啥东西
                if (lp != null && lp.isDecor) {
                    //使用 lp.gravity和 Gravity.HORIZONTAL_GRAVITY_MASK做与操作返回
                    //的结果就是gravity对应的水平方向上的属性：LEFT,CENTER_HORIZONTAL,RIGHT
                    //同理和Gravity.VERTICAL_GRAVITY_MASK做与操作返回的结果是gravity对应的水平方向上的属
                    // 性：TOP,CENTER_VERTICAL,BOTTOM
                    final int hgrav = lp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                    final int vgrav = lp.gravity & Gravity.VERTICAL_GRAVITY_MASK;
                    //先把width 和 height 设置为wrap_content 即依赖父布局的剩余空间来确定最终的宽高
                    int widthMode = MeasureSpec.AT_MOST;
                    int heightMode = MeasureSpec.AT_MOST;
                    //用两个boolean来指示当前子view是偏上/下的还是偏左/右
                    //当偏上/下时，则将宽度设置为match_parent即下面的MeasureSpec.EXACTLY
                    //当偏左/右时同理
                    boolean consumeVertical = vgrav == Gravity.TOP || vgrav == Gravity.BOTTOM;
                    boolean consumeHorizontal = hgrav == Gravity.LEFT || hgrav == Gravity.RIGHT;
                    //见上面注释
                    if (consumeVertical) {
                        widthMode = MeasureSpec.EXACTLY;
                    } else if (consumeHorizontal) {
                        heightMode = MeasureSpec.EXACTLY;
                    }
                    //todo 这里的widthSize，heightSize，widthMode，heightMode在上面都是赋值了的
                    //而在这里为啥子还要判读再赋值呢？我猜上面是通过xml创建的时候会调用 下面的呃是通过代码new出来viwpager
                    //时会调用 当然只是猜测 因为我现在还不知道怎么给viewpager加decor view
                    //这里的childWidthSize、childHeightSize是上面viewpager measure后的宽高减去对应方向的上padding得到的
                    int widthSize = childWidthSize;
                    int heightSize = childHeightSize;

                    //然后开始处理LayoutParams  根据设置的LayoutParams 来设置对应的宽高mode
                    //即当其width不是WRAP_CONTENT时，即是EXACTLY时
                    //Can be one of the constants FILL_PARENT (replaced by MATCH_PARENT
                    //in API Level 8) or WRAP_CONTENT, or an exact size.
                    if (lp.width != LayoutParams.WRAP_CONTENT) {
                        widthMode = MeasureSpec.EXACTLY;
                        //能进入上面的if执行到本语句表示width是EXACTLY模式的，而EXACTLY模式又包括两种：MATCH_PARENT和exact size
                        if (lp.width != LayoutParams.MATCH_PARENT) {
                            //当width是exact size时，即是确切的数值大小时
                            widthSize = lp.width;
                        }
                    }
                    //和上面处理width的流程是一样的
                    if (lp.height != YViewPagerOrigin.LayoutParams.WRAP_CONTENT) {
                        heightMode = MeasureSpec.EXACTLY;
                        if (lp.height != YViewPagerOrigin.LayoutParams.MATCH_PARENT) {
                            heightSize = lp.height;
                        }
                    }
                    //根据上面处理后的size 和 mode  来制作 MeasureSpec
                    final int widthSpec = MeasureSpec.makeMeasureSpec(widthSize, widthMode);
                    final int heightSpec = MeasureSpec.makeMeasureSpec(heightSize, heightMode);
                    //然后用制作好的MeasureSpec来测量该child
                    child.measure(widthSpec, heightSpec);

                    //这里的childWidthSize、childHeightSize是上面viewpager measure后的宽高减去对应方向的上padding得到的
                    //即减去decor在对应方向上的空间占用    剩下的空间就可以用来放置normal类型的view了
                    if (consumeVertical) {
                        childHeightSize -= child.getMeasuredHeight();
                    } else if (consumeHorizontal) {
                        childWidthSize -= child.getMeasuredWidth();
                    }
                }
            }
        }

        //这个是给normal view 计算 MeasureSpec的 因为上面已经报decor占用的空间大小除去了  所以此时 childWidthSize和
        //childHeightSize就是剩给 normal view 的空间大小，此时viewpager的处理方法是把剩下的空间全部给子view 让子view布
        // 满剩下的空间
        mChildWidthMeasureSpec = MeasureSpec.makeMeasureSpec(childWidthSize, MeasureSpec.EXACTLY);
        mChildHeightMeasureSpec = MeasureSpec.makeMeasureSpec(childHeightSize, MeasureSpec.EXACTLY);

        // TODO: 2017/2/10  等着补populate()的坑
        // Make sure we have created all fragments that we need to have shown.
        mInLayout = true;
        if (isVertical) {
            populateVertical();
        } else {
            populateHorizontal();
        }
        mInLayout = false;

        //开始遍历所有不是decor的view 来将其全部测量
        // Page views next.
        size = getChildCount();
        for (int i = 0; i < size; ++i) {
            final View child = getChildAt(i);
            //老规矩 先排除掉状态是GONE的
            if (child.getVisibility() != GONE) {
                //和上面测量decor view的流程一样 先拿到该child的 LayoutParams
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                //当它不是decor时
                if (!isVertical && (lp == null || !lp.isDecor)) {
                    //至于lp.widthFactor是通过重写PagerAdapter的getPageWidth(position)来实现 默认是实现是返回1.0f
                    //其实上面mChildWidthMeasureSpec已经计算出来了  这里再次计算一次是为了加上lp.widthFactor 使用
                    //lp.widthFactor来控制每一页显示的比例 即当但会0.9f时 该页显示的就是全部宽度的0.9倍宽
                    final int widthSpec = MeasureSpec.makeMeasureSpec(
                            (int) (childWidthSize * lp.widthFactor), MeasureSpec.EXACTLY);
                    //而mChildHeightMeasureSpec不用 因为我们viewpager是左右滑动的 每一页高度都是一样的 都是和viewpager的
                    //高度一样(除去padding和decor)
                    child.measure(widthSpec, mChildHeightMeasureSpec);
                } else if (isVertical && (lp == null || !lp.isDecor)) {
                    final int heightSpec = MeasureSpec.makeMeasureSpec(
                            (int) (childHeightSize * lp.widthFactor), MeasureSpec.EXACTLY);
                    //而mChildHeightMeasureSpec不用 因为我们viewpager是左右滑动的 每一页高度都是一样的 都是和viewpager的
                    //高度一样(除去padding和decor)
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
            final YViewPagerOrigin.ItemInfo ii = infoForPosition(mCurItem);
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
        //可用的总宽度
        int width = r - l;
        //可用的总高度
        int height = b - t;
        //上下左右的padding
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        // TODO: 2017/2/10
        //getScrollX() http://www.bubuko.com/infodetail-916594.html
        final int scrollX = getScrollX();
        final int scrollY = getScrollY();

        int decorCount = 0;

        // First pass - decor views. We need to do this in two passes so that
        // we have the proper offsets for non-decor views later.
        //先处理decor view  以保证让 non-decor view 有合适的位置来放
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            //同理先排除状态为GONE的
            if (child.getVisibility() != GONE) {
                final YViewPagerOrigin.LayoutParams lp = (YViewPagerOrigin.LayoutParams) child.getLayoutParams();
                int childLeft = 0;
                int childTop = 0;
                if (lp.isDecor) {
                    //拿到decor的gravity 使用 lp.gravity和 Gravity.HORIZONTAL_GRAVITY_MASK做与操作返回
                    // 的结果就是gravity对应的水平方向上的属性：LEFT,CENTER_HORIZONTAL,RIGHT
                    //同理和Gravity.VERTICAL_GRAVITY_MASK做与操作返回的结果是gravity对应的水平方向上的属
                    // 性：TOP,CENTER_VERTICAL,BOTTOM
                    final int hgrav = lp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                    final int vgrav = lp.gravity & Gravity.VERTICAL_GRAVITY_MASK;

                    switch (hgrav) {
                        default:  //当没有设置layout_gravity属性时  child 放置时的左边值以paddingLeft为准
                            childLeft = paddingLeft;
                            break;
                        case Gravity.LEFT:
                            //设置layout_gravity为left时 child 放置时的左边值以paddingLeft为准 并且将paddingLeft增加
                            //paddingLeft增加是因为此时是decor的放置，decor view放置占用了 child.getMeasuredWidth()的长的
                            //宽度，所以下面放置non-decor view 时就要除去  child.getMeasuredWidth() 的宽度 用剩下的地方来放置non-decor view
                            childLeft = paddingLeft;
                            paddingLeft += child.getMeasuredWidth();
                            break;
                        case Gravity.CENTER_HORIZONTAL: //设置layout_gravity为center_horizontal时 求中间值为child的最左坐标
                            childLeft = Math.max((width - child.getMeasuredWidth()) / 2,
                                    paddingLeft);
                            break;
                        case Gravity.RIGHT:
                            //设置layout_gravity为right时 child的最左坐标为可用宽度减去paddingRight  然后减去 child的measuredWidth
                            //然后要更新paddngRight值，以让non-decor 能够放置到合适的位置
                            childLeft = width - paddingRight - child.getMeasuredWidth();
                            paddingRight += child.getMeasuredWidth();
                            break;
                    }
                    //和上面switch同理
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
                    //上面的两个switch语句就是为了计算出childLeft、childTop值，拿到一个view 的左上角坐标（childLeft，childTop）
                    //就可以开始调用该view 的 layout（）方法了，至于右下角坐标可以结合measuredWidth、measuredHeight来求出

                    //至于scrollX是viewpager 内部的子view水平scroll的值
                    //http://www.bubuko.com/infodetail-916594.html
                    if (isVertical) {
                        childTop += scrollY;
                    } else {
                        childLeft += scrollX;
                    }
                    //使用计算好的childLeft、childTop来调用layout()
                    child.layout(childLeft, childTop,
                            childLeft + child.getMeasuredWidth(),
                            childTop + child.getMeasuredHeight());

                    //将decor view 计数器增加
                    decorCount++;
                }
            }
        }

        //开始放置non-decor view，注意可用的宽度就要变化了，上面放置decor时可用的宽度是width，由于放置decor view时
        //由于decor view 的layout_gravity 影响 ，有些decor会影响paddingLeft 和paddingRight(具体看上面)
        //所以此时要重新计算可用的宽度
        final int childWidth = width - paddingLeft - paddingRight;
        final int childHeight = height - paddingTop - paddingBottom;
        // Page views. Do this once we have the right padding offsets from above.
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            //排除掉view状态为GONE的
            if (child.getVisibility() != GONE) {
                final YViewPagerOrigin.LayoutParams lp = (YViewPagerOrigin.LayoutParams) child.getLayoutParams();
                //ItemInfo 是 本类内部封装的一个静态类 内部存在五个属性
                // TODO: 2017/2/10  ItemInfo
                ItemInfo ii;
                //关于infoForChild()方法的用途请看该方法的注释 简而言之该方法就是当该child不是存在对应的ItemInfo对象
                // 当存在时返回true 不存在返回false
                //下面的if即当当前child不是decor view 并且存在对应的ItemInfo对象时
                if (!lp.isDecor && (ii = infoForChild(child)) != null) {
                    //左边的offset值 即non-decor view child 的左边偏移的量
                    // TODO: 2017/2/10    ii.offset
                    int loff = (int) (childWidth * ii.offset);
                    int toff = (int) (childHeight * ii.offset);
                    //child的左上坐标的横坐标
                    int childLeft = !isVertical ? paddingLeft + loff : paddingLeft;
                    //child的左上坐标的纵坐标
                    int childTop = !isVertical ? paddingTop : paddingTop + toff;
                    //当该子view 是在layout期间新添加的时 lp.needsMeasure 为true 反之为false
                    if (lp.needsMeasure) {
                        // This was added during layout and needs measurement.
                        // Do it now that we know what we're working with.

                        //更新 lp.needsMeasure为false
                        lp.needsMeasure = false;

                        //childWidth值是上面用width-paddingLeft-paddingRight计算出来的 即除去decor view 的占
                        // 用之后剩给non-decor view 大小
                        //最后的宽度是还要乘上lp.widthFactor （记得在上面onMeasure()中也乘了lp.widthFactor的）
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
                        //计算出宽和高的MeasureSpec后就可以开始测量了
                        child.measure(widthSpec, heightSpec);
                    }
                    if (DEBUG) {
                        Log.v(TAG, "Positioning #" + i + " " + child + " f=" + ii.object
                                + ":" + childLeft + "," + childTop + " " + child.getMeasuredWidth()
                                + "x" + child.getMeasuredHeight());
                    }
                    //到这里就应该明白了为什么要在上面先测量在layout  因为在layout中我们要用
                    // 到getMeasuredXXX() 如果不测量的话这里getMeasuredXXX()拿到的就是0
                    child.layout(childLeft, childTop,
                            childLeft + child.getMeasuredWidth(),
                            childTop + child.getMeasuredHeight());
                }
            }
        }
        //开始更新变量 将 non-decor view 的顶部纵坐标和底部纵坐标存起来
        if (!isVertical) {
            mTopPageBounds = paddingTop;
            mBottomPageBounds = height - paddingBottom;
        } else {
            mLeftPageBounds = paddingLeft;
            mRightPageBounds = width - paddingRight;
        }
        //然后还要把decorCount的数量存起来
        mDecorChildCount = decorCount;


        //如果第一次初始化viewPager时mFirstLayout为true
        if (isVertical && mFirstLayout) {
            scrollToItemVertical(mCurItem, false, 0, false);
        } else if (!isVertical && mFirstLayout) {
            scrollToItemHorizontal(mCurItem, false, 0, false);
        }
        //将mFirstLayout改为false
        mFirstLayout = false;
    }

    /**
     * completeScroll()方法和computeScroll()方法的区别：
     * 后者是用户在用手指拖动viewpager页面滑动时会不断调用computeScroll()方法，然后在里面来使用scrollTo方
     * 法来一点点移动non-decor view的位置。
     * <p>
     * 而前者是当用户释放后，viewpager会自动滑动到指定的位置（可能是把这页滑动完，或
     * 者是返回去滑动到初始位置），完成后面的滑动事件。所以在completeScrolled()方法内部一开
     * 始就回调SCROLL_STATE_SETTLING方法 然后使用一个runnable在滑动最终结束后回调SCROLL_STATE_IDLE表示此次滑动过程结束
     */

    @Override
    public void computeScroll() {
        //表示当前已经开始滑动了
        mIsScrollStarted = true;
        //如果还没有结束滑动
        if (!mScroller.isFinished() && mScroller.computeScrollOffset()) {
            //当前的position
            int oldX = getScrollX();
            int oldY = getScrollY();
            //上面mScroller.computeScrollOffset()计算出来下一时刻的position
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();
            //如果当前的position和下一时刻的position是不一样的才移动 non-decor view
            if (oldY != y || oldX != x) {
                //移动到x,y位置  scrollTo执行之后getScrollX()、getScrollY()就分别等于x,y 具体可以看View#scrollTo()方法内部实现
                //该scrollTo()移动的是当用户手机拖动时的viewpager的移动 而completeScroll()是直接移动到mFinalPosition
                scrollTo(x, y);
                //如果pageScrolled()返回false条件是：当mItems为空，这种情况下又包括mFirstLayout是否为true的情况
                //具体可以看pageScrolled()方法内部注释

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
            //通知重绘
            ViewCompat.postInvalidateOnAnimation(this);
            return;
        }

        // Done with scroll, clean up state.
        //结束滑动了
        if (isVertical) {
            completeScrollVertical(true);
        } else {
            completeScrollHorizontal(true);
        }
    }

    /**
     * 根据传入的xPos来计算并且回调onPageScroll()方法
     * 内部执行大概逻辑是：当mItems为空时，表示内部没有任何缓存页面，相当于来说是异常情况，
     * 其中当mFirstLayout时true时，即第一次layout方法还没有执行，此时就直接返回false就行了，因为
     * 此时内部View还没有layout好。然后如果mFirstLayout为false时，就直接回调onPageScrolled(0,0,0)
     * 回调onPageScroll()通知此时滑动到第0页，并且返回false
     * <p>
     * 除上面之外就表示是正常滑动的，就计算pageOffset,pageOffsetPixel然后回调onPageScroll()方法
     * <p>
     * 本方法中可以学到：当你要确定用户是否在重写对应方法时是否调用了super.XXX()方法时就可以在父类中使用一个变量来标示，
     * 然后在父类的方法中修改。
     * <p>
     * 注意本方法只是计算相应的参数，传入onPageScroll()方法中去，然后在onPageScroll()方法中实现了了decor view的滑动
     *
     * @param xpos
     * @return
     */
    private boolean pageScrolledHorizontal(int xpos) {
        if (mItems.size() == 0) {
            //表示是第一次 直接跳过此次调用 返回false 因为还没有layout
            if (mFirstLayout) {
                // If we haven't been laid out yet, we probably just haven't been populated yet.
                // Let's skip this call since it doesn't make sense in this state
                return false;
            }
            //使用mCalledSuper来标记是否之类在继承ViewPager后重写onPageScrolled()时调
            //用了super.onPageScrolled(position, offset, offsetPixels) 这里把mCalledSuper设
            // 置为false 然后在ViewPager 中的 onPageScrolled()中(即ViewPager的之类中的super.onPageScrolled()中)
            // 将mCalledSuper设置为true 用来标记重写时是否调用了super.onPageScrolled()
            mCalledSuper = false;
            //将页面滑动到第一页 初始位置 具体看onPageScrolled()方法
            //能执行本句话的前提是：mItems.size==0 并且 mFirstLayout==false
            // TODO: 2017/2/11
            onPageScrolledHorizontal(0, 0, 0);
            //当用户重写ViewPager的onPageScrolled()时没有调用super.onPageScrolled() 那么mCalledSuper的值仍然会是false
            //如果调用了的话那么mCalledSuper就会使true
            //所以当mCalledSuper=false时就直接抛出异常
            if (!mCalledSuper) {
                throw new IllegalStateException(
                        "onPageScrolled did not call superclass implementation");
            }
            return false;
        }
        // TODO: 2017/2/11
        //拿到当前getScrollX()对应的ItemInfo 具体看infoForCurrentScrollPosition()方法内部注释
        final ItemInfo ii = infoForCurrentScrollPositionHorizontal();

        final int width = getClientWidth();
        final int widthWithMargin = width + mPageMargin;
        final float marginOffset = (float) mPageMargin / width;

        final int currentPage = ii.position;
        //对于ItemInfo的offset,当position=1，offset=1.0,当position=2,offset=2.0
        //当position=4,offset=4.0
        //由infoForCurrentScrollPosition()方法内部代码可知，一个页面是使用offset来标记左右两端的
        //当为第0页时，leftBound=ii.offset=0.0,rightBound=leftBound+ii.widthFactor+marginOffset=2.0
        //其中一页的宽度是：ii.widthFactor+marginOffset，即一页的结束的position是开始的position加上这页的宽度
        //其中marginOffset=(float) mPageMargin / width;

        //其中下面的((float) xpos / width)是当前位置的offset,然后减去ii.offset(减去左边初始offset)
        //然后整体除以一页的宽度的offset，算出当前位置的offset相对一页的整个offset的比例
        //所以pageOffset的取值是0-1，包括0不包括1
        final float pageOffset = (((float) xpos / width) - ii.offset)
                / (ii.widthFactor + marginOffset);
        //然后用此比例乘以该页的全部宽度像素，结果就是水平上位移的像素pixel
        final int offsetPixels = (int) (pageOffset * widthWithMargin);
        //然后准备开始回调onPageScrolled（）方法，先令mCalledSuper=false 然后当你在重写onPageScrolled()时没有调用super.onPageScrolled（）
        //时，就会抛出IllegalStateException异常
        mCalledSuper = false;
        onPageScrolledHorizontal(currentPage, pageOffset, offsetPixels);
        if (!mCalledSuper) {
            throw new IllegalStateException(
                    "onPageScrolled did not call superclass implementation");
        }
        return true;
    }


    private boolean pageScrolledVertical(int ypos) {
        if (mItems.size() == 0) {
            //表示是第一次 直接跳过此次调用 返回false 因为还没有layout
            if (mFirstLayout) {
                // If we haven't been laid out yet, we probably just haven't been populated yet.
                // Let's skip this call since it doesn't make sense in this state
                return false;
            }
            //使用mCalledSuper来标记是否之类在继承ViewPager后重写onPageScrolled()时调
            //用了super.onPageScrolled(position, offset, offsetPixels) 这里把mCalledSuper设
            // 置为false 然后在ViewPager 中的 onPageScrolled()中(即ViewPager的之类中的super.onPageScrolled()中)
            // 将mCalledSuper设置为true 用来标记重写时是否调用了super.onPageScrolled()
            mCalledSuper = false;
            //将页面滑动到第一页 初始位置 具体看onPageScrolled()方法
            //能执行本句话的前提是：mItems.size==0 并且 mFirstLayout==false
            // TODO: 2017/2/11
            onPageScrolledVertical(0, 0, 0);
            //当用户重写ViewPager的onPageScrolled()时没有调用super.onPageScrolled() 那么mCalledSuper的值仍然会是false
            //如果调用了的话那么mCalledSuper就会使true
            //所以当mCalledSuper=false时就直接抛出异常
            if (!mCalledSuper) {
                throw new IllegalStateException(
                        "onPageScrolled did not call superclass implementation");
            }
            return false;
        }
        // TODO: 2017/2/11
        //拿到当前getScrollX()对应的ItemInfo 具体看infoForCurrentScrollPosition()方法内部注释
        final ItemInfo ii = infoForCurrentScrollPositionVertical();

        final int height = getClientHeight();
        final int heightWithMargin = height + mPageMargin;
        final float marginOffset = (float) mPageMargin / height;

        final int currentPage = ii.position;
        //对于ItemInfo的offset,当position=1，offset=1.0,当position=2,offset=2.0
        //当position=4,offset=4.0
        //由infoForCurrentScrollPosition()方法内部代码可知，一个页面是使用offset来标记左右两端的
        //当为第0页时，leftBound=ii.offset=0.0,rightBound=leftBound+ii.widthFactor+marginOffset=2.0
        //其中一页的宽度是：ii.widthFactor+marginOffset，即一页的结束的position是开始的position加上这页的宽度
        //其中marginOffset=(float) mPageMargin / width;

        //其中下面的((float) xpos / width)是当前位置的offset,然后减去ii.offset(减去左边初始offset)
        //然后整体除以一页的宽度的offset，算出当前位置的offset相对一页的整个offset的比例
        //所以pageOffset的取值是0-1，包括0不包括1
        final float pageOffset = (((float) ypos / height) - ii.offset)
                / (ii.widthFactor + marginOffset);
        //然后用此比例乘以该页的全部宽度像素，结果就是水平上位移的像素pixel
        final int offsetPixels = (int) (pageOffset * heightWithMargin);
        //然后准备开始回调onPageScrolled（）方法，先令mCalledSuper=false 然后当你在重写onPageScrolled()时没有调用super.onPageScrolled（）
        //时，就会抛出IllegalStateException异常
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

    //处理decor view 的滑动 ，然后分发onPageScroll()事件给订阅者，然后处理PageTransformer
    // 而对于non-decor view的滑动是在completeScroll()方法中
    @CallSuper
    protected void onPageScrolledHorizontal(int position, float offset, int offsetPixels) {
        // Offset any decor views if needed - keep them on-screen at all times.

        //先处理decor view 的移动
        if (mDecorChildCount > 0) {
            //拿到当前的scroll 的横坐标
            final int scrollX = getScrollX();
            //左右padding
            int paddingLeft = getPaddingLeft();
            int paddingRight = getPaddingRight();
            final int width = getWidth();
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                final LayoutParams lp = (YViewPagerOrigin.LayoutParams) child.getLayoutParams();
                //筛选掉不是decor view 的
                if (!lp.isDecor) continue;

                //下面的代码就和onLayout()中的是一样的 这里就不赘述了
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
                //上面计算后得到的childLeft就是当初layout时给该child view 设置的左上坐标的横坐标 即初始位置

                //然后使用child.getLeft()来拿到当前该child的左上坐标的横坐标减去初始位置的横坐标
                //即算出来的是该child 需要移动到而位移大小
                final int childOffset = childLeft - child.getLeft();
                if (childOffset != 0) {
                    //使用offsetLeftAndRight()来在水平方向上移动childOffset的距离
                    child.offsetLeftAndRight(childOffset);
                }
            }
        }

        //处理完decor view 的移动之后 开始分发滑动时间   不断回调OnPageChangeListener接口的onPageScrolled()方法
        //也就是说此时如果你给ViewPager设置了OnPageChangeListener接口的话 此时onPageScrolled方法就会得到调用
        dispatchOnPageScrolled(position, offset, offsetPixels);

        //开始处理设置的PageTransformer
        //PageTransformer的话是ViewPager内部的类 作用是在Viewpager滑动时给页面做动画
        //注意做动画的对象是non-decor view
        if (mPageTransformer != null) {
            final int scrollX = getScrollX();
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                //排除掉 decor view
                if (lp.isDecor) continue;
                // TODO: 2017/2/11
                final float transformPos = (float) (child.getLeft() - scrollX) / getClientWidth();
                mPageTransformer.transformPage(child, transformPos);
            }
        }
        //将mCalledSuper标记设为true 表示子类已经调用了 super.onPageScrolled()方法
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

    //处理decor view 的滑动 ，然后分发onPageScroll()事件给订阅者，然后处理PageTransformer
    // 而对于non-decor view的滑动是在completeScroll()方法中
    @CallSuper
    protected void onPageScrolledVertical(int position, float offset, int offsetPixels) {
        // Offset any decor views if needed - keep them on-screen at all times.

        //先处理decor view 的移动
        if (mDecorChildCount > 0) {
            //拿到当前的scroll 的横坐标
            final int scrollY = getScrollY();
            //左右padding
            int paddingTop = getPaddingTop();
            int paddingBottom = getPaddingBottom();
            final int height = getHeight();
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                final YViewPagerOrigin.LayoutParams lp = (YViewPagerOrigin.LayoutParams) child.getLayoutParams();
                //筛选掉不是decor view 的
                if (!lp.isDecor) continue;

                //下面的代码就和onLayout()中的是一样的 这里就不赘述了
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

                //然后使用child.getLeft()来拿到当前该child的左上坐标的横坐标减去初始位置的横坐标
                //即算出来的是该child 需要移动到而位移大小
                final int childOffset = childTop - child.getTop();
                if (childOffset != 0) {
                    //使用offsetLeftAndRight()来在水平方向上移动childOffset的距离
                    child.offsetTopAndBottom(childOffset);
                }
            }
        }

        //处理完decor view 的移动之后 开始分发滑动时间   不断回调OnPageChangeListener接口的onPageScrolled()方法
        //也就是说此时如果你给ViewPager设置了OnPageChangeListener接口的话 此时onPageScrolled方法就会得到调用
        dispatchOnPageScrolled(position, offset, offsetPixels);

        //开始处理设置的PageTransformer
        //PageTransformer的话是ViewPager内部的类 作用是在Viewpager滑动时给页面做动画
        //注意做动画的对象是non-decor view
        if (mPageTransformer != null) {
            final int scrollY = getScrollY();
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                //排除掉 decor view
                if (lp.isDecor) continue;
                // TODO: 2017/2/11
                final float transformPos = (float) (child.getTop() - scrollY) / getClientHeight();
                mPageTransformer.transformPage(child, transformPos);
            }
        }
        //将mCalledSuper标记设为true 表示子类已经调用了 super.onPageScrolled()方法
        mCalledSuper = true;
    }


    private void dispatchOnPageScrolled(int position, float offset, int offsetPixels) {
        if (mOnPageChangeListener != null) {
            mOnPageChangeListener.onPageScrolled(position, offset, offsetPixels);
        }
        if (mOnPageChangeListeners != null) {
            for (int i = 0, z = mOnPageChangeListeners.size(); i < z; i++) {
                OnPageChangeListener listener = mOnPageChangeListeners.get(i);
                if (listener != null) {
                    listener.onPageScrolled(position, offset, offsetPixels);
                }
            }
        }
        if (mInternalPageChangeListener != null) {
            mInternalPageChangeListener.onPageScrolled(position, offset, offsetPixels);
        }
    }

    private void dispatchOnPageSelected(int position) {
        if (mOnPageChangeListener != null) {
            mOnPageChangeListener.onPageSelected(position);
        }
        if (mOnPageChangeListeners != null) {
            for (int i = 0, z = mOnPageChangeListeners.size(); i < z; i++) {
                OnPageChangeListener listener = mOnPageChangeListeners.get(i);
                if (listener != null) {
                    listener.onPageSelected(position);
                }
            }
        }
        if (mInternalPageChangeListener != null) {
            mInternalPageChangeListener.onPageSelected(position);
        }
    }

    private void dispatchOnScrollStateChanged(int state) {
        if (mOnPageChangeListener != null) {
            mOnPageChangeListener.onPageScrollStateChanged(state);
        }
        if (mOnPageChangeListeners != null) {
            for (int i = 0, z = mOnPageChangeListeners.size(); i < z; i++) {
                YViewPagerOrigin.OnPageChangeListener listener = mOnPageChangeListeners.get(i);
                if (listener != null) {
                    listener.onPageScrollStateChanged(state);
                }
            }
        }
        if (mInternalPageChangeListener != null) {
            mInternalPageChangeListener.onPageScrollStateChanged(state);
        }
    }


    /**
     * completeScroll()方法和computeScroll()方法的区别：
     * 后者是用户在用手指拖动viewpager页面滑动时会不断调用computeScroll()方法，然后在里面来使用scrollTo方
     * 法来一点点移动non-decor view的位置。
     * <p>
     * 而前者是当用户释放后，viewpager会自动滑动到指定的位置（可能是把这页滑动完，或
     * 者是返回去滑动到初始位置），完成后面的滑动事件。所以在completeScrolled()方法内部一开
     * 始就回调SCROLL_STATE_SETTLING方法 然后使用一个runnable在滑动最终结束后回调SCROLL_STATE_IDLE表示此次滑动过程结束
     *
     * @param postEvents
     */
    private void completeScrollHorizontal(boolean postEvents) {
        //当用户滑动释放后，回调SCROLL_STATE_SETTLING状态 接下来的ViewPager滑动是要靠系统捕获到的用户滑动的滑动速度
        boolean needPopulate = mScrollState == SCROLL_STATE_SETTLING;
        if (needPopulate) {
            // Done with scroll, no longer want to cache view drawing.
            setScrollingCacheEnabled(false);
            boolean wasScrolling = !mScroller.isFinished();
            //当滑动还没有结束时
            if (wasScrolling) {
                //终止动画  直接跳到最后
                mScroller.abortAnimation();

                //现在实际上并没有滑动到最终的位置 只是将mScroller内部的mCurrX、mCurrY 设置为 mFinalX，mFinalY
                //所以此时 getScrollX()返回的是当前的滑动位置  而不是最终的位置  所以这里使用的是oldX,oldY
                int oldX = getScrollX();
                int oldY = getScrollY();
                //由于上面调用了mScroller.abortAnimation()方法  所以这里调用getCurrX() 返回的就是 Scroller 内部的 mFinaX
                //即最终要滑动的横坐标x  具体可以看 Scroller的abortAnimation()方法实现
                int x = mScroller.getCurrX();
                int y = mScroller.getCurrY();
                //如果最终的位置和当前的坐标位置不一样  那么就直接滑动到最终的x、y坐标处
                if (oldX != x || oldY != y) {
                    //这句话执行的话就是已经把non-decor view滑动到指定的位置
                    scrollTo(x, y);
                    //由于ViewPager处理的是左右滑动  所以对于横坐标x  还需要处理一下 具体看pageScrolled()方法
                    if (x != oldX) {
                        //这里不滑动 只是在pageScrolled()方法中计算offset参数 然后回调onPageScrolled()方法 然后
                        // 在onPageScrolled()方法中移动decor view、分发事件、处理pagerTransformer
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
        //当用户滑动释放后，回调SCROLL_STATE_SETTLING状态 接下来的ViewPager滑动是要靠系统捕获到的用户滑动的滑动速度
        boolean needPopulate = mScrollState == SCROLL_STATE_SETTLING;
        if (needPopulate) {
            // Done with scroll, no longer want to cache view drawing.
            setScrollingCacheEnabled(false);
            boolean wasScrolling = !mScroller.isFinished();
            //当滑动还没有结束时
            if (wasScrolling) {
                //终止动画  直接跳到最后
                mScroller.abortAnimation();

                //现在实际上并没有滑动到最终的位置 只是将mScroller内部的mCurrX、mCurrY 设置为 mFinalX，mFinalY
                //所以此时 getScrollX()返回的是当前的滑动位置  而不是最终的位置  所以这里使用的是oldX,oldY
                int oldX = getScrollX();
                int oldY = getScrollY();
                //由于上面调用了mScroller.abortAnimation()方法  所以这里调用getCurrX() 返回的就是 Scroller 内部的 mFinaX
                //即最终要滑动的横坐标x  具体可以看 Scroller的abortAnimation()方法实现
                int x = mScroller.getCurrX();
                int y = mScroller.getCurrY();
                //如果最终的位置和当前的坐标位置不一样  那么就直接滑动到最终的x、y坐标处
                if (oldX != x || oldY != y) {
                    //这句话执行的话就是已经把non-decor view滑动到指定的位置
                    scrollTo(x, y);
                    //由于ViewPager处理的是左右滑动  所以对于横坐标x  还需要处理一下 具体看pageScrolled()方法
                    if (y != oldY) {
                        //这里不滑动 只是在pageScrolled()方法中计算offset参数 然后回调onPageScrolled()方法 然后
                        // 在onPageScrolled()方法中移动decor view、分发事件、处理pagerTransformer
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
            //ACTION_UP或者ACTION_CANCEL事件之后,表示一次触摸事件已经完毕了 重置一些属性在resetTouch()里面
            resetTouch();
            return false;
        }

        // Nothing more to do here if we have decided whether or not we
        // are dragging.
        //当非DOWN事件来临时，即MOVE或者UP事件来临时
        if (action != MotionEvent.ACTION_DOWN) {
            //如果正处于用户拖拽的状态下 那么就拦截该事件 交由ViewPager来处理
            if (mIsBeingDragged) {
                if (DEBUG) Log.v(TAG, "Intercept returning true!");
                return true;
            }
            //不拦截
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

                //mActivePointerId默认为INVALID_POINTER，并且在上面提到的resetTouch()方法中也会将mActivePointerId设为默认值
                final int activePointerId = mActivePointerId;
                if (activePointerId == INVALID_POINTER) {
                    // If we don't have a valid id, the touch down wasn't on content.
                    break;
                }
                //
                final int pointerIndex = ev.findPointerIndex(activePointerId);
                final float x = ev.getX(pointerIndex);
                //计算出水平方向上的距离
                final float dx = isVertical ? x - mInitialMotionX : x - mLastMotionX;
                //计算出水平上的距离的绝对值
                final float xDiff = Math.abs(dx);
                final float y = ev.getY(pointerIndex);

                final float dy = isVertical ? y - mLastMotionY : y - mInitialMotionY;
                //计算出竖直方向上的距离的绝对值
                final float yDiff = Math.abs(dy);

                if (DEBUG) Log.v(TAG, "Moved x to " + x + "," + y + " diff=" + xDiff + "," + yDiff);

                if (!isVertical && dx != 0 && !isGutterDragHorizontal(mLastMotionX, dx)
                        && canScrollHorizontal(this, false, (int) dx, (int) x, (int) y)) {
                    // Nested view has scrollable area under this point. Let it be handled there.
                    //ViewPager内部有可以水平滑动的View，并且点击的点在该区域内 不拦截
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

                //当点击的点在第一象限斜率为1/2的直线以内时，斜率小于0.5时 判定为水平拖动 拦截
                if (!isVertical && xDiff > mTouchSlop && xDiff * 0.5f > yDiff) {
                    //表示开始拖动
                    mIsBeingDragged = true;
                    //告诉父类不要拦截我的事件  本次DOWN到UP事件我来处理
                    requestParentDisallowInterceptTouchEvent(true);
                    //回调onPageScrollStateChanged()方法表示开始拖动滑动了
                    setScrollState(SCROLL_STATE_DRAGGING);
                    //更新坐标x值 ±mTouchSlop
                    mLastMotionX = dx > 0
                            ? mInitialMotionX + mTouchSlop : mInitialMotionX - mTouchSlop;
                    mLastMotionY = y;
                    setScrollingCacheEnabled(true);
                } else if (!isVertical && yDiff > mTouchSlop) {
                    // The finger has moved enough in the vertical
                    // direction to be counted as a drag...  abort
                    // any attempt to drag horizontally, to work correctly
                    // with children that have scrolling containers.
                    //水平拖动 不拦截事件  交给内部去使用
                    mIsUnableToDrag = true;
                } else if (isVertical && yDiff > mTouchSlop && yDiff * 0.5f > xDiff) {
                    //表示开始拖动
                    mIsBeingDragged = true;
                    //告诉父类不要拦截我的事件  本次DOWN到UP事件我来处理
                    requestParentDisallowInterceptTouchEvent(true);
                    //回调onPageScrollStateChanged()方法表示开始拖动滑动了
                    setScrollState(SCROLL_STATE_DRAGGING);
                    //更新坐标x值 ±mTouchSlop
                    mLastMotionY = dy > 0
                            ? mInitialMotionY + mTouchSlop : mInitialMotionY - mTouchSlop;
                    mLastMotionX = x;
                    setScrollingCacheEnabled(true);
                } else if (isVertical && xDiff > mTouchSlop) {
                    mIsUnableToDrag = true;
                }

                if (mIsBeingDragged) {
                    // Scroll to follow the motion event
                    //具体见performDrag()注释
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
                //记录下按下的位置
                mLastMotionX = mInitialMotionX = ev.getX();
                mLastMotionY = mInitialMotionY = ev.getY();
                mActivePointerId = ev.getPointerId(0);
                //初始化两个参数
                mIsUnableToDrag = false;
                mIsScrollStarted = true;

                mScroller.computeScrollOffset();
                //当用户点击ViewPager内部页面滑动时，状态为SCROLL_STATE_DRAGGING,然后当用户拖动释放后，ViewPager依靠惯性
                //完成剩下的部分的滑动，当用户拖动释放后，状态为SCROLL_STATE_SETTLING
                //这里处理的是当用户滑动释放后，页面还没有完全滑动完成的过程中用户又按下了，此时继续跟着用户的拖动来滑动
                if (!isVertical && mScrollState == SCROLL_STATE_SETTLING
                        && Math.abs(mScroller.getFinalX() - mScroller.getCurrX()) > mCloseEnough) {
                    // Let the user 'catch' the pager as it animates.
                    //先终止滑动动画
                    mScroller.abortAnimation();
                    //
                    mPopulatePending = false;
                    if (isVertical) {
                        populateVertical();
                    } else {
                        populateHorizontal();
                    }
                    mIsBeingDragged = true;
                    //不让上一层ViewGroup拦截事件
                    requestParentDisallowInterceptTouchEvent(true);
                    //更新当前的状态，又是SCROLL_STATE_DRAGGING
                    setScrollState(SCROLL_STATE_DRAGGING);
                } else if (!isVertical) {
                    //直接回调滑动完成
                    completeScrollHorizontal(false);
                    mIsBeingDragged = false;
                } else if (isVertical && mScrollState == SCROLL_STATE_SETTLING
                        && Math.abs(mScroller.getFinalY() - mScroller.getCurrY()) > mCloseEnough) {
                    // Let the user 'catch' the pager as it animates.
                    //先终止滑动动画
                    mScroller.abortAnimation();
                    //
                    mPopulatePending = false;
                    if (isVertical) {
                        populateVertical();
                    } else {
                        populateHorizontal();
                    }
                    mIsBeingDragged = true;
                    //不让上一层ViewGroup拦截事件
                    requestParentDisallowInterceptTouchEvent(true);
                    //更新当前的状态，又是SCROLL_STATE_DRAGGING
                    setScrollState(SCROLL_STATE_DRAGGING);
                } else if (isVertical) {
                    //直接回调滑动完成
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

        if (mAdapter == null || mAdapter.getCount() == 0) {
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
                //终止动画
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
                    //开始拖动滑动
                    if (!isVertical && xDiff > mTouchSlop && xDiff > yDiff) {
                        mIsBeingDragged = true;
                        //不让上一级拦截事件
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
                        //不让上一级拦截事件
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
        //水平方向上的位移
        final float deltaX = mLastMotionX - x;
        //更新
        mLastMotionX = x;

        //获得当前的滑动位置
        float oldScrollX = getScrollX();
        //水平方向上要移动的最终距离
        float scrollX = oldScrollX + deltaX;
        final int width = getClientWidth();

        //左边界和右边界
        float leftBound = width * mFirstOffset;
        float rightBound = width * mLastOffset;

        boolean leftAbsolute = true;
        boolean rightAbsolute = true;

        final ItemInfo firstItem = mItems.get(0);
        final ItemInfo lastItem = mItems.get(mItems.size() - 1);
        //表示当前页不是处于0，或者1 当mOffScreenPageLimit=1时
        //因为当当前页处于0或者1时，mItems中存储的是 0、1 或者 0、1、2 这种情况的话mItems.get(0).position=0
        if (firstItem.position != 0) {
            //如果当前页是处于最左边时，leftAbsolute=true
            leftAbsolute = false;
            leftBound = firstItem.offset * width;
        }

        //表示当前页不是处于4或者3， 当mOffScreenPageLimit=1、adapterCount=5时
        //因为当当前页处于4或者3时，mItems中存储的是 3,4 或者 2,3,4 这种情况的话mItems.get(mItems.size()-1).position=mAdapter.getCount() - 1
        if (lastItem.position != mAdapter.getCount() - 1) {
            //如果当前页是处于最右边时，rightAbsolute=true
            rightAbsolute = false;
            rightBound = lastItem.offset * width;
        }

        if (scrollX < leftBound) {
            if (leftAbsolute) {
                //当到了最左边还往左边划
                float over = leftBound - scrollX;
                needsInvalidate = mLeftEdge.onPull(Math.abs(over) / width);
            }
            scrollX = leftBound;
        } else if (scrollX > rightBound) {
            if (rightAbsolute) {
                //当到了最右边还往右边划
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
        //更新
        mLastMotionY = y;

        //获得当前的滑动位置
        float oldScrollY = getScrollY();
        float scrollY = oldScrollY + deltaY;
        final int height = getClientHeight();

        float topBound = height * mFirstOffset;
        float bottomBound = height * mLastOffset;

        boolean topAbsolute = true;
        boolean bottomAbsolute = true;

        final ItemInfo firstItem = mItems.get(0);
        final ItemInfo lastItem = mItems.get(mItems.size() - 1);
        //表示当前页不是处于0，或者1 当mOffScreenPageLimit=1时
        //因为当当前页处于0或者1时，mItems中存储的是 0、1 或者 0、1、2 这种情况的话mItems.get(0).position=0
        if (firstItem.position != 0) {
            //如果当前页是处于最左边时，leftAbsolute=true
            topAbsolute = false;
            topBound = firstItem.offset * height;
        }

        //表示当前页不是处于4或者3， 当mOffScreenPageLimit=1、adapterCount=5时
        //因为当当前页处于4或者3时，mItems中存储的是 3,4 或者 2,3,4 这种情况的话mItems.get(mItems.size()-1).position=mAdapter.getCount() - 1
        if (lastItem.position != mAdapter.getCount() - 1) {
            //如果当前页是处于最右边时，rightAbsolute=true
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
                //当到了最右边还往右边划
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

    //根据当前的getScrollX()值计算出当前应该是对应的那个ItemInfo   一个ItemInfo对应一个ViewPager的页面
    private ItemInfo infoForCurrentScrollPositionHorizontal() {
        //viewpager的measuredWidth减去左右padding
        final int width = getClientWidth();
        //如果上面计算出来的width>0时，则使用getScrollX()的值除以width，getScrollX()的值就是ViewPager内
        // 部的child view 被滑动的距离
        /**
         * 当你第一次滑动时，getScrollX()是从0-width的 然后当getScrollX=width时代表第一页已经完全不见了 划过去了 即scrollOffset=1.0
         * 当你从第二页开始滑动到第三页时，getScrollX()是从width-2*width的 然后当getScrollX=2*width是代表第二页已经完全不见了 划过去了 即scrollOffset=2.0
         * ... 剩下同理
         */
        final float scrollOffset = width > 0 ? (float) getScrollX() / width : 0;
        //margin offset
        final float marginOffset = width > 0 ? (float) mPageMargin / width : 0;

        int lastPos = -1;
        float lastOffset = 0.f;
        float lastWidth = 0.f;
        boolean first = true;

        ItemInfo lastItem = null;
        for (int i = 0; i < mItems.size(); i++) {
            ItemInfo ii = mItems.get(i);
            float offset;

            /**
             * 如果你左右滑动的话 比如你向左滑动的话，就是从position=0 切换到position=1  此时lastPos=0; li.position=1
             * 所以此时li.position恒等于lastPos+1
             *
             */
            //first默认是true 只有当你第一次左滑或者右滑之后 在下面会将first赋值为false 然后滑动就都是false了
            //lastPos保存的是上一次滑动时上一页的position。
            //而ii.position是当前页的position ，即假如你从position=0滑动到position=1页时，lastPos=0，ii.position=1;
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
            /**
             * 当你滑动时 getScrollX()是n*width(n是第几页 即当从第一页滑动到第二页时 n=1，当从第二页滑动到第三页时 n=2 ....)时，scrollOffset=n(n是
             * 第几页 即当从第一页滑动到第二页时 n=1，当从第二页滑动到第三页时 n=2 ....),并且此时leftBound=(n-1)(n为第几页 即当从第一页滑动到第二页时 n=1，
             * 当从第二页滑动到第三页时 n=2 ....)，并且此时rightBound=(n+1) (n为第几页 即当从第一页滑动到第二页时 n=1，当从第二页滑动到第三页时 n=2 ....)
             *
             * 此时能执行到此处代码块的条件就是必须是 getScrollX()是n*width,此时上面的scrollOffset >= leftBound满足，进入后不满足scrollOffset < rightBound
             * （即因为是scrollOffset = rightBound而不是scrollOffset < rightBound）
             *  然后 对于 mItems：当你设置的setOffscreenPageLimit(i) (i<=1) 或者不设置setOffscreenPageLimit()时，即mOffscreenPageLimit=1
             *  假设有五页：
             *    此时当你刚打开时，mItems中存放了两个(position=0和position=1)，然后当你滑动到下一页(即position=1页)时，mItems保持不变
             *    然后当你继续滑动到下一页(即position=2页)时，此时mItems中就存放了三个(position=0,position=1,position=2),继续滑到下一页
             *    (即position=3)时，此时mItems中仍然是存放着三个(position=1,position=2,position=3),当你继续滑，滑到最后一页(position=4)时，此
             *    时mItems中还是3个(position=2,position=3,position=4)
             */

            // TODO: 2017/2/14 mItems搞懂
            first = false;
            lastPos = ii.position;
            lastOffset = offset;
            lastWidth = ii.widthFactor;
            lastItem = ii;

            //执行到这里的话就表示for循环里面一次执行完毕了 因为其他结束的地方都是return
        }
        return lastItem;
    }

    private ItemInfo infoForCurrentScrollPositionVertical() {
        final int height = getClientHeight();
        //如果上面计算出来的width>0时，则使用getScrollX()的值除以width，getScrollX()的值就是ViewPager内
        // 部的child view 被滑动的距离
        /**
         * 当你第一次滑动时，getScrollX()是从0-width的 然后当getScrollX=width时代表第一页已经完全不见了 划过去了 即scrollOffset=1.0
         * 当你从第二页开始滑动到第三页时，getScrollX()是从width-2*width的 然后当getScrollX=2*width是代表第二页已经完全不见了 划过去了 即scrollOffset=2.0
         * ... 剩下同理
         */
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

            /**
             * 如果你左右滑动的话 比如你向左滑动的话，就是从position=0 切换到position=1  此时lastPos=0; li.position=1
             * 所以此时li.position恒等于lastPos+1
             *
             */
            //first默认是true 只有当你第一次左滑或者右滑之后 在下面会将first赋值为false 然后滑动就都是false了
            //lastPos保存的是上一次滑动时上一页的position。
            //而ii.position是当前页的position ，即假如你从position=0滑动到position=1页时，lastPos=0，ii.position=1;
            // TODO: 2017/2/14
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
            /**
             * 当你滑动时 getScrollX()是n*width(n是第几页 即当从第一页滑动到第二页时 n=1，当从第二页滑动到第三页时 n=2 ....)时，scrollOffset=n(n是
             * 第几页 即当从第一页滑动到第二页时 n=1，当从第二页滑动到第三页时 n=2 ....),并且此时leftBound=(n-1)(n为第几页 即当从第一页滑动到第二页时 n=1，
             * 当从第二页滑动到第三页时 n=2 ....)，并且此时rightBound=(n+1) (n为第几页 即当从第一页滑动到第二页时 n=1，当从第二页滑动到第三页时 n=2 ....)
             *
             * 此时能执行到此处代码块的条件就是必须是 getScrollX()是n*width,此时上面的scrollOffset >= leftBound满足，进入后不满足scrollOffset < rightBound
             * （即因为是scrollOffset = rightBound而不是scrollOffset < rightBound）
             *  然后 对于 mItems：当你设置的setOffscreenPageLimit(i) (i<=1) 或者不设置setOffscreenPageLimit()时，即mOffscreenPageLimit=1
             *  假设有五页：
             *    此时当你刚打开时，mItems中存放了两个(position=0和position=1)，然后当你滑动到下一页(即position=1页)时，mItems保持不变
             *    然后当你继续滑动到下一页(即position=2页)时，此时mItems中就存放了三个(position=0,position=1,position=2),继续滑到下一页
             *    (即position=3)时，此时mItems中仍然是存放着三个(position=1,position=2,position=3),当你继续滑，滑到最后一页(position=4)时，此
             *    时mItems中还是3个(position=2,position=3,position=4)
             */

            // TODO: 2017/2/14 mItems搞懂
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
        //mFlingDistance默认为25dp mMinimumVelocity默认为400dp
        if (Math.abs(deltaX) > mFlingDistance && Math.abs(velocity) > mMinimumVelocity) {
            targetPage = velocity > 0 ? currentPage : currentPage + 1;
        } else {
            final float truncator = currentPage >= mCurItem ? 0.4f : 0.6f;
            targetPage = currentPage + (int) (pageOffset + truncator);
        }
        if (mItems.size() > 0) {
            final YViewPagerOrigin.ItemInfo firstItem = mItems.get(0);
            final YViewPagerOrigin.ItemInfo lastItem = mItems.get(mItems.size() - 1);
            // Only let the user target pages we have items for
            targetPage = Math.max(firstItem.position, Math.min(targetPage, lastItem.position));
        }
        return targetPage;
    }

    private int determineTargetPageVertical(int currentPage, float pageOffset, int velocity, int deltaY) {
        int targetPage;
        //mFlingDistance默认为25dp mMinimumVelocity默认为400dp
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
                && mAdapter != null && mAdapter.getCount() > 1)) {
            if (!isVertical&&!mLeftEdge.isFinished()) {
                final int restoreCount = canvas.save();
                final int height = getHeight() - getPaddingTop() - getPaddingBottom();
                final int width = getWidth();
                canvas.rotate(270);
                canvas.translate(-height + getPaddingTop(), mFirstOffset * width);
                mLeftEdge.setSize(height, width);
                needsInvalidate |= mLeftEdge.draw(canvas);
                canvas.restoreToCount(restoreCount);
            }else if (isVertical&&!mTopEdge.isFinished()) {
                final int restoreCount = canvas.save();
                final int height = getHeight();
                final int width = getWidth()-getPaddingLeft()-getPaddingRight();
                canvas.translate(getPaddingLeft(), mFirstOffset * height);
                mTopEdge.setSize(width, height);
                needsInvalidate |= mTopEdge.draw(canvas);
                canvas.restoreToCount(restoreCount);
            }
            if (!isVertical&&!mRightEdge.isFinished()) {
                final int restoreCount = canvas.save();
                final int width = getWidth();
                final int height = getHeight() - getPaddingTop() - getPaddingBottom();

                canvas.rotate(90);
                canvas.translate(-getPaddingTop(), -(mLastOffset + 1) * width);
                mRightEdge.setSize(height, width);
                needsInvalidate |= mRightEdge.draw(canvas);
                canvas.restoreToCount(restoreCount);
            }else if (isVertical&&!mBottomEdge.isFinished()) {
                final int restoreCount = canvas.save();
                final int width = getWidth()-getPaddingLeft()-getPaddingRight();
                final int height = getHeight();
                canvas.rotate(180,width,0);
                canvas.translate(width-getPaddingLeft(), -(mLastOffset + 1) * height);
                mBottomEdge.setSize(width,height);
                needsInvalidate |= mBottomEdge.draw(canvas);
                canvas.restoreToCount(restoreCount);
            }
        } else if(!isVertical){
            mLeftEdge.finish();
            mRightEdge.finish();
        }else if(isVertical){
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

    /**
     * Start a fake drag of the pager.
     * <p>
     * <p>A fake drag can be useful if you want to synchronize the motion of the ViewPager
     * with the touch scrolling of another view, while still letting the ViewPager
     * control the snapping motion and fling behavior. (e.g. parallax-scrolling tabs.)
     * Call fakeDragBy(float)to simulate the actual drag motion. Call
     * endFakeDrag() to complete the fake drag and fling as necessary.
     * <p>
     * <p>During a fake drag the ViewPager will ignore all touch events. If a real drag
     * is already in progress, this method will return false.
     *
     * @return true if the fake drag began successfully, false if it could not be started.
     */
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
//            final YViewPager1.ItemInfo ii = infoForCurrentScrollPosition();
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
//        final YViewPager1.ItemInfo firstItem = mItems.get(0);
//        final YViewPager1.ItemInfo lastItem = mItems.get(mItems.size() - 1);
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
            if (USE_CACHE) { //如果USE_CACHE为true的话就对于每个child来修改cache enable
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
        //如果要检查的view是ViewGroup 那么就进入if  否则就使用ViewCompat.canScrollHorizontally()来判断
        if (v instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) v;
            //拿到水平方向上和竖直方向上的位移，即mScrollX、mScrollY的值
            final int scrollX = v.getScrollX();
            final int scrollY = v.getScrollY();
            final int count = group.getChildCount();
            // Count backwards - let topmost views consume scroll distance first.
            //从最上面的child的开始判断 因为最上面的child应该首先接收到滑动时间，先要产生滑动
            for (int i = count - 1; i >= 0; i--) {
                // TODO: Add versioned support here for transformed views.
                // This will not work for transformed views in Honeycomb+
                final View child = group.getChildAt(i);
                /**
                 * 下面有五个条件 都满足时才在for里面返回true：
                 *   1-4个条件是判断用户点击的点的坐标是否在child的显示范围内
                 *   第5个条件是判断该child是否可以水平滑动  如果该child不是ViewGroup的话，
                 *   就直接使用ViewCompat.canScrollHorizontally()来判断
                 */
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
        //如果要检查的view是ViewGroup 那么就进入if  否则就使用ViewCompat.canScrollHorizontally()来判断
        if (v instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) v;
            //拿到水平方向上和竖直方向上的位移，即mScrollX、mScrollY的值
            final int scrollX = v.getScrollX();
            final int scrollY = v.getScrollY();
            final int count = group.getChildCount();
            // Count backwards - let topmost views consume scroll distance first.
            //从最上面的child的开始判断 因为最上面的child应该首先接收到滑动时间，先要产生滑动
            for (int i = count - 1; i >= 0; i--) {
                // TODO: Add versioned support here for transformed views.
                // This will not work for transformed views in Honeycomb+
                final View child = group.getChildAt(i);
                /**
                 * 下面有五个条件 都满足时才在for里面返回true：
                 *   1-4个条件是判断用户点击的点的坐标是否在child的显示范围内
                 *   第5个条件是判断该child是否可以水平滑动  如果该child不是ViewGroup的话，
                 *   就直接使用ViewCompat.canScrollHorizontally()来判断
                 */
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
        if (mAdapter != null && mCurItem < (mAdapter.getCount() - 1)) {
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
                    YViewPagerOrigin.ItemInfo ii = infoForChild(child);
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
                YViewPagerOrigin.ItemInfo ii = infoForChild(child);
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
                YViewPagerOrigin.ItemInfo ii = infoForChild(child);
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
                final YViewPagerOrigin.ItemInfo ii = infoForChild(child);
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
        return new YViewPagerOrigin.LayoutParams();
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return generateDefaultLayoutParams();
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof YViewPagerOrigin.LayoutParams && super.checkLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new YViewPagerOrigin.LayoutParams(getContext(), attrs);
    }

    class MyAccessibilityDelegate extends AccessibilityDelegateCompat {

        @Override
        public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(host, event);
            event.setClassName(YViewPagerOrigin.class.getName());
            final AccessibilityRecordCompat recordCompat =
                    AccessibilityEventCompat.asRecord(event);
            recordCompat.setScrollable(canScroll());
            if (event.getEventType() == AccessibilityEventCompat.TYPE_VIEW_SCROLLED
                    && mAdapter != null) {
                recordCompat.setItemCount(mAdapter.getCount());
                recordCompat.setFromIndex(mCurItem);
                recordCompat.setToIndex(mCurItem);
            }
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            info.setClassName(YViewPagerOrigin.class.getName());
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
            return (mAdapter != null) && (mAdapter.getCount() > 1);
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

