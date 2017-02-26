package cn.youngkaaa.yviewpagerdemo;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import cn.youngkaaa.yviewpager.YFragmentPagerAdapter;
import cn.youngkaaa.yviewpager.YViewPager;

public class GallaryViewPager extends AppCompatActivity {
    private YViewPager mViewPager;
    private List<FragmentInner> mFragments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallary_view_pager);
        mViewPager= (YViewPager) findViewById(R.id.viewPagerGallary);
        initData();

        mViewPager.setAdapter(new FragmentAdapter(getSupportFragmentManager()));
        mViewPager.setPageMargin(18);
        mViewPager.setOffscreenPageLimit(2);
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
