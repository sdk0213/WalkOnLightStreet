package com.walkonlight.sdk02.walkonlightstreet;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.navdrawer.SimpleSideDrawer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener,OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks {


    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mGoogleMap = null;
    private Marker currentMarker = null;
    //디폴트 위치, Seoul
    private static final LatLng DEFAULT_LOCATION = new LatLng(37.56, 126.97);
    private static final String TAG = "googlemap_example";
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2002;
    private static final int UPDATE_INTERVAL_MS = 1000;  // 1초
    private static final int FASTEST_UPDATE_INTERVAL_MS = 1000; // 1초

    private ClusterManager<electricbulb> mClusterManager;

    private AppCompatActivity mActivity;
    boolean askPermissionOnceAgain = false;
    ProgressDialog progressDialog;
    TextView LightText,LightTextValue;
    Button PlusButton,MinusButton;
    Button findstreet;
    Button bell;
    Button cctv;
    Button call;
    SimpleSideDrawer slide_menu;
    ImageView LightImage;
    ImageButton delete;
    Intent intent;
    String loginid;
    String mJsonString;
    Double ResultLa=null;
    Double ResultLo=null;
    ImageView rank;
    ImageView bulb2;
    ImageButton findLight;
    ImageButton findall;
    LatLng ResultLan;
    private static final String TAG_JSON="webnautes";
    int PLACE_PICKER_REQUEST = 1;
    ArrayList<Marker> senseMarker;
    ArrayList<Polyline> sensePoly;
    Double hlat=0.0;
    Double hlog=0.0;

    PolylineOptions po;
    Polyline polyline;
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);



        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mActivity = this;
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        intent = getIntent();
        loginid = intent.getExtras().getString("id");

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addApi(LocationServices.API)
                .enableAutoManage(this, this)
                .build();


    }

    @Override
    protected void onStart() {                      // 시작
        super.onStart();
        //if (mGoogleApiClient != null)
        // mGoogleApiClient.connect();
    }
    @Override
    public void onResume() {                            // 인터넷연결 허가 확인
        super.onResume();
        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();
        //앱 정보에서 퍼미션을 허가했는지를 다시 검사해봐야 한다.
        if (askPermissionOnceAgain) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                askPermissionOnceAgain = false;
                checkPermissions();
            }
        }
    }
    @Override

    protected void onStop() {                                                            //   구글 API클라이언트 연결 설정
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override

    public void onPause() {                                                              //   구글 API클라이언트 연결 설정
        //위치 업데이트 중지
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {                                                           //   구글 API클라이언트 연결 설정
        if (mGoogleApiClient != null) {
            mGoogleApiClient.unregisterConnectionCallbacks(this);
            mGoogleApiClient.unregisterConnectionFailedListener(this);
            if (mGoogleApiClient.isConnected()) {
                mGoogleApiClient.disconnect();
            }
        }
        super.onDestroy();
    }

    void callPlacePicker() {
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        try {

            startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public class electricbulb implements ClusterItem {
        private LatLng location;
        private String address;

        public electricbulb(LatLng location, String address){
            this.location = location;
            this.address = address;
        }

        public LatLng getLocation(){
            return location;
        }

        public void setLocation(LatLng location){
            this.location = location;
        }

        public String getAddress(){
            return address;
        }

        public  LatLng getPosition(){
            return location;
        }

        public void setAddress(String address){
            this.address = address;
        }
    }


    class OwnIconRendered extends DefaultClusterRenderer<electricbulb> {

        public OwnIconRendered(Context context, GoogleMap map,
                               ClusterManager<electricbulb> clusterManager) {
            super(context, map, mClusterManager);
        }

        @Override
        protected void onBeforeClusterItemRendered(electricbulb item, MarkerOptions markerOptions) {
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.bulblight1));
            super.onBeforeClusterItemRendered(item, markerOptions);
        }
    }

    public void SetIconImage(MarkerOptions Light,String brightness){
        if (brightness.equals("1") || brightness.equals("2")) {
            Light.icon(BitmapDescriptorFactory.fromResource(R.drawable.sun_1));
        }
        else if (brightness.equals("3") || brightness.equals("4")) {
            Light.icon(BitmapDescriptorFactory.fromResource(R.drawable.sun_2));
        }
        else if (brightness.equals("5") || brightness.equals("6")) {
            Light.icon(BitmapDescriptorFactory.fromResource(R.drawable.sun_3));
        }
        else if (brightness.equals("7") || brightness.equals("8")) {
            Light.icon(BitmapDescriptorFactory.fromResource(R.drawable.sun_4));
        }
        else {
            Light.icon(BitmapDescriptorFactory.fromResource(R.drawable.sun_5));
        }
    }

    public void SetSelectedIconImage(Marker Light, String brightness){
        if (brightness.equals("1") || brightness.equals("2")) {
            Light.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.sun_1));
        }
        else if (brightness.equals("3") || brightness.equals("4")) {
            Light.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.sun_2));
        }
        else if (brightness.equals("5") || brightness.equals("6")) {
            Light.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.sun_3));
        }
        else if (brightness.equals("7") || brightness.equals("8")) {
            Light.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.sun_4));
        }
        else{
            Light.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.sun_5));
        }
    }

    public void SetSelectedImageView(ImageView Light,String brightness){
        if (brightness.equals("1") || brightness.equals("2")) {
            Light.setImageResource(R.drawable.sun_1);
        }
        else if (brightness.equals("3") || brightness.equals("4")) {
            Light.setImageResource(R.drawable.sun_2);
        }
        else if (brightness.equals("5") || brightness.equals("6")) {
            Light.setImageResource(R.drawable.sun_3);
        }
        else if (brightness.equals("7") || brightness.equals("8")) {
            Light.setImageResource(R.drawable.sun_4);
        }
        else{
            Light.setImageResource(R.drawable.sun_5);
        }
    }

    public void hpos(Location location){
        hlat = location.getLatitude();
        hlog = location.getLongitude();
    }
    public void onMapReady(final GoogleMap map) {                               // 맵이 준비가된다면
        Log.d(TAG, "onMapReady");               // 기록 : 맵준비중...


        Toast.makeText(getApplicationContext(),"현재위치로 이동합니다.",Toast.LENGTH_SHORT).show();

        mGoogleMap = map;                   // 맵객체 생성

        mGoogleMap.getUiSettings().isMapToolbarEnabled();
        mGoogleMap.getUiSettings().isMyLocationButtonEnabled();
        mGoogleMap.getUiSettings().setTiltGesturesEnabled(true);
        mGoogleMap.getUiSettings().setZoomControlsEnabled(true);
        mGoogleMap.getUiSettings().setCompassEnabled(true);                                                  //구굴 ui 컴퍼스가능하게하기
        //map.setMapType(GoogleMap.MAP_TYPE_HYBRID);



        //        MarkerOptions Light = new MarkerOptions();// 새로운 마커 클래스 생성
        //        Light.icon(BitmapDescriptorFactory.fromResource(R.drawable.bulblight2));        // 마커의 아이콘 (기본 밝기)로 생성         // 포지션은 latLng (현재 클릭한 곳)
        //        Light.alpha(0.8f);
        //        mGoogleMap.addMarker(Light).showInfoWindow();



        MyLocation.LocationResult locationResult = new MyLocation.LocationResult() {
            @Override
            public void gotLocation(Location location) {
                mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(40));
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
            }
        };
        MyLocation myLocation = new MyLocation();
        myLocation.getLocation(getApplicationContext(), locationResult);




        PlusButton = (Button) findViewById(R.id.btn_up);
        MinusButton = (Button) findViewById(R.id.btn_down);
        LightText = (TextView) findViewById(R.id.textLight);
        LightTextValue = (TextView) findViewById(R.id.textLightValue);
        LightImage = (ImageView) findViewById(R.id.bulb);                       //빛 이미지
        findstreet = (Button)findViewById(R.id.findstreet);
        bell = (Button)findViewById(R.id.btn_bell);
        cctv = (Button)findViewById(R.id.btn_cctv);
        call = (Button)findViewById(R.id.btn_call);
        PlusButton.setVisibility(View.INVISIBLE);                        // 안보이게 만들기
        MinusButton.setVisibility(View.INVISIBLE);
        LightText.setVisibility(View.INVISIBLE);
        LightTextValue.setVisibility(View.INVISIBLE);
        LightImage.setVisibility(View.INVISIBLE);
        bell.setVisibility(View.INVISIBLE);
        cctv.setVisibility(View.INVISIBLE);
        findstreet.setVisibility(View.VISIBLE);
        call.setVisibility(View.INVISIBLE);


        rank = (ImageButton)findViewById(R.id.findrank);
        findLight = (ImageButton) findViewById(R.id.findLight);
        findall = (ImageButton) findViewById(R.id.findall);

        delete = (ImageButton) findViewById(R.id.delete);

        slide_menu = new SimpleSideDrawer(this);
        slide_menu.setLeftBehindContentView(R.layout.activity_rank);

        senseMarker = new ArrayList<>();
        sensePoly = new ArrayList<>();

        findstreet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(sensePoly.isEmpty()!=true)
                sensePoly.get(sensePoly.size()-1).remove();
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                callPlacePicker();
            }
        });


        rank.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                slide_menu.toggleLeftDrawer();
            }
        });                                             // 랭크


        findLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),"밝은 빛만 보기",Toast.LENGTH_SHORT).show();
                GetDataLocation GetDL = new GetDataLocation("dataloadonly");               // 서버에서 빛 데이터 얻어오기
                GetDL.execute("https://sdk0213.000webhostapp.com/zyro/lightstreet/SelectLocation.php");      // 실행
            }
        });

        findall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "모든 빛 보기", Toast.LENGTH_SHORT).show();
                GetDataLocation GetDL = new GetDataLocation("dataloadall");               // 서버에서 빛 데이터 얻어오기
                GetDL.execute("https://sdk0213.000webhostapp.com/zyro/lightstreet/SelectLocation.php");      // 실행
            }
        });

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//               if(ResultLa != null){
//                    po.visible(false);
//                }
                if(sensePoly.isEmpty()!=true)
                    sensePoly.get(sensePoly.size()-1).remove();
                mGoogleMap.clear();
