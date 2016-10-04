package slate.common.app

import slate.common.Reflector
import slate.common.info._

import scala.collection.mutable.ListBuffer

/**
 * Created by kv on 11/4/2015.
 */
class AppMeta {
  var about      = About.none
  var host       = Host.none
  var lang       = Lang.none
  var status     = Status.none
  var start      = StartInfo.none

  /**
    * builds a list of properties fully describing this app by adding
    * all the properties from the about, host and lang fields.
    *
    * @return
    */
  def info() : List[(String,Any)] = {
    val items = new ListBuffer[(String,Any)]

    items.append(("ABOUT", "==================================="))
    for((k,v) <- Reflector.getFields(about)) { items.append((k,v)) }

    items.append(("HOST", "==================================="))
    for((k,v) <- Reflector.getFields(host)) { items.append((k,v)) }

    items.append(("LANG", "==================================="))
    for((k,v) <- Reflector.getFields(lang)) { items.append((k,v)) }

    items.toList
  }
}
