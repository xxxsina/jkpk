package com.kwad.demo.open.feed;

import static com.kwad.demo.open.feed.FeedHomeActivity.NEED_ADJUST_WIDTH;
import static com.kwad.demo.open.feed.FeedHomeActivity.NEED_CHANGE_BACKGROUND_COLOR;
import static com.kwad.demo.open.feed.FeedHomeActivity.POS_ID;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.kwad.demo.R;
import com.kwad.demo.open.KSSdkInitUtil;
import com.kwad.demo.open.TestPosId;
import com.kwad.demo.open.utils.LogUtils;
import com.kwad.demo.open.utils.ToastUtil;
import com.kwad.demo.open.utils.ViewUtil;
import com.kwad.demo.open.view.LoadMoreRecyclerView;
import com.kwad.demo.open.view.LoadMoreView;
import com.kwad.sdk.api.KsAdVideoPlayConfig;
import com.kwad.sdk.api.KsFeedAd;
import com.kwad.sdk.api.KsLoadManager;
import com.kwad.sdk.api.KsScene;
import com.kwad.sdk.api.model.AdClickAction;
import com.kwad.sdk.api.model.AdExposureFailedReason;
import com.kwad.sdk.api.model.AdExposureFailureCode;
import com.kwad.sdk.api.model.AdShowAction;

public class TestConfigFeedRecyclerActivity extends Activity {
  private Context mContext;
  private List<KsFeedAd> mFeedList;
  private Handler mHandler = new Handler(Looper.getMainLooper());
  private SeekBar mSeekBar;
  private TextView mSeekProgress;
  private LoadMoreRecyclerView mRecyclerView;
  private FeedRecyclerAdapter mFeedRecyclerAdapter;
  private long mPosId;
  private boolean mNeedAdjustWidth;

  private MyFeedLoadListener mFeedLoadListener;

  private boolean mChangeBackgroundColor;

