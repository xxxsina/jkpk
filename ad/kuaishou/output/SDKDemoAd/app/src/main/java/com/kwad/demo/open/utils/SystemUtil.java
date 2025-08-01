package com.kwad.demo.open.utils;

import java.lang.reflect.Method;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;

public class SystemUtil {
  private static String currentProcessName = "";
  private static volatile Boolean sIsMainProcess;

  /**
   * @return 当前进程名
   */
  public static String getProcessName(@NonNull Context context) {
    if (!TextUtils.isEmpty(currentProcessName)) {
      return currentProcessName;
    }

    //1)通过Application的API获取当前进程名
    currentProcessName = getCurrentProcessNameByApplication();
    if (!TextUtils.isEmpty(currentProcessName)) {
      return currentProcessName;
    }

    //2)通过反射ActivityThread获取当前进程名
    currentProcessName = getCurrentProcessNameByActivityThread();
    if (!TextUtils.isEmpty(currentProcessName)) {
      return currentProcessName;
    }

    //3)通过ActivityManager获取当前进程名
    currentProcessName = getCurrentProcessNameByActivityManager(context);

    return currentProcessName;
  }


  /**
   * 通过Application新的API获取进程名，无需反射，无需IPC，效率最高。
   */
  private static String getCurrentProcessNameByApplication() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      return Application.getProcessName();
    }
    return "";
  }

  /**
   * 通过反射ActivityThread获取进程名，避免了ipc
   */
  private static String getCurrentProcessNameByActivityThread() {
    String processName = "";
    try {
      @SuppressLint("PrivateApi") final Method declaredMethod =
          Class.forName("android.app.ActivityThread", false, Application.class.getClassLoader())
              .getDeclaredMethod("currentProcessName", (Class<?>[]) new Class[0]);
      declaredMethod.setAccessible(true);
      final Object invoke = declaredMethod.invoke(null, new Object[0]);
      if (invoke instanceof String) {
        processName = (String) invoke;
      }
    } catch (Throwable ignored) {
    }
    return processName;
  }

  /**
   * 通过ActivityManager 获取进程名，需要IPC通信
   */
  private static String getCurrentProcessNameByActivityManager(@NonNull Context context) {
    int pid = android.os.Process.myPid();
    ActivityManager am =
        (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    if (am != null) {
      List<ActivityManager.RunningAppProcessInfo> runningAppList =
          am.getRunningAppProcesses();
      if (runningAppList != null) {
        for (ActivityManager.RunningAppProcessInfo processInfo : runningAppList) {
          if (processInfo.pid == pid) {
            return processInfo.processName;
          }
        }
      }
    }
    return "";
  }


  /**
   * 判断当前进程是否主进程，主要是启动阶段在主线程调用
   */
  public static boolean isMainProcess(Context context) {
    if (sIsMainProcess == null) {
      String pName = getProcessName(context);
      sIsMainProcess =
          !TextUtils.isEmpty(pName) && pName.equals(context.getPackageName());
    }
    return sIsMainProcess;
  }
}