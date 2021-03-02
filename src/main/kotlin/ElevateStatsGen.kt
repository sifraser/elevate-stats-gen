import com.opencsv.CSVReaderHeaderAware
import java.io.File
import java.io.FileReader
import java.math.BigDecimal
import kotlin.system.exitProcess


class ElevateStatsGen(csv: File) {

    private val activities by lazy {
        val csvReader = CSVReaderHeaderAware(FileReader(csv))
        val activities = mutableListOf<Activity>()
        while (true) {
            val line = csvReader.readNext() ?: break
            activities.add(Activity(
//                    started = LocalDateTime.parse(line[0]),
                    distance = BigDecimal(line[4])
            ))
        }
        activities
    }

    fun printDistanceStats() {
        println(activities.sumBy { it.distance })
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

    statsGen.printDistanceStats()
}

data class Activity(
//        val started: LocalDateTime,
//        val name: String,
//        val duration: BigDecimal,
        val distance: BigDecimal
//        val elevation: BigDecimal
) {
//    val year by lazy { started.year }
}

enum class ActivityType {
    Run,
    Hike,
    Kayaking,
    Ride,
    Rowing,
    VirtualRide,
    Walk,
}