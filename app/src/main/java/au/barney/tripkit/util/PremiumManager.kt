package au.barney.tripkit.util

import android.content.Context
import android.os.Build

object PremiumManager {
    const val MASTER_ITEM_LIMIT = 30
    const val LIST_LIMIT = 1
    const val TEMPLATE_LIMIT = 2

    fun isPremium(context: Context): Boolean {
        return try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
            installer != "com.android.vending"
        } catch (e: Exception) {
            true
        }
    }

    fun canAddMasterItem(context: Context, currentCount: Int): Boolean {
        return isPremium(context) || currentCount < MASTER_ITEM_LIMIT
    }

    fun canAddList(context: Context, currentCount: Int): Boolean {
        return isPremium(context) || currentCount < LIST_LIMIT
    }

    fun canAddTemplate(context: Context, currentCount: Int): Boolean {
        return isPremium(context) || currentCount < TEMPLATE_LIMIT
    }
}
