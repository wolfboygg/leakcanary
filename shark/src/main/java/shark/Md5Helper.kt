package shark

import java.security.MessageDigest
import kotlin.experimental.and

object Md5Helper {
  // 计算字节流的md5值
  @Throws(Exception::class)
  fun getMd5(bytes: ByteArray): String {
    val md5 = MessageDigest.getInstance("MD5")
    md5.update(bytes, 0, bytes.size)
    return byteArrayToHex(md5.digest()).toLowerCase()
  }

  private fun byteArrayToHex(byteArray: ByteArray): String {
    val hexDigits =
      charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
    val resultCharArray = CharArray(byteArray.size * 2)
    var index = 0
    for (b in byteArray) {
      resultCharArray[index++] = hexDigits[b.toInt() ushr 4 and 0xf]
      resultCharArray[index++] = hexDigits[((b and 0xf).toInt())]
    }
    return String(resultCharArray)
  }
}
