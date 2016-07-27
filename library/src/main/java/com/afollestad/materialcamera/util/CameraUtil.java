package com.afollestad.materialcamera.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.afollestad.materialcamera.ICallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * @author Aidan Follestad (afollestad)
 */
public class CameraUtil {

    private CameraUtil() {
    }

    public static boolean isArcWelder() {
        return Build.BRAND.equalsIgnoreCase("chromium") &&
                Build.MANUFACTURER.equalsIgnoreCase("chromium");
    }

    public static String getDurationString(long durationMs) {
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(durationMs),
                TimeUnit.MILLISECONDS.toSeconds(durationMs) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(durationMs))
        );
    }

    @SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
    public static File makeTempFile(@NonNull Context context, @Nullable String saveDir, String prefix, String extension) {
        if (saveDir == null)
            saveDir = context.getExternalCacheDir().getAbsolutePath();
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        final File dir = new File(saveDir);
        dir.mkdirs();
        return new File(dir, prefix + timeStamp + extension);
    }

    public static boolean hasCamera(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) ||
                context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }

    public static List<String> getSupportedFlashModes(Context context, Camera.Parameters parameters) {
        //check has system feature for flash
        if(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            List<String> modes = parameters.getSupportedFlashModes();
            if(modes.size() == 1 && modes.get(0).equals(parameters.FLASH_MODE_OFF)){
                return null; //not supported
            } else {
                return modes;
            }
        } else {
            return null; //not supported
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static boolean hasCamera2(Context context) {
        if (context == null) return false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return false;
        try {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            String[] idList = manager.getCameraIdList();
            boolean notNull = true;
            if (idList.length == 0) {
                notNull = false;
            } else {
                for (final String str : idList) {
                    if (str == null || str.trim().isEmpty()) {
                        notNull = false;
                        break;
                    }
                    final CameraCharacteristics characteristics = manager.getCameraCharacteristics(str);
                    //noinspection ConstantConditions
                    final int supportLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                    if (supportLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                        notNull = false;
                        break;
                    }
                }
            }
            return notNull;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    @ColorInt
    public static int darkenColor(@ColorInt int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f; // value component
        color = Color.HSVToColor(hsv);
        return color;
    }
}
