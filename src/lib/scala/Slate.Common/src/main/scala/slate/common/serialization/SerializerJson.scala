/**
  * <slate_header>
  * author: Kishore Reddy
  * url: https://github.com/kishorereddy/scala-slate
  * copyright: 2016 Kishore Reddy
  * license: https://github.com/kishorereddy/scala-slate/blob/master/LICENSE.md
  * desc: a scala micro-framework
  * usage: Please refer to license on github for more info.
  * </slate_header>
  */
package slate.common.serialization

import slate.common.{DateTime, Strings}

class SerializerJson(format:Boolean = true) extends SerializerBase {


  override def onVisitItemBegin(item:Any, pos:Int, total:Int):Unit =
  {
    append("{" + Strings.newline())
    _identer.inc()
  }


  override def onVisitItemEnd(item:Any, pos:Int, total:Int):Unit =
  {
    _identer.dec()
    append("}" + Strings.newline())
  }


  override def onVisitFieldBegin(maxFieldLength:Int, item:Any, name:String, value:Option[Any], pos:Int, total:Int):Unit =
  {
    val finalText = value.fold[String]("null")( actual => {
      actual match {
        case s:String   => Strings.toStringRep(s)
        case s:Int      => s.toString
        case s:Long     => s.toString
        case s:Double   => s.toString
        case s:Boolean  => s.toString.toLowerCase()
        case s:DateTime => "\"" + s.toString() + "\""
        case _          => actual.toString
      }
    })
    val comma = if(pos == total -1 ) "" else ","
    append("\"" + name + "\" : " + finalText + comma + Strings.newline())
  }
}