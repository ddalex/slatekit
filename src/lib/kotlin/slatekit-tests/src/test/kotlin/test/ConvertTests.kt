package test

import org.junit.Test
import slatekit.common.DateTime
import slatekit.common.InputArgs
import slatekit.common.Request
import slatekit.common.encrypt.DecDouble
import slatekit.common.encrypt.DecInt
import slatekit.common.encrypt.DecLong
import slatekit.common.encrypt.DecString
import slatekit.meta.Converter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import test.common.MyEncryptor
import java.util.ArrayList
import kotlin.reflect.KClass
import kotlin.reflect.KType

class ConvertTests {

    //val test = """{ "tstr": "abc", "tbool": false, "tint": 123, "tdoub": 123.45, "tarr": [false, 1, 1.12] }"""

//    @Test fun can_get_fields() {
//        val parser = JSONParser()
//        val json = parser.parse(test) as JSONObject
//        printInfo(json.get("tstr"))
//        printInfo(json.get("tbool"))
//        printInfo(json.get("tint"))
//        printInfo(json.get("tdoub"))
//        printInfo(json.get("tarr"))
//    }


    fun test_basic_types(tstr:String, tbool:Boolean, tshort:Short, tint:Int, tlong:Long, tdoub:Double):Unit {}
    @Test fun can_parse_basictypes(){
        val test = """{ "tstr": "abc", "tbool": false, "tshort": 1, "tint": 12, "tlong": 123, "tdoub": 123.45 }"""
        val converter = Converter()
        val results = converter.convert(this::test_basic_types.parameters, test)
        assert(results[0] == "abc")
        assert(results[1] == false)
        assert(results[2] == 1.toShort())
        assert(results[3] == 12)
        assert(results[4] == 123L)
        assert(results[5] == 123.45)
    }


    fun test_dates(tdate: LocalDate, ttime: LocalTime, tlocaldatetime: LocalDateTime, tdatetime: DateTime):Unit{}
    @Test fun can_parse_dates(){
        val test = """{ "tdate": "2017-07-06", "ttime": "10:30:45", "tlocaldatetime": "2017-07-06T10:30:45", "tdatetime": "201707061030" }"""
        val converter = Converter()
        val results = converter.convert(this::test_dates.parameters, test)
        assert(results[0] == LocalDate.of(2017, 7, 6))
        assert(results[1] == LocalTime.of(10,30,45))
        assert(results[2] == LocalDateTime.of(2017,7,6, 10,30, 45))
        assert(results[3] == DateTime.of(2017,7,6, 10,30))
    }


    fun test_decrypted(decString: DecString, decInt: DecInt, decLong: DecLong, decDouble:DecDouble):Unit {}
    @Test fun can_parse_decrypted(){

        val decStr = MyEncryptor.encrypt("abc123")
        val decInt = MyEncryptor.encrypt("123")
        val decLong = MyEncryptor.encrypt("12345")
        val decDoub = MyEncryptor.encrypt("12345.67")

        val test = """{ "decString": "$decStr", "decInt": "$decInt", "decLong": "$decLong", "decDouble": "$decDoub" }"""
        val converter = Converter(MyEncryptor)
        val results = converter.convert(this::test_decrypted.parameters, test)
        assert((results[0] as DecString).value == "abc123")
        assert((results[1] as DecInt).value == 123)
        assert((results[2] as DecLong).value == 12345L)
        assert((results[3] as DecDouble).value == 12345.67)
    }


    fun test_arrays(strings: List<String>, bools:List<Boolean>, ints:List<Int>, longs:List<Long>, doubles:List<Double>):Unit {}
    @Test fun can_parse_arrays(){
        val test = """{ "strings": ["a", "b", "c"], "bools": [true, false, true], "ints": [1,2,3], "longs": [100,200,300], "doubles": [1.2,3.4,5.6] }"""
        val converter = Converter(MyEncryptor)
        val results = converter.convert(this::test_arrays.parameters, test)
        assert((results[0] as List<String>)[0] == "a")
        assert((results[0] as List<String>)[1] == "b")
        assert((results[0] as List<String>)[2] == "c")
        assert((results[1] as List<Boolean>)[0] == true)
        assert((results[1] as List<Boolean>)[1] == false)
        assert((results[1] as List<Boolean>)[2] == true)
        assert((results[2] as List<Int>)[0] == 1)
        assert((results[2] as List<Int>)[1] == 2)
        assert((results[2] as List<Int>)[2] == 3)
        assert((results[3] as List<Long>)[0] == 100L)
        assert((results[3] as List<Long>)[1] == 200L)
        assert((results[3] as List<Long>)[2] == 300L)
        assert((results[4] as List<Double>)[0] == 1.2)
        assert((results[4] as List<Double>)[1] == 3.4)
        assert((results[4] as List<Double>)[2] == 5.6)
    }


