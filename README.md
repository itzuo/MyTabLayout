## 一、简述：
最近项目中有个需求，效果如下图：我的做法是使用系统的TabLayout来实现，但是系统的TabLayout的下划线的长度是每个Item的长度，太长了，而且UI还要求下划线的是圆角，于是我就想办法解决这个问题。
![效果图](https://upload-images.jianshu.io/upload_images/2918620-c75ef86e1995ecb0.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

## 二、 使用网上普遍的方法，反射TabLayout
### 1、写个工具类 封装：
```
public class TabLayoutUtils {

    public static void setIndicator(TabLayout tabs, int leftDip, int rightDip, int bottomDip) {
        Class<?> tabLayout = tabs.getClass();
        Field tabStrip = null;
        try {
            tabStrip = tabLayout.getDeclaredField("mTabStrip");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return;
        }
        tabStrip.setAccessible(true);
        LinearLayout llTab = null;
        try {
            llTab = (LinearLayout) tabStrip.get(tabs);
        } catch (IllegalAccessException e) {
            e.printStackTrace();

            return;
        }

        int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, leftDip, Resources.getSystem().getDisplayMetrics());
        int right = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, rightDip, Resources.getSystem().getDisplayMetrics());
        int bottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, bottomDip, Resources.getSystem().getDisplayMetrics());

        for (int i = 0; i < llTab.getChildCount(); i++) {
            View child = llTab.getChildAt(i);
            child.setPadding(0, 0, 0, 0);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
            params.leftMargin = left;
            params.rightMargin = right;
            params.bottomMargin = bottom;

            child.setLayoutParams(params);
            child.invalidate();
        }
    }
}
```
### 2、在Activity调用
```
tab_view.post(new Runnable() {
    @Override
    public void run() {
        TabUtils.setIndicator(tab_view,10,10);
    }
});
```
使用这种方式，是可以把下划线变短，但是它也会缩短可点击区域，而且下划线还不是圆角的。

因此，我使用了下面的方法来解决这个问题：修改系统的TabLayout

## 三 、查看系统的TabLayout
修改系统的TabLayout前，我们还必须看看TabLayout源码。

tab的创建是TabLayout.addTabI();方法添加到TabLayout的：
```
TabLayout.Tab tab = mTabLayout.newTab().setCustomView(view);
mTabLayout.addTab(tab, true);
```
然后直接查看这个方法，这里的addTab是方法重载，最终调用的是下面的代码：

![addTab方法](https://upload-images.jianshu.io/upload_images/2918620-9f895052eab76a66.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

这里面调用的是`addTabView(tab);`方法,继续看这个方法：

![addTabView方法](https://upload-images.jianshu.io/upload_images/2918620-15f2f5834b6c76c7.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

可以看到最后添加到mTabStrip中，我们先来看看TabView里面有什么东西

![TabView类](https://upload-images.jianshu.io/upload_images/2918620-5f16b69f65b61053.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

其实从属性中就可以看出TabView是可以自定义的，但是在这里并没有看见Indicator（下划线）的痕迹，于是我们在看看mTabStrip.

![SlidingTabStrip类](https://upload-images.jianshu.io/upload_images/2918620-75f78a7ee9ddfec7.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

我们可以发现mSelectedIndicatorHeight属性，和setSelectedIndicatorHeight方法，这是设置下划线的高度，继续追踪mSelectedIndicatorHeight
![draw(Canvas canvas)](https://upload-images.jianshu.io/upload_images/2918620-09bad7ae000b901d.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

原来是在draw方法中进行绘画出来的，其中根据mIndicatorLeft和mIndicatorRight来决定下划线的长短，我们继续看看这两个属性的来源。

![](https://upload-images.jianshu.io/upload_images/2918620-7cc8cdd59ebc45bd.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

从上面setIndicatorPosition方法中给mIndicatorLeft和mIndicatorRight属性赋值的，而mIndicatorLeft和mIndicatorRight方法又是在updateIndicatorPosition方法中调用的，所以分析updateIndicatorPosition方法，可以知道selectedTitle就是TabView，让后直接获取了它的左边坐标和右边坐标，也就是说线的宽度是TabView的宽度。

到这里还有个疑问，就是当我们设置tabView为2个字和5个字的时候，为啥tabVIew的宽度一样呢，这时，我们先看看SlidingTabStrip的onMeasure方法

![](https://upload-images.jianshu.io/upload_images/2918620-dae1b0fc98301f2f.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

看到没，看到没，取最大值，然后记录这个最大值，最后赋值给tabView的宽度，原因就在这里的了。

分析了这么久的源码，也是对源码有了大概的认识，然后我们想实现UI的效果，那就修改TabLayout的源码了，下面就是修改源码的步骤

## 四、修改系统的TabLayout
### 1、拷贝类
首先打开系统TabLayout源码，查看所在的包
![UZW`BO{ZJ][~2DWST`A1P12.png](https://upload-images.jianshu.io/upload_images/2918620-8c85796f9819e974.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

打开下图的路径，找到TabLayout源码，(TabLayout所依赖的类其实都在这里面了)
![](https://upload-images.jianshu.io/upload_images/2918620-3e8b746ff1bafa78.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

拷贝TabLayout类到自己所建的包中，这时类中所引用的类因为是包权限关系会找不到，看看那个找不到，就在拷贝那个类。

最终拷贝的类有如下图：
![](https://upload-images.jianshu.io/upload_images/2918620-2448e73f0ccd7190.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

到此，就可以像普通的自定义view一样使用自定义的TabLayout了。

### 2、自定义属性

TabLayout 先保留系统的属性（将系统定义的属性复制出来），也可添加自定义属性。

下图为系统属性

![](https://upload-images.jianshu.io/upload_images/2918620-72dd294e43ba0f40.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

下图为添加了自定义属性之后,(先增加一个属性tabLineOffset，用于控制下划线的长度)

![](https://upload-images.jianshu.io/upload_images/2918620-c324b92a6ba17d42.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

### 3、自定义属性的实现
1、在TabLayout的构造方法中加入自己的自定义属性

![](https://upload-images.jianshu.io/upload_images/2918620-0736d2cae13eaa46.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

2、修改SlidingTabStrip类的draw方法
```
@Override
public void draw(Canvas canvas) {
    super.draw(canvas);
    // Thick colored underline below the current selection
    if (mIndicatorLeft >= 0 && mIndicatorRight > mIndicatorLeft) {
        if(mTabLineOffset == 0){
            canvas.drawRect(mIndicatorLeft, getHeight() - mSelectedIndicatorHeight,
                mIndicatorRight, getHeight(), mSelectedIndicatorPaint);
        }else {
            //原来的下划线的长度(也就是Tab的宽度)
            int width = mIndicatorRight - mIndicatorLeft;
            //Tab的中心点的坐标（mIndicatorRight-width/2也是）
            int tabCenter = mIndicatorLeft+width/2;
            RectF oval3 = new RectF(tabCenter-mTabLineOffset, getHeight() - mSelectedIndicatorHeight,
                    mIndicatorRight-width/2+mTabLineOffset, getHeight());
            canvas.drawRoundRect(oval3,30,30,mSelectedIndicatorPaint);
        }
    }
}
```
这里我进行了判断，如果在xml中没有使用自定义属性mTabLineOffset ，就还是展示的是系统TabLayout的样式，否者，就是我们可控制的下划线长度。

这里我们先取了Tab的宽度，算出Tab的中心坐标位置，让后根据中心位置坐标和mTabLineOffset 值来确定下划线的长度。

到此，任务完成了，
### 五、使用
1、MainActivity.java
```
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TabLayout mTabLayout = findViewById(R.id.tab_layout);
        for (int i = 0; i < 3; i++) {
            View view = LayoutInflater.from(this).inflate(R.layout.item_skill_tab, null, false);
            TextView tvName = view.findViewById(R.id.tv_name);
            tvName.setText("城市导游" + i);
            TabLayout.Tab tab = mTabLayout.newTab().setCustomView(view);
            //设置第一个默认选中Tab
            if (i == 0) {
                mTabLayout.addTab(tab, true);
            } else {
                mTabLayout.addTab(tab);
            }
        }
    }
}
```

2、activity_main.xml
```
<android.support.constraint.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.zxj.mytablayout.tablayout.TabLayout
        android:id="@+id/tab_layout"
        android:layout_width="match_parent"
        android:layout_height="112dp"
        android:text="Hello World!"
        app:tabGravity="fill"
        app:tabIndicatorHeight="5dp"
        app:tabIndicatorColor="#fd676f"
        app:tabLineOffset="20dp"
        app:tabMode="fixed"
        app:tabSelectedTextColor="#fd676f"
        app:tabTextColor="#fd676f" />

</android.support.constraint.ConstraintLayout>
```
3、最终效果如图

![](https://upload-images.jianshu.io/upload_images/2918620-9fff40a48d82524b.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

当然，这只是一个小小的改动，如有更复杂的需求，可以进一步的修改，反正限制源码都是你的了，你想怎么改就怎么改！

以上如有不对的地方，请多多指教!
