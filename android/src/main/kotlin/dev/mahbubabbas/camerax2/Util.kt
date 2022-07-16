package dev.mahbubabbas.camerax2

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import androidx.camera.core.Camera
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.face.Face
import dev.mahbubabbas.camerax2.Constants.EXTERNAL_IMG_PATH
import dev.mahbubabbas.camerax2.Constants.FILENAME_FORMAT
import dev.mahbubabbas.camerax2.Constants.PHOTO_EXTENSION
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

val Any.TAG: String
    get() = javaClass.simpleName

val Camera.torchable: Boolean
    get() = cameraInfo.hasFlashUnit()

val ImageProxy.yuv: ByteArray
    get() {
        val ySize = y.buffer.remaining()
        val uSize = u.buffer.remaining()
        val vSize = v.buffer.remaining()

        val size = ySize + uSize + vSize
        val data = ByteArray(size)

        var offset = 0
        y.buffer.get(data, offset, ySize)
        offset += ySize
        u.buffer.get(data, offset, uSize)
        offset += uSize
        v.buffer.get(data, offset, vSize)

        return data
    }

val ImageProxy.nv21: ByteArray
    get() {
        if (BuildConfig.DEBUG) {
            if (y.pixelStride != 1 || u.rowStride != v.rowStride || u.pixelStride != v.pixelStride) {
                error("Assertion failed")
            }
        }

        val ySize = width * height
        val uvSize = ySize / 2
        val size = ySize + uvSize
        val data = ByteArray(size)

        var offset = 0
        // Y Plane
        if (y.rowStride == width) {
            y.buffer.get(data, offset, ySize)
            offset += ySize
        } else {
            for (row in 0 until height) {
                y.buffer.get(data, offset, width)
                offset += width
            }

            if (BuildConfig.DEBUG && offset != ySize) {
                error("Assertion failed")
            }
        }
        // U,V Planes
        if (v.rowStride == width && v.pixelStride == 2) {
            if (BuildConfig.DEBUG && v.size != uvSize - 1) {
                error("Assertion failed")
            }

            v.buffer.get(data, offset, 1)
            offset += 1
            u.buffer.get(data, offset, u.size)
            if (BuildConfig.DEBUG) {
                val value = v.buffer.get()
                if (data[offset] != value) {
                    error("Assertion failed")
                }
            }
        } else {
            for (row in 0 until height / 2)
                for (col in 0 until width / 2) {
                    val index = row * v.rowStride + col * v.pixelStride
                    data[offset++] = v.buffer.get(index)
                    data[offset++] = u.buffer.get(index)
                }

            if (BuildConfig.DEBUG && offset != size) {
                error("Assertion failed")
            }
        }

        return data
    }

val ImageProxy.PlaneProxy.size
    get() = buffer.remaining()

val ImageProxy.y: ImageProxy.PlaneProxy
    get() = planes[0]

val ImageProxy.u: ImageProxy.PlaneProxy
    get() = planes[1]

val ImageProxy.v: ImageProxy.PlaneProxy
    get() = planes[2]

val Barcode.data: Map<String, Any?>
    get() = mapOf(
        "corners" to cornerPoints?.map { corner -> corner.data }, "format" to format,
        "rawBytes" to rawBytes, "rawValue" to rawValue, "type" to valueType,
        "calendarEvent" to calendarEvent?.data, "contactInfo" to contactInfo?.data,
        "driverLicense" to driverLicense?.data, "email" to email?.data,
        "geoPoint" to geoPoint?.data, "phone" to phone?.data, "sms" to sms?.data,
        "url" to url?.data, "wifi" to wifi?.data
    )

val Point.data: Map<String, Double>
    get() = mapOf("x" to x.toDouble(), "y" to y.toDouble())

val Barcode.CalendarEvent.data: Map<String, Any?>
    get() = mapOf(
        "description" to description, "end" to end?.rawValue, "location" to location,
        "organizer" to organizer, "start" to start?.rawValue, "status" to status,
        "summary" to summary
    )

val Barcode.ContactInfo.data: Map<String, Any?>
    get() = mapOf(
        "addresses" to addresses.map { address -> address.data },
        "emails" to emails.map { email -> email.data }, "name" to name?.data,
        "organization" to organization, "phones" to phones.map { phone -> phone.data },
        "title" to title, "urls" to urls
    )

val Barcode.Address.data: Map<String, Any?>
    get() = mapOf("addressLines" to addressLines, "type" to type)

val Barcode.PersonName.data: Map<String, Any?>
    get() = mapOf(
        "first" to first, "formattedName" to formattedName, "last" to last,
        "middle" to middle, "prefix" to prefix, "pronunciation" to pronunciation,
        "suffix" to suffix
    )

val Barcode.DriverLicense.data: Map<String, Any?>
    get() = mapOf(
        "addressCity" to addressCity, "addressState" to addressState,
        "addressStreet" to addressStreet, "addressZip" to addressZip, "birthDate" to birthDate,
        "documentType" to documentType, "expiryDate" to expiryDate, "firstName" to firstName,
        "gender" to gender, "issueDate" to issueDate, "issuingCountry" to issuingCountry,
        "lastName" to lastName, "licenseNumber" to licenseNumber, "middleName" to middleName
    )

val Barcode.Email.data: Map<String, Any?>
    get() = mapOf("address" to address, "body" to body, "subject" to subject, "type" to type)

val Barcode.GeoPoint.data: Map<String, Any?>
    get() = mapOf("latitude" to lat, "longitude" to lng)

val Barcode.Phone.data: Map<String, Any?>
    get() = mapOf("number" to number, "type" to type)

val Barcode.Sms.data: Map<String, Any?>
    get() = mapOf("message" to message, "phoneNumber" to phoneNumber)

val Barcode.UrlBookmark.data: Map<String, Any?>
    get() = mapOf("title" to title, "url" to url)

val Barcode.WiFi.data: Map<String, Any?>
    get() = mapOf("encryptionType" to encryptionType, "password" to password, "ssid" to ssid)

//Face data
val Face.data: Map<String, Any?>
    get() = mapOf(
        "rect" to boundingBox.data
    )

val Rect.data: Map<String, Any?>
    get() = mapOf(
        "left" to left.toDouble(),
        "top" to top.toDouble(),
        "right" to right.toDouble(),
        "bottom" to bottom.toDouble()
    )


//Capture photo
fun getFileName(): String {
    return SimpleDateFormat(FILENAME_FORMAT, Locale.US)
        .format(System.currentTimeMillis()) + PHOTO_EXTENSION
}

@Throws(IOException::class)
fun getOutputDirectory(context: Context, gallery: String): File {
    val path = File(EXTERNAL_IMG_PATH)
    //lge("external path: $path")

    val newFolder = File(path, gallery)
    checkFolder(newFolder)
    //lgd("new folder: ${newFolder.path}")

    return newFolder
}

private fun checkFolder(f: File) { if (!f.exists()) f.mkdir()  }

fun createFile(baseFolder: File, fileName: String): File {
    return File(baseFolder, fileName)
}