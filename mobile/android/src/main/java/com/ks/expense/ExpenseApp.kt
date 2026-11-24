package com.ks.expense

import android.app.Application
import com.ks.expense.data.ExpenseDatabase
import com.ks.expense.data.ExpenseStore

class ExpenseApp : Application() {
    val store: ExpenseStore by lazy { ExpenseStore(this, ExpenseDatabase.build(this)) }
}
