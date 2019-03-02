package flaskoski.rs.smartmuseum

import android.content.Context
import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import flaskoski.rs.smartmuseum.model.Item
import kotlinx.android.synthetic.main.grid_item.view.*
import net.librec.recommender.Recommender
import net.librec.recommender.cf.UserKNNRecommender

class ItemsGridListAdapter(private val itemsList: List<Item>, private val context: Context, var recommender: Recommender) : RecyclerView.Adapter<ItemsGridListAdapter.ItemViewHolder>() {


    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){

    }
    override fun onBindViewHolder(p0: ItemsGridListAdapter.ItemViewHolder, p1: Int) {
        p0.itemView.itemName.text = itemsList.get(p1).title
        //p0.itemView.ratingBar.rating = itemsList.get(p1).avgRating

        val userIndex = (recommender as UserKNNRecommender).userMappingData.get("Felipe")
        val itemIndex = (recommender as UserKNNRecommender).itemMappingData.get(itemsList.get(p1).id)?.toInt()
        if(userIndex != null && itemIndex != null)
            p0.itemView.ratingBar.rating = (recommender as UserKNNRecommender).predict(userIndex, itemIndex).toFloat()
        else p0.itemView.ratingBar.rating = 0F

        p0.itemView.setOnClickListener{
            val viewItemDetails = Intent(context, ItemDetailActivity::class.java)
            viewItemDetails.putExtra("itemHash", itemsList.get(p1).id)

            context.startActivity(viewItemDetails)
        }
    }

    override fun onCreateViewHolder(p0: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.grid_item, p0, false)
//        view.img_star1.setImageResource(android.R.drawable.btn_star_big_off)
//        view.img_star2.setImageResource(android.R.drawable.btn_star_big_off)
//        view.img_star3.setImageResource(android.R.drawable.btn_star_big_off)
//        view.img_star4.setImageResource(android.R.drawable.btn_star_big_off)
//        view.img_star5.setImageResource(android.R.drawable.btn_star_big_off)
        view.img_itemThumb.setImageResource(R.mipmap.image_not_found)

        return ItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return itemsList.size
    }

}

