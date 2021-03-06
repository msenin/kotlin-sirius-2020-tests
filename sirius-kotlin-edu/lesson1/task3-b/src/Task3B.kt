/**
 * Объявите класс IntPair2 такой, чтобы он
 *    - допускал наследование
 *    - содержал изменяемые свойства valueX и valueY типа Int,
 *    - имел конструктор с двумя параметрами - valueX и valueY
 *    - имел private конструктор без параметров
 *    - имел метод переопределяемый sum, возвращающий сумму valueX и valueY - Int
 *    - имел метод непереопределяемый prod, возвращающий произведение valueX и valueY - Int
 *    - имел абстрактный метод gcd, возвращающий Int
 */
class IntPair2

/*
 *    Далее объявите класс-наследник DerivedIntPair2 такой, чтобы он
 *    - не допускал дальнейшего наследования
 *    - имел конструктор с двумя параметрами - valueX и valueY
 *    - переопределял метод gcd и реулизовывал в нём вычисление наибольшего общего делителя свойств valueX и valueY
 *     (см. https://younglinux.info/algorithm/euclidean)
 */
class DerivedIntPair2
