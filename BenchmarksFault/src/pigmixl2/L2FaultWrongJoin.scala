package pigmixl2

import org.apache.spark.{SparkConf, SparkContext}

/**
  * Created by malig on 5/15/18.
  */

object L2FaultWrongJoin {
  def main(args: Array[String]) {
    val conf = new SparkConf()
    conf.setMaster("local[*]")
    conf.setAppName("CommuteTime")

    val data1 = Array(", , , , , , ",
      "",
      "",
      "A, , , , , , ",
      "A, , , , , , "
    )
    val data2 =
      Array("","","","","")

    val startTime = System.currentTimeMillis();
    val sc = new SparkContext(conf)
    for (i <- 0 to data1.length - 1) {
      try {
        val pageViews = sc.parallelize(Array(data1(i)))
        val powerUsers = sc.parallelize(Array(data2(i)))
        val A = pageViews.map(x => (x.split(",")(0), x.split(",")(6)))
        val alpha = powerUsers.map(x => x.split(",")(0))
        val beta = alpha.map(x => (x, 1))
        val C = A.leftOuterJoin(beta).map(x => (x._1, x._2._1)) //injecting fault by using a wrong type of join ==> Should lead to wrong output
        C.collect.foreach(println)
      } catch {
        case e: Exception =>
          e.printStackTrace()
      }
    }
    println("Time: " + (System.currentTimeMillis() - startTime))
  }
}
