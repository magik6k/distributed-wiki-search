#!/usr/bin/env scala

import scala.io.Source

object WordWeight extends App {
  val wordWeights = scala.collection.mutable.Map[String, Double]()
  val wordMatrix = scala.collection.mutable.Map[String, scala.collection.mutable.Seq[(Double, String)]]()

  val rawArticles = Source.fromFile("work/articles.clean").getLines().toArray
  val rawLinks = Source.fromFile("work/articles").getLines().toArray

  val articles = rawArticles.map(_.split(" "))
  val lengths = articles.map(_.map(_.length).sum)

  articles.zip(lengths).foreach {
    case (article, length) =>
      article.foreach(word => addWeight(word, word.length.toDouble / length))
  }

  articles.zip(rawLinks).zip(lengths).foreach {
    case ((article, link), length) =>
      article.foreach(word => putWord(word, ((word.length.toDouble / length) / wordWeights(word), link)))
  }

  def putWord(word: String, entry: (Double, String)): Unit = {
    if(word.length == 0)
      return

    if(!wordMatrix.contains(word))
      wordMatrix(word) = scala.collection.mutable.Seq[(Double, String)]()

    wordMatrix(word) :+= entry
  }

  def addWeight(word: String, weight: Double): Unit = {
    val oldWeight = wordWeights.getOrElse(word, 0d)
    wordWeights(word) = oldWeight + weight
  }

  wordMatrix.foreach{
    case (k, entries) =>
      val e = entries.map(e => s"""[${e._1},"${esc(e._2)}"]""").mkString(",")
      println(s"""["${esc(k)}", [$e]]""")
  }

  def esc(str: String): String = str.replace("\\", "\\\\").replace("\"", "\\\"")
}

