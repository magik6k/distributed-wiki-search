import eu.devtty.ipld.util.IPLDLink
import eu.devtty.mboard.AppLauncher
import io.scalajs.JSON

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.util.hashing.MurmurHash3
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js.JSConverters._

@JSExportTopLevel("WikiSearch")
object WikiSearch {
  js.Dynamic.global.search = (s: String) => this.search(s)
  implicit val ipfs = AppLauncher.ipfs

  private val dict = ipfs.dag.get("zdpuAqN6LUsggXfSWDhAzmnLAkWFztsR1w1QhKr7mBp2wfBMK").map(_.value.asInstanceOf[js.Dictionary[IPLDLink]])

  @JSExport
  def search(rawSearch: String): js.Promise[js.Array[String]] = {
    val wordWeights = scala.collection.mutable.Map[String, Double]()

    def addWeight(word: String, weight: Double): Unit = {
      val oldWeight = wordWeights.getOrElse(word, 0d)
      wordWeights(word) = oldWeight + weight
    }


    //println(s"S:$rawSearch")

    val searchWords = rawSearch.toLowerCase.replaceAll("""_\(\)-,""", " ").split(" ")
    searchWords.foreach(word => addWeight(word, word.length.toDouble / rawSearch.length))

    val candidateArticles = scala.collection.mutable.Map[String, Double]()

    val queries = searchWords.map(dictGet).map(futures => futures.map {
      case (word, candidates) =>
        //println(word + " candidates: " + candidates.length)
        candidates.map {
          case (weight, article) =>
            //println("addart: " + article + " || W: " + wordWeights.getOrElse(word, 0d) * weight)
            val oldWeight = candidateArticles.getOrElse(article, 0d)
            candidateArticles(article) = oldWeight + wordWeights.getOrElse(word, 0d) * weight
            true
        }
    })
    Future.sequence(queries.toSeq).map { _ =>
      //println("cands: " + candidateArticles.size)
      val articles = candidateArticles.toArray.sortBy { case (_, p) => p }.map { case (a, _) => a }
      //println("alen: " + articles.length)
      //println("Best: " + articles.last)
      articles.reverse.take(50).toJSArray //TODO: configurable
    }.toJSPromise
  }

  private def dictGet(word: String): Future[(String, Array[(Double, String)])] = {
    val hash = MurmurHash3.stringHash(word)
    val l0 = (hash & 0xfff00000) >>> 20
    val l1 = (hash & 0x000ff000) >>> 12

    val l0map = dict.flatMap(_(l0.toHexString).get).map(_.value.asInstanceOf[js.Dictionary[IPLDLink]])
    val l1map = l0map.flatMap(_(l1.toHexString).get).map(_.value.asInstanceOf[js.Dictionary[js.Dictionary[IPLDLink]]])

    l1map.flatMap { bucket =>
      val bucketData = bucket("data")
      if(bucketData.contains(word)) {
        ipfs.block.get(bucketData(word)./).map { wordBlock =>
          val wordInfo = JSON.parse(wordBlock.data.toString()).asInstanceOf[js.Array[js.Tuple2[Double, String]]]
          (word, wordInfo.toArray.map(t => js.Tuple2.toScalaTuple2(t)))
        }
      } else {
        Future.failed(new Exception("Not found"))
      }
    }
  }
}