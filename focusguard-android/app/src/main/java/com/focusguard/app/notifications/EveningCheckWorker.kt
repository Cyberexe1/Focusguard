package com.focusguard.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.focusguard.app.data.api.CheckInRequest
import com.focusguard.app.data.local.SessionDataStore
import com.focusguard.app.di.NetworkModule
import kotlinx.coroutines.flow.first

/**
 * Runs at 9 PM via EveningAlarmReceiver.
 * 1. Checks if user has checked in today via GET /tasks/checkin/status
 * 2. If NOT checked in → shows "you haven't updated" notification
 * 3. Also triggers a Twilio accountability call via POST /calls/trigger/{taskId}
 *    for the highest-priority active task
 */
class EveningCheckWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val dataStore = SessionDataStore(context)
            val session = dataStore.currentSession() ?: return Result.success() // not logged in, skip

            val moshi = com.squareup.moshi.Moshi.Builder()
                .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                .build()
            val retrofit = retrofit2.Retrofit.Builder()
                .baseUrl(com.focusguard.app.BuildConfig.API_BASE_URL + "/")
                .client(okhttp3.OkHttpClient())
                .addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create(moshi))
                .build()
            val api = retrofit.create(com.focusguard.app.data.api.FocusGuardApiService::class.java)
            val bearer = "Bearer ${session.accessToken}"

            // 1. Check if already checked in today
            val statusResp = api.checkInStatus(bearer)
            if (statusResp.isSuccessful) {
                val status = statusResp.body()
                if (status?.checkedInToday == true) {
                    // User already updated — cancel the alert, nothing to do
                    return Result.success()
                }
            }

            // 2. Not checked in — show the push notification
            EveningAlertNotifier.show(context)

            // 3. Also trigger an accountability call for the highest-risk active task
            val tasksResp = api.getTasks(bearer)
            if (tasksResp.isSuccessful) {
                val tasks = tasksResp.body()
                val topTask = tasks
                    ?.filter { it.status != "completed" }
                    ?.maxByOrNull { it.priorityScore }
                if (topTask != null) {
                    try {
                        api.triggerCall(bearer, topTask.taskId)
                    } catch (e: Exception) {
                        // Call trigger failure is non-fatal — notification already shown
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            // Network failure — still show the local notification as a fallback
            EveningAlertNotifier.show(context)
            Result.success()
        }
    }

    companion object {
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<EveningCheckWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
