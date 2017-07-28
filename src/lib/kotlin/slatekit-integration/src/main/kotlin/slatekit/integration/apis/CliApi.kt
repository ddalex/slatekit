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

package slatekit.integration.apis

import slatekit.apis.ApiConstants

/**
 * Layer on top of the core CliService to provide support for handling command line requests
 * to your APIs using the Protocol independent APIs in the api module.
 * @param creds  : credentials for authentication/authorization purposes.
 * @param ctx    : the app context hosting the selected environment, logger, configs and more
 * @param auth   : the auth provider
 * @param appDir : deprecated
 * @param settings : Settings for the shell functionality
 */
class CliApi(private val creds: slatekit.common.Credentials,
             val ctx: slatekit.core.common.AppContext,
             val auth: slatekit.apis.core.Auth,
             val appDir: String,
             settings: slatekit.core.cli.CliSettings = slatekit.core.cli.CliSettings(),
             apiItems: List<slatekit.apis.ApiReg>? = null
)
    : slatekit.core.cli.CliService(ctx.dirs!!, settings, ctx.app) {

    // api container holding all the apis.
    val apis = slatekit.apis.containers.ApiContainerCLI(ctx, auth, apiItems)


    /**
     * Exposed life-cycle hook for when the shell is starting up.
     */
    override fun onShellStart(): Unit {
        // You don't need to override this as the base method displays help info
        _view.showHelp()
        _writer.highlight("\tStarting up ${_appMeta.about.name} command line")
    }


    /**
     * Exposed life-cycle hook for when the shell is ending/shutting down.
     */
    override fun onShellEnd(): Unit {
        _writer.highlight("\tShutting down ${_appMeta.about.name} command line")
    }


    /**
     * Exposed life-cycle hook to do some work before executing the command
     * @param cmd : The raw user entered command
     * @return
     */
    override fun onCommandBeforeExecute(cmd: slatekit.core.cli.CliCommand): slatekit.core.cli.CliCommand {
        _writer.highlight("\t")
        return cmd
    }


    /**
     * Converts the raw CliCommand the ApiCmd for passing along the API container
     * which will ultimately delegate the call to the respective api action.
     *
     * @param cmd : The raw user entered command.
     * @return
     */
    override fun onCommandExecuteInternal(cmd: slatekit.core.cli.CliCommand): slatekit.core.cli.CliCommand {
        _writer.highlight("Executing ${_appMeta.about.name} api command " + cmd.fullName())

        // Supply the api-key into each command.
        val opts = slatekit.common.InputArgs(mapOf<String, Any>("api-key" to creds.key))
        val apiCmd = slatekit.common.Request.Companion.cli(cmd.line, cmd.args, opts, ApiConstants.ProtocolCLI, cmd)
        return cmd.copy(result = apis.call(apiCmd))
    }


    /**
     * Use case 3d: ( OPTIONAL ) do some stuff after the command execution
     *
     * @param cmd
     * @return
     */
    override fun onCommandAfterExecute(cmd: slatekit.core.cli.CliCommand): slatekit.core.cli.CliCommand {
        return super.onCommandAfterExecute(cmd)
        // Do anything app specific else here.
    }


    override fun showHelp(): Unit {
        _view.showHelp()
        apis.help.help()
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
    override fun showHelpFor(cmd: slatekit.core.cli.CliCommand, mode: Int): Unit {
        when (mode) {
            // 1: {area} ? = help on area
            slatekit.core.cli.CliConstants.VerbPartArea -> {
                apis.help.helpForArea(cmd.args.getVerb(0))
            }
            // 2. {area}.{api} = help on api
            slatekit.core.cli.CliConstants.VerbPartApi  -> {
                apis.help.helpForApi(cmd.args.getVerb(0), cmd.args.getVerb(1))
            }
            // 3. {area}.{api}.{action} = help on api action
            else                                        -> {
                apis.help.helpForAction(cmd.args.getVerb(0), cmd.args.getVerb(1), cmd.args.getVerb(2))
            }
        }
    }


    override fun showResult(result: slatekit.common.Result<Any>): Unit {
        _printer.printResult(result)
    }
}
