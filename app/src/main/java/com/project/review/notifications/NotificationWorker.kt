package com.project.review.notifications

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.project.review.MainActivity
import com.project.review.R
import com.project.review.models.RelatedProduct
import com.project.review.repositories.DatabaseRepository
import com.project.review.repositories.NetworkRepository
import com.project.review.settings.Settings

/**
 obtains the list of suggested products notifiable to the user, eventually downloads the image,
 and notifies the user
 */
class NotificationWorker(
    appContext: Context,
    workerParams:
    WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {


    private lateinit var list: MutableList<RelatedProduct>
    private val databaseRepository = DatabaseRepository(applicationContext)

    override suspend fun doWork(): Result {
        val myProcess = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(myProcess)

        /*  var isInBackground: Boolean =
              myProcess.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND*/

        list =
            databaseRepository.suspendGetScheduledProducts()

        val relatedProducts =
            databaseRepository.suspendGetRelatedProducts(
                100)

        relatedProducts.removeAll(list)
        if (relatedProducts.isNotEmpty()) {
            scheduleNotification(relatedProducts[0])
        }

        return Result.success()
    }

    private suspend fun scheduleNotification(product: RelatedProduct) {


        databaseRepository.insertRelatedProduct(product.apply { scheduledForNotification = true })


        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var productUri: Uri? = null
        var bitmap: Bitmap? = null

        if (!product.imageUrl.isNullOrEmpty()) {
            productUri =
                NetworkRepository().saveImage(applicationContext, product.imageUrl, true)
            if (productUri != null) {
                val bmOptions = BitmapFactory.Options()
                val tempBitmap = BitmapFactory.decodeFile(productUri.toString(), bmOptions)
                bitmap = Bitmap.createScaledBitmap(tempBitmap, 2000, 2000, true)
            }

        }

        val parent = databaseRepository.getProductByCode(product.parentCode!!)
        val title: String = applicationContext.getString(R.string.from) + " " + parent.name
        val content: String = product.name
        val code: String = product.code
        val notificationId = inputData.getInt(Settings.NOTIFICATION_ID, 0)


        val builder =
            NotificationCompat.Builder(applicationContext,
                Channels.DATA_SYNC_CHANNEL_ID.toString())
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .apply {
                    if (productUri != null)
                        setStyle(NotificationCompat.BigPictureStyle()
                            .bigPicture(bitmap)
                            .bigLargeIcon(null))
                }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra(Settings.CODE, code)
        }
        val activity = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
        builder.setContentIntent(activity)

        val notification: Notification = builder.build()

        notificationManager.notify(notificationId, notification)
    }
}