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

package slate.core.shell


case class ShellSettings(
                           argPrefix     :String  = "-"  ,
                           argSeparator  :String  = "="  ,
                           enableLogging :Boolean = false,
                           enableOutput  :Boolean = false,
                           enableStartLog:Boolean = false
                        )
{
}
