package com.project.review.notifications

import android.app.*
import android.content.Context
import android.util.Log
import androidx.work.*
import com.project.review.settings.Settings
import java.util.concurrent.TimeUnit

/**
 * create channels for notifications
 * also instantiates periodic notifications
 */
class Channels : Application() {

    companion object {
        const val DATA_SYNC_CHANNEL_ID: Int = 1

        /**
         * is used to schedule a notification to the user that shows a product to which he may be interested in seeing the reviews         *
         *
         * @see NotificationWorker
         */
        fun scheduleNotification(
            context: Context,
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            /*val notificationRequest: WorkRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        Settings.NOTIFICATION_ID to 1
                    )
                ).setConstraints(constraints).build()

            WorkManager.getInstance(context).enqueue(notificationRequest)*/

            val repeatingRequest =
                PeriodicWorkRequestBuilder<NotificationWorker>(
                    2,
                    TimeUnit.DAYS, // repeatInterval
                    PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS,
                    TimeUnit.MILLISECONDS // flexInterval
                ).setInputData(
                    workDataOf(
                        Settings.NOTIFICATION_ID to 1
                    )
                ).setInitialDelay(2, TimeUnit.DAYS).setConstraints(constraints).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "app",
                ExistingPeriodicWorkPolicy.KEEP,
                repeatingRequest
            )

            Log.d("Channels", "SCHEDULED")


        }
    }

    override fun onCreate() {
        super.onCreate()
        initChannels()
    }

    private fun initChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            DATA_SYNC_CHANNEL_ID.toString(), "Marketplace Data Sync",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
        Log.d("Channels", "CHANNELS CREATED")

    }
}