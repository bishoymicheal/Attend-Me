package net.corpy.loginlocation

import android.app.DownloadManager
import android.content.*
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.core.view.get
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.android.synthetic.main.activity_home.*
import net.corpy.loginlocation.fragments.home.DashboardFragment
import net.corpy.loginlocation.language.LocaleManager
import net.corpy.loginlocation.locationApi.LocationAPI
import net.corpy.loginlocation.locationApi.LocationData
import net.corpy.loginlocation.model.ADMIN
import net.corpy.loginlocation.model.MANAGER
import java.util.*

class HomeActivity : BaseActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        var notificationIntent: Intent? = null
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "notification_checkbox") {
            val sub = sharedPreferences?.getBoolean(key, true)
            if (sub == true) {
                subscribeToNotificationTopic()
            } else {
                unsubscribeToNotificationTopic()
            }
        }

        if (key == "language_list") {
            val lang = sharedPreferences?.getString(key, "en")
            if (!LocaleManager.getLocale(resources).language.contains(lang ?: "en"))
                LocaleManager.setNewLocale(this, lang, true)

        }
    }


    private lateinit var drawerLayout: DrawerLayout
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        drawerLayout = drawer_layout
        navController = Navigation.findNavController(this, R.id.nav_host_fragment)

        when (App.currentEmployee?.type) {
            ADMIN -> {
                subscribeToNotificationTopic()
                navigationView.menu[ADMIN_DASHBOARD_FRAGMENT_ID].isVisible = true
                navigationView.menu[DASHBOARD_FRAGMENT_ID].isVisible = false
                navigationView.menu[EMPLOYEES_FRAGMENT_ID].isVisible = true
                navigationView.menu[ADMIN_LOCATIONS_FRAGMENT_ID].isVisible = true
                navigationView.menu[LOCATIONS_FRAGMENT_ID].isVisible = false
                navigationView.menu[ADMIN_REPORTS_FRAGMENT_ID].isVisible = true
                navigationView.menu[REPORTS_FRAGMENT_ID].isVisible = false
            }
            MANAGER -> {
                subscribeToNotificationTopic()
                navigationView.menu[ADMIN_DASHBOARD_FRAGMENT_ID].isVisible = true
                navigationView.menu[DASHBOARD_FRAGMENT_ID].isVisible = false
                navigationView.menu[EMPLOYEES_FRAGMENT_ID].isVisible = true
                navigationView.menu[ADMIN_LOCATIONS_FRAGMENT_ID].isVisible = false
                navigationView.menu[LOCATIONS_FRAGMENT_ID].isVisible = true
                navigationView.menu[ADMIN_REPORTS_FRAGMENT_ID].isVisible = true
                navigationView.menu[REPORTS_FRAGMENT_ID].isVisible = false
            }
            else -> {

                navController.graph.startDestination = R.id.dashboard_fragment
                navController.navigate(R.id.dashboard_fragment)

                navigationView.menu[ADMIN_DASHBOARD_FRAGMENT_ID].isVisible = false
                navigationView.menu[DASHBOARD_FRAGMENT_ID].isVisible = true
                navigationView.menu[EMPLOYEES_FRAGMENT_ID].isVisible = false
                navigationView.menu[ADMIN_LOCATIONS_FRAGMENT_ID].isVisible = false
                navigationView.menu[LOCATIONS_FRAGMENT_ID].isVisible = true
                navigationView.menu[ADMIN_REPORTS_FRAGMENT_ID].isVisible = false
                navigationView.menu[REPORTS_FRAGMENT_ID].isVisible = true
                navigationView.menu[ADMIN_REQUEST_FRAGMENT_ID].isVisible = false
                subscribeToNotificationTopicEmployees()
            }
        }

        appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)

        // Set up ActionBar
        setSupportActionBar(toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Set up navigation menu
        navigationView.setupWithNavController(navController)


        val headerView = navigationView.getHeaderView(0)
        val employeeName = headerView?.findViewById<TextView>(R.id.title_txt)
        employeeName?.text = App.currentEmployee?.fullName

        val employeeEmail = headerView?.findViewById<TextView>(R.id.email_txt)
        employeeEmail?.text = App.currentEmployee?.email

        signout.setOnClickListener {
            drawer_layout.closeDrawers()
            Utils.showWarningDialog(this, getString(R.string.logout), getString(R.string.logout_confirm), true) {
                unsubscribeToNotificationTopic()
                unsubscribeToNotificationTopicEmployees()
                FirebaseAuth.getInstance().signOut()
                SessionManager(this).logoutEmployee()
                finish()
            }
        }

    }

    var locationAPI: LocationAPI? = null
    override fun onResume() {
        super.onResume()
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this)
        locationAPI = LocationAPI(applicationContext)
        locationAPI?.connect()
        locationAPI?.requestLocationUpdate(true)
        val intent = notificationIntent
        if (intent != null && intent.extras?.containsKey("request") == true) {
            locationAPI?.getCurrentLocation(listener)
        }

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

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(onDownloadComplete)
    }

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //Fetching the download id received with the broadcast
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            //Checking if the received broadcast is for our enqueued download by matching download id
            if (App.downloadList.contains(id)) {
                Toast.makeText(context, getString(R.string.download_complet), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun subscribeToNotificationTopic() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val isNeedToSubscribe = sharedPreferences.getBoolean("notification_checkbox", true)

        if (isNeedToSubscribe) {
            FirebaseMessaging.getInstance().subscribeToTopic("admins")
                .addOnCompleteListener { task ->
                    var msg = "subscribed"
                    if (!task.isSuccessful) {
                        msg = "subscribe failed"
                    }
                    Log.d("subscribeToTopic ", msg)
                }
        }

    }

    private fun subscribeToNotificationTopicEmployees() {
        FirebaseMessaging.getInstance().subscribeToTopic("employees")
            .addOnCompleteListener { task ->
                var msg = "subscribed"
                if (!task.isSuccessful) {
                    msg = "subscribe failed"
                }
                Log.d("subscribeToTopic ", msg)
            }

    }

    private fun unsubscribeToNotificationTopic() {
        FirebaseMessaging.getInstance().unsubscribeFromTopic("admins")
            .addOnCompleteListener { task ->
                var msg = "unsubscribed"
                if (!task.isSuccessful) {
                    msg = "unsubscribe failed"
                }
                Log.d("unsubscribeToTopic ", msg)
            }
    }

    private fun unsubscribeToNotificationTopicEmployees() {
        FirebaseMessaging.getInstance().unsubscribeFromTopic("employees")
            .addOnCompleteListener { task ->
                var msg = "unsubscribed"
                if (!task.isSuccessful) {
                    msg = "unsubscribe failed"
                }
                Log.d("unsubscribeToTopic ", msg)
            }
    }


    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else if (getCurrentFragment() != null && getCurrentFragment() is DashboardFragment) {
            finish()
        } else {
            super.onBackPressed()
        }
    }

    private fun getCurrentFragment(): Fragment? {
        val currentNavHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val currentFragmentClassName =
            (navController.currentDestination as FragmentNavigator.Destination).className
        return currentNavHost?.childFragmentManager?.fragments?.filterNotNull()?.find {
            it.javaClass.name == currentFragmentClassName
        }
    }

}
