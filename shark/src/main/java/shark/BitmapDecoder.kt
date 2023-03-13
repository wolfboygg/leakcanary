/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package shark

import java.awt.Dimension
import java.awt.image.BufferedImage
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.or

/**
 * Created by tangyinsheng on 2017/7/6.
 *
 * This class is ported from Android Studio tools.
 */
object BitmapDecoder {
  const val BITMAP_FQCN = "android.graphics.Bitmap"
  const val BITMAP_DRAWABLE_FQCN = "android.graphics.drawable.BitmapDrawable"
  val SUPPORTED_FORMATS: MutableMap<String, BitmapExtractor> = HashMap()

  /** Maximum height or width of image beyond which we scale it on the device before retrieving. */
  private const val MAX_DIMENSION = 1024

  fun getBitmap(dataProvider: BitmapDataProvider): BufferedImage {
    val config =
      dataProvider.bitmapConfigName
        ?: throw RuntimeException("Unable to determine bitmap configuration")
    val bitmapExtractor =
      SUPPORTED_FORMATS[config]
        ?: throw RuntimeException("Unsupported bitmap configuration: $config")
    var size =
      dataProvider.dimension ?: throw RuntimeException("Unable to determine image dimensions.")

    // if the image is rather large, then scale it down
    if (size.width > MAX_DIMENSION || size.height > MAX_DIMENSION) {
      val couldDownsize = dataProvider.downsizeBitmap(size)
      if (!couldDownsize) {
        throw RuntimeException("Unable to create scaled bitmap")
      }
      size =
        dataProvider.dimension ?: throw RuntimeException("Unable to determine image dimensions.")
    }
    return bitmapExtractor.getImage(size.width, size.height, dataProvider.getPixelBytes(size))
  }

  interface BitmapDataProvider {
    val bitmapConfigName: String?
    val dimension: Dimension?

    // Downsizes the bitmap, in-place, to the newSize.
    fun downsizeBitmap(newSize: Dimension?): Boolean
    fun getPixelBytes(size: Dimension?): ByteArray
  }

  interface BitmapExtractor {
    fun getImage(w: Int, h: Int, data: ByteArray?): BufferedImage
  }

  class ARGB8888_BitmapExtractor : BitmapExtractor {

    override fun getImage(width: Int, height: Int, rgba: ByteArray?): BufferedImage {
      val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
      rgba?.let {
        for (y in 0 until height) {
          val stride = y * width
          for (x in 0 until width) {
            val i = (stride + x) * 4
            var rgb: Long = 0
            rgb = rgb or (rgba[i].toLong() and 0xff shl 16) // r
            rgb = rgb or (rgba[i + 1].toLong() and 0xff shl 8) // g
            rgb = rgb or (rgba[i + 2].toLong() and 0xff) // b
            rgb = rgb or (rgba[i + 3].toLong() and 0xff shl 24) // a
            bufferedImage.setRGB(x, y, (rgb and 0xffffffffL).toInt())
          }
        }
      }
      return bufferedImage
    }
  }

  private class RGB565_BitmapExtractor : BitmapExtractor {
    override fun getImage(width: Int, height: Int, rgb: ByteArray?): BufferedImage {
      val bytesPerPixel = 2
      val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
      rgb?.let {
        for (y in 0 until height) {
          val stride = y * width
          for (x in 0 until width) {
            val index = (stride + x) * bytesPerPixel
            val value: Int =
              (rgb[index] and
                  0x00ff.toByte() or
                  ((rgb[index + 1].toInt() shl 8).toByte()) and
                  0xff00.toByte())
                .toInt()
            // RGB565 to RGB888
            // Multiply by 255/31 to convert from 5 bits (31 max) to 8 bits (255)
            val r = (value ushr 11 and 0x1f) * 255 / 31
            val g = (value ushr 5 and 0x3f) * 255 / 63
            val b = (value and 0x1f) * 255 / 31
            val a = 0xFF
            val rgba = a shl 24 or (r shl 16) or (g shl 8) or b
            bufferedImage.setRGB(x, y, rgba)
          }
        }
      }
      return bufferedImage
    }
  }

  class ALPHA8_BitmapExtractor : BitmapExtractor {
    override fun getImage(width: Int, height: Int, rgb: ByteArray?): BufferedImage {
      val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
      rgb?.let {
        for (y in 0 until height) {
          val stride = y * width
          for (x in 0 until width) {
            val index = stride + x
            val value = rgb[index].toInt()
            val rgba = value shl 24 or (0xff shl 16) or (0xff shl 8) or 0xff
            bufferedImage.setRGB(x, y, rgba)
          }
        }
      }
      return bufferedImage
    }
  }

  init {
    SUPPORTED_FORMATS["\"ARGB_8888\""] = ARGB8888_BitmapExtractor()
    SUPPORTED_FORMATS["\"RGB_565\""] = RGB565_BitmapExtractor()
    SUPPORTED_FORMATS["\"ALPHA_8\""] = ALPHA8_BitmapExtractor()
  }
}
