/*
 * Copyright (c) 2020 ThanksMister LLC
 *  http://www.thanksmister.com
 *
 *  Mozilla Public License 2.0
 *
 *  Permissions of this weak copyleft license are conditioned on making
 *  available source code of licensed files and modifications of those files
 *  under the same license (or in certain cases, one of the GNU licenses).
 *  Copyright and license notices must be preserved. Contributors provide
 *  an express grant of patent rights. However, a larger work using the
 *  licensed work may be distributed under different terms and without source
 *  code for files added in the larger work.
 */

package com.thanksmister.bitcoin.localtrader.ui.viewmodels

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.thanksmister.bitcoin.localtrader.BaseApplication
import com.thanksmister.bitcoin.localtrader.R
import com.thanksmister.bitcoin.localtrader.network.api.LocalBitcoinsApi
import com.thanksmister.bitcoin.localtrader.network.api.fetchers.LocalBitcoinsFetcher
import com.thanksmister.bitcoin.localtrader.network.exceptions.ExceptionCodes
import com.thanksmister.bitcoin.localtrader.network.exceptions.NetworkException
import com.thanksmister.bitcoin.localtrader.network.exceptions.RetrofitErrorHandler
import com.thanksmister.bitcoin.localtrader.persistence.Preferences
import com.thanksmister.bitcoin.localtrader.utils.StringUtils
import com.thanksmister.bitcoin.localtrader.utils.applySchedulers
import com.thanksmister.bitcoin.localtrader.utils.plusAssign
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import retrofit2.HttpException
import timber.log.Timber
import java.io.*
import java.net.SocketTimeoutException
import java.util.*
import javax.inject.Inject


