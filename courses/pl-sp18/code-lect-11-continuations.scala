//
// language FUNC + continuations
// 


//
//  Values
//


abstract class Value {

   def valueError (msg: String) : Nothing = {
       throw new Exception("Runtime error: "+msg)
   }

   def getInt () : Int = 
     valueError("value "+this+" not an integer")

   def getBool () : Boolean = 
     valueError("value "+this+" not a Boolean")

   def getList () : List[Value] = 
     valueError("value "+this+" not a vector")

   // we still need to distinguish primitive operations from functions :/
   def isPrimOp () : Boolean = false
   def apply (args:List[Value]) : Value = 
     valueError("value "+this+" not a function or operation")

   def getParams () : List[String] = 
     valueError("value "+this+" not a function")

   def getSelf () : String = 
     valueError("value "+this+" not a function")

   def getBody () : Exp = 
     valueError("value "+this+" not a function")

   def getEnv () : Env[Value] = 
     valueError("value "+this+" not a function")

}


object VNone extends Value {

  override def toString () : String = "none"
}


class VInteger (val i:Int) extends Value {

  override def toString () : String = i.toString()
  override def getInt () : Int = i
}


class VBoolean (val b:Boolean) extends Value {

  override def toString () : String = b.toString()
  override def getBool () : Boolean = b
}


class VVector (val l:List[Value]) extends Value {

  override def toString () : String = 
     return l.addString(new StringBuilder(), "[ ", " ", " ]").toString()

  override def getList () : List[Value] = l
}


class VPrimOp (val oper : (List[Value]) => Value) extends Value {

  override def toString () : String = "primop(" + oper + ")"

  override def isPrimOp () : Boolean = true
  override def apply (args:List[Value]) : Value = oper(args)
}


class VRecClosure (val self: String, val params: List[String], val body:Exp, val env:Env[Value]) extends Value {

  override def toString () : String = params + " | " + self + " => " + body 

  override def apply (vargs : List[Value]) : Value = {
    var new_env = env.push(self,this)
    for ((p,v) <- params.zip(vargs)) {
      new_env = new_env.push(p,v)
    }
    return body.eval(new_env)
  }

  override def getParams () : List[String] = params
  override def getSelf () : String = self
  override def getBody () : Exp = body
  override def getEnv () : Env[Value] = env
}



//
//  Primitive operations
//

object Ops { 

   def runtimeError (msg: String) : Nothing = {
     throw new Exception("Runtime error: "+msg)
   }

   def checkArgs (vs:List[Value],n:Int) : Unit = {
     if (vs.length != n) { 
       runtimeError("Wrong # of arguments to operation")
     }
   }

   def operPlus (vs:List[Value]) : Value = {
   
      checkArgs(vs,2)
      val v1 = vs(0)
      val v2 = vs(1)
      
      return new VInteger(v1.getInt() + v2.getInt())
   }
   
   
   def operTimes (vs: List[Value]):Value = {
   
      checkArgs(vs,2)
      val v1 = vs(0)
      val v2 = vs(1)
      
      return new VInteger(v1.getInt() * v2.getInt())
   }
   
   
   def operEqual (vs: List[Value]) : Value = {
       
      checkArgs(vs,2)
      val v1 = vs(0)
      val v2 = vs(1)
   
      return new VBoolean(v1.getInt() == v2.getInt())
   }
   
   
   def operLess (vs: List[Value]) : Value = {
   
      checkArgs(vs,2)
      val v1 = vs(0)
      val v2 = vs(1)
   
      return new VBoolean(v1.getBool() < v2.getBool())
   }
   
   
   def operEmpty (vs : List[Value]) : Value = {
  
     checkArgs(vs,1)
     val v = vs(0)
     return new VBoolean(v.getList().length == 0)
   }
   
   
   def operFirst (vs : List[Value]) : Value = {
     checkArgs(vs,1)
     val v = vs(0)
     val l = v.getList()
     if (l.length == 0) {
       runtimeError("Taking first of an empty vector")
     }
     return l(0)
   }
   
   
   def operRest (vs : List[Value]) : Value = {
     checkArgs(vs,1)
     val v = vs(0)
     val l = v.getList()
     if (l.length == 0) {
       runtimeError("Taking rest of an empty vector")
     }
     return new VVector(l.tail)
   }
   
   
   def operCons (vs : List[Value]) : Value = {
     checkArgs(vs,2)
     val item = vs(0)
     val vec = vs(1)
     return new VVector(item::vec.getList())
   }


