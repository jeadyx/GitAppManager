package io.github.jeadyx.appmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

var gitConfigure by mutableStateOf(GitConfigure("", "jeady5", "publisher", "e8d83e715fa7b44d2d89b3cf7d7554e0"))
@Composable
fun DlgConfigure(modifier: Modifier = Modifier, onClose: ()->Unit) {
    Box(modifier.safeContentPadding().background(Color.Gray).clickable {  }, contentAlignment = Alignment.Center) {
        ButtonText("保存退出", Modifier.align(Alignment.TopEnd)) {
            onClose()
        }
        Column(Modifier.background(Color.White).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("配置Git仓库信息", fontWeight = FontWeight.Bold, fontSize = 30.sp)
            TextField(
                value = gitConfigure.host,
                onValueChange = { gitConfigure = gitConfigure.copy(host = it) },
                placeholder = { Text("域名;default(https://gitee.com)") })
            TextField(
                value = gitConfigure.owner,
                onValueChange = { gitConfigure = gitConfigure.copy(owner = it) },
                placeholder = { Text("用户名") })
            TextField(
                value = gitConfigure.repo,
                onValueChange = { gitConfigure = gitConfigure.copy(repo = it) },
                placeholder = { Text("仓库名") })
            TextField(
                value = gitConfigure.token,
                onValueChange = { gitConfigure = gitConfigure.copy(token = it) },
                placeholder = { Text("access_token") })
        }
    }
}

data class GitConfigure(
    val host: String,
    val owner: String,
    val repo: String,
    val token: String
)