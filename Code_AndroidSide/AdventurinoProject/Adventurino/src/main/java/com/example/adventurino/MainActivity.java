package com.example.adventurino;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

import static android.widget.Toast.makeText;

public class MainActivity extends Activity {

    private static final int UDP_PORT = 34567;
    private static final int TCP_PORT = 34568;
    private String CAM_IP;
    private int xState = 0;
    private int zState = 6;
    private boolean sendOn = false;
    private DatagramSocket udpSocket;
    private Socket tcpSocket;
    private ImageView image;
    private Bitmap bitmap;
    private SensorManager sensorManager;

    SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) { /* To Be Removed*/
                String steering = checkRotation(event.values[0]);
                if (steering != "") {
                    new Send().execute(steering);
                    Log.e("covix", "steering: " + steering);
                }
                String motor = checkInclination(event.values[2]);
                if (motor != "") {
                    new Send().execute(motor);
                    Log.e("covix", "motor: " + motor);
                }
            }
        }

        private String checkRotation(float xEvent) {
            String str = "";
            final byte X_THRESHOLD = 5;
            if (xEvent > X_THRESHOLD) {
                if (xState != X_THRESHOLD) {
                    str = "sx";
                    xState = X_THRESHOLD;
                }
            } else if (xEvent < -X_THRESHOLD) {
                if (xState != -X_THRESHOLD) {
                    str = "dx";
                    xState = -X_THRESHOLD;
                }
            } else {
                if (xState != 0) {
                    str = "ctr";
                    xState = 0;
                }
            }
            return str + ".";
        }

        private String checkInclination(float zEvent) {
            String str = "";
            final byte Z_THRESHOLD = 4, Z_AXIS = 5;
            if (zEvent > Z_AXIS + Z_THRESHOLD) {
                if (zState != Z_THRESHOLD) {
                    str = "fwd";
                    zState = Z_THRESHOLD;
                }
            } else if (zEvent < Z_AXIS - Z_THRESHOLD) {
                if (zState != -Z_THRESHOLD) {
                    str = "bck";
                    zState = -Z_THRESHOLD;
                }
            } else {
                if (zState != 0) {
                    str = "stp";
                    zState = 0;
                }
            }
            return str + ".";
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        image = (ImageView) findViewById(R.id.imageView);
//        View.OnTouchListener touch = new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent event) {
//                if (event.getAction() == MotionEvent.ACTION_DOWN) {
//                    Button sender = (Button) view;
//                    new Send().execute(String.valueOf(sender.getText()) + "1");
//                } else if (event.getAction() == MotionEvent.ACTION_UP) {
//                    Button sender = (Button) view;
//                    new Send().execute(String.valueOf(sender.getText()) + "0");
//                }
//                return true;
//            }
//        };
//
//        Button buttonFwd = (Button) findViewById(R.id.buttonFwd);
//        buttonFwd.setOnTouchListener(touch);
//
//        Button buttonSx = (Button) findViewById(R.id.buttonSx);
//        buttonSx.setOnTouchListener(touch);
//
//        Button buttonBck = (Button) findViewById(R.id.buttonBck);
//        buttonBck.setOnTouchListener(touch);
//
//        Button buttonDx = (Button) findViewById(R.id.buttonDx);
//        buttonDx.setOnTouchListener(touch);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.save_picture:
                savePicture();
            case R.id.show_hide_ip:
                int visible = findViewById(R.id.buttonConnect).getVisibility() == View.VISIBLE ?
                        View.GONE :
                        View.VISIBLE;
                findViewById(R.id.buttonConnect).setVisibility(visible);
                findViewById(R.id.editTextIp).setVisibility(visible);
            case R.id.accelerometer_opt:
                if (sendOn)
                    unregisterAccelerometer();
                else
                    registerAccelerometer();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void unregisterAccelerometer() {
        sensorManager.unregisterListener(sensorEventListener);
        sendOn = false;
    }

    private void savePicture() {
        File f = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) +
                        "/Adventurino/"
        );
        int n = f.list().length + 1;

        String mex = "";
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f.getPath() + String.valueOf(n));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mex = "Salvata!";
        }
        makeText(this, mex, Toast.LENGTH_SHORT).show();
    }

    public void btnPicture_click(View view) {
        if (sendOn)
            unregisterAccelerometer();
        new Receive().execute();
    }

    private void registerAccelerometer() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(
                sensorEventListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL
        );
        sendOn = true;
    }

    public void Connect(View view) {
        CAM_IP = String.valueOf(((EditText) findViewById(R.id.editTextIp)).getText());
        Log.e("covix", "connect");
        new Connect().execute();
    }

    private class Connect extends AsyncTask<String, Object, String> {
        protected String doInBackground(String... name) {
            String str = "Connesso";
            try {
                InetAddress address = InetAddress.getByName(CAM_IP);
                tcpSocket = new Socket(address, TCP_PORT);
//                udpSocket = new DatagramSocket(3456);
//                udpSocket.connect(address, UDP_PORT);
//
//                // Send my ip address via UDP
//                byte[] android = "android".getBytes();
//                DatagramPacket p = new DatagramPacket(android, android.length);
//                udpSocket.send(p);
            } catch (Exception e) {
                e.printStackTrace();
                str = e.getMessage();
            }
            Log.e("covix", str);
            return str;
        }

        protected void onPostExecute(String result) {
            makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
            registerAccelerometer();
        }
    }

    private class Send extends AsyncTask<String, Object, Object> {

        protected Object doInBackground(String... mex) {
            Log.e("covix", "strenfen:" + mex[0]);
            try {
                tcpSocket.getOutputStream().write(mex[0].getBytes());
                tcpSocket.getOutputStream().flush();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("covix", e.getMessage());
            }
            Log.e("covix", mex[0]);
            return null;
        }

        protected void onPostExecute(Object result) {
        }
    }

    private class Receive extends AsyncTask<Object, Bitmap, Object> {
        protected Object doInBackground(Object... params) {
            try {
                int length = Start();
                //byte[] img = getPictureDataUdp(length);
                byte[] img = getPictureDataTcp(length);
                Bitmap bm = BitmapFactory.decodeByteArray(img, 0, img.length);
                Log.e("covix", "decoded");
                publishProgress(bm);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("covix", e.getMessage());
            }
            return null;
        }

        private int Start() throws IOException {
            tcpSocket.getOutputStream().write("Shoot".getBytes());
            Log.e("covix", "shoot");

            BufferedReader stream = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
            int length = Integer.parseInt(stream.readLine());
            Log.e("covix", "data " + length);

            // Start to send the image Adventurino!
            tcpSocket.getOutputStream().write("stxadv".getBytes());
            return length;
        }

        private byte[] getPictureDataTcp(int length) throws IOException {
            byte[] img = new byte[length];
            DataInputStream imgStream = new DataInputStream(tcpSocket.getInputStream());
            Log.e("covix", "start receive");
            imgStream.readFully(img);
            Log.e("covix", "end received");

            return img;
        }

        private byte[] getPictureDataUdp(int length) throws IOException {
            DatagramPacket p = new DatagramPacket(new byte[32], 32);
            byte[] img = new byte[length];
            udpSocket.setReceiveBufferSize(length * 10);

            Log.e("covix", "start receive");
            int i;
            for (i = 0; i < length - 32; i += 32) {
                Log.e("covix", String.valueOf(i));
                udpSocket.receive(p);
                for (int k = 0; k < p.getData().length; k++)
                    img[i + k] = p.getData()[k];
            }
            Log.e("covix", "end 32, " + i);
            p = new DatagramPacket(new byte[length - i], length - i);
            udpSocket.receive(p);
            Log.e("covix", p.getLength() + "");
            for (int k = 0; k < p.getData().length; k++)
                img[i + k] = p.getData()[k];
            Log.e("covix", "end received");
            return img;
        }

        protected void onProgressUpdate(Bitmap... img) {
            image.setImageBitmap(img[0]);
            bitmap = img[0];
        }

        protected void onPostExecute(Object result) {
            if (!sendOn)
                registerAccelerometer();
        }
    }
}
