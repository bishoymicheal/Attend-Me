package net.corpy.loginlocation.fragments.reports


import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.fragment_reports_history.*
import net.corpy.loginlocation.App
import net.corpy.loginlocation.R
import net.corpy.loginlocation.Utils
import net.corpy.loginlocation.language.LocaleManager
import net.corpy.loginlocation.model.Report


class ReportsHistoryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reports_history, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val query = FirebaseFirestore.getInstance()
            .collection("reports")

        val options = FirestoreRecyclerOptions.Builder<Report>()
            .setQuery(query, Report::class.java)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        val adapter = object : FirestoreRecyclerAdapter<Report, ReportViewModel>(options) {
            override fun onBindViewHolder(holder: ReportViewModel, position: Int, model: Report) {
                holder.titleTxt.text = model.fileName
                holder.downloadUrlTxt.text = model.downloadUrl

                holder.downloadReport.setOnClickListener {
                    val uri: Uri
                    try {
                        uri = Uri.parse(model.downloadUrl)
                    } catch (e: Exception) {
                        return@setOnClickListener
                    }
                    val downloadManager = activity?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager?
                    val request = DownloadManager.Request(uri)
                    request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)


                    request.setTitle(getString(net.corpy.loginlocation.R.string.attendance_report))
                    request.setDescription(
                        LocaleManager.convertDigitsTo(
                            LocaleManager.getLocale(resources).language,
                            model.fileName
                        )
                    )

                    request.allowScanningByMediaScanner()
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                    request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS + "/attendMe",
                        uri.lastPathSegment
                    )
                    request.setMimeType("*/*")
                    val id = downloadManager?.enqueue(request)
                    id?.let {
                        App.downloadList.add(id)
                    }
                    Toast.makeText(
                        context,
                        getString(net.corpy.loginlocation.R.string.file_start_download),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                val docId = this.snapshots.getSnapshot(position).id
                holder.deleteReport.setOnClickListener {
                    Utils.showWarningDialog(
                        holder.itemView.context,
                        getString(net.corpy.loginlocation.R.string.remove_report),
                        getString(net.corpy.loginlocation.R.string.delete_confirm),
                        true
                    ) {
                        progress_view.visibility = View.VISIBLE
                        FirebaseStorage.getInstance().getReferenceFromUrl(model.downloadUrl).delete()
                            .addOnCompleteListener { storeDelete ->
                                if (storeDelete.isSuccessful) {
                                    FirebaseFirestore.getInstance().collection("reports").document(docId).delete()
                                        .addOnCompleteListener {
                                            if (it.isSuccessful) {
                                                progress_view.visibility = View.GONE

                                                Utils.showSuccessDialog(
                                                    holder.itemView.context,
                                                    getString(net.corpy.loginlocation.R.string.remove_report),
                                                    getString(net.corpy.loginlocation.R.string.report_removed_success)
                                                ) {}
                                            } else {
                                                progress_view.visibility = View.GONE

                                                Utils.showErrorDialog(
                                                    holder.itemView.context,
                                                    getString(net.corpy.loginlocation.R.string.remove_report),
                                                    getString(net.corpy.loginlocation.R.string.error_remove_report),
                                                    getString(net.corpy.loginlocation.R.string.retry)
                                                ) {}
                                            }
                                        }
                                } else {
                                    progress_view.visibility = View.GONE

                                    Utils.showErrorDialog(
                                        holder.itemView.context,
                                        getString(net.corpy.loginlocation.R.string.remove_report),
                                        getString(net.corpy.loginlocation.R.string.error_remove_report),
                                        getString(net.corpy.loginlocation.R.string.retry)
                                    ) {}
                                }
                            }


                    }
                }

            }

            override fun onCreateViewHolder(group: ViewGroup, i: Int): ReportViewModel {
                return ReportViewModel(
                    LayoutInflater.from(group.context)
                        .inflate(net.corpy.loginlocation.R.layout.history_list_item, group, false)
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
        reports_list.layoutManager = LinearLayoutManager(context)
        reports_list.setHasFixedSize(true)
        reports_list.adapter = adapter


    }


    class ReportViewModel(view: View) : RecyclerView.ViewHolder(view) {
        val titleTxt: TextView = view.findViewById(net.corpy.loginlocation.R.id.title_txt)
        val downloadUrlTxt: TextView = view.findViewById(net.corpy.loginlocation.R.id.download_url_txt)
        val downloadReport: ImageView = view.findViewById(net.corpy.loginlocation.R.id.download_report)
        val deleteReport: ImageView = view.findViewById(net.corpy.loginlocation.R.id.delete_report)
    }

}
