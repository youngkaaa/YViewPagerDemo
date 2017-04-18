package cn.youngkaaa.yviewpagerdemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * Created by : youngkaaa on 2017/2/22.
 * Contact me : 645326280@qq.com
 */

public class FragmentIssueInner extends Fragment {
    private YViewPagerNew mYViewPager;
    private ArrayList<Fragment> mFragments;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_issue_inner, container, false);
        mYViewPager = (YViewPagerNew) rootView.findViewById(R.id.viewpagerIssueInner);
        return rootView;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initData();
        mYViewPager.setAdapter(new FragmentAdapter(getChildFragmentManager()));
    }

    private void initData() {
        mFragments = new ArrayList<>();
        FragmentInner fragmentInner1 = FragmentInner.newInstance("fragment1", R.drawable.jay_jay);
        FragmentInner fragmentInner2 = FragmentInner.newInstance("fragment2", R.drawable.jay_fantexi);
        FragmentInner fragmentInner3 = FragmentInner.newInstance("fragment3", R.drawable.image2);
        FragmentInner fragmentInner4 = FragmentInner.newInstance("fragment4", R.drawable.logo);
        FragmentInner fragmentInner5 = FragmentInner.newInstance("fragment5", R.drawable.jay_jay);
        FragmentInner fragmentInner6 = FragmentInner.newInstance("fragment6", R.drawable.image2);
        mFragments.add(fragmentInner1);
        mFragments.add(fragmentInner2);
        mFragments.add(fragmentInner3);
        mFragments.add(fragmentInner4);
        mFragments.add(fragmentInner5);
        mFragments.add(fragmentInner6);
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