   def operPrint (vs:List[Value]) : Value = {
     for (v <- vs) {
       print(v)
       print(" ")
     }
     println(" ")
     return VNone
   }

}



//
//  Expressions
//


class Env[A] (val content: List[(String, A)]) { 

      override def toString () : String = {
          var result = ""
	  for (entry <- content) {
	     result = result + "(" + entry._1 + " <- " + entry._2 + ") "
	  }
	  return result
      }

      
      def push (id : String, v : A) : Env[A] =
          // push a single binding (id,v) on top of the environment

          new Env[A]((id,v)::content)


      def lookup (id : String) : A = {
            // lookup value for an identifier in the environment
      
      	  for (entry <- content) {
	      if (entry._1 == id) {
	      	 return entry._2
	      }
	  }
	  throw new Exception("Environment error: unbound identifier "+id)
      }
}


object Names {

  var counter = 0
  def next () : String = {
    val c = counter
    counter = counter + 1
    return "  x"+counter
  }
}

abstract class Exp {

    def error (msg : String) : Nothing = { 
       throw new Exception("Eval error: "+ msg + "\n   in expression " + this)
    }

    def cps (K : Exp) : Exp

    def eval (env : Env[Value]) : Value
}


case class EInteger (val i:Integer) extends Exp {
    // integer literal

    override def toString () : String = 
        "EInteger(" + i + ")"

    def cps (K : Exp) : Exp = 
      new EApply(K, List(this))

    def eval (env:Env[Value]) : Value =
      new VInteger(i)
}


case class EBoolean (val b:Boolean) extends Exp {
    // boolean literal

    override def toString () : String = 
        "EBoolean(" + b + ")"

    def cps (K : Exp) : Exp = 
      new EApply(K, List(this))

    def eval (env:Env[Value]) : Value = 
      new VBoolean(b)
}


case class EVector (val es: List[Exp]) extends Exp {
    // Vectors

   override def toString () : String =
      "EVector" + es.addString(new StringBuilder(),"(", " ", ")").toString()
      
   def cps (K : Exp) : Exp = { 
     // create names for expressions
     val names = es.map((e) => Names.next())
     var result : Exp = new EApply(K, List(EVector(names.map((n) => new EId(n)))))
     for ((e,n) <- es.zip(names)) { 
        result = e.cps(new ERecFunction("", List(n), result))
     }
     return result
   }

   def eval (env : Env[Value]) : Value = {
      val vs = es.map((e:Exp) => e.eval(env))
      return new VVector(vs)
   }
}


case class EIf (val ec : Exp, val et : Exp, val ee : Exp) extends Exp {
    // Conditional expression

    override def toString () : String =
        "EIf(" + ec + "," + et + "," + ee +")"

    def cps (K : Exp) : Exp = {
      val n = Names.next()
      return ec.cps(new ERecFunction("", List(n), new EIf(new EId(n), et.cps(K), ee.cps(K))))
    }
	
    def eval (env:Env[Value]) : Value = { 
        val cv = ec.eval(env)
        if (!cv.getBool()) { 
          return ee.eval(env)
        } else {
          return et.eval(env)
        }
     }
}


case class EId (val id : String) extends Exp {

    override def toString () : String =
        "EId(" + id + ")"

    def cps (K : Exp) : Exp = 
      return new EApply(K, List(this))

    def eval (env : Env[Value]) : Value =
       env.lookup(id)
}


case class EApply (val f: Exp, val args: List[Exp]) extends Exp {
   override def toString () : String =
      "EApply(" + f + "," + args + ")"

   def cps (K : Exp) : Exp = { 
     // create names for expressions
     val names = args.map((e) => Names.next())
     val fname = Names.next()
     val nargs = names.map((n) => new EId(n))
     var result = f.cps(new ERecFunction("", List(fname), new EApply(new EId(fname), K::nargs)))
     for ((e,n) <- args.zip(names)) { 
        result = e.cps(new ERecFunction("", List(n), result))
     }
     return result
   }
      
   def eval (env : Env[Value]) : Value = {
      val vf = f.eval(env)
      val vargs = args.map((e:Exp) => e.eval(env))
      if (vf.isPrimOp()) {
         return vargs.head.apply(List(vf.apply(vargs.tail)))
      } else {
         // defined function
         // push the vf closure as the value bound to identifier self
         var new_env = vf.getEnv().push(vf.getSelf(),vf)
         for ((p,v) <- vf.getParams().zip(vargs)) {
            new_env = new_env.push(p,v)
         }
         return vf.getBody.eval(new_env)
      }
    }
}


