package commutetype

import org.apache.spark.{SparkConf, SparkContext}

/**
  * Created by malig on 1/11/19.
  */
object CommuteTypeFaultWrongJoin {

  def main(args: Array[String]) {
    val conf = new SparkConf()
    conf.setMaster("local[*]")
    conf.setAppName("CommuteTime")


    val data1 = Array(",, ,0,1",
      ",, ,16,1",
      ",, ,41,1",
      " , , ,",
      " , , , ,0",
      " , , , ,",
      "","","",
      ",A, ,-0,1",
      ",A, ,-0,1")

    val data2 = Array(",Palms",
      ",Palms",
      ",Palms",
      "",
      "",
      "",
      "",
      ",",
      ",",
      "",
      "")

    val startTime = System.currentTimeMillis();
    val sc = new SparkContext(conf)
    for(i <- 0 to data1.length-1){
      try{
        val trips = sc.parallelize(Array(data1(i)))
          .map { s =>
            val cols = s.split(",")
            (cols(1), Integer.parseInt(cols(3)) / Integer.parseInt(cols(4)))
          }
        val locations = sc.parallelize(Array(data2(i)))
          .map { s =>
            val cols = s.split(",")
            (cols(0), cols(1))
          }
          .filter(s => s._2.equals("Palms"))
        val joined = trips.fullOuterJoin(locations)  // Injecting fault by using the wrong type of join ==> Should lead to wrong output or crash
        joined
          .map { s =>
            // Checking if speed is < 25mi/hr
            if (s._2._1 > 40) {
              ("car", 1)
            } else if (s._2._1 > 15) {
              ("public", 1)
            } else {
              ("onfoot", 1)
            }
          }
          .reduceByKey(_ + _)
          .collect
          .foreach(println)
      }
      catch {
        case e: Exception =>
          e.printStackTrace()
      }
    }

    println("Time: " + (System.currentTimeMillis() - startTime))

  }


}
