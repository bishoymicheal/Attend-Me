package net.corpy.loginlocation

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.android.synthetic.main.login_activity.*
import net.corpy.loginlocation.model.Employee


class LoginActivity : BaseActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val usersRef = db.collection("Users")

    private var auth: FirebaseAuth = FirebaseAuth.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        HomeActivity.notificationIntent = intent

        login_btn.setOnClickListener {
            Utils.hideKeyboard(this, password_edit_text)

            progress_view.visibility = View.VISIBLE

            val document = usersRef.whereEqualTo(EMAIL_KEY, email_edit_text.text.toString())
            document.get(Source.SERVER).addOnCompleteListener { task ->

                textInputLayout.isErrorEnabled = false
                textInputLayout2.isErrorEnabled = false


                if (task.isSuccessful) {
                    val result = task.result
                    if (result?.documents?.size == 0) {
                        progress_view.visibility = View.GONE
                        textInputLayout.isErrorEnabled = true
                        textInputLayout.error = getString(R.string.email_not_correct)
                        return@addOnCompleteListener
                    }
                    result?.documents?.forEach { documentSnapshot ->
                        if (documentSnapshot.getString(PASSWORD_KEY).toString() != password_edit_text.text.toString()) {
                            progress_view.visibility = View.GONE
                            textInputLayout2.isErrorEnabled = true
                            textInputLayout2.error = getString(R.string.password_not_correct)
                            return@addOnCompleteListener
                        } else {
                            App.currentEmployee = documentSnapshot.toObject(Employee::class.java)
                            App.currentEmployee?.id = documentSnapshot.id
                            App.currentEmployee?.let {
                                if (save_login_ck?.isChecked == true) {
                                    SessionManager(this).createLoginSession(it)
                                }
                                firebaseSigning()
                            }
                            return@addOnCompleteListener
                        }
                    }
                } else {
                    progress_view.visibility = View.GONE
                    Utils.showErrorDialog(
                        this, getString(R.string.oops),
                        task.exception?.message ?: "Error Connected", getString(R.string.retry), true
                    ) {
                    }
                }
            }

        }

        forget_password?.setOnClickListener {
            startActivity(Intent(this, ForgetPassword::class.java))
        }

    }

    override fun onResume() {
        super.onResume()

        if (SessionManager(this).isLoggedIn) {
            App.currentEmployee = SessionManager(this).employeeDetails
            if (App.currentEmployee?.id?.isEmpty() == false) {
                progress_view.visibility = View.VISIBLE
                firebaseSigning()
            }
        }


    }

    private fun firebaseSigning() {
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    startActivity(
                        Intent(this, HomeActivity::class.java)
                    )
                    finish()
                    overridePendingTransition(0, 0)
                } else {
                    Utils.showErrorDialog(
                        baseContext, getString(R.string.login_failed), getString(R.string.outh_failed),
                        getString(R.string.retry)
                    ) {}

                    progress_view.visibility = View.GONE
                }
            }
    }
}
