package com.example.lbstest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.CityInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchOption;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.baidu.navisdk.adapter.BNCommonSettingParam;
import com.baidu.navisdk.adapter.BNOuterTTSPlayerCallback;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BNaviSettingManager;
import com.baidu.navisdk.adapter.BaiduNaviManager;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * create by chentravelling@163.com
 */
public class MainActivity extends Activity implements OnGetGeoCoderResultListener {
    public static final String ROUTE_PLAN_NODE = "routePlanNode";
    /**
     * 全局变量
     */
    private static final String APP_FOLDER_NAME = "lbstest";    //app在SD卡中的目录名
    public static List<Activity> activityList = new LinkedList<>();
    private final String TTS_API_KEY = "9825418";               //语音播报api_key
    boolean isFirstLoc = true;                                  //是否首次定位
    GeoCoder mSearch = null;                                    //地理编码模块
    String authinfo = null;
    /**
     * UI相关
     */
    private RelativeLayout popuInfoView = null;                 //点击marker后弹出的窗口
    private AutoCompleteTextView autoCompleteTextView = null;   //输入搜索关键字的view
    private ArrayAdapter<String> adapter = null;                //适配器,展示搜索结果
    private Button searchBtn = null;                            //搜索按钮
    private Button goButton = null;                             //到这去 按钮
    private String mSDCardPath = null;
    /**
     * 百度地图相关
     */
    private LocationClient locationClient;                      //定位SDK核心类
    private MapView mapView;                                    //百度地图控件
    private BaiduMap baiduMap;                                  //百度地图对象
    private LatLng myLocation;                                  //当前定位信息
    private LatLng clickLocation;                               //长按地址信息
    private BDLocation currentLocation;                         //当前定位信息[最好使用这个]
    private PoiSearch poiSearch;                                //POI搜索模块
    private SuggestionSearch suggestionSearch = null;           //模糊搜索模块
    private MySensorEventListener mySensorEventListener;        //传感器
    private float lastX = 0.0f;                                 //传感器返回的方向
    private boolean initSuccess = false;                        //初始化标志位
    private boolean initDir = false;
    /**
     * 内部TTS播报状态回传handler
     */
    private Handler ttsHandler = new Handler() {
        public void handleMessage(Message msg) {
            int type = msg.what;
            switch (type) {
                case BaiduNaviManager.TTSPlayMsgType.PLAY_START_MSG: {
                    //showToastMsg("Handler : TTS play start");
                    break;
                }
                case BaiduNaviManager.TTSPlayMsgType.PLAY_END_MSG: {
                    //showToastMsg("Handler : TTS play end");
                    break;
                }
                default:
                    break;
            }
        }
    };
    /**
     * 内部TTS播报状态回调接口
     */
    private BaiduNaviManager.TTSPlayStateListener ttsPlayStateListener = new BaiduNaviManager.TTSPlayStateListener() {

        @Override
        public void playEnd() {
            showToastMsg("TTSPlayStateListener : TTS play end");
        }

        @Override
        public void playStart() {
            showToastMsg("TTSPlayStateListener : TTS play start");
        }
    };
    private BNOuterTTSPlayerCallback mTTSCallback = new BNOuterTTSPlayerCallback() {

        @Override
        public void stopTTS() {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "stopTTS");
        }

