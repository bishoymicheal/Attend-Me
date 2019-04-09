package net.corpy.loginlocation.fragments.home


import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.android.synthetic.main.fragment_admin_dashboard.*
import net.corpy.loginlocation.DateFormatter
import net.corpy.loginlocation.R
import net.corpy.loginlocation.adapters.EmployeesAdapter
import net.corpy.loginlocation.language.LocaleManager
import net.corpy.loginlocation.model.ADMIN
import net.corpy.loginlocation.model.Employee
import net.corpy.loginlocation.model.MANAGER
import java.util.*
import kotlin.collections.ArrayList


class AdminDashboardFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()

    private val infoRef = db.collection("Info")
    private val usersRef = db.collection("Users")


    private var listener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false)
    }

    private var adapter: EmployeesAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progress_view?.visibility = View.VISIBLE
        no_items_view?.visibility = View.GONE

        adapter = EmployeesAdapter()

        employees_list?.layoutManager = LinearLayoutManager(context)
        employees_list?.adapter = adapter


        usersRef.addSnapshotListener { querySnapshot, _ ->
            progress_view?.visibility = View.GONE
            if (querySnapshot?.isEmpty != null && !querySnapshot.isEmpty) {

                val list = ArrayList<Employee>()
                querySnapshot.documents.forEach {
                    if (it.toObject(Employee::class.java)?.type == ADMIN
                        || it.toObject(Employee::class.java)?.type == MANAGER
                    ) return@forEach

                    val employee = it.toObject(Employee::class.java)
                    employee?.id = it.id
                    list.add(employee!!)
                }

                if (list.isEmpty()) {
                    total_employess?.text =
                        LocaleManager.convertDigitsTo(LocaleManager.getLocale(resources).language, 0.toString())

                    no_items_view?.visibility = View.VISIBLE
                } else {
                    total_employess?.text =
                        LocaleManager.convertDigitsTo(LocaleManager.getLocale(resources).language, list.size.toString())
                    no_items_view?.visibility = View.GONE
                }

                adapter?.submitList(list)


            } else {
                no_items_view?.visibility = View.VISIBLE
            }
        }

        current_day?.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                context!!,
                DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                    currentCalendar.set(Calendar.YEAR, year)
                    currentCalendar.set(Calendar.MONTH, month)
                    currentCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    current_day?.text = DateFormatter.getSlashedDay(context!!).format(currentCalendar.time)
                    registerListener()
                    adapter?.updateList()
                },
                currentCalendar.get(Calendar.YEAR),
                currentCalendar.get(Calendar.MONTH),
                currentCalendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.datePicker.maxDate = Calendar.getInstance().timeInMillis

            datePickerDialog.show()
        }

        refresh_btn.setOnClickListener {
            adapter?.updateList()
            registerListener()
            current_day.text = DateFormatter.getSlashedDay(context!!).format(currentCalendar.time)
        }
    }

    override fun onResume() {
        super.onResume()
//        App.setCurrentAdapter {
//            activity?.runOnUiThread {
//                adapter?.updateList()
//
//            }
//        }
        registerListener()
        current_day.text = DateFormatter.getSlashedDay(context!!).format(currentCalendar.time)
    }

    override fun onPause() {
        super.onPause()
        listener?.remove()
        adapter?.removeListeners()
    }

    private fun registerListener() {
        listener?.remove()
        listener = infoRef.document(DateFormatter.getDashedDayFormat().format(currentCalendar.time))
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val count = snapshot.data?.get("count")

                    if (LocaleManager.getLocale(resources).language == "ar")
                        current_attendance_txt?.text = LocaleManager.convertToArabicDigits("$count")
                    else
                        current_attendance_txt?.text = LocaleManager.convertToEnglishDigits("$count")

                } else {
                    if (LocaleManager.getLocale(resources).language == "ar")
                        current_attendance_txt?.text = LocaleManager.convertToArabicDigits("0")
                    else
                        current_attendance_txt?.text = LocaleManager.convertToEnglishDigits("0")
                }
            }
    }

    companion object {
        val currentCalendar: Calendar = Calendar.getInstance()
    }


}
