package com.kwad.demo.open.feed;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Switch;

import com.kwad.demo.R;
import com.kwad.demo.open.KSSdkInitUtil;
import com.kwad.demo.open.TestPosId;
import com.kwad.demo.open.utils.TestSpUtil;
import com.kwad.demo.open.utils.ToastUtil;

public class FeedHomeActivity extends Activity {
  public static final String POS_ID = "posId";
  public static final String NEED_ADJUST_WIDTH = "needAdjustWidth";

  public static final String NEED_CHANGE_BACKGROUND_COLOR = "needChangeBackgroundColor";

  private List<TestPosId> mTestConfigPosIdList;
  private long configPosId;
  private int mSelect;
  private View mBackBtn;
  private Switch mAdjustWidthSwitch; //是否动态调整宽度

  private Switch mChangeBackgroundColorSwitch;//是否改变背景颜色

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_feed);
    mAdjustWidthSwitch = findViewById(R.id.show_width_switch);
    mChangeBackgroundColorSwitch = findViewById(R.id.change_background_color_switch);

    /******** 自定义模板 start ********/
    mTestConfigPosIdList = new ArrayList<>();
    mTestConfigPosIdList.add(TestPosId.POSID_CONFIG_FEED_TYPE_1);
    mTestConfigPosIdList.add(TestPosId.POSID_CONFIG_FEED_TYPE_2);
    mTestConfigPosIdList.add(TestPosId.POSID_CONFIG_FEED_TYPE_3);
    mTestConfigPosIdList.add(TestPosId.POSID_CONFIG_FEED_TYPE_4);
    mTestConfigPosIdList.add(TestPosId.POSID_CONFIG_FEED_TYPE_5);
    mTestConfigPosIdList.add(TestPosId.POSID_CONFIG_FEED_TYPE_9);
    mTestConfigPosIdList.add(TestPosId.POSID_CONFIG_FEED_TYPE_10);
    mTestConfigPosIdList.add(TestPosId.POSID_CONFIG_FEED_TYPE_11);
    mTestConfigPosIdList.add(TestPosId.POSID_CONFIG_FEED_TYPE_12);
    mTestConfigPosIdList.add(TestPosId.POSID_CONFIG_FEED_TYPE_13);
    mTestConfigPosIdList.add(TestPosId.POSID_CONFIG_FEED_TYPE_14);
    mTestConfigPosIdList.add(TestPosId.POSID_CONFIG_FEED_TYPE_15);
    configPosId = TestPosId.POSID_CONFIG_FEED_TYPE_5.posId;
    /******** 自定义模板 end ********/

    mSelect = 4;
    mBackBtn = findViewById(R.id.ksad_main_left_back_btn);
    if (mBackBtn != null) {
      mBackBtn.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          finish();
        }
      });
    }
  }

  /**
   * 测试信息流，自定义模板+ListView实现
   */
  public void testConfigFeedList(View view) {
    configMulCheckDialog(new Runnable() {
      @Override
      public void run() {
        Runtime.getRuntime().gc();

        Intent intent = new Intent(FeedHomeActivity.this,
            TestConfigFeedListActivity.class);
        intent.putExtra("posId", configPosId);
        intent.putExtra(NEED_ADJUST_WIDTH, mAdjustWidthSwitch.isChecked());
        intent.putExtra(NEED_CHANGE_BACKGROUND_COLOR, mChangeBackgroundColorSwitch.isChecked());
        startActivity(intent);
      }
    });
  }

  /**
   * 测试信息流，自定义模板+RecyclerView实现
   */
  public void testConfigFeedRecycler(View view) {
    configMulCheckDialog(new Runnable() {
      @Override
      public void run() {
        Runtime.getRuntime().gc();

        Intent intent = new Intent(
            FeedHomeActivity.this, TestConfigFeedRecyclerActivity.class);
        intent.putExtra(POS_ID, configPosId);
        intent.putExtra(NEED_ADJUST_WIDTH, mAdjustWidthSwitch.isChecked());
        intent.putExtra(NEED_CHANGE_BACKGROUND_COLOR, mChangeBackgroundColorSwitch.isChecked());
        startActivity(intent);
      }
    });
  }

  private void configMulCheckDialog(final Runnable callBack) {
    final String[] items = getResources().getStringArray(R.array.select_config_feed_array);

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("选择模版")
        .setSingleChoiceItems(items, mSelect, new DialogInterface.OnClickListener() {

          @Override
          public void onClick(DialogInterface dialog, int which) {
            mSelect = which;
          }
        })
        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            if (mSelect >= 0 && mSelect < mTestConfigPosIdList.size()) {
              configPosId = mTestConfigPosIdList.get(mSelect).posId;
              ToastUtil.showToast(FeedHomeActivity.this, "选择了" + items[mSelect]);
            }

            callBack.run();
          }
        })
        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
          }
        });

    builder.create().show();
  }
}
