package net.corpy.loginlocation

import android.content.Context
import net.corpy.loginlocation.language.LocaleManager
import java.text.SimpleDateFormat
import java.util.*

object DateFormatter {

    fun getDashedDayFormat(context: Context) =
        SimpleDateFormat("dd-MM-yyyy", LocaleManager.getLocale(context.resources))

    fun getDashedDayFormat() =
        SimpleDateFormat("dd-MM-yyyy", Locale.US)

    fun getSlashedDay(context: Context) =
        SimpleDateFormat("dd/MM/yyyy", LocaleManager.getLocale(context.resources))

    fun getSlashedDay() =
        SimpleDateFormat("dd/MM/yyyy", Locale.US)

    fun getTimeFormat(context: Context) =
        SimpleDateFormat("hh:mm:ss a", LocaleManager.getLocale(context.resources))

    fun getTimeFormat() =
        SimpleDateFormat("hh:mm:ss a", Locale.US)

}