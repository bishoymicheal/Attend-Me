package net.corpy.loginlocation.fragments.reports


import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.android.synthetic.main.fragment_admin_reports.*
import net.corpy.loginlocation.DateFormatter
import net.corpy.loginlocation.R
import net.corpy.loginlocation.Utils
import net.corpy.loginlocation.fragments.employees.EmployeesFragment
import net.corpy.loginlocation.language.LocaleManager
import net.corpy.loginlocation.model.Employee
import net.corpy.permissionslib.PermissionsFile
import java.util.*


class AdminReportsFragment : Fragment() {

    private lateinit var functions: FirebaseFunctions

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_admin_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        functions = FirebaseFunctions.getInstance()

        ReportsFragment.startDate.set(Calendar.DAY_OF_MONTH, 1)
        ReportsFragment.endDate.set(
            Calendar.DAY_OF_MONTH,
            Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
        )


        refresh_btn.setOnClickListener {

            progress_view.visibility = View.VISIBLE

            if (!PermissionsFile.StoragePermissionCheck(context)) {
                PermissionsFile.showStorageDialogPermission(
                    context
                ) { hasPermission ->
                    if (hasPermission) {
                        downloadReport()
                    } else {
                        Toast.makeText(context, getString(R.string.storage_permission_reqiest), Toast.LENGTH_LONG)
                            .show()
                    }
                }
            } else {
                downloadReport()
            }


        }



        start_date.text = DateFormatter.getSlashedDay(context!!).format(ReportsFragment.startDate.time)
        end_date.text = DateFormatter.getSlashedDay(context!!).format(ReportsFragment.endDate.time)



        start_date.setOnClickListener { v ->
            val datePickerDialog = DatePickerDialog(
                context!!,
                DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                    Utils.hideKeyboard(context!!, v)
                    start_date.text = LocaleManager.convertDigitsTo(
                        LocaleManager.getLocale(resources).language,
                        getString(R.string.formatted_birth_day, dayOfMonth, month + 1, year)
                    )
                    ReportsFragment.startDate.set(Calendar.YEAR, year)
                    ReportsFragment.startDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    ReportsFragment.startDate.set(Calendar.MONTH, month)
                },
                ReportsFragment.startDate.get(Calendar.YEAR),
                ReportsFragment.startDate.get(Calendar.MONTH),
                ReportsFragment.startDate.get(Calendar.DAY_OF_MONTH)
            )

            datePickerDialog.datePicker.maxDate = ReportsFragment.endDate.timeInMillis


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
                    ReportsFragment.endDate.set(Calendar.YEAR, year)
                    ReportsFragment.endDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    ReportsFragment.endDate.set(Calendar.MONTH, month)
                },
                ReportsFragment.endDate.get(Calendar.YEAR),
                ReportsFragment.endDate.get(Calendar.MONTH),
                ReportsFragment.endDate.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.datePicker.maxDate = Calendar.getInstance().timeInMillis
            datePickerDialog.datePicker.minDate = ReportsFragment.startDate.timeInMillis

            datePickerDialog.show()
        }


        val query = FirebaseFirestore.getInstance().collection("Users").whereLessThanOrEqualTo("type", 0)

        val options = FirestoreRecyclerOptions.Builder<Employee>()
            .setQuery(query, Employee::class.java)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        val adapter = object : FirestoreRecyclerAdapter<Employee, EmployeesFragment.EmployeesViewHolder>(options) {
            override fun onBindViewHolder(
                holder: EmployeesFragment.EmployeesViewHolder,
                position: Int,
                model: Employee
            ) {

                holder.itemView.setOnClickListener {
                    val id = this.snapshots.getSnapshot(position).id
                    model.id = id
                    ReportsFragment.documentId = model
                    it.findNavController()
                        .navigate(R.id.action_adminReportsFragment_to_reportsFragment)
                }

                holder.phoneTxt.text =
                    LocaleManager.convertDigitsTo(LocaleManager.getLocale(resources).language, model.phone)
                holder.titleTxt.text = getString(R.string.employee_name, model.fullName, model.description)
            }

            override fun onCreateViewHolder(group: ViewGroup, i: Int): EmployeesFragment.EmployeesViewHolder {
                return EmployeesFragment.EmployeesViewHolder(
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
            }
        }

        reports_recycler.layoutManager = LinearLayoutManager(context)
        reports_recycler.setHasFixedSize(true)
        reports_recycler.adapter = adapter


    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.reports_fragment_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        if (item?.itemId == R.id.reports_history) {
            view?.findNavController()?.navigate(R.id.action_adminReportsFragment_to_reportsHistoryFragment)
            return true
        }
        return super.onOptionsItemSelected(item)

    }

    private fun downloadReport() {

        val data = hashMapOf(
            "type" to "employees",
            "start" to DateFormatter.getDashedDayFormat().format(ReportsFragment.startDate.time),
            "end" to DateFormatter.getDashedDayFormat().format(ReportsFragment.endDate.time)
        )

        functions.getHttpsCallable("createReport")
            .call(data)
            .continueWith { task ->
                progress_view.visibility = View.GONE

                if (task.isSuccessful) {
                    showDownloadDialog(task.result?.data)
                } else {
                    Utils.showErrorDialog(
                        context!!, getString(R.string.oops),
                        getString(R.string.error_create_report), getString(R.string.retry)
                    ) {}
                }
            }
    }

    @SuppressLint("InflateParams")
    private fun showDownloadDialog(data: Any?) {
        context?.let {

            val url = (data as kotlin.collections.HashMap<*, *>)["file_url"]


            val dialog = AlertDialog.Builder(it)
            val view = LayoutInflater.from(it).inflate(R.layout.download_report_layout, null)
            dialog.setView(view)
            val d = dialog.create()
            val download = view.findViewById<Button>(R.id.confirm_button)
            val close = view.findViewById<Button>(R.id.cancel_button)

            download.setOnClickListener {
                val uri: Uri
                try {
                    uri = Uri.parse(url.toString())
                } catch (e: Exception) {
                    return@setOnClickListener
                }
                val downloadManager = activity?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager?
                val request = DownloadManager.Request(uri)
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)

                val start = DateFormatter.getDashedDayFormat(context!!).format(ReportsFragment.startDate.time)
                val end = DateFormatter.getDashedDayFormat(context!!).format(ReportsFragment.endDate.time)

                request.setTitle(getString(R.string.attendance_report))
                request.setDescription(
                    LocaleManager.convertDigitsTo(
                        LocaleManager.getLocale(resources).language,
                        getString(R.string.employees_attendance_from_to, start, end)
                    )
                )

                request.allowScanningByMediaScanner()
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

//set the local destination for download file to a path within the application's external files directory
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "attendMe")
                request.setMimeType("*/*")
                downloadManager!!.enqueue(request)


                d.dismiss()
            }
            close.setOnClickListener {
                d.dismiss()
            }

            d.show()

        }
    }

}
