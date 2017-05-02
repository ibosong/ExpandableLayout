package com.bosong.sample;

import android.content.Context;

/**
 * Created by bosong on 4/21/2017.
 */

public class Utils {
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
