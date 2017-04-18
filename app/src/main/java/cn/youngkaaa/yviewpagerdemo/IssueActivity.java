package cn.youngkaaa.yviewpagerdemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.util.ArrayList;

import cn.youngkaaa.yviewpager.YViewPager;


/**
 * Created by : youngkaaa on 2017/3/29.
 * Contact me : 645326280@qq.com
 */

public class IssueActivity extends AppCompatActivity{
    private YViewPager mYViewPager;
    private ArrayList<Fragment> mFragments;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.issue_layout);
        mYViewPager= (YViewPager) findViewById(R.id.viewPagerIssue);

        initData();

        mYViewPager.setAdapter(new FragmentAdapter(getSupportFragmentManager()));

        mYViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                Toast.makeText(IssueActivity.this, ""+position, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void initData() {
        mFragments = new ArrayList<>();
        FragmentIssueInnerOther fragmentInner1 = FragmentIssueInnerOther.newInstance("Fragment1");
        FragmentIssueInner fragmentInner2 = new FragmentIssueInner();
        FragmentIssueInnerOther fragmentInner3 = FragmentIssueInnerOther.newInstance("Fragment3");
        mFragments.add(fragmentInner1);
        mFragments.add(fragmentInner2);
        mFragments.add(fragmentInner3);
    }

    class FragmentAdapter extends FragmentPagerAdapter {

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
