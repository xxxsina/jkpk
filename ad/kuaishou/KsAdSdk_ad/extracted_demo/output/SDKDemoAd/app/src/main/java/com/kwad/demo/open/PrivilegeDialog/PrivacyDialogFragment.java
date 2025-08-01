package com.kwad.demo.open.PrivilegeDialog;

import android.app.Activity;
import android.app.DialogFragment;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.kwad.demo.R;
import com.kwad.demo.open.utils.SpannableStringBuilderUtil;

/**
 * 隐私政策及sdk使用规范弹窗
 */
public class PrivacyDialogFragment extends DialogFragment implements View.OnClickListener{

  private static final String PRIVACY_TERMS_URL = "https://ali-ec.static.yximgs.com/kos/nlav11213/hybrid/demo-user-privacy/index.html";
  private static final String USER_SERVICE_URL = "https://ali-ec.static.yximgs.com/kos/nlav11213/hybrid/demo-use-standard/index.html";
  private PrivacyReadStateListener mPrivacyReadListener;

  public static void showPrivacyDialog(Activity activity, PrivacyReadStateListener privacyReadListener) {
    PrivacyDialogFragment fragment = new PrivacyDialogFragment();
    fragment.setPrivacyReadStateListener(privacyReadListener);
    fragment.show(activity.getFragmentManager(), "privacy");
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.layout_dialog_privacy, container, false);
    initView(rootView);
    return rootView;
  }

  private void initView(View rootView) {
    rootView.findViewById(R.id.iv_close).setOnClickListener(this);
    rootView.findViewById(R.id.tv_not_accept).setOnClickListener(this);
    rootView.findViewById(R.id.tv_accept_and_continue).setOnClickListener(this);

    TextView tvContent = rootView.findViewById(R.id.tv_content);
    tvContent.setText(createPrivacyContent());
    tvContent.setMovementMethod(LinkMovementMethod.getInstance());
  }

  private CharSequence createPrivacyContent() {
    return new SpannableStringBuilderUtil()
        .append(getString(R.string.privacy_policy_pre))
        .append(getString(R.string.privacy_terms_and_policy))
        .setSpan(new ClickableSpan() {
          @Override
          public void onClick(@NonNull View widget) {
            PolicyDetailActivity.launch(getActivity(), getString(R.string.privacy_terms_and_policy), PRIVACY_TERMS_URL);
          }

          @Override
          public void updateDrawState(@NonNull TextPaint ds) {
            super.updateDrawState(ds);
            ds.setColor(getResources().getColor(R.color.link_color));
            ds.setUnderlineText(false);
          }
        })
        .append(getString(R.string.and))
        .append(getString(R.string.user_service_protocol))
        .setSpan(new ClickableSpan() {
          @Override
          public void onClick(@NonNull View widget) {
            PolicyDetailActivity.launch(getActivity(), getString(R.string.user_service_protocol), USER_SERVICE_URL);
          }

          @Override
          public void updateDrawState(@NonNull TextPaint ds) {
            super.updateDrawState(ds);
            ds.setColor(getResources().getColor(R.color.link_color));
            ds.setUnderlineText(false);
          }
        })
        .append(getString(R.string.privacy_policy_post))
        .build();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    Window window = getDialog().getWindow();
    if (window == null) {
      return;
    }
    getDialog().setCanceledOnTouchOutside(false);
    window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    window.getDecorView().setPadding(0, 0, 0, 0);
    window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
  }

  @Override
  public void onClick(View v) {
     switch (v.getId()) {
       case R.id.iv_close:
       case R.id.tv_not_accept:
         dismiss();
         if (mPrivacyReadListener != null) {
           mPrivacyReadListener.onNotAccept();
         }
         break;
       case R.id.tv_accept_and_continue:
         dismiss();
         if (mPrivacyReadListener != null) {
           mPrivacyReadListener.onAccept();
         }
         break;
       default:
         break;
     }
  }

  public void setPrivacyReadStateListener(PrivacyReadStateListener privacyReadListener) {
    mPrivacyReadListener = privacyReadListener;
  }

  public interface PrivacyReadStateListener {
    void onNotAccept();
    void onAccept();
  }
}
