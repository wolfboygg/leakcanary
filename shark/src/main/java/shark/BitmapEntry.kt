package shark

/** author:guolei date:2021/7/16 description: */
data class BitmapEntry(
  val mBufferHash: String,
  val mWidth: Int,
  val mHeight: Int,
  val mBuffer: ByteArray,
  val isLargeBitmap: Boolean = false
) {
  override fun toString(): String {
    return "mBufferHash:$mBufferHash,mWidth:$mWidth,mHeight:$mHeight"
  }
}
