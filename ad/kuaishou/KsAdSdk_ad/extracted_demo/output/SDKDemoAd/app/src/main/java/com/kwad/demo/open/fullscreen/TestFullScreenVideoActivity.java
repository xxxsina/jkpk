package com.kwad.demo.open.fullscreen;

import static com.kwad.demo.open.feed.FeedHomeActivity.POS_ID;

import java.util.List;
import java.util.Random;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.kwad.demo.R;
import com.kwad.demo.open.TestPosId;
import com.kwad.demo.open.feed.TestConfigFeedRecyclerActivity;
import com.kwad.demo.open.serverBid.BiddingDemoUtils;
import com.kwad.demo.open.utils.LogUtils;
import com.kwad.demo.open.KSSdkInitUtil;
import com.kwad.demo.open.utils.TestSpUtil;
import com.kwad.demo.open.utils.ToastUtil;
import com.kwad.sdk.api.KsFeedAd;
import com.kwad.sdk.api.KsFullScreenVideoAd;
import com.kwad.sdk.api.KsLoadManager;
import com.kwad.sdk.api.KsScene;
import com.kwad.sdk.api.KsVideoPlayConfig;
import com.kwad.sdk.api.SdkConfig;
import com.kwad.sdk.api.model.AdClickAction;
import com.kwad.sdk.api.model.AdExposureFailedReason;
import com.kwad.sdk.api.model.AdExposureFailureCode;
import com.kwad.sdk.api.model.AdShowAction;
import com.kwad.sdk.api.model.AdnName;
import com.kwad.sdk.api.model.AdnType;


/**
 * 全屏视频广告，测试页面
 */
