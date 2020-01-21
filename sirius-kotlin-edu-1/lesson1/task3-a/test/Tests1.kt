import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaType

class Tests3A {
    @Test
    fun testIntPair1() {
        val clazz = IntPair1::class
        assertTrue("Класс должен содержать неявные методы hashCode(), equals(), toString(), component1(), component2() и copy()", clazz.isData)
        assertTrue("Должно быть как минимум два конструктора", clazz.constructors.size >= 2)
        assertTrue("Должно быть конструктор без параметров", clazz.constructors.any { it.parameters.isEmpty() })
        assertTrue("Должно быть конструктор (Int, Int)", clazz.constructors.any { it.parameters.size == 2 && isInt(it.parameters[0].type) && isInt(it.parameters[1].type)})
        assertEquals("Должно быть два свойства!", 2, clazz.memberProperties.size)
        assertTrue("Должно быть неизменяемое свойство valueX : Int", clazz.memberProperties.any { it !is KMutableProperty<*> && it.name == "valueX" && isInt(it.returnType) })
        assertTrue("Должно быть неизменяемое свойство valueY : Int", clazz.memberProperties.any { it !is KMutableProperty<*> && it.name == "valueY" && isInt(it.returnType) })
        val check = IntPair1()
        assertEquals("Неверная инициализация в конструкторе без параметров", "IntPair1(valueX=-1, valueY=-1)", check.toString())
    }

    private fun isInt(type: KType) = type.javaType.typeName == "int" || type.javaType.typeName == "java.lang.Integer"
}