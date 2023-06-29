package com.example.weatherapp.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.*
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.example.weatherapp.R
import com.example.weatherapp.adapter.NextDayAdapter
import com.example.weatherapp.model.ModelNextDay
import com.example.weatherapp.networking.ApiEndpoint
import com.cooltechworks.views.shimmer.ShimmerRecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.fragment_next_day.view.*
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class FragmentNextDays : BottomSheetDialogFragment() {

    var lat: Double? = null
    var lng: Double? = null
    var nextDayAdapter: NextDayAdapter? = null
    var rvListWeather: ShimmerRecyclerView? = null
    var fabClose: FloatingActionButton? = null
    var modelNextDays: MutableList<ModelNextDay> = ArrayList()
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private val permissionId = 2

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (view?.parent as View).setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_next_day, container, false)

        nextDayAdapter = NextDayAdapter(activity!!, modelNextDays)
        rvListWeather = rootView.rvListWeather
        rvListWeather?.setLayoutManager(LinearLayoutManager(activity))
        rvListWeather?.setHasFixedSize(true)
        rvListWeather?.setAdapter(nextDayAdapter)
        rvListWeather?.showShimmerAdapter()

        fabClose = rootView.findViewById(R.id.fabClose)
        fabClose?.setOnClickListener({
            dismiss()
        })

        //method get LatLong
        getLatLong()

        return rootView
    }

    @SuppressLint("MissingPermission")
    private fun getLatLong () {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient( activity!!)
        getLocation()

        }


    //Get Current Location Method
    private fun getLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                if (activity?.let {
                        ActivityCompat.checkSelfPermission(
                            it,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    } != PackageManager.PERMISSION_GRANTED && activity?.let {
                        ActivityCompat.checkSelfPermission(
                            it,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    } != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                mFusedLocationClient.lastLocation.addOnCompleteListener(activity!!) { task ->
                    val location: Location? = task.result
                    if (location != null) {
                        val geocoder = Geocoder(activity!!, Locale.getDefault())
                        val list: List<Address> =
                            geocoder.getFromLocation(
                                location.latitude,
                                location.longitude,
                                1
                            ) as List<Address>

                        onLocationChanged(list[0].latitude, list[0].longitude)

                    }
                }
            } else {
                Toast.makeText(activity!!, "Please turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            activity!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    //Check Location Permission Method
    private fun checkPermissions(): Boolean {
        if (activity?.let {
                ActivityCompat.checkSelfPermission(
                    it,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            } == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                activity!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun requestPermissions() {
        activity?.let {
            ActivityCompat.requestPermissions(
                it,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                permissionId
            )
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == permissionId) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLocation()
            }
        }
    }

    fun onLocationChanged(latitude:Double,longitude:Double) {
        lng = longitude
        lat = latitude
//        lng = 21.170240
//        lat = 72.831062

        Handler().postDelayed({
            //method get Data Weather
            getListWeather()
        }, 3000)
    }

    //Current 5 day weather Api call
    private fun getListWeather() {
            AndroidNetworking.get(ApiEndpoint.BASEURL + ApiEndpoint.Daily + "lat=" + lat + "&lon=" + lng + ApiEndpoint.UnitsAppidDaily)
                    .setPriority(Priority.MEDIUM)
                    .build()
                    .getAsJSONObject(object : JSONObjectRequestListener {
                        override fun onResponse(response: JSONObject) {
                            try {
                                val jsonArray = response.getJSONArray("list")
                                for (i in 0 until jsonArray.length()) {
                                    val dataApi = ModelNextDay()
                                    val objectList = jsonArray.getJSONObject(i)
                                    val jsonObjectOne = objectList.getJSONObject("temp")
                                    val jsonArrayOne = objectList.getJSONArray("weather")
                                    val jsonObjectTwo = jsonArrayOne.getJSONObject(0)
                                    val longDate = objectList.optLong("dt")
                                    val formatDate = SimpleDateFormat("d MMM yy")
                                    val readableDate = formatDate.format(Date(longDate * 1000))
                                    val longDay = objectList.optLong("dt")
                                    val format = SimpleDateFormat("EEEE")
                                    val readableDay = format.format(Date(longDay * 1000))

                                    dataApi.nameDate = readableDate
                                    dataApi.nameDay = readableDay
                                    dataApi.descWeather = jsonObjectTwo.getString("description")
                                    dataApi.tempMin = jsonObjectOne.getDouble("min")
                                    dataApi.tempMax = jsonObjectOne.getDouble("max")
                                    modelNextDays.add(dataApi)
                                }
                                nextDayAdapter?.notifyDataSetChanged()
                                rvListWeather?.hideShimmerAdapter()
                            } catch (e: JSONException) {
                                e.printStackTrace()
                                Toast.makeText(activity, "Failed to display data!", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onError(anError: ANError) {
                            Toast.makeText(activity, "No internet network!", Toast.LENGTH_SHORT).show()
                        }
                    })
        }

    companion object {
        fun newInstance(string: String?): FragmentNextDays {
            val fragmentNextDays = FragmentNextDays()
            val args = Bundle()
            args.putString("string", string)
            fragmentNextDays.arguments = args
            return fragmentNextDays
        }
    }
}