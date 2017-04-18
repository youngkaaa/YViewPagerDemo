package cn.youngkaaa.yviewpagerdemo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import cn.youngkaaa.yviewpager.YViewPager;
import q.rorbin.verticaltablayout.VerticalTabLayout;
import q.rorbin.verticaltablayout.adapter.TabAdapter;
import q.rorbin.verticaltablayout.widget.TabView;

public class VerticalActivity extends AppCompatActivity {

    private List<FragmentInner> mFragmentInners;
    private VerticalTabLayout mTabLayout;
    private YViewPager mViewPager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vertical);
        mViewPager= (YViewPager) findViewById(R.id.yviewpager);
        mTabLayout= (VerticalTabLayout) findViewById(R.id.tablayout);
        initData();
        initTabs();
        mViewPager.setAdapter(new FragmentAdapter(getSupportFragmentManager()));
        mTabLayout.setupWithViewPager(mViewPager);
    }

    private void initTabs() {
        mTabLayout.setTabAdapter(new TabAdapter() {
            @Override
            public int getCount() {
                return mFragmentInners.size();
            }

            @Override
            public TabView.TabBadge getBadge(int position) {
                return null;
            }

            @Override
            public TabView.TabIcon getIcon(int position) {
                return null;
            }

            @Override
            public TabView.TabTitle getTitle(int position) {
                TabView.TabTitle title=new TabView.TabTitle.Builder()
                        .setContent("y"+position)
                        .setTextColor(R.color.colorAccent,R.color.colorPrimary)
                        .build();
                return title;
            }

            @Override
            public int getBackground(int position) {
                return 0;
            }
        });
    }

    private void initData() {
        mFragmentInners=new ArrayList<>();
        FragmentInner fragmentInner1=FragmentInner.newInstance("fragment1",R.drawable.jay_jay);
        FragmentInner fragmentInner2=FragmentInner.newInstance("fragment2",R.drawable.jay_fantexi);
        FragmentInner fragmentInner3=FragmentInner.newInstance("fragment3",R.drawable.image2);
        FragmentInner fragmentInner4=FragmentInner.newInstance("fragment4",R.drawable.logo);
        FragmentInner fragmentInner5=FragmentInner.newInstance("fragment5",R.drawable.jay_jay);
        FragmentInner fragmentInner6=FragmentInner.newInstance("fragment6",R.drawable.image2);
        mFragmentInners.add(fragmentInner1);
        mFragmentInners.add(fragmentInner2);
        mFragmentInners.add(fragmentInner3);
        mFragmentInners.add(fragmentInner4);
        mFragmentInners.add(fragmentInner5);
        mFragmentInners.add(fragmentInner6);
    }

    class FragmentAdapter extends FragmentPagerAdapter {

        public FragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentInners.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentInners.size();
        }


    }
}