  /**********DevelopCode Start***********/
  //模拟展示的是否是快手广告
  private boolean mIsBidFailed = false;
  /**********DevelopCode End***********/

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.mContext = this;
    mPosId = getIntent().getLongExtra(POS_ID, TestPosId.POSID_ENTRY_TYPE4.posId);
    mNeedAdjustWidth = getIntent().getBooleanExtra(NEED_ADJUST_WIDTH, false);
    mChangeBackgroundColor = getIntent().getBooleanExtra(NEED_CHANGE_BACKGROUND_COLOR,false);
    setContentView(R.layout.activity_test_configfeed_recycler);
    if(mChangeBackgroundColor){
      try {
        this.getWindow().setBackgroundDrawable(getResources().getDrawable(R.color.test_yellow));
      }catch (Exception e){
        ToastUtil.showToast(mContext, "设置背景色失败");
      }
    }
    initView();
  }

  private void initView() {
    mRecyclerView = findViewById(R.id.feed_list);
    if (mPosId == TestPosId.POSID_CONFIG_FEED_TYPE_15.posId) {
      mRecyclerView
          .setLayoutManager(new GridLayoutManager(mContext, 2, RecyclerView.VERTICAL, false));
    } else {
      mRecyclerView
          .setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
    }
    mFeedList = new ArrayList<>();
    mFeedRecyclerAdapter = new FeedRecyclerAdapter(this, mFeedList);
    mRecyclerView.setAdapter(mFeedRecyclerAdapter);
    mRecyclerView.setLoadMoreListener(new LoadMoreRecyclerView.ILoadMoreListener() {
      @Override
      public void onLoadMore() {
        int width = getRecycleViewWidth();
        if (width > 0) {
          requestAd(mPosId, width);
        }
      }
    });
    mHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        int width = getRecycleViewWidth();
        mSeekBar.setMax(width);
        mSeekBar.setProgress(width);
        if (width > 0) {
          requestAd(mPosId, width);
        }
      }
    }, 500);
    mSeekBar = findViewById(R.id.recycle_seek_bar);
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
    int spanCount = 1;
    ViewGroup.LayoutParams layoutParams = mRecyclerView.getLayoutParams();
    RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
    if (layoutManager instanceof GridLayoutManager) {
      spanCount = ((GridLayoutManager) layoutManager).getSpanCount();
    }
    layoutParams.width = width * spanCount;
    mRecyclerView.setLayoutParams(layoutParams);
    mFeedList.clear();
    mFeedRecyclerAdapter.notifyDataSetChanged();
    mHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        int width = getRecycleViewWidth();
        if (width > 0) {
          requestAd(mPosId, width);
        }
      }
    }, 500);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    ToastUtil.showToast(this, "Recycler信息流页面销毁");
    clearData();
    mHandler.removeCallbacksAndMessages(null);

    if (mFeedLoadListener != null) {
      mFeedLoadListener.destroy();
      mFeedLoadListener = null;
    }
  }


  @Override
  public void onBackPressed() {
    super.onBackPressed();
    finish();
  }

  private void clearData() {
    mFeedList.clear();
    mFeedList = null;

    mRecyclerView = null;

    mFeedRecyclerAdapter.mDataList.clear();
    mFeedRecyclerAdapter.mDataList = null;
    mFeedRecyclerAdapter = null;

    Runtime.getRuntime().gc();
  }

  private int getRecycleViewWidth() {
    int spanCount = 1;
    RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
    if (layoutManager instanceof GridLayoutManager) {
      spanCount = ((GridLayoutManager) layoutManager).getSpanCount();
    }
    return mRecyclerView.getWidth() / spanCount;
  }

  private void requestAd(long posId, int width) {
    // 此为测试posId，请联系快手平台申请正式posId
    KsScene scene = KSSdkInitUtil.createKSSceneBuilder(posId)
        .width(width)
        .adNum(3) // 支持返回多条广告，默认1条，最多5条，参数范围1-5
        .build();

    mFeedLoadListener = new MyFeedLoadListener(this);
    KSSdkInitUtil.getLoadManager().loadConfigFeedAd(scene, mFeedLoadListener);
  }

  private static class FeedRecyclerAdapter extends RecyclerView.Adapter {
    private Context mContext;
    private List<KsFeedAd> mDataList;

    FeedRecyclerAdapter(Context context, List<KsFeedAd> dataList) {
      this.mContext = context;
      this.mDataList = dataList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
      switch (viewType) {
        case ItemViewType.ITEM_VIEW_TYPE_LOAD_MORE:
          return new LoadMoreViewHolder(new LoadMoreView(mContext));
        case ItemViewType.ITEM_VIEW_TYPE_AD:
          return new AdViewHolder(LayoutInflater.from(mContext)
              .inflate(R.layout.config_feed_list_item_ad_container, viewGroup, false));
        case ItemViewType.ITEM_VIEW_TYPE_NORMAL:
        default:
          return new NormalViewHolder(
              LayoutInflater.from(mContext).inflate(R.layout.native_item_normal, viewGroup, false));
      }
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
      if (viewHolder instanceof LoadMoreViewHolder) {
        //
      } else if (viewHolder instanceof NormalViewHolder) {
        NormalViewHolder normalViewHolder = (NormalViewHolder) viewHolder;
        normalViewHolder.textView.setText(String.format("RecyclerView item %d", position));
      } else if (viewHolder instanceof AdViewHolder) {
        AdViewHolder adViewHolder = (AdViewHolder) viewHolder;
        final KsFeedAd ksFeedAd = mDataList.get(position);
        // 设置监听
        // ksFeedAd.setVideoSoundEnable(false);//视频播放是否，默认静音播放
        View videoView = ksFeedAd.getFeedView(mContext);
        if (videoView != null && videoView.getParent() == null) {
          adViewHolder.mAdContainer.removeAllViews();
          adViewHolder.mAdContainer.addView(videoView);
        }
      }
    }

    @Override
    public int getItemCount() {
      if (mDataList != null) {
        return mDataList.size() + 1;
      }
      return 0;
    }

    @Override
    public int getItemViewType(int position) {
      if (position >= mDataList.size()) {
        return ItemViewType.ITEM_VIEW_TYPE_LOAD_MORE;
      } else {
        KsFeedAd ksFeedAd = mDataList.get(position);
        if (ksFeedAd != null) {
          return ItemViewType.ITEM_VIEW_TYPE_AD;
        } else {
          return ItemViewType.ITEM_VIEW_TYPE_NORMAL;
        }
      }
    }

    @IntDef({ItemViewType.ITEM_VIEW_TYPE_NORMAL, ItemViewType.ITEM_VIEW_TYPE_AD,
        ItemViewType.ITEM_VIEW_TYPE_LOAD_MORE})
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.PARAMETER)
    @interface ItemViewType {
      int ITEM_VIEW_TYPE_LOAD_MORE = -1;
      int ITEM_VIEW_TYPE_NORMAL = 0;
      int ITEM_VIEW_TYPE_AD = 1;
    }
  }


  private static class LoadMoreViewHolder extends RecyclerView.ViewHolder {

    LoadMoreViewHolder(@NonNull View itemView) {
      super(itemView);
      itemView.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT,
          RecyclerView.LayoutParams.WRAP_CONTENT));
    }
  }

  private static class NormalViewHolder extends RecyclerView.ViewHolder {
    TextView textView;

    NormalViewHolder(@NonNull View itemView) {
      super(itemView);
      textView = itemView.findViewById(R.id.tv);
    }
  }

  private static class AdViewHolder extends RecyclerView.ViewHolder {
    FrameLayout mAdContainer;

    AdViewHolder(View convertView) {
      super(convertView);
      mAdContainer = convertView.findViewById(R.id.feed_container);
    }
  }

  /**********DevelopCode Start***********/
  static void testBidFailedClickAndShowReport(KsFeedAd ksFeedAd, AdActionType actionType) {
    AdExposureFailedReason reason = new AdExposureFailedReason();
    reason.setAdUserName("feed 广告主名称-test");
    reason.setAdTitle("feed 广告标题-test");
    reason.setAdRequestId("feed 请求ID-test");
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

  private static class MyFeedLoadListener implements KsLoadManager.FeedAdListener {
    private WeakReference<TestConfigFeedRecyclerActivity> mHost;


    public MyFeedLoadListener(TestConfigFeedRecyclerActivity host) {
      this.mHost = new WeakReference<>(host);
    }

    private TestConfigFeedRecyclerActivity obtain() {
      if (mHost == null) {
        return null;
      }

      return mHost.get();
    }

    public void destroy() {
      mHost = null;
    }

    @Override
    public void onError(int code, String msg) {
      TestConfigFeedRecyclerActivity activity = obtain();

      if (activity == null) {
        return;
      }

      if (activity.mRecyclerView != null) {
        activity.mRecyclerView.setLoadingError();
      }
      ToastUtil.showToast(activity, "广告数据请求失败" + code + msg);
      LogUtils.recodeCallback(LogUtils.SCENE_FEED_PRE, "loadConfigFeedAd_onError");
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onFeedAdLoad(@Nullable List<KsFeedAd> adList) {
      final TestConfigFeedRecyclerActivity activity = obtain();
      if (activity == null) {
        return;
      }


      if (activity.mFeedList == null) {
        // 当前的Activity 已经被销毁，后续不需要再进行其他操作
        return;
      }
      LogUtils.recodeCallback(LogUtils.SCENE_FEED_PRE, "onFeedAdLoad");
      if (activity.mRecyclerView != null) {
        activity.mRecyclerView.setLoadingFinish();
      }
      if (adList == null || adList.isEmpty()) {
        ToastUtil.showToast(activity, "广告数据为空");
        return;
      }
      final int loadCount = 15;// 模拟每次展示的Item刷新个数
      for (int i = 0; i < loadCount; i++) {
        activity.mFeedList.add(null);
      }
      final int totalCount = activity.mFeedList.size();
      for (final KsFeedAd ksFeedAd : adList) {
        if (ksFeedAd == null) {
          continue;
        }
        ksFeedAd.setAdInteractionListener(new KsFeedAd.AdInteractionListener() {

          @Override
          public void onAdClicked() {
            ToastUtil.showToast(activity, "广告点击回调");
            LogUtils.recodeCallback(LogUtils.SCENE_FEED_PRE, "onAdClicked");
            /**********DevelopCode Start***********/
            if (activity.mIsBidFailed) {
              //模拟竞价失败，展示其他广告时点击上报
              testBidFailedClickAndShowReport(ksFeedAd, TestConfigFeedRecyclerActivity.AdActionType.CLICK);
            }
            /**********DevelopCode End***********/
          }

          @Override
          public void onAdShow() {
            ToastUtil.showToast(activity, "广告曝光回调");
            LogUtils.recodeCallback(LogUtils.SCENE_FEED_PRE, "onAdShow");
            /**********DevelopCode Start***********/
            if (activity.mIsBidFailed) {
              //模拟竞价失败，展示其他广告时曝光上报
              testBidFailedClickAndShowReport(ksFeedAd, TestConfigFeedRecyclerActivity.AdActionType.SHOW);
            }
            /**********DevelopCode End***********/
          }

          @Override
          public void onDislikeClicked() {
            ToastUtil.showToast(activity, "广告不喜欢回调");
            LogUtils.recodeCallback(LogUtils.SCENE_FEED_PRE, "onDislikeClicked");
            activity.mFeedList.remove(ksFeedAd);
            activity.mFeedRecyclerAdapter.notifyDataSetChanged();
          }

          @Override
          public void onDownloadTipsDialogShow() {
            ToastUtil.showToast(activity, "广告展示下载合规弹窗");
            LogUtils.recodeCallback(LogUtils.SCENE_FEED_PRE, "onDownloadTipsDialogShow");
          }

          @Override
          public void onDownloadTipsDialogDismiss() {
            ToastUtil.showToast(activity, "广告关闭下载合规弹窗");
            LogUtils.recodeCallback(LogUtils.SCENE_FEED_PRE, "onDownloadTipsDialogDismiss");
          }
        });
        KsAdVideoPlayConfig videoPlayConfig = new KsAdVideoPlayConfig.Builder()
            .dataFlowAutoStart(true) // 是否非WiFi下自动播放
            .videoAutoPlayType(KsAdVideoPlayConfig.VideoAutoPlayType.AUTO_PLAY_WIFI) // 设置有网时自动播放，当与dataFlowAutoStart()同时设置时，以最后一个传入的值为准
            .build();
        ksFeedAd.setVideoPlayConfig(videoPlayConfig);

        /**********DevelopCode Start***********/
        // 模拟竞价
        activity.mIsBidFailed = TestConfigFeedListActivity.testBiddingReport(activity, ksFeedAd);
        /**********DevelopCode End***********/

        ksFeedAd.render(new KsFeedAd.AdRenderListener() {
          @Override
          public void onAdRenderSuccess(View view) {
            LogUtils.recodeCallback(LogUtils.SCENE_FEED_PRE, "onAdRenderSuccess");
            int random = (int) (Math.random() * loadCount) + totalCount - loadCount;
            while (activity.mFeedList.get(random) != null) {
              random = (int) (Math.random() * loadCount) + totalCount - loadCount;
            }
            activity.mFeedList.set(random, ksFeedAd);
            activity.mFeedRecyclerAdapter.notifyDataSetChanged();
          }

          @Override
          public void onAdRenderFailed(int errorCode, String errorMsg) {
            LogUtils.recodeCallback(LogUtils.SCENE_FEED_PRE, "onAdRenderFailed");
          }
        });
      }
      activity.mFeedRecyclerAdapter.notifyDataSetChanged();
    }
  }
}