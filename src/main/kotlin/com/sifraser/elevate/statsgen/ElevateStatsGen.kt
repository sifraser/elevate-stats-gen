package com.sifraser.elevate.statsgen

import com.opencsv.CSVReaderHeaderAware
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import java.io.File
import java.io.FileReader
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import kotlin.system.exitProcess


class ElevateStatsGen(csv: File) {

    private val activities by lazy {
        val csvReader = CSVReaderHeaderAware(FileReader(csv))
        val activities = mutableListOf<Activity>()
        while (true) {
            val line = csvReader.readNext() ?: break
            activities.add(Activity(
                    started = LocalDateTime.parse(line[0], ISO_OFFSET_DATE_TIME),
                    durationSec = parseDuration(line[3]),
                    distance = BigDecimal(line[4]),
                    elevationGain = BigDecimal(line[5]),
                    type = ActivityType.valueOf(line[2])
            ))
        }
        activities
    }

    private fun parseDuration(text: String) : BigDecimal {
        val (h, m, s) = text.split(":").map { it.toInt() }
        val total = s + (m * 60) + (h * 60 * 60)
        return BigDecimal(total)
    }

    private val years by lazy { activitiesByYear.keys.toSortedSet() }

    private val types by lazy { ActivityType.values() }

    private val typeGroups by lazy { ActivityTypeGroup.values() }

    private val activitiesByYear by lazy { activities.groupBy { it.year } }

    private val activitiesByType by lazy { activities.groupBy { it.type } }

    private val activitiesByTypeAndYear by lazy {
        activitiesByType.map { (type, act) ->
            type to act.groupBy { it.year }
        }.toMap()
    }

    private fun DIV.typeAndYearTable(getField: (Activity) -> BigDecimal) {
        table {
            thead {
                tr {
                    th { +"\\" }
                    typeGroups.forEach {
                        th { +it.name }
                    }
                    types.forEach {
                        th { +it.name }
                    }
                }
            }
            tbody {
                years.forEach { year ->
                    tr {
                        th { +"$year" }
                        typeGroups.forEach { typeGroup ->
                            val total = typeGroup.types.map { activitiesByTypeAndYear[it]?.get(year) ?: emptyList()}.flatten().sumBy(getField)
                            td { +aggregationFormat.format(total) }
                        }
                        types.forEach { type ->
                            val total = (activitiesByTypeAndYear[type]?.get(year) ?: emptyList()).sumBy(getField)
                            td { +aggregationFormat.format(total) }
                        }
                    }
                }
                tr {
                    th { +"Total" }
                    typeGroups.forEach { typeGroup ->
                        val total = typeGroup.types.map { activitiesByType[it] ?: emptyList()}.flatten().sumBy(getField)
                        td { +aggregationFormat.format(total) }
                    }
                    types.forEach { type ->
                        val total = (activitiesByType[type] ?: emptyList()).sumBy(getField)
                        td { +aggregationFormat.format(total) }
                    }
                }
            }
        }
    }

    fun writeStatsPage(output: File) {
        output.writeText(createHTML().div {
            h1 { +"Distance aggregates (km)" }
            typeAndYearTable { it.distance }
            h1 { +"Elevation gain aggregates (m)" }
            typeAndYearTable { it.elevationGain }
            h1 { +"Duration aggregates (min)" }
            typeAndYearTable { it.durationMin }
        })
    }

    companion object {
        val aggregationFormat = DecimalFormat("#,###")
    }
}

inline fun <T> Iterable<T>.sumBy(selector: (T) -> BigDecimal): BigDecimal {
    var sum: BigDecimal = BigDecimal.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

fun main(args: Array<String>) {
    if (args.size != 1) {
        System.err.println("Need one argument: the CSV exported from Elevate")
        exitProcess(1)
    }
    val csv = File(args.first())
    if (!csv.exists() || !csv.isFile) {
        System.err.println("${csv.absolutePath} is not a file")
        exitProcess(1)
    }
    val statsGen = ElevateStatsGen(csv)

    statsGen.writeStatsPage(File("stats.htm"))
}

data class Activity(
        val started: LocalDateTime,
        val type: ActivityType,
//        val name: String,
        val durationSec: BigDecimal,
        val distance: BigDecimal,
        val elevationGain: BigDecimal
) {
    val year by lazy { started.year }
    val durationMin by lazy { durationSec.divide(BigDecimal(60), 0, RoundingMode.UP) }
}

enum class ActivityType {
    Run,
    Ride,
    VirtualRide,
    Hike,
    Walk,
    Kayaking,
    Rowing,
}

enum class ActivityTypeGroup(val types: List<ActivityType>) {
    Cycling(listOf(ActivityType.Ride, ActivityType.VirtualRide)),
    Walking(listOf(ActivityType.Hike, ActivityType.Walk))
}