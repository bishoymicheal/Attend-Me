package net.corpy.loginlocation.fragments.addLocation

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.SearchView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.add_location_fragment.*
import net.corpy.loginlocation.BaseActivity
import net.corpy.loginlocation.R
import net.corpy.loginlocation.Utils
import net.corpy.loginlocation.locationApi.LocationAPI
import net.corpy.loginlocation.locationApi.LocationAsync
import net.corpy.loginlocation.locationApi.LocationData
import net.corpy.permissionslib.PermissionsFile

class AddLocationActivity : BaseActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    private var radius = 500.0

    private val db = FirebaseFirestore.getInstance()
    private val locationsRef = db.collection("Locations")


    private val locationApi by lazy {
        LocationAPI(this)
    }

    private var mapView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_location_fragment)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapView = mapFragment.view
        mapFragment.getMapAsync(this)

        if (!PermissionsFile.locationPermissionCheck(this)) {
            PermissionsFile.showLocationDialogPermission(this) {
                if (it) {
//                    mMap.uiSettings?.isMyLocationButtonEnabled = true
//                    mMap.isMyLocationEnabled = true
                }
            }
        } else {
//            mMap.uiSettings?.isMyLocationButtonEnabled = true
//            mMap.isMyLocationEnabled = true
        }

        settings_btn.setOnClickListener {
            showRadiusDialog()
        }

        searchImpli()

        manual_btn.setOnClickListener {
            showManualDialog()
        }

        cancel_btn.setOnClickListener {
            onBackPressed()
        }
        save_btn.setOnClickListener {
            saveNewLocation()
        }

        locationApi.connect()

    }

    private fun saveNewLocation() {

        if (location_title_et == null || location_title_et?.text?.isEmpty() == true) {
            textInputLayout?.isErrorEnabled = true
            textInputLayout?.error = getString(R.string.enter_location_title)
            return
        }
        textInputLayout?.isErrorEnabled = false

        val location = net.corpy.loginlocation.model.Location(
            location.latitude,
            location.longitude,
            location_title_et?.text?.toString()!!,
            radius
        )

        progress_view?.visibility = View.VISIBLE
        locationsRef.document().set(location).addOnSuccessListener {
            progress_view?.visibility = View.GONE
            Utils.showSuccessDialog(
                this,
                getString(R.string.new_location),
                getString(R.string.location_added_success),
                false
            ) {
                onBackPressed()
            }
        }.addOnFailureListener {
            progress_view?.visibility = View.GONE
            Utils.showErrorDialog(
                this,
                getString(R.string.new_location),
                getString(R.string.error_adding_location),
                getString(R.string.error_adding_location)
            ) {

            }
        }
    }

    private fun searchImpli() {

        location_search?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null && !query.isEmpty()) {
                    locationAsync?.cancel(true)
                    locationAsync = Utils.getAddressData(this@AddLocationActivity, query) {
                        location = LatLng(it?.latitude ?: 0.0, it?.longitude ?: 0.0)
                        zoomToLocation()
                        Utils.zoomInCircle(mMap, location, radius)
                        current_location?.text = it?.fullAddress ?: getString(R.string.error_loading_location)
                    }
                    return true
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null && !newText.isEmpty()) {
                    locationAsync?.cancel(true)
                    locationAsync = Utils.getAddressData(this@AddLocationActivity, newText) {
                        location = LatLng(it?.latitude ?: 0.0, it?.longitude ?: 0.0)
                        zoomToLocation()
                        Utils.zoomInCircle(mMap, location, radius)
                        current_location?.text = it?.fullAddress ?: getString(R.string.error_loading_location)
                    }
                    return true
                }
                return false
            }

        })
