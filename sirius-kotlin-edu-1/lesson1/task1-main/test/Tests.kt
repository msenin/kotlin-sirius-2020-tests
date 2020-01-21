package com.h0tk3y.spbsu.kotlin.course.lesson1

import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class Test {
    companion object {
        val newline = System.getProperty("line.separator")
    }

    private fun testStdout(expected: String, fn: () -> Unit) {
        val oldStdOut = System.out
        try {
            val stream = ByteArrayOutputStream()
            stream.use {
                PrintStream(stream, true, "UTF-8").use { System.setOut(it); fn(); }
            }
            val actual = stream.toByteArray().inputStream().bufferedReader().readText()
            Assert.assertEquals(expected, actual)
        } finally {
            System.setOut(oldStdOut)
        }
    }

    private fun swapIfNeeded(name: String): String {
        val indexOfSpace = name.indexOf(' ')
        return if (indexOfSpace >= 0 && name.count { it == ' '} == 1) {
            name.drop(indexOfSpace + 1) + " " + name.take(indexOfSpace)
        } else {
            return name
        }
    }

    @Test
    fun testGreet() {
        listOf("abc", "", "=123", "John Doe", "One, two, three", "Иванов Иван").forEach {
            testStdout("Hello, ${swapIfNeeded(it)}!$newline") { greet(it) }
        }
    }

    @Test
    fun testSolutionNoUsername() {
        testStdout("Hello, world!$newline") { main(arrayOf()) }
    }

    @Test
    fun testSolutionWithArgs() {
        testStdout("Hello, one!${newline}Hello, two!${newline}Hello, three!$newline") {
            main(arrayOf("one", "two", "three"))
        }
    }
}