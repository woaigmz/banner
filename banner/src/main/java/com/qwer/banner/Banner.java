package com.qwer.banner;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static android.support.v4.view.ViewPager.OnPageChangeListener;

/**
 * Created by woaigmz on 2018/1/17.
 */

public class Banner extends RelativeLayout implements OnPageChangeListener, View.OnClickListener {

    private int indicatorMargin = BannerConfig.PADDING_SIZE;
    private int indicatorSelectedResId = R.drawable.dot_select;
    private int indicatorUnselectedResId = R.drawable.dot;
    private int bannerLoadingViewImgRes;
    private int bannerStyle = BannerConfig.CIRCLE_INDICATOR;
    private int delayTime = BannerConfig.TIME;
    private int scrollTime = BannerConfig.DURATION;
    private boolean isAutoPlay = BannerConfig.IS_AUTO_PLAY;
    private boolean isScroll = BannerConfig.IS_SCROLL;
    private int bannerHeight;
    private int contentHeight;
    private Context context;
    private List imageUrls;
    private List<View> imageViews;
    private List<ImageView> indicatorImages;
    private View loadingView;
    private BannerViewPager viewPager;
    private BannerScroller mScroller;
    private LinearLayout indicator;
    private BannerPagerAdapter adapter;
    private OnPageChangeListener mOnPageChangeListener;
    private int currentItem = 0;
    private int lastPosition = 1;
    private int count = 0;
    private int scaleType = 1;
    private int gravity = -1;
    private ImageEngine imageLoader;
    private OnBannerListener listener;
    private WeakHandler handler = new WeakHandler();
    private int indicatorWidth;
    private int indicatorHeight;
    private View root;
    private int indictorMarginTop;

    public Banner(@NonNull Context context) {
        this(context, null);
    }

    public Banner(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Banner(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        createModel();
        root = LayoutInflater.from(context).inflate(R.layout.banner, this, true);
        initView(attrs);
    }

    private void createModel() {
        imageUrls = new ArrayList<>();
        imageViews = new ArrayList<>();
        indicatorImages = new ArrayList<>();
        imageViews.clear();
    }

    private void initView(AttributeSet attrs) {
        readAttrsFromXmlAndSet(attrs);
        setIndicatorParams();
        setViewPagerParams();
        initLoadView();
        initViewPagerScroll();
    }

    private void setIndicatorParams() {
        indicator = root.findViewById(R.id.ll_homepage_banner_dots);
        indicator.setMinimumHeight(indicatorHeight);
        indicator.setMinimumWidth(indicatorWidth);
    }

    private void setViewPagerParams() {
        viewPager = root.findViewById(R.id.avp_homepage_banner);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, contentHeight);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        params.bottomMargin = indictorMarginTop;
        viewPager.setLayoutParams(params);
    }


    private void initLoadView() {
        loadingView = root.findViewById(R.id.view_banner_loading);
        loadingView.setBackgroundResource(bannerLoadingViewImgRes);
        loadingView.setOnClickListener(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int spec = bannerHeight == 0 ? heightMeasureSpec : bannerHeight;
        super.onMeasure(widthMeasureSpec, spec);
    }


    public void showLoadingView() {
        loadingView.setVisibility(VISIBLE);
    }

    public void dimissLoadingView() {
        loadingView.setVisibility(GONE);
    }

    private void readAttrsFromXmlAndSet(AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        int dotMarginDefault = context.getResources().getDisplayMetrics().widthPixels / 80;
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.Banner);
        indicatorWidth = typedArray.getDimensionPixelSize(R.styleable.Banner_indicator_width, context.getResources().getDisplayMetrics().widthPixels);
        indicatorHeight = typedArray.getDimensionPixelSize(R.styleable.Banner_indicator_height, (int) context.getResources().getDimension(R.dimen.indicator_height));
        indicatorMargin = typedArray.getDimensionPixelSize(R.styleable.Banner_dot_margin, BannerConfig.PADDING_SIZE);
        indictorMarginTop = typedArray.getDimensionPixelSize(R.styleable.Banner_indicator_margin_top, BannerConfig.PADDING_SIZE);
        bannerHeight = typedArray.getDimensionPixelSize(R.styleable.Banner_banner_height, (int) context.getResources().getDimension(R.dimen.banner_height));
        contentHeight = typedArray.getDimensionPixelSize(R.styleable.Banner_content_height, (int) context.getResources().getDimension(R.dimen.banner_height));
        indicatorSelectedResId = typedArray.getResourceId(R.styleable.Banner_indicator_drawable_selected, R.drawable.dot_select);
        indicatorUnselectedResId = typedArray.getResourceId(R.styleable.Banner_indicator_drawable_unselected, R.drawable.dot);
        scaleType = typedArray.getInt(R.styleable.Banner_content_image_scale_type, scaleType);
        delayTime = typedArray.getInt(R.styleable.Banner_delay_time, BannerConfig.TIME);
        scrollTime = typedArray.getInt(R.styleable.Banner_scroll_time, BannerConfig.DURATION);
        isAutoPlay = typedArray.getBoolean(R.styleable.Banner_is_auto_play, BannerConfig.IS_AUTO_PLAY);
        bannerLoadingViewImgRes = typedArray.getResourceId(R.styleable.Banner_default_image, R.drawable.no_banner);
        typedArray.recycle();
    }

