import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.shickjip.R
import com.example.shickjip.models.Friend

class FriendsAdapter(
    private val friends: MutableList<Friend>, // Firestore 데이터
    private val onFriendClick: (Friend) -> Unit // 클릭 이벤트 처리
) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

    // ViewHolder 정의
    class FriendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.friendsProfile)
        val name: TextView = view.findViewById(R.id.friendName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friends_recyclerview, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friends[position]
        holder.name.text = friend.name

        Glide.with(holder.itemView.context)
            .load(friend.profileImage.takeIf { it.isNotEmpty() } ?: R.drawable.ic_profile_default) // URL이 비어 있지 않으면 로드
            .error(R.drawable.profile_placeholder) // 로드 실패 시 기본 이미지
            .into(holder.profileImage)

        holder.itemView.setOnClickListener {
            onFriendClick(friend)
        }
    }

    override fun getItemCount(): Int = friends.size

    // Firestore 데이터 업데이트
    fun updateFriends(newFriends: List<Friend>) {
        friends.clear()
        friends.addAll(newFriends)
        notifyDataSetChanged() // RecyclerView 새로고침
    }
}