package com.lonx.ecjtu.autologin

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private val sharedPreferences by lazy { getSharedPreferences("userInformation", MODE_PRIVATE) }
    private val accountKey = "student_id"
    private val passwordKey = "student_psd"
    private val ispKey = "isp"
    private val isAutoLogin = "auto_login_status"
    private val ispOptions = arrayOf("中国电信", "中国移动", "中国联通")
    private var autoLogin = true

    private lateinit var etAccount: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var rgISP: Spinner
    private lateinit var switchMaterial: SwitchMaterial

    override fun onResume() {
        super.onResume()
        if (autoLogin) {
            val account = sharedPreferences.getString(accountKey, "")
            val password = sharedPreferences.getString(passwordKey, "")
            if (!account.isNullOrEmpty() && !password.isNullOrEmpty()) {
                Toast.makeText(this, "自动登录中", Toast.LENGTH_SHORT).show()
                findViewById<Button>(R.id.button_login).performClick()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etAccount = findViewById(R.id.editText_account)
        etPassword = findViewById(R.id.editText_password)
        rgISP = findViewById(R.id.dropdown)
        switchMaterial = findViewById(R.id.switch_auto_login)

        etAccount.setText(sharedPreferences.getString(accountKey, ""))
        etPassword.setText(sharedPreferences.getString(passwordKey, ""))
        autoLogin = sharedPreferences.getBoolean(isAutoLogin, true)
        switchMaterial.isChecked = autoLogin

        rgISP.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ispOptions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        rgISP.setSelection(sharedPreferences.getInt(ispKey, 1) - 1)

        switchMaterial.setOnCheckedChangeListener { _, isChecked ->
            autoLogin = isChecked
            sharedPreferences.edit().putBoolean(isAutoLogin, isChecked).apply()
        }
    }

    private fun getISPSelect(): Int = rgISP.selectedItemPosition + 1

    fun buttonSave(view: View) {
        saveUserInformation()
    }

    private fun saveUserInformation() {
        val accountValue = etAccount.text.toString()
        val passwordValue = etPassword.text.toString()
        val ispValue = getISPSelect()

        with(sharedPreferences.edit()) {
            putString(accountKey, accountValue)
            putString(passwordKey, passwordValue)
            putInt(ispKey, ispValue)
            apply()
        }
        Toast.makeText(this, "账号已保存", Toast.LENGTH_SHORT).show()
    }

    private fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun doLogin(view: View) {
        val loginButton = view as Button
        setLoginButtonEnabled(loginButton, false)

        if (!isWifiConnected(this)) {
            showAlertDialog("提示", "检测到您没有连接wifi，如果您的手机显示已经连接了wifi，您可能需要在登录前关闭移动数据开关")
            setLoginButtonEnabled(loginButton, true)
            return
        }

        val account = etAccount.text.toString()
        val password = etPassword.text.toString()
        if (account.isEmpty() || password.isEmpty()) {
            showAlertDialog("提示", "学号或密码不能为空！")
            setLoginButtonEnabled(loginButton, true)
            return
        }

        lifecycleScope.launch {
            val state = withContext(Dispatchers.IO) { AutoLoginECJTUAPI().getState() }
            val rstTxt = when (state) {
                1 -> "您似乎没有网络连接"
                3 -> withContext(Dispatchers.IO) {
                    AutoLoginECJTUAPI().login(account, password, getISPSelect())
                }
                4 -> "您已经处于登录状态"
                else -> "您连接的wifi似乎不是校园网"
            }

            withContext(Dispatchers.Main) {
                handleLoginResult(rstTxt, loginButton)
            }
        }
    }

    private fun handleLoginResult(rstTxt: String, loginButton: Button) {
        if (rstTxt.startsWith("E")) {
            val title = if (rstTxt.startsWith("E3")) "登录失败！" else "失败惹..."
            showAlertDialog(title, if (rstTxt.startsWith("E3")) rstTxt.substring(3) else rstTxt)
        } else {
            showAlertDialog("提示", rstTxt)
        }
        setLoginButtonEnabled(loginButton, true)
    }

    private fun setLoginButtonEnabled(button: Button, isEnabled: Boolean) {
        button.isEnabled = isEnabled
    }

    private fun showAlertDialog(title: String, message: String) {
        AlertDialog.Builder(this).apply {
            setTitle(title)
            setMessage(message)
            setPositiveButton("确定") { _, _ -> }
            show()
        }
    }
}
