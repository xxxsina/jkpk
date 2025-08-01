package com.kwad.demo.open.view;

import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * 功能描述：
 * 1.圆角能力 {@link #setRadius(float)}
 */
public class TestKSFrameLayout extends FrameLayout {
  private static final String TAG = "KSFrameLayout";
  private final AtomicBoolean mIsViewDetached = new AtomicBoolean(true);
  private ViewRCHelper mViewRCHelper;
  //Dialog中根布局设置圆角，会导致布局裁剪异常，不要使用该属性(oppo Reno4)

  public TestKSFrameLayout(@NonNull Context context) {
    super(context);
    init(context, null);
  }

  public TestKSFrameLayout(@NonNull Context context,
      @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(context, attrs);
  }

  public TestKSFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs,
      int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context, attrs);
  }

  protected void init(@NonNull Context context, @Nullable AttributeSet attrs) {
    mViewRCHelper = new ViewRCHelper();
    mViewRCHelper.initAttrs(context, attrs);
  }


  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    mViewRCHelper.onSizeChanged(w, h);
  }

  @Override
  public void draw(Canvas canvas) {
    mViewRCHelper.beforeDraw(canvas);
    super.draw(canvas);
    mViewRCHelper.afterDraw(canvas);
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    mViewRCHelper.beforeDispatchDraw(canvas);
    super.dispatchDraw(canvas);
    mViewRCHelper.afterDispatchDraw(canvas);
  }

  /**
   * 设置圆角, 不能设置0圆角，恢复0使用setAllCorner 设置
   * Dialog中根布局设置圆角，会导致布局裁剪异常，不要使用该属性(oppo Reno4)
   */
  public void setRadius(float radius) {
    mViewRCHelper.setRadius(radius);
    postInvalidate();
  }

  /**
   * 设置圆角
   * Dialog中根布局设置圆角，会导致布局裁剪异常，不要使用该属性(oppo Reno4)
   */
  public void setRadius(float topLeftRadius, float topRightRadius, float bottomRightRadius,
      float bottomLeftRadius) {
    float[] radius = getRadius(topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius);
    mViewRCHelper.setRadius(radius);
    postInvalidate();
  }

  private float[] getRadius(float topLeftRadius, float topRightRadius, float bottomRightRadius,
      float bottomLeftRadius) {
    float[] radiusArray = new float[8];
    radiusArray[0] = topLeftRadius;
    radiusArray[1] = topLeftRadius;
    radiusArray[2] = topRightRadius;
    radiusArray[3] = topRightRadius;
    radiusArray[4] = bottomRightRadius;
    radiusArray[5] = bottomRightRadius;
    radiusArray[6] = bottomLeftRadius;
    radiusArray[7] = bottomLeftRadius;
    return radiusArray;
  }
}
