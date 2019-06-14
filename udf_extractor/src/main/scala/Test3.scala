import org.apache.spark.{ SparkContext, SparkConf }

object Test3 {

    def main(args: Array[String]): Unit = {

        val conf = new SparkConf()
                    .setAppName("Scala Toy Example 1: Add Integers")
                    .setMaster("local[4]")
        val sc = new SparkContext(conf)
        val sum = sc.textFile("input")
                    .map(line => Integer.parseInt(line))
                    .map(x => if(x > 100) x+1 else 0)
                    .filter(x => x > 0)
                    .map(x => if(x < 200) -200 else x)
                    //.reduce(_+_)

        // println("Sum: "+sum)

    }
}