class MessageViewModel @Inject
constructor(application: Application,
            private val okHttpClient: OkHttpClient,
            private val preferences: Preferences) : BaseViewModel(application) {

    private val messagePostStatus = MutableLiveData<Boolean>()
    private val fetcher: LocalBitcoinsFetcher by lazy {
        val endpoint = preferences.getServiceEndpoint()
        val api = LocalBitcoinsApi(okHttpClient, endpoint)
        LocalBitcoinsFetcher(getApplication(), api, preferences)
    }

    fun getMessagePostStatus(): LiveData<Boolean> {
        return messagePostStatus
    }

    private fun setMessagePostStatus(value: Boolean) {
        this.messagePostStatus.value = value
    }

    init {

    }

    fun postMessage(contactId: Int, message: String) {
        disposable += fetcher.postMessage(contactId, message)
                .applySchedulers()
                .subscribe ({
                    setMessagePostStatus(true)
                }, { error ->
                    Timber.e("Message Post Error " + error.message)
                    when (error) {
                        is HttpException -> {
                            val errorHandler = RetrofitErrorHandler(getApplication())
                            val networkException = errorHandler.create(error)
                            handleNetworkException(networkException)
                        }
                        is NetworkException -> handleNetworkException(error)
                        is SocketTimeoutException -> handleSocketTimeoutException()
                        else -> showAlertMessage(getApplication<BaseApplication>().getString(R.string.toast_error_message))
                    }
                })
    }

    /*
    {"message":"Invalid parameters.","errors":{"document":"* File extension '' is not allowed. Allowed extensions are: 'ppm, grib, im, rgba, rgb, jpx, h5, jpe, jpf, jpg, fli, jpc, sgi, gbr, pcx, mpeg, jpeg, ps, flc, tif, hdf, icns, gif, palm, mpg, fits, pgm, mic, pxr, fit, xbm, eps, emf, jp2, dcx, webp, bmp, bw, pbm, j2c, psd, pcd, ras, j2k, mpo, cur, fpx, ftu, png, msp, iim, wmf, jfif, tga, bufr, ico, ftc, xpm, pdf, dds, tiff'."},"error_code":9,"error_lists":{"document":["File extension '' is not allowed. Allowed extensions are: 'ppm, grib, im, rgba, rgb, jpx, h5, jpe, jpf, jpg, fli, jpc, sgi, gbr, pcx, mpeg, jpeg, ps, flc, tif, hdf, icns, gif, palm, mpg, fits, pgm, mic, pxr, fit, xbm, eps, emf, jp2, dcx, webp, bmp, bw, pbm, j2c, psd, pcd, ras, j2k, mpo, cur, fpx, ftu, png, msp, iim, wmf, jfif, tga, bufr, ico, ftc, xpm, pdf, dds, tiff'."]}}
     */
    // TODO we are guessing if we never have an extension, we should ask and also filter the extension types and give warning if wrong file is attempted to upload
    private fun postMessageWithAttachment(contactId: Int, message: String, file: File, fileName: String) {
        var extension: String = fileName.substring(fileName.lastIndexOf("."))
        if(extension.isEmpty()) {
            extension = ".jpg"
        }
        val r = Random()
        val randomNum: Int = r.nextInt(1000 - 100) + 1000
        val sendFileName = "$contactId-$randomNum$extension"
        if(sendFileName.isNotEmpty()) {
            disposable += fetcher.postMessageWithAttachment(contactId, message, file, sendFileName)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe ({
                        setMessagePostStatus(true)
                    }, {
                        error -> Timber.e("Message Post Attachment Error" + error.message)
                        if(error is NetworkException) {
                            if(RetrofitErrorHandler.isHttp403Error(error.code)) {
                                showNetworkMessage(error.message, ExceptionCodes.AUTHENTICATION_ERROR_CODE)
                            } else {
                                showNetworkMessage(error.message, error.code)
                            }
                        } else {
                            showAlertMessage(getApplication<BaseApplication>().getString(R.string.toast_error_message))
                        }
                    })
        }
    }

    fun generateMessageBitmap(contactId: Int, fileName: String, message: String, uri: Uri) {
        disposable.add(
                generateBitmapObservable(fileName, uri)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ file ->
                            if(file != null) {
                                postMessageWithAttachment(contactId, message, file, fileName)
                            }
                        }, { error ->
                            Timber.e(error.message)
                            showAlertMessage(getApplication<BaseApplication>().getString(R.string.toast_file_no_upload))
                        }))
    }

    private fun generateBitmapObservable(fileName: String, uri: Uri): Observable<File> {
        return Observable.create { subscriber ->
            try {
                val file = getBitmapFromStream(fileName, uri)
                subscriber.onNext(file)
            } catch (e: Exception) {
                subscriber.onError(e)
            }
        }
    }

    private fun getBitmapFromStream(fileName: String, uri: Uri): File {
        var bitmap: Bitmap? = null
        try {
            val outDimens = getBitmapDimensions(getApplication<BaseApplication>(), uri)
            val sampleSize = calculateSampleSize(outDimens.outWidth, outDimens.outHeight, 1200, 1200)
            bitmap = downSampleBitmap(getApplication<BaseApplication>(), uri, sampleSize)
            val file = File(getApplication<BaseApplication>().cacheDir, StringUtils.removeExtension(fileName))
            file.createNewFile()
            val bos = ByteArrayOutputStream()
            bitmap!!.compress(Bitmap.CompressFormat.PNG, 0, bos)
            val bitmapData = bos.toByteArray()
            //write the bytes in file
            val fos = FileOutputStream(file)
            fos.write(bitmapData)
            fos.flush()
            fos.close()
            return file
        } catch (e: Exception) {
            Timber.e("File Exception: " + e.message)
            throw Exception(e.message)
        }
    }

    @Throws(FileNotFoundException::class, IOException::class)
    private fun getBitmapDimensions(context: Context, uri: Uri): BitmapFactory.Options {
        val outDimens = BitmapFactory.Options()
        outDimens.inJustDecodeBounds = true // the decoder will return null (no bitmap)
        val inputStream = context.contentResolver.openInputStream(uri)
        // if Options requested only the size will be returned
        BitmapFactory.decodeStream(inputStream, null, outDimens)
        inputStream.close()

        return outDimens
    }

    private fun calculateSampleSize(width: Int, height: Int, targetWidth: Int, targetHeight: Int): Int {
        var inSampleSize = 1
        if (height > targetHeight || width > targetWidth) {
            // Calculate ratios of height and width to requested height and
            // width
            val heightRatio = Math.round(height.toFloat() / targetHeight.toFloat())
            val widthRatio = Math.round(width.toFloat() / targetWidth.toFloat())
            // Choose the smallest ratio as inSampleSize value, this will
            // guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
        }
        return inSampleSize
    }

    @Throws(FileNotFoundException::class, IOException::class)
    private fun downSampleBitmap(context: Context, uri: Uri, sampleSize: Int): Bitmap? {
        val resizedBitmap: Bitmap?
        val outBitmap = BitmapFactory.Options()
        outBitmap.inJustDecodeBounds = false // the decoder will return a bitmap
        outBitmap.inSampleSize = sampleSize
        val inputStream = context.contentResolver.openInputStream(uri)
        resizedBitmap = BitmapFactory.decodeStream(inputStream, null, outBitmap)
        inputStream.close()
        return resizedBitmap
    }
}