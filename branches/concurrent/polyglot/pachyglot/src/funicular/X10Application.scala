package funicular

import funicular.runtime.Runtime

abstract class X10Application {
  def run(args: Array[String]): Unit
  
  final def main(args: Array[String]): Unit = {
      Runtime.runFinish { run(args) }
  }
}