    data class SampleObject1(val tstr:String, val tbool:Boolean, val tshort:Short, val tint:Int, val tlong:Long, val tdoub:Double)
    fun test_object(sample1:SampleObject1):Unit{}
    @Test fun can_parse_object(){
        val test = """{ "sample1": { "tstr": "abc", "tbool": false, "tshort": 1, "tint": 12, "tlong": 123, "tdoub": 123.45 } }"""
        val converter = Converter()
        val results = converter.convert(this::test_object.parameters, test)
        assert(results[0] == SampleObject1("abc", false, 1, 12, 123, 123.45))
    }


    fun test_object_list(items:List<SampleObject1>):Unit{}
    @Test fun can_parse_object_lists(){
        val test = """{ "items":
        [
            { "tstr": "abc", "tbool": false, "tshort": 1, "tint": 12, "tlong": 123, "tdoub": 123.45 },
            { "tstr": "def", "tbool": true , "tshort": 2, "tint": 34, "tlong": 456, "tdoub": 678.91 }
        ]}"""
        val converter = Converter()
        val inputs = converter.convert(this::test_object_list.parameters, test)
        val results = inputs.get(0) as ArrayList<*>
        println(results)
        assert(results[0] == SampleObject1("abc", false, 1, 12, 123, 123.45))
        assert(results[1] == SampleObject1("def", true , 2, 34, 456, 678.91))
    }


    data class NestedObject1(val name:String, val items:List<SampleObject1>)
    fun test_nested_object_list(str:String, item:NestedObject1):Unit{}
    @Test fun can_parse_object_lists_nested(){
        val test = """{
        "str"  : "abc",
        "item":
            {
                "name": "nested_objects",
                "items":
                [
                    { "tstr": "abc", "tbool": false, "tshort": 1, "tint": 12, "tlong": 123, "tdoub": 123.45 },
                    { "tstr": "def", "tbool": true , "tshort": 2, "tint": 34, "tlong": 456, "tdoub": 678.91 }
                ]
            }
        }"""
        val converter = Converter()
        val results = converter.convert(this::test_nested_object_list.parameters, test)
        val item = results[1] as NestedObject1
        assert(results[0] == "abc")
        assert(item.items[0] == SampleObject1("abc", false, 1, 12, 123, 123.45))
        assert(item.items[1] == SampleObject1("def", true , 2, 34, 456, 678.91))
    }


    fun test_custom_converter(tstr:String, tbool:Boolean, req: Request):Unit {}
    @Test fun can_parse_custom_types(){
        val test = """{ "tstr": "abc", "tbool": false }"""
        val req = Request("a.b.c", listOf("a", "b", "c"), "cli", "post", InputArgs(mapOf()), InputArgs(mapOf()))
        val converter = Converter(null, mapOf("slatekit.common.Request" to { json, tpe -> req }))
        val results = converter.convert(this::test_custom_converter.parameters, test)
        assert(results[0] == "abc")
        assert(results[1] == false)
        assert(results[2] == req)
    }


    @Test fun can_check_types(){
        val params = this::test_arrays.parameters
        val first = params[0]
        println(first.type)
        val tpe = first.type
        val listCls = tpe.classifier as KClass<*>
        println(listCls == List::class)
        val paramType: KType = tpe.arguments[0].type!!

        val cls = tpe.classifier as KClass<*>
        println(cls.toString())
        println(cls.qualifiedName)
    }


    fun printInfo(item:Any?):Unit {
        println(item)
    }
}