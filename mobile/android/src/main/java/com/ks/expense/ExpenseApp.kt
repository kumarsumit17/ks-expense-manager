package com.ks.expense

import android.app.Application
import android.os.StrictMode
import com.ks.expense.data.ExpenseDatabase
import com.ks.expense.data.ExpenseStore
import kotlin.concurrent.thread

class ExpenseApp : Application() {
    val store: ExpenseStore by lazy { ExpenseStore(this, ExpenseDatabase.build(this)) }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            // Debug builds only: report disk or network work on the main thread and leaked files.
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectLeakedClosableObjects().detectLeakedSqlLiteObjects().penaltyLog().build())
        }
        // Open the database while the first frame is drawn, so the month's data is ready sooner.
        thread(name = "open-database") { runCatching { store.database.openHelper.writableDatabase } }
    }
}
