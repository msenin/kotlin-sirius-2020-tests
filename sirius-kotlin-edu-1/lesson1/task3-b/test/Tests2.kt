import org.junit.Assert.*
import org.junit.Test
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KType
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.javaType

class Tests2 {

    @Test
    fun testIntPair2() {
        val clazz = IntPair2::class
        assertTrue("Класс должен допускать наследование", clazz.isOpen || clazz.isAbstract)
        assertTrue("Должно быть как минимум два конструктора", clazz.constructors.size >= 2)
        assertTrue("Должен быть конструктор (Int, Int)", clazz.constructors.any { it.parameters.size == 2 && isInt(it.parameters[0].type) && isInt(it.parameters[1].type)})
        assertTrue("Должен быть конструктор без параметров", clazz.constructors.any { it.parameters.isEmpty() && "PRIVATE" == it.visibility?.name })
        assertEquals("Должно быть два свойства!", 2, clazz.memberProperties.size)
        assertTrue("Должно быть неизменяемое свойство valueX : Int", clazz.memberProperties.any { it is KMutableProperty<*> && it.name == "valueX" && isInt(it.returnType) })
        assertTrue("Должно быть неизменяемое свойство valueY : Int", clazz.memberProperties.any { it is KMutableProperty<*> && it.name == "valueY" && isInt(it.returnType) })
        assertTrue("Класс должен быть абстрактным, т.к. должен содержать абстрактный метод gcd", clazz.isAbstract)
        assertTrue("Нет требуемого метода sum", clazz.memberFunctions.any { it.name == "sum" && !it.isAbstract && it.isOpen && it.parameters.size == 1 && isInt(it.returnType) } )
        assertTrue("Нет требуемого метода prod", clazz.memberFunctions.any { it.name == "prod" && !it.isAbstract && !it.isOpen && it.parameters.size == 1 && isInt(it.returnType) } )
        assertTrue("Нет требуемого метода gcd", clazz.memberFunctions.any { it.name == "gcd" && it.isAbstract && it.parameters.size == 1 && isInt(it.returnType) } )
    }

    @Test
    fun testDerivedIntPair2() {
        val clazz = DerivedIntPair2::class
        assertTrue("Класс не должен допускать наследование", !clazz.isOpen && !clazz.isAbstract)
        assertTrue("Должен быть конструктор (Int, Int)", clazz.constructors.any { it.parameters.size == 2 && isInt(it.parameters[0].type) && isInt(it.parameters[1].type)})
        assertTrue("Нет требуемого метода gcd", clazz.memberFunctions.any { it.name == "gcd" && !it.isAbstract && it.parameters.size == 1 && isInt(it.returnType) } )
    }

    @Test
    fun testMethods() {
        assertMethodResult("sum", -9, 10, 1, "Неверный ответ для sum(-9,10)")
        assertMethodResult("prod", 10, 10, 100, "Неверный ответ для prod(10,10)")
        assertMethodResult("sum", 0, -10, -10, "Неверный ответ для sum(0,-10)")
        assertMethodResult("prod", 0, -100, 0, "Неверный ответ для prod(0,-100)")
        assertMethodResult("gcd", 30, 18, 6, "Неверный ответ для gcd(30,18)")
    }

    private fun assertMethodResult(methodName: String, x: Int, y: Int, expected: Int, message: String = "") {
        try {
            val kClass = DerivedIntPair2::class
            val instance = kClass.constructors
                    .first {
                        it.parameters.size == 2 &&
                                isInt(it.parameters[0].type) &&
                                isInt(it.parameters[1].type)
                    }
                    .javaConstructor!!.newInstance(x, y)
            val result: Int = kClass.memberFunctions.first { it.name == methodName }.javaMethod!!.invoke(instance) as Int
            assertEquals(message, expected, result)
        }
        catch (e: Exception) {
            fail("Не получается вызвать метод $methodName: $e")
        }
    }

    private fun isInt(type: KType) = type.javaType.typeName == "int" || type.javaType.typeName == "java.lang.Integer"
}