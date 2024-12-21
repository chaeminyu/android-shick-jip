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

    private var selectedPosition = -1 // 클릭된 항목의 위치를 저장

    // ViewHolder 정의
    class FriendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.friendsProfile)
        val name: TextView = view.findViewById(R.id.friendName)
        val rootView: View = view // 전체 뷰에 접근하여 배경을 변경할 수 있도록
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friends_recyclerview, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friends[position]
        holder.name.text = friend.name

        // 프로필 이미지 로드
        Glide.with(holder.itemView.context)
            .load(friend.profileImage.takeIf { it.isNotEmpty() } ?: R.drawable.ic_profile_default) // URL이 비어 있지 않으면 로드
            .error(R.drawable.profile_placeholder) // 로드 실패 시 기본 이미지
            .into(holder.profileImage)

        // 이전에 선택된 항목의 배경을 초기화
        if (position == selectedPosition) {
            holder.rootView.setBackgroundResource(R.drawable.friendprofile_selected_background) // 클릭된 항목 배경 변경
        } else {
            // 아무 배경도 설정하지 않음 (비어있게 둠)
            holder.rootView.background = null
        }

        // 친구 클릭 시 배경 변경 및 클릭된 위치 업데이트
        holder.itemView.setOnClickListener {
            // 이전 선택된 항목의 배경을 기본값으로 변경
            notifyItemChanged(selectedPosition)

            // 클릭된 항목의 배경을 변경하고, 선택된 항목 업데이트
            selectedPosition = position
            notifyItemChanged(position)

            // 클릭된 친구에 대한 추가 작업
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