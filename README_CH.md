# YViewPagerDemo
YViewPager,support horizontal and vertical,based on `support.v4.view.ViewPager`

## 使用

### Gradle

```
compile 'cn.youngkaaa:yviewpager:0.3'
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

## 简介
支持水平方向上滑动和竖直方向上的滑动，直接修改的官方提供的`ViewPager`的源码，注入了方向判断，使用起来和官方的`ViewPager`基本一样，还有监听事件也是一样的，支持`setOffscreenPageLimit()`可以放心滑动，以及`TabLayout`等。

## 特性

### 方向

在官方原有特性下，新增加`direction`属性。可以通过两种方式来设置：

#### xml

```
<cn.youngkaaa.yviewpager.YViewPager
    android:id="@+id/viewpager"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:orientation="vertical"/>
```

就像上面这样，只需要设置`app:orientation`属性，该属性可选值为:`vertical`和`horizontal`，分别代表竖直方向上滑动和水平方向上滑动。

#### java代码

```
mViewPager.setDirection(YViewPager.VERTICAL);

mViewPager.setDirection(YViewPager.HORIZONTAL);

```

具体意思同上
注意，设计的起始是想着在`YViewPager`一开始inflate时就应该确定了方向，如果在后期app运行时（即在`YViewPager`创建显示后）通过代码来修改方向的话，这种做法不推荐，因为`YViewPager`内部可能有很深View结构的`Fragment`，所以在此时修改`YViewPager`的`direction`的话，会通知刷新`YViewPager`，会造成顿时卡顿，所以不推荐此做法，但是不代表不可以这样做，如果您的需求确实是这样那么这样做也是可以的。

### 循环滚动

在`v1.2`版本开始增添了`circulatory`属性，你可以这样使用：

#### xml

```
<cn.youngkaaa.yviewpager.YViewPager
  android:id="@+id/viewpager1"
  android:layout_width="match_parent"
  android:layout_height="188dp"
  app:circulatory="true"
  app:orientation="horizontal"/>
```
是的，就只需要一句代码就可以了。

#### java

对不起，暂时不支持使用java代码动态修改`circulatory`属性。因为一般循环与否都是在xml中提前决定好的，所以就仅仅支持xml修改

## 版本

### v0.3

minSdkVersion调整到14

### v0.2

加入循环属性，同样支持水平和竖直两个方向上的循环。

### v0.1

支持水平和竖直方向上得滑动切换

## 最后

为什么我要在官方原有的`ViewPager`源码的基础上来修改，是因为官方在做`ViewPager`中做了很多优化和提升，所以我只修改了原有的源码的事件逻辑，并没有大的修改，所以官方的心血还是保留了的。但是这样带来的也有缺点，那就是官方的那些`FragmentPagerAdapter`和`FragmentStatePagerAdapter`以及`PagerAdapter`都不能直接调用了，因为这些类里面有一些方法不支持外部类调用，所以我拷贝了这三个类到本仓库中，代码和官方的是一样的，只是改了类名以保证可以在`YViewPager`中调用而已，所以你可以按照官方的方式来使用，你可以在你的项目中需要哪一个`XXXXAdapter`就拷贝该类到你的项目中去。

最后附上运行图：


>    v0.1

![](https://github.com/youngkaaa/YViewPagerDemo/blob/master/screens/record.gif)

>    v0.2

![](https://github.com/youngkaaa/YViewPagerDemo/blob/master/screens/record_circle.gif)

如果你觉得本仓库对你有用的话请给个star表示鼓励吧

具体的关于`ViewPager`的源码简单分析可以看我的博客

# License
```
Copyright 2016 youngkaaa

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

