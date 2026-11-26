package com.ks.expense.budgets

/** How far spending has come against a monthly limit. */
enum class BudgetLevel { OK, NEAR, OVER }

/** [spentMinor] against [limitMinor], both in the same currency. */
data class BudgetStatus(val spentMinor: Long, val limitMinor: Long) {
    init {
        require(limitMinor > 0) { "limit must be positive" }
    }

    /** 0 and up; 1 means the limit is reached. */
    val fraction: Float get() = (spentMinor.toDouble() / limitMinor).toFloat()

    /** Amber from 80 %, red over 100 %. */
    val level: BudgetLevel
        get() = when {
            spentMinor > limitMinor -> BudgetLevel.OVER
            spentMinor * 5 >= limitMinor * 4 -> BudgetLevel.NEAR
            else -> BudgetLevel.OK
        }

    /** What is left; negative when over. */
    val leftMinor: Long get() = Math.subtractExact(limitMinor, spentMinor)

    /** Whole percent for reading out (rounded down, so 99.6 % is not "100 %" before the limit). */
    val percent: Int get() = (spentMinor.toDouble() * 100 / limitMinor).toInt()
}
