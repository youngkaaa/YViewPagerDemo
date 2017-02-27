# YViewPagerDemo
YViewPager,support horizontal and vertical,based on `support.v4.view.ViewPager`

[中文版点这里](https://github.com/youngkaaa/YViewPagerDemo/blob/master/README_CH.md)

## Usage

### Gradle

```
compile 'cn.youngkaaa:yviewpager:0.2'
```

### Maven

```
<dependency>
  <groupId>cn.youngkaaa</groupId>
  <artifactId>yviewpager</artifactId>
  <version>0.2</version>
  <type>pom</type>
</dependency>
```

## Intro
The `YViewPager` is modified from the official `support.v4.view.ViewPager`,which support the direction of horizontal and vertical(new direction).You can use this lib just like the official `support.v4.view.ViewPager`,include the features of  `setPageTransformer()`、`addOnPageChangeListener()`、`setOffscreenPageLimit()` and so on,And also supported the `TabLayout`.


## Feature

### Direction

On the base of the `support.v4.view.ViewPager`,add the property of `direction`. You can use it by：

#### xml

```
<cn.youngkaaa.yviewpager.YViewPager
    android:id="@+id/viewpager"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:orientation="vertical"/>
```

just like the above xml.you can add the property `app:orientation` in `YViewPager` tag, the value of this property is `horizontal` and `vertical`.


#### java code
```
mViewPager.setDirection(YViewPager.VERTICAL);

mViewPager.setDirection(YViewPager.HORIZONTAL);

```

you can change the direction by calling the `YViewPager.setDirection()`。

Tip：It's not recommended that change the direction when your application is running,which include a inflated `YViewPager`,because at this time the `YViewPager` may included the Fragments,which may has the deeply view hierarchy,and when you call the `setDriection()` may invaliate the layout,so may cause the application ANR or bad layout.but it's just not recommended,not unsupport!

### Circulatory

in `v1.2`,add the property of `circulatory`.you can use it by:

#### xml

```
<cn.youngkaaa.yviewpager.YViewPager
  android:id="@+id/viewpager1"
  android:layout_width="match_parent"
  android:layout_height="188dp"
  app:circulatory="true"
  app:orientation="horizontal"/>
```

yeah,you just need to add one line.

#### java

sorry,the property of `circulatory` do not support to change by using java code.


## Version

### v0.2

add the `circulatory` property，which support the direction of horizontal and vertical  (`2017-2-26`)

### v0.1

support the direction of vertical and horizontal（`2017-2-22`）


## Finally

Because the `FragmentPagerAdapter` 、`FragmentStatePagerAdapter` and `PagerAdapter` has some methods are not br called in `YViewPager`，so I copy the code in this lib named `YFragmentPagerAdapter`、`YFragmentStatePagerAdapter` and `YPagerAdapter`，you can choose the `YxxxxxAdapter` in this lib which you needed!

The running screenshot：

>    v0.1

![](https://github.com/youngkaaa/YViewPagerDemo/blob/master/screens/record.gif)

>    v0.2

![](https://github.com/youngkaaa/YViewPagerDemo/blob/master/screens/record_circle.gif)


If you think this lib help you,you can give me a star to encourage me! thanks a lot

# License
```
Copyright 2017 youngkaaa

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

```



