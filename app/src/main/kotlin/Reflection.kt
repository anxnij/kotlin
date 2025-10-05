//1. Вывести список всех атрибутов класса;
//2. Вывести список ВСЕХ предков;
//3. Вывести список всех МЕТОДОВ класса;
//4. Вывести список всех СВОЙСТВ класса;
//5. Вывести список аргументов функции;
//6. Вывести список ТИПОВ аргументов функции;
//7. Добавить свойство в класс (исследовательская работа);
//8. Добавить метод в класс (исследовательская работа);
//9. Изменить метод в классе (исследовательская работа);
//10. Изменить метод в классе-предке (исследовательская работа);
//11. Удалить метод в классе (исследовательская работа);
//12. Удалить свойство в классе (исследовательская работа).
//13. Изменить свойство в классе (исследовательская работа);
package org.example

import java.io.PrintStream
import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible

fun setupConsole() {
    System.setOut(PrintStream(System.out, true, StandardCharsets.UTF_8.name()))
    System.setErr(PrintStream(System.err, true, StandardCharsets.UTF_8.name()))

    System.setProperty("console.encoding", "utf-8")
}

class BankAccount(val name: String, private var balance: Double) {
    val isRich: Boolean
        get() = balance > 1000

    fun deposit(amount: Double) {
        if (amount > 0) {
            balance += amount
            println("$name пополнение счета на $amount")
        } else {
            println("Sum > 0!!!!")
        }
    }

    fun snyat(amount: Double): Boolean {
        if (balance > 0 && balance >= amount && amount > 0) {
            balance -= amount
            println("$name снятие со счета $amount")
            return true
        }
        println("Недостаточно средств")
        return false
    }

    fun transfer(toAccount: BankAccount, amount: Double): Boolean {
        if (snyat(amount)) {
            toAccount.deposit(amount)
            println("Перевод успешен")
            return true
        }
        println("Перевод не может быть выполнен")
        return false
    }

    private fun showBalance(name: String) {
        println("Баланс $name = $balance")
    }

    fun getInfo(): String {
        return "Владелец: $name | Баланс счета: $balance | Богатство: $isRich"
    }
}

class Inspector {

    fun showAllProperties(className: KClass<*>) {
        println("СВОЙСТВА КЛАССА ${className.simpleName}:")
        className.declaredMemberProperties.forEach { property ->
            val canChange = if (property is KMutableProperty<*>) "МОЖНО МЕНЯТЬ" else "ТОЛЬКО ЧТЕНИЕ"
            println("${property.name} (${property.returnType}) - $canChange")
        }
    }

    fun showAllPredkov(className: KClass<*>) {
        println("ПРЕДКИ КЛАССА ${className.simpleName}:")
        className.supertypes.forEach { superType ->
            println(superType)
        }
    }

    fun showAllMethods(className: KClass<*>) {
        println("ВСЕ МЕТОДЫ КЛАССА ${className.simpleName}:")
        className.declaredMemberFunctions.forEach { method ->
            method.isAccessible = true
            println("МЕТОД: ${method.name}(${method.parameters.joinToString { it.type.toString() }})")
        }
    }

    fun showAllMethodNotGetAndSet(className: KClass<*>) {
        println("ВСЕ МЕТОДЫ КРОМЕ get/set ${className.simpleName}:")
        className.declaredMemberFunctions.forEach { function ->
            function.isAccessible = true
            if (!function.name.startsWith("get") && !function.name.startsWith("set")) {
                println("МЕТОД: ${function.name}()")
            }
        }
    }

    fun showMethodArgument(className: KClass<*>, methodName: String) {
        println("АРГУМЕНТЫ МЕТОДА $methodName:")
        val method = className.declaredMemberFunctions.find { it.name == methodName }
        if (method == null) {
            println("МЕТОД НЕ СУЩЕСТВУЕТ")
            return
        }
        method.parameters.forEachIndexed { index, argument ->
            if (index > 0) { // index 0 = this
                println("${argument.name ?: "АРГУМЕНТ_$index"} ПОЗИЦИЯ - $index")
            }
        }
    }

    fun showMethodArgumentType(className: KClass<*>, methodName: String) {
        println("ТИПЫ АРГУМЕНТОВ МЕТОДА $methodName:")
        val method = className.declaredMemberFunctions.find { it.name == methodName }
        if (method == null) {
            println("МЕТОД НЕ НАЙДЕН")
            return
        }
        method.parameters.forEachIndexed { index, argument ->
            if (index > 0) {
                println("${argument.name ?: "АРГУМЕНТ_$index"}: ${argument.type}")
            }
        }
    }

    fun modifirePropertyInClass(instance: Any, propertyName: String, newValue: Any) {
        println("ИЗМЕНЕНИЕ СВОЙСТВА $propertyName:")
        val property = instance::class.declaredMemberProperties.find { it.name == propertyName }
        if (property != null && property is KMutableProperty<*>) {
            property.isAccessible = true
            try {
                property.setter.call(instance, newValue)
                println("СВОЙСТВО $propertyName изменено на $newValue")
            } catch (e: Exception) {
                println("ОШИБКА: ${e.message}")
            }
        } else {
            println("Свойство $propertyName не найдено или его нельзя менять")
        }
    }
}

fun main() {
    setupConsole()

    println("РАБОТА БАНКОВСКИХ СЧЕТОВ")

    val account1 = BankAccount("Надя", 500.0)
    val account2 = BankAccount("Лера", 1750.0)

    println(account1.getInfo())
    println(account2.getInfo())
    println()

    account1.deposit(500.0)
    println()
    account2.snyat(200.0)
    println()

    println("ПЕРЕВОД МЕЖДУ СЧЕТАМИ:")
    account1.transfer(account2, 200.0)

    println("\n\nАНАЛИЗ:")
    val inspector = Inspector()
    val bankAccountClass = BankAccount::class

    inspector.showAllMethods(bankAccountClass)
    println()

    inspector.showAllPredkov(bankAccountClass)
    println()

    inspector.showAllProperties(bankAccountClass)
    println()

    inspector.showMethodArgument(bankAccountClass, "трансфер")
    println()

    inspector.showMethodArgumentType(bankAccountClass, "трансфер")
    println()

    inspector.showAllMethodNotGetAndSet(bankAccountClass)
    println()

    inspector.modifirePropertyInClass(account1, "баланс", 2000.0)
    println(account1.getInfo())
}
