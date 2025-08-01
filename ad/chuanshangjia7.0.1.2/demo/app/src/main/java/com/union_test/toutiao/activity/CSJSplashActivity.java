package com.union_test.toutiao.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.CSJAdError;
import com.bytedance.sdk.openadsdk.CSJSplashCloseType;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTAppDownloadListener;
import com.bytedance.sdk.openadsdk.CSJSplashAd;
import com.union_test.toutiao.R;
import com.union_test.toutiao.config.TTAdManagerHolder;
import com.union_test.toutiao.utils.SplashCardManager;
import com.union_test.toutiao.utils.TToast;
import com.union_test.toutiao.utils.UIUtils;
import com.union_test.toutiao.view.CountdownView;

import java.lang.ref.WeakReference;

/**
 * 开屏广告Activity示例
 */
public class CSJSplashActivity extends Activity {
    private static final String TAG = "SplashActivity";

    private TTAdNative mTTAdNative;
    private FrameLayout mSplashContainer;
    //是否强制跳转到主页面
    private boolean mForceGoMain;

    //开屏广告加载超时时间,建议大于3000,这里为了冷启动第一次加载到广告并且展示,示例设置了3000ms
    private static final int AD_TIME_OUT = 3000;
    private String mCodeId = "801121648";
    private boolean mIsExpress = false; //是否请求模板广告
    private boolean mIsHalfSize = false;//是否是半全屏开屏

    private LinearLayout mSplashHalfSizeLayout;
    private FrameLayout mSplashSplashContainer;

    private CSJSplashAd mSplashAd;

    @SuppressWarnings("RedundantCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);
        mSplashContainer = (FrameLayout) findViewById(R.id.splash_container);
        mSplashHalfSizeLayout = (LinearLayout) findViewById(R.id.splash_half_size_layout);
        mSplashSplashContainer = (FrameLayout) findViewById(R.id.splash_container_half_size);
        //step2:创建TTAdNative对象

        mTTAdNative = TTAdManagerHolder.get().createAdNative(this);
        getExtraInfo();
        //在合适的时机申请权限，如read_phone_state,防止获取不了imei时候，下载类广告没有填充的问题
        //在开屏时候申请不太合适，因为该页面倒计时结束或者请求超时会跳转，在该页面申请权限，体验不好
        // TTAdManagerHolder.getInstance(this).requestPermissionIfNecessary(this);
        //加载开屏广告
        loadSplashAd();
    }

