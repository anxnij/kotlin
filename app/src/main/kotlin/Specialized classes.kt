//1. Разработать class enum;
//2. Разработать data class;
//3. Привести пример паттерна "Фабрика";
//4. Привести пример ИСПОЛЬЗОВАНИЯ паттерна "Строитель"
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class CoffeeSize(
    val ml: Int,
    val price: Double
) {
    SMALL(200, 1.50),
    MEDIUM(300, 2.10),
    LARGE(400, 2.70);
    fun label(): String =
        "${name.lowercase().replaceFirstChar { it.uppercase() }} (${ml}ml, €${"%.2f".format(price)})"
}

data class Customer(
    val id: Int,
    val name: String,
    val email: String?,
    val vip: Boolean = false
)

// Общий интерфейс для логгеров:
interface Logger {
    fun log(message: String)
}

//логгер в консоль
class ConsoleLogger : Logger {
    override fun log(message: String) {
        println("[CONSOLE] $message")
    }
}

//логгер в файл
class FileLogger(private val fileName: String) : Logger {

    private val logFile = File(fileName)
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    init {
        if (!logFile.exists()) {
            logFile.parentFile?.mkdirs()
            logFile.createNewFile()
        }
    }

    override fun log(message: String) {
        val ts = LocalDateTime.now().format(formatter)
        try {
            FileWriter(logFile, true).use { writer ->
                writer.write("[$ts] $message\n")
            }
        } catch (e: IOException) {
            System.err.println("Ошибка записи в файл $fileName: ${e.message}")
        }
    }
}

// Типы логгеров, которые умеет создавать фабрика:
enum class LoggerType { CONSOLE, FILE }

// Сама фабрика:
object LoggerFactory {
    fun create(type: LoggerType,
               fileName: String = "app.log"
    ): Logger = when (type) {              // Возвращаем интерфейс Logger (полиморфизм).
        LoggerType.CONSOLE -> ConsoleLogger()
        LoggerType.FILE -> FileLogger(fileName)
    }
}

//Паттерн "Строитель"
class Laptop private constructor(
    val cpu: String,
    val ramGb: Int,
    val storageGb: Int,
    val gpu: String?,
    val os: String
) {
    override fun toString(): String =
        "Laptop(cpu=$cpu, ram=${ramGb}GB, storage=${storageGb}GB, gpu=${gpu ?: "integrated"}, os=$os)"

    // Вложенный класс Builder — поэтапная сборка объекта Laptop:
    class Builder {
        private var cpu: String = "Intel i5"
        private var ramGb: Int = 8
        private var storageGb: Int = 256
        private var gpu: String? = null
        private var os: String = "Windows 11"

        // Цепочные методы конфигурации — возвращают this для fluent-стиля:
        fun cpu(value: String) = apply { this.cpu = value }
        fun ramGb(value: Int) = apply { this.ramGb = value }
        fun storageGb(value: Int) = apply { this.storageGb = value }
        fun gpu(value: String?) = apply { this.gpu = value }
        fun os(value: String) = apply { this.os = value }

        fun build(): Laptop {                 //неизменяемый объект Laptop.
            require(cpu.isNotBlank()) { "CPU не должен быть пустым" }
            require(ramGb > 0) { "RAM должна быть > 0" }
            require(storageGb > 0) { "Объём накопителя должен быть > 0" }
            return Laptop(cpu, ramGb, storageGb, gpu, os)
        }
    }
}

// Удобная DSL-обёртка для билдера
fun laptop(buildSteps: Laptop.Builder.() -> Unit): Laptop {
    val builder = Laptop.Builder()
    builder.buildSteps()
    return builder.build()
}

fun main() {

    val size = CoffeeSize.MEDIUM
    println("Выбран размер: ${size.label()}")

    val c1 = Customer(id = 1, name = "Надия", email = "anxnij@yandex.ru")
    val c2 = c1.copy(vip = true)
    println("Customer c1 = $c1")
    println("Customer c2 = $c2")
    println("c1 == c2? ${c1 == c2}")

    val consoleLogger: Logger = LoggerFactory.create(LoggerType.CONSOLE)
    val fileLogger: Logger = LoggerFactory.create(LoggerType.FILE, "events.log")

    consoleLogger.log("Приложение запущено")      // Полиморфный вызов log() — конкретная реализация решается в рантайме.
    fileLogger.log("Событие: выбран размер ${size.name}")

    val laptop1 = Laptop.Builder()             // Создаём Builder вручную.
        .cpu("AMD Ryzen 7 7840U")
        .ramGb(32)
        .storageGb(1024)
        .gpu("RTX 4070")
        .os("Arch Linux")
        .build()

    println("laptop1 = $laptop1")

    val laptop2 = laptop {                     // Вызываем DSL-обёртку: сюда передаём только шаги конфигурации.
        cpu("Apple M3")
        ramGb(16)
        storageGb(512)
        gpu(null)
        os("macOS Sequoia")
    }

    println("laptop2 = $laptop2")              // Вывод конфигурации второй сборки.

    // Мини-полиморфизм поверх фабрики
    val loggers: List<Logger> = listOf(consoleLogger, fileLogger)
    for (logger in loggers) {
        logger.log("Полиморфный вызов log()")
    }

    // Дополнительно покажем значения enum:
    CoffeeSize.values().forEach { s ->
        println("В меню: ${s.label()}")
    }
}
