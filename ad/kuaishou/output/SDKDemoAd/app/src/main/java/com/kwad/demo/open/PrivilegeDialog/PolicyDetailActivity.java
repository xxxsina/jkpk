package com.kwad.demo.open.PrivilegeDialog;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kwad.demo.R;

/**
 * 展示隐私政策及SDK使用规范的Activity
 */
public class PolicyDetailActivity extends Activity {

  private static final String KEY_URL = "page_url";
  private static final String KEY_TITLE = "page_title";
  private static final int PROGRESS_MAX = 100;

  public static void launch(Context context, String pageTitle, String pageUrl) {
    Intent intent = new Intent(context, PolicyDetailActivity.class);
    intent.putExtra(KEY_TITLE, pageTitle);
    intent.putExtra(KEY_URL, pageUrl);
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_policy_detail);
    String pageTitle = getIntent().getStringExtra(KEY_TITLE);
    String pageUrl = getIntent().getStringExtra(KEY_URL);
    TextView tvTitle = findViewById(R.id.tv_title);
    tvTitle.setText(pageTitle);
    findViewById(R.id.iv_back).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onBackPressed();
      }
    });
    WebView webView = findViewById(R.id.web_view);
    webView.getSettings().setJavaScriptEnabled(true);
    webView.loadUrl(pageUrl);
    webView.setWebViewClient(new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        view.loadUrl(url);
        return super.shouldOverrideUrlLoading(view, url);
      }
    });
    final ProgressBar progressBar = findViewById(R.id.progress_bar);
    webView.setWebChromeClient(new WebChromeClient() {
      @Override
      public void onProgressChanged(WebView view, int newProgress) {
        if (newProgress == PROGRESS_MAX) {
          progressBar.setVisibility(View.GONE);
        } else {
          progressBar.setVisibility(View.VISIBLE);
          progressBar.setProgress(newProgress);
        }
        super.onProgressChanged(view, newProgress);
      }
    });
  }
}