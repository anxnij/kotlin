//1. Создать поток при помощи наследования от интерфейса Runnable;
//2. Создать поток при помощи наследования от Thread;
//3. Создать поток при помощи Callable и Futures;
//4. Создать пуллы потоков для первых трёх заданий;
//5. Создать массив потоков, как только будет готов результат хотя бы в одном из них - отменить остальные;
//6. Создать массив потоков, как только будут готовы результаты первых трёх потоков - отменить остальные;
//7. Создать массив потоков, выводить результат выполнения каждого потока сразу как только он будет готов;
//8. Создать массив потоков, как только очередной поток вернет результат - ставить на паузу на 1 секунду все оставшиеся потоки.

import java.util.concurrent.Callable                      // Интерфейс задачи, которая возвращает результат (V) и может кидать исключение
import java.util.concurrent.Executors                     // Фабрики пулов потоков
import java.util.concurrent.ExecutorCompletionService     // Очередь завершённых задач
import java.util.concurrent.Future                        // Обёртка результата Callable
import java.util.concurrent.TimeUnit                      // Единицы времени для awaitTermination
import java.util.concurrent.ThreadLocalRandom             // Быстрый случайный генератор, отдельный на поток
import java.util.concurrent.atomic.AtomicBoolean          // Атомарный флаг — для кооперативной отмены в примере с паузами
import java.util.concurrent.atomic.AtomicLong// Атомарное число храним момент времени

fun main() {

    //Поток через Runnable
    run {
        val t = Thread(
            Runnable {
                println("[${Thread.currentThread().name}] Runnable: hello")
            },
            "R-1"
        )
        t.start()
        t.join()
    }

    //Поток через наследование от Thread
    run {
        class MyThread(name: String) : Thread(name) {
            override fun run() {
                println("[${currentThread().name}] Thread subclass: hello")
            }
        }
        val t = MyThread("T-1")
        t.start()
        t.join()
    }

    //Поток через Callable + Future
    run {
        val pool = Executors.newSingleThreadExecutor()
        try {
            val fut: Future<Int> = pool.submit(Callable {
                val n = ThreadLocalRandom.current().nextInt(1, 100)
                println("[${Thread.currentThread().name}] compute n^2, n=$n")
                n * n
            })
            println("[main] Future result = ${fut.get()}")
        } finally {
            pool.shutdown()
            pool.awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    // Повторяем Runnable, как Thread и Callable, но через ExecutorService
    run {
        val pool = Executors.newFixedThreadPool(3)
        try {
            pool.execute {
                println("[${Thread.currentThread().name}] Runnable via pool")
            }
            pool.execute {
                println("[${Thread.currentThread().name}] 'Thread-like' via pool")
            }
            val f: Future<String> = pool.submit(Callable {
                println("[${Thread.currentThread().name}] Callable via pool")
                "ok"
            })
            println("[main] Callable from pool = ${f.get()}")
        } finally {
            pool.shutdown()
            pool.awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    //Массив потоков: первый готов — отменить остальные
    run {
        val pool = Executors.newFixedThreadPool(6)
        val ecs = ExecutorCompletionService<String>(pool)
        val futures = mutableListOf<Future<String>>()
        try {
            repeat(6) { idx ->                               // Создаём 6 задач
                val fut = ecs.submit(Callable {
                    val d = ThreadLocalRandom.current().nextLong(100, 800)
                    Thread.sleep(d)
                    "[${Thread.currentThread().name}] done in ${d}ms (idx=$idx)"
                })
                futures += fut
            }
            val first = ecs.take().get()
            println("[main] first ready: $first")
            futures.forEach { if (!it.isDone) it.cancel(true) }
        } finally {
            pool.shutdownNow()
            pool.awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    //Массив потоков: первые три готовы — отменить остальные
    run {
        val total = 8
        val need = 3
        val pool = Executors.newFixedThreadPool(total)
        val ecs = ExecutorCompletionService<String>(pool)
        val futures = mutableListOf<Future<String>>()
        try {
            repeat(total) { idx ->
                val fut = ecs.submit(Callable {
                    val d = ThreadLocalRandom.current().nextLong(100, 1200)
                    Thread.sleep(d)
                    "[${Thread.currentThread().name}] idx=$idx delay=${d}ms"
                })
                futures += fut
            }
            repeat(need) { k ->
                val res = ecs.take().get()
                println("[main] got #${k + 1}: $res")
            }
            futures.forEach { if (!it.isDone) it.cancel(true) }
        } finally {
            pool.shutdownNow()
            pool.awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    //Массив потоков: печатать результат по мере готовности
    run {
        val total = 6
        val pool = Executors.newFixedThreadPool(total)
        val ecs = ExecutorCompletionService<String>(pool)
        try {
            repeat(total) { idx ->
                ecs.submit(Callable {
                    val d = ThreadLocalRandom.current().nextLong(100, 900)
                    Thread.sleep(d)
                    "[${Thread.currentThread().name}] ready idx=$idx after ${d}ms"
                })
            }
            repeat(total) {
                val res = ecs.take().get()
                println("[main] ready: $res")
            }
        } finally {
            pool.shutdown()
            pool.awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    //Массив потоков: каждый новый результат — пауза 1 сек для остальных
    run {
        val total = 6
        val pool = Executors.newFixedThreadPool(total)
        val ecs = ExecutorCompletionService<String>(pool)
        val pauseUntil = AtomicLong(0L)
        val cancelAll = AtomicBoolean(false)
        try {
            repeat(total) { idx ->
                ecs.submit(Callable {
                    val steps = ThreadLocalRandom.current().nextInt(5, 10)
                    var acc = 0L
                    repeat(steps) {
                        if (cancelAll.get() || Thread.currentThread().isInterrupted) {
                            throw InterruptedException("cancelled")
                        }
                        // Кооперативная пауза пока «сейчас» меньше pauseUntil — спим короткими срезами
                        while (System.currentTimeMillis() < pauseUntil.get()) {
                            Thread.sleep(20)
                        }
                        // «Работа» шага: чуть поспать + арифметика
                        Thread.sleep(50)
                        val x = ThreadLocalRandom.current().nextInt(1, 10)
                        val y = ThreadLocalRandom.current().nextInt(1, 10)
                        acc += (x * y + x - y).toLong()
                    }
                    "[${Thread.currentThread().name}] idx=$idx steps=$steps acc=$acc"
                })
            }
            repeat(total) { k ->
                val res = ecs.take().get()
                println("[main] completion #${k + 1}: $res")
                pauseUntil.set(System.currentTimeMillis() + 1_000)
            }
        } finally {
            pool.shutdownNow()
            pool.awaitTermination(5, TimeUnit.SECONDS)
        }
    }
}
