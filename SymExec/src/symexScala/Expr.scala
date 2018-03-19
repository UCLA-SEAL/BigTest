package symexScala

import java.util.HashSet

import NumericUnderlyingType._
import NonNumericUnderlyingType._

object ArithmeticOp extends Enumeration {
    type ArithmeticOp = Value
    val Addition = Value("+")
    val Subtraction = Value("-")
    val Multiplication = Value("*")
    val Division = Value("/")
}


object StringOp extends Enumeration {
    type StringOp = Value
    val Substr = Value("substring")
    val IndexOf = Value("IndexOf")
    val CharAt = Value("CharAt") 
    val Length = Value("Length") 
    val ToInt = Value("VALUEOF") 
    
}

import ArithmeticOp._
import StringOp._


abstract class Expr {
    val actualType: VType

    def toString: String
    def applyEffect(x: SymVar, effect: Expr): Expr
    def checkValidity(ss: SymbolicState): Boolean
    def toZ3Query(initials :HashSet[(String , VType)] ): String
    def deepCopy: Expr

}

abstract class Terminal extends Expr {}

abstract class SymRDD extends Terminal {
    def getName: String
}

case class SymVar(var atype: VType, name: String) extends SymRDD {
    
    val actualType = atype
      /**
     * Setting types of the newly introduced return variable in the effect
     * */
    def setType(_type:VType) {
      atype = _type
    }
    def getName: String = {name}

    override def toString: String = { name /*+": "+actualType*/ }

    override def applyEffect(x: SymVar, effect: Expr): Expr = {
        if (this.equals(x)) effect
        else this //TODO TEST: may need to do a deep-copy instead of returning the same instance, in case of further effects 
    }

    override def checkValidity(ss: SymbolicState): Boolean = {
        ss.isDefined(this)
    }

    override def toZ3Query(initials: HashSet[(String , VType)]): String = {
        var temp_name = name.replaceAll("[^A-Za-z0-9]","")
        initials.add((temp_name , atype))
        temp_name
    }

    override def deepCopy: SymVar = {
        new SymVar(atype, name)
    }
}

case class SymTuple(ttype: Tuple, name: String) extends SymRDD {
    val actualType = ttype

    def getName: String = {name}

    val _1: SymVar = new SymVar(ttype._1Type, name+".key") 
    val _2: SymVar = new SymVar(ttype._2Type, name+".val")

    def getFirst: SymVar = {_1}
    def getSecond: SymVar = {_2}

    override def toString: String = name+"=("+_1.getName+", "+_2.getName+")"

    //TODO: update this for tuple
    override def applyEffect(x: SymVar, effect: Expr): Expr = {
        if (this.equals(x)) effect
        else this
    }

    override def checkValidity(ss: SymbolicState): Boolean = {
        ss.isDefined(_1)
        ss.isDefined(_2)
    }

    def toZ3Query(initials :HashSet[(String , VType)] ): String = {""}

    override def deepCopy: SymTuple = {
        new SymTuple(actualType, name)
    }


}

case class SymOp(atype: VType, op: ArithmeticOp) /*extends Terminal*/ {
    val actualType = atype
    override def toString: String = { op.toString }
}

case class SymStringOp(atype: VType, op: StringOp) /*extends Terminal*/ {
    val actualType = atype
    override def toString: String = { 
      op match {
        case IndexOf =>
              "str.indexof"
        case CharAt =>
              "str.at"
        case Length =>
              "str.len"
        case Substr =>
              "str.substr"
          case ToInt =>
              "str.to.int"
        case _ =>
          throw new NotSupportedRightNow("String Operator no tsupported")
      }
   }
    
}

/*
case class SymTuple(ttype: Tuple, name: String) extends SymVar(ttype, name) {
    override val actualType = ttype

    val _1: SymVar = new SymVar(ttype._1Type, name+".key") 
    val _2: SymVar = new SymVar(ttype._2Type, name+".val")

    def getFirst: SymVar = {_1}
    def getSecond: SymVar = {_2}

    override def toString: String = name+"=("+_1.getName+", "+_2.getName+")"

    //TODO: update this for tuple
    override def applyEffect(x: SymVar, effect: Expr): Expr = {
        if (this.equals(x)) effect
        else this
    }

    override def checkValidity(ss: SymbolicState): Boolean = {
        ss.isDefined(_1)
        ss.isDefined(_2)
    }

    //def toZ3Query(initials :HashSet[(String , VType)] ): String

    override def deepCopy: SymTuple = {
        new SymTuple(actualType, name)
    }

}
*/

