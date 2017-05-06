#!/usr/bin/env scala

import scala.io.Source
import sys.process._
import scala.language.postfixOps
import java.io.ByteArrayInputStream

object WordWeight extends App {
  val lines = Source.fromFile(args(0)).getLines().toArray
  var n = 0d

  lines.zipWithIndex.par.foreach{ case (line, index) =>
    println(index + " " + ("ipfs add --raw-leaves -q --pin=false" #< new ByteArrayInputStream(s"$line".getBytes("UTF-8")) ).lineStream.head)
    n += 1
    if(n % 20 == 0)
      Console.err.print(s"\r${Math.floor(n / (lines.length / 100d))}%    ")
  }
  Console.err.println("\rdone          ")
}
