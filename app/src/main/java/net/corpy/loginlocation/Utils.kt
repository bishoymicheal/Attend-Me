package net.corpy.loginlocation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil
import com.hazem.utilslib.libs.UtilsFunctions
import net.corpy.loginlocation.dialogLib.SweetAlertDialog
import net.corpy.loginlocation.language.LocaleManager
import net.corpy.loginlocation.locationApi.LocationAsync
import net.corpy.loginlocation.locationApi.LocationData
import net.corpy.loginlocation.model.EmployeeLocation
import java.util.*


object Utils {

    fun showErrorDialog(
        context: Context?, title: String, msg: String, confirmText: String, cancellable: Boolean = false,
        onClick: () -> Unit
    ) {

        context?.let {
            val dialog = SweetAlertDialog(it, SweetAlertDialog.ERROR_TYPE)
            dialog.titleText = title
            dialog.contentText = msg
            dialog.setCancelable(cancellable)
            dialog.confirmText = confirmText
            dialog.setConfirmClickListener {
                onClick()
                dialog.dismissWithAnimation()
            }
            dialog.show()

        }
    }

    fun showSuccessDialog(
        context: Context?, title: String, msg: String,
        cancellable: Boolean = false, onClick: () -> Unit
    ) {

        context?.let {
            val dialog = SweetAlertDialog(it, SweetAlertDialog.SUCCESS_TYPE)
            dialog.titleText = title
            dialog.contentText = msg
            dialog.setCancelable(cancellable)
            dialog.confirmText = it.getString(android.R.string.ok)
            dialog.setConfirmClickListener {
                onClick()
                dialog.dismissWithAnimation()
            }
            dialog.show()

        }
    }

    fun hideKeyboard(context: Context?, view: View) {
        val inputManager: InputMethodManager? =
            context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager?.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    fun showWarningDialog(
        context: Context?, title: String, msg: String,
        cancellable: Boolean = false, onClick: () -> Unit
    ) {
        context?.let {
            val dialog = SweetAlertDialog(it, SweetAlertDialog.WARNING_TYPE)
            dialog.titleText = title
            dialog.contentText = msg
            dialog.setCancelable(cancellable)
            dialog.cancelText = it.getString(android.R.string.cancel)
            dialog.showCancelButton(true)
            dialog.confirmText = it.getString(android.R.string.ok)
            dialog.setCancelClickListener { sDialog -> sDialog.cancel() }
            dialog.setConfirmClickListener {
                onClick()
                dialog.dismissWithAnimation()
            }
            dialog.show()
        }
    }

    fun zoomInCircle(mMap: GoogleMap, center: LatLng, radios: Double) {
        val latLngBounds = getCircleBounds(center, radios)
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 0))
    }

    fun isValidEmail(target: CharSequence): Boolean {
        return if (TextUtils.isEmpty(target)) {
            false
        } else {
            android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches()
        }
    }

    fun getCircleBounds(center: LatLng, radiusInMeters: Double): LatLngBounds {
        val distanceFromCenterToCorner = radiusInMeters * Math.sqrt(2.0)
        val southwestCorner = SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 245.0)
        val northeastCorner = SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 45.0)
        return LatLngBounds(southwestCorner, northeastCorner)
    }


    fun getLocationData(mContext: Context, location: Location, dataReady: (LocationData?) -> Unit): LocationAsync {
        val gcd = Geocoder(mContext.applicationContext, LocaleManager.getLocale(mContext.resources))
        val loc = LocationAsync(
            gcd, location,
            LocationAsync.DataReady { data ->
                dataReady(data)
            })
        loc.execute()
        return loc
    }

    fun getAddressData(mContext: Context, address: String, dataReady: (LocationData?) -> Unit): LocationAsync {
        val gcd = Geocoder(mContext.applicationContext, LocaleManager.getLocale(mContext.resources))
        val loc = LocationAsync(
            gcd, address,
            LocationAsync.DataReady { data ->
                dataReady(data)
            })
        loc.execute()
        return loc
    }

    private fun getNetworkInfo(context: Context): NetworkInfo? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo
    }

    /**
     * Check if There Is Network Connection Available
     *
     * @param context context only needed
     * @return true if there is network available other wise return false
     */
    @JvmStatic
    fun isNetworkConnected(context: Context): Boolean {
        val info = getNetworkInfo(context)
        return info != null && info.isConnected
    }

    fun getDistanceBetweenTwoPoints(startLat: Double, startLng: Double, endLat: Double, endLng: Double): Double {
        val start = LatLng(startLat, startLng)
        val end = LatLng(endLat, endLng)

        return SphericalUtil.computeDistanceBetween(start, end)
    }

    fun setAppLanguage(context: Context, languageCode: String, onComplete: () -> Unit) {
        try {
            val locale = Locale(languageCode)
            val lang = locale.language
            val current = context.resources.configuration.locale
            val LAng = current.language
            if (lang != LAng) {
                Locale.setDefault(locale)
                val config = Configuration()
                config.locale = locale
                context.resources.updateConfiguration(config, context.resources.displayMetrics)
                saveLanguage(context, languageCode)
                if (context is Activity) {
                    val i = context.baseContext.packageManager
                        .getLaunchIntentForPackage(context.baseContext.packageName)
                    i?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    context.startActivity(i)
                    context.overridePendingTransition(0, 0)
                }
            } else {
                onComplete()
            }
        } catch (e: Exception) {
        }
    }

    private fun saveLanguage(context: Context, languageCode: String) {
        val editor = context.getSharedPreferences(UtilsFunctions.PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.putString(UtilsFunctions.PREF_LANGUAGE, languageCode)
        editor.apply()
    }

    fun getCurrentLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(UtilsFunctions.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(UtilsFunctions.PREF_LANGUAGE, "en") ?: "en"
    }

    fun drawCenterCircle(
        mMap: GoogleMap,
        locationsList: ArrayList<EmployeeLocation>
        /* , strokeWidth: Float,
          strokeColor: Int,
          fillColor: Int*/
    ) {


        val builder = LatLngBounds.Builder()

        locationsList.forEach {
            val start = LatLng(it.lat, it.long)
            builder.include(start)
        }

        val latLngBounds = builder.build()
        val center1 = latLngBounds.center

        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 0))


        var radius = 0.0

        locationsList.forEach {
            val d = getDistanceBetweenTwoPoints(center1.latitude, center1.longitude, it.lat, it.long)
            if (radius <= d) radius = (d + 10)
        }

//        val radios = SphericalUtil.computeDistanceBetween(start, end) / 2


        addCircle(mMap, center1, radius)

    }


    private fun addCircle(
        mMap: GoogleMap,
        center: LatLng, radios: Double/*, strokeWidth: Float,
        strokeColor: Int, fillColor: Int*/
    ) {
        val c = mMap.addCircle(
            CircleOptions()
                .center(center)
                .radius(radios)
                .strokeWidth(2f).strokeColor(Color.parseColor("#16738F"))
        )

        zoomInCircle(mMap, center, radios)
    }

}