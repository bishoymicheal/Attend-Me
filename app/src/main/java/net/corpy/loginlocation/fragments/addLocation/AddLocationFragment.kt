package net.corpy.loginlocation.fragments.addLocation


import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.textfield.TextInputEditText
import kotlinx.android.synthetic.main.add_location_fragment.*
import net.corpy.loginlocation.R
import net.corpy.loginlocation.Utils
import net.corpy.loginlocation.locationApi.LocationAsync


class AddLocationFragment : Fragment(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap

    private var radius = 500.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.add_location_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val mapFragment = childFragmentManager.findFragmentById(net.corpy.loginlocation.R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        settings_btn.setOnClickListener {
            showRadiusDialog()
        }

    }

    @SuppressLint("InflateParams")
    private fun showRadiusDialog() {
        val dialog = AlertDialog.Builder(context!!)
        val view = LayoutInflater.from(context).inflate(R.layout.radius_layout, null)
        dialog.setView(view)
        val d = dialog.create()
        d.show()

        val radius_et = view.findViewById<TextInputEditText>(R.id.circle_radius_et)

        val ok_btn = view.findViewById<Button>(R.id.confirm_button)
        ok_btn.setOnClickListener {
            if (radius_et != null && radius_et.text?.isEmpty() == false)
                radius = try {
                    radius_et.text?.toString()?.toDouble()!!
                } catch (e: Exception) {
                    500.0
                }

            circle?.remove()
            circle = mMap.addCircle(
                CircleOptions().center(location).radius(radius).strokeWidth(2f).strokeColor(
                    ContextCompat.getColor(
                        context!!,
                        R.color.primaryColor
                    )
                )
            )

            Utils.zoomInCircle(mMap, location, radius)

            d.dismiss()
        }
        val cancel_btn = view.findViewById<Button>(R.id.cancel_button)
        cancel_btn.setOnClickListener {
            d.dismiss()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLoadedCallback {
            mapLoaded(mMap)
        }
    }

    var marker: Marker? = null
    var circle: Circle? = null
    var location: LatLng = LatLng(-34.0, 151.0)

    private fun mapLoaded(mMap: GoogleMap) {
        // Add a marker in Sydney and move the camera
        this.mMap = mMap

            showData()

    }

    var locationAsync: LocationAsync? = null
    fun showData() {

        marker = this.mMap.addMarker(MarkerOptions().position(location))
        this.mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 16f))
        circle = this.mMap.addCircle(
            CircleOptions().center(location).radius(radius).strokeWidth(2f).strokeColor(
                ContextCompat.getColor(
                    context!!,
                    R.color.primaryColor
                )
            )
        )

        Utils.zoomInCircle(this.mMap, location, radius)

        this.mMap.setOnCameraMoveListener {
            location = this.mMap.projection.visibleRegion.latLngBounds.center

            marker?.remove()
            circle?.remove()

            marker = this.mMap.addMarker(MarkerOptions().position(location))
            circle = this.mMap.addCircle(
                CircleOptions().center(location).radius(radius).strokeWidth(2f).strokeColor(
                    ContextCompat.getColor(
                        context!!,
                        R.color.primaryColor
                    )
                )
            )

            locationAsync?.cancel(true)

            val loca = Location("")
            loca.latitude = location.latitude
            loca.longitude = location.longitude
            locationAsync =  Utils.getLocationData(context!!, loca) {
                current_location?.text = it?.fullAddress
            }
        }
    }

    override fun onPause() {
        super.onPause()
        locationAsync?.cancel(true)
        mMap.clear()
    }

}
