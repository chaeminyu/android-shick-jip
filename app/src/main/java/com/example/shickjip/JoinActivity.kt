package com.example.shickjip

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.shickjip.databinding.ActivityJoinBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class JoinActivity : AppCompatActivity() {
    val binding: ActivityJoinBinding by lazy { ActivityJoinBinding.inflate(layoutInflater) }
    private lateinit var requestLauncher: ActivityResultLauncher<Intent>
    private lateinit var auth: FirebaseAuth //Firebase를 사용하는 권한
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //memo: 뒤로가기 버튼의 라벨 설정 : 따옴표안에넣으면댐
        supportActionBar?.title = ""

        auth = FirebaseAuth.getInstance() //Firebase에서 인스턴스를 가져올 것이다!
        firestore = FirebaseFirestore.getInstance()

        // 회원가입 버튼 클릭 시 로그인 화면으로 넘어감
        binding.joinbtn.setOnClickListener {
            registerUser()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            Log.d("JoinActivity", "go to LoginActivity")
        }
    }

    private fun registerUser() { //xml에 있는 id의 이름을 가져와서 객체로 생성
        val username = binding.nickname.text.toString()
        val email = binding.email.text.toString()
        val password = binding.pwd.text.toString()

        auth.createUserWithEmailAndPassword(email, password) //firebase 권한으로 email, password를 만든다.
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    // Firestore에 사용자 세부 정보 저장
                    saveUserData(username, email)
                    // 회원가입 성공 메시지 표시
                    Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT).show() //Toast는 아래에 메세지를 띄워줍니다.
                    // 메인 액티비티로 이동
                    navigateToMainActivity()
                } else {
                    // 회원가입 실패 시 사용자에게 메시지 표시
                    Toast.makeText(this, "회원가입 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
    private fun saveUserData(username: String, email: String) { //firebase에 저장
        val user = hashMapOf( //해시맵으로 username, email 필드에 저장
            "username" to username,
            "email" to email
        )

        // 생성된 ID로 새 문서 추가
        firestore.collection("users") //여기서! 컬렉션 이름과 같아야합니다
            .add(user)
            .addOnSuccessListener { documentReference ->
                Log.d("RegisterActivity", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.e("RegisterActivity", "문서 추가 오류", e)
            }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java) //로그인 화면으로 돌아가게 하는 intent 이용!
        startActivity(intent)
        finish()  // 현재 액티비티를 종료하여 뒤로가기 버튼으로 다시 돌아오지 않도록 한다.
    }
}