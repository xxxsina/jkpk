package com.kwad.demo.open.splash;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.kwad.demo.R;
import com.kwad.demo.open.KSSdkInitUtil;
import com.kwad.demo.open.TestPosId;
import com.kwad.demo.open.serverBid.BiddingDemoUtils;
import com.kwad.demo.open.utils.TestSpUtil;
import com.kwad.demo.open.utils.ToastUtil;
import com.kwad.sdk.api.KsScene;

public class SplashHomeActivity extends Activity implements View.OnClickListener {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_splash_home);
    findViewById(R.id.ksad_main_left_back_btn).setOnClickListener(this);
  }

  public void testSplashNormal(View view) {
    Intent intent = new Intent(SplashHomeActivity.this, TestSplashScreenViewActivity.class);
    intent.putExtra(TestSplashScreenViewActivity.KEY_GO_TO_MAIN, false);
    intent.putExtra(TestSplashScreenViewActivity.KEY_BID_RESPONSE, mBidResponseV1);
    intent.putExtra(TestSplashScreenViewActivity.KEY_BID_RESPONSE_V2, mBidResponseV2);
    startActivity(intent);
  }

  public void testSplashGoToMain(View view) {
    Intent intent = new Intent(SplashHomeActivity.this, TestSplashScreenViewActivity.class);
    intent.putExtra(TestSplashScreenViewActivity.KEY_GO_TO_MAIN, true);
    startActivity(intent);
  }

  public void testSplashNormalLandscape(View view) {
    Intent intent = new Intent(SplashHomeActivity.this, TestSplashScreenViewActivity.class);
    intent.putExtra(TestSplashScreenViewActivity.KEY_GO_TO_MAIN, false);
    intent.putExtra(TestSplashScreenViewActivity.KEY_IS_LANDSCAPE, 1);
    intent.putExtra(TestSplashScreenViewActivity.KEY_BID_RESPONSE, mBidResponseV1);
    intent.putExtra(TestSplashScreenViewActivity.KEY_BID_RESPONSE_V2, mBidResponseV2);
    startActivity(intent);
  }

  /********   服务端竞价模拟 start   ********/
  private String mBidResponseV1;
  private String mBidResponseV2;

  // 获取服务端竞价信息
  public void fetchBidResponseV1(View view) {
    KsScene ksScene = KSSdkInitUtil.createKSSceneBuilder(TestPosId.POSID_SPLASHSCREEN.posId)
        .build(); // 根据需要传入场景参数，需传入有效posId
    String token = KSSdkInitUtil.getLoadManager().getBidRequestToken(ksScene);
    BiddingDemoUtils.fetchBidResponse(this, TestPosId.POSID_SPLASHSCREEN.posId, token, false,
        new BiddingDemoUtils.FetchResponseCallback() {
          @Override
          public void onSuccess(String bidResponse) {
            mBidResponseV1 = bidResponse;
          }
        });
  }

  public void showAdV1(View view) {
    if (TextUtils.isEmpty(mBidResponseV1)) {
      ToastUtil.showToast(SplashHomeActivity.this, "请先获取竞价信息");
      return;
    }
    testSplashNormal(view);
  }

  public void fetchBidResponseV2(View view) {
    KsScene ksScene = KSSdkInitUtil.createKSSceneBuilder(0)
        .build(); // 根据需要传入场景参数，注意：创建KsScene时 posId 可传无效值，在adx服务端拉取快手竞价信息时必须传有效的 posId
    String token = KSSdkInitUtil.getLoadManager().getBidRequestTokenV2(ksScene);
    BiddingDemoUtils.fetchBidResponse(this, TestPosId.POSID_SPLASHSCREEN.posId, token, true,
        new BiddingDemoUtils.FetchResponseCallback() {
          @Override
          public void onSuccess(String bidResponse) {
            mBidResponseV2 = bidResponse;
          }
        });
  }

  public void showAdV2(View view) {
    if (TextUtils.isEmpty(mBidResponseV2)) {
      ToastUtil.showToast(SplashHomeActivity.this, "请先获取竞价信息");
      return;
    }
    testSplashNormal(view);
  }

  /********   服务端竞价模拟 end     ********/

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.ksad_main_left_back_btn:
        finish();
        break;
    }
  }
}