//
//        location_search?.setOnSearchClickListener {
//            locationAsync = Utils.getAddressData(this,location_search?.text)
//        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("InflateParams")
    private fun showRadiusDialog() {
        val dialog = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.radius_layout, null)
        dialog.setView(view)
        val d = dialog.create()
        d.show()

        val radiusEt = view.findViewById<TextInputEditText>(R.id.circle_radius_et)

        val okBtn = view.findViewById<Button>(R.id.confirm_button)
        okBtn.setOnClickListener {
            if (radiusEt != null && radiusEt.text?.isEmpty() == false)
                radius = try {
                    radiusEt.text?.toString()?.toDouble()!!
                } catch (e: Exception) {
                    500.0
                }

            circle?.remove()
            circle = mMap.addCircle(
                CircleOptions().center(location).radius(radius).strokeWidth(2f).strokeColor(
                    ContextCompat.getColor(
                        this,
                        R.color.primaryColor
                    )
                )
            )

            Utils.zoomInCircle(mMap, location, radius)

            d.dismiss()
        }
        val cancelBtn = view.findViewById<Button>(R.id.cancel_button)
        cancelBtn.setOnClickListener {
            d.dismiss()
        }
    }

    @SuppressLint("InflateParams")
    private fun showManualDialog() {
        val dialog = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.manual_layout, null)
        dialog.setView(view)
        val d = dialog.create()
        d.show()

        val latEt = view.findViewById<TextInputEditText>(R.id.lat_et)
        val latEtLayout = view.findViewById<TextInputLayout>(R.id.lat_et_layout)

        val longEt = view.findViewById<TextInputEditText>(R.id.long_et)
        val longEtLayout = view.findViewById<TextInputLayout>(R.id.long_et_layout)

        val okBtn = view.findViewById<Button>(R.id.confirm_button)
        okBtn.setOnClickListener {


            if (latEt == null || latEt.text?.isEmpty() == true) {
                latEtLayout?.isErrorEnabled = true
                latEtLayout?.error = getString(R.string.enter_latitude)
                return@setOnClickListener
            }
            latEtLayout?.isErrorEnabled = false

            if (longEt == null || longEt.text?.isEmpty() == true) {
                longEtLayout?.isErrorEnabled = true
                longEtLayout?.error = getString(R.string.enter_longitude)
                return@setOnClickListener
            }
            longEtLayout?.isErrorEnabled = false

            val lat = latEt.text?.toString()?.toDouble() ?: 0.0
            val long = longEt.text?.toString()?.toDouble() ?: 0.0

            location = LatLng(lat, long)

            val loca = Location("")
            loca.latitude = location.latitude
            loca.longitude = location.longitude

            locationAsync?.cancel(true)
            locationAsync = Utils.getLocationData(this@AddLocationActivity, loca) {
                location = LatLng(it?.latitude ?: 0.0, it?.longitude ?: 0.0)
                zoomToLocation()
                Utils.zoomInCircle(mMap, location, radius)
                current_location?.text = it?.fullAddress ?: getString(R.string.error_loading_location)
            }

            d.dismiss()
        }
        val cancelBtn = view.findViewById<Button>(R.id.cancel_button)
        cancelBtn.setOnClickListener {
            d.dismiss()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {

        googleMap.isMyLocationEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true


        mMap = googleMap
//        mMap.isMyLocationEnabled = true
//        mMap.uiSettings?.isMyLocationButtonEnabled = true

        if (mapView?.findViewById<View>(Integer.parseInt("1")) != null) {
            // Get the button view

            val locationButton =
                ((mapView?.findViewById<View>(Integer.parseInt("1"))?.parent) as View).findViewById<View>(
                    Integer.parseInt("2")
                )
            // and next place it, on bottom right (as Google Maps app)
            val layoutParams = locationButton.layoutParams as (RelativeLayout.LayoutParams)
            // position on right bottom
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
            layoutParams.setMargins(0, 0, 30, 30)
        }


        locationApi.getCurrentLocation(object : LocationAPI.LocationChangeListener {
            override fun onLocationSuccess(location: LocationData?) {
                if (location != null) {
                    googleMap.isMyLocationEnabled = true
                    googleMap.uiSettings.isMyLocationButtonEnabled = true
                    this@AddLocationActivity.location = LatLng(location.latitude, location.longitude)
                }
            }

            override fun onLocationFailed(errorMessage: String?) {

            }


        })

        mMap.setOnMapLoadedCallback {
            mapLoaded(mMap)
        }
    }

    private var marker: Marker? = null
    private var circle: Circle? = null
    var location: LatLng = LatLng(-34.0, 151.0)

    private fun mapLoaded(mMap: GoogleMap) {
        // Add a marker in Sydney and move the camera
        this.mMap = mMap

        showData()

    }

    var locationAsync: LocationAsync? = null
    private fun showData() {

        marker = this.mMap.addMarker(MarkerOptions().position(location))
        this.mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 16f))
        circle = this.mMap.addCircle(
            CircleOptions().center(location).radius(radius).strokeWidth(2f).strokeColor(
                ContextCompat.getColor(
                    this,
                    R.color.primaryColor
                )
            )
        )

        Utils.zoomInCircle(this.mMap, location, radius)

        this.mMap.setOnCameraMoveListener {
            location = this.mMap.projection.visibleRegion.latLngBounds.center

            zoomToLocation()

            locationAsync?.cancel(true)

            val loca = Location("")
            loca.latitude = location.latitude
            loca.longitude = location.longitude
            locationAsync = Utils.getLocationData(this, loca) {
                current_location?.text = it?.fullAddress ?: getString(R.string.error_loading_location)
            }
        }
    }

    fun zoomToLocation() {
        marker?.remove()
        circle?.remove()

        marker = this.mMap.addMarker(MarkerOptions().position(location))
        circle = this.mMap.addCircle(
            CircleOptions().center(location).radius(radius).strokeWidth(2f).strokeColor(
                ContextCompat.getColor(
                    this,
                    R.color.primaryColor
                )
            )
        )
    }

    override fun onPause() {
        super.onPause()
        locationAsync?.cancel(true)
        mMap.clear()
    }

}
