package net.corpy.loginlocation.fragments.employeesLocations


import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.android.synthetic.main.fragment_employees_locations.*
import net.corpy.loginlocation.DateFormatter
import net.corpy.loginlocation.R
import net.corpy.loginlocation.Utils
import net.corpy.loginlocation.locationApi.LocationData
import net.corpy.loginlocation.model.EmployeeLocation
import net.corpy.permissionslib.PermissionsFile
import java.util.*
import kotlin.collections.ArrayList

class EmployeesLocations : Fragment(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private var mapView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_employees_locations, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapView = mapFragment?.view
        mapFragment?.getMapAsync(this)

        progress_view.visibility = View.VISIBLE
        request_btn?.setOnClickListener {
            getEmployeesLocations()
        }
        cancel_btn.setOnClickListener {
            it.findNavController().navigateUp()
        }

    }

    override fun onResume() {
        super.onResume()
        if (isMapReady) registerLocationsListener()
    }

    private var listener: ListenerRegistration? = null

    private var locationsList: ArrayList<EmployeeLocation>? = ArrayList()

    fun registerLocationsListener() {
        listener?.remove()
        val path = "employeeRequests/requests/${DateFormatter.getDashedDayFormat().format(Calendar.getInstance().time)}"
        listener = FirebaseFirestore.getInstance().collection(path)
            .addSnapshotListener { querySnapshot, fireBaseFireStoreException ->
                if (fireBaseFireStoreException != null) {
                    Utils.showErrorDialog(
                        context,
                        getString(R.string.oops),
                        getString(R.string.error_requesting_locations),
                        getString(R.string.retry)
                    ) {}
                    return@addSnapshotListener
                }


                locationsList?.clear()
                querySnapshot?.documents?.forEach {
                    val eLocation = it.toObject(EmployeeLocation::class.java)
                    eLocation?.id = it.id
                    eLocation?.let {
                        locationsList?.add(eLocation)
                    }
                }
                updateMap()

                mMap?.let {
                    val list = locationsList ?: ArrayList()
                    if (!list.isEmpty())
                        Utils.drawCenterCircle(it, list)
                }
                /*  val builder = LatLngBounds.Builder()

                  locationsList?.forEach {
                      val start = LatLng(it.lat, it.long)
                      builder.include(start)
                  }

                  val latLngBounds = builder.build()

                  mMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 15))*/

            }
    }

    private fun updateMap() {
        mMap?.clear()
        mMap?.let {
            locationsList?.forEach {
                val location = LatLng(it.lat, it.long)
                val marker = mMap?.addMarker(MarkerOptions().position(location).title(it.name))
                marker?.tag = it
            }
        }
    }

    override fun onPause() {
        super.onPause()

        listener?.remove()
    }

    private fun getEmployeesLocations() {
        progress_view.visibility = View.VISIBLE

        FirebaseFunctions.getInstance()
            .getHttpsCallable("locationReq")
            .call()
            .addOnSuccessListener { httpsCallableResult ->
                progress_view.visibility = View.GONE
                val status = httpsCallableResult.data as Boolean
                if (status) {
                    Utils.showSuccessDialog(
                        context,
                        getString(R.string.request_employees_location_success),
                        getString(R.string.request_location_message)
                    ) {}
                } else {
                    Utils.showErrorDialog(
                        context!!, getString(R.string.request_location_failed),
                        getString(R.string.error_requesting_locations), getString(R.string.retry)
                    ) {}
                }
            }
            .addOnFailureListener {
                progress_view.visibility = View.GONE
                Utils.showErrorDialog(
                    context, getString(R.string.oops),
                    getString(R.string.cannot_connect_to_server), getString(R.string.retry)
                ) {}
            }
    }

    var isMapReady = false

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {

        if (!PermissionsFile.locationPermissionCheck(context)) {
            PermissionsFile.showLocationDialogPermission(context) {
                if (it) {
                    googleMap.isMyLocationEnabled = true
                    googleMap.uiSettings.isMyLocationButtonEnabled = true
                }
            }
        } else {
            googleMap.isMyLocationEnabled = true
            googleMap.uiSettings.isMyLocationButtonEnabled = true
        }

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


        mMap = googleMap
        mMap?.setOnMapLoadedCallback {
            showData()
        }

        mMap?.setOnMarkerClickListener { marker ->
            if (marker.tag == null || marker.tag !is EmployeeLocation) return@setOnMarkerClickListener false

            val eLocation = marker.tag as EmployeeLocation

            val loca = Location("")
            loca.latitude = eLocation.lat
            loca.longitude = eLocation.long
            context?.let { context ->
                Utils.getLocationData(context, loca) {
                    if (it != null) {
                        updateText(it, eLocation)
                    }
                }
            }
            return@setOnMarkerClickListener true
        }

    }

    private fun updateText(locationData: LocationData, employeeLocation: EmployeeLocation) {
        employee_details.text = "${employeeLocation.name} is located at \n ${locationData.fullAddress}"
    }

    private fun showData() {
        progress_view.visibility = View.GONE

        isMapReady = true

        registerLocationsListener()
        /*  this.mMap.addMarker(MarkerOptions().position(loc))
          this.mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 17f))
          this.mMap.addCircle(
              CircleOptions().center(loc).radius(location?.radius ?: 0.0).strokeWidth(2f).strokeColor(
                  ContextCompat.getColor(
                      context!!,
                      R.color.primaryColor
                  )
              )
          )
          Utils.zoomInCircle(this.mMap, loc, location?.radius ?: 0.0)*/
    }

}
