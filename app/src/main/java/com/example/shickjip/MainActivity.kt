package com.example.shickjip

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.example.shickjip.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private lateinit var requestLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        //로그인 버튼 클릭 시 -> LoginActivity로 이동
        binding.login.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            Log.d("MainActivity", "Login Button Clicked")
        }
        binding.join.setOnClickListener {
            val intent = Intent(this, JoinActivity::class.java)
            startActivity(intent)
            Log.d("MainActivity", "Join Button Clicked")
        }
    }
}
