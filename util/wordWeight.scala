#!/usr/bin/env scala

import scala.io.Source

object WordWeight extends App {
  val wordWeights = scala.collection.mutable.Map[String, Double]()

  val rawArticles = Source.fromFile("work/articles.clean").getLines().toArray
  val articles = rawArticles.map(_.split(" "))
  val lengths = articles.map(_.map(_.length).sum)

  articles.zip(lengths).foreach {
    case (article, length) =>
      article.foreach(word => addWeight(word, word.length.toDouble / length))
  }

  def addWeight(word: String, weight: Double) = {
    val oldWeight = wordWeights.getOrElse(word, 0d)
    wordWeights(word) = oldWeight + weight
  }

  wordWeights.foreach{
    case (k, weight) =>
      println(s"$weight $k")
  }
}

