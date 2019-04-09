package net.corpy.loginlocation.fragments.addEmployee


import android.app.DatePickerDialog
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.android.synthetic.main.add_empolyee_fragment.*
import net.corpy.loginlocation.EMAIL_KEY
import net.corpy.loginlocation.R
import net.corpy.loginlocation.Utils
import net.corpy.loginlocation.model.Employee
import java.util.*


class AddEmployee : Fragment() {


    private val db = FirebaseFirestore.getInstance()
    private val usersRef = db.collection("Users")


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.add_empolyee_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        automatic_password_ck.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                auto_password_txt.visibility = View.VISIBLE
                password_et_layout.visibility = View.GONE
                password_confirm_et_layout.visibility = View.GONE

            } else {
                auto_password_txt.visibility = View.GONE
                password_et_layout.visibility = View.VISIBLE
                password_confirm_et_layout.visibility = View.VISIBLE
            }
        }

        birth_date_et.setOnTouchListener { v, event ->

            if (event.action == MotionEvent.ACTION_DOWN)
                context?.let {
                    val newCalendar = Calendar.getInstance()
                    val datePickerDialog = DatePickerDialog(
                        it,
                        DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                            Utils.hideKeyboard(context!!, v)
                            birth_date_et.setText(
                                getString(
                                    R.string.formatted_birth_day,
                                    year,
                                    (month + 1),
                                    dayOfMonth
                                )
                            )
                        },
                        newCalendar.get(Calendar.YEAR),
                        newCalendar.get(Calendar.MONTH),
                        newCalendar.get(Calendar.DAY_OF_MONTH)
                    )
                    datePickerDialog.show()
                }
            return@setOnTouchListener true

        }


        ok_btn.setOnClickListener {
            progress_view.visibility = View.VISIBLE
            if (validateInputs()) {
                createNewUser(it)
            } else {
                progress_view.visibility = View.GONE
            }
        }
    }


    private fun createNewUser(view: View) {
        val email = email_et.text.toString()
        val phone = phone_et.text.toString()
        val fullName = fullname_et.text.toString()
        val gender = gender_spinner.selectedItemPosition
        val type = employees_types.selectedItemPosition
        val description = description_et.text.toString()

        val password = if (automatic_password_ck.isChecked) email else password_et.text.toString()

        emailIsRegisteredBefore(email) {
            if (it) {
                progress_view.visibility = View.GONE
                email_et_layout.isErrorEnabled = true
                email_et_layout.error = getString(R.string.email_already_exist)
            } else {
                email_et_layout.isErrorEnabled = false
                val employee = Employee(
                    email,
                    phone,
                    fullName,
                    gender,
                    description,
                    birth_date_et.text.toString(),
                    password,
                    type
                )

                usersRef.document().set(employee).addOnSuccessListener {
                    progress_view.visibility = View.GONE
                    Utils.showSuccessDialog(
                        context!!, getString(R.string.adding_employee),
                        getString(R.string.adding_employee_success), false
                    ) {
                        view.findNavController().navigateUp()
                    }
                }.addOnFailureListener {
                    progress_view.visibility = View.GONE
                    Utils.showErrorDialog(
                        context!!, getString(R.string.adding_employee),
                        getString(R.string.error_adding_employee), getString(R.string.retry)
                    ) {

                    }
                }
            }

        }


    }

    private fun emailIsRegisteredBefore(email: String, onResult: (Boolean) -> Unit) {
        FirebaseFirestore.getInstance().collection("Users")
            .whereEqualTo(EMAIL_KEY, email)
            .get(Source.SERVER).addOnCompleteListener {
                if (it.isSuccessful) {
                    if (it.result?.documents?.isEmpty() == true) {
                        onResult(false)
                    } else {
                        onResult(true)
                    }
                } else {
                    onResult(true)
                }
            }
    }

    private fun validateInputs(): Boolean {
        if (TextUtils.isEmpty(fullname_et.text.toString())) {
            fullname_et_layout.isErrorEnabled = true
            fullname_et_layout.error = getString(R.string.not_valid_input)
            return false
        } else {
            fullname_et_layout.isErrorEnabled = false
        }

        if (!Utils.isValidEmail(email_et.text.toString())) {
            email_et_layout.isErrorEnabled = true
            email_et_layout.error = getString(R.string.not_valid_email)
            return false
        } else {
            email_et_layout.isErrorEnabled = false
        }

        if (TextUtils.isEmpty(phone_et.text.toString())) {
            phone_et_layout.isErrorEnabled = true
            phone_et_layout.error = getString(R.string.not_valid_input)
            return false
        } else {
            phone_et_layout.isErrorEnabled = false
        }
        if (gender_spinner.selectedItemPosition == 0) {
            Toast.makeText(context, getString(R.string.please_select_gender), Toast.LENGTH_SHORT).show()
            return false
        }
        if (TextUtils.isEmpty(description_et.text.toString())) {
            description_et_layout.isErrorEnabled = true
            description_et_layout.error = getString(R.string.not_valid_input)
            return false
        } else {
            description_et_layout.isErrorEnabled = false
        }

        if (TextUtils.isEmpty(birth_date_et.text.toString())) {
            birth_date_et_layout.isErrorEnabled = true
            birth_date_et_layout.error = getString(R.string.enter_employee_age)
            return false
        } else {
            birth_date_et_layout.isErrorEnabled = false
        }



        if (!automatic_password_ck.isChecked) {
            if (TextUtils.isEmpty(password_et.text.toString())) {
                password_et_layout.isErrorEnabled = true
                password_et_layout.error = getString(R.string.not_valid_input)
                return false
            } else {
                password_et_layout.isErrorEnabled = false
            }

            if (TextUtils.isEmpty(password_confirm_et.text.toString())) {
                password_confirm_et_layout.isErrorEnabled = true
                password_confirm_et_layout.error = getString(R.string.not_valid_input)
                return false
            } else {
                password_confirm_et_layout.isErrorEnabled = false
            }

            if (password_et.text.toString() != password_confirm_et.text.toString()) {
                password_confirm_et_layout.isErrorEnabled = true
                password_confirm_et_layout.error = getString(R.string.password_not_match)
                return false
            } else {
                password_confirm_et_layout.isErrorEnabled = false
            }

        }

        return true
    }


}
