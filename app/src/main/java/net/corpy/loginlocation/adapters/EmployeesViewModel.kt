package net.corpy.loginlocation.adapters

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import net.corpy.loginlocation.R

class EmployeesViewModel(view: View) : RecyclerView.ViewHolder(view) {
    val total_time = view.findViewById<TextView>(R.id.total_time)
    val employee_name = view.findViewById<TextView>(R.id.day_number)
    val checkin_txt = view.findViewById<TextView>(R.id.checkin_txt)
    val checkout_txt = view.findViewById<TextView>(R.id.checkout_txt)
    val info_view = view.findViewById<ConstraintLayout>(R.id.info_view)
    val tools_view = view.findViewById<ConstraintLayout>(R.id.tools_view)
    val arrow_image = view.findViewById<ImageView>(R.id.arrow_image)


    fun resetData() {
        total_time.text = itemView.context.getString(R.string.default_hours_diff)
        checkin_txt.text = itemView.context.getString(R.string.default_time)
        checkout_txt.text = itemView.context.getString(R.string.default_time)
        employee_name.text = ""
    }

}