package com.example.sites

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*

class ActivityGPS : AppCompatActivity(), OnMapReadyCallback {

    var tvMensaje: TextView?=null
    val MIN_TIME: Long=10000
    var local:Localizacion?=null
    var locationManager:LocationManager?=null

    //Parte de Fragmentmaps
    var lat: Double=0.0
    var lon: Double=0.0




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gps)
        tvMensaje = findViewById(R.id.tvMensaje)

        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),1000)
        }else{
            iniciarLocalizacion()
        }
    }

    fun iniciarLocalizacion(){
        locationManager= getSystemService(Context.LOCATION_SERVICE) as LocationManager
        local = Localizacion(this)


        local?.tvMensaje = tvMensaje

        val gpsEnabled: Boolean= locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if(!gpsEnabled){
            var intent: Intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }

        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),1000)

            return;
        }
        locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, 0F, local as LocationListener)

        tvMensaje?.setText("Localización Agregada")

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode==1000){
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
                iniciarLocalizacion()
                return
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),1000)
        }else{
            iniciarLocalizacion()
        }

        var contextAct:ActivityGPS=this
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        // Create a listener
        val gyroscopeSensorListener = object : SensorEventListener {
            override fun onSensorChanged(sensorEvent: SensorEvent) {
                if (sensorEvent.values[0]<0.5 ) {
                    if (ActivityCompat.checkSelfPermission(
                            contextAct,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(
                            contextAct,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            contextAct,
                            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                            1000
                        )
                    } else {
                        
                        tvMensaje?.setText("Actualizando ")
                        locationManager?.removeUpdates(local as LocationListener)
                        locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, 0F, local as LocationListener)
                        if(locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)!=null){
                            local?.onLocationChanged(locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)!!)
                            tvMensaje?.setText("Actualizando..."+ tvMensaje?.text.toString())
                        }

                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, i: Int) {}
        }
        sensorManager.registerListener(gyroscopeSensorListener,gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL)


    }





    //Parte de mapFragment

    //Se llama desde Localización
    fun onCreateView(lati: Double, longi: Double){
        //Fragment del mapa
        var fragment = SupportMapFragment()

        var bundle= Bundle()
        bundle.putDouble("lat",lat)
        bundle.putDouble("lon",lon)
        fragment.arguments = bundle


        var fragmentManager: FragmentManager =this.supportFragmentManager
        var fragmentTransaction: FragmentTransaction =fragmentManager.beginTransaction()
        fragmentTransaction.add(R.id.fragment,fragment, null)
        fragmentTransaction.commitAllowingStateLoss()


        if(fragment.arguments!=null){
            this.lat=lati
            this.lon=longi
        }

        fragment.getMapAsync(this)
    }

    //Viene de OnMapCallBack
    override fun onMapReady(googleMap: GoogleMap?) {
        var latLng = LatLng(lat, lon)
        var zoom: Float = 17F
        var m=Miradores

        if (googleMap != null) {

            val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            val act=this
            // Create a listener
            val lightSensorListener = object : SensorEventListener {
                override fun onSensorChanged(sensorEvent: SensorEvent) {
                    if (sensorEvent.values[0]<5 ){
                        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(act, R.raw.style_dark_json))
                    }else{
                        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(act, R.raw.style_json))
                    }

                }

                override fun onAccuracyChanged(sensor: Sensor, i: Int) {}
            }
            sensorManager.registerListener(lightSensorListener,lightSensor, SensorManager.SENSOR_DELAY_NORMAL)


            //Modo noche
            //googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_dark_json))

            //Modo normal
            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json))

            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))

            googleMap.uiSettings.isZoomControlsEnabled=false
            googleMap.uiSettings.isCompassEnabled=true


            googleMap.addMarker(MarkerOptions().position(latLng))



            var somewhere: Marker
            for(i in 0..(m.arraySitios.size-1)){
                somewhere = googleMap.addMarker(
                    MarkerOptions().position(
                        LatLng(
                            m.arraySitios[i].lat,
                            m.arraySitios[i].lon
                        )
                    ).title(m.arrayNombres[i]).snippet("Esto es la info").icon(
                        BitmapDescriptorFactory.fromResource(
                            R.drawable.chincheta
                        )
                    )
                )

                googleMap.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {


                    // Defines the contents of the InfoWindow
                    override fun getInfoContents(arg0: Marker): View?
                    {

                        // Getting view from the layout file infowindowlayout.xml
                        var v = getLayoutInflater ().inflate(R.layout.infowindowlayout, null);

                        var latLng: LatLng= arg0.getPosition ();

                        var im : ImageView = v.findViewById (R.id.imageView1);
                        var tv1: TextView = v.findViewById (R.id.textView1);
                        var tv2: TextView =v.findViewById (R.id.textView2);
                        var title = arg0 . getTitle ();
                        var informations = arg0 . getSnippet ();

                        tv1.setText(title);
                        tv2.setText(informations);

                        if (onMarkerClick(arg0, somewhere) == true) {
                            im.setImageResource(R.drawable.agente);
                        }


                        return v;

                    }

                    override fun getInfoWindow(p0: Marker?): View? {
                        return null;
                    }
                });



            }

        }
    }
    fun  onMarkerClick(marker: Marker, somewhere: Marker): Boolean{

        if (marker.equals(somewhere))
        {
            return true;
        }
        return false;
    }

}