public class TestFullScreenVideoActivity extends FragmentActivity {
  private static final String TAG = "TestFullScreenVideo";
  private Context mContext;
  private Switch mChangeOrientation; //切换当前屏幕方向
  private Switch mPlayOnlineSwitch; //是否开启在线播放
  private Switch mShowLandscapeSwitch; //是否开启横屏展示
  private Switch mShowAfterRequestSwitch; // 是否在请求后后立即显示
  private KsFullScreenVideoAd mFullScreenVideoAd;
  private Long mPosId;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mContext = this;
    mPosId = getIntent().getLongExtra(POS_ID, TestPosId.POSID_FULLSCREEN.posId);
    setContentView(R.layout.activity_test_full_screen);
    initView();
  }

  private void initView() {
    mChangeOrientation = findViewById(R.id.change_orientation_switch);
    mPlayOnlineSwitch = findViewById(R.id.play_online_switch);
    mShowLandscapeSwitch = findViewById(R.id.show_landscape_switch);
    mShowAfterRequestSwitch = findViewById(R.id.show_reward_after_reuquest);
    setChangeOrientationListener();
  }

  // 1.请求全屏视频广告，获取广告对象，KsFullScreenVideoAd
  public void requestFullScreenAd(View view) {
    //视频展示的屏幕方向，建议和当前屏幕方向一致
    int screenOrientation = getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ?
        SdkConfig.SCREEN_ORIENTATION_PORTRAIT : SdkConfig.SCREEN_ORIENTATION_LANDSCAPE;

    //构建请求参数，此为测试posId，请联系快手平台申请正式posId
    KsScene.Builder builder = KSSdkInitUtil.createKSSceneBuilder(mPosId)
        .screenOrientation(screenOrientation);
    if (!TextUtils.isEmpty(mBidResponseV1)) { // 设置在服务端竞价后的广告信息
      builder.setBidResponse(mBidResponseV1);
    } else if (!TextUtils.isEmpty(mBidResponseV2)) {
      builder.setBidResponseV2(mBidResponseV2);
    }
    KsScene scene = builder.build();

    final long startTime = System.currentTimeMillis();
    KSSdkInitUtil.getLoadManager().loadFullScreenVideoAd(scene,
        new KsLoadManager.FullScreenVideoAdListener() {
          @Override
          public void onError(int code, String msg) {
            ToastUtil.showToast(mContext, "全屏视频广告请求失败" + code + msg);
            Log.e(TAG, "Callback --> onError: " + code + ", " + msg);
            LogUtils.recodeCallback(LogUtils.SCENE_FULL_PRE, "loadFullScreenVideoAd_onError");
          }

          public void onFullScreenVideoResult(@Nullable List<KsFullScreenVideoAd> adList) {
            ToastUtil.showToast(mContext, "全屏视频广告数据请求成功");
            //视频广告的数据加载完毕，在此回调后，可以播放在线视频，网络不好可能出现加载缓冲，影响体验。
            Log.e(TAG, "Callback --> onFullScreenVideoResult time: "
                + (System.currentTimeMillis() - startTime));
            LogUtils.recodeCallback(LogUtils.SCENE_FULL_PRE, "onFullScreenVideoResult");
            if (mPlayOnlineSwitch.isChecked()) {
              showFullScreenVideoAd(adList);
            }
          }

          @Override
          public void onFullScreenVideoAdLoad(@Nullable List<KsFullScreenVideoAd> adList) {
            ToastUtil.showToast(mContext, "全屏视频广告数据请求且资源缓存成功");
            //视频广告的数据加载和资源缓存完毕，在此回调后，播放本地视频，流畅不阻塞。
            Log.e(TAG, "Callback --> onFullScreenVideoAdLoad time: "
                + (System.currentTimeMillis() - startTime));
            LogUtils.recodeCallback(LogUtils.SCENE_FULL_PRE, "onFullScreenVideoAdLoad");
            if (!mPlayOnlineSwitch.isChecked()) {
              showFullScreenVideoAd(adList);
            }
          }
        });
  }

  //2.展示全屏视频，可以竖屏展示也可以横屏展示，建议与当前屏幕方向一致
  private void showFullScreenVideoAd(@Nullable List<KsFullScreenVideoAd> adList) {
    if (isFinishing()) {
      return;
    }
    if (adList == null || adList.isEmpty()) {
      return;
    }
    if (!mShowAfterRequestSwitch.isChecked()) {
      return;
    }

    KsFullScreenVideoAd fullScreenVideoAd = adList.get(0);
    //1.设置监听
    setFullScreenVidListener(fullScreenVideoAd);
    //测试代码
    //2.设置展示配置
    KsVideoPlayConfig videoPlayConfig = new KsVideoPlayConfig.Builder()
        .showLandscape(mShowLandscapeSwitch.isChecked()) // 横屏播放, 建议和当前屏幕方向保持一致
        .build();
    //3.展示激励视频广告
    fullScreenVideoAd.showFullScreenVideoAd(this, videoPlayConfig);
  }


  private void setFullScreenVidListener(@NonNull final KsFullScreenVideoAd fullScreenVideoAd) {
    fullScreenVideoAd.setFullScreenVideoAdInteractionListener(
        new KsFullScreenVideoAd.FullScreenVideoAdInteractionListener() {
          @Override
          public void onAdClicked() {
            ToastUtil.showToast(mContext, "全屏视频广告点击");
            LogUtils.recodeCallback(LogUtils.SCENE_FULL_PRE, "onAdClicked");
          }

          @Override
          public void onPageDismiss() {
            ToastUtil.showToast(mContext, "全屏视频广告关闭");
            LogUtils.recodeCallback(LogUtils.SCENE_FULL_PRE, "onPageDismiss");
          }

          @Override
          public void onVideoPlayError(int code, int extra) {
            ToastUtil.showToast(mContext, "全屏视频广告播放出错");
            LogUtils.recodeCallback(LogUtils.SCENE_FULL_PRE, "onVideoPlayError");
          }

          @Override
          public void onVideoPlayEnd() {
            ToastUtil.showToast(mContext, "全屏视频广告播放完成");
            LogUtils.recodeCallback(LogUtils.SCENE_FULL_PRE, "onVideoPlayEnd");
          }

          @Override
          public void onVideoPlayStart() {
            ToastUtil.showToast(mContext, "全屏视频广告播放开始");
            LogUtils.recodeCallback(LogUtils.SCENE_FULL_PRE, "onVideoPlayStart");
          }

          @Override
          public void onSkippedVideo() {
            ToastUtil.showToast(mContext, "全屏视频广告播放跳过");
            LogUtils.recodeCallback(LogUtils.SCENE_FULL_PRE, "onSkippedVideo");
          }
        });
  }

  private void setChangeOrientationListener() {
    mChangeOrientation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setRequestedOrientation(isChecked ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
      }
    });
  }

  /********   服务端竞价模拟 start   ********/
  private String mBidResponseV1;
  private String mBidResponseV2;

  // 获取服务端竞价信息
  public void fetchBidResponseV1(View view) {
    // 根据需要传入场景参数，需传入有效posId
    KsScene ksScene = KSSdkInitUtil.createKSSceneBuilder(mPosId)
        .build();
    String token = KSSdkInitUtil.getLoadManager().getBidRequestToken(ksScene);
    BiddingDemoUtils.fetchBidResponse(this, TestPosId.POSID_FULLSCREEN.posId, token, false,
        new BiddingDemoUtils.FetchResponseCallback() {
          @Override
          public void onSuccess(String bidResponse) {
            mBidResponseV1 = bidResponse;
          }
        });
  }

  public void showAdV1(View view) {
    if (TextUtils.isEmpty(mBidResponseV1)) {
      ToastUtil.showToast(TestFullScreenVideoActivity.this, "请先获取竞价信息");
      return;
    }
    requestFullScreenAd(view);
  }

  public void fetchBidResponseV2(View view) {
    KsScene ksScene = KSSdkInitUtil.createKSSceneBuilder(0)
        .build(); // 根据需要传入场景参数，注意：创建KsScene时 posId 可传无效值，在adx服务端拉取快手竞价信息时必须传有效的 posId
    String token = KSSdkInitUtil.getLoadManager().getBidRequestTokenV2(ksScene);
    BiddingDemoUtils.fetchBidResponse(this, TestPosId.POSID_FULLSCREEN.posId, token, true,
        new BiddingDemoUtils.FetchResponseCallback() {
          @Override
          public void onSuccess(String bidResponse) {
            mBidResponseV2 = bidResponse;
          }
        });
  }

  public void showAdV2(View view) {
    if (TextUtils.isEmpty(mBidResponseV2)) {
      ToastUtil.showToast(TestFullScreenVideoActivity.this, "请先获取竞价信息");
      return;
    }
    requestFullScreenAd(view);
  }
  /********   服务端竞价模拟 end   ********/
  //返回按钮点击
  public void onBackClick(View view) {
    onBackPressed();
  }
}