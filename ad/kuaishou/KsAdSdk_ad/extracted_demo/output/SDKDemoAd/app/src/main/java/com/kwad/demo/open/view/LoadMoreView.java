package com.kwad.demo.open.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kwad.demo.R;


public class LoadMoreView extends FrameLayout {
  private ProgressBar mProgressBar;
  private TextView mTextView;

  public LoadMoreView(@NonNull Context context) {
    this(context, null);
  }

  public LoadMoreView(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public LoadMoreView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  private void init(Context context) {
    LayoutInflater.from(context).inflate(R.layout.load_more_view, this, true);
    mProgressBar = findViewById(R.id.load_more_progress);
    mTextView = findViewById(R.id.load_more_tip);
  }

  public void showLoading() {
    mTextView.setText("加载中...");
    mProgressBar.setVisibility(VISIBLE);
  }

  public void showError() {
    mTextView.setText("加载失败，请重试");
    mProgressBar.setVisibility(GONE);
  }
}
