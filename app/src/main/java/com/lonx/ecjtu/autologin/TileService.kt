package com.lonx.ecjtu.autologin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

   class TileService : TileService() {
       private var notificationTitle = ""
       private var notificationMessage = ""

       private fun createNotificationChannel() {
           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
               val name = "ECJTUAutoLoginChannel"
               val descriptionText = "Channel for ECJTUAutoLogin notifications"
               val importance = NotificationManager.IMPORTANCE_DEFAULT
               val channel = NotificationChannel("autologin_channel", name, importance).apply {
                   description = descriptionText
               }
               val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
               notificationManager.createNotificationChannel(channel)
           }
       }

       private fun doLogin(studentid: String, password: String, theISP: Int) {
           CoroutineScope(Dispatchers.IO).launch {
               val state = AutoLoginECJTUAPI.getState()
               when (state) {
                   1 -> {  //无网络
                       notificationTitle = "无网络"
                       notificationMessage = "请检查网络连接"
                   }
                   2 -> {  //未知错误
                       notificationTitle = "未知错误"
                       notificationMessage = "请检查网络连接"
                   }
                   3 -> {  //未登录
                       if (studentid == "" || password == "") {
                           notificationTitle = "账号/密码为空"
                           notificationMessage = "请检查账号/密码是否填写并保存"
                       } else {
                           try {
                               val result = AutoLoginECJTUAPI.login(studentid, password, theISP)
                               if (result.startsWith("E")) {
                                   notificationTitle = "登录失败"
                                   notificationMessage = result.substring(3)
                               } else {
                                   notificationTitle = "登录成功"
                                   notificationMessage = result
                               }
                           } catch (e: Exception) {
                               e.printStackTrace()
                           }
                       }
                   }
                   4 -> {  //已登录
                       notificationTitle = "已登录"
                       notificationMessage = "您已登录校园网"
                   }
                   else -> {
                       notificationTitle = "未知错误"
                       notificationMessage = "请检查网络连接"
                   }
               }
               withContext(Dispatchers.Main) {
                   sendNotification(notificationTitle, notificationMessage)
               }
           }
       }

       override fun onClick() {
           super.onClick()
           createNotificationChannel()

           val sharedPreferences by lazy { getSharedPreferences("userInformation", MODE_PRIVATE) }
           val studentid = sharedPreferences.getString("student_id", "")
           val password = sharedPreferences.getString("student_psd", "")
           val theISP = sharedPreferences.getInt("isp", 0)
           doLogin(studentid!!, password!!, theISP)
       }

       private fun sendNotification(title: String, message: String) {
           val intent = Intent(this, MainActivity::class.java).apply {
               flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
           }
           val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

           val builder = NotificationCompat.Builder(this, "autologin_channel")
               .setSmallIcon(R.drawable.tile_icon)
               .setContentTitle(title)
               .setShowWhen(false)
               .setContentText(message)
               .setPriority(NotificationCompat.PRIORITY_DEFAULT)
               .setContentIntent(pendingIntent)
               .setAutoCancel(true)

           with(getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager) {
               notify(1, builder.build())
           }
       }
   }
