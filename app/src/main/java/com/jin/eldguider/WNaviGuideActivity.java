/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.jin.eldguider;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.baidu.mapapi.walknavi.WalkNavigateHelper;
import com.baidu.mapapi.walknavi.adapter.IWNaviStatusListener;
import com.baidu.mapapi.walknavi.adapter.IWRouteGuidanceListener;
import com.baidu.mapapi.walknavi.adapter.IWTTSPlayer;
import com.baidu.mapapi.walknavi.model.RouteGuideKind;
import com.baidu.platform.comapi.walknavi.WalkNaviModeSwitchListener;
import com.baidu.platform.comapi.walknavi.widget.ArCameraView;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;


public class WNaviGuideActivity extends Activity {

    private final static String TAG = "walkingguide";

    private WalkNavigateHelper mNaviHelper;

    private volatile static int deviateCount = 0;

    private static View view;

    private final static String BAIDU_OAUTH_URL = "https://openapi.baidu.com/oauth/2.0/token?";
    private final static String BAIDU_VOICE_URL = "https://tsn.baidu.com/text2audio?";
    private final static String APP_ID = "15964912";
    private final static String APP_KEY = "";

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
            view = mNaviHelper.onCreate(WNaviGuideActivity.this);
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
                showWarning();
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
                voice(s);
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
                Log.d(TAG, "road guide text update");
            }

            @Override
            public void onRemainDistanceUpdate(CharSequence charSequence) {
                Log.d(TAG, "onRemainDistanceUpdate:");

            }

            @Override
            public void onRemainTimeUpdate(CharSequence charSequence) {
                Log.d(TAG, "onRemainTimeUpdate");

            }

            @Override
            public void onGpsStatusChange(CharSequence charSequence, Drawable drawable) {
                Log.d(TAG, "onGpsStatusChange");
            }

            @Override
            public void onRouteFarAway(CharSequence charSequence, Drawable drawable) {
                Log.d(TAG, "onRouteFarAway: charSequence = :" + charSequence);
                deviateCount++;
                Log.d(TAG, "deviateCount = " + deviateCount);
                showWarning();

                /*if (deviateCount >= 3) {
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
                }*/
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

    private void showWarning() {
        final Button button = new Button(WNaviGuideActivity.this);
        button.setText("您已偏离导航3次。(模拟短信)");

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button.setVisibility(View.INVISIBLE);
            }
        });
        ((FrameLayout) view).addView(button);
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

    private void voice2() {

    }

    private void voice(String msg) {
        String token = auth();
        msg = URLEncoder.encode(msg);
        msg = URLEncoder.encode(msg);
        String body = "tex=" + msg + "&lan=zh&cuid=865580033151510865580033258224&ctp=1&aue=3&tok=" + token;
        System.out.println(BAIDU_VOICE_URL + body);
        Log.i("voice", BAIDU_VOICE_URL + body);
        http(BAIDU_VOICE_URL, body);
    }

    private String auth() {
        String accessToken = null;
        String url = BAIDU_OAUTH_URL + "grant_type=client_credentials&client_id=" + "5qnsS77Fm3snqSYwgr8O6MXz" + "&client_secret=" + "nYLLZC1eu8uPC6KztapV3fwyo66d8H7D";
        String response = http(url, null);
        try {
            JSONObject jsonObject = new JSONObject(response);
            accessToken = jsonObject.getString("access_token");
        } catch (JSONException e) {
            return null;
        }
        return accessToken;
    }

    private String http(String urlAddress, String body) {
        String response = null;
        try {
            URL url = new URL(urlAddress);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true); // 设置该连接是可以输出的
            connection.setRequestMethod("POST"); // 设置请求方式
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            if (body != null) {
                PrintWriter pw = new PrintWriter(new BufferedOutputStream(connection.getOutputStream()));
                pw.write(body);
                pw.flush();
                pw.close();
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
            String line = null;
            StringBuilder result = new StringBuilder();
            while ((line = br.readLine()) != null) { // 读取数据
                result.append(line);
            }
            connection.disconnect();
            response = result.toString();
            Log.i("voice", response);
            System.out.println(result.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }
}
