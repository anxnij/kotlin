//1. Применить не менее трёх аннотаций из стандартной библиотеки Kotlin;
//2. Применить не менее трёх аннотаций из стандартной библиотеки Java;
//3. Создать не менее трёх своих аннотаций;
//4. Написать код, который по аннотациям изменяет функциональность кода.
package org.example

import org.example.oldFunction
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import kotlin.random.Random
import kotlin.reflect.full.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

fun setupConsole(){
    System.setOut(PrintStream(System.out, true, StandardCharsets.UTF_8.name()))
    System.setErr(PrintStream(System.err, true, StandardCharsets.UTF_8.name()))

    System.setProperty("console.encoding", "utf-8")
}
//Kotlin

@Suppress("UNCHECKED_CAST")
fun Podavlenie(){
    val any: Any = "Hello"
    val str: String = any as String

    println(str)
    println("Предупреждения подавлены)")
}

@Deprecated(
    "Пожалуйста используйте новую функцию!!!!!!",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("newFunction")
)

fun oldFunction(){
    println("Старая функция")
}

fun newFunction(){
    println("Новая функция")
}

@RequiresOptIn(message = "Это функция - эксперимент")
annotation class Expirience

@Expirience
fun expFunction(){
    println("Простой экспермент(0_0)")
}

//Java

open class Parent{
    open fun show(){
        println("Parent method!!!")
    }
}

class Children : Parent(){
    @Override
    override fun show(){
        println("Children method")
    }
}


fun notReturnNull(@NotNull str: String){
    println("Длина: ${str.length}")
}

//@Nullable
fun ReturnNull(): String? {
    return if (Random.nextBoolean()) "text" else null
}


@FunctionalInterface
interface TextInterface{
    fun changeText(text: String): String

    fun show(){
        println("Функциональный интерфейс")
    }
}

//Java and Kotlin

@MustBeDocumented
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ImportantAPI(val version: String)

@ImportantAPI("1.0")
class MyImportantClass{
    @ImportantAPI("1.1")
    fun MyImportantFunction(){
        println("Важный метод!!!!!!")
    }
}

@MustBeDocumented
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Important(val version: String)
annotation class Transport
annotation class Drive

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Comment(val version: Double, val comment: String)

inline fun <reified T : Annotation> KFunction<*>.hasAnnotation(): Boolean {
    return this.annotations.any{it is T}
}

inline  fun <reified T : Annotation> KFunction<*>.getAnnotation(): T? {
    return this.annotations.find { it is T } as? T
}

@Important("1.0.1")
@Transport
class AutoAfterMasters(val carName: String) {

    @Drive
    fun driveAutoAfterMasters(){
        val driveFunction = ::driveAutoAfterMasters
        if (driveFunction.hasAnnotation<Drive>()){
            println("НАЧАЛО ТЕСТ ДРАЙВА!!!")
            println("ПРОВЕРКА ДВИГАТЕЛЯ...")
            println("ПРОВЕРКА ТОРМОЗОВ...")
        }
        println("Машина $carName выехала после ремонта двигателя")
    }

    @Important("1.0.1")
    @Comment(0.1, "Первые 5 миль машина едет исправно")
    fun testDriveFirstFiveMils(){
        val firstTestDriveFunction = ::testDriveFirstFiveMils
        if (firstTestDriveFunction.hasAnnotation<Comment>()){
            val firstComment = firstTestDriveFunction.getAnnotation<Comment>()
            if (firstComment != null){
                println("\u001B[92mИНФОРМАЦИЯ О ПОЕЗДКЕ НА ПЕРВЫЕ 5 МИЛЬ:\u001b[0m")
                println("КОММЕНТАРИЙ ВОДИТЕЛЯ - ${firstComment.comment}")
            }
        }
        println("Машина едет исправно")
    }

    @Important("1.0.1")
    @Comment(0.2, "Проехала еще 5 миль и снова 10 ошибок на приборке (7 _ 7) ")
    fun testDriveSecondFiveMils(){
        val secondTestDriveFunction = ::testDriveSecondFiveMils
        if (secondTestDriveFunction.hasAnnotation<Comment>()){
            val secondComment = secondTestDriveFunction.getAnnotation<Comment>()
            if (secondComment != null){
                println("\u001B[92mИНФОРМАЦИЯ О ПОЕЗДКЕ НА ВТОРЫЕ 5 МИЛЬ:\u001b[0m")
                println("КОММЕНТАРИЙ ВОДИТЕЛЯ - ${secondComment.comment}")
            }
        }
        println("Машина сново сломана")
    }
}


@OptIn(Expirience::class)
fun main(){
    setupConsole()

    println("Работа аннотации - Suppres")
    Podavlenie()
    println()

    println("Работа аннотации - Deprecated")
    oldFunction()
    newFunction()
    println()

    println("Работа аннотации - OptIn(RequiresOptIn)")
    expFunction()
    println()

    println("Работа аннотации - Override")
    val children = Children()
    children.show()
    println()

    println("Работа аннотации - NotNull")
    notReturnNull("text")
    //notReturnNull(null)
    println()

    println("Работа аннотации(Java) - Nullable")
    val resultNull = ReturnNull()
    println("Результат: $resultNull")
    println()

    println("Работа аннотации - FunctionalInterface")
    val change = object : TextInterface{
        override fun changeText(text: String) = text.uppercase()
    }
    println("${change.changeText("slava")}")
    println()

    val importantClass = MyImportantClass()
    importantClass.MyImportantFunction()

    val annotation = MyImportantClass::class.annotations
    annotation.forEach { println("Аннотация - $it") }

    val autoClass = AutoAfterMasters("\u001B[93mBMW M8 Sport\u001B[0m")
    println("1. Тестируем @Drive аннотацию:")
    autoClass.driveAutoAfterMasters()
    println()

    println("2. Тестируем @Comment аннотацию (версия 0.1):")
    autoClass.testDriveFirstFiveMils()
    println()

    println("2. Тестируем @Comment аннотацию (версия 0.2):")
    autoClass.testDriveSecondFiveMils()
    println()

    println("\u001B[93m===ИНФОРМАЦИЯ ОБ АННОТАЦИЯХ @Comments:\u001B[0m")
    println()
    val function = AutoAfterMasters::class.declaredMemberFunctions

    for (func in function){
        val commentAnnotation = func.findAnnotation<Comment>()
        if (commentAnnotation != null ){
            println("Функция - ${func.name}")
            println("Версия комментария - ${commentAnnotation.version}")
            println("Комментарий - ${commentAnnotation.comment}")
            println()

        }
    }

}