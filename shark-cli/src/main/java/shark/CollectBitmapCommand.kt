package shark

import com.github.ajalt.clikt.core.CliktCommand
import java.io.File
import shark.SharkCliCommand.Companion.echo
import shark.SharkCliCommand.Companion.retrieveHeapDumpFile
import shark.SharkCliCommand.Companion.sharkCliParams

class CollectBitmapCommand :
  CliktCommand(name = "collect-bitmap", help = "Dump the heap and collect memory bitmap.") {
  override fun run() {
    val params = context.sharkCliParams
    collect(retrieveHeapDumpFile(params))
  }

  companion object {
    fun CliktCommand.collect(heapDumpFile: File) {
      echo("collect hprof file bitmap")
      // 将这个文件中的所有bitmap保存到一个目录中
      val heapCollectBitmap = HeapCollectBitmap()
      val collect = heapCollectBitmap.collect(heapDumpFile)
      echo(collect.toString())
    }
  }
}
