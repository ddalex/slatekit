/**
 * <slate_header>
 * url: www.slatekit.com
 * git: www.github.com/code-helix/slatekit
 * org: www.codehelix.co
 * author: Kishore Reddy
 * copyright: 2016 CodeHelix Solutions Inc.
 * license: refer to website and/or github
 * about: A tool-kit, utility library and server-backend
 * mantra: Simplicity above all else
 * </slate_header>
 */

package slatekit.meta

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaField
import slatekit.common.Types

/**
 * Created by kishorereddy on 5/23/17.
 */
object Reflector {

    /**
     * Creates an instance of the type supplied ( assumes existance of a 0 param constructor )
     *
     * @tparam T
     * @return
     */

    @Suppress("UNCHECKED_CAST")
    fun <T> create(cls: KClass<*>): T {
        val con = cls.primaryConstructor!!
        val res = con.call()
        return res as T
    }

    /**
     * Creates an instance of the type supplied ( assumes existance of a 0 param constructor )
     *
     * @tparam T
     * @return
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> createWithArgs(cls: KClass<*>, args: Array<Any?>): T {
        val con = cls.primaryConstructor!!
        val res = con.call(*args)
        return res as T
    }


    fun getFieldValue(inst: Any, name: String): Any? {
        val item = findField(inst, name)
        return item?.getter?.call(inst)
    }


    fun getFieldValue(inst: Any, prop: KProperty<*>): Any? {

        return prop.getter.call(inst)
    }


    @Suppress("UNCHECKED_CAST")
    fun <T> getAnnotationForClass(cls: KClass<*>, anoType: KClass<*>): T {

        val ano = cls.annotations.filter { it -> it.annotationClass == anoType }.first()
        return ano as T
    }


    @Suppress("UNCHECKED_CAST")
    fun <T> getAnnotationForClassOpt(cls: KClass<*>, anoType: KClass<*>): T? {

        val ano = cls.annotations.filter { it -> it.annotationClass == anoType }.firstOrNull()
        return ano as? T
    }


    @Suppress("UNCHECKED_CAST")
    fun <T> getAnnotatedMembers(cls: KClass<*>, anoType: KClass<*>): List<Pair<KCallable<*>, T>> {

        val members = cls.members.map { member ->
            Pair(member, member.annotations.filter { annotation ->
                annotation.annotationClass == anoType
            }.firstOrNull())
        }
        // 1. filter out ones with annotation supplied
        // 2. convert them to type T
        return members.filter { pair -> pair.second != null }
                .map { (first, second) -> Pair(first, second as T) }
    }


    @Suppress("UNCHECKED_CAST")
    fun <T> getAnnotatedMembersOpt(cls: KClass<*>, anoType: KClass<*>, declared:Boolean = true): List<Pair<KCallable<*>, T?>> {

        val members = if(declared) cls.declaredMemberFunctions else cls.memberFunctions
        val filtered = members.filter{ it.visibility == KVisibility.PUBLIC }
                              .map   { Pair( it, it.annotations.filter { anno -> anno.annotationClass == anoType }.firstOrNull() as? T )}
        return filtered
    }


    @Suppress("UNCHECKED_CAST")
    fun <T> getAnnotatedProps(cls: KClass<*>, anoType: KClass<*>): List<Pair<KProperty<*>, T?>> {

        val props = getProperties(cls).map { prop ->
            Pair(prop, prop.annotations.filter { annotation ->
                annotation.annotationClass == anoType
            }.firstOrNull())
        }
        // 1. filter out ones with annotation supplied
        // 2. convert them to type T
        return props.filter { pair -> pair.second != null }
                .map { (first, second) -> Pair(first, second as T) }
    }


    fun setFieldValue(inst: Any, name: String, value: Any) {
        val item = findField(inst, name)
        item?.javaField?.set(inst, value)
    }


    fun findField(inst: Any, name: String): KProperty1<Any, *>? {
        val item = inst.kClass.declaredMemberProperties.find { it.name == name }
        return item
    }


    fun findProperty(cls: KClass<*>, name: String): KProperty<*>? {
        val item = cls.declaredMemberProperties.find { it.name == name }
        return item
    }


    fun getMethod(cls: KClass<*>, name: String): KCallable<*>? {
        val mem = cls.members.find { m -> m.name == name }
        return mem
    }


    fun getMethodArgs(cls: KClass<*>, name: String): Collection<KParameter>? {
        val mem = cls.members.find { m -> m.name == name }
        return mem?.parameters
    }


    fun callMethod(cls: KClass<*>, inst: Any, name: String, args: Array<Any?>): Any? {
        val mem = cls.members.find { m -> m.name == name }
        val params = arrayOf(inst, *args)
        return mem?.call(*params)
    }


    fun getProperties(cls: KClass<*>): List<KProperty<*>> {
        return if (cls.isData) {
            val propsMap = cls.memberProperties.map { it.name to it }.toMap()
            val items = cls.primaryConstructor!!.parameters.map { it -> propsMap[it.name]!! }
            items
        }
        else {
            cls.memberProperties.toList()
        }
    }


    fun isDataClass(cls: KClass<*>): Boolean = cls.isData


    fun getTypeFromProperty(tpe: KProperty<*>): KClass<*> {
        return when (tpe.returnType.toString()) {
            "kotlin.String"            -> KTypes.KStringClass
            "kotlin.Boolean"           -> KTypes.KBoolClass
            "kotlin.Short"             -> KTypes.KShortClass
            "kotlin.Int"               -> KTypes.KIntClass
            "kotlin.Long"              -> KTypes.KLongClass
            "kotlin.Float"             -> KTypes.KFloatClass
            "kotlin.Double"            -> KTypes.KDoubleClass
            "java.time.LocalDate"      -> KTypes.KLocalDateClass
            "java.time.LocalTime"      -> KTypes.KLocalTimeClass
            "java.time.LocalDateTime"  -> KTypes.KLocalDateTimeClass
            "slatekit.common.DateTime" -> KTypes.KDateTimeClass
            else                       -> Any::class
        }
    }


    //    fun findFieldJ(inst:Any, name:String): Field? {
    //        val item = inst.kClass.java.declaredFields.find { it.name == name }
    //        for(m in inst.kClass.java.declaredFields){
    //            if(m.name == name){
    //                println(m)
    //            }
    //        }
    //        return item
    //    }
}


val <T : Any> T.kClass: KClass<T> get() = javaClass.kotlin