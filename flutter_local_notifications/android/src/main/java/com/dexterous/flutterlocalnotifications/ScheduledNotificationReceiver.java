package com.dexterous.flutterlocalnotifications;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.core.app.NotificationManagerCompat;

import com.dexterous.flutterlocalnotifications.models.NotificationDetails;
import com.dexterous.flutterlocalnotifications.utils.StringUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.HashMap;

/** Created by michaelbui on 24/3/18. */
@Keep
public class ScheduledNotificationReceiver extends BroadcastReceiver {

  private static final String TAG = "ScheduledNotifReceiver";

  @Override
  @SuppressWarnings("deprecation")
  public void onReceive(final Context context, Intent intent) {
    String notificationDetailsJson =
        intent.getStringExtra(FlutterLocalNotificationsPlugin.NOTIFICATION_DETAILS);
    if (StringUtils.isNullOrEmpty(notificationDetailsJson)) {
      // This logic is needed for apps that used the plugin prior to 0.3.4

      Notification notification;
      int notificationId = intent.getIntExtra("notification_id", 0);

      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        notification = intent.getParcelableExtra("notification", Notification.class);
      } else {
        notification = intent.getParcelableExtra("notification");
      }

      if (notification == null) {
        // This means the notification is corrupt
        FlutterLocalNotificationsPlugin.removeNotificationFromCache(context, notificationId);
        Log.e(TAG, "Failed to parse a notification from  Intent. ID: " + notificationId);
        return;
      }

      notification.when = System.currentTimeMillis();
      NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
      notificationManager.notify(notificationId, notification);
      boolean repeat = intent.getBooleanExtra("repeat", false);
      if (!repeat) {
        FlutterLocalNotificationsPlugin.removeNotificationFromCache(context, notificationId);
      }

     

    } else {
      Gson gson = FlutterLocalNotificationsPlugin.buildGson();
      Type type = new TypeToken<NotificationDetails>() {}.getType();
      NotificationDetails notificationDetails = gson.fromJson(notificationDetailsJson, type);

      // NotificationDetails 객체에 'deliveredAt' 필드 추가 및 설정
      long deliveredAt = System.currentTimeMillis();
      notificationDetails.deliveredAt = deliveredAt;
      // System.out.println("ScheduledNotificationReceiver onReceive - details.deliveredAt: " + notificationDetails.deliveredAt);

      // ⭐️ [핵심 수정] deliveredAt을 payload JSON에 통합
      String currentPayload = notificationDetails.payload;
      try {
          // 1. 기존 payload를 Map으로 변환
          Map<String, Object> payloadMap = new Gson().fromJson(currentPayload, new TypeToken<Map<String, Object>>(){}.getType());
          
          // 2. deliveredAt 추가
          payloadMap.put("deliveredAt", deliveredAt); 
          
          // 3. Map을 다시 JSON 문자열로 변환하여 payload에 재설정
          notificationDetails.payload = new Gson().toJson(payloadMap);
          
      } catch (Exception e) {
          Log.e(TAG, "Failed to update payload with deliveredAt: " + e.getMessage());
          // 오류 발생 시, deliveredAt 값만 포함하는 JSON으로 payload를 대체하는 등 오류 처리 필요
      }

      FlutterLocalNotificationsPlugin.showNotification(context, notificationDetails);
      FlutterLocalNotificationsPlugin.scheduleNextNotification(context, notificationDetails);

    }
  }
}
