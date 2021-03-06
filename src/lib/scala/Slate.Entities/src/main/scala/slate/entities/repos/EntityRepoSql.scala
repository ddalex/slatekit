/**
<slate_header>
  url: www.slatekit.com
  git: www.github.com/code-helix/slatekit
  org: www.codehelix.co
  author: Kishore Reddy
  copyright: 2016 CodeHelix Solutions Inc.
  license: refer to website and/or github
  about: A Scala utility library, tool-kit and server backend.
  mantra: Simplicity above all else
</slate_header>
  */


package slate.entities.repos

import slate.common.databases.Db
import slate.common.query.IQuery
import slate.entities.core.{Entity, EntityMapper, EntityRepo}

import scala.reflect.runtime.universe.Type

/**
  *
  * @param entityType   : The data type of the entity/model
  * @param entityIdType : The data type of the primary key/identity field
  * @param entityMapper : The entity mapper that maps to/from entities / records
  * @param nameOfTable  : The name of the table ( defaults to entity name )
  * @param _db
  * @tparam T
  */
abstract class EntityRepoSql [T >: Null <: Entity ]
    (
      entityType  :Type                       ,
      entityIdType:Option[Type]         = None,
      entityMapper:Option[EntityMapper] = None,
      nameOfTable :Option[String]       = None,
      val _db:Db
    )
    extends EntityRepo[T](entityType, entityIdType, entityMapper, nameOfTable) {


  override def create(entity: T):Long =
  {
    val sql = mapFields(entity, false)
    val id = _db.insert(s"insert into ${tableName} " + sql + ";")
    id
  }


  override def update(entity: T):T =
  {
    val sql = mapFields(entity, true)
    val id = entity.identity()
    sqlExecute(s"update ${tableName} set " + sql + s" where ${idName} = ${id};")
    entity
  }


  /**
    * deletes the entity in memory
    *
    * @param id
    */
  override def delete(id: Long): Boolean =
  {
    val count = sqlExecute(s"delete from ${tableName} where ${idName} = ${id};")
    count > 0
  }


  def get(id: Long) : Option[T] =
  {
    sqlMapOne(s"select * from ${tableName} where ${idName} = ${id};")
  }


  override def getAll() : List[T] =
  {
    val result = sqlMapMany(s"select * from ${tableName};")
    result.getOrElse( List[T]() )
  }


  override def count() : Long  =
  {
    val count = _db.getScalarLong(s"select count(*) from ${tableName};" )
    count
  }


  override def find(query:IQuery):List[T] =
  {
    val filter = query.toFilter()
    val sql = s"select * from ${tableName} where " + filter
    val results = sqlMapMany(sql)
    results.getOrElse( List[T]() )
  }


  protected def scriptLastId(): String =
  {
    ""
  }


  private def sqlExecute(sql: String) :Int =
  {
    _db.update(sql)
  }


  private def sqlMapMany(sql: String) :Option[List[T]] =
  {
    _db.mapMany[T](sql, _entityMapper)
  }


  private def sqlMapOne(sql: String) :Option[T] =
  {
    _db.mapOne[T](sql, _entityMapper)
  }


  private def mapFields(item: Entity, isUpdate: Boolean) : String =
  {
    _entityMapper.mapToSql(item, isUpdate, false)
  }
}
