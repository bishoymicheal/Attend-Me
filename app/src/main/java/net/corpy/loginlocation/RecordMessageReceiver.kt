package net.corpy.loginlocation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import net.corpy.loginlocation.locationApi.LocationAPI
import net.corpy.loginlocation.locationApi.LocationData
import java.util.*

const val TAG = "RecordMessageReceiver"

class RecordMessageReceiver : FirebaseMessagingService() {
    var locationAPI: LocationAPI? = null

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        Log.d(TAG, "From: ${remoteMessage?.from}")
        // Check if message contains a notification payload.

        //  "remoteMessage?.from"  => "/topics/admins"

        remoteMessage?.notification?.let {

            Log.d(TAG, "Message Notification Body: ${it.body}")
            sendNotification(it.title ?: "New Record", it.body ?: "")
        }



        if (remoteMessage?.from?.contains("employees") == true) {

            locationAPI = LocationAPI(applicationContext)
            locationAPI?.connect()
            locationAPI?.requestLocationUpdate(true)
            locationAPI?.getCurrentLocation(listener)

        }


    }


    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT
        )

        val channelId = "default_notification_channel_id"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_login_right)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "AttendMe New Attendance Record Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }


    val listener: LocationAPI.LocationChangeListener = object : LocationAPI.LocationChangeListener {
        override fun onLocationSuccess(location: LocationData?) {
            if (location != null) {
                locationAPI?.requestLocationUpdate(false)
                locationAPI?.setLocationChangeListener(null)
                val data = HashMap<String, Any>()
                data["name"] = App.currentEmployee?.fullName ?: ""
                data["description"] = App.currentEmployee?.description ?: ""
                data["lat"] = location.latitude
                data["long"] = location.longitude

                val docname = "/${App.currentEmployee?.id}"
                FirebaseFirestore.getInstance()
                    .collection(
                        "employeeRequests/requests/${DateFormatter.getDashedDayFormat().format(
                            Calendar.getInstance().time
                        )}"
                    )
                    .document(docname)
                    .set(data, SetOptions.merge())
            } else {
                Utils.showErrorDialog(
                    applicationContext,
                    getString(R.string.oops),
                    getString(R.string.location_detect_error),
                    getString(R.string.retry)
                ) {
                    updateListener()
                }
            }
        }

        override fun onLocationFailed(errorMessage: String?) {
            Utils.showErrorDialog(
                applicationContext,
                getString(R.string.oops),
                errorMessage ?: getString(R.string.location_detect_error),
                getString(R.string.retry)
            ) {
                updateListener()
            }
        }
    }

    private fun updateListener() {
        locationAPI?.getCurrentLocation(listener)
    }

}