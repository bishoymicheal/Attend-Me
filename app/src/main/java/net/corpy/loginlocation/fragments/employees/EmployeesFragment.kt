package net.corpy.loginlocation.fragments.employees


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.android.synthetic.main.employees_fragment.*
import net.corpy.loginlocation.R
import net.corpy.loginlocation.language.LocaleManager
import net.corpy.loginlocation.model.Employee


class EmployeesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.employees_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        add_employee_txt.setOnClickListener {
            it.findNavController().navigate(R.id.action_employees_fragment_to_addEmployee)
        }

        val query = FirebaseFirestore.getInstance().collection("Users")

        val options = FirestoreRecyclerOptions.Builder<Employee>()
            .setQuery(query, Employee::class.java)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        val adapter = object : FirestoreRecyclerAdapter<Employee, EmployeesViewHolder>(options) {
            override fun onBindViewHolder(holder: EmployeesViewHolder, position: Int, model: Employee) {


                holder.itemView.setOnClickListener {
                    EmployeeDetails.employee = model
                    it.findNavController().navigate(R.id.action_employees_fragment_to_employeesDetails)
                }

                holder.phoneTxt.text = LocaleManager.convertDigitsTo(
                    LocaleManager.getLocale(resources).language,
                    model.phone
                )
                holder.titleTxt.text = getString(R.string.employee_name, model.fullName, model.description)
            }

            override fun onCreateViewHolder(group: ViewGroup, i: Int): EmployeesViewHolder {
                return EmployeesViewHolder(
                    LayoutInflater.from(group.context)
                        .inflate(R.layout.employee_list_item, group, false)
                )
            }

            override fun onDataChanged() {
                if (this.itemCount == 0) {
                    no_items_view?.visibility = View.VISIBLE
                } else {
                    no_items_view?.visibility = View.GONE
                }
            }

            override fun onError(e: FirebaseFirestoreException) {
                // Called when there is an error getting a query snapshot. You may want to update
                // your UI to display an error message to the user.
                // ...
            }
        }

        employees_list.layoutManager = LinearLayoutManager(context)
        employees_list.setHasFixedSize(true)
        employees_list.adapter = adapter

    }

    class EmployeesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val phoneTxt: TextView = view.findViewById(net.corpy.loginlocation.R.id.phone_txt)
        val titleTxt: TextView = view.findViewById(net.corpy.loginlocation.R.id.title_txt)
    }

}
