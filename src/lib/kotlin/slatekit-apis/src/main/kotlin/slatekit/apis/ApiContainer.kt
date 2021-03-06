package slatekit.apis

import slatekit.apis.core.*
import slatekit.apis.doc.DocConsole
import slatekit.apis.svcs.Format
import slatekit.apis.helpers.ApiHelper
import slatekit.apis.helpers.ApiLookup
import slatekit.apis.helpers.ApiValidator
import slatekit.apis.helpers.Areas
import slatekit.common.args.ArgsFuncs
import slatekit.common.results.ResultFuncs
import slatekit.common.results.ResultFuncs.badRequest
import slatekit.common.results.ResultFuncs.failure
import slatekit.common.results.ResultFuncs.ok
import slatekit.common.results.ResultFuncs.okOrFailure
import slatekit.common.results.ResultFuncs.success
import slatekit.common.results.ResultFuncs.unexpectedError
import slatekit.meta.Reflector
import slatekit.apis.middleware.Rewriter
import slatekit.apis.support.ApiWithMiddleware
import slatekit.common.*
import slatekit.meta.Serialization
import java.io.File

/**
 * This is the core container hosting, managing and executing the protocol independent apis.
 * @param ctx      :
 * @param allowIO  :
 * @param auth     :
 * @param protocol :
 * @param apis     :
 * @param errors   :
 * @param hooks    :
 * @param filters  :
 * @param controls :
 */
