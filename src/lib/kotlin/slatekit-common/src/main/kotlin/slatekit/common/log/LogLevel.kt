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

package slatekit.common.log


abstract class LogLevel(val name: String, val code: Int) {

  operator fun compareTo(lv: LogLevel): Int = this.code.compareTo(lv.code)
}


object Debug : LogLevel("Debug", 1)
object Info  : LogLevel("Info" , 2)
object Warn  : LogLevel("Warn" , 3)
object Error : LogLevel("Error", 4)
object Fatal : LogLevel("Fatal", 5)