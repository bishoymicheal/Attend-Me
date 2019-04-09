package net.corpy.loginlocation.fragments.locations


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
import kotlinx.android.synthetic.main.locations_fragment.*
import net.corpy.loginlocation.R
import net.corpy.loginlocation.language.LocaleManager
import net.corpy.loginlocation.model.Location

class LocationsFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.locations_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val query = FirebaseFirestore.getInstance()
            .collection("Locations")

        val options = FirestoreRecyclerOptions.Builder<Location>()
            .setQuery(query, Location::class.java)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        val adapter = object : FirestoreRecyclerAdapter<Location, LocationsViewModel>(options) {
            override fun onBindViewHolder(holder: LocationsViewModel, position: Int, model: Location) {
                holder.titleTxt.text = model.title
                holder.locationTxt.text =
                    LocaleManager.convertDigitsTo(
                        LocaleManager.getLocale(holder.itemView.resources).language,
                        getString(R.string.current_location, model.lat.toString(), model.lng.toString())
                    )

                holder.itemView.setOnClickListener {
                    LocationDetails.location = model
                    it.findNavController().navigate(R.id.action_locationsFragment_to_locationDetails)
                }

            }

            override fun onCreateViewHolder(group: ViewGroup, i: Int): LocationsViewModel {
                return LocationsViewModel(
                    LayoutInflater.from(group.context)
                        .inflate(R.layout.location_list_item, group, false)
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
        locations_list.layoutManager = LinearLayoutManager(context)
        locations_list.setHasFixedSize(true)
        locations_list.adapter = adapter


    }

    class LocationsViewModel(view: View) : RecyclerView.ViewHolder(view) {
        val titleTxt: TextView = view.findViewById(R.id.title_txt)
        val locationTxt: TextView = view.findViewById(R.id.location_txt)
    }

}
