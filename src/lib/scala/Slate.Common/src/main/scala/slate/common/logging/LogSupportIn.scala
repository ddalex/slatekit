/**
<slate_header>
  author: Kishore Reddy
  url: https://github.com/kishorereddy/scala-slate
  copyright: 2015 Kishore Reddy
  license: https://github.com/kishorereddy/scala-slate/blob/master/LICENSE.md
  desc: a scala micro-framework
  usage: Please refer to license on github for more info.
</slate_header>
  */

package slate.common.logging


trait LogSupportIn {

  protected def log(): Option[LoggerBase] = None


  /**
   * Logs an debug message
 *
   * @param msg : The message
   * @param ex : The exception to log
   */
  protected def debug(msg: String, ex: Option[Exception] = None, tag: Option[String] = None):Unit =
  {
    log(Debug, msg, ex, tag)
  }


  /**
   * Logs an info message
 *
   * @param msg : The message
   * @param ex : The exception to log
   */
  protected def info(msg: String, ex: Option[Exception] = None, tag: Option[String] = None):Unit =
  {
    log(Info, msg, ex, tag)
  }


  /**
   * Logs an warning
 *
   * @param msg : The message
   * @param ex : The exception to log
   */
  protected def warn(msg: String, ex: Option[Exception] = None, tag: Option[String] = None):Unit =
  {
    log(Warn, msg, ex, tag)
  }


  /**
   * Logs an error
 *
   * @param msg : The message
   * @param ex : The exception to log
   */
  protected def error(msg: String, ex: Option[Exception] = None, tag: Option[String] = None):Unit =
  {
    log(Error, msg, ex, tag)
  }


  /**
   * Logs an fatal
   *
   * @param msg : The message
   * @param ex : The exception to log
   */
  protected def fatal(msg: String, ex: Option[Exception] = None, tag: Option[String] = None):Unit =
  {
    log(Fatal, msg, ex, tag)
  }


  /**
   * Logs an entry
 *
   * @param level
   * @param msg
   * @param ex
   */
  protected def log(level: LogLevel, msg: String, ex: Option[Exception] = None, tag: Option[String] = None): Unit =
  {
    val logger = log()
    if(logger.isDefined ) {
      logger.get.log( level, msg, ex, tag)
    }
  }
}
