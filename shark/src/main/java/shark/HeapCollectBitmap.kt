package shark

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO
import shark.HprofHeapGraph.Companion.openHeapGraph

open class HeapCollectBitmap {

  private class CollectBitmap(val graph: HeapGraph)

  fun collect(heapDumpFile: File): HeapCollectBitmap {
    if (!heapDumpFile.exists()) {
      return HeapCollectBitmapFailure(heapDumpFile = heapDumpFile)
    }
    val sourceProvider = ConstantMemoryMetricsDualSourceProvider(FileSourceProvider(heapDumpFile))
    return try {
      sourceProvider.openHeapGraph().use { graph ->
        collect(heapDumpFile = heapDumpFile, graph = graph)
      }
    } catch (throwable: Throwable) {
      HeapCollectBitmapFailure(heapDumpFile = heapDumpFile)
    }
  }

  fun collect(heapDumpFile: File, graph: HeapGraph): HeapCollectBitmap {
    val list = arrayListOf<BitmapEntry>()
    list.findBitmap(graph = graph)
    println("collect bitmap entry size:" + list.size)
    recoverBitmap(heapDumpFile.name, list)
    return HeapCollectBitmapSuccess(heapDumpFile = heapDumpFile)
  }

  private fun ArrayList<BitmapEntry>.findBitmap(
    graph: HeapGraph,
  ) {
    val bitmapClass = graph.findClassByName("android.graphics.Bitmap")
    if (bitmapClass == null) {
      println("bitmapClass is null")
      return
    }
    val maxDisplayPixels =
      graph
        .findClassByName("android.util.DisplayMetrics")
        ?.directInstances
        ?.map { instance ->
          val width = instance["android.util.DisplayMetrics", "widthPixels"]?.value?.asInt ?: 0
          val height = instance["android.util.DisplayMetrics", "heightPixels"]?.value?.asInt ?: 0
          width * height
        }
        ?.max()
        ?: 0

    val maxDisplayPixelsWithThreshold = (maxDisplayPixels * 1.1).toInt()

    bitmapClass.instances.forEach { bitmap ->
      val width = bitmap["android.graphics.Bitmap", "mWidth"]?.value?.asInt ?: 0
      val height = bitmap["android.graphics.Bitmap", "mHeight"]?.value?.asInt ?: 0
      val bytePrimitiveArray = bitmap["android.graphics.Bitmap", "mBuffer"]?.valueAsPrimitiveArray
      if (width == 0 || height == 0 || bytePrimitiveArray == null) {
        println("width:$width")
        println("height:$height")
        println("bytePrimitiveArray:$bytePrimitiveArray")
        return
      }
      val byteArrayDump =
        bytePrimitiveArray.readRecord()
          as HprofRecord.HeapDumpRecord.ObjectRecord.PrimitiveArrayDumpRecord.ByteArrayDump
      val byteArray = byteArrayDump.array
      val isLarge =
        maxDisplayPixelsWithThreshold > 0 && width * height > maxDisplayPixelsWithThreshold
      val bitmapEntry = BitmapEntry(Md5Helper.getMd5(byteArray), width, height, byteArray, isLarge)
      add(bitmapEntry)
    }
  }

  private fun recoverBitmap(name: String, list: java.util.ArrayList<BitmapEntry>) {
    println("recoverBitmap")
    val outputFile = File("./${name}-allpng-.zip")
    if (outputFile.exists()) {
      outputFile.delete()
    }
    outputFile.createNewFile()
    var zos: ZipOutputStream? = null
    try {
      zos = ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile)))
      list.forEach {
        val img = BitmapDecoder.getBitmap(HprofBitmapProvider(it.mBuffer, it.mWidth, it.mHeight))
        // Since bmp format is not compatible with alpha channel, we export buffer as png instead.
        // Since bmp format is not compatible with alpha channel, we export buffer as png instead.

        var largeStr = if (it.isLargeBitmap) "large" else ""
        val pngName: String =
          "buffer_contents" +
            "/" +
            largeStr +
            it.mBufferHash +
            "_" +
            System.currentTimeMillis() +
            ".png"
        try {
          zos.putNextEntry(ZipEntry(pngName))
          ImageIO.write(img, "png", zos)
          zos.flush()
        } finally {
          zos.closeEntry()
        }
      }
    } finally {
      zos?.close()
    }
  }
}
