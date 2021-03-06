/**
  * <slate_header>
  * author: Kishore Reddy
  * url: https://github.com/kishorereddy/scala-slate
  * copyright: 2015 Kishore Reddy
  * license: https://github.com/kishorereddy/scala-slate/blob/master/LICENSE.md
  * desc: a scala micro-framework
  * usage: Please refer to license on github for more info.
  * </slate_header>
  */

package slate.common


import slate.common.reflect.ReflectedArg
import slate.common.utils.Temp

import scala.collection.mutable.{ListBuffer}
import scala.collection.immutable.{List}
import scala.reflect.runtime.{universe => ru}
import ru._

// http://docs.scala-lang.org/overviews/reflection/overview.html
// https://gist.github.com/ConnorDoyle/7002426
object Reflector {


  /**
    * Creates an instance of the type supplied ( assumes existance of a 0 param constructor )
    * Only works for non-inner classes and for types with 0 parameter constructors.
    *
    * @tparam T
    * @return
    */
  def createInstance[T:TypeTag]() : T = {
    createInstance(typeOf[T]).asInstanceOf[T]
  }


  /**
   * Creates an instance of the type dynamically using the parameters supplied.
    *
    * @param tpe
   * @return
   */
  def createInstance(tpe:Type, args:Option[Seq[_]] = None): AnyRef = {
    val mirror = ru.runtimeMirror(getClass.getClassLoader)
    val clsSym = tpe.typeSymbol.asClass
    val clsMirror = mirror.reflectClass(clsSym)
    val defaultConstructor = getConstructorDefault(tpe)
    val constructorMethod = clsMirror.reflectConstructor(defaultConstructor)
    val instance = if(args.isEmpty) {
      constructorMethod()
    }
    else {
      val params = args.get
      constructorMethod(params: _*)
    }
    //val instance = constructorMethod(args: _*)
    instance.asInstanceOf[AnyRef]
  }


  /**
   * Determines whether or not the type provided is a case class
   * @param instance
   * @return
   */
  def isCaseClass(instance: Any): Boolean = {
    val typeMirror = runtimeMirror(instance.getClass.getClassLoader)
    val instanceMirror = typeMirror.reflect(instance)
    val symbol = instanceMirror.symbol
    symbol.isCaseClass
  }


  /**
   * Determines whether or not the type provided is a case class
   * @param tpe
   * @return
   */
  def isCaseClass(tpe: Type): Boolean = {
    val clsSym = tpe.typeSymbol.asClass
    clsSym.isCaseClass
  }


  /**
   * Gets the default constructor
   * @param tpe
   * @return
   */
  def getConstructorDefault(tpe:Type): scala.reflect.runtime.universe.MethodSymbol = {
   val conSymbol = tpe.decl(ru.termNames.CONSTRUCTOR)
    val defaultConstructor =
      if (conSymbol.isMethod) conSymbol.asMethod
      else {
        val ctors = conSymbol.asTerm.alternatives
        ctors.map { _.asMethod }.find { _.isPrimaryConstructor }.get
      }
    defaultConstructor
  }


  /**
    * gets the scala.reflect.Type of a class using the instance.
    *
    * @param inst
    * @return
    */
  def getTypeFromInstance(inst:Any): Type =
  {
    val m = ru.runtimeMirror(getClass.getClassLoader)
    val im = m.reflect(inst)
    val clsSym = im.symbol
    val tpe = clsSym.selfType
    tpe
  }


  /**
    * Gets a member annotaction of the type supplied. This works only for the
    * class level annotations.
    *
    * @param clsType : The type of the class to check for the annotation
    * @param anoType : The type of the annotation to check/retrieve
    * @return
    */
  def getClassAnnotation(clsType:Type, anoType:Type): Option[Any] =
  {
    val clsSym = clsType.typeSymbol.asClass
    getAnnotation(clsType, anoType, clsSym)
  }


