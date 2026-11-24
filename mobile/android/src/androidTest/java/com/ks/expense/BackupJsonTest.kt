package com.ks.expense

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ks.expense.backup.BackupData
import com.ks.expense.backup.BackupJson
import com.ks.expense.data.Category
import com.ks.expense.data.Expense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** org.json is part of Android, so the backup file is tested on the device. */
@RunWith(AndroidJUnit4::class)
class BackupJsonTest {
    private val data = BackupData(
        listOf(Category(1, "food", null, "🍽️", false, 0), Category(2, null, "चाय, \"special\"", "☕", true, 1)),
        listOf(
            Expense(1, 25_000, "INR", 1, "Lunch\nwith friends", 1_700_000_000_000, 1_700_000_000_001),
            Expense(2, 1_000_000_000_000_000, "USD", 2, "", 5, 6),
        ),
    )

    @Test fun roundTrip() {
        assertEquals(data, BackupJson.read(BackupJson.write(data, 123)))
    }

    @Test fun refusesOtherFilesAndNewerVersions() {
        val good = BackupJson.write(data, 123)
        listOf(
            "",
            "not json",
            "{}",
            "[]",
            good.replace("\"ks-expense-backup\"", "\"something-else\""),
            good.replace("\"version\": 1", "\"version\": 2"),
            good.replace("\"amountMinor\": 25000", "\"amountMinor\": 250.5"),
            good.replace("\"amountMinor\": 25000", "\"amountMinor\": \"25000\""),
            good.replace("\"categoryId\": 2", "\"categoryId\": 3"),
            good.substring(0, good.length / 2),
        ).forEach { assertNull(it.take(60), BackupJson.read(it)) }
    }
}