//                ResultLa=null;
//                GetDataLocation GetDL = new GetDataLocation("dataloaddelete");               // 서버에서 빛 데이터 얻어오기
//                GetDL.execute("https://sdk0213.000webhostapp.com/zyro/lightstreet/SelectLocation.php");      // 실행
                Toast.makeText(getApplicationContext(),"맵을 깨끗히합니다.(길찾기종료)",Toast.LENGTH_SHORT).show();

            }
        });



        call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),"해당 정보가 신고접수 되었습니다.\n  신속히 처리하겠습니다.\n  감사합니다.",Toast.LENGTH_SHORT).show();
            }
        });
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                call.setVisibility(View.INVISIBLE);// 맵을 클릭했을때 마커 설정 버튼 닫기
                PlusButton.setVisibility(View.INVISIBLE);
                MinusButton.setVisibility(View.INVISIBLE);
                LightText.setVisibility(View.INVISIBLE);
                LightTextValue.setVisibility(View.INVISIBLE);
                LightImage.setVisibility(View.INVISIBLE);
                bell.setVisibility(View.INVISIBLE);
                cctv.setVisibility(View.INVISIBLE);
            }
        });

        GetDataLocation GetDL = new GetDataLocation("dataload");               // 서버에서 빛 데이터 얻어오기
        GetDL.execute("https://sdk0213.000webhostapp.com/zyro/lightstreet/SelectLocation.php");      // 실행

        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {                                                                                     // 맵을 끝까지 누르면 마커 생성

                final LatLng tolatLng = latLng;
                MyLocation.LocationResult locationResult = new MyLocation.LocationResult() {
                    public void gotLocation(Location location) {
                        hpos(location);
                        Location locationC = new Location("point C");                                  // 현재 마커 Location A 를  생성
                        locationC.setLatitude(hlat);                                        // 현재 생성하려고하는 마커의 Latitude 정보를 Location A에저장
                        locationC.setLongitude(hlog);                                      // 현재 생성하려고하는 마커의 Longitude 정보를 Location A에저장

                        Location locationD = new Location("point D");
                        locationD.setLatitude(tolatLng.latitude);         // 커서에서 받아온 다른 마커의 Latitude 정보를 Location B에저장
                        locationD.setLongitude(tolatLng.longitude);        // 커서에서 받아온 다른 마커의 Longitude 정보  Location B에저장
                        double distance = locationC.distanceTo(locationD);

//                        if ((int) distance <= 99) {
                            GetDataLocation GetDL = new GetDataLocation("makemarker",tolatLng);               // 서버에서 읽어와서 비교쓰레드후에 생성
                            GetDL.execute("https://sdk0213.000webhostapp.com/zyro/lightstreet/SelectLocation.php");      // 실행                                  // 만약에 LocationA 랑 Location B랑 8미터보다 작으면
//                        }


                    }
                };

                MyLocation myLocation = new MyLocation();
                myLocation.getLocation(getApplicationContext(), locationResult);
            }
        });

        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                call.setVisibility(View.VISIBLE);
                map.animateCamera(CameraUpdateFactory.zoomTo(20));                                         // 구글맵 줌 40로설정
                PlusButton.setVisibility(View.VISIBLE);
                MinusButton.setVisibility(View.VISIBLE);
                LightText.setVisibility(View.VISIBLE);
                LightTextValue.setVisibility(View.VISIBLE);
                LightImage.setVisibility(View.VISIBLE);
                bell.setVisibility(View.VISIBLE);
                cctv.setVisibility(View.VISIBLE);


                GetDataLocation GetDL = new GetDataLocation("selectedmarker", marker, "Click");               // 서버에서 읽어와서 비교쓰레드후에 생성
                GetDL.execute("https://sdk0213.000webhostapp.com/zyro/lightstreet/SelectLocation.php");      // 실행

                PlusButton.setTag(marker);
                MinusButton.setTag(marker);
                bell.setTag(marker);
                cctv.setTag(marker);

                return  false;
            }
        });


        bell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cctv.setVisibility(View.INVISIBLE);
                PlusButton.setVisibility(View.INVISIBLE);
                MinusButton.setVisibility(View.INVISIBLE);
                Marker marker1 = (Marker) v.getTag();
                GetDataLocation GetDL = new GetDataLocation("selectedmarker", marker1, "bell");               // 서버에서 읽어와서 비교쓰레드후에 생성
                GetDL.execute("https://sdk0213.000webhostapp.com/zyro/lightstreet/SelectLocation.php");      // 실행
            }
        });

        cctv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bell.setVisibility(View.INVISIBLE);
                PlusButton.setVisibility(View.INVISIBLE);
                MinusButton.setVisibility(View.INVISIBLE);
                Marker marker1 = (Marker) v.getTag();
                GetDataLocation GetDL = new GetDataLocation("selectedmarker", marker1, "cctv");               // 서버에서 읽어와서 비교쓰레드후에 생성
                GetDL.execute("https://sdk0213.000webhostapp.com/zyro/lightstreet/SelectLocation.php");      // 실행
            }
        });

        PlusButton.setOnClickListener(new View.OnClickListener() {
            @Override

            public void onClick(View v) {
                Marker marker1 = (Marker) v.getTag();
                GetDataLocation GetDL = new GetDataLocation("selectedmarker", marker1, "Plus");               // 서버에서 읽어와서 비교쓰레드후에 생성
                GetDL.execute("https://sdk0213.000webhostapp.com/zyro/lightstreet/SelectLocation.php");      // 실행


            }
        });

        MinusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Marker marker1 = (Marker) v.getTag();
                GetDataLocation GetDL = new GetDataLocation("selectedmarker",marker1,"Minus");               // 서버에서 읽어와서 비교쓰레드후에 생성
                GetDL.execute("https://sdk0213.000webhostapp.com/zyro/lightstreet/SelectLocation.php");

            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {                                       // 구글맵 실행가능한 런타임 설정 (건들지않기)
            //API 23 이상이면 런타임 퍼미션 처리 필요
            int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);
            if (hasFineLocationPermission == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(mActivity,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            } else {
                if (mGoogleApiClient == null) {
                    buildGoogleApiClient();
                }
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    mGoogleMap.setMyLocationEnabled(true);
                    mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(15));                                         // 구글맵 줌 15로설정
                }
            }
        } else {
            if (mGoogleApiClient == null) {
                buildGoogleApiClient();
            }
            mGoogleMap.setMyLocationEnabled(true);
            mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(15));                                         // 구글맵 줌 15로설정
        }
    }

    private class GetDataLocation extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;                          // 로딩중입니다 뜨게하는 표시
        String errorString = null;
        String mode;
        String manage;
        LatLng latLng;
        Marker marker;

        public GetDataLocation(String mode){
            this.mode = mode;
        }
        public GetDataLocation(String mode,LatLng latLng){
            this.mode = mode;
            this.latLng = latLng;
        }
        public GetDataLocation(String mode,Marker marker,String manage){
            this.mode = mode;
            this.marker = marker;
            this.manage = manage;
        }
        @Overridee
        protected void onPreExecute() {                          // 초기화
            super.onPreExecute();
            progressDialog = ProgressDialog.show(MainActivity.this, "잠시만기다려주세요", null, true, true);   //로딩중입니다를 please wait로 바꿔서 외침
        }

        @Override
        protected void onPostExecute(String result) {           // 일처리 끝나고
            super.onPostExecute(result);
            progressDialog.dismiss();                       // 로딩중입니다. 끝내기
            Log.d(TAG, "response  - " + result);

            if (result == null) {                                // 결과값이 null이라면 에러 스트링 보내기
                Toast.makeText(getApplicationContext(),errorString,Toast.LENGTH_SHORT).show();
            }
            else {
                mJsonString = result;                              // 결과값을 mJsonString 스트링에 저장
                if(mode.equals("dataload")) {
                    showResultLocation();
                }                           // 결과값을 mJsonString 스트링에 저장
                else if(mode.equals("dataloadonly")) {
                    showResultLocationonly();
                }
                else if(mode.equals("dataloadall")) {
                    showResultLocationall();
                }
                else if(mode.equals("dataloaddelete")) {
                    showResultLocationdelete();
                }
                else if (mode.equals("makemarker")){
                    showResultMarker(latLng);
                }
                else if (mode.equals("selectedmarker")){
                    showResultselectedMarker(marker,manage);
                }
            }
        }
        @Override
        protected String doInBackground(String... params) {          // 쓰레드 돌기 param에 URL 볼러오기

            String serverURL = params[0];               //  param에 URL 저장된거 불러오기
            try {

                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();     // url 에 연결하기

                httpURLConnection.setReadTimeout(5000);                         // 5초동안 못읽으면 타임아웃
                httpURLConnection.setConnectTimeout(5000);                      // 5초동안 못연결시 타임아웃
                httpURLConnection.connect();                                    // 연결


                int responseStatusCode = httpURLConnection.getResponseCode();          // url 연결 잘되어있는 확인하는 플레그?
                Log.d(TAG, "response code - " + responseStatusCode);

                InputStream inputStream;
                if (responseStatusCode == HttpURLConnection.HTTP_OK) {       // url연결이 잘되었다면
                    inputStream = httpURLConnection.getInputStream();           // url에서 stream 가져오기
                } else {
                    inputStream = httpURLConnection.getErrorStream();           // url에서 에러 가져오기
                }


                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");      // 가져온 inputstream 을 UTF-8 형식으로 저장하기
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);          //  가져온것들 버퍼에 저장하고

                StringBuilder sb = new StringBuilder();         // 문자열수정이 용이한 StringBuilder를 선언
                String line;

                while ((line = bufferedReader.readLine()) != null) {      // 버퍼리더에서 한라인씩 읽어서 null이 될때까지
                    sb.append(line);                                    // StringBuilder에 저장하기
                }


                bufferedReader.close();                         // 버퍼리더를 모두 썻으니 끝내기


                return sb.toString().trim();                // Stringbuilder에 저장된것을 string으로 바꾸고 맨앞 맨끝 공백 없애서 반환하기  이것이 result가 됨


            } catch (Exception e) {

                Log.d(TAG, "InsertData: Error ", e);
                errorString = e.toString();                 // 에러가 나면 에러를 스트링으로 바꾸고 에러에 저장

                return null;
            }

        }
    }

    private void showResultLocation(){                            ////// 마커(모두포함) 생성하기
        try {
            JSONObject jsonObject = new JSONObject(mJsonString);        // mJsonString은 읽어온 결과값이 저장된곳임 그것은 jsonObject를 사용하여 읽기 편하기 만들기
            JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);       // jsonarray는 jsonobject가 들어가는 배열을 뜻함

            //            Lightmarker = new Marker[jsonArray.length()];


            for(int i=0;i<jsonArray.length();i++){      // 제이슨배열 길이만큼 반복하기
                JSONObject item = jsonArray.getJSONObject(i);       // 제이슨배열에서 오브젝트를 item에 저장하고

                String mark_id = item.getString("mark_id");     // 아이템에서 TAG_ID "id" 부분
                String latitude = item.getString("latitude");     // 아이템에서 TAG_ID "pw" 부분
                String longitude = item.getString("longitude");     // 아이템에서 TAG_ID "pw" 부분
                String brightness = item.getString("brightness");     // 아이템에서 TAG_ID "pw" 부분
                String id = item.getString("id");     // 아이템에서 TAG_ID "pw" 부분
                String what = item.getString("what");     // 아이템에서 TAG_ID "pw" 부분

                MarkerOptions Light = new MarkerOptions();
                Light.title("만든이 : "+id);
                Light.position(new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude)));
                SetIconImage(Light,brightness);
                if(what.equals("bell")){
                    Light.icon(BitmapDescriptorFactory.fromResource(R.drawable.bell));
                }
                else if(what.equals("cctv")) {
                    Light.icon(BitmapDescriptorFactory.fromResource(R.drawable.cctv));
                }
                senseMarker.add(mGoogleMap.addMarker(Light));
                senseMarker.get(i).showInfoWindow();
                //                Lightmarker[i] = mGoogleMap.addMarker(Light);
                //



            }

        } catch (JSONException e) {
            Log.d(TAG, "showResult : ", e);
        }

    }
    private void showResultLocationonly(){                             ////// 마커(밝은곳만) VISIBLE
        try {
            JSONObject jsonObject = new JSONObject(mJsonString);        // mJsonString은 읽어온 결과값이 저장된곳임 그것은 jsonObject를 사용하여 읽기 편하기 만들기
            JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);       // jsonarray는 jsonobject가 들어가는 배열을 뜻함

            //            mClusterManager = new ClusterManager<electricbulb>(this,mGoogleMap);
            //            mGoogleMap.setOnCameraIdleListener(mClusterManager);
            //            mGoogleMap.setOnMarkerClickListener(mClusterManager);
            //            mClusterManager.setRenderer(new OwnIconRendered(MainActivity.this, mGoogleMap, mClusterManager));

            for(int i=0;i<jsonArray.length();i++){      // 제이슨배열 길이만큼 반복하기
                JSONObject item = jsonArray.getJSONObject(i);       // 제이슨배열에서 오브젝트를 item에 저장하고

                String mark_id = item.getString("mark_id");     // 아이템에서 TAG_ID "id" 부분
                String latitude = item.getString("latitude");     // 아이템에서 TAG_ID "pw" 부분
                String longitude = item.getString("longitude");     // 아이템에서 TAG_ID "pw" 부분
                String brightness = item.getString("brightness");     // 아이템에서 TAG_ID "pw" 부분
                String id = item.getString("id");     // 아이템에서 TAG_ID "pw" 부분

                if(Integer.parseInt(brightness) < 5){
                    senseMarker.get(i).setVisible(false);
                }


            }

        } catch (JSONException e) {
            Log.d(TAG, "showResult : ", e);
        }

    }
    private void showResultLocationall(){                             ////// 마커(밝은곳만) VISIBLE
        try {
            JSONObject jsonObject = new JSONObject(mJsonString);        // mJsonString은 읽어온 결과값이 저장된곳임 그것은 jsonObject를 사용하여 읽기 편하기 만들기
            JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);       // jsonarray는 jsonobject가 들어가는 배열을 뜻함

            //            mClusterManager = new ClusterManager<electricbulb>(this,mGoogleMap);
            //            mGoogleMap.setOnCameraIdleListener(mClusterManager);
            //            mGoogleMap.setOnMarkerClickListener(mClusterManager);
            //            mClusterManager.setRenderer(new OwnIconRendered(MainActivity.this, mGoogleMap, mClusterManager));

            for(int i=0;i<jsonArray.length();i++){      // 제이슨배열 길이만큼 반복하기
                JSONObject item = jsonArray.getJSONObject(i);       // 제이슨배열에서 오브젝트를 item에 저장하고

                String mark_id = item.getString("mark_id");     // 아이템에서 TAG_ID "id" 부분
                String latitude = item.getString("latitude");     // 아이템에서 TAG_ID "pw" 부분
                String longitude = item.getString("longitude");     // 아이템에서 TAG_ID "pw" 부분
                String brightness = item.getString("brightness");     // 아이템에서 TAG_ID "pw" 부분
                String id = item.getString("id");     // 아이템에서 TAG_ID "pw" 부분

                senseMarker.get(i).setVisible(true);

            }

        } catch (JSONException e) {
            Log.d(TAG, "showResult : ", e);
        }

    }

    private void showResultLocationdelete(){                             ////// 마커(밝은곳만) VISIBLE
        try {
            JSONObject jsonObject = new JSONObject(mJsonString);        // mJsonString은 읽어온 결과값이 저장된곳임 그것은 jsonObject를 사용하여 읽기 편하기 만들기
            JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);       // jsonarray는 jsonobject가 들어가는 배열을 뜻함

            //            mClusterManager = new ClusterManager<electricbulb>(this,mGoogleMap);
            //            mGoogleMap.setOnCameraIdleListener(mClusterManager);
            //            mGoogleMap.setOnMarkerClickListener(mClusterManager);
            //            mClusterManager.setRenderer(new OwnIconRendered(MainActivity.this, mGoogleMap, mClusterManager));

            for(int i=0;i<jsonArray.length();i++){      // 제이슨배열 길이만큼 반복하기
                JSONObject item = jsonArray.getJSONObject(i);       // 제이슨배열에서 오브젝트를 item에 저장하고

                String mark_id = item.getString("mark_id");     // 아이템에서 TAG_ID "id" 부분
                String latitude = item.getString("latitude");     // 아이템에서 TAG_ID "pw" 부분
                String longitude = item.getString("longitude");     // 아이템에서 TAG_ID "pw" 부분
                String brightness = item.getString("brightness");     // 아이템에서 TAG_ID "pw" 부분
                String id = item.getString("id");     // 아이템에서 TAG_ID "pw" 부분

                senseMarker.get(i).setVisible(false);

            }

        } catch (JSONException e) {
            Log.d(TAG, "showResult : ", e);
        }

    }


    private void showResultselectedMarker(Marker marker,String manage){
        try {
            JSONObject jsonObject = new JSONObject(mJsonString);        // mJsonString은 읽어온 결과값이 저장된곳임 그것은 jsonObject를 사용하여 읽기 편하기 만들기
            JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);       // jsonarray는 jsonobject가 들어가는 배열을 뜻함
            String mark_id;
            String latitude; // 아이템에서 TAG_ID "pw" 부분
            String longitude;  // 아이템에서 TAG_ID "pw" 부분
            String brightness;     // 아이템에서 TAG_ID "pw" 부분
            String id;     // 아이템에서 TAG_ID "pw" 부분
            String what;     // 아이템에서 TAG_ID "what" 부분

            for(int i=0;i<jsonArray.length();i++){      // 제이슨배열 길이만큼 반복하기
                JSONObject item = jsonArray.getJSONObject(i);       // 제이슨배열에서 오브젝트를 item에 저장하고

                mark_id = item.getString("mark_id");     // 아이템에서 TAG_ID "id" 부분
                //                latitude = item.getString("latitude");     // 아이템에서 TAG_ID "pw" 부분
                //                longitude = item.getString("longitude");     // 아이템에서 TAG_ID "pw" 부분
                brightness = item.getString("brightness");     // 아이템에서 TAG_ID "pw" 부분
                //                id = item.getString("id");     // 아이템에서 TAG_ID "pw" 부분
                what = item.getString("what");     // 아이템에서 TAG_ID "pw" 부분


                if(marker.getId().replaceAll("[^0-9]","").toString().equals(mark_id)){
                    int bright = Integer.parseInt(brightness);
                    Toast.makeText(getApplicationContext(),"sql id = "+ mark_id+ "currentmarker id = "+marker.getId().replaceAll("[^0-9]","").toString(),Toast.LENGTH_SHORT).show();
                    if(what.equals("bell")){
                        bell.setVisibility(View.INVISIBLE);
                        cctv.setVisibility(View.INVISIBLE);
                        PlusButton.setVisibility(View.INVISIBLE);
                        MinusButton.setVisibility(View.INVISIBLE);
                        LightText.setVisibility(View.INVISIBLE);
                        LightTextValue.setText("비상벨");
                        LightImage.setImageResource(R.drawable.bell);
                        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.bell));

                    }
                    else if(what.equals("cctv")){
                        bell.setVisibility(View.INVISIBLE);
                        cctv.setVisibility(View.INVISIBLE);
                        PlusButton.setVisibility(View.INVISIBLE);
                        MinusButton.setVisibility(View.INVISIBLE);
                        LightText.setVisibility(View.INVISIBLE);
                        LightTextValue.setText("cctv");
                        LightImage.setImageResource(R.drawable.cctv);
                        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.cctv));
                    }

                    if(manage.equals("Click")){
                        LightTextValue.setText(brightness);
                        SetSelectedImageView(LightImage,brightness);
                        SetSelectedIconImage(marker,brightness);
                        if(what.equals("bell")){
                            LightTextValue.setText("비상벨");
                            LightImage.setImageResource(R.drawable.bell);
                            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.bell));
                        }
                        else if(what.equals("cctv")){
                            LightTextValue.setText("cctv");
                            LightImage.setImageResource(R.drawable.cctv);
                            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.cctv));
                        }
                    }

                    else if(manage.equals("Plus")){
                        bright++;
                        if(bright < 1 || bright > 10){
                            Toast.makeText(getApplicationContext(),"빛의밝기는 1~10사이만가능합니다.",Toast.LENGTH_SHORT).show();
                        }
                        else{
                            ManageDataLocation updateDL = new ManageDataLocation("update");
                            updateDL.execute(mark_id,bright+"");
                            String Light = "" + bright;
                            SetSelectedIconImage(marker,Light);
                            SetSelectedImageView(LightImage,Light);
                            LightTextValue.setText(Light);
                        }
                    }
                    else if(manage.equals("Minus")){
                        bright--;
                        if(bright < 1 || bright > 10){
                            Toast.makeText(getApplicationContext(),"빛의밝기는 1~10사이만가능합니다.",Toast.LENGTH_SHORT).show();
                        }
                        else{
                            ManageDataLocation updateDL = new ManageDataLocation("update");
                            updateDL.execute(mark_id,bright+"");
                            String Light = "" + bright;
                            SetSelectedIconImage(marker,Light);
                            SetSelectedImageView(LightImage,Light);
                            LightTextValue.setText(Light);
                        }
                    }
                    else if(manage.equals("bell")){
                        bell.setVisibility(View.INVISIBLE);
                        cctv.setVisibility(View.INVISIBLE);
                        PlusButton.setVisibility(View.INVISIBLE);
                        MinusButton.setVisibility(View.INVISIBLE);
                        LightText.setVisibility(View.INVISIBLE);
                        String bell = "bell";
                        ManageDataLocation updateDL = new ManageDataLocation("updatewhat");
                        updateDL.execute(mark_id,bell);
                        LightTextValue.setText("비상벨");
                        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.bell));
                        LightImage.setImageResource(R.drawable.bell);

                    }
                    else if(manage.equals("cctv")){
                        bell.setVisibility(View.INVISIBLE);
                        cctv.setVisibility(View.INVISIBLE);
                        PlusButton.setVisibility(View.INVISIBLE);
                        MinusButton.setVisibility(View.INVISIBLE);
                        LightText.setVisibility(View.INVISIBLE);
                        LightTextValue.setText("cctv");
                        String cctv = "cctv";
                        ManageDataLocation updateDL = new ManageDataLocation("updatewhat");
                        updateDL.execute(mark_id,cctv);
                        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.cctv));
                        LightImage.setImageResource(R.drawable.cctv);

                    }
                    break;
                }
            }

        } catch (JSONException e) {
            Log.d(TAG, "showResult : ", e);
        }

    }

    private void showResultMarker(LatLng latLng){                                       // 마커 생성 함수
        boolean distanceshort =false;
        String mark_id = null;
        String latitude = null;    // 아이템에서 TAG_ID "pw" 부분
        String longitude = null;     // 아이템에서 TAG_ID "pw" 부분
        String brightness = null;     // 아이템에서 TAG_ID "pw" 부분
        String id = null;    // 아이템에서 TAG_ID "pw" 부분

        try {
            JSONObject jsonObject = new JSONObject(mJsonString);        // mJsonString은 읽어온 결과값이 저장된곳임 그것은 jsonObject를 사용하여 읽기 편하기 만들기
            JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);       // jsonarray는 jsonobject가 들어가는 배열을 뜻함
            for(int i=0;i<jsonArray.length();i++){      // 제이슨배열 길이만큼 반복하기
                JSONObject item = jsonArray.getJSONObject(i);       // 제이슨배열에서 오브젝트를 item에 저장하고

                mark_id = item.getString("mark_id");     // 아이템에서 TAG_ID "id" 부분
                latitude = item.getString("latitude");     // 아이템에서 TAG_ID "pw" 부분
                longitude = item.getString("longitude");     // 아이템에서 TAG_ID "pw" 부분
                brightness = item.getString("brightness");     // 아이템에서 TAG_ID "pw" 부분
                id = item.getString("id");     // 아이템에서 TAG_ID "pw" 부분

                Location locationA = new Location("point A");                                  // 현재 마커 Location A 를  생성
                locationA.setLatitude(latLng.latitude);                                        // 현재 생성하려고하는 마커의 Latitude 정보를 Location A에저장
                locationA.setLongitude(latLng.longitude);                                      // 현재 생성하려고하는 마커의 Longitude 정보를 Location A에저장

                Location locationB = new Location("point B");
                locationB.setLatitude(Double.parseDouble(latitude));         // 커서에서 받아온 다른 마커의 Latitude 정보를 Location B에저장
                locationB.setLongitude(Double.parseDouble(longitude));        // 커서에서 받아온 다른 마커의 Longitude 정보  Location B에저장

                double distance = locationA.distanceTo(locationB);
                if ((int) distance <= 15) {                                              // 만약에 LocationA 랑 Location B랑 8미터보다 작으면
                    distanceshort = true;
                    break;                                                                                              // 하나라도 짧으면 바로 while비교문 종료
                }                                   // 현재 생성하려고하는 마커의 Longitude 정보를 Location A에저장
                else {
                    distanceshort = false;
                }

            }

            if(distanceshort == true){
                Toast.makeText(getApplicationContext(), "다른 빛과의 거리가 너무 15m 이상이여야 빛을 생성가능합니다.", Toast.LENGTH_SHORT).show();     //거리가 짧다고 표시
            }
            else{
                ManageDataLocation insertDL = new ManageDataLocation("insert");                                                                               // 마커 생성
                insertDL.execute((Integer.parseInt(mark_id)+1)+"",latLng.latitude+"",latLng.longitude+"","5",loginid,"");                               //  mark_id 데이터베이스내 마커 갯수마지막번호 + 1을 해서 생성하고
                MarkerOptions Light = new MarkerOptions();// 새로운 마커 클래스 생성
                Light.icon(BitmapDescriptorFactory.fromResource(R.drawable.sun_3));        // 마커의 아이콘 (기본 밝기)로 생성
                Light.position(latLng);
                senseMarker.add(mGoogleMap.addMarker(Light));
                senseMarker.get(senseMarker.size()-1).showInfoWindow();
                //mGoogleMap.addMarker(Light).showInfoWindow();
            }
        } catch (JSONException e) {
            Log.d(TAG, "showResult : ", e);
        }

    }

    class ManageDataLocation extends AsyncTask<String, Void, String> {                                          // 데이터 추가
        ProgressDialog progressDialog;
        String manage;
        public ManageDataLocation(String manage){
            this.manage = manage;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(MainActivity.this, "빛를 업데이트중입니다...", null, true, true);
        }


        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
            Toast.makeText(getApplicationContext(),result,Toast.LENGTH_SHORT).show();
            Log.d(TAG, "POST response - " + result);
        }


        protected String doInBackground(String... params) {

            String serverURL = null;
            String postParameters = null;
            String mark_id  = null;     // 마커번호,위도,경도,밝기,생성장id
            String latitude  = null;
            String longitude  = null;
            String brightness = null;
            String connectid  = null;
            String markwhat  = null;
            if(manage.equals("insert")) {
                mark_id = (String) params[0];     // 마커번호,위도,경도,밝기,생성장id
                latitude = (String) params[1];
                longitude = (String) params[2];
                brightness = (String) params[3];
                connectid = (String) params[4];
                markwhat = (String) params[5];
                serverURL = "https://sdk0213.000webhostapp.com/zyro/lightstreet/InsertLocation.php";
                postParameters = "mark_id=" + mark_id + "&latitude=" + latitude + "&longitude=" + longitude + "&brightness=" + brightness + "&id=" + connectid + "&mark=" + markwhat;     // 여기다가저장
            }
            else if(manage.equals("update")){
                mark_id = (String) params[0];
                brightness = (String) params[1];
                serverURL = "https://sdk0213.000webhostapp.com/zyro/lightstreet/UpdateLocationBrightness.php";
                postParameters = "mark_id=" + mark_id + "&brightness=" + brightness;     // 여기다가저장
            }
            else if(manage.equals("updatewhat")){
                mark_id = (String) params[0];
                markwhat = (String) params[1];
                serverURL = "https://sdk0213.000webhostapp.com/zyro/lightstreet/UpdateLocationWhat.php";
                postParameters = "mark_id=" + mark_id + "&what=" + markwhat;     // 여기다가저장
            }



            try {

                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                //httpURLConnection.setRequestProperty("content-type", "application/json");
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();

                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));       // 여기서 쓰기
                outputStream.flush();
                outputStream.close();

                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d(TAG, "POST response code - " + responseStatusCode);

                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                }
                else{
                    inputStream = httpURLConnection.getErrorStream();
                }

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line = null;

                while((line = bufferedReader.readLine()) != null){
                    sb.append(line);
                }

                bufferedReader.close();

                return sb.toString();


            } catch (Exception e) {

                Log.d(TAG, "manageData: Error ", e);

                return new String("Error: " + e.getMessage());
            }

        }
    }



    public void onLocationChanged(Location location) {                                          // 로케이션이 바뀔때 해주는것 다삭제해버림

        //        Log.d(TAG, "onLocationChanged");
        //        String markerTitle = getCurrentAddress(location);
        //
        //        String markerSnippet = "위도:" + String.valueOf(location.getLatitude())
        //
        //                + " 경도:" + String.valueOf(location.getLongitude());
        //
        //
        //        //현재 위치에 마커 생성
        //
        //        setCurrentLocation(location, markerTitle, markerSnippet);
    }

    ////////////////// 여기아래로는 구글맵 기본기능 설정에 관한것 안걸들여도됨 ///////////////////
    ////////////////// 여기아래로는 구글맵 기본기능 설정에 관한것 안걸들여도됨 ///////////////////
    ////////////////// 여기아래로는 구글맵 기본기능 설정에 관한것 안걸들여도됨 ///////////////////
    ////////////////// 여기아래로는 구글맵 기본기능 설정에 관한것 안걸들여도됨 ///////////////////

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    public void onConnected(Bundle connectionHint) {


        Log.d(TAG, "onConnected");

        if (!checkLocationServicesStatus()) {

            showDialogForLocationServiceSetting();

        }


        LocationRequest locationRequest = new LocationRequest();

        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationRequest.setInterval(UPDATE_INTERVAL_MS);

        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_MS);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ActivityCompat.checkSelfPermission(this,

                    Manifest.permission.ACCESS_FINE_LOCATION)

                    == PackageManager.PERMISSION_GRANTED) {




            }

        } else {


            Log.d(TAG, "onConnected : call FusedLocationApi");




            mGoogleMap.getUiSettings().setCompassEnabled(true);

            //mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

            mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(150));

        }

    }


    @Override

    public void onConnectionFailed(ConnectionResult connectionResult) {

        Location location = null;

        location.setLatitude(DEFAULT_LOCATION.latitude);

        location.setLongitude(DEFAULT_LOCATION.longitude);


        //        setCurrentLocation(location, "위치정보 가져올 수 없음",
        //
        //                "위치 퍼미션과 GPS 활성 요부 확인하세요");

    }


    public void onConnectionSuspended(int cause) {

        if (cause == CAUSE_NETWORK_LOST)

            Log.e(TAG, "onConnectionSuspended(): Google Play services " +

                    "connection lost.  Cause: network lost.");

        else if (cause == CAUSE_SERVICE_DISCONNECTED)

            Log.e(TAG, "onConnectionSuspended():  Google Play services " +

                    "connection lost.  Cause: service disconnected");

    }


    //    public String getCurrentAddress(Location location) {
    //
    //
    //        //지오코더... GPS를 주소로 변환
    //
    //        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
    //
    //
    //        List<Address> addresses;
    //
    //
    //        try {
    //
    //
    //            addresses = geocoder.getFromLocation(
    //
    //                    location.getLatitude(),
    //
    //                    location.getLongitude(),
    //
    //                    1);
    //
    //        } catch (IOException ioException) {
    //
    //            //네트워크 문제
    //
    //            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();
    //
    //            return "지오코더 서비스 사용불가";
    //
    //        } catch (IllegalArgumentException illegalArgumentException) {
    //
    //            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
    //
    //            return "잘못된 GPS 좌표";
    //
    //
    //        }
    //
    //
    //        if (addresses == null || addresses.size() == 0) {
    //
    //            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
    //
    //            return "주소 미발견";
    //
    //
    //        } else {
    //
    //            Address address = addresses.get(0);
    //
    //            return address.getAddressLine(0).toString();
    //
    //        }
    //
    //
    //    }


    public boolean checkLocationServicesStatus() {

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);


        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

    }





    //여기부터는 런타임 퍼미션 처리을 위한 메소드들

    @TargetApi(Build.VERSION_CODES.M)

    private void checkPermissions() {
        boolean fineLocationRationale = ActivityCompat
                .shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (hasFineLocationPermission == PackageManager
                .PERMISSION_DENIED && fineLocationRationale)
            showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
        else if (hasFineLocationPermission
                == PackageManager.PERMISSION_DENIED && !fineLocationRationale) {
            showDialogForPermissionSetting("퍼미션 거부 + Don't ask again(다시 묻지 않음) " +
                    "체크 박스를 설정한 경우로 설정에서 퍼미션 허가해야합니다.");
        } else if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED) {
            if (mGoogleApiClient == null) {
                buildGoogleApiClient();
            }
            mGoogleMap.setMyLocationEnabled(true);
        }
    }


    @Override

    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (permsRequestCode

                == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION && grantResults.length > 0) {


            boolean permissionAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;


            if (permissionAccepted) {


                if (mGoogleApiClient == null) {

                    buildGoogleApiClient();

                }


                if (ActivityCompat.checkSelfPermission(this,

                        Manifest.permission.ACCESS_FINE_LOCATION)

                        == PackageManager.PERMISSION_GRANTED) {


                    mGoogleMap.setMyLocationEnabled(true);

                }


            } else {


                checkPermissions();

            }

        }

    }


    @TargetApi(Build.VERSION_CODES.M)

    private void showDialogForPermission(String msg) {


        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle("알림");

        builder.setMessage(msg);

        builder.setCancelable(false);

        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {

                ActivityCompat.requestPermissions(mActivity,

                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},

                        PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

            }

        });


        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {

                finish();

            }

        });

        builder.create().show();

    }


    private void showDialogForPermissionSetting(String msg) {


        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle("알림");

        builder.setMessage(msg);

        builder.setCancelable(true);

        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {


                askPermissionOnceAgain = true;


                Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,

                        Uri.parse("package:" + mActivity.getPackageName()));

                myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);

                myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                mActivity.startActivity(myAppSettings);

            }

        });

        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {

                finish();

            }

        });

        builder.create().show();

    }


    //여기부터는 GPS 활성화를 위한 메소드들

    private void showDialogForLocationServiceSetting() {


        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle("위치 서비스 비활성화");

        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"

                + "위치 설정을 수정하실래요?");

        builder.setCancelable(true);

        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {

            @Override

            public void onClick(DialogInterface dialog, int id) {

                Intent callGPSSettingIntent

                        = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);

                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);

            }

        });

        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {

            @Override

            public void onClick(DialogInterface dialog, int id) {

                dialog.cancel();

            }

        });

        builder.create().show();

    }


    @Override

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PLACE_PICKER_REQUEST) {

            if (resultCode == RESULT_OK) {

                final Place place = PlacePicker.getPlace(data, this);

                ResultLa = place.getLatLng().latitude;
                ResultLo = place.getLatLng().longitude;
                ResultLan = place.getLatLng();
                Toast.makeText(this,"목적지 "+place.getName()+"로 방향안내를 시작합니다.", Toast.LENGTH_LONG).show();
                if(ResultLa != null){
                    MyLocation.LocationResult locationResult = new MyLocation.LocationResult() {
                        public void gotLocation(Location location) {
                            po = new PolylineOptions();
                            sensePoly.add(mGoogleMap.addPolyline(po.add(new LatLng(location.getLatitude(), location.getLongitude()),new LatLng(ResultLa, ResultLo)).width(30).color(Color.RED)));
//                            polyline = mGoogleMap.addPolyline(po.add(new LatLng(location.getLatitude(), location.getLongitude()),new LatLng(ResultLa, ResultLo)).width(30).color(Color.RED));
                        }
                    };
                    MyLocation myLocation = new MyLocation();
                    myLocation.getLocation(getApplicationContext(), locationResult);

                }

            }

        }

        super.onActivityResult(requestCode, resultCode, data);


        switch (requestCode) {


            case GPS_ENABLE_REQUEST_CODE:


                //사용자가 GPS 활성 시켰는지 검사

                if (checkLocationServicesStatus()) {

                    if (checkLocationServicesStatus()) {


                        if (mGoogleApiClient == null) {

                            buildGoogleApiClient();

                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                            if (ActivityCompat.checkSelfPermission(this,

                                    Manifest.permission.ACCESS_FINE_LOCATION)

                                    == PackageManager.PERMISSION_GRANTED) {


                                mGoogleMap.setMyLocationEnabled(true);

                            }

                        } else mGoogleMap.setMyLocationEnabled(true);


                        return;

                    }

                } else {

                    //                    setCurrentLocation(null, "위치정보 가져올 수 없음",
                    //
                    //                            "위치 퍼미션과 GPS 활성 요부 확인하세요");

                }


                break;

        }

    }

}
