package com.example.shickjip

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.example.shickjip.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    val binding: ActivityLoginBinding by lazy { ActivityLoginBinding.inflate(layoutInflater) }
    private lateinit var requestLauncher: ActivityResultLauncher<Intent>
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //memo: 뒤로가기 버튼의 라벨 설정 : 따옴표안에넣으면댐
        supportActionBar?.title = ""
        auth = FirebaseAuth.getInstance() //firebase에서 인스턴스를 가져온다.


        // Login 버튼 클릭 시 로그인하기
        binding.loginbtn.setOnClickListener {
            loginUser()

        }

    }
    private fun loginUser() { //xml에서 id를 가져온다
        val email = binding.email.text.toString()
        val password = binding.pwd.text.toString()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // 로그인 성공 시
                    val user = auth.currentUser
                    Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show() //로그인 성공이라는 메세지를 띄운다.
                    // Add your logic to navigate to another activity or perform other actions
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                } else {
                    // 로그인 실패 시
                    Toast.makeText(this, "로그인 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}