package net.corpy.loginlocation.fragments.reports


import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.reports_fragment.*
import net.corpy.loginlocation.*
import net.corpy.loginlocation.language.LocaleManager
import net.corpy.loginlocation.model.Attendance
import net.corpy.loginlocation.model.Employee
import net.corpy.loginlocation.model.NORMAL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class ReportsFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.reports_fragment, container, false)
    }


    companion object {
        var documentId: Employee? = App.currentEmployee
        var startDate: Calendar = Calendar.getInstance()
        var endDate: Calendar = Calendar.getInstance()
    }


    private lateinit var query: Query
    private lateinit var adapter: MyAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (App.currentEmployee?.type == NORMAL) {
            startDate.set(Calendar.DAY_OF_MONTH, 1)
            endDate.set(Calendar.DAY_OF_MONTH, Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH))
        }

        start_date.text = DateFormatter.getSlashedDay(context!!).format(startDate.time)
        end_date.text = DateFormatter.getSlashedDay(context!!).format(endDate.time)


        refresh_btn.setOnClickListener {
            refreshQuery()
        }

        start_date.setOnClickListener { v ->
            val datePickerDialog = DatePickerDialog(
                context!!,
                DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                    Utils.hideKeyboard(context!!, v)
                    start_date.text = LocaleManager.convertDigitsTo(
                        LocaleManager.getLocale(resources).language,
                        getString(R.string.formatted_birth_day, dayOfMonth, month + 1, year)
                    )
                    startDate.set(Calendar.YEAR, year)
                    startDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    startDate.set(Calendar.MONTH, month)
                    refreshQuery()

                },
                startDate.get(Calendar.YEAR),
                startDate.get(Calendar.MONTH),
                startDate.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        end_date.setOnClickListener { v ->
            val datePickerDialog = DatePickerDialog(
                context!!,
                DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                    Utils.hideKeyboard(context!!, v)
                    end_date.text = LocaleManager.convertDigitsTo(
                        LocaleManager.getLocale(resources).language,
                        getString(R.string.formatted_birth_day, dayOfMonth, month + 1, year)
                    )
                    endDate.set(Calendar.YEAR, year)
                    endDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    endDate.set(Calendar.MONTH, month)
                    refreshQuery()

                },
                endDate.get(Calendar.YEAR),
                endDate.get(Calendar.MONTH),
                endDate.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }



        if (documentId == null) activity?.finish()

        adapter = MyAdapter()

        reports_recycler.layoutManager = LinearLayoutManager(context)
        reports_recycler.setHasFixedSize(true)
        reports_recycler.adapter = adapter

        refreshQuery()
    }

    override fun onResume() {
        super.onResume()

        if (documentId == App.currentEmployee) {
            (activity as HomeActivity).toolbar.title = getString(R.string.attendance_records)
        } else
            (activity as HomeActivity).toolbar.title = getString(R.string.user_records, documentId?.fullName)

    }

    class MyAdapter(var items: ArrayList<Attendance> = ArrayList()) : RecyclerView.Adapter<AttendancesViewModel>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendancesViewModel {
            return AttendancesViewModel(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.employee_attendance_item,
                    parent,
                    false
                )
            )
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onBindViewHolder(holder: AttendancesViewModel, position: Int) {
            holder.resetData()
            val dayFormat = SimpleDateFormat("dd/MM/yyyy", LocaleManager.getLocale(holder.itemView.resources))
            val timeFormat =
                SimpleDateFormat("hh:mm:ss a", LocaleManager.getLocale(holder.itemView.context.resources))

            if (items[position].inTime != null) {
                val inTime = timeFormat.format(items[position].inTime?.toDate())
                val day = dayFormat.format(items[position].inTime?.toDate())
                holder.dayNumber.text = day
                holder.checkinTxt.text = inTime
            }
            if (items[position].onTime != null) {
                val outTime = timeFormat.format(items[position].onTime?.toDate())
                holder.checkoutTxt.text = outTime

            }

            if (!holder.checkinTxt.text.isEmpty() && holder.checkinTxt.text != holder.itemView.context.getString(R.string.default_time)
                && !holder.checkoutTxt.text.isEmpty() && holder.checkoutTxt.text != holder.itemView.context.getString(
                    R.string.default_time
                )
            ) {
                try {
                    val checkingTime =
                        DateFormatter.getTimeFormat(holder.itemView.context).parse(holder.checkinTxt.text.toString())
                    val checkoutTime =
                        DateFormatter.getTimeFormat(holder.itemView.context).parse(holder.checkoutTxt.text.toString())

                    val difference = checkoutTime.time - checkingTime.time
                    val days = (difference / (1000 * 60 * 60 * 24)).toInt()
                    var hours = ((difference - 1000 * 60 * 60 * 24 * days) / (1000 * 60 * 60)).toInt()
                    val min = (difference - 1000 * 60 * 60 * 24 * days - 1000 * 60 * 60 * hours).toInt() / (1000 * 60)
                    hours = if (hours < 0) -hours else hours

                    var hour = "h"

                    if (LocaleManager.getLocale(holder.itemView.resources).language == "ar") {
                        hour = "س"
                    }

                    holder.totalTime.text =
                        holder.itemView.context.getString(R.string.diff_time, hours, min) + " " + hour
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

        }

    }

    private fun refreshQuery() {
        progress_view.visibility = View.VISIBLE
        val startStamp = Timestamp(startDate.time)
        val endStamp = Timestamp(endDate.time)

        query = db.collection("Users/${documentId?.id}/attendance")
            .orderBy("in", Query.Direction.ASCENDING)
            .whereGreaterThanOrEqualTo("in", startStamp)
            .whereLessThanOrEqualTo("in", endStamp)

        query.get().addOnCompleteListener {
            progress_view.visibility = View.GONE
            if (it.isSuccessful) {
                val list = ArrayList<Attendance>()
                it.result?.documents?.forEach { document ->
                    var inR: Timestamp? = null
                    if (document.contains("in")) {
                        inR = document.getTimestamp("in")
                    }
                    var outR: Timestamp? = null
                    if (document.contains("out")) {
                        outR = document.getTimestamp("out")
                    }
                    val att = Attendance(inR, outR)
                    list.add(att)
                }
                adapter.items = list
                adapter.notifyDataSetChanged()
            }
        }
    }

    class AttendancesViewModel(view: View) : RecyclerView.ViewHolder(view) {
        val dayNumber: TextView = view.findViewById(R.id.day_number)
        val checkinTxt: TextView = view.findViewById(R.id.checkin_txt)
        val checkoutTxt: TextView = view.findViewById(R.id.checkout_txt)
        val totalTime: TextView = view.findViewById(R.id.total_time)


        fun resetData() {
            totalTime.text = itemView.context.getString(R.string.default_hours_diff)
            checkinTxt.text = itemView.context.getString(R.string.default_time)
            checkoutTxt.text = itemView.context.getString(R.string.default_time)
        }


    }

}
