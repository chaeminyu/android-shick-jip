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
            val email = binding.email.text.toString()
            val password = binding.pwd.text.toString()
            if (password.length < 6) {
                Toast.makeText(this, "비밀번호는 최소 6자 이상이어야 합니다", Toast.LENGTH_SHORT).show()
            } else {
                loginUser(email, password)
            }

        }

    }
    private fun loginUser(email: String, password: String) { //xml에서 id를 가져온다.
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // 로그인 성공 시
                    val user = auth.currentUser
                    Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show() //로그인 성공이라는 메세지를 띄운다.
                    // Add your logic to navigate to another activity or perform other actions
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()//로그인 성공시 뒤로가기 금지
                } else {
                    // 로그인 실패 시
                    Toast.makeText(this, "로그인 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}