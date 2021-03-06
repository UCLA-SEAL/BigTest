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

object ArrayOp extends Enumeration {
  type ArrayOp = Value
  val Select = Value("select")
  val Length = Value("length")
}

object StringOp extends Enumeration {
  type StringOp = Value
  val Substr = Value("substring")
  val IndexOf = Value("IndexOf")
  val CharAt = Value("CharAt")
  val Length = Value("Length")
  val ToInt = Value("VALUEOF")
  val Splitn = Value("splitn")
  val Concat = Value("concat")
  val Split = Value("split")
}

import ArithmeticOp._
import StringOp._
import ArrayOp._

abstract class Expr {
  var actualType: VType
  def toString: String
  def applyEffect(x: SymVar, effect: Expr): Expr
  def checkValidity(ss: SymbolicState): Boolean
  def toZ3Query(initials: Z3QueryState): String
  def deepCopy: Expr
  def replace(thisVar: SymVar, other: SymVar): Expr
  def addSuffix(sfx: String): Expr
}

abstract class Terminal extends Expr {}

case class SymOp(atype: VType, op: ArithmeticOp) /*extends Terminal*/ {
  val actualType = atype
  override def toString: String = {
    op match {
      case Division =>
        "/"
      case Multiplication =>
        "*"
      case Addition =>
        "+"
      case Subtraction =>
        "-"
      case _ =>
        throw new NotSupportedRightNow("String Operator not supported")
    }
  }
}

case class SymStringOp(atype: VType, op: StringOp) /*extends Terminal*/ {
  val actualType = atype
  override def toString: String = {
    op match {
      case IndexOf =>
        "str.indexof"
      case CharAt =>
        "str.at"
      case StringOp.Length =>
        "str.len"
      case Substr =>
        "str.substr"
      case ToInt =>
        "str.to.int"
      case Splitn =>
        "splitn"
      case Concat =>
        "str.++"
      case Split =>
        "str.split"
      case _ =>
        throw new NotSupportedRightNow("String Operator not supported")
    }
  }
}

