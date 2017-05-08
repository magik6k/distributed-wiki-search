#!/usr/bin/env scala

import scala.io.Source
import sys.process._
import scala.language.postfixOps
import java.io.ByteArrayInputStream
import scala.collection.mutable

object HahmapBucketsDouble extends App {
  type Bucket = mutable.Map[String, String]
  type L1 = mutable.Map[Int, Bucket]
  type L0 = mutable.Map[Int, L1]

  val keyFile = args(0)
  val valueFile = args(1)

  val rawKeys = Source.fromFile(keyFile).getLines().toArray
  val rawValues = Source.fromFile(valueFile).getLines().toArray
  val data = rawKeys.zip(rawValues).toMap

  val hashedData = data.map(e => (e._1, (e._2, scala.util.hashing.MurmurHash3.stringHash(e._1))))

  val root: L0 = mutable.Map[Int, L1]()

  def getBucket[T <: mutable.Map[B1, B2], B1, B2, K](map: mutable.Map[K, T], bucket: K): T = {
    if(!map.contains(bucket))
      map(bucket) = mutable.Map[B1, B2]().asInstanceOf[T]
    map(bucket)
  }

  hashedData.foreach {
    case (key, (value, hash)) =>
      val l0 = (hash & 0xfff00000) >>> 20
      val l1 = (hash & 0x000ff000) >>> 12

      val lowBucket = getBucket[L1, Int, Bucket, Int](root, l0)
      val highBucket = getBucket[Bucket, String, String, Int](lowBucket, l1)

      highBucket(key) = value
  }

  var l0added = 0

  val rootObject = root.par.map {
    case (l0: Int, l1bucket: L1) =>

      if(l0added % 20 == 0)
        Console.err.print(s"\r${Math.floor(l0added / 40.92)}%    ")
      l0added += 1

      "\"" + l0.toHexString + "\":{\"/\":\"" + addLowBucket(l1bucket) + "\"}"
  }.mkString(",")

  Console.err.println("\rdone          ")
  println(dagPut(s"{$rootObject}"))

  private def addLowBucket(bucket: L1): String = {
    val lowEntry = bucket.map {
      case (l1: Int, dataBucket: Bucket @ unchecked) =>
        "\"" + l1.toHexString + "\":{\"/\":\"" + addHighBucket(dataBucket) + "\"}"
    }.mkString(",")

    Process(Array("./util/dagPut.sh", s"{$lowEntry}")).lineStream.head
  }

  private def addHighBucket(bucket: Bucket): String = {
    val entry = "{\"data\":{" + bucket.map{ case (k, v) => "\"" + esc(k) + "\": " + v }.mkString(",") + "}}"
    Process(Array("./util/dagPut.sh", entry)).lineStream.head
  }

  private def dagPut(in: String): String = {
    ("ipfs dag put"#< new ByteArrayInputStream(in.getBytes("UTF-8")) ).lineStream.head
  }

  private def esc(str: String): String = str.replace("\\", "\\\\").replace("\"", "\\\"")
}
