package io.github.jeadyx.appmanager

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import io.github.jeadyx.appmanager.ui.theme.AppManagerTheme

class ShareActivity: ComponentActivity() {
    private val TAG = "ShareActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { 
            AppManagerTheme {
                Scaffold {
                    val fileSelector = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {res->
                        Log.d(TAG, "onCreate: get pick resutl $res")
                        // 分享给其他应用
                        if(res.isNotEmpty()){
                            val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
                            intent.type = "*/*"
                            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(res))
                            startActivity(Intent.createChooser(intent, "分享文件"))
                        }
                    }
                    Box(Modifier.padding(it)){
                        ButtonText("分享文件") {
                            fileSelector.launch(arrayOf("*/*"))
                        }
                    }
                }
            }
        }
    }
}