  /**
    * Gets a member annotation of the type supplied.
    * NOTE: This does not yet populate the annotation values
    * TODO: figure out how to fill the annotation values
    *
    * @param clsType   : The type of the class to check for the annotation
    * @param anoType   : The type of the annotation to check/retrieve
    * @param fieldName : The name of the field containing the annotations
    * @return
    */
  def getMemberAnnotation(clsType:Type, anoType:Type, fieldName:String): Option[Any] =
  {
    val memSym = clsType.member(ru.TermName(fieldName)).asMethod
    getAnnotation(clsType, anoType, memSym)
  }


  /**
   * Gets a member annotation of the type supplied.
   * NOTE: This does not yet populate the annotation values
   * TODO: figure out how to fill the annotation values
    *
    * @param clsType   : The type of the class to check for the annotation
   * @param anoType   : The type of the annotation to check/retrieve
   * @return
   */
  def getFieldAnnotation(clsType:Type, anoType:Type, fieldName:String): Option[Any] =
  {
    val fieldSym = clsType.decl(ru.TermName(fieldName)).asTerm.accessed.asTerm
    getAnnotation(clsType, anoType, fieldSym)
  }


  /**
   * Gets a member annotation of the type supplied.
   * NOTE: This does not yet populate the annotation values
   * TODO: figure out how to fill the annotation values
    *
    * @param clsType
   * @param anoType
   * @return
   */
  def getAnnotation(clsType:Type, anoType:Type, mem:Symbol): Option[Any] =
  {
    val annotations = mem.annotations
    val annotation = annotations.find(a => {
      val tree = a.tree
      val tpe = tree.tpe
      val sym = tpe.typeSymbol
      val tsym = anoType.typeSymbol
      //a.tree.tpe == anoType
      sym == tsym
    })
    if(annotation.isEmpty) {
      None
    }
    else {
      val annotationArgs = annotation.get.tree.children.tail
      val annotationInputs = annotationArgs.map(a => {

        // NOTE: could use pattern matching
        val pe0 = a.productElement(0)
        val annoValue = if (pe0.isInstanceOf[ru.Constant])
          a.productElement(0).asInstanceOf[ru.Constant].value
        else if (Option(a.children).fold(false)( c => c.size > 1 && a.productElement(1)
          .isInstanceOf[ru.Literal]))
          a.productElement(1).asInstanceOf[ru.Literal].value.asInstanceOf[Constant].value
        else
          null

        annoValue
      })
      val anoInstance = createInstance(anoType, Some(annotationInputs))
      Option(anoInstance)
    }
  }


  /**
   * Gets the data type of the field name
   * @param tpe
   * @param fieldName
   * @return
   */
  def getFieldType(tpe:Type, fieldName:String): Type =
  {
    val fieldX = tpe.decl(ru.TermName(fieldName)).asTerm.accessed.asTerm
    fieldX.typeSignature.resultType
  }


  /**
   * Used internally
   * @return
   */
  def getFieldTypeString(): Type = {
    getFieldType(typeOf[Temp], "typeString")
  }


  /**
   * Get all the fields of the instance
   * @param cc
   * @return
   */
  def getFields(cc: AnyRef) :Map[String,Any] =
  ( Map[String, Any]() /: cc.getClass.getDeclaredFields) {(a, f) => f.setAccessible(true)
     a + (f.getName -> f.get(cc))
  }


  /**
   * Get fields by the type
   * @tparam T
   * @return
   */
  def getFields[T:TypeTag]():List[MethodSymbol] = getFieldsByType(typeOf[T])


  /**
   * Get fields by the type
   * @param tpe
   * @return
   */
  def getFieldsByType(tpe:Type):List[MethodSymbol] = {
    val fields = tpe.members.sorted.collect {
      case m: MethodSymbol if m.isCaseAccessor => m
    }
    fields
  }


