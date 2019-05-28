package flaskoski.rs.smartmuseum.model

import java.io.Serializable
import java.util.*
import android.R.attr.name



class Rating(val user : String ="", val item : String = "", val rating : Float = 0F, val type : String = TYPE_ITEM) : Serializable{
    companion object {
        fun containsRating(ratingsList: ArrayList<Rating>, element: Rating): Boolean {
            return ratingsList.filter { rating ->
                rating.user == element.user && rating.item == element.item && rating.type == element.type
            }.isNotEmpty()
        }
        fun indexOfRating(ratingsList: ArrayList<Rating>, element: Rating): Int {
            return ratingsList.indexOf(ratingsList.filter { rating ->
                rating.user == element.user && rating.item == element.item && rating.type == element.type
            }[0])
        }

        val TYPE_ITEM = "item";
        val TYPE_FEATURE = "feature";
    }

    override fun toString(): String {
        return "$user $item ${rating.toString()}"
    }

    override fun equals(o: Any?): Boolean {
        if (o === this) return true
        if (!(o is Rating)) {
            return false
        }
        val rating : Rating = o as Rating

        return rating.user.equals(user) &&
                rating.item.equals(item) &&
                rating.type.equals(type)
    }

    override fun hashCode(): Int {
        var result = 17
        result = 31 * result + user.hashCode()
        result = 31 * result + item.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

}