        @Override
        public void resumeTTS() {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "resumeTTS");
        }

        @Override
        public void releaseTTSPlayer() {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "releaseTTSPlayer");
        }

        @Override
        public int playTTSText(String speech, int bPreempt) {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "playTTSText" + "_" + speech + "_" + bPreempt);

            return 1;
        }

        @Override
        public void phoneHangUp() {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "phoneHangUp");
        }

        @Override
        public void phoneCalling() {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "phoneCalling");
        }

        @Override
        public void pauseTTS() {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "pauseTTS");
        }

        @Override
        public void initTTSPlayer() {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "initTTSPlayer");
        }

        @Override
        public int getTTSState() {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "getTTSState");
            return 1;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        //界面初始化：控件初始化
        initView();
        //初始化百度地图相关
        initBaiduMap();
        //初始化传感器
        initSensor();
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

    /**
     * 初始化百度地图相关模块
     */
    private void initBaiduMap() {
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
        option.setScanSpan(1000);
        //设备方向
        option.setNeedDeviceDirect(true);
        //是否需要地址信息
        option.setIsNeedAddress(true);
        //是否需要地址语义化信息
        option.setIsNeedLocationDescribe(true);
        locationClient.setLocOption(option);
        //开启定位
        locationClient.start();
        //定位模式
        baiduMap
                .setMyLocationConfigeration(new MyLocationConfiguration(
                        MyLocationConfiguration.LocationMode.NORMAL, true, null));
        //增加监听：长按地图
        baiduMap.setOnMapLongClickListener(new OnMapLongClickListener());
        //增加监听：map click listener ,主要监听poi点击
        baiduMap.setOnMapClickListener(new OnMapClickListener());
        /******************************************************
         * 地理编码模块
         ******************************************************/
        //地理编码模块
        mSearch = GeoCoder.newInstance();
        //增加监听：地理编码查询结果
        mSearch.setOnGetGeoCodeResultListener(this);
        /******************************************************
         * POI搜索模块
         ******************************************************/
        //POI搜索模块
        poiSearch = PoiSearch.newInstance();
        //增加监听：POI搜索结果
        poiSearch.setOnGetPoiSearchResultListener(new PoiSearchListener());
        //模糊搜索
        suggestionSearch = SuggestionSearch.newInstance();
        //增加监听：模糊搜索查询结果
        suggestionSearch.setOnGetSuggestionResultListener(new SuggestionResultListener());
        /***
         * 导航模块需要的初始化
         */
        initDir = initDirs();
        initNavi();
    }

    /**
     * 界面初始化
     **/
    private void initView() {
        //关键字输入view
        autoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView);
        //增加监听：text change listener
        autoCompleteTextView.addTextChangedListener(new TextViewWatcher());
        //搜索btn
        searchBtn = (Button) findViewById(R.id.searchBtn);
        //增加监听
        searchBtn.setOnClickListener(new OnClickListener());
        //百度地图view
        mapView = (MapView) findViewById(R.id.bmapView);
        //到这去 按钮
        goButton = (Button) findViewById(R.id.go_button);
    }

    /**
     * 反向搜索
     *
     * @param latLng
     */
    public void reverseSearch(LatLng latLng) {
        mSearch.reverseGeoCode(new ReverseGeoCodeOption()
                .location(latLng));
    }

    /**
     * 监听正向地理编码和反向地理编码搜索结果
     *
     * @param result
     */
    @Override
    public void onGetGeoCodeResult(GeoCodeResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(MainActivity.this, "抱歉，未能找到结果", Toast.LENGTH_LONG)
                    .show();
            return;
        }
        baiduMap.clear();
        baiduMap.addOverlay(new MarkerOptions().position(result.getLocation())
                .icon(BitmapDescriptorFactory
                        .fromResource(R.drawable.icon_gcoding)));
        baiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(result
                .getLocation()));
        String strInfo = String.format("纬度：%f 经度：%f",
                result.getLocation().latitude, result.getLocation().longitude);
        Toast.makeText(MainActivity.this, strInfo, Toast.LENGTH_LONG).show();

    }

    @Override
    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(MainActivity.this, "抱歉，未能找到结果", Toast.LENGTH_LONG)
                    .show();
            return;
        }
        baiduMap.clear();
        baiduMap.addOverlay(
                new MarkerOptions()
                        .position(result.getLocation())                                     //坐标位置
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding))  //图标
                        .title(result.getAddress())                                         //标题

        );
        baiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(result
                .getLocation()));
        /**
         * 弹出InfoWindow，显示信息
         */
        BDLocation bdLocation = new BDLocation();
        bdLocation.setLatitude(result.getLocation().latitude);
        bdLocation.setLongitude(result.getLocation().longitude);
        bdLocation.setAddrStr(result.getAddress());
        poputInfo(bdLocation, result.getAddress());
    }

    /**
     * 弹出InfoWindow，显示信息
     */
    public void poputInfo(final BDLocation bdLocation, final String address) {
        /**
         * 获取弹窗控件
         */
        popuInfoView = (RelativeLayout) findViewById(R.id.id_marker_info);
        TextView addrNameView = (TextView) findViewById(R.id.addrName);
        if (addrNameView != null)
            addrNameView.setText(address);
        popuInfoView.setVisibility(View.VISIBLE);
        /**
         * 进入导航部分：稍微不符合使用逻辑,可根据实际情况调整
         */
        /**
         * 首先进行授权
         */
        if (!initSuccess && !initDir)
            initNavi();
        /**
         * 为到这去按钮绑定点击事件
         */
        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**
                 * 判断是否已经授权
                 */
                if (!BaiduNaviManager.isNaviInited()) {
                    Toast.makeText(MainActivity.this, "授权失败咯", Toast.LENGTH_SHORT).show();
                    return;
                }
                /**
                 * 获取该点的坐标位置
                 */;
                BNRoutePlanNode sNode = null;
                BNRoutePlanNode eNode = null;

                Toast.makeText(MainActivity.this, "开始获取起点和终点", Toast.LENGTH_SHORT).show();
                sNode = new BNRoutePlanNode(
                        currentLocation.getLongitude(),          //经度
                        currentLocation.getLatitude(),           //纬度
                        currentLocation.getBuildingName(),       //算路节点名
                        null,                                   //算路节点地址描述
                        BNRoutePlanNode.CoordinateType.BD09LL
                ); //坐标类型
                eNode = new BNRoutePlanNode(
                        bdLocation.getLongitude(), bdLocation.getLatitude(), address,
                        null,
                        BNRoutePlanNode.CoordinateType.BD09LL);

                if (sNode != null && eNode != null) {
                    List<BNRoutePlanNode> list = new ArrayList<BNRoutePlanNode>();
                    list.add(sNode);
                    list.add(eNode);
                    /**
                     * 发起算路操作并在算路成功后通过回调监听器进入导航过程,返回是否执行成功
                     */
                    BaiduNaviManager
                            .getInstance()
                            .launchNavigator(
                                    MainActivity.this,               //建议是应用的主Activity
                                    list,                            //传入的算路节点，顺序是起点、途经点、终点，其中途经点最多三个
                                    1,                               //算路偏好 1:推荐 8:少收费 2:高速优先 4:少走高速 16:躲避拥堵
                                    true,                            //true表示真实GPS导航，false表示模拟导航
                                    new DemoRoutePlanListener(sNode)//开始导航回调监听器，在该监听器里一般是进入导航过程页面
                            );
                }

            }
        });
    }

    public void showToastMsg(final String msg) {
        MainActivity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
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
     * 使用SDK前，先进行百度服务授权和引擎初始化
     */
    private void initNavi() {

        BNOuterTTSPlayerCallback ttsCallback = null;
        //Toast.makeText(MainActivity.this,"授权",Toast.LENGTH_LONG).show();
        BaiduNaviManager.getInstance().init(this, mSDCardPath, APP_FOLDER_NAME, new BaiduNaviManager.NaviInitListener() {
            @Override
            public void onAuthResult(int status, String msg) {
                if (0 == status) {
                    authinfo = "key校验成功!";
                } else {
                    authinfo = "key校验失败, " + msg;
                }
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, authinfo, Toast.LENGTH_LONG).show();
                    }
                });
            }

            public void initSuccess() {
                initSuccess = true;
                initSetting();
                goButton.setText("授权成功,点击进入导航");
                goButton.setEnabled(true);
            }

            public void initStart() {
                //Toast.makeText(MainActivity.this, "百度导航引擎初始化开始", Toast.LENGTH_SHORT).show();
            }

            public void initFailed() {
                Toast.makeText(MainActivity.this, "百度导航引擎初始化失败", Toast.LENGTH_SHORT).show();
            }
        }, null, ttsHandler, ttsPlayStateListener);

    }

    private String getSdcardDir() {
        if (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().toString();
        }
        return null;
    }

    /**
     * 导航设置管理器
     */
    private void initSetting() {
        /**
         * 日夜模式 1：自动模式 2：白天模式 3：夜间模式
         */
        BNaviSettingManager.setDayNightMode(BNaviSettingManager.DayNightMode.DAY_NIGHT_MODE_DAY);
        /**
         * 设置全程路况显示
         */
        BNaviSettingManager.setShowTotalRoadConditionBar(BNaviSettingManager.PreViewRoadCondition.ROAD_CONDITION_BAR_SHOW_ON);
        /**
         * 设置语音播报模式
         */
        BNaviSettingManager.setVoiceMode(BNaviSettingManager.VoiceMode.Veteran);
        /**
         * 设置省电模式
         */
        BNaviSettingManager.setPowerSaveMode(BNaviSettingManager.PowerSaveMode.DISABLE_MODE);
        /**
         * 设置实时路况条
         */
        BNaviSettingManager.setRealRoadCondition(BNaviSettingManager.RealRoadCondition.NAVI_ITS_ON);

        Bundle bundle = new Bundle();
        // 必须设置APPID，否则会静音
        bundle.putString(BNCommonSettingParam.TTS_APP_ID, TTS_API_KEY);
        BNaviSettingManager.setNaviSdkParam(bundle);

    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mapView.onResume();
        baiduMap.clear();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        // 退出时销毁定位
        locationClient.stop();
        // 关闭定位图层
        baiduMap.setMyLocationEnabled(false);
        mapView.onDestroy();
        mapView = null;
        super.onDestroy();
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
        public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {

            if (poiDetailResult == null
                    || poiDetailResult.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
                Toast.makeText(MainActivity.this, "未找到结果", Toast.LENGTH_LONG)
                        .show();
                return;
            }
            if (poiDetailResult.error == SearchResult.ERRORNO.NO_ERROR) {
                //成功在传入的搜索city中搜索到POI
                //对result进行一些应用
                //一般都是添加到地图中，然后绑定一些点击事件
                //官方Demo的处理如下：
                baiduMap.clear();
                baiduMap.addOverlay(
                        new MarkerOptions()
                                .position(poiDetailResult.location)                                     //坐标位置
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding))
                                .title(poiDetailResult.getAddress())                                         //标题

                );
                //将该POI点设置为地图中心
                baiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(poiDetailResult.location));
                Toast.makeText(MainActivity.this, "hhah", Toast.LENGTH_LONG).show();
                return;
            }
            if (poiDetailResult.error == SearchResult.ERRORNO.AMBIGUOUS_KEYWORD) {
            }
        }

        @Override
        public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

        }
    }

    private class OnMarkerClickListener implements BaiduMap.OnMarkerClickListener {
        @Override
        public boolean onMarkerClick(Marker marker) {
            BDLocation bdLocation = new BDLocation();
            bdLocation.setAddrStr(marker.getTitle());
            bdLocation.setLatitude(marker.getPosition().latitude);
            bdLocation.setLongitude(marker.getPosition().longitude);
            //弹出信息
            poputInfo(bdLocation, marker.getTitle());
            return false;
        }
    }

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
                    poiSearch.searchPoiDetail(new PoiDetailSearchOption().poiUid(info.uid));
                }
            });
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * 重写map poi click:监听地图中已标记的POI点击事件
     */
    private class OnMapClickListener implements BaiduMap.OnMapClickListener {
        @Override
        public void onMapClick(LatLng latLng) {

        }

        @Override
        public boolean onMapPoiClick(MapPoi mapPoi) {
            String POIName = mapPoi.getName();//POI点名称
            LatLng POIPosition = mapPoi.getPosition();//POI点坐标
            //下面就是自己随便应用了
            //根据POI点坐标反向地理编码
            reverseSearch(POIPosition);
            //添加图层显示POI点
            baiduMap.clear();
            baiduMap.addOverlay(
                    new MarkerOptions()
                            .position(POIPosition)                                     //坐标位置
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding))
                            .title(POIName)                                         //标题

            );
            //将该POI点设置为地图中心
            baiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(POIPosition));
            return true;
        }
    }

    /**
     * 重写map long click:长按地图选点,进行反地理编码,查询该点信息
     */
    private class OnMapLongClickListener implements BaiduMap.OnMapLongClickListener {
        @Override
        public void onMapLongClick(LatLng latLng) {
            clickLocation = latLng;
            reverseSearch(latLng);
        }
    }

    /**
     * 导航回调监听器
     */
    public class DemoRoutePlanListener implements BaiduNaviManager.RoutePlanListener {

        private BNRoutePlanNode mBNRoutePlanNode = null;

        public DemoRoutePlanListener(BNRoutePlanNode node) {
            mBNRoutePlanNode = node;
        }

        @Override
        public void onJumpToNavigator() {
            /*
             * 设置途径点以及resetEndNode会回调该接口
             */

            for (Activity ac : activityList) {
                if (ac.getClass().getName().endsWith("GuideActivity")) {

                    return;
                }
            }
            /**
             * 导航activity
             */
            Intent intent = new Intent(MainActivity.this, GuideActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable(ROUTE_PLAN_NODE, (BNRoutePlanNode) mBNRoutePlanNode);
            intent.putExtras(bundle);
            startActivity(intent);

        }

        @Override
        public void onRoutePlanFailed() {
            // TODO Auto-generated method stub
            Toast.makeText(MainActivity.this, "算路失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 点击事件类
     */
    private class OnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            Intent intent;
            switch (view.getId()) {
                case R.id.searchBtn:
                    poiSearch.searchInCity(
                            new PoiCitySearchOption()
                                    .city(currentLocation.getCity())
                                    .keyword(autoCompleteTextView.getText().toString())
                                    .pageNum(0));
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 定位SDK监听函数
     */
    public class MyLocationListenner implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            if (location == null || mapView == null) {
                return;
            }
            //Toast.makeText(MainActivity.this, "定位结果编码："+location.getLocType(), Toast.LENGTH_LONG).show();

            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(lastX)//该参数由传感器提供
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            baiduMap.setMyLocationData(locData);
            if (isFirstLoc) {
                isFirstLoc = false;
                //city.setText(location.getCity());
                myLocation = new LatLng(location.getLatitude(),
                        location.getLongitude());
                currentLocation = location;

                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(myLocation).zoom(18.0f);
                baiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }
        }

        @Override
        public void onConnectHotSpotMessage(String s, int i) {

        }
    }
}
