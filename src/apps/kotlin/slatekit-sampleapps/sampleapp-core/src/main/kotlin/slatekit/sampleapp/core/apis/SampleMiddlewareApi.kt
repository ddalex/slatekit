package slatekit.sampleapp.core.apis

import slatekit.apis.support.ApiWithMiddleware
import slatekit.common.Context
import slatekit.common.Request
import slatekit.common.Result
import slatekit.common.results.ResultFuncs
import slatekit.common.results.ResultFuncs.badRequest
import slatekit.sampleapp.core.models.User


open class SampleMiddlewareApi(
        enableHooks:Boolean = false,
        enableFilter :Boolean = false) : ApiWithMiddleware {

    override val isErrorEnabled :Boolean = true
    override val isHookEnabled = enableHooks
    override val isFilterEnabled = enableFilter


    // Used for demo/testing purposes
    var _user: User = User(0, "", "", "", "", "", "", "", "")
    var onBeforeHookCount = mutableListOf<Request>()
    var onAfterHookCount = mutableListOf<Request>()


    /**
     * Hook for before this api handles any request
     */
    override fun onBefore(context:Context, request:Request, source:Any, target:Any): Unit {
        onBeforeHookCount.add(request)
    }


    /**
     * Hook for after this api handles any request
     */
    override fun onAfter(context:Context, request:Request, source:Any, target:Any): Unit {
        onAfterHookCount.add(request)
    }


    /**
     * Hook to first filter a request before it is handled by this api.
     */
    override fun onFilter(context: Context, request:Request, source:Any, target:Any): Result<Any>  {
        return if(request.action.startsWith("hi")) {
            badRequest<Boolean>("filtered out")
        } else {
            ResultFuncs.ok()
        }
    }


    fun hi(): String = "hi world"


    fun hello(): String = "hello world"
}