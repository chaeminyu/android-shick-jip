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
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

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
        FirebaseApp.initializeApp(this)

        auth = FirebaseAuth.getInstance() //Firebase에서 인스턴스를 가져올 것이다!
        firestore = FirebaseFirestore.getInstance()

        // 회원가입 버튼 클릭 시 로그인 화면으로 넘어감
        binding.joinbtn.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val username = binding.nickname.text.toString()
        val email = binding.email.text.toString()
        val password = binding.pwd.text.toString()

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "모든 필드를 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = task.result?.user?.uid // FirebaseAuth로 생성된 UID 가져오기
                    if (uid != null) {
                        saveUserData(uid, username, email) // UID와 함께 Firestore에 사용자 정보 저장
                    }
                    Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity()
                } else {
                    Toast.makeText(this, "회원가입 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    Log.e("JoinActivity", "회원가입 실패", task.exception)
                }
            }
    }

    private fun saveUserData(uid: String, username: String, email: String) {
        Log.d("JoinActivity", "saveUserData called with UID: $uid, username: $username, email: $email")
        val user = hashMapOf( // 사용자 정보를 해시맵으로 구성
            "uid" to uid,
            "username" to username,
            "email" to email
        )

        // Firestore에 UID를 문서 ID로 사용하여 저장
        firestore.collection("users")
            .document(uid) // UID를 문서 ID로 지정
            .set(user)
            .addOnSuccessListener {
                Log.d("JoinActivity", "사용자 데이터가 Firestore에 성공적으로 저장되었습니다.")
            }
            .addOnFailureListener { e ->
                Log.e("JoinActivity", "Firestore 데이터 저장 실패", e)
            }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java) //로그인 화면으로 돌아가게 하는 intent 이용!
        startActivity(intent)
        finish()  // 현재 액티비티를 종료하여 뒤로가기 버튼으로 다시 돌아오지 않도록 한다.
    }
}