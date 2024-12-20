package com.example.shickjip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment

class PaymentDialog(
    private val currentCoins: Int,
    private val price: Int,
    private val onConfirm: () -> Unit
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 다이얼로그 레이아웃을 설정
        return inflater.inflate(R.layout.dialog_payment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 다이얼로그 UI 요소 바인딩
        val dialogMessage = view.findViewById<TextView>(R.id.dialogMessage)
        val cancelButton = view.findViewById<TextView>(R.id.cancelButton)
        val confirmButton = view.findViewById<TextView>(R.id.confirmButton)

        // 메시지 설정
        dialogMessage.text = "보유하신 ${currentCoins} 코인 중 ${price} 코인이 차감됩니다!"

        // 취소 버튼 클릭 리스너
        cancelButton.setOnClickListener {
            dismiss()
        }

        // 결제하기 버튼 클릭 리스너
        confirmButton.setOnClickListener {
            onConfirm()
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}