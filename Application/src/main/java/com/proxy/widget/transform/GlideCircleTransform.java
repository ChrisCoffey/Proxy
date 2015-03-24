package com.proxy.widget.transform;

import android.content.Context;
import android.graphics.Bitmap;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.proxy.util.ViewUtils;

import static com.proxy.util.DebugUtils.getSimpleName;

public class GlideCircleTransform extends BitmapTransformation {
    private final Context context;
    private static final String TAG = getSimpleName(GlideCircleTransform.class);

    public GlideCircleTransform(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    protected Bitmap transform(BitmapPool pool, Bitmap source, int outWidth, int outHeight) {
        return ViewUtils.getCircularBitmapImage(context, gti source);
    }

    @Override
    public String getId() {
        return "Glide_Circle_Transformation";
    }
}