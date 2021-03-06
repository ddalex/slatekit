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

package slatekit.meta.models


import slatekit.common.DateTime
import slatekit.common.nonEmptyOrDefault
import slatekit.meta.Reflector

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import kotlin.reflect.KClass
import kotlin.reflect.KProperty


/**
 * Stores the schema of a data-model with properties.
 * @param name     :
 * @param fullName :
 * @param dataType :
 */
class Model(val name: String,
            val fullName: String,
            val dataType: KClass<*>? = null,
            val desc: String = "",
            tableName: String = "",
            private val _propList: List<ModelField>? = null) {

    constructor(dataType: KClass<*>) : this(dataType.simpleName!!, dataType.qualifiedName!!, dataType)

    /**
     * The name of the table
     */
    val table = tableName.nonEmptyOrDefault(name)


    /**
     * The field that represents the id
     */
    val idField: ModelField? get() = _propList?.find { p -> p.cat == "id" }


    /**
     * The mapping of property names to the fields.
     */
    val _propMap = _propList?.toHashSet()


    /**
     * whether there are any fields in the model
     * @return
     */
    val any: Boolean get() = size > 0


    /**
     * whether this model has an id field
     * @return
     */
    val hasId: Boolean get() = idField != null


    /**
     * the number of fields in this model.
     * @return
     */
    val size: Int get() = _propList?.size ?: 0


    /**
     * gets the list of fields in this model or returns an emptylist if none
     * @return
     */
    val fields: List<ModelField> get() = _propList ?: listOf<ModelField>()


    /**
     * builds a new model by adding an text field to the list of fields
     * @param name
     * @param desc
     * @param isRequired
     * @param minLength
     * @param maxLength
     * @param storedName
     * @param defaultValue
     * @param tag
     * @param cat
     * @return
     */
    fun add(field: KProperty<*>, desc: String = "", minLength: Int = 0, maxLength: Int = 50, storedName: String? = null,
            defaultValue: String = "", tag: String = "", cat: String = "data"
    ): Model {
        return addField(field.name, Reflector.getTypeFromProperty(field), desc, !field.returnType.isMarkedNullable,
                minLength, maxLength, storedName, defaultValue, tag, cat)
    }


    /**
     * builds a new model by adding an id field to the list of fields
     * @param name
     * @param dataType
     * @param autoIncrement
     * @return
     */
    fun addId(field: KProperty<*>, autoIncrement: Boolean = false): Model {
        return addField(field.name, Reflector.getTypeFromProperty(field), "", true, 0, 0, name, 0, cat = "id")
    }


    /**
     * builds a new model by adding an id field to the list of fields
     * @param name
     * @param dataType
     * @param autoIncrement
     * @return
     */
    fun addId(name: String, dataType: KClass<*>, autoIncrement: Boolean = false): Model {
        return addField(name, Long::class, "", true, 0, 0, name, 0, cat = "id")
    }


    /**
     * builds a new model by adding an text field to the list of fields
     * @param name
     * @param desc
     * @param isRequired
     * @param minLength
     * @param maxLength
     * @param storedName
     * @param defaultValue
     * @param tag
     * @param cat
     * @return
     */
    fun addText(name: String, desc: String = "", isRequired: Boolean = false, minLength: Int = 0,
                maxLength: Int = 50, storedName: String? = null, defaultValue: String = "",
                tag: String = "", cat: String = "data"
    ): Model {
        return addField(name, String::class, desc, isRequired, minLength, maxLength, storedName,
                defaultValue, tag, cat)
    }


    /**
     * builds a new model by adding a bool field to the list of fields
     * @param name
     * @param desc
     * @param isRequired
     * @param storedName
     * @param tag
     * @param cat
     * @return
     */
    fun addBool(name: String, desc: String = "", isRequired: Boolean = false,
                storedName: String? = null, tag: String = "", cat: String = "data"): Model {
        return addField(name, Boolean::class, desc, isRequired, 0, 0, storedName, false, tag, cat)
    }


    /**
     * builds a new model by adding a date field to the list of fields
     * @param name
     * @param desc
     * @param isRequired
     * @param storedName
     * @param tag
     * @param cat
     * @return
     */
    fun addLocalDate(name: String, desc: String = "", isRequired: Boolean = false,
                     storedName: String? = null, tag: String = "", cat: String = "data"): Model {
        return addField(name, LocalDate::class, desc, isRequired, 0, 0, storedName, DateTime.MIN, tag, cat)
    }


    /**
     * builds a new model by adding a time field to the list of fields
     * @param name
     * @param desc
     * @param isRequired
     * @param storedName
     * @param tag
     * @param cat
     * @return
     */
    fun addLocalTime(name: String, desc: String = "", isRequired: Boolean = false,
                     storedName: String? = null, tag: String = "", cat: String = "data"): Model {
        return addField(name, LocalTime::class, desc, isRequired, 0, 0, storedName, DateTime.MIN, tag, cat)
    }


    /**
     * builds a new model by adding a datetime field to the list of fields
     * @param name
     * @param desc
     * @param isRequired
     * @param storedName
     * @param tag
     * @param cat
     * @return
     */
    fun addLocalDateTime(name: String, desc: String = "", isRequired: Boolean = false,
                         storedName: String? = null, tag: String = "", cat: String = "data"): Model {
        return addField(name, LocalDateTime::class, desc, isRequired, 0, 0, storedName, DateTime.MIN, tag, cat)
    }


    /**
     * builds a new model by adding a date field to the list of fields
     * @param name
     * @param desc
     * @param isRequired
     * @param storedName
     * @param tag
     * @param cat
     * @return
     */
    fun addDateTime(name: String, desc: String = "", isRequired: Boolean = false,
                    storedName: String? = null, tag: String = "", cat: String = "data"): Model {
        return addField(name, DateTime::class, desc, isRequired, 0, 0, storedName, DateTime.MIN, tag, cat)
    }


    /**
     * builds a new model by adding a short field to the list of fields.
     * @param name
     * @param desc
     * @param isRequired
     * @param storedName
     * @param tag
     * @param cat
     * @return
     */
    fun addShort(name: String, desc: String = "", isRequired: Boolean = false,
                 storedName: String? = null, tag: String = "", cat: String = "data"): Model {
        return addField(name, Short::class, desc, isRequired, 0, 0, storedName, 0, tag, cat)
    }


    /**
     * builds a new model by adding a new integer field to the list of fields.
     * @param name
     * @param desc
     * @param isRequired
     * @param storedName
     * @param tag
     * @param cat
     * @return
     */
    fun addInt(name: String, desc: String = "", isRequired: Boolean = false,
               storedName: String? = null, tag: String = "", cat: String = "data"): Model {
        return addField(name, Int::class, desc, isRequired, 0, 0, storedName, 0, tag, cat)
    }


    /**
     * builds a new model by adding a new long field to the list of fields.
     * @param name
     * @param desc
     * @param isRequired
     * @param storedName
     * @param tag
     * @param cat
     * @return
     */
    fun addLong(name: String, desc: String = "", isRequired: Boolean = false,
                storedName: String? = null, tag: String = "", cat: String = "data"): Model {
        return addField(name, Long::class, desc, isRequired, 0, 0, storedName, 0, tag, cat)
    }


    /**
     * builds a new model by adding a new double field to the list of fields.
     * @param name
     * @param desc
     * @param isRequired
     * @param storedName
     * @param tag
     * @param cat
     * @return
     */
    fun addFloat(name: String, desc: String = "", isRequired: Boolean = false,
                 storedName: String? = null, tag: String = "", cat: String = "data"): Model {
        return addField(name, Float::class, desc, isRequired, 0, 0, storedName, 0, tag, cat)
    }


    /**
     * builds a new model by adding a new double field to the list of fields.
     * @param name
     * @param desc
     * @param isRequired
     * @param storedName
     * @param tag
     * @param cat
     * @return
     */
    fun addDouble(name: String, desc: String = "", isRequired: Boolean = false,
                  storedName: String? = null, tag: String = "", cat: String = "data"): Model {
        return addField(name, Double::class, desc, isRequired, 0, 0, storedName, 0, tag, cat)
    }


    /**
     * builds a new model by adding a new object field to the list of fields.
     * @param name
     * @param desc
     * @param isRequired
     * @param dataType
     * @param storedName
     * @param defaultValue
     * @return
     */
    fun addObject(name: String, desc: String = "", isRequired: Boolean = false, dataType: KClass<*>,
                  storedName: String? = null, defaultValue: Any? = null): Model {
        return addField(name, dataType, desc, isRequired, 0, 0, storedName, defaultValue)
    }


    /**
     * builds a new model by adding a new field to the list of fields using the supplied fields.
     * @param name
     * @param dataType
     * @param desc
     * @param isRequired
     * @param minLength
     * @param maxLength
     * @param destName
     * @param defaultValue
     * @param tag
     * @param cat
     * @return
     */
    fun addField(
            name: String,
            dataType: KClass<*>,
            desc: String = "",
            isRequired: Boolean = false,
            minLength: Int = -1,
            maxLength: Int = -1,
            destName: String? = null,
            defaultValue: Any? = null,
            tag: String = "",
            cat: String = "data"
    ): Model {
        val field = ModelField.build(name, desc, dataType, isRequired, minLength, maxLength, destName, defaultValue, tag, cat)
        return add(field)
    }


    fun add(field: ModelField): Model {
        val newPropList = _propList?.plus(field) ?: listOf(field)
        return Model(this.name, fullName, this.dataType, desc, table, newPropList)
    }


    /**
     * Standardizes the model by adding all the slatekit standard entity fields which include
     * uniqueId, tag, timestamps, audit fields
     * @return
     */
    fun standardize(): Model {
        addText("uniqueId", isRequired = false, maxLength = 50, tag = "standard", cat = "meta")
        addText("tag", isRequired = false, maxLength = 20, tag = "standard", cat = "meta")
        addDateTime("createdAt", isRequired = false, tag = "standard", cat = "meta")
        addInt("createdBy", isRequired = false, tag = "standard", cat = "meta")
        addDateTime("updatedAt", isRequired = false, tag = "standard", cat = "meta")
        addInt("updatedBy", isRequired = false, tag = "standard", cat = "meta")
        return this
    }


    /**
     * iterates over each model.
     * @param callback
     */
    fun eachField(callback: (ModelField, Int) -> Unit): Unit {
        _propList?.mapIndexed { index, item -> callback(item, index) }
    }
}
