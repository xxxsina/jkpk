package com.kwad.demo.open.feed;

import static com.kwad.demo.open.feed.FeedHomeActivity.NEED_ADJUST_WIDTH;
import static com.kwad.demo.open.feed.FeedHomeActivity.NEED_CHANGE_BACKGROUND_COLOR;
import static com.kwad.demo.open.feed.FeedHomeActivity.POS_ID;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.kwad.demo.R;
import com.kwad.demo.open.KSSdkInitUtil;
import com.kwad.demo.open.TestPosId;
import com.kwad.demo.open.utils.LogUtils;
import com.kwad.demo.open.utils.ToastUtil;
import com.kwad.demo.open.utils.ViewUtil;
import com.kwad.demo.open.view.LoadMoreListView;
import com.kwad.sdk.api.KsAdVideoPlayConfig;
import com.kwad.sdk.api.KsFeedAd;
import com.kwad.sdk.api.KsLoadManager;
import com.kwad.sdk.api.KsScene;
import com.kwad.sdk.api.model.AdClickAction;
import com.kwad.sdk.api.model.AdExposureFailedReason;
import com.kwad.sdk.api.model.AdExposureFailureCode;
import com.kwad.sdk.api.model.AdShowAction;
import com.kwad.sdk.api.model.AdnName;
import com.kwad.sdk.api.model.AdnType;

public class TestConfigFeedListActivity extends Activity {

  private Context mContext;
  private List<KsFeedAd> mFeedList;
  private FeedListAdapter mFeedListAdapter;
  private Handler mHandler = new Handler(Looper.getMainLooper());
  private SeekBar mSeekBar;
  private TextView mSeekProgress;
  private LoadMoreListView mListView;
  private long mPosId;
  private boolean mNeedAdjustWidth;

  private boolean mChangeBackgroundColor;

