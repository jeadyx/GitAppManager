package io.github.jeadyx.appmanager

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import io.github.jeadyx.gitversionmanager.GitManager
import java.io.File
import java.io.FileInputStream
import kotlin.concurrent.thread
import kotlin.random.Random


class SaveActivity : ComponentActivity() {
    private val TAG = "SaveActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val gitManager = GitManager("jeady5", "publisher", "e8d83e715fa7b44d2d89b3cf7d7554e0")
        val paths = mutableStateListOf<FileInfo>()
        intent.clipData?.let {data->
            for (i in 0 until data.itemCount){
                Log.d(TAG, "onCreate: clipdata ${data} $i ${data.getItemAt(i).uri}")
                saveFile(data.getItemAt(i).uri)?.let{path->
                    val file = File(path)
                    paths.add(FileInfo(
                        file.name, path,
                        MimeTypeMap.getSingleton().getMimeTypeFromExtension(File(path).extension).toString(),
                        file.length()
                    ))
                }
            }
        }
        val receiverSelect = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()){uri->
            Log.d(TAG, "onCreate: get a path $uri ${uri?.path}")
            uri?.let {
                paths.forEach{info->
                    uri.path?.let { path ->
                        val handler = Handler(mainLooper, Handler.Callback { msg->
                            paths.withIndex().find {
                                it.value.name == msg.obj.toString()
                            }?.let {
                                paths.set(it.index, it.value.copy(operate = "copied"))
                            }
                            Log.d(TAG, "onCreate: get handle $msg ${msg.obj.toString()}")
                            return@Callback true
                        })
                        saveTo(info, uri, handler)
                    }
                }
            }
        }
        setContent {
            Scaffold {
                val context = LocalContext.current
                Box(Modifier.padding(it)){
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("intent: ${intent}")
                        Text("extras: ${intent.extras?.size ()}")
                        Text("extras: ${intent.extras?.keySet()?.toList()}")
                        Text("extras: ${intent.extras?.keySet()?.map { "extras ${intent.extras?.get(it)}"}}")
                        Text("data: ${intent.data}")
                        Text("action: ${intent.action}")
                        Text("categories: ${intent.categories}")
                        Text("clipData: ${intent.clipData}")
                        Text("package: ${intent.`package`}")
                        Text("scheme: ${intent.scheme}")
                        Text("type: ${intent.type}")
                        Text("flags: ${intent.flags}")
                        LazyColumn {
                            items(paths){
                                Row{
                                    Text(it.name, color= when(it.operate){
                                        "copied" -> Color(0xff30a050)
                                        else -> Color(0xff303050)
                                    })
                                }
                            }
                        }
                        ButtonText("另存为"){
                            // 另存为收到的文件
                            receiverSelect.launch(Uri.EMPTY)
                        }
                        ButtonText("上传到git"){
                            paths.forEach {
                                gitManager.uploadFile(it.path, "share/${it.name}", "share file ${it.name}"){res->
                                    Log.d(TAG, "onCreate: upload $res")
                                    if(res?.startsWith("{") == true){
                                        showToast(context, "上传成功")
                                    }else{
                                        showDialog1(res.toString())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun getApkInfo(apkPath: String){
        if(!File(apkPath).exists()) return
        packageManager.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES)?.let{
            Log.d(TAG, "onCreate: fileinfo $it ${it.applicationInfo} ${it.packageName} ${it.signingInfo} ${it.sharedUserLabel} ${it.versionName} ${it.longVersionCode}")
            Log.d(TAG, "onCreate: package info ${it.applicationInfo.loadLabel(packageManager)} ${it.applicationInfo.loadDescription(packageManager)} ${it.applicationInfo.loadIcon(packageManager)}")
        }
    }

    fun saveFile(uri: Uri): String? {
//        val fileName = uri.lastPathSegment
        val inputStream =try {
            contentResolver.openInputStream(uri)
        }catch (e: Exception) {
            null
        }
        inputStream?.let {
            val fileName = uri.path?.substringAfterLast("/")?: System.currentTimeMillis().toString()
            val savePath = dataDir.path + "/files/${fileName}"
            val saveFile = File(savePath)
            if (saveFile.exists()) {
                saveFile.delete()
            }
            Log.d(TAG, "saveFile: uri $uri")
            Log.d(TAG, "saveFile: path ${uri.path}")
            Log.d(TAG, "saveFile: segments $fileName ${uri.pathSegments}")
            Log.d(TAG, "saveFile: savepath $savePath")
            val outputStream = openFileOutput(fileName, Context.MODE_PRIVATE)
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream.read(buffer)
                    .also { bytesRead = it } != -1
            ) {
                outputStream.write(buffer, 0, bytesRead)
            }
            inputStream.close()
            outputStream.close()
            Log.d(TAG, "saveFile: file ${File(savePath).exists()}")
            File(savePath).deleteOnExit()
            return savePath
        }
        return null
    }
    fun saveFile(filePath: String){
        saveFile(Uri.fromFile(File(filePath)))
    }
    fun saveToDownload(srcFile: File, destFileName: String){
        val savePath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/$destFileName"
        srcFile.copyTo(File(savePath), true)
    }
    
    private fun saveTo(srcFileInfo: FileInfo, destTreeUri: Uri, handler: Handler) {
        // 通过uri保存来自srcFile的文件
        thread {
            Log.d(TAG, "saveTo: save file $srcFileInfo ")
            Log.d(TAG, "saveTo: save file to $destTreeUri")
            DocumentFile.fromTreeUri(this, destTreeUri)?.createFile(srcFileInfo.type, srcFileInfo.name)?.let {
                val outputStream = contentResolver.openOutputStream(it.uri)
                outputStream?.let {
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    val inputStream = FileInputStream(srcFileInfo.path)
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    inputStream.close()
                }
                outputStream?.close()
                handler.sendMessage(Message.obtain(handler, 0, srcFileInfo.name))
            }
        }
    }
}

private data class FileInfo(
    val name: String,
    val path: String,
    val type: String,
    val size: Long,
    val operate: String=""
)