package cn.youngkaaa.yviewpagerdemo;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by : youngkaaa on 2017/2/27.
 * Contact me : 645326280@qq.com
 */

public class TaobaoAdDemo extends AppCompatActivity {
    private YViewPagerNew mViewPager;
    private List<TextView> mTextViewList;
    private List<String> mAdStringList;
    private int mCurrentPos=0;
    private boolean isPrev=true;
    private List<LinearLayout> mLinearLayouts;
    private Timer mTimer;
    private Button mBtnPrev;
    private Button mBtnAfter;

    private Handler mHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what==-1){
                mViewPager.setCurrentItem(mCurrentPos);
            }
        }
    };

    class ScrollTask extends TimerTask{
        @Override
        public void run() {
            if(isPrev){
                int newPos=mCurrentPos==0?mAdStringList.size()/2-1:mCurrentPos-1;
                mCurrentPos=newPos;
                mHandler.sendEmptyMessage(-1);
            }else{
                int newPos=mCurrentPos==mAdStringList.size()/2-1?0:mCurrentPos+1;
                mCurrentPos=newPos;
                mHandler.sendEmptyMessage(-1);
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.taobao_ad_activity);

        mViewPager = (YViewPagerNew) findViewById(R.id.viewpagerTaobaoAd);
        initAds();
        mViewPager.setAdapter(new VerticalAdAdapter());
        mTimer=new Timer();
        mTimer.schedule(new ScrollTask(),1000,1000);


    }

    private void initAds() {
        mTextViewList=new ArrayList<>();
        mAdStringList=new ArrayList<>();
        mAdStringList.add("iPhone8售价曝光:6900元起");
        mAdStringList.add("youngkaaa广告测试1");
        mAdStringList.add("160、170、180男生开春怎么穿？");
        mAdStringList.add("youngkaaa广告测试2");
        mAdStringList.add("小米Meri真机曝光：屏占比、自主...");
        mAdStringList.add("youngkaaa广告测试3");

    }

    class VerticalAdAdapter extends PagerAdapter {

        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            LinearLayout linearLayout=new LinearLayout(TaobaoAdDemo.this);
            YViewPagerNew.LayoutParams lp=new YViewPagerNew.LayoutParams();
            lp.width= YViewPagerNew.LayoutParams.MATCH_PARENT;
            lp.height=200;
            lp.gravity= Gravity.CENTER;
            linearLayout.setLayoutParams(lp);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.setBackgroundColor(getResources().getColor(R.color.colorAccent));

            LinearLayout.LayoutParams lp1=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            lp1.weight=1;
            TextView textView1=new TextView(TaobaoAdDemo.this);
            textView1.setLayoutParams(lp1);
            textView1.setText(mAdStringList.get(position*2));
            textView1.setGravity(Gravity.CENTER);
            Drawable drawable= getResources().getDrawable(R.drawable.hot);
            drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
            textView1.setCompoundDrawables(drawable, null,null,null);
            textView1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(TaobaoAdDemo.this, ""+mAdStringList.get(position*2), Toast.LENGTH_SHORT).show();
                }
            });

            TextView textView2=new TextView(TaobaoAdDemo.this);
            textView2.setLayoutParams(lp1);
            textView2.setText(mAdStringList.get(position*2+1));
            textView2.setCompoundDrawables(drawable, null,null,null);
            textView2.setGravity(Gravity.CENTER_HORIZONTAL);
            textView2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(TaobaoAdDemo.this, ""+mAdStringList.get(position*2+1), Toast.LENGTH_SHORT).show();
                }
            });

            linearLayout.addView(textView1);
            linearLayout.addView(textView2);

            container.addView(linearLayout);

            return linearLayout;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return mAdStringList.size()/2;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view==object;
        }
    }
}