  def getFieldsDeclared(item:Any): List[FieldMirror] =
  {
    val items = item.getClass.getDeclaredFields.map( mem => getField(item, mem.getName))
    items.toList
  }


  /**
    * Gets a field value from the instance
    *
    * @param item: The instance to get the field value from
    * @param fieldName: The name of the field to get
    * @return
    */
  def getField(item:Any, fieldName:String) : FieldMirror =
  {
    val m = ru.runtimeMirror(getClass.getClassLoader)
    val im = m.reflect(item)
    val clsSym = im.symbol
    val tpe = clsSym.selfType
    val fieldX = tpe.decl(ru.TermName(fieldName)).asTerm.accessed.asTerm
    val fmX = im.reflectField(fieldX)
    fmX
  }


  /**
   * Gets a field value from the instance
   *
   * @param item: The instance to get the field value from
   * @param fieldSym: The name of the field to get
   * @return
   */
  def getField(item:Any, fieldSym:TermSymbol) : FieldMirror =
  {
    val m = ru.runtimeMirror(getClass.getClassLoader)
    val im = m.reflect(item)
    val fmX = im.reflectField(fieldSym)
    fmX
  }


  /**
    * Gets a field value from the instance
    *
    * @param item: The instance to get the field value from
    * @param fieldName: The name of the field to get
    * @return
    */
  def getFieldValue(item:Any, fieldName:String) : Any =
  {
    val m = ru.runtimeMirror(getClass.getClassLoader)
    val im = m.reflect(item)
    val clsSym = im.symbol
    val tpe = clsSym.selfType
    val fieldX = tpe.decl(ru.TermName(fieldName)).asTerm.accessed.asTerm
    val fmX = im.reflectField(fieldX)
    val result = fmX.get
    result
  }


  /**
    * Sets a field value in the instance
    *
    * @param item: The instance to set the field value to
    * @param fieldName: The name of the field to set
    * @param v
    */
  def setFieldValue(item:Any, fieldName:String, v:Any) =
  {
    val m = ru.runtimeMirror(getClass.getClassLoader)
    val im = m.reflect(item)
    val clsSym = im.symbol
    val tpe = clsSym.selfType
    val fieldX = tpe.decl(ru.TermName(fieldName)).asTerm.accessed.asTerm
    val fmX = im.reflectField(fieldX)
    fmX.set(v)
  }


  /**
   * Sets a field value in the instance
   *
   * @param item: The instance to set the field value to
   * @param fieldSym: The name of the field to set
   * @param v
   */
  def setFieldValue(item:Any, fieldSym:TermSymbol, v:Any) =
  {
    val m = ru.runtimeMirror(getClass.getClassLoader)
    val im = m.reflect(item)
    val fmX = im.reflectField(fieldSym)
    fmX.set(v)
  }


  /**
   * Gets a method on the instance with the supplied name
    *
    * @param instance
   * @param name
   * @return
   */
  def getMethod(instance:Any, name:String) : Symbol =
  {
    val m = ru.runtimeMirror(getClass.getClassLoader)
    val im = m.reflect(instance)
    val mem = im.symbol.typeSignature.member(ru.TermName(name)).asMethod
    mem
  }


  /**
   * gets a handle to the scala method mirror which can be used for an optimized approach
   * to invoking the method later
    *
    * @param instance
   * @param name
   * @return
   */
  def getMethodMirror(instance:Any, name:String) : MethodMirror =
  {
    val m = ru.runtimeMirror(getClass.getClassLoader)
    val im = m.reflect(instance)
    val mem = im.symbol.typeSignature.member(ru.TermName(name)).asMethod
    val mirror = im.reflectMethod(mem)
    mirror
  }


