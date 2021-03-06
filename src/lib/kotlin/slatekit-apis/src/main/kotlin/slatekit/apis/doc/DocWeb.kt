package slatekit.apis.doc

import slatekit.apis.ApiAction
import slatekit.apis.ApiReg
import slatekit.apis.ApiRegAction
import slatekit.common.console.ConsoleWrites
import slatekit.common.console.WebWriter


/**
 * Generates help docs on the console.
 */
class DocWeb : Doc() {
    override val pathSeparator = "/"
    override val helpSuffix = "help"
    override val helpSeparator = "/"

    override val writer:ConsoleWrites = WebWriter()

    override fun toString(): String = writer.toString()

    override fun onApiBegin(api: ApiReg, options: ApiVisitOptions?): Unit {
        writer.title(getFormattedText(api.area + "/" + api.name, (options?.maxLength ?: 0) + 3), endLine = false)
        writer.keyValue("desc" , api.desc, false)
        writer.keyValue("route", "${api.area}/${api.name}", false)
        writer.keyValue("area" , api.area, false)
        writer.keyValue("name" , api.name, false)
        writer.keyValue("verb" , api.verb, false)
        writer.keyValue("roles", api.roles, false)
        writer.keyValue("proto", api.protocol, false)
        writer.line()
        writer.line()
    }


    override fun onApiActionBegin(action: ApiRegAction, name: String, options: ApiVisitOptions?): Unit {
        writer.tab(1)
        writer.subTitle(getFormattedText(name, (options?.maxLength ?: 0) + 3), endLine = false)
        writer.keyValue("desc", action.desc, false)
        writer.keyValue("verb", action.verb, false)
        writer.keyValue("roles", action.roles, false)
        writer.keyValue("proto", action.protocol, false)
    }
}
