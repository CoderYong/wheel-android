package dev.yong.wheel.base;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;

import org.simple.eventbus.EventBus;

import javax.inject.Inject;

import butterknife.ButterKnife;
import dagger.android.AndroidInjection;
import dagger.android.AndroidInjector;
import dagger.android.DaggerApplication;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import dagger.android.HasFragmentInjector;
import dagger.android.support.HasSupportFragmentInjector;
import dev.yong.wheel.AppManager;
import dev.yong.wheel.network.Network;
import dev.yong.wheel.network.NetworkReceiver;
import dev.yong.wheel.utils.Logger;
import dev.yong.swipeback.ISwipeBack;
import dev.yong.swipeback.SwipeBackHelper;
import dev.yong.swipeback.SwipeBackLayout;

/**
 * Activity基类
 *
 * @author CoderYong
 */
public abstract class BaseActivity extends AppCompatActivity implements NetworkReceiver.OnNetworkListener, ISwipeBack, HasActivityInjector, HasFragmentInjector, HasSupportFragmentInjector {

    /**
     * 网络状态广播
     * <p>
     * 可通过重写{@link #onNetworkChange(int)}接收到网络状态
     * </p>
     */
    protected NetworkReceiver mReceiver;
    /**
     * 滑动返回是否可用
     */
    protected boolean mSwipeBackEnable = true;
    /**
     * 滑动返回监听
     */
    protected SwipeBackLayout.SwipeListener mSwipeListener;

    @Inject
    DispatchingAndroidInjector<Activity> mActivityInjector;
    @Inject
    DispatchingAndroidInjector<Fragment> mSupportFragmentInjector;
    @Inject
    DispatchingAndroidInjector<android.app.Fragment> mFrameworkFragmentInjector;

    @Override
    protected final void onCreate(Bundle savedInstanceState) {
        if (isSinkingStatusBar()) {
            WindowManager.LayoutParams attrs = getWindow().getAttributes();
            attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
            getWindow().setAttributes(attrs);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
        if (isInject()) {
            try {
                //注入当前Activity
                AndroidInjection.inject(this);
            } catch (Exception e) {
                Logger.e(e, "https://google.github.io/dagger//android.html");
            }
        }
        super.onCreate(savedInstanceState);
        int layoutId = layoutId();
        if (layoutId == 0) {
            throw new Resources.NotFoundException("Not found layout resources, resources id: " + layoutId);
        }
        setContentView(layoutId);
        ButterKnife.bind(this);
        SwipeBackHelper.with(this)
                .alphaColor(alphaColor())
                .hasAlpha(hasAlpha())
                .hasShadow(hasShadow())
                .prevViewScrollable(prevViewScrollable())
                .swipeListener(mSwipeListener)
                .build();
        if (AppManager.getInstance().isUseEventBus()) {
            EventBus.getDefault().register(this);
        }
        init(savedInstanceState);
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mReceiver = NetworkReceiver.register(this, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        if (AppManager.getInstance().isUseEventBus()) {
            EventBus.getDefault().unregister(this);
        }
    }

    @Override
    public AndroidInjector<Activity> activityInjector() {
        return mActivityInjector;
    }

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return mSupportFragmentInjector;
    }

    @Override
    public AndroidInjector<android.app.Fragment> fragmentInjector() {
        return mFrameworkFragmentInjector;
    }

    /**
     * 返回视图资源id
     * <p>此方法会在 setContentView 之前被调用，可以制定设置view前的一些操作
     *
     * @return 返回视图资源id
     */
    protected abstract int layoutId();

    /**
     * 用于视图、数据、监听等一些初始化操作
     */
    protected void init() {
    }

    /**
     * 用于视图、数据、监听等一些初始化操作
     */
    protected void init(Bundle savedInstanceState) {
    }

    /**
     * 启动意图
     * <p>该方法适用于Intent中不携带数据跳转</p>
     *
     * @param clazz 启动action Activity
     */
    public void startActivity(Class<?> clazz) {
        startActivity(new Intent(this, clazz));
    }

    /**
     * 启动意图
     *
     * @param bundle 携带Bundle数据
     * @param clazz  启动action Activity
     */
    public void startActivity(Bundle bundle, Class<?> clazz) {
        Intent intent = new Intent(this, clazz);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    /**
     * 网络状态改变
     *
     * @param type 网络连接类型{@link Network#getNetworkType(Context)}
     */
    @Override
    public void onNetworkChange(int type) {
    }

    /**
     * 上一个页面视图
     * <p>此视图为实现滑动返回所使用</P>
     *
     * @return prevView
     */
    @Override
    public View prevView() {
        Activity activity = AppManager.getInstance().getPreActivity();
        if (activity != null) {
            return activity.getWindow().getDecorView();
        }
        return null;
    }

    /**
     * 是否沉侵状态栏
     *
     * @return true 全屏且沉侵，默认为false
     */
    public boolean isSinkingStatusBar() {
        return false;
    }

    /**
     * 背景透明度颜色值
     *
     * @return 默认为0x99000000
     */
    public int alphaColor() {
        return 0x99000000;
    }

    /**
     * 背景是否有透明度
     *
     * @return 默认为false
     */
    public boolean hasAlpha() {
        return false;
    }

    /**
     * 是否带有阴影
     *
     * @return 默认为true
     */
    public boolean hasShadow() {
        return true;
    }

    /**
     * 上一个视图是否可滚动
     *
     * @return 默认为true
     */
    public boolean prevViewScrollable() {
        return true;
    }

    public void setSwipeListener(SwipeBackLayout.SwipeListener listener) {
        mSwipeListener = listener;
    }

    /**
     * 是否支持滑动返回
     * <p>
     * 默认支持，如果当前Activity不希望被滑动返回，返回false即可
     * </P>
     *
     * @return true 支持，false不支持
     */
    @Override
    public boolean isSupportSwipeBack() {
        return mSwipeBackEnable;
    }

    /**
     * 是否将当前Activity注入
     * <p>
     * 默认被注入，如果你不想注入当前Activity，返回false即可
     * </P>
     *
     * @return true 注入，false不注入
     */
    protected boolean isInject() {
        return getApplication() instanceof DaggerApplication;
    }
}