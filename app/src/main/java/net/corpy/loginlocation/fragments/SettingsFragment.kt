package net.corpy.loginlocation.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import net.corpy.loginlocation.App
import net.corpy.loginlocation.R
import net.corpy.loginlocation.SessionManager

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val notificationGroup = findPreference("notification_group")
        if (App.currentEmployee?.type == 0) notificationGroup.isVisible = false

        val appSettingsKey = findPreference("app_settings_key")
        appSettingsKey.isVisible = true

        val changePassword = findPreference("change_password")
        changePassword.setOnPreferenceClickListener {
            showPasswordDialog()
            true
        }


    }

    @SuppressLint("InflateParams")
    private fun showPasswordDialog() {
        val dialog = AlertDialog.Builder(context!!)
        val view = LayoutInflater.from(context!!).inflate(R.layout.change_password_layout, null)
        dialog.setView(view)
        val d = dialog.create()

        val okBtn = view.findViewById<Button>(R.id.ok_btn)
        val cancelBtn = view.findViewById<Button>(R.id.cancel_btn)


        val oldPassword = view.findViewById<TextInputEditText>(R.id.old_password_et)
        val newPasswordEt = view.findViewById<TextInputEditText>(R.id.new_password_et)
        val repeatNewPasswordEt =
            view.findViewById<TextInputEditText>(R.id.repeat_new_password_et)

        val oldPasswordEtLayout =
            view.findViewById<TextInputLayout>(R.id.old_password_et_layout)
        val newPasswordEtLayout =
            view.findViewById<TextInputLayout>(R.id.new_password_et_layout)
        val repeatNewPasswordEtLayout =
            view.findViewById<TextInputLayout>(R.id.repeat_new_password_et_layout)

        val progressView = view.findViewById<View>(R.id.progress_view)


        okBtn.setOnClickListener {
            if (oldPassword.text.toString() != App.currentEmployee?.password) {
                oldPasswordEtLayout.isErrorEnabled = true
                oldPasswordEtLayout.error = getString(R.string.old_password_not_match)
                return@setOnClickListener
            } else {
                oldPasswordEtLayout.isErrorEnabled = false
            }

            if (newPasswordEt.text.isNullOrEmpty()) {
                newPasswordEtLayout.isErrorEnabled = true
                newPasswordEtLayout.error = getString(R.string.empty_password)
                return@setOnClickListener
            } else {
                newPasswordEtLayout.isErrorEnabled = false
            }

            if (repeatNewPasswordEt.text.toString() != newPasswordEt.text.toString()) {
                repeatNewPasswordEtLayout.isErrorEnabled = true
                repeatNewPasswordEtLayout.error = getString(R.string.new_passowrd_not_match)
                return@setOnClickListener
            } else {
                repeatNewPasswordEtLayout.isErrorEnabled = false
            }

            progressView.visibility = View.VISIBLE
            FirebaseFirestore.getInstance().document("Users/${App.currentEmployee?.id}")
                .update("password", newPasswordEt.text.toString()).addOnCompleteListener {
                    progressView.visibility = View.GONE
                    d.dismiss()
                    if (it.isSuccessful) {
                        Toast.makeText(
                            context,
                            getString(R.string.password_updated_success),
                            Toast.LENGTH_LONG
                        ).show()
                        SessionManager(context!!).logoutEmployee()
                    } else Toast.makeText(
                        context,
                        getString(R.string.error_update_password),
                        Toast.LENGTH_LONG
                    ).show()

                }
        }

        cancelBtn.setOnClickListener {
            d.dismiss()
        }

        d.show()
    }
}