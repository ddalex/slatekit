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
package slatekit.sampleapp.core.common

import slatekit.core.app.AppProcess
import slatekit.core.common.AppContext

class AppBase(context: AppContext?) : AppProcess(context)
{
  /**
    * Initialize app
    *
    * NOTES:
    * 1. Base class parses the raw command line args and builds the Args object
    * 2. Base class has command line args Array[String] iniitally supplied as rawArgs
    */
  override fun onInit(): Unit
  {
  }
}
