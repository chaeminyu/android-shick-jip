package com.example.shickjip

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.example.shickjip.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    val binding: ActivityLoginBinding by lazy { ActivityLoginBinding.inflate(layoutInflater) }
    private lateinit var requestLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //memo: 뒤로가기 버튼의 라벨 설정 : 따옴표안에넣으면댐
        supportActionBar?.title = ""


        // Login 버튼 클릭 시 홈으로 이동
        binding.loginbtn.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

    }

}