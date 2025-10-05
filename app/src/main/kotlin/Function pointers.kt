//1. Создать несколько функций с одинаковой сигнатурой. Создать массив указателей на эти функции. Вызвать их.
//2. Создать несколько функций с разной сигнатурой. Создать массив указателей на эти функции. Вызвать их.
//3. Создать несколько функций с разным количеством аргументов в сигнатуре. Создать массив указателей на эти функции. Вызвать их. (Исследовательская работа)
//4. Создать три любых декоратора функции, применить (по одному и вместе), показать результат.
//5. Создать декоратор функции с параметром (Исследовательская работа)
typealias UnaryInt = (Int) -> Int

fun times2(x: Int): Int {
    return x * 2
}
fun square(x: Int): Int {
    return x * x
}
fun next(x: Int): Int {
    return x + 1
}

fun lenOf(s: String): Int {
    return s.length
}
fun concat(a: String, b: String): String {
    return a + b
}
fun pi(): Double {
    return Math.PI
}

@Suppress("UNCHECKED_CAST")        //подавляем предупреждения о небезопасных кастах — мы контролируем арность вручную
fun invokeDynamic(func: Function<*>, vararg args: Any?): Any? {
    return when (args.size) {
        0 -> (func as Function0<Any?>).invoke()
        1 -> (func as Function1<Any?, Any?>).invoke(args[0])
        2 -> (func as Function2<Any?, Any?, Any?>).invoke(
            args[0], args[1]
        )
        else -> error("Арность ${args.size} не поддержана")
    }
}

fun sum2(a: Int, b: Int): Int {
    return a + b
}
fun sum3(a: Int, b: Int, c: Int): Int {
    return a + b + c
}
fun sumAll(vararg xs: Int): Int {
    return xs.sum()
}

//адаптеры приводят Array<Any> к нужному вызову
val adapterSum2: (Array<Any>) -> Any = { args ->
    require(args.size == 2 && args[0] is Int && args[1] is Int) {
        "adapterSum2: нужны два Int"
    }
    sum2(args[0] as Int, args[1] as Int)
}
val adapterSum3: (Array<Any>) -> Any = { args ->
    require(args.size == 3 && args.all { it is Int }) {
        "adapterSum3: нужны три Int"
    }
    sum3(args[0] as Int, args[1] as Int, args[2] as Int)
}
val adapterSumAll: (Array<Any>) -> Any = { args ->
    require(args.all { it is Int }) { "adapterSumAll: только Int" }
    sumAll(*args.map { it as Int }.toIntArray())
}

fun logDecorator(f: UnaryInt): UnaryInt {
    return { x ->
        println("[log] вход: x=$x")
        val r = f(x)
        println("[log] выход: r=$r")
        r
    }
}
fun timeDecorator(f: UnaryInt): UnaryInt {
    return { x ->
        val t0 = System.nanoTime()
        val r = f(x)
        val t1 = System.nanoTime()
        println("[time] ${t1 - t0} ns")
        r
    }
}
fun memoDecorator(f: UnaryInt): UnaryInt {          // Декоратор мемоизации: кэширует результаты по ключу x
    val cache = mutableMapOf<Int, Int>()
    return { x ->
        cache[x]?.let {
            println("[memo] hit x=$x -> $it")
            it
        } ?: run {
            val r = f(x)
            cache[x] = r
            println("[memo] miss x=$x, save $r")
            r
        }
    }
}

//декоратор с параметром: retry(times)
fun retryDecorator(times: Int): (UnaryInt) -> UnaryInt { // Функция высшего порядка: принимает параметр times
    require(times >= 1) { "times >= 1" }
    return { f: UnaryInt ->
        // Возвращаем не лямбду, а анонимную функцию — в ней можно делать обычный `return`
        fun(x: Int): Int {
            var attempt = 0
            var lastError: Throwable? = null
            while (attempt < times) {
                try {
                    return f(x)
                } catch (t: Throwable) {
                    attempt++
                    lastError = t
                    println("[retry] попытка #$attempt провалилась: ${t.message}")
                    if (attempt >= times) throw lastError!!
                }
            }
            throw IllegalStateException("недостижимо")
        }
    }
}

fun main() {
    //одинаковая сигнатура
    val sameSig: Array<(Int) -> Int> = arrayOf(
        ::times2, ::square, ::next
    )
    println("одинаковая сигнатура")
    sameSig.forEach { f ->
        val input = 5
        val result = f(input)
        println("f($input) = $result")
    }

    //разные сигнатуры
    val mixed: Array<Function<*>> = arrayOf(
        ::pi, ::lenOf, ::concat
    )
    println("\nразные сигнатуры")
    println("pi() = ${invokeDynamic(mixed[0])}")
    println("lenOf(\"hello\") = ${invokeDynamic(mixed[1], "hello")}")
    println("concat(\"A\",\"B\") = ${invokeDynamic(mixed[2], "A", "B")}")

    //разное число аргументов через адаптеры
    val adapters = arrayOf(adapterSum2, adapterSum3, adapterSumAll) // Единый массив адаптеров типа (Array<Any>)->Any
    println("\nразное число аргументов")
    println("sum2(10,20) = ${adapters[0](arrayOf(10, 20))}")
    println("sum3(1,2,3) = ${adapters[1](arrayOf(1, 2, 3))}")
    println("sumAll(1..6) = ${adapters[2](arrayOf(1, 2, 3, 4, 5, 6))}")

    //декораторы
    val target: UnaryInt = { n ->
        var acc = 0
        var i = 1
        while (i <= n) {
            acc += (i * i) % 97
            i++
        }
        acc
    }

    val logged = logDecorator(target)
    val timed = timeDecorator(target)
    val memoed = memoDecorator(target)

    println("\nдекораторы")
    println("logDecorator")
    println("result = ${logged(5)}")
    println("timeDecorator")
    println("result = ${timed(200_000)}")
    println("memoDecorator")
    println("first  = ${memoed(500_000)}")
    println("second = ${memoed(500_000)}")

    val composed: UnaryInt =
        logDecorator(
            timeDecorator(
                memoDecorator(target)
            )
        )
    println("composed")
    println("result = ${composed(300_000)}")

    //декоратор с параметром
    println("\nдекоратор с параметром")
    val flaky: UnaryInt = { x ->                      // Функция с "нестабильным" поведением — иногда кидает исключение
        if (x % 2 == 0 && System.nanoTime() % 3L == 0L) { // Условие "иногда падаем": чётное x и совпало по времени
            throw RuntimeException("случайный сбой")  // Бросаем исключение — повод для retry
        }
        x * 10
    }
    val flakyWithRetry = retryDecorator(3)(flaky)     // Настраиваем декоратор на 3 попытки и применяем к flaky

    println("flakyWithRetry")
    try {
        println("result = ${flakyWithRetry(2)}")
    } catch (t: Throwable) {
        println("итоговая ошибка: ${t.message}")
    }

    println("flakyWithRetry")
    try {
        println("result = ${flakyWithRetry(3)}")
    } catch (t: Throwable) {
        println("итоговая ошибка: ${t.message}")
    }
}
