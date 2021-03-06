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

package slate.common.envs

import slate.common.results.ResultSupportIn
import slate.common.{Strings, Result, envs}


/**
  * Store the currently selected environment ( local, dev, qa, stg, prod ) and provides some
  * utility functions to parse an environment
  */
case class Envs(all:List[Env], current:Option[Env] = None) extends EnvSupport with ResultSupportIn {

  /**
   * Name of the currently selected environment e.g. ( dev1, qa1, qa2, beta, prod )
   * @return
   */
  def name: String = current.fold("")( c => c.name )


  /**
   * Environment of the currently selected ( dev, qa, uat, pro )
   * @return
   */
  def env: String = current.fold("")( c => c.mode.name )


  /**
   * The fully qualified name of the currently selected environment ( combines the name + key )
   * @return
   */
  def key: String = current.fold("")( c => c.key )


  /**
   * whether the current environment matches the environment name supplied.
   * @param env
   * @return
   */
  override def isEnv(env: String): Boolean = {
    current.fold(false)( c => c.mode.name == env)
  }


  /**
    * whether the current environment matches the environment  supplied.
    * @param env
    * @return
    */
  override def isEnv(env: EnvMode): Boolean = {
    current.fold(false)( c => c.mode.name == env.name)
  }


  /**
   * selects a new environment and returns a new Envs collection with the s
   * selected environment
   * @param name : Name of the environment
   * @return
   */
  def select(name:String): Envs = {
    val matched = all.filter( item => Strings.isMatch(item.name, name ))
    new Envs(all, Option(matched.head))
  }


  /**
   * validates the environment against the supported
   *
   * @param env
   * @return
   */
  def validate(env:envs.Env): Result[envs.Env] = {
    this.apply(env.name)
  }


  /**
   * validates the environment against the supported
   *
   * @param name
   * @return
   */
  def isValid(name:String): Boolean = {
    this.apply(name).map[Boolean]( e => true).getOrElse(false)
  }


  /**
   * validates the environment against the supported
   *
   * @param name
   * @return
   */
  def apply(name:String): Result[Env] = {
    val matched = all.filter( item => Strings.isMatch(item.name, name ))
    if(Option(matched).fold(false)(m => m.nonEmpty))
      success(matched.head)
    else
      failure(msg = Some(s"Unknown environment name : ${name} supplied"))
  }
}