case class ERecFunction (val self: String, val params: List[String], val body : Exp) extends Exp {

   override def toString () : String =
     "ERecFunction(" + self + "," + params + "," + body + ")"

   def cps (K : Exp) : Exp = {
      val n = Names.next()
      return new EApply(K, List(new ERecFunction(self, n::params, body.cps(new EId(n)))))
   }
     
   def eval (env : Env[Value]) : Value =
      new VRecClosure(self,params,body,env)
}


case class ELet (val bindings : List[(String,Exp)], val ebody : Exp) extends Exp {

    override def toString () : String =
        "ELet(" + bindings + "," + ebody + ")"

   def cps (K : Exp) : Exp = { 
     // create names for expressions
     val names = bindings.map((_) => Names.next())
     val nbindings = bindings.zip(names).map({ case ((n,e),nnew) => (n,new EId(nnew)) })
     var result : Exp = new ELet(nbindings,ebody.cps(K))
     for (((n,e),nnew) <- bindings.zip(names)) { 
        result = e.cps(new ERecFunction("", List(nnew), result))
     }
     return result
   }

    def eval (env : Env[Value]) : Value = {
        var new_env = env
        for ((n,e) <- bindings) { 
          val v = e.eval(env)
	  new_env = new_env.push(n,v)
	}
	return ebody.eval(new_env)
    }
}



//
// SURFACE SYNTAX (S-expressions)
//


import scala.util.parsing.combinator._


class SExpParser extends RegexParsers { 

   // tokens
   
   def LP : Parser[Unit] = "(" ^^ { s => () }
   def RP : Parser[Unit] = ")" ^^ { s => () }
   def LB : Parser[Unit] = "[" ^^ { s => () }
   def RB : Parser[Unit] = "]" ^^ { s => () } 
   def PLUS : Parser[Unit] = "+" ^^ { s => () }
   def TIMES : Parser[Unit] = "*" ^^ { s => () }
   def INT : Parser[Int] = """-?[0-9]+""".r ^^ { s => s.toInt }
   def IF : Parser[Unit] = "if" ^^ { s => () }
   def ID : Parser[String] = """[a-zA-Z_+*\-:.?=<>!|][a-zA-Z0-9_+\-*:.?=<>!|]*""".r ^^ { s => s }

   def FUN : Parser[Unit] = "fun" ^^ { s => () }
   def LET : Parser[Unit] = "let" ^^ { s => () }


   // grammar

   def atomic_int : Parser[Exp] = INT ^^ { i => new EInteger(i) }

   def atomic_id : Parser[Exp] =
      ID ^^ { s => new EId(s) }

   def atomic : Parser[Exp] =
      ( atomic_int | atomic_id ) ^^ { e => e}
      
   def expr_if : Parser[Exp] =
      LP ~ IF ~ expr ~ expr ~ expr ~ RP ^^
        { case _ ~ _ ~ e1 ~ e2 ~ e3 ~ _ => new EIf(e1,e2,e3) }

   def binding : Parser[(String,Exp)] =
      LP ~ ID ~ expr ~ RP ^^ { case _ ~ n ~ e ~ _ => (n,e) }
      
   def expr_let : Parser[Exp] =
      LP ~ LET ~ LP ~ rep(binding) ~ RP ~ expr ~ RP ^^
           { case _ ~ _ ~ _ ~ bindings ~ _ ~ e2 ~ _ => new ELet(bindings,e2) }

   def expr_vec : Parser[Exp] =
      LB ~ rep(expr) ~ RB ^^ { case _ ~ es ~ _ => new EVector(es) }

   def expr_fun : Parser[Exp] =
      LP ~ FUN ~ LP ~ rep(ID) ~ RP ~ expr ~ RP ^^
        { case _ ~ _ ~ _ ~ params ~ _ ~ e ~ _ => new ERecFunction("",params,e) }