  /**********DevelopCode Start***********/
  //模拟展示的是否是快手广告
  private boolean mIsBidFailed = false;
  /**********DevelopCode End***********/

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.mContext = getApplicationContext();
    mPosId = getIntent().getLongExtra(POS_ID, TestPosId.POSID_ENTRY_TYPE4.posId);
    mNeedAdjustWidth = getIntent().getBooleanExtra(NEED_ADJUST_WIDTH, false);
    mChangeBackgroundColor = getIntent().getBooleanExtra(NEED_CHANGE_BACKGROUND_COLOR,false);
    setContentView(R.layout.activity_test_configfeed_list);
    if(mChangeBackgroundColor){
      try {
        this.getWindow().setBackgroundDrawable(getResources().getDrawable(R.color.test_yellow));
      }catch (Exception e){
        ToastUtil.showToast(mContext, "设置背景色失败");
      }
    }
    initView();
  }


  private long getTestPosId() {
    return mPosId;
  }

  private void initView() {
    mListView = findViewById(R.id.feed_list);
    mFeedList = new ArrayList<>();
    mFeedListAdapter = new FeedListAdapter(this, mFeedList);
    mListView.setAdapter(mFeedListAdapter);
    mListView.setLoadMoreListener(new LoadMoreListView.ILoadMoreListener() {
      @Override
      public void onLoadMore() {
        int width = mListView.getWidth();
        if (width > 0) {
          requestAd(getTestPosId(), width);
        }
      }
    });
    mHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        int width = mListView.getWidth();
        mSeekBar.setMax(width);
        mSeekBar.setProgress(width);
        if (width > 0) {
          requestAd(getTestPosId(), width);
        }
      }
    }, 500);
    mSeekBar = findViewById(R.id.list_seek_bar);
    mSeekProgress = findViewById(R.id.text_progress);
    mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mSeekProgress.setText("当前宽度：" + progress + "px " + ViewUtil.px2dip(mContext, progress) + " dip");
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {

      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        resetRecycleViewWidth(seekBar.getProgress());
      }
    });
    if (!mNeedAdjustWidth) {
      mSeekBar.setVisibility(View.GONE);
      mSeekProgress.setVisibility(View.GONE);
    }
  }

  private void resetRecycleViewWidth(int width) {
    ViewGroup.LayoutParams layoutParams = mListView.getLayoutParams();
    layoutParams.width = width;
    mListView.setLayoutParams(layoutParams);
    mFeedList.clear();
    mFeedListAdapter.notifyDataSetChanged();
    mHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        int width = mListView.getWidth();
        if (width > 0) {
          requestAd(mPosId, width);
        }
      }
    }, 500);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    ToastUtil.showToast(this, "List信息流页面销毁");
    clearData();
    mHandler.removeCallbacksAndMessages(null);
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();
    finish();
  }

  private void clearData() {
    mFeedList.clear();
    mFeedList = null;

    mListView = null;

    mFeedListAdapter.mFeedList.clear();
    mFeedListAdapter.mFeedList = null;
    mFeedListAdapter = null;

    Runtime.getRuntime().gc();
  }

  private void requestAd(long posId, int width) {
    // 此为测试posId，请联系快手平台申请正式posId
    KsScene scene = KSSdkInitUtil.createKSSceneBuilder(posId)
        .width(width)
        .adNum(5)
        .build();

    KSSdkInitUtil.getLoadManager()
        .loadConfigFeedAd(scene, new KsLoadManager.FeedAdListener() {
      @Override
      public void onError(int code, String msg) {
        if (mListView != null) {
          mListView.setLoadingError();
        }
        ToastUtil.showToast(mContext, "广告数据请求失败" + code + msg);
        LogUtils.recodeCallback(LogUtils.SCENE_FEED_PRE, "loadConfigFeedAd_onError");
      }

      @Override
      public void onFeedAdLoad(@Nullable List<KsFeedAd> adList) {
        if (mFeedList == null) {
          // 当前的Activity 已经被销毁，后续不需要再进行其他操作
          return;
        }
        LogUtils.recodeCallback(LogUtils.SCENE_FEED_PRE, "onFeedAdLoad");
        if (mListView != null) {
          mListView.setLoadingFinish();
        }
        if (adList == null || adList.isEmpty()) {
          ToastUtil.showToast(mContext, "广告数据为空");
          return;
        }
        final int loadCount = 15;// 模拟每次展示的Item刷新个数
        for (int i = 0; i < loadCount; i++) {
          mFeedList.add(null);
        }
        final int totalCount = mFeedList.size();
        for (final KsFeedAd ksFeedAd : adList) {
          if (ksFeedAd == null) {
            continue;
          }
          ksFeedAd.setAdInteractionListener(new KsFeedAd.AdInteractionListener() {

            @Override
            public void onAdClicked() {
              ToastUtil.showToast(mContext, "广告点击回调");
              LogUtils.recodeCallback(LogUtils.SCENE_FEED_PRE, "onAdClicked");
              /**********DevelopCode Start***********/
              if (mIsBidFailed) {
                //模拟竞价失败，展示其他广告时点击上报
                testBidFailedClickAndShowReport(ksFeedAd, AdActionType.CLICK);
              }
              /**********DevelopCode End***********/
            }

            @Override
            public void onAdShow() {
              ToastUtil.showToast(mContext, "广告曝光回调");
              LogUtils.recodeCallback(LogUtils.SCENE_FEED_PRE, "onAdShow");
              /**********DevelopCode Start***********/
              if (mIsBidFailed) {
                //模拟竞价失败，展示其他广告时曝光上报
                testBidFailedClickAndShowReport(ksFeedAd, AdActionType.SHOW);
              }
              /**********DevelopCode End***********/
            }

            @Override
            public void onDislikeClicked() {
              ToastUtil.showToast(mContext, "广告不喜欢回调");
              LogUtils.recodeCallback(LogUtils.SCENE_FEED_PRE, "onDislikeClicked");
              mFeedList.remove(ksFeedAd);
              mFeedListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onDownloadTipsDialogShow() {
              ToastUtil.showToast(mContext, "广告展示下载合规弹窗");
              LogUtils.recodeCallback(LogUtils.SCENE_FEED_PRE, "onDownloadTipsDialogShow");
            }

            @Override
            public void onDownloadTipsDialogDismiss() {
              ToastUtil.showToast(mContext, "广告关闭下载合规弹窗");
              LogUtils.recodeCallback(LogUtils.SCENE_FEED_PRE, "onDownloadTipsDialogDismiss");
            }
          });

          KsAdVideoPlayConfig videoPlayConfig = new KsAdVideoPlayConfig.Builder()
              .dataFlowAutoStart(false) // 是否非WiFi下自动播放
              .videoAutoPlayType(KsAdVideoPlayConfig.VideoAutoPlayType.AUTO_PLAY) // 设置有网时自动播放，当与dataFlowAutoStart()同时设置时，以最后一个传入的值为准
              .build();
          ksFeedAd.setVideoPlayConfig(videoPlayConfig);

          /**********DevelopCode Start***********/
          // 模拟竞价
          mIsBidFailed = testBiddingReport(mContext, ksFeedAd);
          /**********DevelopCode End***********/

          ksFeedAd.render(new KsFeedAd.AdRenderListener() {
            @Override
            public void onAdRenderSuccess(View view) {
              LogUtils.recodeCallback(LogUtils.SCENE_FEED_PRE, "onAdRenderSuccess");
              int random = (int) (Math.random() * loadCount) + totalCount - loadCount;
              while (mFeedList.get(random) != null) {
                random = (int) (Math.random() * loadCount) + totalCount - loadCount;
              }
              mFeedList.set(random, ksFeedAd);
              mFeedListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onAdRenderFailed(int errorCode, String errorMsg) {
              LogUtils.recodeCallback(LogUtils.SCENE_FEED_PRE, "onAdRenderFailed");
            }
          });

        }
      }
    });
  }

  /**********DevelopCode Start***********/
  static boolean testBiddingReport(Context context,KsFeedAd ksFeedAd) {
    LogUtils.recodeFuncLog(LogUtils.SCENE_FEED_PRE,
        "getECPM() =" + ksFeedAd.getECPM());
    LogUtils.recodeFuncLog(LogUtils.SCENE_FEED_PRE,
        "getMaterialType() =" + ksFeedAd.getMaterialType());
    LogUtils.recodeFuncLog(LogUtils.SCENE_FEED_PRE,
        "getInteractionType() =" + ksFeedAd.getInteractionType());
    Random random = new Random();
    boolean bidFailed = false;
    if (random.nextBoolean()) {
      //模拟竞价成功
      int bidEcpm = random.nextInt(9999) + 1;
      int lossBidEcpm = random.nextInt(bidEcpm);
      LogUtils.recodeFuncLog(LogUtils.SCENE_FEED_PRE,
          "setBidEcpm() =" + bidEcpm);
      ksFeedAd.setBidEcpm(bidEcpm);
      LogUtils.recodeFuncLog(LogUtils.SCENE_FEED_PRE,
          "setBidEcpm() =" + (bidEcpm + "|" + lossBidEcpm));
      ksFeedAd.setBidEcpm(bidEcpm, lossBidEcpm);
    } else {
      //模拟竞价失败
      @AdExposureFailureCode int failureCod = random.nextInt(5);
      AdExposureFailedReason reason = new AdExposureFailedReason();
      if (failureCod == AdExposureFailureCode.BID_FAILED) {
        ToastUtil.showToast(context, "竞价失败");
        bidFailed = true;
        int winEcpm = random.nextInt(10000);
        @AdnType int adnType = new Random().nextInt(3) + 1;
        reason.setWinEcpm(winEcpm)
            .setAdnType(adnType);
        if (adnType == AdnType.THIRD_PARTY_AD) {
          @AdnName String adnName;
          int rd = random.nextInt(4);
          if (rd == 0) {
            adnName = AdnName.CHUANSHANJIA;
          } else if (rd == 1) {
            adnName = AdnName.GUANGDIANTONG;
          } else if (rd == 2) {
            adnName = AdnName.BAIDU;
          } else {
            adnName = AdnName.OTHER;
          }
          reason.setAdnName(adnName);
        }
        reason.setAdUserName("feed 广告主名称-test");
        reason.setAdTitle("feed 广告标题-test");
        reason.setAdRequestId("feed 请求ID-test");
        reason.setAdnMaterialType(new Random().nextInt(7) + 1);
        reason.setAdnMaterialUrl("feed 广告素材url-test");
      }
      ksFeedAd.reportAdExposureFailed(failureCod, reason);
    }
    return bidFailed;
  }

  void testBidFailedClickAndShowReport(KsFeedAd ksFeedAd, AdActionType actionType) {
    AdExposureFailedReason reason = new AdExposureFailedReason();
    reason.setAdUserName("feed 广告主名称-test");
    reason.setAdTitle("feed 广告标题-test");
    reason.setAdRequestId("feed 请求ID-test");
    reason.setAdnMaterialType(new Random().nextInt(7) + 1);
    reason.setAdnMaterialUrl("feed 广告素材url-test");
    if (actionType == AdActionType.SHOW) {
      reason.setIsShow(AdShowAction.SHOW);
    } else if (actionType == AdActionType.CLICK) {
      reason.setIsClick(AdClickAction.CLICK);
    }
    ksFeedAd.reportAdExposureFailed(AdExposureFailureCode.BID_FAILED, reason);
  }

  public enum AdActionType {
    UNKNOWN,
    CLICK,
    SHOW
  }
  /**********DevelopCode End***********/

  private static class FeedListAdapter extends BaseAdapter {
    private Context mContext;
    private List<KsFeedAd> mFeedList;

    FeedListAdapter(Context context, List<KsFeedAd> feedList) {
      this.mContext = context;
      this.mFeedList = feedList;
    }

    @Override
    public int getCount() {
      return mFeedList.size();
    }

    @Override
    public KsFeedAd getItem(int position) {
      return mFeedList.get(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public int getItemViewType(int position) {
      KsFeedAd ksFeedAd = getItem(position);
      if (ksFeedAd != null) {
        return ItemViewType.ITEM_VIEW_TYPE_AD;
      } else {
        return ItemViewType.ITEM_VIEW_TYPE_NORMAL;
      }
    }

    @Override
    public int getViewTypeCount() {
      return 2;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      KsFeedAd ksFeedAd = getItem(position);
      switch (getItemViewType(position)) {
        case ItemViewType.ITEM_VIEW_TYPE_AD:
          return getAdItemView(convertView, parent, ksFeedAd);
        case ItemViewType.ITEM_VIEW_TYPE_NORMAL:
        default:
          return getNormalItemView(convertView, parent, position);
      }
    }

    private View getAdItemView(View convertView, ViewGroup parent, final KsFeedAd ksFeedAd) {
      AdViewHolder adViewHolder;
      if (convertView == null) {
        convertView = LayoutInflater.from(mContext).inflate(R.layout.config_feed_list_item_ad_container,
            parent, false);
        adViewHolder = new AdViewHolder(convertView);
        convertView.setTag(adViewHolder);
      } else {
        adViewHolder = (AdViewHolder) convertView.getTag();
      }
      // 设置监听
      // ksFeedAd.setVideoSoundEnable(false);//视频播放是否，默认静音播放
      View videoView = ksFeedAd.getFeedView(mContext);
      if (videoView != null && videoView.getParent() == null) {
        adViewHolder.mAdContainer.removeAllViews();
        adViewHolder.mAdContainer.addView(videoView);
      }
      return convertView;
    }

    @SuppressLint("DefaultLocale")
    private View getNormalItemView(View convertView, ViewGroup parent, int position) {
      NormalViewHolder normalViewHolder;
      if (convertView == null) {
        normalViewHolder = new NormalViewHolder();
        convertView =
            LayoutInflater.from(mContext).inflate(R.layout.native_item_normal, parent, false);
        normalViewHolder.textView = convertView.findViewById(R.id.tv);
        convertView.setTag(normalViewHolder);
      } else {
        normalViewHolder = (NormalViewHolder) convertView.getTag();
      }
      normalViewHolder.textView.setText(String.format("ListView item %d", position));
      return convertView;
    }

    @IntDef({ItemViewType.ITEM_VIEW_TYPE_NORMAL, ItemViewType.ITEM_VIEW_TYPE_AD})
    @interface ItemViewType {
      int ITEM_VIEW_TYPE_NORMAL = 0;
      int ITEM_VIEW_TYPE_AD = 1;
    }

    private static class NormalViewHolder {
      TextView textView;
    }

    private static class AdViewHolder {
      FrameLayout mAdContainer;

      AdViewHolder(View convertView) {
        mAdContainer = convertView.findViewById(R.id.feed_container);
      }
    }
  }
}
