package com.hello.sandbox.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import top.niunaijun.blackboxa.R;

public class FakeHomeView extends FrameLayout {

  public FakeHomeView(@NonNull Context context) {
    super(context);
    initView(context);
  }

  public FakeHomeView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    initView(context);
  }

  public FakeHomeView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initView(context);
  }

  public FakeHomeView(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    initView(context);
  }

  public void initView(Context context) {
    LayoutInflater.from(context).inflate(R.layout.fake_home_view, this, true);
    setContent(context);
  }

  private void setContent(Context context) {
    ((ImageView) findViewById(R.id.icon))
        .setImageDrawable(getResources().getDrawable(R.drawable.home_icon_add));
    ((TextView) findViewById(R.id.name)).setText("添加应用");
    findViewById(R.id.cornerLabel).setVisibility(View.INVISIBLE);
  }

}
