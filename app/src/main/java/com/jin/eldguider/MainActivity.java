package com.jin.eldguider;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.*;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.CityInfo;
import com.baidu.mapapi.search.core.PoiDetailInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.*;
import com.baidu.mapapi.search.route.*;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.baidu.mapapi.utils.DistanceUtil;
import com.baidu.mapapi.walknavi.WalkNavigateHelper;
import com.baidu.mapapi.walknavi.adapter.IWEngineInitListener;
import com.baidu.mapapi.walknavi.adapter.IWRouteGuidanceListener;
import com.baidu.mapapi.walknavi.adapter.IWRoutePlanListener;
import com.baidu.mapapi.walknavi.model.RouteGuideKind;
import com.baidu.mapapi.walknavi.model.WalkRoutePlanError;
import com.baidu.mapapi.walknavi.params.WalkNaviLaunchParam;
import com.baidu.mapapi.walknavi.params.WalkRouteNodeInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    /*
     *  UI相关
     */
    private Button startWalkGuideBtn;                           //开始导航按钮
    private MapView mapView;                                    //百度地图控件
    private AutoCompleteTextView autoCompleteTextView;          //自动提示输入控件
    private Button distanceBtn;                                 //显示偏离距离
    private Button deviateCountBtn;                             //显示偏离次数
    private Button settingBtn;                                  //设置按钮
    /*
     *   百度地图相关
     * */
    private BaiduMap baiduMap;                                  //百度地图实例
    private PoiSearch poiSearch;                                //POI搜索模块
    private SuggestionSearch suggestionSearch = null;           //模糊搜索模块
    private RoutePlanSearch routePlanSearch;                    //路径规划模块
    private WalkNavigateHelper walkNavigateHelper;              //步行导航模块
    private LocationClient locationClient;                      //位置定位模块
    private final int hz = 5;                                   //定位频率
    /*
     * 其他变量
     * */
    private ArrayAdapter<String> adapter = null;                //适配器,展示搜索结果
    private static final int authBaseRequestCode = 1;
    private static BDLocation currentLocation;                  //当前定位信息[最好使用这个]
    boolean isFirstLoc = true;                                  //是否首次定位
    private LatLng myLocation;                                  //当前定位信息
    private float lastX = 0.0f;                                 //传感器返回的方向
    private MySensorEventListener mySensorEventListener;        //传感器监听器
    private static LatLng terminalStation;                      //终点信息
    public static WalkingRouteLine walkingRouteLine;            //步行规划路径
    private final static double MIN_D = 0.000001f;              //浮点型数据是否相等的误差
    private final static int MAX_MIN_D = -1;                    //是否偏离导航的距离阈值
    private volatile static boolean isWalkGuidering = false;    //是否已进入导航模式,防止重复进入
    int nodeIndex = -1;                                         // 节点索引,供浏览节点时使用
    private static final String[] authBaseArr =                 //需要动态申请的权限
            {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
    private static final String APP_FOLDER_NAME = "eldguider";    //app在SD卡中的目录名
    private String mSDCardPath = null;                            //app在SD卡中的路径
    private volatile static int deviateCount = 0;                 //记录偏离导航次数
    private final static int MAX_DEVIATE_COUNT = 3;               //偏离导航次数阈值
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //initBaiduMap();
        setContentView(R.layout.activity_main);
        initView();
        if (initDirs()) {
            questPermission();
        }
        initBaiduMap();
        //初始化传感器
        initSensor();
    }

    private boolean initView() {
        startWalkGuideBtn = (Button) findViewById(R.id.startWalkGuideBtn);
        startWalkGuideBtn.setOnClickListener(new BtnClickListener());
        mapView = (MapView) findViewById(R.id.bmapView);
        distanceBtn = (Button) findViewById(R.id.distanceBtn);
        deviateCountBtn = (Button) findViewById(R.id.deviateCountBtn);

        //关键字输入view
        autoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView);
        //增加监听：text change listener
        autoCompleteTextView.addTextChangedListener(new TextViewWatcher());
        return true;
    }

    private boolean initBaiduMap() {
        /*****************************************************
         * 地图模块
         *****************************************************/
        //百度地图map
        baiduMap = mapView.getMap();
        //增加监听:Marker click listener
        baiduMap.setOnMarkerClickListener(new OnMarkerClickListener());
        /*****************************************************
         * 定位模块
         *****************************************************/
        // 开启定位图层
        baiduMap.setMyLocationEnabled(true);
        //定位服务客户端
        locationClient = new LocationClient(this);
        //注册监听
        locationClient.registerLocationListener(new MyLocationListenner());
        //定位配置信息
        LocationClientOption option = new LocationClientOption();
        // 打开gps
        option.setOpenGps(true);
        // 设置坐标类型,国测局经纬度坐标系:gcj02;  百度墨卡托坐标系:bd09;  百度经纬度坐标系:bd09ll
        option.setCoorType("bd09ll");
        //定位请求时间间隔 1秒
        option.setScanSpan(hz * 1000);
        option.setLocationNotify(true);
        //设备方向
        option.setNeedDeviceDirect(true);
        //是否需要地址信息
        option.setIsNeedAddress(true);
        //是否需要地址语义化信息
        option.setIsNeedLocationDescribe(true);
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);

        locationClient.setLocOption(option);
        //开启定位
        locationClient.start();
        //定位模式
        baiduMap
                .setMyLocationConfiguration(new MyLocationConfiguration(
                        MyLocationConfiguration.LocationMode.NORMAL, true, null));
        walkNavigateHelper = WalkNavigateHelper.getInstance();
        //walkNavigateHelper.setRouteGuidanceListener(this, new WRouteGuidanceListener());

        //POI搜索模块
        poiSearch = PoiSearch.newInstance();
        //增加监听：POI搜索结果
        poiSearch.setOnGetPoiSearchResultListener(new PoiSearchListener());

        //模糊搜索
        suggestionSearch = SuggestionSearch.newInstance();
        //增加监听：模糊搜索查询结果
        suggestionSearch.setOnGetSuggestionResultListener(new SuggestionResultListener());

        routePlanSearch = RoutePlanSearch.newInstance();
        routePlanSearch.setOnGetRoutePlanResultListener(new RoutePlanResultListener());
        return true;
    }

    private class TextViewWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.length() < 1) {
                return;
            }
            suggestionSearch
                    .requestSuggestion(new SuggestionSearchOption()
                            .city(currentLocation.getCity())
                            .keyword(s.toString()));
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

    private class RoutePlanResultListener implements OnGetRoutePlanResultListener {
        @Override
        public void onGetWalkingRouteResult(WalkingRouteResult result) {
            if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
                Toast.makeText(MainActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
            }
            if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
                // 起终点或途经点地址有岐义，通过以下接口获取建议查询信息
                // result.getSuggestAddrInfo()
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("提示");
                builder.setMessage("检索地址有歧义，请重新设置。\n可通过getSuggestAddrInfo()接口获得建议查询信息");
                builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.create().show();
                return;
            }
            if (result.error == SearchResult.ERRORNO.NO_ERROR) {
                nodeIndex = -1;
                baiduMap.clear();
                MyWalkingRouteOverlay overlay = new MyWalkingRouteOverlay(baiduMap);
                walkingRouteLine = result.getRouteLines().get(0);
                overlay.setData(walkingRouteLine);
                overlay.addToMap();
                overlay.zoomToSpan();
            }
        }

        @Override
        public void onGetTransitRouteResult(TransitRouteResult transitRouteResult) {

        }

        @Override
        public void onGetMassTransitRouteResult(MassTransitRouteResult massTransitRouteResult) {

        }

        @Override
        public void onGetDrivingRouteResult(DrivingRouteResult drivingRouteResult) {

        }

        @Override
        public void onGetIndoorRouteResult(IndoorRouteResult indoorRouteResult) {

        }

        @Override
        public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {

        }
    }

    // 自定义步行路线图层
    private class MyWalkingRouteOverlay extends WalkingRouteOverlay {

        public MyWalkingRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public BitmapDescriptor getStartMarker() {
            if (true) {
                return BitmapDescriptorFactory.fromResource(R.drawable.icon_st);
            }
            return null;
        }

        @Override
        public BitmapDescriptor getTerminalMarker() {
            if (true) {
                return BitmapDescriptorFactory.fromResource(R.drawable.icon_en);
            }
            return null;
        }
    }

    /**
     * 判断是否偏离航线
     *
     * @param walkingRouteLine 航线
     * @param latLng           位置坐标
     * @return true 是  flse 否
     */
    private boolean isDeviate(WalkingRouteLine walkingRouteLine, LatLng latLng) {
        if (walkingRouteLine == null || latLng == null)
            return false;
        List<WalkingRouteLine.WalkingStep> walkingSteps = walkingRouteLine.getAllStep();
        double MIN = 5000f;
        for (WalkingRouteLine.WalkingStep step : walkingSteps) {
            double distance = getDistance(step, latLng);
            MIN = MIN < distance ? MIN : distance;
            //Log.i("distance", "" + distance);
        }
        Log.i("distance", "Min:" + MIN);
        //startWalkGuideBtn.setText(MIN + "m");
        distanceBtn.setText(String.format("%.1f", MIN) + "|" + MAX_MIN_D);
        if (MIN > MAX_MIN_D) {
            Log.i("warning", "偏离路线");
            return true;
        }
        return false;
    }

    /**
     * 计算当前位置到步行路段的距离
     *
     * @param step
     * @param latLng
     * @return
     */
    private double getDistance(WalkingRouteLine.WalkingStep step, LatLng latLng) {
        LatLng entrance = step.getEntrance().getLocation();
        LatLng exit = step.getExit().getLocation();
        if (Math.abs(entrance.latitude - exit.latitude) < MIN_D) {
            LatLng point = new LatLng(entrance.latitude, latLng.longitude);
            return DistanceUtil.getDistance(point, new LatLng(latLng.latitude, latLng.longitude));
        }
        if (Math.abs(entrance.longitude - exit.longitude) < MIN_D) {
            LatLng point = new LatLng(entrance.longitude, latLng.latitude);
            return DistanceUtil.getDistance(point, new LatLng(latLng.latitude, latLng.longitude));
        }
        double X1 = entrance.longitude;
        double Y1 = entrance.latitude;
        double X2 = exit.longitude;
        double Y2 = exit.latitude;
        double X0 = latLng.longitude;
        double Y0 = latLng.latitude;
        double A = Y2 - Y1;
        double B = X1 - X2;
        double C = X2 * Y1 - X1 * Y2;
        double x = (B * B * X0 - A * B * Y0 - A * C) / (A * A + B * B);
        double y = -(A * x + C) / B;
        if (x < 0 || y < 0)
            return 0.0f;
        return DistanceUtil.getDistance(new LatLng(y, x), new LatLng(Y0, X0));
    }

    // 模糊搜索监听器
    private class SuggestionResultListener implements OnGetSuggestionResultListener {
        @Override
        public void onGetSuggestionResult(final SuggestionResult suggestionResult) {
            if (suggestionResult == null || suggestionResult.getAllSuggestions() == null) {
                return;
            }
            List<String> suggest = new ArrayList<>();
            for (SuggestionResult.SuggestionInfo suggestionInfo : suggestionResult.getAllSuggestions()) {
                if (suggestionInfo.key != null) {
                    suggest.add(suggestionInfo.key);
                }
            }
            adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_dropdown_item_1line, suggest);
            autoCompleteTextView.setAdapter(adapter);
            autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    SuggestionResult.SuggestionInfo info = suggestionResult.getAllSuggestions().get(position);
                    //poiSearch.searchPoiDetail(new PoiDetailSearchOption().poiUid(info.uid));
                    PlanNode from = PlanNode.withLocation(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
                    PlanNode to = PlanNode.withLocation(info.pt);
                    terminalStation = info.pt;
                    routePlanSearch.walkingSearch(new WalkingRoutePlanOption().from(from).to(to));
                    Log.i("POI", info.uid);
                }
            });
            adapter.notifyDataSetChanged();
        }

    }

    private class PoiSearchListener implements OnGetPoiSearchResultListener {
        @Override
        public void onGetPoiResult(PoiResult poiResult) {
            if (poiResult == null
                    || poiResult.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
                Toast.makeText(MainActivity.this, "未找到结果", Toast.LENGTH_LONG)
                        .show();
                return;
            }
            if (poiResult.error == SearchResult.ERRORNO.NO_ERROR) {
                //成功在传入的搜索city中搜索到POI
                //对result进行一些应用
                //一般都是添加到地图中，然后绑定一些点击事件
                //官方Demo的处理如下：
                baiduMap.clear();
                PoiOverlay overlay = new PoiOverlay(baiduMap);
                baiduMap.setOnMarkerClickListener(overlay);
                //MyPoiOverlayextends PoiOverlay;PoiOverlay extends OverlayManager
                //看了这三个class之间的关系后瞬间明白咱自己也可以写overlay，重写OverlayManager中的一些方法就可以了
                //比如重写了点击事件，这个方法真的太好，对不同类型的图层可能有不同的点击事件，百度地图3.4.0之后就支持设置多个监听对象了，只是本人还没把这个方法彻底掌握...
                overlay.setData(poiResult); //图层数据
                overlay.addToMap();         //添加到地图中(添加的都是marker)
                overlay.zoomToSpan();       //保证能显示所有marker
                return;
            }
            if (poiResult.error == SearchResult.ERRORNO.AMBIGUOUS_KEYWORD) {

                // 当输入关键字在本市没有找到，但在其他城市找到时，返回包含该关键字信息的城市列表
                String strInfo = "在";
                for (CityInfo cityInfo : poiResult.getSuggestCityList()) {
                    strInfo += cityInfo.city;
                    strInfo += ",";
                }
                strInfo += "找到结果";
                Toast.makeText(MainActivity.this, strInfo, Toast.LENGTH_LONG)
                        .show();
            }
        }

        @Override
        public void onGetPoiDetailResult(PoiDetailSearchResult poiDetailSearchResult) {
            Log.i("POI", "wwwww");
            if (poiDetailSearchResult == null
                    || poiDetailSearchResult.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
                Toast.makeText(MainActivity.this, "未找到结果", Toast.LENGTH_LONG)
                        .show();
                return;
            }
            List<PoiDetailInfo> poiDetailInfoList = poiDetailSearchResult.getPoiDetailInfoList();

            for (int i = 0; i < poiDetailInfoList.size(); i++) {
                PoiDetailInfo poiDetailInfo = poiDetailInfoList.get(i);
                if (null != poiDetailInfo) {
                    baiduMap.addOverlay(
                            new MarkerOptions()
                                    .position(poiDetailInfo.naviLocation)                                     //坐标位置
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding))
                                    .title(poiDetailInfo.getAddress())                                         //标题

                    );
                }
            }
            //将该POI点设置为地图中心
            //*baiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(poiDetailResult.location));
            //Toast.makeText(MainActivity.this, "hhah", Toast.LENGTH_LONG).show();
            //return;
        }

        @Override
        public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

        }

        @Override
        public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {
            if (poiDetailResult == null
                    || poiDetailResult.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
                Toast.makeText(MainActivity.this, "未找到结果", Toast.LENGTH_LONG)
                        .show();
                return;
            }
            if (poiDetailResult.error == SearchResult.ERRORNO.NO_ERROR) {
                baiduMap.clear();
                baiduMap.addOverlay(
                        new MarkerOptions()
                                .position(poiDetailResult.location)                                     //坐标位置
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding))
                                .title(poiDetailResult.getAddress())                                         //标题

                );
                //将该POI点设置为地图中心
                baiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(poiDetailResult.location));
                terminalStation = poiDetailResult.location;
                return;
            }
            if (poiDetailResult.error == SearchResult.ERRORNO.AMBIGUOUS_KEYWORD) {
            }

        }
    }

    private class OnMarkerClickListener implements BaiduMap.OnMarkerClickListener {
        @Override
        public boolean onMarkerClick(Marker marker) {
            BDLocation bdLocation = new BDLocation();
            bdLocation.setAddrStr(marker.getTitle());
            bdLocation.setLatitude(marker.getPosition().latitude);
            bdLocation.setLongitude(marker.getPosition().longitude);
            Log.i("route", "onMarkerClick: " + marker.getPosition().toString());
            //弹出信息
            //poputInfo(bdLocation, marker.getTitle());
            return false;
        }
    }

    // 步行导航监听器
    private class WRouteGuidanceListener implements IWRouteGuidanceListener {
        @Override
        public void onRouteGuideIconUpdate(Drawable drawable) {

        }

        @Override
        public void onRouteGuideKind(RouteGuideKind routeGuideKind) {

        }

        // 偏离航线
        @Override
        public void onRouteFarAway(CharSequence charSequence, Drawable drawable) {
            Log.i("Route", "偏离航线");
        }

        @Override
        public void onRemainDistanceUpdate(CharSequence charSequence) {
            Log.i("Route", "剩余距离");

        }

        @Override
        public void onRoadGuideTextUpdate(CharSequence charSequence, CharSequence charSequence1) {

        }

        @Override
        public void onRemainTimeUpdate(CharSequence charSequence) {
            Log.i("Route", "剩余时间");

        }

        @Override
        public void onGpsStatusChange(CharSequence charSequence, Drawable drawable) {
            Log.i("Route", "GPS变化");

        }

        // 偏航规划中
        @Override
        public void onRoutePlanYawing(CharSequence charSequence, Drawable drawable) {
            Log.i("route", "偏离导航");
            deviateCount++;
            if (deviateCount > MAX_DEVIATE_COUNT) {
                //发短信 警告处理
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
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
        public void onArriveDest() {

        }

        @Override
        public void onIndoorEnd(Message message) {

        }

        @Override
        public void onFinalEnd(Message message) {
            Log.i("route", "导航结束");
            isWalkGuidering = false;
        }

        @Override
        public void onVibrate() {
            Log.i("Route", "振动");
        }

        // 重新规划完成
        @Override
        public void onReRouteComplete() {
            Log.i("route", "重新规划完成");
            isWalkGuidering = true;
            deviateCount = 0;
        }
    }

    // 动态申请权限
    private void questPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            if (!checkPermission()) {
                this.requestPermissions(authBaseArr, authBaseRequestCode);
                return;
            }
        }
        //authWithKey();
    }

    private boolean checkPermission() {
        PackageManager packageManager = this.getPackageManager();
        for (String auth : authBaseArr) {
            if (packageManager.checkPermission(auth, this.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                Log.e("perssion_error", auth);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == authBaseRequestCode) {
            for (int ret : grantResults) {
                if (ret == 0) {
                    Log.i("perssion_info", ret + "");
                    continue;
                } else {
                    Toast.makeText(this, "缺少导航基本的权限!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            questPermission();
        }
    }

    /**
     * 初始化传感器
     */
    private void initSensor() {
        //方向传感器监听
        mySensorEventListener = new MySensorEventListener(this);
        //增加监听：orientation listener
        mySensorEventListener.setOnOrientationListener(new MySensorEventListener.OnOrientationListener() {
            @Override
            public void onOrientationChanged(float x) {
                //将获取的x轴方向赋值给全局变量
                lastX = x;
            }
        });
        //开启监听
        mySensorEventListener.start();
    }

    private void startWalkGuider(final LatLng start, final LatLng end) {
        //引擎初始化成功的回调
        WalkRouteNodeInfo startWalkRouteNodeInfo = new WalkRouteNodeInfo();
        startWalkRouteNodeInfo.setLocation(start);
        WalkRouteNodeInfo endWalkRouteNodeInfo = new WalkRouteNodeInfo();
        endWalkRouteNodeInfo.setLocation(end);
        WalkNaviLaunchParam walkNaviLaunchParam = new WalkNaviLaunchParam();
        walkNaviLaunchParam.startNodeInfo(startWalkRouteNodeInfo);
        walkNaviLaunchParam.endNodeInfo(endWalkRouteNodeInfo);
        walkNaviLaunchParam.extraNaviMode(0);
        startWalkGuide(walkNaviLaunchParam);
    }

    private void startWalkGuide(final WalkNaviLaunchParam walkNaviLaunchParam) {
        WalkNavigateHelper.getInstance().initNaviEngine(this, new IWEngineInitListener() {
            @Override
            public void engineInitSuccess() {
                routePlan(walkNaviLaunchParam);
            }

            @Override
            public void engineInitFail() {
                //引擎初始化失败的回调
                Log.e("route", "引擎初始化失败。 ");
            }
        });
    }

    private void routePlan(WalkNaviLaunchParam walkNaviLaunchParam) {
        WalkNavigateHelper.getInstance().routePlanWithRouteNode(walkNaviLaunchParam, new IWRoutePlanListener() {
            @Override
            public void onRoutePlanStart() {
                //开始算路的回调
                Log.i("route", "开始算路。");
            }

            @Override
            public void onRoutePlanSuccess() {
                isWalkGuidering = true;
                deviateCount = 0;
                //算路成功
                //跳转至诱导页面
                Intent intent = new Intent(MainActivity.this, WNaviGuideActivity.class);
                startActivity(intent);
            }

            @Override
            public void onRoutePlanFail(WalkRoutePlanError walkRoutePlanError) {
                //算路失败的回调
                Log.e("route", "算路失败。" + walkRoutePlanError.name());
            }
        });
    }

    private class BtnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            //authWithKey();
            if (terminalStation == null) {
                Toast.makeText(MainActivity.this, "请先输入目的地", Toast.LENGTH_LONG);
                return;
            }
            //startWalkGuide();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        if (mapView != null)
            mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        if (mapView != null)
            mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        if (mapView != null)
            mapView.onDestroy();
    }

    private String getSdcardDir() {
        if (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().toString();
        }
        return null;
    }

    /**
     * 初始化SD卡，在SD卡路径下新建文件夹：App目录名，文件中包含了很多东西，比如log、cache等等
     *
     * @return
     */
    private boolean initDirs() {
        mSDCardPath = getSdcardDir();
        if (mSDCardPath == null) {
            return false;
        }
        File f = new File(mSDCardPath, APP_FOLDER_NAME);
        if (!f.exists()) {
            try {
                f.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /**
     * 定位SDK监听函数
     */
    public class MyLocationListenner extends BDAbstractLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            Log.i("location", lastX + "");
            if (location == null || mapView == null) {
                return;
            }
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(lastX)//该参数由传感器提供
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            baiduMap.setMyLocationData(locData);
            myLocation = new LatLng(location.getLatitude(),
                    location.getLongitude());
            currentLocation = location;
            if (isFirstLoc) {
                isFirstLoc = false;
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(myLocation).zoom(18.0f);
                baiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }
            // 如果偏离导航
            if (isDeviate(walkingRouteLine, myLocation)) {
                deviateCount++;
                Log.i("distance", "偏离次数：" + deviateCount);
                deviateCountBtn.setText(deviateCount + "|" + MAX_DEVIATE_COUNT);
                if (!isWalkGuidering)
                    Toast.makeText(MainActivity.this, "您已偏离路线" + deviateCount + "次", Toast.LENGTH_LONG).show();
                // 偏离次数达到3次
                if (deviateCount >= MAX_DEVIATE_COUNT && !isWalkGuidering) {
                    // 发起导航
                    Log.i("distance", "开始导航");
                    startWalkGuider(myLocation, terminalStation);
                }
            }
        }
    }
}
