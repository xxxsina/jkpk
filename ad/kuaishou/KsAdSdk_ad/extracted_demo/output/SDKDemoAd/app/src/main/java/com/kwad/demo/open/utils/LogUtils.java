package com.kwad.demo.open.utils;

import android.util.Log;

import com.kwad.demo.BuildConfig;

/**
 * Created by shihongyu on 2022/8/12.
 * Describe: 测试自动化需要的日志工具
 */
public class LogUtils {
  /**
   * 场景前缀字段
   */
  public static final String SCENE_INSERT_PRE = "insertAd_";
  public static final String SCENE_FULL_PRE   = "fullAd_";
  public static final String SCENE_REWARD_PRE = "rewardAd_";
  public static final String SCENE_SPLASH_PRE = "splashAd_";
  public static final String SCENE_DRAW_PRE   = "drawAd_";
  public static final String SCENE_FEED_PRE   = "feedAd_";
  public static final String SCENE_NATIVE_PRE = "nativeAd_";
  public static final String SCENE_BANNER_PRE = "banner_";
  //记录各种回调 tag
  public static final String TAG_AUTOMATION_CALLBACK = "callbackLog";
  public static final String TAG_FUNC_LOG = "funcLog";

  public static void d(String tag, String msg) {
  }

  public static void w(String tag, String msg) {
  }

  public static void e(String tag, String msg) {
  }

  /**
   * 回调日志
   */
  public static void recodeCallback(String scene, String methodName) {
    e(TAG_AUTOMATION_CALLBACK, scene + methodName);
  }
  /**
   * 回调日志
   */
  public static void recodeFuncLog(String scene, String methodName) {
    e(TAG_FUNC_LOG, scene + methodName);
  }
}
