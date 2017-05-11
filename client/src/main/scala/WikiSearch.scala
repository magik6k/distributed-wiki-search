import eu.devtty.ipfs.IpfsNode
import eu.devtty.ipld.util.IPLDLink
import io.scalajs.JSON

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.annotation.ScalaJSDefined
import scala.util.hashing.MurmurHash3
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js.JSConverters._

@ScalaJSDefined
class WikiSearch(dictCid: String) extends js.Object {
  implicit val ipfs: IpfsNode = AppLauncher.ipfs

  private val dict = ipfs.dag.get(dictCid).map(_.value.asInstanceOf[js.Dictionary[IPLDLink]])

  def search(rawSearch: String): js.Promise[js.Array[String]] = {
    val searchWords = toWords(rawSearch)
    val wordWeights = getWordWeights(searchWords)

    val candidateArticles = mutable.Map[String, (Double, mutable.HashSet[String])]()

    val queries = searchWords.map(dictGet).map(futures => futures.map {
      case (word, candidates) =>
        candidates.map {
          case (weight, article) =>
            val (oldWeight, words) = candidateArticles.getOrElse(article, (0d, mutable.HashSet[String]()))
            candidateArticles(article) = (oldWeight + wordWeights.getOrElse(word, 0d) * weight, words += word)
            true
        }
    })

    Future.sequence(queries.toSeq).map { _ =>
      //apply penalty for not containing words searched for
      val candidates = candidateArticles.map {
        case (article, (relevance, words)) =>
          val nonMatchingWords = searchWords.filterNot(words.contains).map(wordWeights.apply).map(1 - _).product
          (article, relevance * nonMatchingWords)
      }

      val articles = candidates.toArray.sortBy { case (_, p) => p }.map { case (a, _) => a }
      articles.reverse.take(50).toJSArray //TODO: configurable
    }.toJSPromise
  }

  private def getWordWeights(words: Array[String]): mutable.Map[String, Double] = {
    val wordWeights = mutable.Map[String, Double]()
    val len = words.map(_.length).sum

    def addWeight(word: String, weight: Double): Unit = {
      val oldWeight = wordWeights.getOrElse(word, 0d)
      wordWeights(word) = oldWeight + weight
    }

    words.foreach(word => addWeight(word, word.length.toDouble / len))
    wordWeights
  }

  private def toWords(query: String): Array[String] = {
    query.toLowerCase.replaceAll("""_\(\)-,""", " ").split(" ")
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