  /**
   * gets a list of all the method parameters
    *
    * @param mem
   * @return tuple(name:String, type:String, position:Index)
   */
  def getMethodParameters(mem: Symbol): List[ReflectedArg] =
  {
    val list = ListBuffer[ReflectedArg]()
    val args = mem.typeSignature.paramLists
    if(Option(args).fold(false)( a => a.isEmpty)) {
      List[ReflectedArg]()
    }
    else {
      for (arg <- args) {
        arg.indices.foreach( pos => {
          val sym = arg(pos)
          val term = sym.asTerm
          val isDefault = term.isParamWithDefault
          val typeSym = sym.typeSignature.typeSymbol
          val typeAsType = sym.info
          list.append(new ReflectedArg(sym.name.toString, typeSym.name.toString, pos, typeSym, typeAsType, isDefault))
        })
      }
      list.toList
    }
  }


  /**
   * Gets all the methods with the annotations.
   * @param instance
   * @param clsTpe
   * @param anoTpe
   * @param declaredInSelfType
   * @return
   */
  def getMethodsWithAnnotations(instance:AnyRef, clsTpe:Type, anoTpe:Type,
                                declaredInSelfType:Boolean = true):
      List[(String,MethodSymbol,MethodMirror,Any)] =
  {
    val m = ru.runtimeMirror(getClass.getClassLoader)
    val im = m.reflect(instance)

    val matches = ListBuffer[(String,MethodSymbol,MethodMirror,Any)]()
    for(mem <- clsTpe.members)
    {
      // Method + Public + declared in type
      if(mem.isMethod && mem.isPublic && (!declaredInSelfType ||
        ( declaredInSelfType && mem.owner == clsTpe.typeSymbol)))
      {
        val anno = getAnnotation(clsTpe, anoTpe, mem)
        if(anno != None )
        {
          val methodName = mem.name.toString()
          val methodMirror = im.reflectMethod(mem.asMethod)
          matches.append((methodName,mem.asMethod,methodMirror, anno.get))
        }
      }
    }
    matches.toList
  }


  /**
   * Gets all the fields with the supplied annotation
   * @param instance
   * @param clsTpe
   * @param anoTpe
   * @param declaredInSelfType
   * @return
   */
  def getFieldsWithAnnotations(instance:Option[AnyRef], clsTpe:Type, anoTpe:Type,
                               declaredInSelfType:Boolean = true):
      List[(String,TermSymbol,FieldMirror,Any,Type)] =
  {
    val m = ru.runtimeMirror(getClass.getClassLoader)
    val im:Option[InstanceMirror] = instance.map(m.reflect)

    val matches = ListBuffer[(String,TermSymbol,FieldMirror,Any,Type)]()
    for(mem <- clsTpe.members)
    {
      // Method + Public + declared in type
      if(mem.isMethod && mem.isPublic && mem.asMethod.isGetter && (!declaredInSelfType ||
        ( declaredInSelfType && mem.owner == clsTpe.typeSymbol)))
      {
        val fieldSym = mem.asTerm.accessed.asTerm
        val fieldType = mem.typeSignature.resultType
        val anno = getAnnotation(clsTpe, anoTpe, fieldSym)
        if(anno != None )
        {
          val memberName = mem.name.toString()
          val memberMirror:Option[FieldMirror] = im.map( _.reflectField(fieldSym))
          matches.append((memberName,fieldSym, memberMirror.getOrElse(null), anno.get, fieldType))
        }
      }
    }
    matches.toList
  }


  /**
   * calls a method on the instance supplied
   *
   * @param inst
   * @param name
   * @param inputs
   */
  def callMethod(inst:Any, name: String, inputs: Array[Any]):Any =
  {
    val m = ru.runtimeMirror(getClass.getClassLoader)
    val im = m.reflect(inst)
    val mem = im.symbol.typeSignature.member(ru.TermName(name)).asMethod
    val result = if(Option(inputs).isEmpty)
      im.reflectMethod(mem).apply()
    else
      im.reflectMethod(mem).apply(inputs:_*)
    result
  }


