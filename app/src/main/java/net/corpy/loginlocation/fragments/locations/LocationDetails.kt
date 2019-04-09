package net.corpy.loginlocation.fragments.locations


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.fragment_location_details.*
import net.corpy.loginlocation.R
import net.corpy.loginlocation.Utils
import net.corpy.loginlocation.language.LocaleManager
import net.corpy.loginlocation.model.Location

class LocationDetails : Fragment(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap

    companion object {
        var location: Location? = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_location_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        progress_view.visibility = View.VISIBLE

        ok_btn.setOnClickListener {
            it.findNavController().navigateUp()
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLoadedCallback {
            mapLoaded(mMap)
        }
    }

    private fun mapLoaded(mMap: GoogleMap) {
        this.mMap = mMap
        showData()
    }

    private fun showData() {
        progress_view.visibility = View.GONE

        current_location.text = location?.title
        radius_text.text = LocaleManager.convertDigitsTo(
            LocaleManager.getLocale(resources).language, getString(R.string.radius, location?.radius.toString())
        )

        val loc = LatLng(location?.lat ?: 0.0, location?.lng ?: 0.0)
        this.mMap.addMarker(MarkerOptions().position(loc))
        this.mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 17f))
        this.mMap.addCircle(
            CircleOptions().center(loc).radius(location?.radius ?: 0.0).strokeWidth(2f).strokeColor(
                ContextCompat.getColor(
                    context!!,
                    R.color.primaryColor
                )
            )
        )
        Utils.zoomInCircle(this.mMap, loc, location?.radius ?: 0.0)
    }

}
