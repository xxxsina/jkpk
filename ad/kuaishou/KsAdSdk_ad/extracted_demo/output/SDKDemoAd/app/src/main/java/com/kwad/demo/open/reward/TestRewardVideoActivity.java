package com.kwad.demo.open.reward;

import static com.kwad.demo.open.feed.FeedHomeActivity.POS_ID;
import static com.kwad.sdk.api.ApiConst.EXTRA_KEY_ERRORCODE;
import static com.kwad.sdk.api.ApiConst.EXTRA_KEY_FRAUD;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.kwad.demo.open.KSSdkInitUtil;
import com.kwad.demo.open.TestPosId;
import com.kwad.demo.open.serverBid.BiddingDemoUtils;
import com.kwad.demo.open.utils.LogUtils;
import com.kwad.demo.open.utils.TestSpUtil;
import com.kwad.demo.open.utils.ToastUtil;
import com.kwad.sdk.api.KsInnerAd;
import com.kwad.sdk.api.KsLoadManager;
import com.kwad.sdk.api.KsRewardVideoAd;
import com.kwad.sdk.api.KsScene;
import com.kwad.sdk.api.KsVideoPlayConfig;
import com.kwad.sdk.api.SdkConfig;
import com.kwad.sdk.api.model.AdClickAction;
import com.kwad.sdk.api.model.AdExposureFailedReason;
import com.kwad.sdk.api.model.AdExposureFailureCode;
import com.kwad.sdk.api.model.AdShowAction;
import com.kwad.sdk.api.model.AdnName;
import com.kwad.sdk.api.model.AdnType;
import com.kwad.sdk.api.model.RewardTaskType;

/**
 * 激励视频广告，测试页面
 */
