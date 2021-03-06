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
package slate.integration


import slate.common.info._
import slate.common.serialization.{SerializerUtils}
import slate.core.apis.{ApiContainer, Request, Api, ApiAction}
import slate.core.apis.svcs.ApiWithSupport
import slate.core.common.AppContext

@Api(area = "app", name = "info", desc = "api info about the application and host", roles= "admin", auth="key-roles", verb = "post", protocol = "*")
class AppApi( context:AppContext ) extends ApiWithSupport(context)
{

  @ApiAction(name = "", desc= "get info about the application", roles= "@parent", verb = "@parent", protocol = "@parent")
  def app(format:Option[String] = Some("props")):About = {
    context.app.about
  }


  @ApiAction(name = "", desc= "get info about the application", roles= "@parent", verb = "@parent", protocol = "@parent")
  def cmd(cmd:Request):About = {
    println(cmd.fullName)
    context.app.about
  }


  @ApiAction(name = "", desc= "gets info about the language", roles= "@parent", verb = "@parent", protocol = "@parent")
  def lang():Lang = {
    context.app.lang
  }


  @ApiAction(name = "", desc= "gets info about the host", roles= "@parent", verb = "@parent", protocol = "@parent")
  def host():Host = {
    context.app.host
  }


  @ApiAction(name = "", desc= "gets info about the folders", roles= "@parent", verb = "@parent", protocol = "@parent")
  def dirs():Folders = {
    context.dirs.getOrElse(Folders.none)
  }


  @ApiAction(name = "", desc= "gets info about the start up time", roles= "@parent", verb = "@parent", protocol = "@parent")
  def start():StartInfo = {
    context.app.start
  }


  @ApiAction(name = "", desc= "gets info about the status", roles= "@parent", verb = "@parent", protocol = "@parent")
  def status():Status = {
    context.app.status
  }


  @ApiAction(name = "", desc= "gets all info", roles= "@parent", verb = "@parent", protocol = "@parent")
  def all():String = {
    SerializerUtils.asJson(context.app.info())
  }
}
