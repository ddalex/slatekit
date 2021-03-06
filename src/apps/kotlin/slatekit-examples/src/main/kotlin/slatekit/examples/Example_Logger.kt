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


package slatekit.examples

//<doc:import_required>
import slatekit.common.log.*
import slatekit.common.results.ResultFuncs.ok

//</doc:import_required>

//<doc:import_examples>
import slatekit.core.cmds.Cmd
import slatekit.common.Result
//</doc:import_examples>


class Example_Logger  : Cmd("logger"), LogSupport {

  //<doc:setup>
  // NOTE: The logger is very simple and designed to be extended for customization
  // especially to use a more robust logging system such as :
  // 1. log4net
  // 2. loggly
  // 3. new relic
  override val logger:LoggerBase? = LoggerConsole()


  // Setup a custom logger
  class MyCustomLogger : LoggerBase(Warn)  {


    override val logger:LoggerBase? = LoggerConsole()


    override fun performLog(entry: LogEntry): Unit
    {
      println("custom logger : " + entry.level + " : " + entry.msg)
    }
  }


  //</doc:setup>
  override fun executeInternal(args: Array<String>?) : Result<Any>
  {

    //<doc:examples>
    // Sample exception
    val ex = IllegalArgumentException("Example exception")

    // CASE 1: Different ways to log with the static logger
    // 1. message only
    // 2. message + exception
    // 3. message + exception + tag
    val logger = LoggerConsole(Debug)
    logger.debug("debug with message only")
    logger.info("info with message and exception", ex)
    logger.warn("debug with message, exception, and tag", ex, "APP1")
    logger.fatal("fatal message", tag = "123")

    // CASE: 2 Standard info, warn, error levels available
    // Same overloads ( msg, ex, tag ) are available.
    logger.debug("debug")
    logger.info("info")
    logger.warn("warn")
    logger.error("error")
    logger.fatal("fatal")


    // CASE 3: Log explicitly using log method.
    logger.log(Error, "error", ex, "APP1")


    // CASE 4: You can extend a class with the LogSupportIn trait
    // to add logging methods to any class. The trait expects to have
    // a _log member variable available.
    debug("debug from trait")
    info ("info from trait")
    warn ("warn from trait")
    error("error from trait")
    fatal("fatal from trait")


    // CASE 5: Custom logger ( see setup above )
    // YOu just have to implement the log method.
    val log = MyCustomLogger()
    log.debug("debug from trait")
    log.info ("info from trait")
    log.warn ("warn from trait")
    log.error("error from trait")
    log.fatal("fatal from trait")
    //</doc:examples>

    return ok()
  }
}