   def expr_funr : Parser[Exp] =
      LP ~ FUN ~ ID ~ LP ~ rep(ID) ~ RP ~ expr ~ RP ^^
        { case _ ~ _ ~ self ~ _ ~ params ~ _ ~ e ~ _ => new ERecFunction(self,params,e) }

   def expr_app : Parser[Exp] =
      LP ~ expr ~ rep(expr) ~ RP ^^ { case _ ~ ef ~ eargs ~ _ => new EApply(ef,eargs) }


   def expr : Parser[Exp] =
      ( atomic | expr_if | expr_vec | expr_fun | expr_funr | expr_let | expr_app) ^^
           { e => e }

   def shell_entry : Parser[ShellEntry] =
      (LP ~ "define" ~ ID ~ expr ~ RP  ^^ { case _ ~ _ ~ n ~ e ~ _  => new SEdefine(n,e) }) |
      (expr ^^ { e => new SEexpr(e) }) |
      ("#show" ~ expr ^^ { case _ ~ e => new SEshow(e) }) |
      ("#quit" ^^ { s => new SEquit() })

}



//
//  Shell 
//

abstract class ShellEntry {

   // abstract class for shell entries
   // (representing the various entries you
   //  can type at the shell)

   def processEntry (env:Env[Value]) : Env[Value]
}


class SEexpr (e:Exp) extends ShellEntry {

   def processEntry (env:Env[Value]) : Env[Value] = {
      val ee = e.cps(new ERecFunction("",List("x"),new EId("x")))
      val v = ee.eval(env)
      println(v)
      return env
   }
}

class SEshow (e:Exp) extends ShellEntry { 

   def processEntry (env:Env[Value]) : Env[Value] = {
      val ee = e.cps(new ERecFunction("",List("x"),new EId("x")))
      println("CPS: "+ee)
      val v = ee.eval(env)
      println(v)
      return env
   }
}

class SEdefine (n:String, e:Exp) extends ShellEntry {

   def processEntry (env:Env[Value]) : Env[Value] = {
      val ee = e.cps(new ERecFunction("",List("x"),new EId("x")))
      val v = ee.eval(env)
      println(n + " defined")
      return env.push(n,v)
   }
}

class SEquit extends ShellEntry {

   def processEntry (env:Env[Value]) : Env[Value] = {

      System.exit(0)
      return env
   }
}


object Shell {

   val parser = new SExpParser

   def parse (input:String) : ShellEntry = {
   
      parser.parseAll(parser.shell_entry, input) match {
         case parser.Success(result,_) => result
         case failure : parser.NoSuccess => throw new Exception("Cannot parse "+input+": "+failure.msg)
      }  
   }


   def time[R](block: => R):R = {
     val t0 = System.currentTimeMillis()
     val result = block
     val t1 = System.currentTimeMillis()
     println("Elapsed time: " + (t1-t0) + "ms")
     result
   }
   
   val nullEnv = new Env[Value](List())
   
   //
   // Standard environment
   //

   val stdEnv = new Env[Value](List(
     ("true",new VBoolean(true)),
     ("false",new VBoolean(false)),
     ("not", new VRecClosure("",List("a"), new EIf(new EId("a"), new EBoolean(false), new EBoolean(true)),nullEnv)),
     ("+", new VPrimOp(Ops.operPlus)),
     ("*", new VPrimOp(Ops.operTimes)),
     ("=", new VPrimOp(Ops.operEqual)),
     ("<", new VPrimOp(Ops.operLess)),
     ("empty?",new VPrimOp(Ops.operEmpty)),
     ("first",new VPrimOp(Ops.operFirst)),
     ("rest",new VPrimOp(Ops.operRest)),
     ("empty",new VVector(List())),
     ("cons",new VPrimOp(Ops.operCons)),
     ("print!",new VPrimOp(Ops.operPrint))
   ))


   def shell () : Unit = {
   
       var env = stdEnv

       println("With CPS")
       while (true) {
          print("TFUNC> ")
          try { 
             val input = scala.io.StdIn.readLine()
             val se = parse(input)
	     env = time { se.processEntry(env) }
          } catch {
             case e : Exception => println(e.getMessage)
          } 
       }
   }

   def main (argv:Array[String]) : Unit = {
       shell()
   }

}
