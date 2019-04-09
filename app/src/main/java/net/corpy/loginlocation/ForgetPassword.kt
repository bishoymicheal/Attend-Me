package net.corpy.loginlocation

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.android.synthetic.main.activity_forget_password.*

class ForgetPassword : AppCompatActivity() {
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forget_password)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        reset_btn.setOnClickListener {

            if (email_et?.text?.toString().isNullOrEmpty()) {
                email_et_layout.isErrorEnabled = true
                email_et_layout.error = getString(R.string.enter_your_email)
                return@setOnClickListener
            } else {
                email_et_layout.isErrorEnabled = false
            }

            val data = hashMapOf(
                "email" to email_et?.text?.toString()
            )
            progress_view.visibility = View.VISIBLE
            FirebaseFunctions.getInstance()
                .getHttpsCallable("resetPassword")
                .call(data)
                .addOnSuccessListener { result ->
                    progress_view.visibility = View.GONE
                    if (result?.data == true) {
                        Toast.makeText(this, getString(R.string.email_sent), Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this, getString(R.string.error_sending_email), Toast.LENGTH_LONG).show()
                    }
                }.addOnFailureListener {
                    progress_view.visibility = View.GONE
                    Toast.makeText(this, getString(R.string.error_sending_email), Toast.LENGTH_LONG).show()
                }


        }

    }
}
