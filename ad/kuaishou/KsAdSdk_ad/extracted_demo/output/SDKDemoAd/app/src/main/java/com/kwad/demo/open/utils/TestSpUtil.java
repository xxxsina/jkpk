package com.kwad.demo.open.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.kwad.demo.open.DemoApplication;

/**
 * 本地配置信息存储的工具类
 */
public class TestSpUtil {
  private static final String PREFERENCE_NAME = "kssdk_demo";

  @SuppressLint("ApplySharedPref")
  public static void setInt(Context context, String key, int value) {
    SharedPreferences prefs = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
    prefs.edit().putInt(key, value).commit();
  }

  public static void savaInt(String key, int value) {
    getSPEditor().putInt(key, value).apply();
  }

  public static int getInt(String key, int defValue) {
    return getSP().getInt(key, defValue);
  }

  public static void savaBoolean(String key, boolean value) {
    getSPEditor().putBoolean(key, value).apply();
  }

  public static boolean getBoolean(String key, boolean defValue) {
    return getSP().getBoolean(key, defValue);
  }

  public static boolean getBoolean(String key) {
    return getSP().getBoolean(key, false);
  }

  public static void savaString(String key, String value) {
    getSPEditor().putString(key, value).apply();
  }

  public static String getString(String key) {
    return getSP().getString(key, "");
  }

  private static SharedPreferences.Editor getSPEditor() {
    return getSP().edit();
  }

  private static SharedPreferences getSP() {
    return DemoApplication.sContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
  }
}
