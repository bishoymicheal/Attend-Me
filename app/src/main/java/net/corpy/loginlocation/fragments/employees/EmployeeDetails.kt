package net.corpy.loginlocation.fragments.employees


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.fragment_employee_details.*
import net.corpy.loginlocation.HomeActivity
import net.corpy.loginlocation.R
import net.corpy.loginlocation.language.LocaleManager
import net.corpy.loginlocation.model.Employee

class EmployeeDetails : Fragment() {
    companion object {
        var employee: Employee? = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_employee_details, container, false)
    }

    override fun onResume() {
        super.onResume()


        (activity as HomeActivity).toolbar.title = getString(R.string.employee_detail_name, employee?.fullName)

        employee_name.text = employee?.fullName
        employee_description.text = employee?.description
        employee_email.text = employee?.email
        phone_number.text = LocaleManager.convertDigitsTo(
            LocaleManager.getLocale(resources).language,
            employee?.phone
        )

        val gender = when (employee?.gender) {
            1 -> getString(R.string.gender_male)
            else -> getString(R.string.gender_female)
        }

        gender_txt.text = gender
        date_of_birth_txt.text = LocaleManager.convertDigitsTo(
            LocaleManager.getLocale(resources).language,
            getString(R.string.employee_detail_birth_date, employee?.birthDate)
        )


    }
}