    public void setOnBannerListener(OnBannerListener listener) {
        this.listener = listener;
    }

    public void setOnPageChangeListener(OnPageChangeListener onPageChangeListener) {
        this.mOnPageChangeListener = onPageChangeListener;
    }

    private void initViewPagerScroll() {
        try {
            Field mField = ViewPager.class.getDeclaredField("mScroller");
            mField.setAccessible(true);
            mScroller = new BannerScroller(viewPager.getContext());
            mScroller.setDuration(scrollTime);
            mField.set(viewPager, mScroller);
        } catch (Exception e) {
            // log
        }
    }

    public Banner setImages(List<?> imageUrls) {
        this.imageUrls = imageUrls;
        this.count = imageUrls.size();
        return this;
    }

    public Banner start() {
        setImageList(imageUrls);
        setData();
        return this;
    }

    private void setData() {
        currentItem = 1;
        if (adapter == null) {
            adapter = new BannerPagerAdapter();
            viewPager.addOnPageChangeListener(this);
        }
        viewPager.setAdapter(adapter);
        viewPager.setFocusable(true);
        viewPager.setCurrentItem(1);
        if (gravity != -1)
            indicator.setGravity(gravity);
        if (isScroll && count > 1) {
            viewPager.setScrollable(true);
        } else {
            viewPager.setScrollable(false);
        }
        if (isAutoPlay)
            startAutoPlay();
    }

    private void startAutoPlay() {
        handler.removeCallbacks(task);
        handler.postDelayed(task, delayTime);
    }

    public void stopAutoPlay() {
        handler.removeCallbacks(task);
    }

