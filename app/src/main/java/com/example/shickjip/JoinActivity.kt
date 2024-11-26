package com.example.shickjip

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.shickjip.databinding.ActivityJoinBinding

class JoinActivity : AppCompatActivity() {
    val binding: ActivityJoinBinding by lazy { ActivityJoinBinding.inflate(layoutInflater) }
    private lateinit var requestLauncher: ActivityResultLauncher<Intent>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //memo: 뒤로가기 버튼의 라벨 설정
//        supportActionBar?.title = "뒤로가기"
    }
    /* todo: join 형식 check */
    /*
    private var passwordFlag = false
    private var passwordCheckFlag = false

    fun flagCheck() {
        if (passwordFlag && passwordCheckFlag) {
            binding.joinbtn.isEnabled = true
        } else {
            binding.joinbtn.isEnabled = false
        }
    }
    private val passwordListener = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            if (s != null) {
                when {
                    s.isEmpty() -> {
                        binding.pwdInputLayout.error = "비밀번호를 입력해주세요."
                        passwordFlag = false
                    }
                    !passwordRegex(s.toString()) -> {
                        binding.pwdInputLayout.error = "비밀번호 양식이 일치하지 않습니다."
                        passwordFlag = false
                    }
                    else -> {
                        binding.pwdInputLayout.error = null
                        passwordFlag = true
                    }
                }
                flagCheck()
            }
        }
    }
    fun passwordRegex(password: String): Boolean {
        return password.matches("^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[$@$!%*#?&.])[A-Za-z[0-9]$@$!%*#?&.]{8,16}$".toRegex())
    }
     */

}