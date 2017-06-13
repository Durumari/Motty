package com.ct.motty.motty;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.skp.openplatform.android.sdk.api.APIRequest;
import com.skp.openplatform.android.sdk.common.PlanetXSDKConstants;
import com.skp.openplatform.android.sdk.common.PlanetXSDKException;
import com.skp.openplatform.android.sdk.common.RequestBundle;
import com.skp.openplatform.android.sdk.common.RequestListener;
import com.skp.openplatform.android.sdk.common.ResponseMessage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    Button btnClear;
    Button btnSend;
    Button btnGood;
    Button btnBad;
    Button btnVeryBad;
    Switch switchDevMode;
    Switch switchWaterMode;
    TextView tvResult;
    LinearLayout devLayout;
    ImageView imageView;
    //위치정보 객체
    LocationManager lm = null;
    Location mLocation = null;
    private final int NOTIFICATION_ID = 1004;
    private final String PUSH_TITLE = "title";
    private final String PUSH_CONTENT = "content";

    public static final String ACTION_USB_READY = "com.felhr.connectivityservices.USB_READY";
    public static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    public static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    public static final String ACTION_USB_NOT_SUPPORTED = "com.felhr.usbservice.USB_NOT_SUPPORTED";
    public static final String ACTION_NO_USB = "com.felhr.usbservice.NO_USB";
    public static final String ACTION_USB_PERMISSION_GRANTED = "com.felhr.usbservice.USB_PERMISSION_GRANTED";
    public static final String ACTION_USB_PERMISSION_NOT_GRANTED = "com.felhr.usbservice.USB_PERMISSION_NOT_GRANTED";
    public static final String ACTION_USB_DISCONNECTED = "com.felhr.usbservice.USB_DISCONNECTED";
    public static final String ACTION_CDC_DRIVER_NOT_WORKING = "com.felhr.connectivityservices.ACTION_CDC_DRIVER_NOT_WORKING";
    public static final String ACTION_USB_DEVICE_NOT_WORKING = "com.felhr.connectivityservices.ACTION_USB_DEVICE_NOT_WORKING";
    public static final int MESSAGE_FROM_SERIAL_PORT = 0;
    public static final int CTS_CHANGE = 1;
    public static final int DSR_CHANGE = 2;
    public static final int SYNC_READ = 3;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final int BAUD_RATE = 9600; // BaudRate. Change this value if you need
    public static boolean SERVICE_CONNECTED = false;

    boolean isWatered = false;

    UsbManager usbManager = null;

    private Handler mHandler;
    private UsbManager usbManagerƒ;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;

    private class Data{
        String city = "";
        String grade = "";
        double value = 0;
        String time = "";
        int gradeInt = 0;
    };

    public void write(byte[] data) {
        if (serialPort != null) {
            tvResult.setText("sended : " + data);

            serialPort.write(data);
        }
    }
    @Override
    public void onStop(){
        super.onStop();
        if(registerdRCV) {
            unregisterReceiver(broadcastReceiver);
            registerdRCV = false;
        }
        if (serialPort != null)
            serialPort.close();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    Data finalResult = new Data();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
        initSerialCommunication();
        /**위치정보 객체를 생성한다.*/
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

    }


    void showGuide(){
//        tvResult.setText(tvResult.getText() + "good!!!!!!!");
//        imageView.setImageDrawable(getDrawable(R.drawable.good));
//        notificationBigText(MainActivity.this,"대전","23 좋음", R.drawable.family, MainActivity.class );
        if(switchDevMode.isChecked()){
            if(switchWaterMode.isChecked()){
                if(GradeforDEVMODE == 0){
                    //좋은 이미지
                    tvResult.setText(tvResult.getText() + "good!!!!!!!");
                    imageView.setImageDrawable(getDrawable(R.drawable.good));
                    notificationBigText(MainActivity.this,"대전","23 좋음", R.drawable.family, MainActivity.class );
                }else if(GradeforDEVMODE == 1){
                    //나쁜 이미지
                    tvResult.setText(tvResult.getText() + "bad!!!!!!!");
                    imageView.setImageDrawable(getDrawable(R.drawable.bad));
                    notificationBigText(MainActivity.this,"대전","51 나쁨", R.drawable.family, MainActivity.class );
                }else{
                    //매우 나쁜 이미지
                    tvResult.setText(tvResult.getText() + "vb!!!!!!!");
                    imageView.setImageDrawable(getDrawable(R.drawable.verybad));
                    notificationBigText(MainActivity.this,"대전","102 매우 나쁨", R.drawable.family, MainActivity.class );
                }
            }else{
                //물 달라는 이미지
                tvResult.setText(tvResult.getText() + "water!!!!!!!");

                imageView.setImageDrawable(getDrawable(R.drawable.nowater));
                notificationBigText(MainActivity.this,"대전","먹고 살자하는 건데, 물좀 주세요!!", R.drawable.family, MainActivity.class );
            }
        }else{
            if(isWatered){
                if(finalResult.gradeInt == 0){
                    //좋은 이미지
                    imageView.setImageDrawable(getDrawable(R.drawable.good));
                    notificationBigText(MainActivity.this,"대전",finalResult.value + " " + finalResult.grade, R.drawable.family, MainActivity.class );
                }else if(finalResult.gradeInt == 1){
                    //나쁜 이미지
                    imageView.setImageDrawable(getDrawable(R.drawable.bad));
                    notificationBigText(MainActivity.this,"대전",finalResult.value + " " + finalResult.grade, R.drawable.family, MainActivity.class );
                }else{
                    //매우 나쁜 이미지
                    imageView.setImageDrawable(getDrawable(R.drawable.verybad));
                    notificationBigText(MainActivity.this,"대전",finalResult.value + " " + finalResult.grade, R.drawable.family, MainActivity.class );
                }
            }
            else{
                //물 달라는 이미지
                imageView.setImageDrawable(getDrawable(R.drawable.nowater));
                notificationBigText(MainActivity.this,"대전","먹고 살자하는 건데, 물좀 주세요!!", R.drawable.family, MainActivity.class );
            }
        }
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            mLocation = location;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };


    private boolean serialPortConnected;
    int deviceVID;
    int devicePID;
    public void initSerialCommunication(){
        serialPortConnected = false;
        MainActivity.SERVICE_CONNECTED = true;
        setFilter();

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            tvResult.setText("usb found! "+ usbDevices.size());
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                deviceVID = device.getVendorId();
                devicePID = device.getProductId();

                tvResult.setText("usb found! "+ usbDevices.size()+ deviceVID + " " + devicePID);
                //if(deviceVID == 11111) //vendor ID
                {
                    requestUserPermission();

                    keep = false;
                }
                /*else {
                    connection = null;
                    device = null;
                }*/

                if (!keep)
                    break;

            }
        }

    }

    boolean registerdRCV = false;
    private void setFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DETACHED);
        filter.addAction(ACTION_USB_ATTACHED);
        registerReceiver(broadcastReceiver, filter);
        registerdRCV = true;
    }
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            /*if (arg1.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = arg1.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) // User accepted our USB connection. Try to open the device as a serial port
                {
                    Intent intent = new Intent(ACTION_USB_PERMISSION_GRANTED);
                    arg0.sendBroadcast(intent);
                    connection = usbManager.openDevice(device);
                    new ConnectionThread().start();
                } else // User not accepted our USB connection. Send an Intent to the Main Activity
                {
                    Intent intent = new Intent(ACTION_USB_PERMISSION_NOT_GRANTED);
                    arg0.sendBroadcast(intent);
                }
            } else if (arg1.getAction().equals(ACTION_USB_ATTACHED)) {
                if (!serialPortConnected)
                    initSerialCommunication(); // A USB device has been attached. Try to open it as a Serial port
            } else if (arg1.getAction().equals(ACTION_USB_DETACHED)) {
                // Usb device was disconnected. send an intent to the Main Activity
                Intent intent = new Intent(ACTION_USB_DISCONNECTED);
                arg0.sendBroadcast(intent);
                if (serialPortConnected) {
                    serialPort.syncClose();
                }
                serialPortConnected = false;
            }*/
        }
    };
    private void requestUserPermission() {
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(device, mPendingIntent);
    }
    public void initUI() {
        btnClear = (Button) findViewById(R.id.btnClear);
        btnSend = (Button) findViewById(R.id.btnSend);
        btnGood = (Button) findViewById(R.id.btnGood);
        btnBad = (Button) findViewById(R.id.btnBad);
        btnVeryBad = (Button) findViewById(R.id.btnVeryBad);
        tvResult = (TextView) findViewById(R.id.tvResult);
        devLayout = (LinearLayout) findViewById(R.id.Dev_Layout);
        devLayout.setVisibility(View.INVISIBLE);
        imageView = (ImageView) findViewById(R.id.imgView_result);

        switchDevMode = (Switch) findViewById(R.id.switch_dev);
        switchWaterMode = (Switch) findViewById(R.id.switch_water);
        switchDevMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                    devLayout.setVisibility(View.VISIBLE);
                else
                    devLayout.setVisibility(View.INVISIBLE);

            }
        });


        switchWaterMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            }
        });
        switchWaterMode.setChecked(true);



        btnSend.setOnClickListener(this);
        btnClear.setOnClickListener(this);
        btnGood.setOnClickListener(this);
        btnBad.setOnClickListener(this);
        btnVeryBad.setOnClickListener(this);
    }

    public int GradeforDEVMODE = 0;
    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btnClear:
                tvResult.setText("");
                break;
            case R.id.btnSend:
                if(mLocation != null)
                    commWithOpenApiServer();
                break;

            case R.id.btnGood:
                GradeforDEVMODE = 0;
                write("0".getBytes());
                hndResult = "Value[" + 23 +"] " + "WHO Grade[좋음]";
                break;
            case R.id.btnBad:
                GradeforDEVMODE = 1;
                write("1".getBytes());
                hndResult = "Value[" + 56 +"] " + "WHO Grade[나쁨]";
                break;
            case R.id.btnVeryBad:
                GradeforDEVMODE =2;
                write("2".getBytes());
                hndResult = "Value[" + 102 +"] " + "WHO Grade[매우 나쁨]";
                break;
        }

    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted =
                        intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    tvResult.setText("granted");
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            tvResult.setText(tvResult.getText() + " PORT OPEN");
                            Log.d("SERIAL", "PORT OPEN");
                        } else {

                            tvResult.setText(tvResult.getText() + " PORT NOT OPEN");
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {

                        tvResult.setText(tvResult.getText() + " PORT IS NULL");
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {

                    tvResult.setText(tvResult.getText() + " PORT NOT GRANTED");
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            }
        };
    };

    private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] arg0) {
            try {
                if(arg0 == null){
                    Log.d("SERIAL_RECEIVED_ERROR", "null");
                    return;
                }

                if(arg0.length == 0){
                    Log.d("SERIAL_RECEIVED_ERROR", "arg0 length is 0");
                    return;
                }
                String data = new String(arg0, "UTF-8");
                Log.d("SERIAL_RECEIVED", data);
                if(data.length() == 0){
                    Log.d("SERIAL_RECEIVED_ERROR", "data length is 0");
                    return;
                }

                /*runOnUiThread(new Runnable() {
                    @Override
                    public void run() {


                        tvResult.setText(data + " recieved");
                    }
                });*/

                //Log.d("SERIAL_RECEIVED_BYTES: ", arg0.toString());
                //String error_bytes = new String(arg0, "UTF-8");
                //Log.d("SERIAL_RECEIVED_ERROR:", error_bytes);
                /*if ("3".equals(data)){
                    isWatered = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showGuide();
                        }
                    });
                }else if("4".equals(data)){
                    isWatered = true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showGuide();
                        }
                    });
                }
                else
                {
                    Log.e("SERIAL_RECEIVED_ERROR:","null error");
                }*/
                //Case
                if (arg0[0] == '3'){
                    isWatered = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showGuide();
                        }
                    });
                }else if(arg0[0] == '4'){
                    isWatered = true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showGuide();
                        }
                    });
                }else
                {
                    Log.e("SERIAL_RECEIVED_ERROR:","null error");
                }



            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    };

    public void commWithOpenApiServer() {
        // Init Comm Data.
        api = new APIRequest();
        APIRequest.setAppKey("3bb2d9ec-0dc5-3ef2-bab2-893a20011f8e");

        param = new HashMap<String, Object>();
        param.put("version", "1");
        param.put("lat", "" + mLocation.getLatitude());
        param.put("lon", "" + mLocation.getLongitude());
        /*
        param.put("city", "");
        param.put("country", "");
        param.put("village", "");*/
        //param.put("page", "1");
        //param.put("count", "10");

        requestBundle = new RequestBundle();
        requestBundle.setUrl(URL);
        requestBundle.setParameters(param);
        requestBundle.setHttpMethod(PlanetXSDKConstants.HttpMethod.GET);
        requestBundle.setResponseType(PlanetXSDKConstants.CONTENT_TYPE.JSON);

        try {
            api.request(requestBundle, reqListener);
        } catch (PlanetXSDKException e) {
            e.printStackTrace();
        }

    }

    RequestListener reqListener = new RequestListener() {

        @Override
        public void onPlanetSDKException(PlanetXSDKException e) {
            hndResult = e.toString();
            msgHandler.sendEmptyMessage(0);
        }

        @Override
        public void onComplete(ResponseMessage result) {
            hndResult = result.getStatusCode() + "\n" + result.toString();
            String z = "", a = "";
            JSONObject jObject = null;
            try{
                JSONObject json = new JSONObject(result.toString());

                JSONObject res = json.getJSONObject("result");
                String data = res.getString("message");
                if(data.equals("성공")){

                    JSONObject weather = json.getJSONObject("weather");
                    JSONArray dust = weather.getJSONArray("dust");
                    for(int i = 0 ; i < dust.length() ; i++){
                        JSONObject v = dust.getJSONObject(i);

                        JSONObject pm10 = v.getJSONObject("pm10");
                        double value = pm10.getDouble("value");
                        String grade = pm10.getString("grade");
                        String time = v.getString("timeObservation");

                        String name = v.getJSONObject("station").getString("name");

                        finalResult.city = name;
                        finalResult.value = value;

                        if(value < 51){
                            finalResult.grade  =  "좋음";
                            finalResult.gradeInt = 0;
                        }else if(value < 76){
                            finalResult.grade  =  "나쁨";
                            finalResult.gradeInt = 1;
                        }else{ //if(value < 101){
                            finalResult.grade  =  "매우 나쁨";
                            finalResult.gradeInt = 2;
                        }/*else if(value < 151){
                            finalResult.grade  =  "매우 나쁨";
                        }else{
                            finalResult.grade  =  "최악";
                        }*/
                        //finalResult.grade = grade;
                        finalResult.time = time;

                        hndResult = "Value[" + finalResult.value +"] " + "WHO Grade["+ finalResult.grade+"]";
                    }


                }


            }catch(Exception e){
                Log.e("error",e.toString());
            }finally{

            }


            msgHandler.sendEmptyMessage(0);
        }

    };


    APIRequest api;
    RequestBundle requestBundle;
    String URL = "http://apis.skplanetx.com/weather/dust";
    Map<String, Object> param;

    String hndResult = "";

    Handler msgHandler = new Handler(){
        public void dispatchMessage(Message msg) {
            tvResult.setText(hndResult);
            if(finalResult.gradeInt == 0){
                //String data = "48";

//                byte[] a = new byte[1];
//                a[0] = 0x0000;
//                write(a);
                write("0".getBytes());
                Log.i("write:", "0");
            }else if(finalResult.gradeInt == 1){
//                byte[] a = new byte[1];
//                a[0] = 0x0001;
//                write(a);
                write("1".getBytes());
                Log.i("write:", "1");
            }else{
//                byte[] a = new byte[1];
//                a[0] = 0x0002;
//                write(a);
                write("2".getBytes());
                Log.i("write:", "2");
            }

        };
    };
    /*void notificationColorFont(Context context, String title, String message, int icon, Class<?> activityClass) {

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.view_push);
        remoteViews.setTextViewText(R.id.item_push_title, title);
        remoteViews.setTextViewText(R.id.item_push_message, message);

        Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.family);

        Intent intent = new Intent(context, activityClass);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setAutoCancel(true);
        builder.setSmallIcon(icon);
        builder.setLargeIcon(largeIcon);
        builder.setTicker(title);
        builder.setContentTitle(title);
        builder.setContentText(message);
        builder.setContentIntent(pendingIntent);
        builder.setContent(remoteViews);
        builder.setDefaults(Notification.DEFAULT_VIBRATE);
        builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }*/

    void notificationBigText(Context context, String title, String message, int icon, Class<?> activityClass) {

        Intent intent = new Intent(context, activityClass);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.family);

        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setSummaryText("Google");
        bigTextStyle.setBigContentTitle(title);
        bigTextStyle.bigText(message);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(icon);
        builder.setLargeIcon(largeIcon);
        builder.setTicker(title);
        builder.setContentTitle(title);
        builder.setContentText(message);
        builder.setAutoCancel(true);
        builder.setStyle(bigTextStyle);
        builder.setContentIntent(pendingIntent);
        builder.setDefaults(Notification.DEFAULT_VIBRATE);
        builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    void notificationBigPicture(Context context, String title, String message, int icon, Bitmap banner, Class<?> activityClass) {

        Intent intent = new Intent(context, activityClass);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle();
        bigPictureStyle.setBigContentTitle(title);
        bigPictureStyle.setSummaryText(message);
        bigPictureStyle.bigPicture(banner);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(icon);
        builder.setTicker(title);
        builder.setContentTitle(title);
        builder.setContentText(message);
        builder.setAutoCancel(true);
        builder.setStyle(bigPictureStyle);
        builder.setContentIntent(pendingIntent);
        builder.setDefaults(Notification.DEFAULT_VIBRATE);
        builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
