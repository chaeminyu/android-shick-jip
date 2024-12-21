data class ThemeItem(
    val id: String,
    val title: String,
    val description: String,
    val imageResId: Int,
    val price: Int,
    var isPurchased: Boolean = false,
    val backgroundColor: Int,
    val treeImageResId: Int,   // 나무 이미지 리소스
    val hillImageResId: Int,   // 바닥 이미지 리소스
    val buttonIconColor: Int,  // 버튼 아이콘 색상
    val progressBarIndicatorColor: Int, // 프로그래스바 indicatorColor
    val progressBarTrackColor: Int,      // 프로그래스바 trackColor
    val textColor: Int, // TextView 텍스트 색상
    val buttonTextColor: Int // MaterialButton 텍스트 색상

)