package net.corpy.loginlocation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import net.corpy.loginlocation.DateFormatter
import net.corpy.loginlocation.R
import net.corpy.loginlocation.fragments.home.AdminDashboardFragment
import net.corpy.loginlocation.language.LocaleManager
import net.corpy.loginlocation.model.Employee


private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Employee>() {
    override fun areItemsTheSame(oldItem: Employee, newItem: Employee): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Employee, newItem: Employee): Boolean {
        return oldItem == newItem
    }
}

class EmployeesAdapter : ListAdapter<Employee, EmployeesViewModel>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmployeesViewModel {
        return EmployeesViewModel(
            LayoutInflater.from(parent.context).inflate(
                R.layout.employee_item_admin_dashboard,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: EmployeesViewModel, position: Int) {
        holder.resetData()
        holder.employee_name.text = holder.itemView.context.getString(
            R.string.employee_name,
            getItem(position).fullName,
            getItem(position).description
        )

        handleCheckingText(holder, getItem(position))

        holder.info_view.setOnClickListener {
            //            toggleToolsView(holder)
        }

        holder.arrow_image.visibility = View.INVISIBLE
    }

    private var listener = ArrayList<ListenerRegistration>()

    fun updateList() {
        removeListeners()
        notifyDataSetChanged()
    }

    fun removeListeners() {
        listener.forEach {
            it.remove()
        }
    }

    private fun handleCheckingText(holder: EmployeesViewModel, employee: Employee) {

        val day = DateFormatter.getDashedDayFormat().format(AdminDashboardFragment.currentCalendar.time)

        listener.add(FirebaseFirestore.getInstance().document(
            "Users/${employee.id}/attendance/$day"
        ).addSnapshotListener { documentSnapshot, fireBaseFireStoreException ->

            if (fireBaseFireStoreException != null) {
                holder.checkin_txt?.text = holder.itemView.context.getString(R.string.default_time)
                holder.checkout_txt?.text = holder.itemView.context.getString(R.string.default_time)
                return@addSnapshotListener
            }

            if (documentSnapshot?.exists() == true) {
                val haveIn = documentSnapshot.data?.contains("in")
                val haveOut = documentSnapshot.data?.contains("out")

                if (haveIn == true) {
                    val timestamp = documentSnapshot.getTimestamp("in")
                    holder.checkin_txt?.text =
                        DateFormatter.getTimeFormat(holder.itemView.context).format(timestamp?.toDate())
                }
                if (haveOut == true) {
                    val timestamp = documentSnapshot.getTimestamp("out")
                    holder.checkout_txt?.text =
                        DateFormatter.getTimeFormat(holder.itemView.context).format(timestamp?.toDate())
                }

                if (haveIn == true && haveOut == true) {
                    if (!holder.checkin_txt.text.isEmpty() && holder.checkin_txt.text != holder.itemView.context.getString(
                            R.string.default_time
                        )
                        && !holder.checkout_txt.text.isEmpty() && holder.checkout_txt.text != holder.itemView.context.getString(
                            R.string.default_time
                        )
                    ) {
                        try {
                            val checkinTime =
                                DateFormatter.getTimeFormat(holder.itemView.context)
                                    .parse(holder.checkin_txt?.text.toString())
                            val checkoutTime =
                                DateFormatter.getTimeFormat(holder.itemView.context)
                                    .parse(holder.checkout_txt?.text.toString())
                            val difference = checkoutTime.time - checkinTime.time
                            val days = (difference / (1000 * 60 * 60 * 24)).toInt()
                            var hours = ((difference - 1000 * 60 * 60 * 24 * days) / (1000 * 60 * 60)).toInt()
                            val min =
                                (difference - 1000 * 60 * 60 * 24 * days - 1000 * 60 * 60 * hours).toInt() / (1000 * 60)
                            hours = if (hours < 0) -hours else hours

                            var hour = "h"

                            if (LocaleManager.getLocale(holder.itemView.resources).language == "ar") {
                                hour = "س"
                            }


                            holder.total_time.text =
                                holder.itemView.context.getString(R.string.diff_time, hours, min) + " " + hour
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } else {
                holder.checkin_txt.text = holder.itemView.context.getString(
                    R.string.default_time
                )
                holder.checkout_txt.text = holder.itemView.context.getString(
                    R.string.default_time
                )
            }
        })
    }
}