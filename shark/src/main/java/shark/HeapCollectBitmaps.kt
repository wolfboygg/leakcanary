package shark

import java.io.File

data class HeapCollectBitmapFailure(val heapDumpFile: File) : HeapCollectBitmap() {
  override fun toString(): String {
    return """====================================
            HEAP Collect Bitmap FAILED
          ===================================="""
  }
}

data class HeapCollectBitmapSuccess(val heapDumpFile: File) : HeapCollectBitmap() {
  override fun toString(): String {
    return """====================================
            HEAP Collect Bitmap Success
          ===================================="""
  }
}
