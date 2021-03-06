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

package slatekit.apis.doc


import slatekit.apis.ApiArg
import slatekit.apis.ApiReg
import slatekit.apis.ApiRegAction
import slatekit.common.Serializer
import slatekit.common.console.ConsoleSettings
import slatekit.common.console.ConsoleWriter
import slatekit.common.console.ConsoleWrites
import slatekit.common.nonEmptyOrDefault
import slatekit.common.serialization.SerializerJson
import slatekit.meta.KTypes
import slatekit.meta.Serialization
import java.lang.Math.abs
import kotlin.reflect.KParameter


abstract class Doc : ApiVisit {

    open protected val writer: ConsoleWrites = ConsoleWriter(ConsoleSettings())
    override val docSettings= DocSettings()
    open val pathSeparator = "."
    open val helpSuffix = "?"
    open val helpSeparator = " "


    fun lineBreak(): Unit {
        writer.text("---------------------------------------------------------------", endLine = true)
    }


    override fun onApiError(msg: String): Unit {
        writer.error(msg)
    }


    override fun onVisitSeparator(): Unit {
        writer.line()
    }


    override fun onAreasBegin(): Unit {
        lineBreak()
        writer.title("supported areas: ", endLine = true)
        writer.line()
    }


    override fun onAreasEnd(): Unit {
        writer.text("use {area}$helpSeparator$helpSuffix to list all apis in the area. ")
        writer.url("e.g. sys ?", endLine = true)
        lineBreak()
    }


    override fun onAreaBegin(area: String): Unit {
        writer.highlight(area, endLine = true)
    }


    override fun onAreaEnd(area: String): Unit {
    }


    override fun onApisBegin(area: String): Unit {
        lineBreak()
        writer.title("supported apis: ", endLine = true)
        writer.line()
    }


    override fun onApisEnd(area: String, exampleApi: String?): Unit {
        val eg = exampleApi ?: "sys.models"
        writer.line()
        writer.text("use {area}$pathSeparator{api}$helpSeparator$helpSuffix to list all actions on an api. ")
        writer.url("e.g. $eg ?", endLine = true)
        lineBreak()
    }


    override fun onApiEnd(api: ApiReg): Unit {
        writer.line()
    }


    override fun onArgEnd(arg: ApiArg): Unit {
        writer.line()
    }


    override fun onApiActionSyntax(action: ApiRegAction?): Unit {
        val exampleCli = action?.let{ it -> buildPath(it, null) } ?: "app.movies.last"
        val exampleWeb = action?.let{ it -> buildPath(it, "/") }  ?: "app/movies/last"
        writer.line()
        writer.text("use {area}$pathSeparator{api}$pathSeparator{action}$helpSeparator$helpSuffix to list inputs for an action. ")
        writer.url("cli: $exampleCli ?", endLine = true)
        writer.url("web: $exampleWeb ?", endLine = true)
        lineBreak()
    }


    protected fun getFormattedText(text: String, max: Int): String {
        return if (text.length == max)
            text
        else {
            val remainder = " ".repeat(abs(max - text.length))
            text + remainder
        }
    }


    override fun onApiBegin(api: ApiReg, options: ApiVisitOptions?): Unit {
        writer.highlight(getFormattedText(api.name, (options?.maxLength ?: 0) + 3), endLine = false)
        writer.text(":", endLine = false)
        writer.text(api.desc, endLine = options?.endApiWithLine ?: false)
    }


    override fun onApiBeginDetail(api: ApiReg, options: ApiVisitOptions?): Unit {

        writer.subTitle("AREA   : ", false)
        writer.highlight(api.area, true)

        writer.subTitle("API    : ", false)
        writer.highlight(api.name, endLine = false)
        writer.text(" ", endLine = false)
        writer.text(api.desc, endLine = options?.endApiWithLine ?: false)
    }


    override fun onApiActionBegin(action: ApiRegAction, name: String, options: ApiVisitOptions?): Unit {
        writer.tab(1)
        writer.subTitle(getFormattedText(name, (options?.maxLength ?: 0) + 3), endLine = false)
        writer.text(":", endLine = false)
        writer.text(action.desc, endLine = true)
    }


    override fun onApiActionBeginDetail(action: ApiRegAction, name: String, options: ApiVisitOptions?): Unit {
        writer.subTitle("ACTION : ", false)
        writer.highlight(name, endLine = false)
        writer.text(" ", endLine = false)
        writer.text(action.desc, endLine = true)
        writer.subTitle("PATH   : ", false)
        writer.highlight(buildPath(action.api.area, action.api.name, action.name, null), true)
    }


    override fun onApiActionEnd(action: ApiRegAction, name: String): Unit {
        writer.line()
    }


    override fun onApiActionExample(api: ApiReg, actionName: String, action: ApiRegAction,
                                    args: List<KParameter>): Unit {
        writer.line()

        val exampleCli = buildPath(api.area, api.name, actionName, null)
        val exampleWeb = buildPath(api.area, api.name, actionName, "/" )
        val paramsCli  = args.fold("", { s, arg ->
            s + "-" + arg.name + "=" + KTypes.getTypeExample(arg.name!!, arg.type, "'a bc'") + " "
        })
        val paramsQuery  = args.fold("", { s, arg ->
            s + "&" + arg.name + "=" + KTypes.getTypeExample(arg.name!!, arg.type, "a%20bc")
        })
        val serializer = Serialization.sampler()
        val json = serializer.serialize(args)
        writer.tab(1)
        writer.url("1. cli      : $exampleCli ", endLine = false)
        writer.text(paramsCli, true)

        writer.tab(1)
        writer.url("2. web/url  : $exampleWeb ", endLine = false)
        writer.text(paramsQuery, true)

        if(!actionName.startsWith("get")) {
            writer.tab(1)
            writer.url("3. web/json : $exampleWeb ", endLine = false)
            writer.text(json, true)
        }

        writer.line()
    }


    override fun onArgsBegin(action: ApiRegAction): Unit {
        writer.text("Inputs : ", true)
    }


    override fun onArgBegin(arg: ApiArg, options: ApiVisitOptions?): Unit {
        onArgBegin(arg.name, arg.desc, arg.required, arg.name, arg.defaultVal, arg.eg, options)
    }


    override fun onArgBegin(
            name: String,
            desc: String,
            required: Boolean,
            type: String,
            defaultVal: String,
            eg: String,
            options: ApiVisitOptions?
    ): Unit {
        writer.line()
        writer.tab(2)

        // 1. name of the argument and description
        // e.g. email  : ""
        writer.highlight(getFormattedText(name, (options?.maxLength ?: 10) + 3), endLine = false)
        writer.text(":", endLine = false)
        writer.text(desc.nonEmptyOrDefault("\"\""), endLine = true)

        // 2. required/optional
        writer.tab(2)
        val space = getFormattedText("", (options?.maxLength ?: 10) + 3)
        writer.text(space, endLine = false)

        val txt = if (required) "!" else "?"
        if (required) {
            writer.important(txt, endLine = false)
            writer.text("required : " + type, endLine = false)
        }
        else {
            writer.text(txt, endLine = false)
            writer.text("optional : " + type, endLine = false)
        }
    }


    open fun buildPath(action: ApiRegAction, sep:String? = null) : String {
        return buildPath(action.api.area, action.api.name, action.name, sep)
    }


    open fun buildPath(area:String, api:String, action:String, sep:String? = null) : String {
        val separator = sep ?: pathSeparator
        return area + separator + api + separator + action
    }
}
