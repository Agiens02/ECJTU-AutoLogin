package com.lonx.ecjtu.autologin

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class AutoLoginECJTUAPI {

    fun login(studentID: String, passwordECJTU: String, theISP: Int): String {
        if (studentID.isEmpty()) {
            return "E3 您没有填写学号！"
        }
        if (passwordECJTU.isEmpty()) {
            return "E3 您没有填写密码！"
        }
        val strTheISP = when (theISP) {
            1 -> "telecom"
            2 -> "cmcc"
            else -> "unicom"
        }

        Log.d(TAG, "开始创建OkHttpClient对象")
        val client = OkHttpClient.Builder()
            .followRedirects(false)
            .build()

        Log.d(TAG, "开始准备请求头")
        val mediaType = "application/x-www-form-urlencoded".toMediaType()
        val postBody = "DDDDD=%2C0%2C$studentID@$strTheISP&upass=$passwordECJTU&R1=0&R2=0&R3=0&R6=0&para=00&0MKKey=123456&buttonClicked=&redirect_url=&err_flag=&username=&password=&user=&cmd=&Login="
        val request = Request.Builder()
            .url("http://172.16.2.100:801/eportal/?c=ACSetting&a=Login&protocol=http:&hostname=172.16.2.100&iTermType=1&wlanacip=null&wlanacname=null&mac=00-00-00-00-00-00&enAdvert=0&queryACIP=0&loginMethod=1")
            .post(postBody.toRequestBody(mediaType))
            .build()

        Log.d(TAG, "开始发送请求")
        val call = client.newCall(request)
        return try {
            val response = call.execute()
            val headers = response.headers
            val location = headers["Location"]
            if (location != null) {
                if (!location.contains("RetCode=")) {
                    return "登录完成"
                }
                val startIndex = location.indexOf("RetCode=") + 8
                val endIndex = location.indexOf("&", startIndex)
                if (startIndex >= 0 && endIndex >= 0) {
                    return when (val extractedText = location.substring(startIndex, endIndex)) {
                        "userid error1" -> "E3 账号不存在(或未绑定宽带账号或运营商选择有误)"
                        "userid error2" -> "E3 密码错误"
                        "512" -> "E3 AC认证失败(重复登录之类的)"
                        "Rad:Oppp error: Limit Users Err" -> "E3 超出校园网设备数量限制"
                        else -> {
                            Log.d(TAG, "登录未知错误码：$extractedText")
                            "E4 登录失败：\n未知错误"
                        }
                    }
                }
                Log.d(TAG, "错误码为空：$headers")
                "E2 无法解析回包数据：$headers"
            } else {
                Log.d(TAG, "登录返回头找不到重定向字段：$headers")
                "E1 无法解析回包数据：$headers"
            }
        } catch (e: IOException) {
            Log.d(TAG, "登录时捕获到异常：$e")
            "E0 发送登录请求失败，捕获到异常：$e"
        }
    }

    fun getState(): Int {
        Log.d(TAG, "开始创建OkHttpClient对象")
        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .build()

        Log.d(TAG, "开始准备请求头")
        val request = Request.Builder()
            .url("http://172.16.2.100")
            .get()
            .build()

        Log.d(TAG, "开始发送请求")
        val call = client.newCall(request)
        return try {
            val response = call.execute()
            if (response.code == 200) {
                Log.d(TAG, "get请求成功")
                val responseBody = response.body?.string() ?: ""
                if (responseBody.contains("<title>注销页</title>")) {
                    4
                } else {
                    3
                }
            } else {
                Log.d(TAG, "奇怪的http状态码：${response.code}\n${response.headers}\n${response.body?.string()}")
                2
            }
        } catch (e: IOException) {
            when (e) {
                is SocketTimeoutException -> 2
                is ConnectException -> 1
                else -> {
                    Log.d(TAG, "奇怪的异常捕获：$e")
                    2
                }
            }
        }
    }

    companion object {
        private const val TAG = "AutoLoginECJTUAPI"
    }
}
