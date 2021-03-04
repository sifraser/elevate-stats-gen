package com.sifraser.elevate.statsgen

import com.opencsv.CSVReaderHeaderAware
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
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
                    name = line[1],
                    distance = BigDecimal(line[4]),
                    elevationGain = BigDecimal(line[5]),
                    type = ActivityType.valueOf(line[2])
            ))
        }
        activities
    }

    private fun parseDuration(text: String): BigDecimal {
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

    private fun BODY.typeAndYearTable(title: String, getField: (Activity) -> BigDecimal) {
        h2 { +title }
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
                    th { +"All" }
                }
            }
            tbody {
                years.forEach { year ->
                    tr {
                        th { +"$year" }
                        typeGroups.forEach { typeGroup ->
                            val total = typeGroup.types.map {
                                activitiesByTypeAndYear[it]?.get(year) ?: emptyList()
                            }.flatten().sumBy(getField)
                            td(classes = "number $typeGroup") { +aggregationFormat.format(total) }
                        }
                        types.forEach { type ->
                            val total = (activitiesByTypeAndYear[type]?.get(year) ?: emptyList()).sumBy(getField)
                            td(classes = "number $type") { +aggregationFormat.format(total) }
                        }
                        val yearTotal = (activitiesByYear[year] ?: emptyList()).sumBy(getField)
                        td(classes = "number all") { +aggregationFormat.format(yearTotal) }
                    }
                }
                tr {
                    th { +"Total" }
                    typeGroups.forEach { typeGroup ->
                        val total = typeGroup.types.map {
                            activitiesByType[it] ?: emptyList()
                        }.flatten().sumBy(getField)
                        td(classes = "number total $typeGroup") { +aggregationFormat.format(total) }
                    }
                    types.forEach { type ->
                        val total = (activitiesByType[type] ?: emptyList()).sumBy(getField)
                        td(classes = "number total $type") { +aggregationFormat.format(total) }
                    }
                    val total = activities.sumBy(getField)
                    td(classes = "number total all") { +aggregationFormat.format(total) }
                }
            }
        }
    }

    private fun BODY.top10Table(title: String, type: ActivityType, getField: (Activity) -> BigDecimal) =
            top10Table(title, activitiesByType[type] ?: emptyList(), getField)

    private fun BODY.top10Table(title: String, typeGroup: ActivityTypeGroup, getField: (Activity) -> BigDecimal) =
            top10Table(title, typeGroup.types.map { activitiesByType[it] ?: emptyList() }.flatten(), getField)

    private fun BODY.top10Table(title: String, act: List<Activity>, getField: (Activity) -> BigDecimal) {
        h2 { +title }
        top10Table(act.sortedByDescending(getField).take(10), getField)
        act.groupBy { it.year }.toSortedMap().filter { (_, a) -> a.isNotEmpty() }.forEach { (y, a) ->
            h3 { +"$y" }
            top10Table(a.sortedByDescending(getField).take(10), getField)
        }
    }

    private fun BODY.top10Table(top10: List<Activity>, getField: (Activity) -> BigDecimal) {
        table {
            thead {
                tr {
                    th { +"Measure" }
                    th { +"Date" }
                    th { +"Name" }
                }
            }
            tbody {
                top10.forEach {
                    tr {
                        td(classes = "number ${it.type}") { +singleFormat.format(getField(it)) }
                        td(classes = "date ${it.type}") { +it.started.toLocalDate().toString() }
                        td(classes = "name ${it.type}") { +it.name }
                    }
                }
            }
        }
    }

    private val css = """
        html {
            font-size: 12pt;
            line-height: 1.2;
        }
        body {
            font-family: georgia;
            max-width: 1000px;
            margin: 0 auto;
        }
	    table {
            border-collapse: collapse;
        }
        table, th, td {
            border: 1px solid black;
        }
		th, td {
            min-width: 80px;
            padding: 5px;
        }
        td.number {
			text-align: right;
		}
        td.Ride, td.VirtualRide, td.Cycling {
            background: #B1D4EC;
        }
        td.Walk, td.Hike, td.Walking {
            background: #F1D8C5;
        }
        td.Run {
            background: #F9F0C2;
        }
        td.Rowing {
            background: #B3B8DF;
        }
        td.Kayaking {
            background: #ECE9DD;
        }
        td.all {
            background: #D6F8E0;
        }
        td.total, td.all, th {
            font-weight: bold;
        }
        th {
            background: lightgray;
        }
    """

    fun writeStatsPage(output: File) {
        output.writeText(StringBuilder().appendHTML().html {
            head {
                title { +"Activity stats" }
                style { +css }
            }
            body {
                h1 { +"Activity stats" }
                typeAndYearTable("Distance aggregates (km)") { it.distance }
                typeAndYearTable("Elevation gain aggregates (m)") { it.elevationGain }
                typeAndYearTable("Duration aggregates (min)") { it.durationMin }

                typeGroups.forEach {
                    top10Table("Top 10 distance (km) - ${it.name}", it) { it.distance }
                }
                types.forEach {
                    top10Table("Top 10 distance (km) - ${it.name}", it) { it.distance }
                }
                typeGroups.forEach {
                    top10Table("Top 10 elevation gain (m) - ${it.name}", it) { it.elevationGain }
                }
                types.forEach {
                    top10Table("Top 10 elevation gain (m) - ${it.name}", it) { it.elevationGain }
                }
                typeGroups.forEach {
                    top10Table("Top 10 climb (m/km) - ${it.name}", it) { it.avgClimb }
                }
                types.forEach {
                    top10Table("Top 10 climb (m/km) - ${it.name}", it) { it.avgClimb }
                }
            }
        }.toString())
    }

    companion object {
        val aggregationFormat = DecimalFormat("#,###")
        val singleFormat = DecimalFormat("###.0")
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
        val name: String,
        val durationSec: BigDecimal,
        val distance: BigDecimal,
        val elevationGain: BigDecimal
) {
    val year by lazy { started.year }
    val durationMin by lazy { durationSec.divide(BigDecimal(60), 0, RoundingMode.UP) }
    val avgClimb by lazy { if (distance == BigDecimal.ZERO) BigDecimal.ZERO else elevationGain.divide(distance, 2, RoundingMode.UP) }
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