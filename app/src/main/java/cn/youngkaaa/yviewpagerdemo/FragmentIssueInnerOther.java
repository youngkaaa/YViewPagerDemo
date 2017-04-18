package cn.youngkaaa.yviewpagerdemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by : youngkaaa on 2017/2/22.
 * Contact me : 645326280@qq.com
 */

public class FragmentIssueInnerOther extends Fragment {
    public static final String KEY="FragmentIssueInnerOther_TITLE";
    private TextView mTextView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_issue_inner_other, container, false);
        mTextView= (TextView) rootView.findViewById(R.id.tvIssueFragmentOther);
        return rootView;
    }

    public static FragmentIssueInnerOther newInstance(String title){
        FragmentIssueInnerOther fragmentIssueInnerOther=new FragmentIssueInnerOther();
        Bundle bundle=new Bundle();
        bundle.putString(KEY,title);
        fragmentIssueInnerOther.setArguments(bundle);
        return fragmentIssueInnerOther;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String title = getArguments().getString(KEY);
        mTextView.setText(title);
    }

}
