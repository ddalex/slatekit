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

package slatekit.entities.core

import slatekit.common.query.IQuery
import slatekit.common.query.Query
import slatekit.meta.Reflector
import kotlin.reflect.KProperty

/**
 * Base entity service with generics to support all CRUD operations.
 * Delegates calls to the entity repository, and also manages the timestamps
 * on the entities for create/update operations
 * @tparam T
 */
open class EntityService<T>(protected val _repo: EntityRepo<T>)
    : IEntityService where T : Entity {

    protected val _enableHooks = false


    /**
     * gets the repo representing the underlying datastore
     * @return
     */
    override fun repo(): IEntityRepo = _repo


    /**
     * creates the entity in the datastore
     * @param entity
     * @return
     */
    fun create(entity: T): Long {
        val finalEntity = applyFieldData(1, entity)
        return _repo.create(finalEntity)
    }


    /**
     * updates the entity in the datastore
     * @param entity
     * @return
     */
    fun update(entity: T): Unit {
        val finalEntity = applyFieldData(2, entity)
        _repo.update(finalEntity)
    }


    /**
     * updates the entity field in the datastore
     * @param id: id of the entity
     * @param field: the name of the field
     * @param value: the value to set on the field
     * @return
     */
    fun update(id: Long, field: String, value: String): Unit {
        val item = get(id)
        item?.let { entity ->
            Reflector.setFieldValue(entity, field, value)
            update(entity)
        }
    }


    /**
     * updates items based on the field name
     * @param prop: The property reference
     * @param value: The value to check for
     * @return
     */
    fun updateByField(prop: KProperty<*>, value: Any): Int {
        return _repo.updateByField(prop.name, value)
    }


    /**
     * updates items by a stored proc
     */
    fun updateByProc(name:String, args:List<Any>? = null): Int {
        return _repo.updateByProc(name, args)
    }


    /**
     * updates items using the query
     */
    fun updateByQuery(query:IQuery): Int {
        return _repo.updateByQuery(query)
    }


    /**
     * deletes the entity
     *
     * @param entity
     */
    fun delete(entity: T?): Unit {
        _repo.delete(entity)
    }


    /**
     * deletes the entity by its id
     * @param id
     * @return
     */
    fun deleteById(id: Long): Boolean = _repo.delete(id)


    /**
     * deletes items based on the field value
     * @param prop: The property reference
     * @param value: The value to check for
     * @return
     */
    fun deleteByField(prop: KProperty<*>, value: Any): Int {
        return _repo.deleteByField(prop.name, value)
    }


    /**
     * updates items using the query
     */
    fun deleteByQuery(query:IQuery): Int {
        return _repo.deleteByQuery(query)
    }


    /**
     * gets the entity from the datastore using the id
     * @param id
     * @return
     */
    fun get(id: Long): T? {
        return _repo.get(id)
    }


    /**
     * gets all the entities from the datastore.
     * @return
     */
    fun getAll(): List<T> {
        return _repo.getAll()
    }


    /**
     * gets the total number of entities in the datastore
     * @return
     */
    override fun count(): Long {
        return _repo.count()
    }


    /**
     * gets the top count entities in the datastore sorted by asc order
     * @param count: Top / Limit count of entities
     * @param desc : Whether to sort by descending
     * @return
     */
    fun top(count: Int, desc: Boolean): List<T> {
        return _repo.top(count, desc)
    }


    /**
     * determines if there are any entities in the datastore
     * @return
     */
    override fun any(): Boolean {
        return _repo.any()
    }


    /**
     * whether this is an empty dataset
     */
    fun isEmpty():Boolean = !any()


    /**
     * saves an entity by either creating it or updating it based on
     * checking its persisted flag.
     * @param entity
     */
    fun save(entity: T?): Unit {
        entity?.let { item ->
            val finalEntity = applyFieldData(3, item)
            _repo.save(finalEntity)
        }
    }


    /**
     * saves all the entities
     *
     * @param items
     */
    fun saveAll(items: List<T>): Unit {
        _repo.saveAll(items)
    }


    /**
     * Gets the first/oldest item
     * @return
     */
    fun first(): T? {
        return _repo.first()
    }


    /**
     * Gets the last/recent item
     * @return
     */
    fun last(): T? {
        return _repo.last()
    }


    /**
     * Gets the most recent n items represented by count
     * @param count
     * @return
     */
    fun recent(count: Int): List<T> {
        return _repo.recent(count)
    }


    /**
     * Gets the most oldest n items represented by count
     * @param count
     * @return
     */
    fun oldest(count: Int): List<T> {
        return _repo.oldest(count)
    }


    /**
     * finds items based on the query
     * @param query
     * @return
     */
    fun find(query: IQuery): List<T> {
        return _repo.find(query)
    }


    /**
     * finds items based on the field value
     * @param prop: The property reference
     * @param value: The value to check for
     * @return
     */
    fun findByField(prop: KProperty<*>, value: Any): List<T> {
        return _repo.findBy(prop.name, "=", value)
    }


    /**
     * finds items by a stored proc
     */
    fun findByProc(name:String, args:List<Any>? = null):List<T>? {
        return _repo.findByProc(name, args)
    }


    /**
     * finds the first item by the query
     */
    fun findFirst(query: IQuery): T? {
        val results = find(query.limit(1))
        return results.firstOrNull()
    }


    /**
     * Hook for derived to apply any other logic/field changes before create/update
     * @param mode
     * @param entity
     * @return
     */
    fun applyFieldData(mode: Int, entity: T): T {
        return entity
    }


    open fun where(prop:KProperty<*>, op:String, value:Any): IQuery = Query().where(prop.name, op, value)
}