case class SymArrayOp(atype: VType, op: ArrayOp) /*extends Terminal*/ {
  val actualType = atype
  override def toString: String = {
    op match {
      case Select =>
        "select"
      case ArrayOp.Length =>
        "length"
      case _ =>
        throw new NotSupportedRightNow("String Operator not supported")
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
  var actualType = atype
  //check validity of passed ConcreteValue
  assert(atype match {
    case t: Numeric =>
      try {
        if (value.startsWith("CONST_")) {
          value = value.substring(6)
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
      } else if (t.underlyingType == _String) {
        val b = value.toString
        true
      } else true
  })

  override def toString: String = { value.toString /*+" of type "+actualType*/ }

  override def applyEffect(x: SymVar, effect: Expr): Expr = { this }

  override def checkValidity(ss: SymbolicState): Boolean = { true }

  override def toZ3Query(initials: Z3QueryState): String = {
    atype match {
      case t: NonNumeric =>
        if (t.underlyingType == _String) {
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

  override def replace(thisVar: SymVar, other: SymVar): ConcreteValue = { this }
  override def addSuffix(sfx: String) = { this }
}

// case class UnaryExpr(op: SymOp, right: Expr) extends Expr{}

case class NonTerminal(left: Expr, middle: SymOp, right: Expr) extends Expr {
  val op: SymOp = middle

  val leftExpr: Expr = left
  val rightExpr: Expr = right

  //check validity of this partial expression before proceeding
  assert(left != null && right != null)
  assert(op.actualType == leftExpr.actualType && op.actualType == rightExpr.actualType)
  var actualType = op.actualType

  override def toString(): String = {
    left.toString + " " + op.toString + " " + right.toString
  }

  override def applyEffect(x: SymVar, effect: Expr): Expr = {
    new NonTerminal(left.applyEffect(x, effect), op, right.applyEffect(x, effect))
  }

  override def checkValidity(ss: SymbolicState): Boolean = {
    leftExpr.checkValidity(ss) && rightExpr.checkValidity(ss)
  }

  override def toZ3Query(initials: Z3QueryState): String = {
    // left.toString + " " + op.toString + " " + right.toString
    s"""(${op.toString}  ${leftExpr.toZ3Query(initials)} ${rightExpr
      .toZ3Query(initials)} )"""
    //"FIX NON TERMINAL Z3 QUERY"

  }

  override def deepCopy(): NonTerminal = {
    new NonTerminal(left.deepCopy, middle, right.deepCopy)
  }
  override def replace(thisVar: SymVar, other: SymVar): NonTerminal = {
    new NonTerminal(left.replace(thisVar, other), middle, right.replace(thisVar, other))
  }
  override def addSuffix(sfx: String) = {
    new NonTerminal(left.addSuffix(sfx), middle, right.addSuffix(sfx))
  }
}

case class StringExpr(obj: Expr, op: SymStringOp, opr: Array[Expr]) extends Expr {

  //check validity of this partial expression before proceeding
  assert(obj != null)
  // assert(op.actualType == obj.actualType )//&& op.actualType == rightExpr.actualType)
  var actualType = op.actualType

  override def toString(): String = {
    obj.toString + " " + op.toString + " " + {
      if (opr.length != 0)
        opr.map(s => s.toString).reduce(_ + " " + _)
      else
        ""
    }
  }

  override def applyEffect(x: SymVar, effect: Expr): Expr = {
    new StringExpr(obj.applyEffect(x, effect), op, opr.map(s => s.applyEffect(x, effect)))
  }

  override def checkValidity(ss: SymbolicState): Boolean = {
    obj.checkValidity(ss) //&& rightExpr.checkValidity(ss)
  }
  def addStringToStringArray(initials: Z3QueryState, name: String, idx: Int, del: String, new_name: String) {
    val arr_str = initials.split.getOrElse(name, SplitHandler(Array(), del))
    if (arr_str.str_arr.length <= idx) {
      val temp_arr = new Array[String](idx + 1)
      for (i <- 0 to arr_str.str_arr.length - 1) {
        temp_arr(i) = arr_str.str_arr(i)
      }
      temp_arr(idx) = new_name
      initials.split(name) = SplitHandler(temp_arr, del)
    } else {
      arr_str.str_arr(idx) = new_name
      initials.split(name) = SplitHandler(arr_str.str_arr, del)
    }
  }

  override def toZ3Query(initials: Z3QueryState): String = {
    if (op.op == Splitn) {
      val name = obj.toZ3Query(initials)
      val idx = opr(0).toZ3Query(initials)
      val del = opr(1).toZ3Query(initials)
      val new_name = name + idx
      /// var temp_name = name.replaceAll("[^A-Za-z0-9]","")
      initials.init.add((new_name, NonNumeric(_String)))
      addStringToStringArray(initials, name, idx.toInt, del, new_name)
      new_name
    } else {
      s"""( ${op.toString}  ${obj.toZ3Query(initials)} ${if (opr.length > 0)
        if (opr.length == 2 && op.op == Substr) {
          val a1 = opr(0).toZ3Query(initials)
          val a2 = opr(1).toZ3Query(initials)
          s""" $a1 (- ${a2} ${a1})"""
        } else
          opr.map(s => s.toZ3Query(initials)).reduce(_ + " " + _)
      else
        ""} )"""
    }
    //"FIX NON TERMINAL Z3 QUERY"

  }

  override def deepCopy(): StringExpr = {
    new StringExpr(obj.deepCopy, op, opr.clone())
  }
  override def replace(thisVar: SymVar, other: SymVar): StringExpr = {
    new StringExpr(obj.replace(thisVar, other), op, opr.map(_.replace(thisVar, other)))
  }
  override def addSuffix(sfx: String) = {
    new StringExpr(obj.addSuffix(sfx), op, opr.map(_.addSuffix(sfx)))
  }
}

case class ArrayExpr(obj: Expr, op: SymArrayOp, opr: Array[Expr]) extends Expr {

  //check validity of this partial expression before proceeding
  assert(obj != null)
  assert(obj.isInstanceOf[SymArray], "Array operation on a non-array object")
  // assert(op.actualType == obj.actualType )//&& op.actualType == rightExpr.actualType)
  var actualType = op.actualType

  override def toString(): String = {
    obj.toString + " " + op.toString + " " + {
      if (opr.length != 0)
        opr.map(s => s.toString).reduce(_ + " " + _)
      else
        ""
    }
  }

  override def applyEffect(x: SymVar, effect: Expr): Expr = {
    new ArrayExpr(obj.applyEffect(x, effect), op, opr.map(s => s.applyEffect(x, effect)))
  }

  override def checkValidity(ss: SymbolicState): Boolean = {
    obj.checkValidity(ss) //&& rightExpr.checkValidity(ss)
  }

  override def toZ3Query(initials: Z3QueryState): String = {
    s"""( ${op.toString}  ${obj.toZ3Query(initials)} ${if (opr.length > 0)
      opr.map(s => s.toZ3Query(initials)).reduce(_ + " " + _)
    else
      ""} )"""
  }

  override def deepCopy(): ArrayExpr = {
    new ArrayExpr(obj.deepCopy, op, opr.clone())
  }
  override def replace(thisVar: SymVar, other: SymVar): ArrayExpr = {
    new ArrayExpr(obj.replace(thisVar, other), op, opr.map(_.replace(thisVar, other)))
  }
  override def addSuffix(sfx: String) = {
    new ArrayExpr(obj.addSuffix(sfx), op, opr.map(_.addSuffix(sfx)))
  }
}
