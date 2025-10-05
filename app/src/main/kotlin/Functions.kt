//1. Создать три функции со следующими сигнатурами: () -> Unit; (Any) -> Any; (Any) -> Collecions<Any>;
//2. Создать функцию с переменным количеством аргументов (vararg);
//3. Создать функцию с параметрами по умолчанию;
//4. Создать функцию, аргументы которой можно передать только по имени;
//5. Три функции нужно представить в следующем виде - полноценное объявление функции, анонимная функция, лямбда-функция;
//6. Создать функцию, которая в качестве аргумента принимает другую функцию;
//7. Создать функцию, которая в качестве возвращаемого значения возвращает другую функцию;
//8. Заменить один из циклов на рекурсивную функцию;
fun fullUnit(): Unit {
    println("fullUnit")
}
val anonymousUnit = fun():Unit {
    println("anonymousUnit")
}
val LambdaUnit: () -> Unit = {
    println("LambdaUnit")
}

fun fullAny(x: Any): Any = x
val anonymousAny = fun(value: Any): Any{
    return value
}
val LambdaAny: (Any)->Any ={it}

fun fullCollection(item:Any): Collection<Any> = listOf(item)
val anonymousCollection = fun(v:Any): Collection<Any> {
    return listOf(v)
}
val LambdaCollection: (Any) -> Collection<Any> = {
        value -> listOf(value)
}

fun varargSred (vararg numbers: Double): Double{
    if (numbers.isEmpty()) return Double.NaN
    val sum = numbers.sum()
    return sum / numbers.size
}

fun withParameters (name: String="Nadya", age: Int=18):String {
    return "никита: $name, age: $age"
}

fun passingArgumentsByName(
    width: Int = 100,
    height: Int = 100
): String {
    return "width=$width, height=$height"
}

fun funTakingFun (x: Int, op: (Int) -> Int): Int { // op — это функция, получающая Int и возвращающая Int
    val once = op(x)
    val twice = op(once)
    return twice
}

fun funReturnsFun (k: Int): (Int) -> Int {
    return { n: Int -> n * k }             //лямбда
}//применим коэффициент k и получим новую функцию (Int) -> Int

fun cycle(nums: List<Int>): Int {
    var acc = 0
    for (n in nums) {
        acc += n
    }
    return acc
}

//хвостовая рекурсия tailrec — эффективно и без переполнения стека на больших списках
tailrec fun recursion(                 //tailrec даёт компилятору оптимизировать хвостовую рекурсию в цикл
    nums: List<Int>,
    index: Int = 0,
    acc: Int = 0
): Int {
    return if (index >= nums.size) {
        acc
    } else {
        recursion(nums, index + 1, acc + nums[index])
    }
}

fun main() {



    fullUnit()
    anonymousUnit()
    LambdaUnit()


    val sum = { a: Int, b: Int -> a + b }

    println(fullAny("ok"))
    println(anonymousAny(42))
    println(LambdaAny(3.14))

    println(fullCollection("X"))
    println(anonymousCollection(7))
    println(LambdaCollection(true))

    println(varargSred(10.0, 20.0, 30.0))
    println(varargSred())//пустой вызов: вернёт NaN.

    println(withParameters(name = "Nadya"))
    println(withParameters(name = "Nadya", age = 18))

    val boxA = passingArgumentsByName(width = 120, height = 80)
    println(boxA)

    val plusOneTwice = funTakingFun(10) { it + 1 } //передаём лямбду (Int)->Int: сначала 10+1=11, затем 11+1=12
    println(plusOneTwice)

    val times3 = funReturnsFun(3) //получили функцию (Int)->Int, которая умножает на 3
    println(times3(14))

    val data = listOf(1, 2, 3, 4, 5)
    println(cycle(data))                     //циклом
    println(recursion(data))                //рекурсией
}