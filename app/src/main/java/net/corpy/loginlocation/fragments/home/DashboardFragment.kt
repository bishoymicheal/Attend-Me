package net.corpy.loginlocation.fragments.home

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.dashboard_fragment.*
import net.corpy.loginlocation.*
import net.corpy.loginlocation.language.LocaleManager
import net.corpy.loginlocation.locationApi.LocationAPI
import net.corpy.loginlocation.locationApi.LocationData
import net.corpy.loginlocation.model.Employee
import net.corpy.loginlocation.model.Location
import net.corpy.permissionslib.PermissionsFile
import java.io.IOException
import java.util.*
import kotlin.collections.HashMap

const val PICK_IMAGE_REQUEST = 71

class DashboardFragment : Fragment() {

    private val locationApi by lazy {
        LocationAPI(activity)
    }

    private var documentId: Employee? = null

    private val db = FirebaseFirestore.getInstance()


    private val locRef = db.collection("Locations")
    private val infoRef = db.collection("Info")


    private var storage: FirebaseStorage? = null
    private var storageReference: StorageReference? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dashboard_fragment, container, false)
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        storage = FirebaseStorage.getInstance()
        storageReference = FirebaseStorage.getInstance().reference


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)

        App.currentEmployee?.let {
            documentId = it
        }
        if (documentId == null) activity?.finish()


        name_txt?.text = App.currentEmployee?.fullName
        desc_txt?.text = App.currentEmployee?.description
        mobile_txt?.text =
            LocaleManager.convertDigitsTo(LocaleManager.getLocale(resources).language, App.currentEmployee?.phone)
        email_txt?.text = App.currentEmployee?.email

        locationApi.connect()

        check_in_btn?.setOnClickListener {
            getLocation()
        }


        profile_image.setOnClickListener {
            if (!PermissionsFile.StoragePermissionCheck(context)) {
                PermissionsFile.showStorageDialogPermission(context) {
                    if (it) {
                        chooseImage()
                    } else {
                        Toast.makeText(context, getString(R.string.storage_permission_reqiest), Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } else {
                chooseImage()
            }
        }

        if (!App.currentEmployee?.image.isNullOrEmpty()) {
            image_progress.visibility = View.VISIBLE
            Picasso.get().load(App.currentEmployee?.image)
                .placeholder(R.drawable.profile)
                .error(R.drawable.profile)
                .into(profile_image, object : Callback {
                    override fun onError(e: Exception?) {
                        image_progress.visibility = View.GONE
                    }

                    override fun onSuccess() {
                        image_progress.visibility = View.GONE
                    }
                })
        }
    }

    override fun onResume() {
        super.onResume()
        check_in_btn?.isEnabled = false

        updateCheckingBtn()
    }


    private var checkingIn: Boolean? = true

    private fun updateCheckingBtn() {
        var timeinmillis: Long
        FirebaseFunctions.getInstance()
            .getHttpsCallable("getTime")
            .call()
            .addOnSuccessListener { httpsCallableResult ->
                timeinmillis = httpsCallableResult.data as Long
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = timeinmillis


                db.document(
                    "Users/${documentId?.id}/attendance/" +
                            DateFormatter.getDashedDayFormat().format(calendar.time)
                ).get(Source.SERVER).addOnCompleteListener {
                    if (it.isSuccessful) {
                        if (it.result?.contains("in") == true) {
                            checkingIn = false
                            check_in_btn?.isEnabled = true
                            check_in_btn?.text = getString(R.string.Checkout)
                        } else {
                            check_in_btn?.isEnabled = true
                            checkingIn = true
                        }
                    }
                }

            }
            .addOnFailureListener {
                Utils.showErrorDialog(
                    context!!, getString(R.string.oops),
                    getString(R.string.cannot_connect_to_server), getString(R.string.retry)
                ) {
                    updateCheckingBtn()
                }
            }
    }

    private val locationListener = object : LocationAPI.LocationChangeListener {
        override fun onLocationSuccess(location: LocationData?) {

            if (location == null) {
                progress_view?.visibility = View.GONE
                Utils.showErrorDialog(
                    context!!, getString(R.string.detecting_location),
                    getString(R.string.error_try_again), getString(R.string.retry)
                ) {
                    locationApi.getCurrentLocation(null)
                }
                return
            }
            getNearestLocation(location)
        }

        override fun onLocationFailed(errorMessage: String?) {
            progress_view?.visibility = View.GONE
            context?.let {
                Utils.showErrorDialog(
                    it, getString(R.string.checking),
                    "${errorMessage ?: "Error"} Try Again", getString(R.string.retry)
                ) {
                    locationApi.getCurrentLocation(null)
                }
            }
        }

    }

    private fun getLocation() {
        progress_view.visibility = View.VISIBLE
        locationApi.getCurrentLocation(locationListener)
    }

    override fun onPause() {
        super.onPause()

        locationApi.getCurrentLocation(null)

    }

    private var locationDetect = false

    private fun getNearestLocation(location: LocationData) {

        locRef.get().addOnSuccessListener { querySnapshot ->
            querySnapshot.documents.forEach { document ->
                if (document.exists()) {

                    val locationDocument = document.toObject(Location::class.java)

                    val lat = locationDocument?.lat ?: 0.0
                    val lng = locationDocument?.lng ?: 0.0

                    val radius = locationDocument?.radius ?: 0.0

                    val userLat = location.latitude
                    val userLng = location.longitude

                    val distance = Utils.getDistanceBetweenTwoPoints(
                        userLat,
                        userLng,
                        lat,
                        lng
                    )
                    if (distance <= radius) {
                        locationDetect = true
                        getServerTime(locationDocument?.title)
                        return@addOnSuccessListener
                    } else {
                        return@forEach
                    }

                }
            }

            if (!locationDetect) {
                progress_view.visibility = View.GONE
                showLocationNotDetectedDialog()
            }

        }.addOnFailureListener {
            progress_view.visibility = View.GONE
            Utils.showErrorDialog(
                context!!, getString(R.string.checking),
                getString(R.string.location_detect_error), getString(R.string.retry)
            ) {

            }
        }
    }

    private fun getServerTime(locationTitle: String?) {
        var timeinmillis: Long

        FirebaseFunctions.getInstance()
            .getHttpsCallable("getTime")
            .call()
            .addOnSuccessListener { httpsCallableResult ->
                timeinmillis = httpsCallableResult.data as Long
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = timeinmillis

                updateInfoDocument(calendar)
                showLocationDetectDialog(locationTitle ?: "", calendar)

            }.addOnFailureListener {
                progress_view?.visibility = View.GONE
                showLocationNotDetectedDialog()
            }

    }

    private fun updateInfoDocument(calendar: Calendar) {
        if (checkingIn == true)
            infoRef.document(DateFormatter.getDashedDayFormat().format(calendar.time))
                .get(Source.SERVER).addOnCompleteListener {
                    if (it.isSuccessful) {
                        if (it.result?.exists() == true) {
                            val count = it.result?.get("count")
                            try {
                                var num = Integer.parseInt(count.toString())
                                num++
                                infoRef.document(
                                    it.result?.id ?: (DateFormatter.getDashedDayFormat().format(calendar.time))
                                ).update("count", num).addOnCompleteListener {}
                            } catch (e: Exception) {
                            }
                        } else {
                            val data = HashMap<String, Any>()
                            data.set(key = "count", value = 1)
                            infoRef.document(DateFormatter.getDashedDayFormat().format(calendar.time))
                                .set(data)
                        }

                    }
                }
    }

    private fun showLocationNotDetectedDialog() {
        progress_view?.visibility = View.GONE
        locationApi.requestLocationUpdate(false)
        Utils.showErrorDialog(
            context!!,
            getString(R.string.checking),
            getString(R.string.location_detect_error),
            getString(R.string.retry)
        ) {

        }
    }

    private fun showLocationDetectDialog(locationTitle: String, calendar: Calendar) {

        val type = when (checkingIn) {
            true -> "in"
            false -> "out"
            else -> {
                progress_view?.visibility = View.GONE
                return
            }
        }

        if (type != "in" && type != "out") {
            progress_view?.visibility = View.GONE
            showLocationNotDetectedDialog()
            return
        }

        val data = HashMap<String, Any>()
        data[type] = FieldValue.serverTimestamp()
        data["${type}_location"] = locationTitle
        db.document(
            "Users/${documentId?.id}/attendance/" +
                    DateFormatter.getDashedDayFormat().format(calendar.time)
        ).set(data, SetOptions.merge())
            .addOnSuccessListener {
                updateCheckingBtn()
                progress_view?.visibility = View.GONE
                Utils.showSuccessDialog(
                    context!!, getString(R.string.checking),
                    getString(R.string.location_detected, locationTitle), true
                ) {

                }
            }.addOnFailureListener {
                progress_view?.visibility = View.GONE
                showLocationNotDetectedDialog()
            }
    }

    private fun chooseImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
            && data != null && data.data != null
        ) {
            val filePath = data.data ?: return

            try {
                val bitmap = MediaStore.Images.Media.getBitmap(context?.contentResolver, filePath)
                profile_image.setImageBitmap(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
            }


            progress_view.visibility = View.VISIBLE
            val ref = FirebaseStorage.getInstance().reference.child("images/" + UUID.randomUUID().toString())
            val uploadTask = ref.putFile(filePath)
            uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        progress_view.visibility = View.GONE
                        Toast.makeText(context, "Failed " + task.exception?.message, Toast.LENGTH_SHORT).show()
                        throw it
                    }
                }
                return@Continuation ref.downloadUrl
            }).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    if (documentId?.id == null) {
                        progress_view.visibility = View.GONE
                        return@addOnCompleteListener
                    }

                    if (!App.currentEmployee?.image.isNullOrEmpty())
                        FirebaseStorage.getInstance().getReferenceFromUrl(App.currentEmployee?.image!!).delete()

                    FirebaseFirestore.getInstance().document("Users/" + documentId?.id!!)
                        .update("image", downloadUri.toString())
                        .addOnCompleteListener {
                            progress_view.visibility = View.GONE
                            if (it.isSuccessful) {
                                App.currentEmployee?.image = downloadUri.toString()
                                if (SessionManager(context!!).isLoggedIn) {
                                    SessionManager(context!!).updateImage(downloadUri.toString())
                                }
                                Utils.showSuccessDialog(
                                    context!!,
                                    getString(R.string.image_uploading),
                                    getString(R.string.image_upload_success)
                                ) {}
                            } else {
                                Utils.showErrorDialog(
                                    context!!, getString(R.string.oops),
                                    getString(R.string.failed_to_upload_image), getString(R.string.retry)
                                ) {}
                            }
                        }

                } else {
                    progress_view.visibility = View.GONE
                    Utils.showErrorDialog(
                        context!!,
                        getString(R.string.oops),
                        getString(R.string.failed_to_upload_image),
                        getString(R.string.retry)
                    ) {}
                }
            }
        }
    }


}