    private final Runnable task = new Runnable() {
        @Override
        public void run() {
            if (count > 1 && isAutoPlay) {
                currentItem = currentItem % (count + 1) + 1;
                if (currentItem == 1) {
                    viewPager.setCurrentItem(currentItem, false);
                    handler.post(task);
                } else {
                    viewPager.setCurrentItem(currentItem);
                    handler.postDelayed(task, delayTime);
                }
            }
        }
    };

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (isAutoPlay) {
            int action = ev.getAction();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL
                    || action == MotionEvent.ACTION_OUTSIDE) {
                startAutoPlay();
            } else if (action == MotionEvent.ACTION_DOWN) {
                stopAutoPlay();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    public void setImageLoader(ImageEngine imageLoader) {
        this.imageLoader = imageLoader;
    }

    public Banner setIndicatorGravity(int type) {
        switch (type) {
            case BannerConfig.LEFT:
                this.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
                break;
            case BannerConfig.CENTER:
                this.gravity = Gravity.CENTER;
                break;
            case BannerConfig.RIGHT:
                this.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
                break;
        }
        return this;
    }


    private void setImageList(List<?> imagesUrl) {
        if (imagesUrl == null || imagesUrl.size() <= 0) {
            loadingView.setVisibility(VISIBLE);
            return;
        }
        loadingView.setVisibility(GONE);
        initImages();
        for (int i = 0; i <= count + 1; i++) {
            View imageView = null;
            if (imageLoader != null) {
                imageView = imageLoader.createImageView(context);
            }
            if (imageView == null) {
                imageView = new ImageView(context);
            }
            imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, contentHeight == 0 ? ViewGroup.LayoutParams.MATCH_PARENT : contentHeight));
            setScaleType(imageView);
            Object url = null;
            if (i == 0) {
                url = imagesUrl.get(count - 1);
            } else if (i == count + 1) {
                url = imagesUrl.get(0);
            } else {
                url = imagesUrl.get(i - 1);
            }

            imageViews.add(imageView);
            if (imageLoader != null) {
                imageLoader.displayImage(context, url, imageView);
            } else {
                // log
            }

        }
    }

    private void initImages() {

        imageViews.clear();

        indicatorImages.clear();
        indicator.removeAllViews();

        for (int i = 0; i < count; i++) {
            ImageView imageView = new ImageView(context);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.leftMargin = indicatorMargin;
            params.rightMargin = indicatorMargin;
            if (i == 0) {
                imageView.setImageResource(indicatorSelectedResId);
            } else {
                imageView.setImageResource(indicatorUnselectedResId);
            }
            indicatorImages.add(imageView);

            indicator.addView(imageView, params);

        }
    }

    private void setScaleType(View imageView) {
        if (imageView instanceof ImageView) {
            ImageView view = ((ImageView) imageView);
            switch (scaleType) {
                case 0:
                    view.setScaleType(ImageView.ScaleType.CENTER);
                    break;
                case 1:
                    view.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    break;
                case 2:
                    view.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    break;
                case 3:
                    view.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    break;
                case 4:
                    view.setScaleType(ImageView.ScaleType.FIT_END);
                    break;
                case 5:
                    view.setScaleType(ImageView.ScaleType.FIT_START);
                    break;
                case 6:
                    view.setScaleType(ImageView.ScaleType.FIT_XY);
                    break;
                case 7:
                    view.setScaleType(ImageView.ScaleType.MATRIX);
                    break;
            }

        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if (mOnPageChangeListener != null) {
            mOnPageChangeListener.onPageScrolled(toRealPosition(position), positionOffset, positionOffsetPixels);
        }
    }

    @Override
    public void onPageSelected(int position) {
        currentItem = position;
        if (mOnPageChangeListener != null) {
            mOnPageChangeListener.onPageSelected(toRealPosition(position));
        }
        if (bannerStyle == BannerConfig.CIRCLE_INDICATOR || bannerStyle == BannerConfig.CIRCLE_INDICATOR_TITLE || bannerStyle == BannerConfig.CIRCLE_INDICATOR_TITLE_INSIDE) {
            if (count != 0) {
                indicatorImages.get((lastPosition - 1 + count) % count).setImageResource(indicatorUnselectedResId);
                indicatorImages.get((position - 1 + count) % count).setImageResource(indicatorSelectedResId);
                lastPosition = position;
            }
        }

    }

    @Override
    public void onPageScrollStateChanged(int state) {

        switch (state) {
            case 0:
                if (currentItem == 0) {
                    viewPager.setCurrentItem(count, false);
                } else if (currentItem == count + 1) {
                    viewPager.setCurrentItem(1, false);
                }
                break;
            case 1:
                if (currentItem == count + 1) {
                    viewPager.setCurrentItem(1, false);
                } else if (currentItem == 0) {
                    viewPager.setCurrentItem(count, false);
                }
                break;
            case 2:
                break;
        }
    }


    public int toRealPosition(int position) {
        int realPosition = 0;
        if (count != 0) {
            realPosition = (position - 1) % count;
            if (realPosition < 0)
                realPosition += count;
        }
        return realPosition;
    }

    @Override
    public void onClick(View v) {
        listener.onBannerLoadViewClick();
    }

    class BannerPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return imageViews.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            container.addView(imageViews.get(position));
            View view = imageViews.get(position);

            if (listener != null) {
                view.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onBannerClick(toRealPosition(position));
                    }
                });
            }
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

    }
}
