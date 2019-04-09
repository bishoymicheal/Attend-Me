package net.corpy.loginlocation

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import net.corpy.loginlocation.model.Employee

@SuppressLint("CommitPrefEdits")
class SessionManager(private val _context: Context) {
    // Shared Preferences
    private val pref: SharedPreferences
    // Editor for Shared preferences
    private val editor: SharedPreferences.Editor

    /**
     * Get stored session data
     */
    val employeeDetails: Employee
        get() = Employee(
            pref.getString(EMAIL, "") ?: "",
            pref.getString(PHONE, "") ?: "",
            pref.getString(FULL_NAME, "") ?: "",
            pref.getInt(GENDER, -1),
            pref.getString(DESCRIPTION, "") ?: "",
            pref.getString(BIRTH_DATE, "") ?: "",
            pref.getString(PASSWORD, "") ?: "",
            pref.getInt(EMPLOYEE_TYPE, -1),
            pref.getString(EMPLOYEE_ID, "") ?: "",
            pref.getString(IMAGE, "") ?: ""
        )


    /**
     * Quick check for login
     */
    // Get Login State
    val isLoggedIn: Boolean
        get() = pref.getBoolean(IS_LOGIN, false)

    init {
        // Shared pref mode
        pref = _context.getSharedPreferences(PREF_NAME, 0)
        editor = pref.edit()
    }

    /**
     * Create login session
     */
    fun createLoginSession(employee: Employee) {
        // Storing login value as TRUE
        editor.putBoolean(IS_LOGIN, true)

        // Storing name in pref
        editor.putString(FULL_NAME, employee.fullName)
        editor.putString(PASSWORD, employee.password)
        editor.putString(EMAIL, employee.email)
        editor.putString(PHONE, employee.phone)
        editor.putString(DESCRIPTION, employee.description)
        editor.putString(BIRTH_DATE, employee.birthDate)
        editor.putString(EMPLOYEE_ID, employee.id)
        editor.putInt(GENDER, employee.gender)
        editor.putInt(EMPLOYEE_TYPE, employee.type)
        editor.putString(IMAGE, employee.image)

        // commit changes
        editor.commit()
    }

    fun updateImage(image: String) {
        editor.putString(IMAGE, image)
        // commit changes
        editor.commit()
    }

    /**
     * Clear session details
     */
    fun logoutEmployee() {
        // Clearing all data from Shared Preferences
        editor.clear()
        editor.commit()
        // After logout redirect user to Loing Activity
        val i = Intent(_context, LoginActivity::class.java)
        // Closing all the Activities
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        // Add new Flag to start new Activity
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        // Staring Login Activity
        _context.startActivity(i)
    }

    companion object {
        // User name (make variable public to access from outside)
        private const val FULL_NAME = "fullName"
        private const val PASSWORD = "Password"
        private const val EMAIL = "Email"
        private const val PHONE = "phone"
        private const val GENDER = "gender"
        private const val EMPLOYEE_TYPE = "TYPE"
        private const val DESCRIPTION = "description"
        private const val BIRTH_DATE = "birthDate"
        private const val EMPLOYEE_ID = "EMPLOYEE_ID"
        // Sharedpref file name
        private const val PREF_NAME = "LoggingSession"
        // All Shared Preferences Keys
        private const val IS_LOGIN = "IsLoggedIn"
        private const val IMAGE = "IMAGE"
    }
}