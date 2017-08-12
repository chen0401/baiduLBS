package com.example.lbstest;

import com.baidu.navisdk.adapter.BNaviCommonParams;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class BNEventHandler {
    
    private BNEventDialog mEventDialog = null;
    
    private static class LazyLoader {
        private static BNEventHandler mInstance = new BNEventHandler();
    }
    
    public static BNEventHandler getInstance() {
        return LazyLoader.mInstance;
    }
    
    private BNEventHandler() {
    }
    
    public BNEventDialog getDialog(Context ctx) {
        if (mEventDialog == null) {
            mEventDialog = new BNEventDialog(ctx);
        }
        return mEventDialog;
    }
    
    public void showDialog() {
        if (mEventDialog != null) {
            mEventDialog.setCanceledOnTouchOutside(false);
            mEventDialog.show();
        }
    }
    
    public void dismissDialog() {
        if (mEventDialog != null) {
            mEventDialog.dismiss();
        }
    }
    
    public void disposeDialog() {
        mEventDialog = null;
    }
    
    public void handleNaviEvent(int what, int arg1, int arg2, Bundle bundle) {
        Log.i("onCommonEventCall", String.format("%d,%d,%d,%s", what, arg1, arg2,
                (bundle == null ? "" : bundle.toString())));
        switch (what) {
            case BNaviCommonParams.MessageType.EVENT_NAVIGATING_STATE_BEGIN:
                break;
            case BNaviCommonParams.MessageType.EVENT_NAVIGATING_STATE_END:
                break;
            case BNaviCommonParams.MessageType.EVENT_GPS_LOCATED:
                mEventDialog.updateLocateState(true);
                break;
            case BNaviCommonParams.MessageType.EVENT_GPS_DISMISS:
                mEventDialog.updateLocateState(false);
                break;
            case BNaviCommonParams.MessageType.EVENT_ON_YAW_SUCCESS:
                break;
            case BNaviCommonParams.MessageType.EVENT_ROAD_TURN_ICON_UPDATE:
                byte[] byteArray = bundle.getByteArray(BNaviCommonParams.BNGuideKey.ROAD_TURN_ICON);
                Bitmap map = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                mEventDialog.updateTurnIcon(map);
                break;
            case BNaviCommonParams.MessageType.EVENT_ROAD_TURN_DISTANCE_UPDATE:
                String turndis = bundle.getString(BNaviCommonParams.BNGuideKey.TROAD_TURN_DISTANCE);
                mEventDialog.updateGoDistanceTx(turndis);
                mEventDialog.updateAlongMeters(turndis);
                break;
            case BNaviCommonParams.MessageType.EVENT_ROAD_NEXT_ROAD_NAME:
                String nextRoad = bundle.getString(BNaviCommonParams.BNGuideKey.NEXT_ROAD_NAME);
                if (!TextUtils.isEmpty(nextRoad)) {
                    mEventDialog.updateNextRoad(nextRoad); 
                }
                break;
            case BNaviCommonParams.MessageType.EVENT_ROAD_CURRENT_ROAD_NAME:
                String currentRoad = bundle.getString(BNaviCommonParams.BNGuideKey.CURRENT_ROAD_NAME);
                if (!TextUtils.isEmpty(currentRoad)) {
                    mEventDialog.updateCurrentRoad(currentRoad);
                }
                break;
            case BNaviCommonParams.MessageType.EVENT_REMAIN_DISTANCE_UPDATE:
                String remainDisctance = bundle.getString(BNaviCommonParams.BNGuideKey.TOTAL_REMAIN_DISTANCE);
                mEventDialog.updateRemainDistance(remainDisctance);
                break;
            case BNaviCommonParams.MessageType.EVENT_REMAIN_TIME_UPDATE:
                String remainTime = bundle.getString(BNaviCommonParams.BNGuideKey.TOTAL_REMAIN_TIME);
                mEventDialog.updateRemainTime(remainTime);
                break;
            case BNaviCommonParams.MessageType.EVENT_RASTER_MAP_SHOW:
                int type = bundle.getInt(BNaviCommonParams.BNEnlargeRoadKey.ENLARGE_TYPE);
                byte[] arrowByte = bundle.getByteArray(BNaviCommonParams.BNEnlargeRoadKey.ARROW_IMAGE);
                byte[] bgByte = bundle.getByteArray(BNaviCommonParams.BNEnlargeRoadKey.BACKGROUND_IMAGE);
                Bitmap arrowMap = BitmapFactory.decodeByteArray(arrowByte, 0, arrowByte.length);
                Bitmap bgMap = BitmapFactory.decodeByteArray(bgByte, 0, bgByte.length);
                mEventDialog.onEnlageShow(type, arrowMap, bgMap);
                break;
            case BNaviCommonParams.MessageType.EVENT_RASTER_MAP_UPDATE:
                String remainDistance = bundle.getString(BNaviCommonParams.BNEnlargeRoadKey.REMAIN_DISTANCE);
                String roadName = bundle.getString(BNaviCommonParams.BNEnlargeRoadKey.ROAD_NAME);
                int progress = bundle.getInt(BNaviCommonParams.BNEnlargeRoadKey.DRIVE_PROGRESS);
                break;
            case BNaviCommonParams.MessageType.EVENT_RASTER_MAP_HIDE:
                mEventDialog.onEnlargeHide();
                break;
            case BNaviCommonParams.MessageType.EVENT_ROUTE_PLAN_SUCCESS:
                int distance = bundle.getInt(BNaviCommonParams.BNRouteInfoKey.TOTAL_DISTANCE);
                int time = bundle.getInt(BNaviCommonParams.BNRouteInfoKey.TOTAL_TIME);
                int tollFees = bundle.getInt(BNaviCommonParams.BNRouteInfoKey.TOLL_FESS);
                int lightCounts = bundle.getInt(BNaviCommonParams.BNRouteInfoKey.TRAFFIC_LIGHT);
                int gasMoney = bundle.getInt(BNaviCommonParams.BNRouteInfoKey.GAS_MONEY);
                break;
            case BNaviCommonParams.MessageType.EVENT_SERVICE_AREA_UPDATE:
                String firstName = bundle.getString(BNaviCommonParams.BNGuideKey.FIRST_SERVICE_NAME);
                int firstDistance = bundle.getInt(BNaviCommonParams.BNGuideKey.FIRST_SERVICE_TIME);
                String secondeName = bundle.getString(BNaviCommonParams.BNGuideKey.SECOND_SERVICE_NAME);
                int secondeDistance = bundle.getInt(BNaviCommonParams.BNGuideKey.SECOND_SERVICE_TIME);
                break;
            case BNaviCommonParams.MessageType.EVENT_CURRENT_SPEED:
                mEventDialog.updateCurrentSpeed(String.valueOf(arg1));
                break;
            case BNaviCommonParams.MessageType.EVENT_ALONG_UPDATE:
                boolean isAlong = bundle.getBoolean(BNaviCommonParams.BNGuideKey.IS_ALONG);
                break;
            case BNaviCommonParams.MessageType.EVENT_CURRENT_MILES:
                int miles = arg1;
            default :
                break;
        }
    }
}
