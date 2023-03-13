package shark

import java.awt.Dimension

class HprofBitmapProvider(
  private val mBuffer: ByteArray,
  private val mWidth: Int,
  private val mHeight: Int
) : BitmapDecoder.BitmapDataProvider {
  override val bitmapConfigName: String?
    get() {
      val area = mWidth * mHeight
      val pixelSize = mBuffer.size / area
      return if (area > mBuffer.size) {
        null
      } else
        when (pixelSize) {
          4 -> "\"ARGB_8888\""
          2 -> "\"RGB_565\""
          else -> "\"ALPHA_8\""
        }
    }

  override val dimension: Dimension?
    get() = if (mWidth < 0 || mHeight < 0) null else Dimension(mWidth, mHeight)

  override fun downsizeBitmap(newSize: Dimension?): Boolean {
    return true
  }

  override fun getPixelBytes(size: Dimension?): ByteArray {
    return mBuffer
  }
}
