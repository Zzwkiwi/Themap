package com.example.themap;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Polygon;
import com.baidu.mapapi.map.PolygonOptions;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.map.Stroke;
import com.baidu.mapapi.map.TextOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.SpatialRelationUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, BaiduMap.OnMarkerClickListener, BaiduMap.OnMapClickListener, BaiduMap.OnMarkerDragListener {

    protected Button btnYes;
    protected Button btnClear;
    protected MapView map;
    protected ImageView location;
    protected Button btnCheck;
    private BaiduMap baidumap;

    //定位相关
    private LocationClient mLocationClient;
    private MyLocationListener mLocationListener;
    //是否第一次定位，如果是第一次定位的话要将自己的位置显示在地图 中间
    private boolean isFirstLocation = true;

    //marker 相关
    private Marker marker;
    List<Marker> markers = new ArrayList<>();
    //算是map的索引，通过此id 来按顺序取出坐标点
    private List<String> ids = new ArrayList<>();
    //用来存储坐标点
    private Map<String, LatLng> latlngs = new HashMap<>();

    private InfoWindow mInfoWindow;
    //线
    private Polyline mPolyline;
    //多边形
    private Polygon polygon;
    //private List<Polygon> polygons = new ArrayList<>();
    private double latitude;
    private double longitude;
    private double la;
    private double lo;

    private int size;
    //根据别名来存储画好的多边形
    private Map<String, Polygon> polygonMap = new HashMap<>();
    //多边形的别名
    private List<String> aliasname = new ArrayList<>();
    //
    private boolean polygonContainsPoint;
    //用来存储一个点所在的所有的区域
    List<String> areas = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_main);
        initView();
        //定位
        //initLocation();
    }

    @Override
    public void onClick(View view) {
        //用来确定多变形
        if (view.getId() == R.id.btn_yes) {
            //--------------------------确定多边形的大小和别名-----------------------------

            LatLng l = null;
            la = 0;
            lo = 0;
            size = ids.size();
            if (size <= 2) {
                Toast.makeText(this, "点必须大于2", Toast.LENGTH_SHORT).show();
                return;
            }
            for (int i = 0; i < size; i++) {
                l = latlngs.get(ids.get(i));
                la = la + l.latitude;
                lo = lo + l.longitude;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("请输入名字：");
            View inflate = View.inflate(this, R.layout.dialog_aliasname, null);
            final EditText edt_alias = inflate.findViewById(R.id.edt_alias);
            builder.setView(inflate);
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String trim = edt_alias.getText().toString().trim();
                    if (trim.equals("")) {
                        Toast.makeText(MainActivity.this, "别名不能为空！", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    drawPolygon();
                    // 添加文字，求出多边形的中心点向中心点添加文字
                    LatLng llText = new LatLng(la / size, lo / size);
                    OverlayOptions ooText = new TextOptions()
                            .fontSize(24).fontColor(0xFFFF00FF).text(trim + "")
                            .position(llText);
                    baidumap.addOverlay(ooText);
                    polygonMap.put(trim, polygon);
                    aliasname.add(trim);
                    polygon = null;
                    Log.e("aaa", "多边形有几个：" + polygonMap.size());
                    Log.e("aaa", "别名有：" + aliasname.toString());
                    for (int j = 0; j < markers.size(); j++) {
                        markers.get(j).remove();
                    }
                    //polygons.add(polygon);
                    //polygon = null;
                    latlngs.clear();
                    ids.clear();
                }
            });
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            builder.create().show();


        } else if (view.getId() == R.id.btn_clear) {
            map.getMap().clear();
            latlngs.clear();
            ids.clear();
            markers.clear();
            areas.clear();
            //polygons.clear();
            //用来定位
        } else if (view.getId() == R.id.location) {
            //点击定位按钮，返回自己的位置
            isFirstLocation = true;
            showInfo("返回自己位置");
        } else if (view.getId() == R.id.btn_check) {

            String name = null;
            Polygon polygon = null;
            areas.clear();
            for (int i = 0; i < aliasname.size(); i++) {
                name = aliasname.get(i);
                Log.e("aaa", "检查的别名是：" + name);
                polygon = polygonMap.get(name);
                String s = polygon.getPoints().toString();
                Log.e("aaa", "sssss---->" + s);
                //判断一个点是否在多边形中
                polygonContainsPoint = SpatialRelationUtil.isPolygonContainsPoint(polygon.getPoints(), new LatLng(latitude, longitude));
                if (polygonContainsPoint) {
                    Toast.makeText(this, "该点在 " + name + " 区域内。", Toast.LENGTH_SHORT).show();
                    areas.add(name);
                }
            }
            Log.e("aaa","areas"+areas.toString());
            if (areas.size() > 0) {
                String message = areas.toString();
                showDialog("所在的区域有："+message);
            } else {
                showDialog("该点不在任何区域内。");
            }

        }
    }

    private void showDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("百度地图");

        builder.setMessage(message);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        builder.create().show();
    }


    /**
     * 通过点击地图，来获取坐标
     *
     * @param latLng
     */
    @Override
    public void onMapClick(LatLng latLng) {
        Toast.makeText(this, "坐标是：" + latLng.latitude + ",,," + latLng.longitude, Toast.LENGTH_SHORT).show();
        Log.e("aaa", "ditu d zuobiao is -->" + latLng.latitude + ",,," + latLng.longitude);
        latitude = latLng.latitude;
        longitude = latLng.longitude;
        //向地图添加marker
        addMarler(latitude, longitude);
        if (ids.size() >= 2) {
            drawLine();
        }
    }

    /**
     * 地图上marker的点击事件
     * @param marker
     * @return
     */
    @Override
    public boolean onMarkerClick(final Marker marker) {
        Button button = new Button(getApplicationContext());
        button.setBackgroundResource(R.drawable.popup);
        button.setText("删除");
        button.setTextColor(Color.BLACK);
        //button.setWidth(300);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                marker.remove();
                String id1 = marker.getId();
                ids.remove(id1);
                latlngs.remove(id1);
                Log.e("aaa", "删除后map的size--》" + latlngs.size());
                baidumap.hideInfoWindow();
                if (ids.size() < 2) {
                    if (mPolyline != null) {
                        mPolyline.remove();
                    }
                    return;
                }
                drawLine();
            }
        });
        LatLng ll = marker.getPosition();
        mInfoWindow = new InfoWindow(button, ll, -50);
        baidumap.showInfoWindow(mInfoWindow);
        return true;
    }


    @Override
    public void onMarkerDragEnd(Marker marker) {
        String id = marker.getId();
        Log.e("aaa", "id-->" + id);
        double latitude1 = marker.getPosition().latitude;
        double longitude1 = marker.getPosition().longitude;
        //当拖拽完成后，需要把原来存储的坐标给替换掉
        latlngs.remove(id);
        latlngs.put(id, new LatLng(latitude1, longitude1));
        Toast.makeText(MainActivity.this, "拖拽结束，新位置：" + latitude1 + ", " + longitude1, Toast.LENGTH_LONG).show();

        Log.e("aaa", ids.size() + "---拖拽结束后map 的 " + latlngs.size());
       /* for (int i = 0; i < ids.size(); i++) {
            String s = ids.get(i);
            Log.e("aaa", "key= " + s + " and value= " + latlngs.get(s).toString());
        }*/
        //当拖拽完成后，重新画线
        drawLine();
    }

    @Override
    public void onMapPoiClick(MapPoi mapPoi) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }


    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    /**
     * 如果有大于两个点，就画多边形
     */
    private void drawPolygon() {
        if (polygon != null) {
            polygon.remove();
        }
        LatLng ll = null;
        List<LatLng> pts = new ArrayList<LatLng>();
        for (int i = 0; i < ids.size(); i++) {
            String s = ids.get(i);
            Log.e("aaa", "key= " + s + " and value= " + latlngs.get(s).toString());
            ll = latlngs.get(s);
            pts.add(ll);
        }
        OverlayOptions ooPolygon = new PolygonOptions().points(pts)
                .stroke(new Stroke(5, 0xAA00FF00)).fillColor(0xAAFFFF00);
        polygon = (Polygon) baidumap.addOverlay(ooPolygon);
    }

    /**
     * 如果此时有两个点，就画线
     */
    private void drawLine() {
        if (mPolyline != null) {
            mPolyline.remove();
        }
        List<LatLng> points = new ArrayList<LatLng>();
        LatLng l = null;
        for (int i = 0; i < ids.size(); i++) {
            l = latlngs.get(ids.get(i));
            points.add(l);
        }
        OverlayOptions ooPolyline = new PolylineOptions().width(10)
                .color(0xAAFF0000).points(points);
        mPolyline = (Polyline) baidumap.addOverlay(ooPolyline);
    }

    /**
     * 根据坐标来添加marker
     *
     * @param latitude
     * @param longitude
     */
    private void addMarler(double latitude, double longitude) {
        //定义Maker坐标点
        LatLng point = new LatLng(latitude, longitude);
        //构建Marker图标
        BitmapDescriptor bitmap = BitmapDescriptorFactory
                .fromResource(R.drawable.point);
        //构建MarkerOption，用于在地图上添加Marker
        OverlayOptions option = new MarkerOptions()
                .position(point)
                .icon(bitmap)
                //.zIndex(9)
                .draggable(true);
        //在地图上添加Marker，并显示
        marker = (Marker) baidumap.addOverlay(option);
        markers.add(marker);
        String id = marker.getId();
        latlngs.put(id, new LatLng(latitude, longitude));
        ids.add(id);
    }

    private void initLocation() {
        //定位客户端的设置
        mLocationClient = new LocationClient(this);
        mLocationListener = new MyLocationListener();
        //注册监听
        mLocationClient.registerLocationListener(mLocationListener);
        //配置定位
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll;
        option.setScanSpan(1000);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setIsNeedLocationDescribe(true);//可选，设置是否需要地址描述
        option.setNeedDeviceDirect(false);//可选，设置是否需要设备方向结果
        option.setLocationNotify(false);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        option.setIgnoreKillProcess(true);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        option.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        option.setIsNeedAltitude(false);//可选，默认false，设置定位时是否需要海拔信息，默认不需要，除基础定位版本都可用
        mLocationClient.setLocOption(option);
    }

    private class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            Log.e("aaa", "位置：" + location.getLongitude());
            //将获取的location信息给百度map
            MyLocationData data = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(location.getDirection())
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude())
                    .build();
            baidumap.setMyLocationData(data);
            if (isFirstLocation) {

                LatLng ll = new LatLng(location.getLatitude(),
                        location.getLongitude());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(15.0f);
                baidumap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));

                isFirstLocation = false;
                showInfo("位置：" + location.getAddrStr());
            }
        }

    }


    //显示消息
    private void showInfo(String str) {
        Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onStart() {
        super.onStart();
        //开启定位
       /* baidumap.setMyLocationEnabled(true);
        if (!mLocationClient.isStarted()) {
            mLocationClient.start();
        }*/
    }

    @Override
    protected void onStop() {
        super.onStop();
        //关闭定位
       /* baidumap.setMyLocationEnabled(false);
        if (mLocationClient.isStarted()) {
            mLocationClient.stop();
        }*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 关闭定位图层
        baidumap.setMyLocationEnabled(false);
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        map.onDestroy();
        latlngs.clear();
        ids.clear();
        //polygons.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        map.onPause();
    }


    private void initView() {
        btnYes = (Button) findViewById(R.id.btn_yes);
        btnYes.setOnClickListener(MainActivity.this);
        btnClear = (Button) findViewById(R.id.btn_clear);
        btnClear.setOnClickListener(MainActivity.this);
        location = (ImageView) findViewById(R.id.location);
        location.setOnClickListener(MainActivity.this);
        map = (MapView) findViewById(R.id.map);
        baidumap = map.getMap();
        //给marker设置点击事件，用来删除marker
        baidumap.setOnMarkerClickListener(this);
        //给map设置监听事件，用来拿到点击地图的点的坐标
        baidumap.setOnMapClickListener(this);
        //给marker设置拖拽监听事件，用来获取拖拽完成后的坐标
        baidumap.setOnMarkerDragListener(this);
        btnCheck = (Button) findViewById(R.id.btn_check);
        btnCheck.setOnClickListener(MainActivity.this);
    }


}