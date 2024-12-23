package com.example.sample.flow.files;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.R;
import android.content.res.Resources;

public class getDrawableSample extends Activity {
    private int theme;

    private void getThemeResource() {
        // AppTheme のスタイル ID を theme 変数に設定
        this.theme = R.style.Theme;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // テーマを設定
        getThemeResource();

        // Drawable リソース ID を設定
        int drawableResId = R.drawable.title_bar;
        Resources resources = getResources();
        Drawable drawable;

        // SDK バージョンに応じて Drawable を取得
        if (Build.VERSION.SDK_INT >= 22) {
            drawable = resources.getDrawable(drawableResId, new ContextThemeWrapper(this, theme).getTheme());
        } else {
            drawable = resources.getDrawable(drawableResId);
        }

        // Drawable の使用（例: View にセットなど）
    }
}

