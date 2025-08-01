package com.kwad.demo.open.serverBid;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.kwad.demo.open.utils.ToastUtil;

/**
 * Server bidding 模拟媒体adx服务端请求快手联盟服务端、进行广告比价及返回bidResponse给媒体app的工具类
 */
public class BiddingDemoUtils {

  private static final String TAG = BiddingDemoUtils.class.getSimpleName();
  private static final String SERVER_BIDDING_URL =
      "https://open.e.kuaishou.com/rest/e/v4/open/univ";
  private static final String SERVER_BIDDING_URL_V2 =
      "https://open.e.kuaishou.com/rest/e/v4/open/univ/bidding";

  public static final ExecutorService SINGLE_THREAD_EXECUTOR = Executors.newSingleThreadExecutor();

  private static Handler sHandler = new Handler(Looper.getMainLooper());

  public static void fetchBidResponse(final Context context, final long posId, final String token,
      final boolean isServerBidV2, final FetchResponseCallback callback) {
    SINGLE_THREAD_EXECUTOR.execute(new Runnable() {
      @Override
      public void run() {
        try {
          OutputStream outputStream;
          HttpURLConnection connection = (HttpURLConnection) new URL(
              isServerBidV2 ? SERVER_BIDDING_URL_V2 : SERVER_BIDDING_URL).openConnection();
          connection.setDoOutput(true);
          connection.setDoInput(true);
          // 设置请求头
          connection.setRequestProperty("Content-Type", "application/json");
          // 内部调试使用
          appendTokenHeader(token, connection);
          String postData = buildPostData(token, posId);
          Log.d(TAG, "post_data: " + postData);
          connection.setConnectTimeout(5000);
          connection.setReadTimeout(5000);
          connection.setUseCaches(false);
          if (!TextUtils.isEmpty(postData)) {
            // 获取输出流,connection.getOutputStream已经包含了connect方法的调用
            outputStream = connection.getOutputStream();
            // 使用输出流将string类型的参数写到服务器
            outputStream.write(postData.getBytes());
            outputStream.flush();
          }

          int responseCode = connection.getResponseCode();
          if (responseCode == HttpURLConnection.HTTP_OK) {
            // 获取快手联盟服务端的响应
            String tmpStr = getStringContent(connection);
            Log.d(TAG, "response: " + tmpStr);
            Gson gson = new Gson();
            BidResponse response = gson.fromJson(tmpStr, BidResponse.class);
            if (response.adBids == null || response.adBids.isEmpty()) {
              showToast(context, "回包中竞价信息为空");
            } else {
              for (BidResponse.AdBid adBid : response.adBids) {
                // 取列表中的竞价信息项，与其他sdk比价，若快手竞胜，将竞胜价格填充到bidEcpm中
                adBid.bidEcpm = 100; // 填入竞胜价格
                adBid.winNoticeUrl
                    .replace("WIN_PRICE", String.valueOf(100)); // 替换url中的 WIN_PRICE, 并请求该url通知快手服务端
              }
              showToast(context, isServerBidV2 ? "成功获取竞价信息和广告素材id" : "成功获取竞价信息和广告素材");
              if (callback != null) {
                callback.onSuccess(gson.toJson(response)); // 回传竞价信息给快手SDK，用于拉取真实广告素材
              }
            }
          } else {
            showToast(context, "获取竞价信息失败");
          }
        } catch (IOException e) {
          showToast(context, "获取竞价信息失败");
          e.printStackTrace();
        }
      }
    });
  }

  /**
   * 附加参数到header中，仅限于内部调试用
   *
   * @param token
   * @param connection
   */
  private static void appendTokenHeader(String token, HttpURLConnection connection) {
    try {
      JSONObject object = new JSONObject(token);
      String trace = object.optString("trace-context");

      Log.d("jky", "appendTokenHeader: trace: " + trace);
      if (!TextUtils.isEmpty(trace)) {
        connection.setRequestProperty("trace-context", trace);
      }
    } catch (JSONException e) {

    }
  }

  public static byte[] getBytesContent(HttpURLConnection connection) throws IllegalStateException
      , IOException {
    InputStream in = connection.getInputStream();
    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    try {
      byte[] buffer = new byte[1024];
      int len = 0;
      while ((len = (in.read(buffer))) > 0) {
        bo.write(buffer, 0, len);
      }
    } finally {
      try {
        if (in != null) {
          in.close();
        }
      } catch (IOException ignore) {
      }
    }
    return bo.toByteArray();
  }

  public static String getStringContent(HttpURLConnection connection) throws IOException {
    byte[] bytes = getBytesContent(connection);
    if (bytes == null) {
      return null;
    } else if (bytes.length == 0) {
      return "";
    }

    String charset = null;
    try {
      charset = connection.getContentEncoding();
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (charset == null) {
      charset = "UTF-8";
    }
    return new String(bytes, charset);
  }

  // 媒体根据需要填入实际的请求参数，此处仅为示例
  private static String buildPostData(String token, long posId) {
    BidRequest bidRequest = new BidRequest();
    bidRequest.ip = "123.12.2.12";
    bidRequest.adxId = "100023";
    bidRequest.sdkToken = token;
    BidRequest.AdImpInfo adImpInfo = new BidRequest.AdImpInfo();
    adImpInfo.posId = posId;
    adImpInfo.adCount = 1;
    adImpInfo.cpmBidFloor = 1;
    bidRequest.adImpInfos = new ArrayList<>();
    bidRequest.adImpInfos.add(adImpInfo);
    Gson gson = new Gson();
    return gson.toJson(bidRequest);
  }

  private static void showToast(final Context context, final String msg) {
    sHandler.post(new Runnable() {
      @Override
      public void run() {
        ToastUtil.showToast(context, msg);
      }
    });
  }

  public interface FetchResponseCallback {
    void onSuccess(String bidResponse);
  }
}
