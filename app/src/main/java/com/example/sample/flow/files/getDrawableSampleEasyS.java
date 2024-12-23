package com.example.sample.flow.files;

import android.R;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;

public class getDrawableSampleEasy extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Drawable リソース ID を設定
        int drawableResId = R.drawable.title_bar;
        Resources resources = getResources();
        Drawable drawable;
        int theme = R.style.Theme;

        // SDK バージョンに応じて Drawable を取得
        if (Build.VERSION.SDK_INT >= 22) {
            drawable = resources.getDrawable(drawableResId, new ContextThemeWrapper(this, theme).getTheme());
        } else {
            drawable = resources.getDrawable(drawableResId);
        }

        // Drawable の使用（例: View にセットなど）
        System.out.println("hoge");
    }
}
