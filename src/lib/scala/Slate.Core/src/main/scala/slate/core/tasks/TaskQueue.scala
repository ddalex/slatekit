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
package slate.core.tasks

import java.util.concurrent.atomic.AtomicBoolean

import slate.common.Result
import slate.common.app.AppMeta
import slate.common.queues.QueueSource
import slate.common.results.ResultCode
import slate.common.status._
import scala.annotation.tailrec
import scala.reflect.runtime.universe.{Type,TypeTag,typeOf}

/**
 * Task queue is background task/worker pattern to process items in a queue.
 * This has a few notable design approaches:
 *
 * DESIGN:
 * 1. This can be interrupted
 * 2. You can pause, stop, resume the task
 * 3. This decouples the logic of running your code, states/actions, with scheduling.
 * 4. You can supply our own implementation of statusUpdate to handle
 *    scheduling either via a simple Timer or via Actors
 *
 *
 * OPERATIONS:
 * 1. start  : starts the task and processes the queue items
 * 2. stop   : stops the task completely
 * 3. pause  : pauses processing of queue for x seconds as specified in settings
 * 4. resume : resumes processing of queue
 *
 * It also processes the queue items in a batch.
 *
 * OPTIONS:
 * ( See the TaskSettings for more options )
 * 1. you can set the task to run continuously, or run 1 time for the items in the queue.
 * 2. you can set the pause time can be configured
 * 3. you can set the batch size of the items to process from the queue
 * 4. you you can configure the type of queue the task uses.
 *
 * NOTES:
 * 1. Task always processes the queue items in a batch
 * 2. Task will check for stopped/paused state after a batch is completed.
 * 3. Task goes through a life-cycle of events ( init, exec, end )
 * 4. Task can be stopped paused and resumed which changes the internal status of the task
 * 5. You can query the status of the task via the "status()" method.
 *
 * SETUP:
 * 1. You can derive from TaskQueue and override the processItems method
 * 2. You can
 * @param name
 */
class TaskQueue[T:TypeTag](  name     : String       ,
                  settings : TaskSettings ,
                  meta     : AppMeta      ,
                  args     : Option[Any]  ,
                  val queue: QueueSource
                )
  extends Task(name, settings, meta, args) {


  /**
   * moves the current state to the name supplied and performs a status update
   *
   * @param state
   * @return
   */
  override protected def moveToState(state:RunState):RunStatus = {
    // Change current state via the atomic ref in Task
    super.moveToState(state)
    this.statusUpdate(_runState.get(), true, ResultCode.SUCCESS, None)
    _runStatus.get()
  }


  override protected def process(): RunState = {

    // Get items from queue
    val items = queue.nextBatchAs[T](_settings.batchSize)
    val anyItems = items.isDefined && items.fold(0)( all => all.length) > 0

    // process
    if ( anyItems ) {

      // process
      processItems(items)

      // Indicate to keep going for next check
      RunStateExecuting
    }
    // wait fore more
    else {
      RunStateWaiting
    }
  }


  /**
   * iterates through the batch of items and processes each one, and completes it
   * if successful, or abandons it on failure.
    *
    * @param items
   */
  protected def processItems(items:Option[List[T]]):Unit = {

    items.fold(Unit)( all => {
      all.foreach( item => {
        try {
          processItem(item)
          queue.complete(Some(item))
        }
        catch {
          case ex: Exception => {
            queue.abandon(Some(item))
          }
        }
      })
      Unit
    })
  }


  /**
   * processes a single item. derived classes should implement this.
    *
    * @param item
   */
  protected def processItem(item:T): Unit = {
  }
}
