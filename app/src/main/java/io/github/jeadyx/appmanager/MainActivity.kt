package io.github.jeadyx.appmanager

import android.content.ContentResolver.MimeTypeInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastJoinToString
import androidx.compose.ui.window.Popup
import androidx.core.content.FileProvider
import io.github.jeadyx.gitversionmanager.GitManager
import io.github.jeadyx.appmanager.ui.theme.AppManagerTheme
import java.io.File
import java.text.DecimalFormat
import kotlin.random.Random

private val TAG = "MainActivity"
class MainActivity : ComponentActivity() {
    private var gitManager = GitManager("jeady5", "publisher", "e8d83e715fa7b44d2d89b3cf7d7554e0")
    private val fileList = mutableStateListOf<FileStat>()
    private var currentPath by mutableStateOf("")
    private var lastPath by mutableStateOf("")
    private var displayConfigure by mutableStateOf(false)
    private val currentPathWithSlash: String get() = currentPath.takeIf { it.isNotEmpty() }?.plus("/")?:""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppManagerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(Modifier.fillMaxSize()){
                        AppManger(Modifier.padding(innerPadding))
                        if(displayConfigure){
                            DlgConfigure(Modifier.fillMaxSize()){
                                Log.d(TAG, "onCreate: configure $gitConfigure $currentPath")
                                currentPath = ""
                                displayConfigure = false
                                gitManager = GitManager(gitConfigure.owner, gitConfigure.repo, gitConfigure.token)
                                gitManager.setHost(gitConfigure.host)
                                refreshFileList(currentPath)
                            }
                        }
                        Dialog1()
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun AppManger(modifier: Modifier) {
        val context = LocalContext.current
        Column(modifier.padding(horizontal = 10.dp)) {
            val fileSelector = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { res->
                Log.d(TAG, "onCreate: get pick resutl $res")
                // 上传到git
                if(res.isNotEmpty()){
                    res.forEach {uri->
                        contentResolver.openInputStream(uri)?.readBytes()?.let{
                            val name = uri.path?.substringAfterLast("/")?: Random.nextInt(10000, 99999).toString()
                            Log.d(TAG, "AppManger: upload file $name $uri ${it.size}")
                            gitManager.uploadFile(it, "$currentPathWithSlash/$name", "share file $name"){res->
                                Log.d(TAG, "onCreate: upload $name $res")
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
            FlowRow(
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp), horizontalArrangement = Arrangement.SpaceAround
            ) {
                ButtonText("刷新列表") {
                    refreshFileList(currentPath)
                }
                ButtonText(text = "${currentPath.replace("/", ">")} 返回上一层") {
                    backToParent(context)
                }
                ButtonText(text = "上传文件") {
                    fileSelector.launch(arrayOf("*/*"))
                }
                ButtonText(text = "配置") {
                    displayConfigure = true
                }
            }
            LazyVerticalGrid(GridCells.Adaptive(100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(fileList){
                    AppItem(it)
                }
            }
        }
        LaunchedEffect(Unit) {
            refreshFileList(currentPath)
        }
    }

    private fun backToParent(context: Context) {
        lastPath = currentPath
        currentPath = if(currentPath.contains("/")) currentPath.substringBeforeLast("/") else ""
        if(currentPath != lastPath) {
            refreshFileList(currentPath)
        }else{
            showToast(context, "已在根目录")
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            backToParent(this)
        }
        return true
    }

    private fun refreshFileList(filePath: String){
        fileList.clear()
        getFileInfo(filePath){
            if(it.isEmpty()){
                showToast(this, "获取失败")
            }else {
                fileList.addAll(it)
            }
        }
    }

    private fun getFileInfo(filePath: String, callback: (List<FileStat>)->Unit) {
        gitManager.getFileInfo(filePath){res->
            Log.d(TAG, "getFileInfo: get file info $filePath $res")
            res?.let{ callback(it.map {
                FileStat(
                    it,
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(File(it.name).extension)?:"none"
                )
            })}?:callback(emptyList())
        }
    }

    @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
    @Composable
    private fun AppItem(info: FileStat) {
        var longClicked by remember{ mutableStateOf(false) }
        val context = LocalContext.current
        val isFile = info.file.type == "file"
        Box(
            Modifier
                .fillMaxWidth(0.9f)
                .height(100.dp)
                .background(
                    if (isFile) Color(0x3fa0a0a0) else Color(0x3fe0e000),
                    RoundedCornerShape(5.dp)
                )
                .padding(5.dp)
                .shadow(1.dp)
                .padding(5.dp)
                .combinedClickable(onLongClick = {
                    longClicked = !longClicked
                }) {
                    if (!isFile) {
                        currentPath = currentPathWithSlash + info.file.name
                        refreshFileList(currentPath)
                    } else {
                        showToast(context, "查询中")
                        getFileInfo(info.file.path) {
                            Log.d(TAG, "AppItem: get intem info $it")
                            val fileInfo = it[0].file
                            showDialog1(
                                DialogActions(
                                    "name: ${fileInfo.name}\nsize ${gitManager.formatSize(fileInfo.size.toLong())}; ",
                                    cancelText = "取消",
                                    confirmText = "下载"
                                ) {
                                    val fileSavePath =
                                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path + "/AppManager/${info.file.name}"
                                    if (fileInfo.content.isNotBlank() && fileInfo.content.length == fileInfo.size) {
                                        showToast(context, "保存中")
                                        gitManager.saveByteArrayToFile(
                                            fileSavePath,
                                            Base64.decode(fileInfo.content, Base64.DEFAULT)
                                        )
                                        showDialog1(
                                            DialogActions(
                                                "《${info.file.name}》已下载至$fileSavePath",
                                                cancelText = "确定",
                                                confirmText = "打开"
                                            ) {
                                                previewFile(context, fileSavePath)
                                            })
                                        return@DialogActions
                                    }
                                    showToast(context, "下载中")
                                    gitManager.downloadBlobs(info.file.sha, fileSavePath) {
                                        Log.d(TAG, "AppItem: download ${info.file.name} $it")
                                        showDialog1(
                                            "《${info.file.name}》 下载中 ${
                                                DecimalFormat("#.##%").format(
                                                    it.current.toFloat() / it.total
                                                )
                                            }\n路径：${it.savePath}"
                                        )
                                        if (it.progress == 100) {
                                            closeDialog1()
                                            showDialog1(
                                                DialogActions(
                                                    "《${info.file.name}》已下载至${it.savePath}",
                                                    cancelText = "确定",
                                                    confirmText = "打开"
                                                ) {
                                                    previewFile(context, it.savePath)
                                                }
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                },
            contentAlignment= Alignment.Center
        ) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally){
                Image(painterResource(if(isFile) R.drawable.file else R.drawable.folder),
                    contentDescription = null, modifier = Modifier.weight(1f)
                )
                Text(
                    info.file.name,
                    textDecoration = TextDecoration.Underline,
                    color = Color(0xff303030),
                    lineHeight = 20.sp,
                    maxLines = 1
                )
            }
            AnimatedVisibility(longClicked) {
                var offset by remember{ mutableStateOf(IntOffset(50, 50)) }
                Popup(
                    offset=offset,
                    onDismissRequest = {
                        if(isFile) longClicked = false
                    }
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth(0.3f)
                            .fillMaxHeight(0.3f)
                            .background(Color(0xf0ffffff)),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val dragState = rememberDraggable2DState{
                            offset += IntOffset(it.x.toInt(), it.y.toInt())
                        }
                        Text(info.file.name, color = Color.Gray, fontSize = 20.sp,
                            modifier=Modifier.draggable2D(dragState)
                        )
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .background(Color(0x30f05050))
                                    .clickable {
                                        showToast(context, "删除中")
                                        longClicked = false
                                        gitManager.deleteFile(
                                            info.file.path,
                                            info.file.sha,
                                            "delete file"
                                        ) {
                                            Log.d(
                                                TAG,
                                                "AppItem: delete file ${info.file.name} $it $gitManager"
                                            )
                                            if(it == null || it == "Bad Request"){
                                                showToast(context, "删除失败")
                                            } else {
                                                it.let {
                                                    showToast(context, "删除成功")
                                                    refreshFileList(currentPath)
                                                }
                                            }
                                        }
                                    }, contentAlignment = Alignment.Center
                            ) {
                                Text("删除")
                            }
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .background(Color(0x3050f050))
                                    .clickable {
                                        showToast(context, "下载中")
                                        longClicked = false
                                        gitManager.downloadBlobs(
                                            info.file.sha,
                                            Environment.getExternalStoragePublicDirectory(
                                                Environment.DIRECTORY_DOWNLOADS
                                            ).path + "/AppManager/${info.file.name}"
                                        ) {
                                            Log.d(TAG, "AppItem: download ${info.file.name} $it")
                                            showDialog1("《${info.file.name}》 下载中 ${it.progress}%\n路径：${it.savePath}")
                                            if (it.progress == 100) {
                                                closeDialog1()
                                                showDialog1(
                                                    DialogActions(
                                                        "《${info.file.name}》已下载至${it.savePath}",
                                                        cancelText = "确定",
                                                        confirmText = "打开"
                                                    ) {
                                                        previewFile(context, it.savePath)
                                                    })
                                            }
                                            it.errMsg?.let{
                                                closeDialog1()
                                                showDialog1(it)
                                            }
                                        }
                                    }, contentAlignment = Alignment.Center
                            ) {
                                Text("下载")
                            }
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .background(Color(0x305050f0))
                                    .clickable { longClicked = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("取消")
                            }
                        }
                    }
                }
            }
        }
    }
    private fun previewFile(context: Context, filePath: String){
        // 使用系统默认的打开操作
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    File(filePath)
                ),
                MimeTypeMap
                    .getSingleton()
                    .getMimeTypeFromExtension(File(filePath).extension)
            )
            setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(context, "打开失败")
        }
    }
    private fun getColorByType(mimeType: String): Color {
        return when{
//            mimeType.startsWith("image")->Color(0xfff000f0)
//            mimeType.startsWith("video")->Color(0xffc0c000)
//            mimeType.startsWith("audio")->Color(0xfff0f0f0)
//            mimeType.startsWith("application")->Color(0xff00f0f0)
            else->Color(0xfff0f0f0)
        }
    }
}

private data class FileStat(
    val file: GitManager.ResponseInfo,
    val type: String,
    val status: String=""
)