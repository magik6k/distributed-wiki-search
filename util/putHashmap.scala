#!/usr/bin/env scala

import scala.io.Source
import sys.process._
import scala.language.postfixOps
import java.io.ByteArrayInputStream

object HahmapBucketsDouble extends App {
  val keyFile = args(0)
  val valueFile = args(1)

  val rawKeys = Source.fromFile(keyFile).getLines().toArray
  val rawValues = Source.fromFile(valueFile).getLines().toArray

  val data = rawKeys.zip(rawValues).toMap

  val hashedData = data.map(e => (e._1, (e._2, scala.util.hashing.MurmurHash3.stringHash(e._1))))

  val root = scala.collection.mutable.Map[Int, Object]()

  def getBucket[T, B1, B2](map: scala.collection.mutable.Map[Int, Object], bucket: Int): T = {
    if(!map.contains(bucket))
      map(bucket) = scala.collection.mutable.Map[B1, B2]()
    map(bucket).asInstanceOf[T]
  }

  hashedData.foreach {
    case (key, (value, hash)) =>
      val l0 = ((hash & 0xfff00000) >>> 20)
      val l1 = ((hash & 0x000ff000) >>> 12)

      val lowBucket = getBucket[scala.collection.mutable.Map[Int, Object], Int, Object](root, l0)
      val highBucket = getBucket[scala.collection.mutable.Map[String, String], String, String](lowBucket, l1)

      highBucket(key) = value
  }

  // Should have picked Selene/Go/C/Lua/Assembly/PHP for that....
  var n = 0

  val rootObject = root.par.map {
    case (l0: Int, l0bucket: Object) =>
      val lowEntry = l0bucket.asInstanceOf[scala.collection.mutable.Map[Int, scala.collection.mutable.Map[String, String]]].map {
        case (l1: Int, dataBucket: scala.collection.mutable.Map[String, String] @ unchecked) =>
          val entry = "{\"data\":{" + (dataBucket.map{ case (k, v) => "\"" + esc(k) + "\": " + v }.mkString(",")) + "}}"
          "\"" + l1.toHexString + "\":{\"/\":\"" + (Process(Array("./util/dagPut.sh", entry)).lineStream.head) + "\"}"
      }.mkString(",")

      if(n % 20 == 0)
        Console.err.print(s"\r${Math.floor(n/40.92)}%    ")
      n+=1
      "\"" + l0.toHexString + "\":{\"/\":\"" + (Process(Array("./util/dagPut.sh", s"{$lowEntry}")).lineStream.head) + "\"}"     
  }.mkString(",")

  Console.err.println("\rdone          ")
  println(("ipfs dag put"#< new ByteArrayInputStream(s"{$rootObject}".getBytes("UTF-8")) ).lineStream.head)

  def esc(str: String): String = str.replace("\\", "\\\\").replace("\"", "\\\"")
}
