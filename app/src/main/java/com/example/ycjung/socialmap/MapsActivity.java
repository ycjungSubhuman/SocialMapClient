package com.example.ycjung.socialmap;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.JsonWriter;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    public static boolean mMapIsTouched = false;
    private static boolean isInDrawMode = false;
    DrawSupportMapFragment drawSupportMapFragment;
    Projection projection;
    Toast toast_pin;
    Toast toast_accept;
    PolylineOptions polylineOptions = new PolylineOptions();
    List<List<LatLng>> list_point_list = new ArrayList<List<LatLng>>();
    Polyline polyline;
    boolean pendingline = false;
    public double latitude;
    public double longitude;
    String mUsername;

    private class JsonToServerJob extends AsyncTask<String,Void,String> {
        ProgressDialog progressDialog;
        AlertDialog alertDialog;
        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            progressDialog.dismiss();
            if (s!=null) {
                CharSequence text = "";
                Context context = getApplicationContext();
                int duration = Toast.LENGTH_SHORT;

                try{
                    JSONObject response = new JSONObject(s);
                    if (response.getString("status").equals("OK")) {
                        text = "data sent to server";
                    }
                    else {
                        text = "server error";
                    }

                } catch (JSONException e) {
                    text = "server message not correct";
                }

                toast_accept = Toast.makeText(context, text, duration);
                toast_accept.show();
            }
            else {
                AlertDialog.Builder builder_alertDialog;
                builder_alertDialog = new AlertDialog.Builder(MapsActivity.this);
                builder_alertDialog.setTitle("Communication Error")
                        .setMessage("Connection couldn't be established.")
                        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog = builder_alertDialog.create();
                alertDialog.show();
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(MapsActivity.this, "Saving...", "Saving...");
        }

        @Override
        protected String doInBackground(String... jsons) {
            //send json request to server. get response from server.
            String response = "";
            try {
                Socket socket = new Socket(getString(R.string.address_server), 8000);
                OutputStream outputStream = socket.getOutputStream();
                InputStream inputStream = socket.getInputStream();

                int size = jsons[0].getBytes(StandardCharsets.US_ASCII).length;
                int sent = 0;
                final int block = 512;
                while(sent < size) {
                    if (sent+block<size) {
                        outputStream.write(jsons[0].getBytes(StandardCharsets.US_ASCII), sent, block);
                    }
                    else {
                        byte[] tmp = new byte[size+5];
                        byte[] fin = "\r\n\r\n\0".getBytes(StandardCharsets.US_ASCII);
                        System.arraycopy(jsons[0].getBytes(StandardCharsets.US_ASCII), 0, tmp, 0, size);
                        System.arraycopy(fin, 0, tmp, size, fin.length);

                        outputStream.write(tmp, sent, size-sent+fin.length);
                    }
                    sent += block;
                }
                outputStream.flush();

                byte[] buff = new byte[513];
                while(inputStream.read(buff, 0, 512) != -1) {
                    buff[buff.length-1] = 0;
                    int i;
                    for (i=0; i<buff.length && buff[i] != 0; i++) {}
                    response += new String(buff, 0, i);
                }

                Log.i("RESPONSE", response);

                outputStream.close();

                socket.close();
            } catch (IOException e) {
                return null;
            }
            return response;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id==R.id.action_draw) {
            //change button type (on to off)
            mMap.getUiSettings().setScrollGesturesEnabled(false);
            isInDrawMode = true;
            pendingline = true;
            polylineOptions = new PolylineOptions();
            return true;
        }
        if(id==R.id.action_pin) {
            //choose where to pin
            Context context = this.getApplicationContext();
            CharSequence text = "Pick a point to pin";
            int duration = Toast.LENGTH_LONG;
            toast_pin = Toast.makeText(context, text, duration);
            toast_pin.setGravity(Gravity.TOP| Gravity.LEFT, 0, 20);
            toast_pin.show();
        }
        if(id==R.id.action_navi) {
            mMap.getUiSettings().setScrollGesturesEnabled(true);
            isInDrawMode = false;

            String for_debug = "";
            if(!list_point_list.isEmpty()) {
                JSONObject json = new JSONObject();
                JSONArray array_vertice = new JSONArray();
                JSONArray array_array_vertice = new JSONArray();
                //make JSON
                for(List<LatLng> li : list_point_list) {
                    for(int i=0; i<li.size(); i++) {
                        try {
                            JSONObject pair = new JSONObject()
                                    .put("lat", li.get(i).latitude)
                                    .put("lng", li.get(i).longitude);
                            array_vertice.put(i, pair);
                        } catch (JSONException e) {
                            Log.e("JSONERROR", "JSON PUT EXCEPTION");
                            break;
                        }
                    }
                    array_array_vertice = array_array_vertice.put(array_vertice);
                }

                try {
                    json = json.put("operation", "SAVEPAINTING")
                            .put("sender", mUsername)
                            .put("body", array_array_vertice);
                } catch (JSONException e) {
                    Log.e("JSONERROR", "JSON PUT EXCEPTION");
                }
                //init array_list_point_liset
                list_point_list = new ArrayList<List<LatLng>>();

                //debug message
                for (List<LatLng> li : list_point_list) {
                    for_debug += "NEWLINE : [";
                    for (LatLng lat : li) {
                        for_debug += lat.toString();
                        for_debug += ",";
                    }
                    for_debug += "]\n";
                }
                Log.i("ON_DRAWFIN", for_debug);
                Log.i("JSONRESULT", json.toString());

                //send to server
                new JsonToServerJob().execute(json.toString());
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        mUsername = intent.getStringExtra("username");
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        DrawSupportMapFragment drawSupportMapFragment = ((DrawSupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
        mMap = drawSupportMapFragment.getMap();

        drawSupportMapFragment.setOnDragListener(new MapWrapperLayout.OnDragListener() {@Override
        public void onDrag(MotionEvent motionEvent) {
            Log.i("ON_DRAG", "X:" + String.valueOf(motionEvent.getX()));
            Log.i("ON_DRAG", "Y:" + String.valueOf(motionEvent.getY()));

            mMapIsTouched = true;

            float x = motionEvent.getX();
            float y = motionEvent.getY();

            int x_co = Integer.parseInt(String.valueOf(Math.round(x)));
            int y_co = Integer.parseInt(String.valueOf(Math.round(y)));

            projection = mMap.getProjection();
            Point x_y_points = new Point(x_co, y_co);
            LatLng latLng = mMap.getProjection().fromScreenLocation(x_y_points);
            latitude = latLng.latitude;
            longitude = latLng.longitude;

            Log.i("ON_DRAG", "lat:" + latitude);
            Log.i("ON_DRAG", "long:" + longitude);

            // Handle motion event:
            if(isInDrawMode) {
                if(pendingline){
                    polyline = mMap.addPolyline(polylineOptions.add(latLng).width(5).color(Color.BLACK));
                    pendingline = false;
                }
                else {
                    polyline = mMap.addPolyline(polylineOptions.add(latLng).width(5).color(Color.BLACK));
                }
            }
        }
        });
        drawSupportMapFragment.setOnTouchUpListener(new MapWrapperLayout.OnTouchUpListener() {
            @Override
            public void onTouchUp(MotionEvent motionEvent) {
                mMapIsTouched = false;
                pendingline = true;
                if(isInDrawMode) {
                    list_point_list.add(polylineOptions.getPoints());
                    polylineOptions = new PolylineOptions();
                    Log.i("ON_TOUCHUP", "TOUCHFINISHED");
                }
            }
        });
    }
}
