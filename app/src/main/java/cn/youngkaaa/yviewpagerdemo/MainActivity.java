package cn.youngkaaa.yviewpagerdemo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.youngkaaa.yviewpager.YFragmentPagerAdapter;
import cn.youngkaaa.yviewpager.YViewPager;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private YViewPager mViewPager;
    private List<ImageView> mImageViews;
    private LinearLayout lTopCircleIndicator;
    private int mPosition;
    private boolean isEdgeSwipe;
    private int mLastPos;
    private List<FragmentInner> mFragments;
    private TextView mTextView;
    private int mCurrentItem=0;
    private Button mButtonHorizontal;
    private Button mButtonVertical;
    private int mCurrentPos=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mViewPager = (YViewPager) findViewById(R.id.viewpager);
        mTextView= (TextView) findViewById(R.id.numIndicator);
        mButtonHorizontal= (Button) findViewById(R.id.btnHorizontal);
        mButtonVertical= (Button) findViewById(R.id.btnVertical);
        initData();

        mViewPager.setAdapter(new FragmentAdapter(getSupportFragmentManager()));

        mViewPager.setPageMargin(10);
        mViewPager.addOnPageChangeListener(new YViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                Log.d(TAG, "onPageScrolled() position=>" + position + ",positionOffset=>" + positionOffset +
                        ",positionOffsetPixels=>" + positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                Log.d(TAG, "onPageSelected() position=>" + position);
                setIndicators(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                Log.d(TAG, "onPageScrollStateChanged() state=>"+state);
            }
        });

        mButtonHorizontal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mViewPager.setDirection(YViewPager.HORIZONTAL);
            }
        });
        mButtonVertical.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mViewPager.setDirection(YViewPager.VERTICAL);
            }
        });
    }

    private void initData() {
        mFragments=new ArrayList<>();
        FragmentInner fragmentInner1=FragmentInner.newInstance("fragment1",R.drawable.jay_jay);
        FragmentInner fragmentInner2=FragmentInner.newInstance("fragment2",R.drawable.jay_fantexi);
        FragmentInner fragmentInner3=FragmentInner.newInstance("fragment3",R.drawable.image2);
        FragmentInner fragmentInner4=FragmentInner.newInstance("fragment4",R.drawable.logo);
        FragmentInner fragmentInner5=FragmentInner.newInstance("fragment5",R.drawable.jay_jay);
        FragmentInner fragmentInner6=FragmentInner.newInstance("fragment6",R.drawable.image2);
        mFragments.add(fragmentInner1);
        mFragments.add(fragmentInner2);
        mFragments.add(fragmentInner3);
        mFragments.add(fragmentInner4);
        mFragments.add(fragmentInner5);
        mFragments.add(fragmentInner6);
        setIndicators(mCurrentPos);
    }

    private void setIndicators(int position) {
        mTextView.setText(position+1+"/"+mFragments.size());
    }

    private void initImages() {
        mImageViews = new ArrayList<>();

        ViewPager.LayoutParams lp=new ViewPager.LayoutParams();
        lp.gravity= Gravity.LEFT;

        ImageView imageView1 = new ImageView(this);
        imageView1.setImageResource(R.drawable.jay_fantexi);
        imageView1.setScaleType(ImageView.ScaleType.CENTER_CROP);

        ImageView imageView2 = new ImageView(this);
        imageView2.setImageResource(R.drawable.jay_jay);
        imageView2.setScaleType(ImageView.ScaleType.CENTER_CROP);

        ImageView imageView3 = new ImageView(this);
        imageView3.setImageResource(R.drawable.image2);
        imageView3.setScaleType(ImageView.ScaleType.CENTER_CROP);

        ImageView imageView4 = new ImageView(this);
        imageView4.setImageResource(R.drawable.jay_jay);
        imageView4.setScaleType(ImageView.ScaleType.CENTER_CROP);

        ImageView imageView5 = new ImageView(this);
        imageView5.setImageResource(R.drawable.logo);
        imageView5.setScaleType(ImageView.ScaleType.CENTER_CROP);

        imageView1.setLayoutParams(lp);
        imageView2.setLayoutParams(lp);
        imageView3.setLayoutParams(lp);
        imageView4.setLayoutParams(lp);
        imageView5.setLayoutParams(lp);

        mImageViews.add(imageView1);
        mImageViews.add(imageView2);
        mImageViews.add(imageView3);
        mImageViews.add(imageView4);
        mImageViews.add(imageView5);
    }

    class FragmentAdapter extends YFragmentPagerAdapter {

        public FragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }


    }
}
