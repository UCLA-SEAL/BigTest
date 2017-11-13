import org.apache.spark.{ SparkContext, SparkConf }
import org.apache.log4j.{ Logger, Level }

import scala.language.postfixOps //for zipWithIndex

object Test11 {

    def main(args: Array[String]): Unit = {

        Logger.getLogger("org").setLevel(Level.OFF)
        Logger.getLogger("akka").setLevel(Level.OFF)

        val conf = new SparkConf()
                    .setAppName("Scala Toy Example 9: Join with a path in one of RDDs")
                    .setMaster("local[4]")
        val sc = new SparkContext(conf)

        val firstRDD = sc.textFile("small_data/tuple1")
                    .map(line => {
                        val parts = line.split(" ")
                        (Integer.parseInt(parts(0)), Integer.parseInt(parts(1)))
                    })
                    .filter(pair => pair._1 < 10)
        println("First RDD ---------------------------")
        println(firstRDD.collect().mkString("\n"))

        val secondRDD = sc.textFile("small_data/tuple2")
                    .map(line => {
                        val parts = line.split(" ")
                        (Integer.parseInt(parts(0)), Integer.parseInt(parts(1)))
                    })
                    .filter(pair => pair._1 < 15)
        println("Second RDD ---------------------------")
        println(secondRDD.collect().mkString("\n"))

        val joined = firstRDD.join(secondRDD)
                    .collect()
                    
        println("Join: "+joined.mkString("\n"))

    }
}