case class ConcreteValue(atype: VType, var value: String) extends Expr {
    val actualType = atype
    //check validity of passed ConcreteValue
    assert(atype match {
        case t: Numeric => try {
          if(value.startsWith("CONST_")){
            value  = value.substring(6)
          }
            val v = value.toDouble
            true
        } catch {
            case _: java.lang.NumberFormatException => false
        }
        case t: NonNumeric =>
            if (t.underlyingType == _Boolean) {
                try {
                    val b = value.toBoolean
                    true
                } catch {
                    case _: java.lang.IllegalArgumentException => false
                }
            } else if(t.underlyingType == _String){
              val b = value.toString
              true
            }else true
    })

    override def toString: String = { value.toString /*+" of type "+actualType*/ }

    override def applyEffect(x: SymVar, effect: Expr): Expr = {this}

    override def checkValidity(ss: SymbolicState): Boolean = {true}

    override def toZ3Query(initials :HashSet[(String , VType)]): String = {
     atype match {
        case t: NonNumeric =>
           if(t.underlyingType == _String){
              return s""" "${value}" """
            }
        case _ =>
          //
    }
      return value.toString
    }

    override def deepCopy: ConcreteValue = {
        new ConcreteValue(actualType, value)
    }
}

// case class UnaryExpr(op: SymOp, right: Expr) extends Expr{}

case class NonTerminal(left: Expr, middle: SymOp, right: Expr) extends Expr {
    val op: SymOp = middle

    val leftExpr: Expr = left
    val rightExpr: Expr = right

    //check validity of this partial expression before proceeding
    assert(left != null && right != null)
    assert(op.actualType == leftExpr.actualType && op.actualType == rightExpr.actualType)
    val actualType = op.actualType

    override def toString(): String = {
        left.toString + " " + op.toString + " " + right.toString
    }

    override def applyEffect(x: SymVar, effect: Expr): Expr = {
        new NonTerminal(left.applyEffect(x, effect), op, right.applyEffect(x, effect))
    }

    override def checkValidity(ss: SymbolicState): Boolean = {
        leftExpr.checkValidity(ss) && rightExpr.checkValidity(ss)
    }

    override def toZ3Query(initials :HashSet[(String , VType)]): String = {
        // left.toString + " " + op.toString + " " + right.toString
        s"""(${op.toString}  ${leftExpr.toZ3Query(initials)} ${rightExpr.toZ3Query(initials)} )"""
        //"FIX NON TERMINAL Z3 QUERY"

    }

    override def deepCopy(): NonTerminal = {
        new NonTerminal(left.deepCopy, middle, right.deepCopy)
    }
}



case class StringExpr(obj: Expr, op: SymStringOp , opr:Array[Expr]) extends Expr {

    //check validity of this partial expression before proceeding
    assert(obj != null)
   // assert(op.actualType == obj.actualType )//&& op.actualType == rightExpr.actualType)
    val actualType = op.actualType

    override def toString(): String = {
        obj.toString + " " + op.toString + " " + {
          if(opr.length!=0)
            opr.map(s => s.toString).reduce(_+" "+_)
          else
            ""
        }
    }

    override def applyEffect(x: SymVar, effect: Expr): Expr = {
        new StringExpr(obj.applyEffect(x, effect), op, opr.map(s=> s.applyEffect(x, effect)))
    }

    override def checkValidity(ss: SymbolicState): Boolean = {
        obj.checkValidity(ss) //&& rightExpr.checkValidity(ss)
    }

    override def toZ3Query(initials :HashSet[(String , VType)]): String = {
        s"""( ${op.toString}  ${obj.toZ3Query(initials)} ${
          if(opr.length>0) 
            opr.map(s => s.toZ3Query(initials)).reduce(_ +" " + _)
          else 
              ""
         } )"""
        //"FIX NON TERMINAL Z3 QUERY"

    }

    override def deepCopy(): StringExpr = {
        new StringExpr(obj.deepCopy, op, opr.clone())
    }
}
