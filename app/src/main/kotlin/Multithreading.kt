//1. Создать поток, запустить в нем функцию;
//2. Создать два потока, запустить в них функции;
//3. Создать массив потоков, запустить в них функции;
//4. Прервать побочный поток из главного до того, как он завершит свою работу;
//5. Прервать побочный поток из другого побочного потока до того, как они завершат свою работу;
//6. Создать функции "Производитель" и "Потребитель", которые работают с общим массивом (одна функция кладет в него данные, другая забирает). Создать несколько потоков-потребителей и несколько потоков-производителей;
//7. Создать побочный поток, который создает другой побочный поток. Сделать так, чтобы при прерывании первого потока перед своим завершением он прервал другой поток;
//8. Написать любую уникальную собственную задачу с использованием многопоточности.

@file:JvmName("ThreadsDemo")
package demo
import java.util.ArrayDeque //реализация очереди
import java.util.concurrent.CountDownLatch //синхронизатор
import java.util.concurrent.atomic.AtomicBoolean //для остановки потоков
import kotlin.random.Random

private fun nap(ms: Long) {
    try { Thread.sleep(ms) }
    catch (_: InterruptedException) { Thread.currentThread().interrupt() }
}

private fun banner(title: String) = println("\n==== $title ====")

//Создать поток, запустить в нем функцию
fun task1() {
    banner("[1] Один поток")
    val t = Thread({
        println("[${Thread.currentThread().name}] hi from task1()")
    }, "T-1")
    t.start()
    t.join()
}

//Создать два потока, запустить в них функции
fun task2() {
    banner("[2] Два потока")
    val t1 = Thread { println("[T-2A] work") }
    val t2 = Thread { println("[T-2B] work") }
    t1.start(); t2.start()
    t1.join(); t2.join()
}

//Массив потоков, запустить в них функции
fun task3() {
    banner("[3] Массив потоков")
    val threads = Array(5) { i ->
        Thread({
            println("[${Thread.currentThread().name}] i=$i")
        }, "T-3-$i")
    }
    threads.forEach { it.start() }
    threads.forEach { it.join() }
}

//Прервать побочный поток из главного до завершения
fun task4() {
    banner("[4] Прерываем из main")
    val worker = Thread({
        println("[T-4-worker] start long job")
        while (!Thread.currentThread().isInterrupted) {
            nap(100)
        }
        println("[T-4-worker] noticed interrupt, cleanup & exit")
    }, "T-4-worker")
    worker.start()
    nap(300)
    println("[main] interrupt T-4-worker")
    worker.interrupt() // Прерываем поток из main.
    worker.join()
}

//Прервать побочный поток из другого побочного потока
fun task5() {
    banner("[5] Прерываем из другого побочного потока")
    lateinit var victim: Thread
    val interrupter = Thread({
        nap(250)
        println("[T-5-interrupter] interrupt victim")
        victim.interrupt()
    }, "T-5-interrupter")
    victim = Thread({
        println("[T-5-victim] running...")
        while (!Thread.currentThread().isInterrupted) {
            nap(100)
        }
        println("[T-5-victim] interrupted → exit")
    }, "T-5-victim")
    victim.start()
    interrupter.start()
    victim.join(); interrupter.join()
}

//Производители/Потребители с общим массивом (буфер)
class BoundedBuffer<T>(private val capacity: Int = 10) {
    private val q = ArrayDeque<T>()
    private val lock = Object()
    fun put(x: T) {
        synchronized(lock) {
            while (q.size >= capacity && !Thread.currentThread().isInterrupted) {
                try { lock.wait(100) }
                catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return
                }
            }
            if (Thread.currentThread().isInterrupted) return
            q.addLast(x)
            lock.notifyAll()
        }
    }

    fun take(): T? {
        synchronized(lock) {
            while (q.isEmpty() && !Thread.currentThread().isInterrupted) {
                try { lock.wait(100) }
                catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return null
                }
            }
            if (q.isEmpty()) return null
            val v = q.removeFirst()
            lock.notifyAll()
            return v
        }
    }
}

fun task6() {
    banner("[6] Producers/Consumers")
    val buf = BoundedBuffer<Int>(capacity = 8)
    val stop = AtomicBoolean(false)
    fun producer(id: Int) = Thread({
        var x = 0
        while (!stop.get() && !Thread.currentThread().isInterrupted) {
            buf.put((id shl 16) or x)
            println("[P-$id] produced $x")
            x++
            nap(Random.nextLong(20, 80))
        }
        println("[P-$id] stop")
    }, "P-$id")

    fun consumer(id: Int) = Thread({
        while (!stop.get() && !Thread.currentThread().isInterrupted) {
            val v = buf.take()
            if (v != null) {
                println("[C-$id] consumed $v")
            }
            nap(Random.nextLong(40, 120))
        }
        println("[C-$id] stop")
    }, "C-$id")

    val producers = List(3) { i -> producer(i) }
    val consumers = List(4) { i -> consumer(i) }
    (producers + consumers).forEach { it.start() }
    nap(1000)
    stop.set(true) // Ставим флаг остановки.
    (producers + consumers).forEach { it.interrupt() } // Прерываем всех.
    (producers + consumers).forEach { it.join() }
}

//Побочный поток создаёт другой; при прерывании первичный прерывает дочерний
fun task7() {
    banner("[7] Parent прерывает Child при своём прерывании")
    val started = CountDownLatch(1) // Счётчик для синхронизации старта ребёнка.
    val parent = Thread({
        var child: Thread? = null
        try {
            child = Thread({
                println("[T-7-child] working...")
                while (!Thread.currentThread().isInterrupted) {
                    nap(100)
                }
                println("[T-7-child] interrupted → exit")
            }, "T-7-child")
            child.start()
            started.countDown()
            println("[T-7-parent] doing stuff...")
            while (!Thread.currentThread().isInterrupted) {
                nap(100)
            }
        } finally {
            println("[T-7-parent] got interrupt → interrupt child & join")
            child?.interrupt()
            val needRestore = Thread.interrupted() // Сбрасываем флаг прерывания, чтобы join не упал.
            try { child?.join() }
            catch (_: InterruptedException) {}
            finally {
                if (needRestore) {
                    Thread.currentThread().interrupt() // Восстанавливаем флаг.
                }
            }
            println("[T-7-parent] exit")
        }
    }, "T-7-parent")

    parent.start()
    started.await()
    nap(350)
    println("[main] interrupt parent")
    parent.interrupt()
    parent.join()
}

//Гонка поисковиков
fun task8() {
    banner("[8] Гонка поисковиков с отменой остальных")
    val MAGIC = 7777
    val found = AtomicBoolean(false)
    val workers = mutableListOf<Thread>()
    repeat(6) { idx ->
        val t = Thread({
            val rng = Random(idx)
            while (!Thread.currentThread().isInterrupted && !found.get()) {
                val candidate = rng.nextInt(1, 1_000_000)
                if (candidate % MAGIC == 0) {
                    if (found.compareAndSet(false, true)) {
                        println("[${Thread.currentThread().name}] FOUND $candidate")
                        workers.forEach { if (it !== Thread.currentThread()) it.interrupt() }
                    }
                    break
                }
                if (rng.nextInt(0, 10) == 0) nap(1)
            }
            println("[${Thread.currentThread().name}] exit")
        }, "Finder-$idx")
        workers += t
    }

    workers.forEach { it.start() }
    workers.forEach { it.join() }
    println("[main] task8 done (found=${found.get()})")
}

fun main() {
    task1()
    task2()
    task3()
    task4()
    task5()
    task6()
    task7()
    task8()
    println("\nALL DONE ✅")
}
