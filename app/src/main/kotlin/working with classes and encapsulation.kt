//Необходимо создать класс, описывающий предмет в мире, несколько объектов, и продемонстрировать их функциональность. Класс обязан содержать публичные, приватные, защищенные и внутренние свойства. Сделать такие же статические свойства и методы во всех комбинациях.
//Одно из свойств должно иметь собственную реализацию сеттера и геттера. Один из сеттеров или геттеров должен иметь модификатор доступа.
//Создать при помощи наследования цепочку классов. Реалистичность функционала не важна. Создать несколько объектов этих классов.
//Продемонстрировать результат механизма наследования. Продемонстрировать проверку на принадлежность объекта определенному классу.
//Добавить и использовать в работу абстрактный базовый класс (минимум один) и несколько интерфейсов (минимум два).
//Продемонстрировать полиморфизм в коде (написать конкретный пример применения)

interface Wearable {
    fun wear(): String                     //надеть
}
interface Repairable {
    fun repair(): String                   //ремонт
}
interface Sellable {
    val price: Double                      //цена
    fun sellTo(customer: String): String
}

abstract class Material(
    val name: String
) {
    open fun description(): String =
        "Материал: $name"

    abstract fun density(): Double         //плотность
}

open class МагматическийКамень(
    name: String,
    private val silicaPercent: Int         //процент кремнезёма
) : Material(name) {

    override fun description(): String =
        "Магматический камень: $name (SiO2=$silicaPercent%)"

    override fun density(): Double =       //абстрактная плотность
        2.5 + (silicaPercent / 100.0) * 0.5
}

class Железо(
    name: String,
    private val carbonPercent: Double       //процент углерода
) : МагматическийКамень(name, 0) {

    override fun description(): String =
        "Железо: $name (C=${"%.2f".format(carbonPercent)}%)"

    override fun density(): Double =        //плотность
        7.87 + carbonPercent * 0.01
}

open class Glasses(
    val brand: String,
    internal var model: String,
    protected var frameMaterial: String,
    private var _basePrice: Double
) : Wearable, Repairable, Sellable {

    override val price: Double
        get() = _basePrice * (1.0 + warrantyYears * 0.05)
    //геттер вычисляет финальную цену как базу + 5% за каждый год гарантии

    internal var warrantyYears: Int = 1     //гарантия
        set(value) {                        //проверка и нормализация
            field = value.coerceIn(0, 5)
        }

    var sizeMm: Int = 140                   //ширина оправы
        private set(value) {
            field = value.coerceIn(100, 170)
        }
        get() = field
    fun resize(newSize: Int) {              //изменяет sizeMm, не раскрывая приватный сеттер
        sizeMm = newSize                    // Вызовет приватный set(...) и применит нормализацию
    }

    private var scratches: Int = 0

    companion object {

        var totalProduced: Int = 0
        internal var internalMetric: Long = 0L

        @JvmStatic
        protected var protectedKey: String = "PROT"

        private var privateSalt: String = "s3cr3t"

        public fun publicInfo(): String =
            "Всего выпущено: $totalProduced"

        internal fun touchMetric() {
            internalMetric++
        }

        @JvmStatic
        protected fun signPayload(s: String): String =
            "$s#$protectedKey"

        private fun rotateSalt() {
            privateSalt = privateSalt.reversed()
        }

        fun factory(
            brand: String,
            model: String,
            material: String,
            basePrice: Double
        ): Glasses {
            rotateSalt()
            touchMetric()
            totalProduced++
            return Glasses(brand, model, material, basePrice)
        }
    }

    override fun wear(): String =
        "Надеты очки $brand $model (рамка: $frameMaterial, размер: ${sizeMm}мм)"

    override fun repair(): String {
        val before = scratches
        scratches = 0                       //обнуляем царапины
        return "Ремонт: царапин было $before, стало $scratches"
    }

    override fun sellTo(customer: String): String =
        "Продано $customer за ${"%.2f".format(price)} €"

    fun addScratch() {                      //добавить царапину
        scratches++
    }

    open fun lensInfo(): String =
        "Линзы: прозрачные"

    override fun toString(): String =
        "Glasses(brand=$brand, model=$model, frameMaterial=$frameMaterial, sizeMm=$sizeMm, price=${"%.2f".format(price)})"
}

