package com.lonx.ecjtu.autologin


import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var editor: SharedPreferences.Editor
    private val accountKey = "学号"
    private val passwordKey = "密码"
    private val ispKey = "运营商"
    private val ispOptions = arrayOf("中国电信", "中国移动", "中国联通")

    private lateinit var etAccount: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var rgISP: Spinner

    private var accountValue_p: String? = null
    private var passwordValue_p: String? = null
    private var ispValue_p: Int = 0

    override fun onResume() {
        super.onResume()
        val sharedPreferences = getSharedPreferences("userInformation", MODE_PRIVATE)
        accountValue_p = sharedPreferences.getString(accountKey, "")
        passwordValue_p = sharedPreferences.getString(passwordKey, "")
        if (accountValue_p != null && passwordValue_p != null) {
            Toast.makeText(this, "自动登录中", Toast.LENGTH_SHORT).show()
            findViewById<Button>(R.id.button_login).performClick()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPreferences = getSharedPreferences("userInformation", MODE_PRIVATE)
        editor = sharedPreferences.edit()

        etAccount = findViewById(R.id.editText_account)
        etPassword = findViewById(R.id.editText_password)
        rgISP = findViewById(R.id.dropdown)

        accountValue_p = sharedPreferences.getString(accountKey, "")
        etAccount.setText(accountValue_p)

        passwordValue_p = sharedPreferences.getString(passwordKey, "")
        etPassword.setText(passwordValue_p)

        ispValue_p = sharedPreferences.getInt(ispKey, 1)
        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ispOptions)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        rgISP.adapter = arrayAdapter
        rgISP.setSelection(ispValue_p - 1)
        rgISP.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {}
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

//        val autoRun = !accountValue_p.isNullOrEmpty() && !passwordValue_p.isNullOrEmpty()
//        if (autoRun) {
//            Toast.makeText(this, "自动登录中", Toast.LENGTH_SHORT).show()
//            findViewById<Button>(R.id.button_login).performClick()
//        }
    }

    private fun getISPSelect(): Int {
        return when (rgISP.selectedItemPosition) {
            0 -> 1
            1 -> 2
            else -> 3
        }
    }

    fun buttonSave(view: View) {
        saveUserInformation()
    }

    private fun saveUserInformation() {
        saveUserInformation(true)
    }

    private fun saveUserInformation(showUI: Boolean) {
        val accountValue = etAccount.text.toString()
        val passwordValue = etPassword.text.toString()
        val ispValue = getISPSelect()

        var isChange = false
        if (accountValue_p != accountValue) {
            accountValue_p = accountValue
            editor.putString(accountKey, accountValue)
            isChange = true
        }
        if (passwordValue_p != passwordValue) {
            passwordValue_p = passwordValue
            editor.putString(passwordKey, passwordValue)
            isChange = true
        }
        if (ispValue_p != ispValue) {
            ispValue_p = ispValue
            editor.putInt(ispKey, ispValue)
            isChange = true
        }
        if (isChange) {
            editor.commit()
        }
        if (showUI) {
            Toast.makeText(this, "信息已保存", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.type == ConnectivityManager.TYPE_WIFI
    }

    fun doLogin(view: View) {
        val loginButton = view as Button
        loginButton.isEnabled = false

        if (!isWifiConnected(this)) {
            showAlertDialog("提示", "检测到您没有连接wifi\n如果您的手机显示已经连接了wifi，您可能需要在登录前关闭移动数据开关")
            loginButton.isEnabled = true
            return
        }

        if (etAccount.text.toString().isEmpty()) {
            showAlertDialog("提示", "您没有填写学号！")
            loginButton.isEnabled = true
            return
        }

        if (etPassword.text.toString().isEmpty()) {
            showAlertDialog("提示", "您没有填写密码！")
            loginButton.isEnabled = true
            return
        }

        val ecjtuApi = AutoLoginECJTUAPI()
        val ispValue = getISPSelect()

        Thread {
            val state = ecjtuApi.getState()
            val rstTxt = when (state) {
                1 -> "您似乎没有网络连接"
                3 -> ecjtuApi.login(etAccount.text.toString(), etPassword.text.toString(), ispValue)
                4 -> "您已经处于登录状态"
                else -> "您连接的wifi似乎不是校园网"
            }

            Handler(Looper.getMainLooper()).post {
                if (rstTxt.startsWith("E")) {
                    val title = if (rstTxt.startsWith("E3")) "登录失败！" else "失败惹..."
                    showAlertDialog(title, if (rstTxt.startsWith("E3")) rstTxt.substring(3) else rstTxt)
                } else {
                    showAlertDialog("提示", rstTxt)
                    saveUserInformation(false)
                }
                loginButton.isEnabled = true
            }
        }.start()
    }

    private fun showAlertDialog(title: String, message: String) {
        AlertDialog.Builder(this).apply {
            setTitle(title)
            setMessage(message)
            setPositiveButton("确定", null)
            show()
        }
    }
}