    private void getExtraInfo() {
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }
        String codeId = intent.getStringExtra("splash_rit");
        if (!TextUtils.isEmpty(codeId)) {
            mCodeId = codeId;
        }
        mIsExpress = intent.getBooleanExtra("is_express", false);
        mIsHalfSize = intent.getBooleanExtra("is_half_size", false);
    }

    @Override
    protected void onResume() {
        //判断是否该跳转到主页面
        if (mForceGoMain) {
            goToMainActivity();
        }
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mForceGoMain = true;
    }

    /**
     * 加载开屏广告
     */
    private void loadSplashAd() {
        //step3:创建开屏广告请求参数AdSlot,具体参数含义参考文档
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        int statusBarHeightPx = (int) UIUtils.getStatusBarHeight(this);
        float splashWidthDp = UIUtils.getScreenWidthDp(this);
        int splashWidthPx = UIUtils.getScreenWidthInPx(this);
        int screenHeightPx = UIUtils.getScreenHeight(this) + statusBarHeightPx;
        float screenHeightDp = UIUtils.px2dip(this, screenHeightPx);
        float splashHeightDp;
        int splashHeightPx;


        if (mIsHalfSize) {
            // 开屏高度 = 屏幕高度 - 下方预留的高度，demo中是预留了屏幕高度的1/5，因此开屏高度传入 屏幕高度*4/5
            splashHeightDp = screenHeightDp * 4/5.f;
            splashHeightPx = (int) (screenHeightPx * 4/5.f);
        } else {
            splashHeightDp = screenHeightDp;
            splashHeightPx = screenHeightPx;
        }


        AdSlot adSlot = new AdSlot.Builder()
                .setCodeId(mCodeId)
                //模板广告需要设置期望个性化模板广告的大小,单位dp,代码位是否属于个性化模板广告，请在穿山甲平台查看
                //view宽高等于图片的宽高
                .setExpressViewAcceptedSize(splashWidthDp, splashHeightDp) // 单位是dp
                .setImageAcceptedSize(splashWidthPx, splashHeightPx) // 单位是px
                .build();
        if (mTTAdNative == null) return;
        //step4:请求广告，调用开屏广告异步请求接口，对请求回调的广告作渲染处理

        final CSJSplashAd.SplashAdListener listener = new SplashAdListener(this);

        mTTAdNative.loadSplashAd(adSlot, new TTAdNative.CSJSplashAdListener() {

            @Override
            public void onSplashLoadSuccess(CSJSplashAd ad) {
                Log.e(TAG, "CSJActivity-onSplashLoadSuccess");
                if (ad == null) {
                    return;
                }
                mSplashAd = ad;
                if (mIsHalfSize) {
                    mSplashHalfSizeLayout.setVisibility(View.VISIBLE);
                    mSplashAd.showSplashView(mSplashSplashContainer);
                    mSplashContainer.setVisibility(View.GONE);
                } else {
                    mSplashAd.showSplashView(mSplashContainer);
                    mSplashHalfSizeLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onSplashLoadFail(CSJAdError error) {
                Log.e(TAG, "CSJActivity-onSplashLoadFail, errorCode: " + error.getCode() + ", errorMsg: " + error.getMsg());
                showToast(error.getMsg());
                goToMainActivity();
            }

            @Override
            public void onSplashRenderSuccess(CSJSplashAd ad) {
                Log.e(TAG, "CSJActivity-onSplashRenderSuccess");
                //设置SplashView的交互监听器
                mSplashAd.setSplashAdListener(listener);

                if (ad.getInteractionType() == TTAdConstant.INTERACTION_TYPE_DOWNLOAD) {
                    TTAppDownloadListener downloadListener = new SplashDownloadListener();
                    mSplashAd.setDownloadListener(downloadListener);
                }
                //初始化卡片开屏相关数据
                SplashCardManager.getInstance().init(CSJSplashActivity.this, mSplashAd, mSplashAd.getSplashView(),  new SplashCardManager.Callback() {
                    @Override
                    public void onStart() {
                        // 当动画开始时回调，您可以在此处理渲染卡片背后的界面等操作
                    }

                    @Override
                    public void onClose() {
                        // 当卡片关闭时回调，您可以在这里处理Activity的关闭操作等
                        Intent intent = new Intent(CSJSplashActivity.this, MainActivity.class);
                        startActivity(intent);
                        if (mSplashContainer != null) {
                            mSplashContainer.removeAllViews();
                        }
                        finish();
                    }
                });

            }

            @Override
            public void onSplashRenderFail(CSJSplashAd ad, CSJAdError csjAdError) {
                Log.e(TAG, "CSJActivity-onSplashRenderFail, errorCode: " + csjAdError.getCode() + ", errorMsg: " + csjAdError.getMsg());
                showToast(csjAdError.getMsg());
                goToMainActivity();
            }
        }, AD_TIME_OUT);

    }
    private void showToast(String msg) {
        TToast.show(this, msg);
    }

    public static class SplashDownloadListener implements TTAppDownloadListener{
        private boolean hasShow = false;
        @Override
        public void onIdle() {

        }

        @Override
        public void onDownloadActive(long totalBytes, long currBytes, String fileName, String appName) {
            if (!hasShow) {
                Log.d(TAG, "下载中...");
                hasShow = true;
            }
        }

        @Override
        public void onDownloadPaused(long totalBytes, long currBytes, String fileName, String appName) {
            Log.d(TAG, "下载暂停...");
        }

        @Override
        public void onDownloadFailed(long totalBytes, long currBytes, String fileName, String appName) {
            Log.d(TAG, "下载失败...");
        }

        @Override
        public void onDownloadFinished(long totalBytes, String fileName, String appName) {
            Log.d(TAG, "下载完成...");
        }

        @Override
        public void onInstalled(String fileName, String appName) {
            Log.d(TAG, "安装完成...");
        }
    }

    public static class SplashAdListener implements CSJSplashAd.SplashAdListener {

        public WeakReference<Activity> mContextRef;
        public SplashAdListener(Activity activity) {
            mContextRef = new WeakReference<>(activity);
        }

        private void showToast(Context context, String msg) {
            if (context == null || TextUtils.isEmpty(msg)) {
                return;
            }
            TToast.show(context, msg);
        }

        private void goToMainActivity() {
            if (mContextRef.get() == null) {
                return;
            }
            if (SplashCardManager.getInstance().canShowInnerActivityCard()) {
                return;
            }
            Intent intent = new Intent(mContextRef.get(), MainActivity.class);
            mContextRef.get().startActivity(intent);
            mContextRef.get().finish();
        }

        @Override
        public void onSplashAdShow(CSJSplashAd ad) {
            Log.d(TAG, "onAdShow");
            showToast(mContextRef.get(),"开屏广告展示");
        }

        @Override
        public void onSplashAdClick(CSJSplashAd ad) {
            Log.d(TAG, "onAdClicked");
            showToast(mContextRef.get(),"开屏广告点击");
        }

        @Override
        public void onSplashAdClose(CSJSplashAd ad, int closeType) {
            Log.d(TAG, "onSplashAdClose closeType: " + closeType);
            if (closeType == CSJSplashCloseType.CLICK_SKIP) {
                showToast(mContextRef.get(),"开屏广告点击跳过 ");
            } else if (closeType == CSJSplashCloseType.COUNT_DOWN_OVER) {
                showToast(mContextRef.get(),"开屏广告点击倒计时结束");
            } else if (closeType == CSJSplashCloseType.CLICK_JUMP) {
                showToast(mContextRef.get(),"点击跳转");
            }
            goToMainActivity();
        }
    }

    /**
     * 设置是否禁用SDK的开屏广告倒计时，如果设置为true，您需要自定义倒计时逻辑，本方法为参考样例
     * @param isUseCustomCountdownButton 是否使用自定义按钮
     * @param ad 获取到的开屏广告
     */

    private void useCustomCountdownButton(boolean isUseCustomCountdownButton, final CSJSplashAd ad) {
        if (isUseCustomCountdownButton) {
            ad.hideSkipButton();
            CountdownView countdownView = new CountdownView(CSJSplashActivity.this);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.END | Gravity.TOP;
            params.topMargin = 20;
            params.rightMargin = 20;
            countdownView.setLayoutParams(params);
            countdownView.startCountDown();
            //*************************使用点睛样式***********************************
            //设置在点击按钮时调用点睛样式
            countdownView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    goToMainActivity();
                }
            });
            //设置在倒计时结束时调用点睛样式
            countdownView.setCountdownListener(new CountdownView.CountdownListener() {
                @Override
                public void onStart() {

                }

                @Override
                public void onEnd() {
                    goToMainActivity();

                }

                @Override
                public void onPause() {

                }
            });
            //************************************************************

            if (mSplashContainer != null) {
                mSplashContainer.addView(countdownView);
            }
        }
    }

    /**
     * 跳转到主页面
     */
    private void goToMainActivity() {
        if (SplashCardManager.getInstance().canShowInnerActivityCard()) {
            return;
        }
        Intent intent = new Intent(CSJSplashActivity.this, MainActivity.class);
        startActivity(intent);
        if (mSplashContainer != null) {
            mSplashContainer.removeAllViews();
        }
        this.finish();
    }
}