class Sunglasses(
    brand: String,
    model: String,
    frameMaterial: String,
    basePrice: Double,
    private var uv: Int                      //уровень защиты от уф
) : Glasses(brand, model, frameMaterial, basePrice) {

    override fun lensInfo(): String =
        "Линзы: тёмные, UV-$uv"

    fun signCaseLabel(label: String): String {
        // Доступ к protected “статикам” компаньона родителя
        // после @JvmStatic вызываем БЕЗ Companion — напрямую через имя класса (или просто signPayload(...) внутри наследника)
        return Glasses.signPayload(label)    // Корректный вызов protected @JvmStatic метода компаньона
    }

    override fun toString(): String =
        "Sunglasses(brand=$brand, model=$model, frameMaterial=$frameMaterial, sizeMm=$sizeMm, UV=$uv, price=${"%.2f".format(price)})"
}

fun printMaterialInfo(m: Material) {
    println(m.description())                 // Вызовет версию description(), соответствующую реальному типу (виртуально)
    println("Плотность: ${"%.3f".format(m.density())}") // То же с density()
}

fun putOn(w: Wearable) {
    println(w.wear())
}

fun main() {

    //создание через фабрику-компаньон
    val g1 = Glasses.factory(
        brand = "Acme",
        model = "Model-A",
        material = "TR90",
        basePrice = 120.0
    )

    val g2 = Sunglasses(
        brand = "RayBee",
        model = "Sun-X",
        frameMaterial = "Aluminum",
        basePrice = 200.0,
        uv = 400
    )

    println(g1.brand)
    g1.warrantyYears = 10
    println("warrantyYears (clamped): ${g1.warrantyYears}")

    println("size (before): ${g1.sizeMm}")
    g1.resize(95)
    println("size (after 95): ${g1.sizeMm}")
    g1.resize(165)
    println("size (after 165): ${g1.sizeMm}")

    println(g1.wear())                       // Wearable
    g1.addScratch()                          // “Изнашиваем” очки.
    println(g1.repair())                     // Repairable
    println(g1.sellTo("Талиана")) // Sellable (цена считается через кастомный геттер price).

    // Доступ к “статикам” компаньона:
    println(Glasses.publicInfo())
    println("Всего очков выпущено: ${Glasses.totalProduced}")

    // Наследник может обратиться к protected члену компаньона через свою логику:
    println(g2.signCaseLabel("CASE-001"))

    // Полиморфизм по интерфейсу:
    putOn(g1)
    putOn(g2)

    // Переопределённое поведение у наследника:
    println("g1.lensInfo(): ${g1.lensInfo()}")
    println("g2.lensInfo(): ${g2.lensInfo()}")

    val m1: Material = МагматическийКамень("Габбро", silicaPercent = 45)
    val m2: Material = Железо("Fe-Arm", carbonPercent = 0.8)

    printMaterialInfo(m1)
    printMaterialInfo(m2)

    //Проверки на принадлежность класса
    println("m1 is МагматическийКамень? ${m1 is МагматическийКамень}")
    println("m1 is Железо? ${m1 is Железо}")
    println("m2 is Material? ${m2 is Material}")

    // Безопасное приведение as? — вернёт null, если тип не подходит:
    val maybeIron: Железо? = m1 as? Железо
    println("safe cast m1 as? Железо = $maybeIron")

    // Пример “умного” when с типами:
    fun materialKind(x: Material): String = when (x) {
        is Железо -> "Это железо"
        is МагматическийКамень -> "Это магматический камень"
        else -> "Неизвестный материал"
    }
    println(materialKind(m1))
    println(materialKind(m2))

    //Полиморфизм: список базового типа с разными реализациями
    val materials: List<Material> = listOf(m1, m2)
    for (mat in materials) {
        println(">> ${mat.description()} — ρ=${"%.2f".format(mat.density())}"
    }

    //вывод объектов очков:
    println(g1)
    println(g2)
}