open class ApiContainer(
        val ctx: Context,
        val allowIO: Boolean,
        val auth: Auth? = null,
        val protocol: Protocol = AllProtocols,
        val apis: List<ApiReg>? = null,
        val errors: Errors? = null,
        val namer : Namer? = null,
        val rewrites: List<Rewriter>? = null,
        val serializer: ((String,Any?) -> String)? = null,
        val docKey:String? = null,
        val docBuilder: () -> slatekit.apis.doc.Doc = ::DocConsole
) {
    /**
     * The lookup/map for all the areas in the container
     * e.g. Slate Kit apis are in a 3 part route format :
     *
     *    e.g. area/api/action
     *         app/users/activate
     *
     * 1. area  : top level category containing 1 or more apis
     * 2. api   : an api represents a specific resource and has 1 or more actions
     * 3. action: the lowest level endpoint that maps to a method/function.
     *
     * NOTES:
     *
     * 1. The _lookup stores all the top level "areas" in the container
     *    as a mapping between area -> ApiLookup.
     * 2. The ApiLookup contains all the Apis as a mapping between "api" names to
     *    an ApiBase ( which is what you extend from to create your own api )
     * 3. The ApiBase then has a lookup of all "actions" mapped to methods.
     */
    protected val _lookup = Areas(namer).registerAll(apis)


    /**
     * The validator for requests, checking protocol, parameter validation, etc
     */
    protected val _validator = Validation(this)


    /**
     * The error handler that responsible for several expected errors/bad-requests
     * and also to handle unexpected errors
     */
    val errs = errors ?: Errors(null)


    /**
     * The settings for the api ( limited for now )
     */
    val settings = ApiSettings()


    /**
     * The help class to handle help on an area, api, or action
     */
    val help = Help(this, _lookup, docBuilder)


    /**
     * Success flag to indicate to proceeed to call without a filter
     * This is pre-built to avoid rebuilding a static success flag each time
     */
    val proceedOk = ok()


    val formatter = Format()


    val emptyArgs = mapOf<String, Any>()


    fun rename(text:String):String = namer?.name(text)?.text ?: text


    fun register(reg: ApiReg): Unit {
        _lookup.register(reg)
    }


    /**
     * validates the request by checking for the api/action, and ensuring inputs are valid.
     *
     * @param req
     * @return
     */
    fun check(req: Request): Result<Boolean> {
        val result = ApiValidator.validateCall(req, { req -> get(req) })
        return okOrFailure(result.success, msg = result.msg, tag = req.fullName)
    }


    /**
     * gets the api info associated with the request
     * @param cmd
     * @return
     */
    fun get(cmd: Request): Result<ApiRef> {
        return getApi(cmd.area, cmd.name, cmd.action)
    }


    fun sample(cmd: Request, path: File): Result<String> {
        val action = get(cmd)
        val sample = if(action.success) {
                val parameters = action.value!!.action.paramList
                val serializer = Serialization.sampler()
                val text = serializer.serialize(parameters)
                text
        } else "Unable to find command: " + cmd.path

        path.writeText(sample)
        return success("sample call written to : ${path.absolutePath}")
    }


    /**
     * gets the api info associated with the request
     * @param cmd
     * @return
     */
    fun contains(area: String): Boolean {
        val parts = area.split('.')
        return when (parts.size) {
            0    -> false
            1    -> _lookup.contains(parts[0]) || _lookup.contains("", parts[0])
            2    -> _lookup.contains(parts[0], parts[1]) || _lookup.contains("", parts[0], parts[1])
            3    -> _lookup.contains(parts[0], parts[1], parts[2])
            else -> false
        }
    }


    /**
     * calls the api/action associated with the request
     * @param req
     * @return
     */
    fun call(req: Request): Result<Any> {
        val result: Result<Any> = try {
            execute(req)
        }
        catch(ex: Exception) {
            errs.error(ctx, req, this, ex)
        }
        return result
    }


    fun call(area: String, api: String, action: String, verb: String, opts: Map<String, Any>, args: Map<String, Any>): Result<Any> {
        val req = Request.raw(area, api, action, verb, opts, args)
        return call(req)
    }


    /**
     * gets the mapped method associated with the api action.
     * @param area
     * @param name
     * @param action
     * @return
     */
    fun getApi(area: String, name: String, action: String): Result<ApiRef> {
        //if (area.isNullOrEmpty()) return badRequest("api area not supplied")
        if (name.isNullOrEmpty()) return badRequest("api name not supplied")
        if (action.isNullOrEmpty()) return badRequest("api action not supplied")

        val result = _lookup.get(area)?.get(name)?.let { lookup ->
            val apiAction = lookup.get(action)
            apiAction?.let { a ->
                val reg = _lookup.getApi(area, name)
                val instance = _lookup.getInstance(area, name, ctx)
                success(ApiRef(reg, a, instance))
            } ?: badRequest("api route $area $name $action not found")
        } ?: badRequest("api route $area $name $action not found")
        return result
    }


    /**
     * executes the api request in a pipe-line of various checks and validations.
     * @param cmd
     * @return
     */
    protected fun execute(raw: Request): Result<Any> {
        // Case 1: Check for help / discovery
        val helpCheck = isHelp(raw)
        if(helpCheck.isHelp) {
            return buildHelp(raw, helpCheck)
        }

        // Case 2: Check for a rewrites ( e.g. restify get /movies => get /movies/getAll )
        val rewrittenReq = convertRequest(raw)

        // Case 3: Finally check for formats ( e.g. recentMovies.csv => recentMovies -format=csv
        val req = formatter.rewrite(ctx, rewrittenReq, this, emptyArgs)
        var apiReference:ApiRef? = null

        val result = try {
            val info = _validator.validateApi(req)
            info.flatMap { apiRef ->
                apiReference = apiRef
                val pro = _validator.validateProtocol(req, apiRef)
                pro.flatMap { a ->
                    val auth = _validator.validateAuthorization(req, apiRef)
                    auth.flatMap { au ->
                        val md = _validator.validateMiddleware(req, apiRef)
                        md.flatMap { m ->
                            val pm = _validator.validateParameters(req)
                            executeWithMiddleware(req, apiRef)
                        }
                    }
                }
            }
        }
        catch(ex: Exception) {
            val api = _lookup.get(req.area)?.get(req.name)
            handleError(api, apiReference, req, ex)
        }

        // Finally: If the format of the content specified ( json | csv | props )
        // Then serialize it here and return the content
        val finalResult = convertResult(req, result)
        return finalResult
    }


    /**
     * executes the api request factoring in the middleware filters and hooks.
     * @param req
     * @param api
     * @param action
     * @return
     */
    protected open fun executeWithMiddleware(req: Request, apiRef:ApiRef): Result<Any> {
        val instance = apiRef.instance
        val action = apiRef.action

        // Filter
        val proceed = when(instance) {
            is ApiWithMiddleware -> if(instance.isFilterEnabled) instance.onFilter(this.ctx, req, this, action) else proceedOk
            else                 -> proceedOk
        }

        // Ok to call.
        val callResult = if (proceed.success) {

            // Hook: Before
            if (instance is ApiWithMiddleware && instance.isHookEnabled) {
                instance.onBefore(this.ctx, req, this, action)
            }

            // Finally make the call here.
            val result = execute(req, apiRef)

            // Hook: After
            if (instance is ApiWithMiddleware && instance.isHookEnabled) {
                instance.onAfter(this.ctx, req, this, action)
            }

            // Return the result
            result

        }
        else {
            proceed
        }
        return callResult
    }


    protected open fun execute(req: Request, apiRef:ApiRef): Result<Any> {
        // Finally make call.
        val inputs = ApiHelper.fillArgs(apiRef, req, req.args!!, allowIO, this.ctx.enc)
        val returnVal = Reflector.callMethod(apiRef.api.cls, apiRef.instance, apiRef.action.member.name, inputs)

        val result = returnVal?.let { res ->
            if (res is Result<*>) {
                res as Result<Any>
            }
            else {
                success(res)
            }
        } ?: failure()

        // Return the result
        return result
    }


    protected open fun handleError(api: ApiLookup?, apiRef: ApiRef?, cmd: Request, ex: Exception): Result<Any> {
        // OPTION 1: Api level
        return if (apiRef != null && apiRef.instance is ApiWithMiddleware && apiRef.instance.isErrorEnabled) {
            apiRef.instance.onException(this.ctx, cmd, this, ex)
        }
        // OPTION 2: GLOBAL Level custom handler
        else if (errors != null) {
            errs.error(ctx, cmd, this, ex)
        }
        // OPTION 3: GLOBAL Level default handler
        else {
            handleErrorInternally(ctx, cmd, ex)
        }
    }


    /**
     * global handler for an unexpected error ( for derived classes to override )
     *
     * @param ctx    : the application context
     * @param req    : the request
     * @param ex     : the exception
     * @return
     */
    fun handleErrorInternally(ctx: Context, req: Request, ex: Exception): Result<Any> {
        println(ex.message)
        return unexpectedError(msg = "error executing : " + req.path + ", check inputs")
    }


    open fun isHelp(req:Request):Result<String> {

        // Case 3a: Help ?
        return if (ArgsFuncs.isHelp(req.parts, 0)) {
            ResultFuncs.help(msg = "?", tag = req.action)
        }
        // Case 3b: Help on area ?
        else if (ArgsFuncs.isHelp(req.parts, 1)) {
            ResultFuncs.help(msg = "area ?", tag = req.action)
        }
        // Case 3c: Help on api ?
        else if (ArgsFuncs.isHelp(req.parts, 2)) {
            ResultFuncs.help(msg = "area.api ?", tag = req.action)
        }
        // Case 3d: Help on action ?
        else if (ArgsFuncs.isHelp(req.parts, 3)) {
            ResultFuncs.help(msg = "area.api.action ?", tag = req.action)
        }
        else {
            failure<String>()
        }
    }


    /**
     * Handles help request on any part of the api request. Api requests are typically in
     * the format "area.api.action" so you can type help on each part / region.
     * e.g.
     * 1. area ?
     * 2. area.api ?
     * 3. area.api.action ?
     * @param cmd
     * @param mode
     */
    open fun buildHelp(req:Request, result:Result<String>): Result<Content> {
        return if ( !isDocKeyAvailable(req) ) {
            failure("Unauthorized access to API docs")
        }
        else {
            val content = when (result.msg ?: "") {
            // 1: {area} ? = help on area
                "?"          -> {
                    help.help()
                }
            // 2: {area} ? = help on area
                "area ?"     -> {
                    help.helpForArea(req.parts[0])
                }
            // 3. {area}.{api} = help on api
                "area.api ?" -> {
                    help.helpForApi(req.parts[0], req.parts[1])
                }
            // 3. {area}.{api}.{action} = help on api action
                else         -> {
                    help.helpForAction(req.parts[0], req.parts[1], req.parts[2])
                }
            }
            success(Content.html(content))
        }
    }


    protected open fun convertRequest(req: Request): Request {
        val finalRequest = rewrites?.fold(req, { acc, rewriter -> rewriter.rewrite(ctx, acc, this, emptyArgs) })
        return finalRequest ?: req
    }


    /**
     * Finally: If the format of the content specified ( json | csv | props )
     * Then serialize it here and return the content
     */
    protected open fun convertResult(req: Request, result:Result<Any>): Result<Any> {
        return if(result.success && !req.output.isNullOrEmpty()) {
            val finalSerializer = serializer ?: this::serialize
            val serialized = finalSerializer(req.output ?: "", result.value)
            ( result as Success ).copy(data = serialized!!)
        } else {
            result
        }
    }



    /**
     * Explicitly supplied content
     * Return the value of the result as a content with type
     */
    fun serialize(format:String, data:Any?): Any? {

        val content = when (format) {
            ContentTypeCsv .ext  -> Content.csv ( Serialization.csv().serialize(data)  )
            ContentTypeJson.ext  -> Content.json( Serialization.json().serialize(data) )
            ContentTypeProp.ext  -> Content.prop( Serialization.props(true).serialize(data))
            else                 -> data
        }
        return content
    }


    fun isDocKeyAvailable(req:Request):Boolean {
        // Ensure that docs are only available w/ help key
        val docKeyValue = if(req.opts?.containsKey("doc-key") ?: false){
            req.opts?.get("doc-key") ?: ""
        }
        else if(req.args?.containsKey("doc-key") ?: false){
            req.args?.get("doc-key") ?: ""
        }
        else
            ""
        return docKeyValue == docKey
    }


    fun isCliAllowed(cmd: Request, supportedProtocol: String): Boolean =
            supportedProtocol == "*" || supportedProtocol == "cli"

}