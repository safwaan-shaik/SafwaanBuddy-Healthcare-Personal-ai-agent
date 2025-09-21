package com.nurturepk.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.nurturepk.data.ReminderDatabase
import com.nurturepk.data.ReminderRepository
import com.nurturepk.utils.ReminderScheduler

/**
 * BroadcastReceiver that handles device boot completion
 * Reschedules all active reminders after device restart
 */
class BootCompleteReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                rescheduleReminders(context)
            }
        }
    }
    
    /**
     * Reschedules all active reminders after boot
     */
    private fun rescheduleReminders(context: Context) {
        // Use coroutine scope for database operations
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = ReminderDatabase.getDatabase(context)
                val repository = ReminderRepository(database.reminderDao())
                val reminderScheduler = ReminderScheduler(context)
                
                // Get all active reminders from database
                val activeReminders = repository.getUpcomingReminders(
                    kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                )
                
                // Reschedule all active reminders
                reminderScheduler.rescheduleAllReminders(activeReminders)
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}