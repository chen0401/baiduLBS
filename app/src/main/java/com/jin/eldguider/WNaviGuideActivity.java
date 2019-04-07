/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.jin.eldguider;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.baidu.mapapi.walknavi.WalkNavigateHelper;
import com.baidu.mapapi.walknavi.adapter.IWNaviStatusListener;
import com.baidu.mapapi.walknavi.adapter.IWRouteGuidanceListener;
import com.baidu.mapapi.walknavi.adapter.IWTTSPlayer;
import com.baidu.mapapi.walknavi.model.RouteGuideKind;
import com.baidu.platform.comapi.walknavi.WalkNaviModeSwitchListener;
import com.baidu.platform.comapi.walknavi.widget.ArCameraView;


public class WNaviGuideActivity extends Activity {

    private final static String TAG = "walkingguide";

    private WalkNavigateHelper mNaviHelper;

    private volatile static int deviateCount = 0;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mNaviHelper.quit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNaviHelper.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNaviHelper.pause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNaviHelper = WalkNavigateHelper.getInstance();

        try {
            View view = mNaviHelper.onCreate(WNaviGuideActivity.this);
            if (view != null) {
                setContentView(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mNaviHelper.setWalkNaviStatusListener(new IWNaviStatusListener() {
            @Override
            public void onWalkNaviModeChange(int mode, WalkNaviModeSwitchListener listener) {
                Log.d(TAG, "onWalkNaviModeChange : " + mode);
                mNaviHelper.switchWalkNaviMode(WNaviGuideActivity.this, mode, listener);
            }

            @Override
            public void onNaviExit() {
                Log.d(TAG, "onNaviExit");
            }
        });

        mNaviHelper.setTTsPlayer(new IWTTSPlayer() {
            @Override
            public int playTTSText(final String s, boolean b) {
                Log.d(TAG, "tts: " + s);
                return 0;
            }
        });

        boolean startResult = mNaviHelper.startWalkNavi(WNaviGuideActivity.this);
        Log.e(TAG, "startWalkNavi result : " + startResult);

        mNaviHelper.setRouteGuidanceListener(this, new IWRouteGuidanceListener() {
            @Override
            public void onRouteGuideIconUpdate(Drawable icon) {

            }

            @Override
            public void onRouteGuideKind(RouteGuideKind routeGuideKind) {
                Log.d(TAG, "onRouteGuideKind: " + routeGuideKind);
            }

            @Override
            public void onRoadGuideTextUpdate(CharSequence charSequence, CharSequence charSequence1) {
                Log.d(TAG, "onRoadGuideTextUpdate   charSequence=: " + charSequence + "   charSequence1 = : " +
                        charSequence1);

            }

            @Override
            public void onRemainDistanceUpdate(CharSequence charSequence) {
                Log.d(TAG, "onRemainDistanceUpdate: charSequence = :" + charSequence);

            }

            @Override
            public void onRemainTimeUpdate(CharSequence charSequence) {
                Log.d(TAG, "onRemainTimeUpdate: charSequence = :" + charSequence);

            }

            @Override
            public void onGpsStatusChange(CharSequence charSequence, Drawable drawable) {
                Log.d(TAG, "onGpsStatusChange: charSequence = :" + charSequence);

            }

            @Override
            public void onRouteFarAway(CharSequence charSequence, Drawable drawable) {
                Log.d(TAG, "onRouteFarAway: charSequence = :" + charSequence);
                deviateCount++;
                if (deviateCount >= 3) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(WNaviGuideActivity.this);
                    builder.setTitle("短信警告");
                    builder.setMessage("您已偏离导航3次");
                    builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deviateCount = 0;
                            dialog.dismiss();
                        }
                    });
                    builder.create().show();
                }
            }

            @Override
            public void onRoutePlanYawing(CharSequence charSequence, Drawable drawable) {
                Log.d(TAG, "onRoutePlanYawing: charSequence = :" + charSequence);

            }

            @Override
            public void onReRouteComplete() {
                deviateCount = 0;
            }

            @Override
            public void onArriveDest() {

            }

            @Override
            public void onIndoorEnd(Message msg) {

            }

            @Override
            public void onFinalEnd(Message msg) {
                deviateCount = 0;
            }

            @Override
            public void onVibrate() {

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ArCameraView.WALK_AR_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(WNaviGuideActivity.this, "没有相机权限,请打开后重试", Toast.LENGTH_SHORT).show();
            } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mNaviHelper.startCameraAndSetMapView(WNaviGuideActivity.this);
            }
        }
    }
}
