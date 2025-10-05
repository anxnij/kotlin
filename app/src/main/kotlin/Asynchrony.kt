//1. Создать прерываемую функцию (suspend);
//2. Создать задачу при помощи launch;
//3. Создать задачу, которая будет возвращать значение при помощи async;
//4. Создать массив задач при помощи launch;
//5. Создать массив задач при помощи async;
//6. Отменить задачу и все её дочерние задачи из main;
//7. Перехватить исключение, возникающее при отмене задачи и обработать его;
//8. Создать задачи с разным контекстом;
//9. Использовать функцию yield в своей работе;
//10. Реализовать шаблон "Производитель-потребитель" при помощи асинхронности.

import java.util.concurrent.ArrayBlockingQueue
import kotlin.random.Random
import kotlinx.coroutines.*
import java.lang.Thread.sleep

object Consumers {

    private val buffer = ArrayBlockingQueue<Int>(10)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    suspend fun startWork() =
        CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                val producers = listOf(
                    coroutineScope.launch { produceData(1) },
                    coroutineScope.launch { produceData(2) },
                    coroutineScope.launch { produceData(3) }
                )

                val consumers = listOf(
                    coroutineScope.launch { consumeData(1) },
                    coroutineScope.launch { consumeData(2) },
                    coroutineScope.launch { consumeData(3) },
                )

                producers.joinAll()
                consumers.joinAll()
            }
        }

    private suspend fun produceData(producerId : Int) {
        try {
            val data = Random.nextInt(100)
            buffer.put(data)
            println("Producer $producerId put: $data (size: ${buffer.size})")
            yield()
        } catch (ce: CancellationException) {
            println("Producer $producerId cancelled: ${ce.message}")
            throw ce
        } catch (_ : Exception) {}
    }

    private suspend fun consumeData(consumerId : Int) {
        try {
            val data = buffer.take()
            println("Consumer $consumerId take: $data (size: ${buffer.size})")
            yield()
        } catch (ce: CancellationException) {
            println("Consumer $consumerId cancelled: ${ce.message}")
            throw ce
        } catch (_ : Exception) {}
    }
}

fun main(): Unit = runBlocking {
    val coroutineExceptionHandler = CoroutineExceptionHandler { context, throwable ->
        println("ERROR FROM COROUTINE: $throwable")
    }
    val firstScope = CoroutineScope(
        Dispatchers.IO +
                coroutineExceptionHandler +
                SupervisorJob()
    )
    firstScope.launch {
        mySuspendFun()
    }.join()

    firstScope.launch {
        val res = firstScope.async {
            myNewFun()
        }.await()
        println("result: $res")
    }.join()

    val launchTasks = List(5) { idx ->
        launch(Dispatchers.Default) {
            repeat(3) { step ->
                println("launch[$idx] step=$step on ${Thread.currentThread().name}")
                yield()
            }
        }
    }
    launchTasks.joinAll()

    val asyncTasks = List(3) { idx ->
        async(Dispatchers.IO) {
            delay(50L * (idx + 1))
            idx * 10
        }
    }
    val asyncResults = asyncTasks.awaitAll()
    println("async results: $asyncResults")

    val coroutine = CoroutineScope(Dispatchers.IO).launch {
        println("SOME TEXT")
        try {
            while (isActive) {
                print("0")
                yield()
            }
        } catch (ce: CancellationException) {
            println("\nloop coroutine cancelled: ${ce.message}")
            throw ce
        }
        launch {
            println("new 2 launch")
            launch {
                println("new 3 launch")
            }.join()
        }.join()
        println("END WORK")
    }
    coroutine.cancel(CancellationException("cancel from main"))
    coroutine.cancelAndJoin()

    launch {
        repeat(5) { i ->
            println("Coroutine 1: $i")
            yield()
        }
    }
    launch {
        repeat(5) { i ->
            println("Coroutine 2: $i")
            yield()
        }
    }

    val work = Consumers.startWork()
    sleep(1500)
    work.cancel(CancellationException("stop Consumers from main"))
    work.cancelAndJoin()
}

suspend fun mySuspendFun() {
    println("I AM FROM SUSPEND")
    throw Exception("BASIC PROBLEM")
}

suspend fun myNewFun() : Int {
    return 144 * 15 + 122
}