public class TestRewardVideoActivity extends FragmentActivity {
  private static final String TAG = "TestRewardVideo";
  private Context mContext;
  private Switch mChangeOrientation; //切换当前屏幕方向
  private Switch mServerCallbackSwitch; //是否开启服务端回调
  private Switch mPlayOnlineSwitch; //是否开启在线播放
  private Switch mShowLandscapeSwitch; //是否开启横屏播放
  private Switch mShowAfterRequestSwitch; // 是否在请求后后立即显示
  private long mPosId;
  private int mFailCode = -1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mContext = this;
    mPosId = getIntent().getLongExtra(POS_ID, TestPosId.POSID_REWARD.posId);
    setContentView(R.layout.activity_test_reward);
    initView();
  }

  private void initView() {
    mChangeOrientation = findViewById(R.id.change_orientation_switch);
    mServerCallbackSwitch = findViewById(R.id.reward_enable_server_callback);
    mPlayOnlineSwitch = findViewById(R.id.play_online_switch);
    mShowLandscapeSwitch = findViewById(R.id.show_landscape_switch);
    mShowAfterRequestSwitch = findViewById(R.id.show_reward_after_reuquest);
    setChangeOrientationListener();
  }

  // 1.请求激励视频广告，获取广告对象，KsRewardVideoAd
  public void requestRewardAd(View view) {
    Map<String, String> rewardCallbackExtraData = new HashMap<>();
    if (mServerCallbackSwitch.isChecked()) {
      //开启服务端回调后的配置参数
      rewardCallbackExtraData.put("thirdUserId", "test-uerid-jia");
      rewardCallbackExtraData.put("extraData", "testExtraData");
    }


    //视频展示的屏幕方向，建议和当前屏幕方向一致
    int screenOrientation = getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ?
        SdkConfig.SCREEN_ORIENTATION_PORTRAIT : SdkConfig.SCREEN_ORIENTATION_LANDSCAPE;

    //构建请求参数，此为测试posId，请联系快手平台申请正式posId
    KsScene.Builder builder = KSSdkInitUtil.createKSSceneBuilder(mPosId)
        .screenOrientation(screenOrientation)
        .rewardCallbackExtraData(rewardCallbackExtraData);

    if (!TextUtils.isEmpty(mBidResponseV1)) { // 设置在服务端竞价后的广告信息
      builder.setBidResponse(mBidResponseV1);
    } else if (!TextUtils.isEmpty(mBidResponseV2)) {
      builder.setBidResponseV2(mBidResponseV2);
    }
    KsScene scene = builder.build();

    final long startTime = System.currentTimeMillis();
    KSSdkInitUtil.getLoadManager()
        .loadRewardVideoAd(scene, new KsLoadManager.RewardVideoAdListener() {
          @Override
          public void onError(int code, String msg) {
            ToastUtil.showToast(mContext, "激励视频广告请求失败" + code + msg);
            Log.e(TAG, "Callback --> onError: " + code + ", " + msg);
            LogUtils.recodeCallback(LogUtils.SCENE_REWARD_PRE, "loadRewardVideoAd_onError");
          }

          @Override
          public void onRewardVideoResult(@Nullable List<KsRewardVideoAd> adList) {
            ToastUtil.showToast(mContext, "激励视频广告数据请求成功");
            //视频广告的数据加载完毕，在此回调后，可以播放在线视频，网络不好可能出现加载缓冲，影响体验。
            Log.e(TAG, "Callback --> onRewardVideoResult time: "
                + (System.currentTimeMillis() - startTime));
            LogUtils.recodeCallback(LogUtils.SCENE_REWARD_PRE, "onRewardVideoResult");
            if (mPlayOnlineSwitch.isChecked()) {
              showRewardVideoAd(adList);
            }
          }

          @Override
          public void onRewardVideoAdLoad(@Nullable List<KsRewardVideoAd> adList) {
            ToastUtil.showToast(mContext, "激励视频广告数据请求且资源缓存成功");
            //视频广告的数据加载和资源缓存完毕，在此回调后，播放本地视频，流畅不阻塞。
            Log.e(TAG, "Callback --> onRewardVideoAdLoad time: "
                + (System.currentTimeMillis() - startTime));
            LogUtils.recodeCallback(LogUtils.SCENE_REWARD_PRE, "onRewardVideoAdLoad");
            if (!mPlayOnlineSwitch.isChecked()) {
              showRewardVideoAd(adList);
            }
          }
        });
  }

  //2.展示激励视频，可以竖屏展示也可以横屏展示，建议与当前屏幕方向一致
  private void showRewardVideoAd(@Nullable List<KsRewardVideoAd> adList) {
    if (isFinishing()) {
      return;
    }
    if (adList == null || adList.isEmpty()) {
      return;
    }
    if (!mShowAfterRequestSwitch.isChecked()) {
      return;
    }

    KsRewardVideoAd rewardVideoAd = adList.get(0);
    //1.设置监听
    setRewardListener(rewardVideoAd);
    //测试代码
    //2.设置展示配置
    KsVideoPlayConfig videoPlayConfig = new KsVideoPlayConfig.Builder()
        .showLandscape(mShowLandscapeSwitch.isChecked()) // 横屏播放, 建议和当前屏幕方向保持一致
        .build();
    //3.展示激励视频广告
    rewardVideoAd.showRewardVideoAd(this, videoPlayConfig);

  }

  private void setRewardListener(@NonNull final KsRewardVideoAd rewardVideoAd) {
    rewardVideoAd.setInnerAdInteractionListener(
        new KsInnerAd.KsInnerAdInteractionListener() {
          @Override
          public void onAdClicked(KsInnerAd ksInnerAd) {
            ToastUtil.showToast(mContext, "激励视频内部广告点击：" + ksInnerAd.getType());
            LogUtils.recodeCallback(LogUtils.SCENE_REWARD_PRE, "onAdClicked_Inner");
          }

          @Override
          public void onAdShow(KsInnerAd ksInnerAd) {
            ToastUtil.showToast(mContext, "激励视频内部广告曝光：" + ksInnerAd.getType());
            LogUtils.recodeCallback(LogUtils.SCENE_REWARD_PRE, "onAdShow_Inner");
          }
        });

    rewardVideoAd
        .setRewardAdInteractionListener(new KsRewardVideoAd.RewardAdInteractionListener() {
          @Override
          public void onAdClicked() {
            ToastUtil.showToast(mContext, "激励视频广告点击");
            LogUtils.recodeCallback(LogUtils.SCENE_REWARD_PRE, "onAdClicked");
          }

          @Override
          public void onPageDismiss() {
            ToastUtil.showToast(mContext, "激励视频广告关闭");
            LogUtils.recodeCallback(LogUtils.SCENE_REWARD_PRE, "onPageDismiss");
          }

          @Override
          public void onVideoPlayError(int code, int extra) {
            ToastUtil.showToast(mContext, "激励视频广告播放出错");
            LogUtils.recodeCallback(LogUtils.SCENE_REWARD_PRE, "onVideoPlayError");
          }

          @Override
          public void onVideoPlayEnd() {
            ToastUtil.showToast(mContext, "激励视频广告播放完成");
            LogUtils.recodeCallback(LogUtils.SCENE_REWARD_PRE, "onVideoPlayEnd");
          }

          @Override
          public void onVideoSkipToEnd(long playDuration) {
            ToastUtil.showToast(mContext, "激励视频广告跳过播放完成");
            LogUtils.recodeCallback(LogUtils.SCENE_REWARD_PRE, "onVideoSkipToEnd");
          }

          @Override
          public void onVideoPlayStart() {
            ToastUtil.showToast(mContext, "激励视频广告播放开始");
            LogUtils.recodeCallback(LogUtils.SCENE_REWARD_PRE, "onVideoPlayStart");
          }

          /**
           * 激励视频广告激励回调，只会回调一次
           */
          @Override
          public void onRewardVerify() {
            ToastUtil.showToast(mContext, "激励视频广告获取激励");
            LogUtils.recodeCallback(LogUtils.SCENE_REWARD_PRE, "onRewardVerify");
          }

          @Override
          public void onRewardVerify(Map<String, Object> extraMap) {
            String showExtraMap =
                "extraMap = {IS_FRAUD:" + extraMap.get(EXTRA_KEY_FRAUD) + ",IS_FRAUD_ERROR_CODE:" +
                    extraMap.get(EXTRA_KEY_ERRORCODE) + "}";
            ToastUtil.showToast(mContext, "激励视频广告获取激励 " + showExtraMap);
            LogUtils.recodeCallback(LogUtils.SCENE_REWARD_PRE, "onRewardVerify " + showExtraMap);
          }

          /**
           *  视频激励分阶段回调
           * @param taskType 当前激励视频所属任务类型
           *                 RewardTaskType.LOOK_VIDEO 观看视频类型             属于浅度奖励类型
           *                 RewardTaskType.LOOK_LANDING_PAGE 浏览落地页N秒类型  属于深度奖励类型
           *                 RewardTaskType.USE_APP 下载使用App N秒类型          属于深度奖励类型
           * @param currentTaskStatus  当前所完成任务类型，@RewardTaskType中之一
           */
          @Override
          public void onRewardStepVerify(int taskType, int currentTaskStatus) {
            ToastUtil.showToast(mContext,
                "激励视频广告分阶段获取激励，当前任务类型为：" + getTaskStatusStr(taskType) +
                    "，当前完成任务类型为：" + getTaskStatusStr(currentTaskStatus));
            LogUtils.recodeCallback(LogUtils.SCENE_REWARD_PRE, "onRewardStepVerify");
          }

          @Override
          public void onExtraRewardVerify(int extraRewardType) {
            ToastUtil.showToast(mContext, "激励视频广告获取额外奖励：" + extraRewardType);
            LogUtils.recodeCallback(LogUtils.SCENE_REWARD_PRE, "onExtraRewardVerify");
          }
        });
  }

  private String getTaskStatusStr(int taskType) {
    String taskStatusStr = "";
    switch (taskType) {
      case RewardTaskType.LOOK_VIDEO:
        taskStatusStr = "观看视频";
        break;
      case RewardTaskType.LOOK_LANDING_PAGE:
        taskStatusStr = "浏览落地页";
        break;
      case RewardTaskType.USE_APP:
        taskStatusStr = "使用APP";
        break;
    }
    return taskStatusStr;
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
    mBidResponseV1 = null;
    mBidResponseV2 = null;
    KsScene ksScene = KSSdkInitUtil.createKSSceneBuilder(TestPosId.POSID_REWARD.posId)
        .build(); // 根据需要传入场景参数，需传入有效posId
    String token = KSSdkInitUtil.getLoadManager().getBidRequestToken(ksScene);
    BiddingDemoUtils.fetchBidResponse(this, TestPosId.POSID_REWARD.posId, token, false,
        new BiddingDemoUtils.FetchResponseCallback() {
          @Override
          public void onSuccess(String bidResponse) {
            mBidResponseV1 = bidResponse;
          }
        });
  }

  // 展示竞价广告
  public void showAdV1(View view) {
    if (TextUtils.isEmpty(mBidResponseV1)) {
      ToastUtil.showToast(TestRewardVideoActivity.this, "请先获取竞价信息");
      return;
    }
    requestRewardAd(view);
  }

  // 获取服务端竞价信息
  public void fetchBidResponseV2(View view) {
    mBidResponseV1 = null;
    mBidResponseV2 = null;
    KsScene ksScene = KSSdkInitUtil.createKSSceneBuilder(0)
        .build(); // 根据需要传入场景参数，注意：创建KsScene时 posId 可传无效值，在adx服务端拉取快手竞价信息时必须传有效的 posId
    String token = KSSdkInitUtil.getLoadManager().getBidRequestTokenV2(ksScene);
    BiddingDemoUtils.fetchBidResponse(this, TestPosId.POSID_REWARD.posId, token, true,
        new BiddingDemoUtils.FetchResponseCallback() {
          @Override
          public void onSuccess(String bidResponse) {
            mBidResponseV2 = bidResponse;
          }
        });
  }

  // 展示竞价广告
  public void showAdV2(View view) {
    if (TextUtils.isEmpty(mBidResponseV2)) {
      ToastUtil.showToast(TestRewardVideoActivity.this, "请先获取竞价信息");
      return;
    }
    requestRewardAd(view);
  }
  /********   服务端竞价模拟 end     ********/


  //返回按钮点击
  public void onBackClick(View view) {
    onBackPressed();
  }
}