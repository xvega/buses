package com.enrutatec.application;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.enrutatec.model.Stop;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.overlay.PathOverlay;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.views.MapController;
import com.mapbox.mapboxsdk.views.MapView;
import com.enrutatec.controller.RouteController;
import com.enrutatec.model.Route;
import com.enrutatec.services.RoutesManagerService;
import com.enrutatec.services.impl.RoutesManagerServiceImpl;
import com.enrutatec.application.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class MainActivity extends AppCompatActivity implements LocationListener {
    private MapView mv;
    private MapController mapController;

    private double longitude = 10.019446;
    private double latitude = -84.1970462;
    private LocationManager locationManager;

    //UI
    private Toolbar toolbar;
    private MenuItem mSearchAction;
    private boolean isSearchOpened = false;
    private EditText edtSearch;
    //UI

    //Drawing
    PathOverlay line = new PathOverlay(Color.parseColor("#77DD77"), 7);
    private boolean startDrawing = false;

    //Bus stop button
    private Button stopButton;

    //Initial time
    private long startTime;

    RoutesManagerService routeManager = new RoutesManagerServiceImpl();
    Route route = new Route();

    RouteMarker routeMarker = new RouteMarker(this);

    RouteController routeController = new RouteController();

    //URL connection
    private static String URL = "http://186.32.26.170:8080/roadmap/batch";

    private List<Stop> stops = new ArrayList<Stop>();
    private List<Route> routes = new ArrayList<Route>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crashlytics.start(this);
        setContentView(R.layout.activity_main);

        stopButton = (Button) findViewById(R.id.parada);
        stopButton.setVisibility(View.GONE);

        //UI
        toolbar = (Toolbar) findViewById(R.id.tool_bar); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);
        //UI

        mv = (MapView) findViewById(R.id.mapview);
        mv.setMinZoomLevel(mv.getTileProvider().getMinimumZoomLevel());
        mv.setMaxZoomLevel(mv.getTileProvider().getMaximumZoomLevel());


        mapController = mv.getController();
        mapController.setZoom(15);

        LatLng currentPoint = new LatLng(longitude, latitude);

        mapController.setCenter(currentPoint);

        // Show user location (purposely not in follow mode)
        mv.setUserLocationEnabled(true);
        mv.getUserLocationOverlay().setDirectionArrowBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.user));
        mv.getUserLocationOverlay().setPersonBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.user));
        mv.getUserLocationOverlay().setTrackingMode(UserLocationOverlay.TrackingMode.FOLLOW_BEARING);
        mv.getUserLocationOverlay().setTrackingMode(UserLocationOverlay.TrackingMode.NONE);

        mv.loadFromGeoJSONURL("https://gist.githubusercontent.com/tmcw/10307131/raw/21c0a20312a2833afeee3b46028c3ed0e9756d4c/map.geojson");

        mapController.setCenter(currentPoint);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        /*if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    public void requestPermissions(@NonNull String[] permissions, int requestCode)
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return;
        }*/
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 25, this);
        }
        catch (SecurityException e){

        }

        mv.setMapViewListener(routeMarker);
    }

    //Creates a marker specifying a position
    public void newMarker(LatLng position, Route route){
        //routeMarker.setData(route.getName(), route.getPrice(), route.getDuracion(), route.getDistancia());
        Marker x = new Marker("","", position);
        mv.addMarker(x);
    }

    //Extra options
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.menu_activity_main, menu);
		return super.onCreateOptionsMenu(menu);
	}

    protected void handleMenuSearch(){
        ActionBar action = getSupportActionBar(); //get the actionbar

        if(isSearchOpened){ //test if the search is open

            action.setDisplayShowCustomEnabled(false); //disable a custom view inside the actionbar
            action.setDisplayShowTitleEnabled(true); //show the title in the action bar

            //hides the keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(edtSearch.getWindowToken(), 0);

            //add the search icon in the action bar
            mSearchAction.setIcon(ContextCompat.getDrawable(this,R.drawable.search_action));

            isSearchOpened = false;
        } else { //open the search entry

            action.setDisplayShowCustomEnabled(true); //enable it to display a
            // custom view in the action bar.
            action.setCustomView(R.layout.search_bar);//add the custom view
            action.setDisplayShowTitleEnabled(false); //hide the title

            edtSearch = (EditText)action.getCustomView().findViewById(R.id.edtSearch); //the text editor

            //this is a listener to do a search when the user clicks on search button
            edtSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        //doSearch();
                        return true;
                    }
                    return false;
                }
            });

            edtSearch.requestFocus();

            //open the keyboard focused in the edtSearch
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(edtSearch, InputMethodManager.SHOW_IMPLICIT);

            //add the close icon
            mSearchAction.setIcon(ContextCompat.getDrawable(this,R.drawable.close_search));

            isSearchOpened = true;
        }
    }

    @Override
    public void onBackPressed() {
        if(isSearchOpened) {
            handleMenuSearch();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mSearchAction = menu.findItem(R.id.action_search);
        return super.onPrepareOptionsMenu(menu);
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                return true;
            case R.id.action_search:
                handleMenuSearch();
                return true;
        }

        return super.onOptionsItemSelected(item);
	}

    //When the location changes it resets the latitude and longitude
    @Override
    public void onLocationChanged(Location location) {
        latitude =  (location.getLatitude());
        longitude =  (location.getLongitude());
        LatLng newPosition = new LatLng(latitude, longitude);

        mapController.setCenter(newPosition);


        //Start drawing the route
        if(startDrawing){
            routeManager.addCoordinate(route, newPosition);
            line.addPoint(newPosition);
            mv.getOverlays().add(line);
        }
    }

    //Start an action depending on the action in the button
    public void setStartDrawing(View view) throws IOException {
        this.startDrawing=!startDrawing;
        TextView tx=(TextView) findViewById(R.id.drawingButton);

        if(startDrawing){
            stopButton.setVisibility(View.VISIBLE);
            startTime = System.nanoTime();
            routeManager.setRouteInfo(route, this);

            stops.add(routeManager.addStop(new LatLng(latitude, longitude)));

            routeManager.addCoordinate(route, new LatLng(latitude, longitude));
            line.addPoint(new LatLng(latitude, longitude));
            mv.getOverlays().add(line);
            tx.setText("Detener");
        }
        else{
            stopButton.setVisibility(View.GONE);

            stops.add(routeManager.addStop(new LatLng(latitude, longitude)));

            stops.get(0).setPrice(stops.get(1).getPrice());
            stops.get(0).setRoutes(stops.get(1).getRoutes());

            routes.add(new Route(stops.get(stops.size() - 2).getName(), stops.get(stops.size() - 1).getName(), routeManager.calcDistance(route.getCoordinates()), NANOSECONDS.toSeconds(System.nanoTime() - startTime), route.getName(), route.getPrice(), route.getCoordinates()));
            route.setCoordinates(new ArrayList<LatLng>());

            routeController.doPost(URL, toJSON().toString());

            //newMarker(route.getCoordinates().get(0), route);
            tx.setText("Iniciar");
            line = new PathOverlay(Color.parseColor("#77DD77"), 7);
            Log.v("PRUEBA", routes.toString());
            Log.v("PRUEBA",toJSON().toString());

            stops = new ArrayList<Stop>();
            routes = new ArrayList<Route>();
            route = new Route();

        }
    }

    //Create a new stop
    public void setNewBusStop(View view){

        stops.add(routeManager.addStop(new LatLng(latitude, longitude)));

        routes.add(new Route(stops.get(stops.size() - 2).getName(), stops.get(stops.size() - 1).getName(), routeManager.calcDistance(route.getCoordinates()), NANOSECONDS.toSeconds(System.nanoTime() - startTime), route.getName(),route.getPrice(), route.getCoordinates()));
        route.setCoordinates(new ArrayList<LatLng>());
    }

    public void getUserLoc(View view){ mapController.setCenter(new LatLng(latitude, longitude)); }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {}

    @Override
    public void onProviderEnabled(String s) {}

    @Override
    public void onProviderDisabled(String s) {}

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    //Create the JSON
    public JSONObject toJSON(){
        JSONArray JSONstops = new JSONArray();
        for(Stop stop : stops){
            JSONObject JSONstop = new JSONObject();

            try {
                JSONstop.put("name",stop.getName());
                JSONstop.put("latitude",stop.getCoordinate().getLatitude());
                JSONstop.put("longitude",stop.getCoordinate().getLongitude());
                JSONstop.put("price",stop.getPrice());
                JSONArray routeNames = new JSONArray();
                for(String routeName : stop.getRoutes()){
                    routeNames.put(routeName);
                }
                JSONstop.put("routes",routeNames);

                JSONstops.put(JSONstop);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        JSONArray JSONroutes = new JSONArray();
        for(Route newRoute : routes){
            Log.v("PRUEBA",newRoute.getFrom());
            JSONObject JSONroute = new JSONObject();

            try {
                JSONroute.put("from",newRoute.getFrom());
                JSONroute.put("to",newRoute.getTo());
                JSONroute.put("distance",newRoute.getDistance());
                JSONroute.put("duration",newRoute.getDuration());

                JSONArray JSONcoordinates = new JSONArray();

                for(LatLng coordinate : newRoute.getCoordinates()){
                    JSONArray point = new JSONArray();

                    point.put(coordinate.getLatitude());
                    point.put(coordinate.getLongitude());

                    JSONcoordinates.put(point);
                }

                JSONroute.put("coordinate",JSONcoordinates);
                JSONroutes.put(JSONroute);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        JSONObject finalJSON = new JSONObject();

        try {
            finalJSON.put("stops",JSONstops);
            finalJSON.put("routes",JSONroutes);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return finalJSON;
    }
}