  def toFields(item:AnyRef):List[(String,Any)] = {
    val items = new ListBuffer[(String,Any)]

    items.append(("ABOUT", "==================================="))
    for((k,v) <- Reflector.getFields(item)) {
      items.append((k,v))
    }
    items.toList
  }


  /*



  /*
    def createInstanceWithNoParams(tpe:Type, params:Option[List[Any]] = None): Any = {
      val mirror = ru.runtimeMirror(getClass.getClassLoader)
      val clsSym = tpe.typeSymbol.asClass
      val clsMirror = mirror.reflectClass(clsSym)
      val conSym = tpe.decl(ru.termNames.CONSTRUCTOR).asMethod
      val conMirror = clsMirror.reflectConstructor(conSym)
      val instance = if(params.isEmpty) conMirror() else conMirror(params: _*)
      instance
    }


    def createInstance(tpe:Type): Any = {
      val mirror = ru.runtimeMirror(getClass.getClassLoader)
      val clsSym = tpe.typeSymbol.asClass
      val clsMirror = mirror.reflectClass(clsSym)

      val conSym = tpe.decls.filter( s =>
      {
        s.isMethod && s.isConstructor && s.asMethod.paramLists.flatten.isEmpty
      }).toList.apply(0).asMethod

      if(conSym == null)
        return null

      val conMirror = clsMirror.reflectConstructor(conSym)
      val instance = conMirror()
      instance
    }


    def createInstanceWithParams(tpe:Type, args:List[Any]): Any = {
      val mirror = ru.runtimeMirror(getClass.getClassLoader)
      val clsSym = tpe.typeSymbol.asClass
      val clsMirror = mirror.reflectClass(clsSym)
      val conSym = tpe.decl(ru.termNames.CONSTRUCTOR).asMethod
      val conMirror = clsMirror.reflectConstructor(conSym)
      val instance = conMirror(args: _*)
      instance
    }
  */
  def printParams(mem: Symbol): Unit =
  {
    val args = mem.typeSignature.paramLists
    if (args != null && args.size > 0) {
      for (arg <- args) {
        arg.foreach( sym => {
          println(sym.name + " : " + sym.typeSignature.typeSymbol.name)
        })
      }
    }
  }


  def printMethods(tpe:Type, anoTpe:Type): Unit =
  {
    for(mem <- tpe.members)
    {
      // Method + Public + declared in type
      if(mem.isMethod && mem.isPublic && mem.owner == tpe.typeSymbol)
      {
        println("METHOD     : " + mem.fullName)
        println("owner      : " + mem.owner)
        println("name       : " + mem.name)
        println("type sig   : " + mem.typeSignature)
        printParams(mem)
        printAnnotations(tpe, anoTpe, mem)
        println("returns    : " + mem.typeSignature.resultType)
        println()
      }
    }
  }


  def printFields(tpe:Type, anoTpe:Type): Unit =
  {
    for(mem <- tpe.members)
    {
      // Method + Public + declared in type
      if(mem.isMethod && mem.isPublic && mem.asMethod.isGetter && mem.owner == tpe.typeSymbol)
      {
        println("FIELD      : " + mem.fullName)
        println("owner      : " + mem.owner)
        println("name       : " + mem.name)
        println("type sig   : " + mem.typeSignature)
        println("returns    : " + mem.typeSignature.resultType)
        val anno = getAnnotation(tpe, anoTpe, mem.asTerm.accessed.asTerm)
        if(anno != null ) println(anno.toString)
        println()
      }
    }
  }


  def printAnnotations(tpe:Type, anoTpe:Type, mem: Symbol): Unit =
  {
    val annos = mem.annotations
    if(annos != null && annos.size > 0) {
      for (anno <- annos) {
        println(anno.tree.tpe.typeSymbol.name)
        for (child <- anno.tree.children) {
          println(child.productPrefix)
          for (pe <- child.productIterator) {
            println(pe)
          }
        }
      }
    }
  }
  */
}
