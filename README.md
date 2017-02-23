# YViewPagerDemo
YViewPager,support horizontal and vertical,based on `support.v4.view.ViewPager`

[中文版点这里](https://github.com/youngkaaa/YViewPagerDemo/blob/master/README_CH.md)

## Intro
The `YViewPager` is modified from the official `support.v4.view.ViewPager`,which support the direction of horizontal and vertical(new direction).You can use this lib just like the official `support.v4.view.ViewPager`,include the features of  `setPageTransformer()`、`addOnPageChangeListener()`、`setOffscreenPageLimit()` and so on,And also supported the `TabLayout`.


## Feature
On the base of the `support.v4.view.ViewPager`,add the property of `direction`. You can use it by：

### xml

```
<cn.youngkaaa.yviewpager.YViewPager
    android:id="@+id/viewpager"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:orientation="vertical"/>
```

just like the above xml.you can add the property `app:orientation` in `YViewPager` tag, the value of this property is `horizontal` and `vertical`.


### java code
```
mViewPager.setDirection(YViewPager.VERTICAL);

mViewPager.setDirection(YViewPager.HORIZONTAL);

```

you can change the direction by calling the `YViewPager.setDirection()`。

Tip：It's not recommended that change the direction when your application is running,which include a inflated `YViewPager`,because at this time the `YViewPager` may included the Fragments,which may has the deeply view hierarchy,and when you call the `setDriection()` may invaliate the layout,so may cause the application ANR or bad layout.but it's just not recommended,not unsupport!

## TODO

make this lib can swipe circulating!

## Finally

Because the `FragmentPagerAdapter` 、`FragmentStatePagerAdapter` and `PagerAdapter` has some methods are not br called in `YViewPager`，so I copy the code in this lib named `YFragmentPagerAdapter`、`YFragmentStatePagerAdapter` and `YPagerAdapter`，you can choose the `YxxxxxAdapter` in this lib which you needed!

The running screenshot：

![](https://github.com/youngkaaa/YViewPagerDemo/blob/master/screens/record.gif)

If you think this lib help you,you can give me a star to encourage me! thanks a lot

