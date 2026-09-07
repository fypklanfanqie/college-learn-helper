package com.zhiwei.math

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zhiwei.math.ui.AppRoot
import java.lang.ref.WeakReference

/**
 * 玻璃采样宿主：MainActivity 装配的一个兄弟层（ComposeView 之外），
 * 供 QmDeve LiquidGlassView bind —— 源不含玻璃 → record() 无自引用 → 不递归不崩。
 * （结构与聊天终端安卓本地的性能浮窗完全一致：玻璃是根内容视图的兄弟。）
 */
object GlassHost {
    private var ref: WeakReference<ViewGroup>? = null

    /** 由 MainActivity 装配时注入 */
    fun attach(host: ViewGroup) {
        ref = WeakReference(host)
    }

    /** 玻璃 bind 用；null 表示宿主未装配（回退静态背板） */
    fun source(): ViewGroup? = ref?.get()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 结构：content(FrameLayout)
        //   ├─ glassHostSource(FrameLayout)   ← 玻璃采样源（不含玻璃，防自引用）
        //   └─ ComposeView(AppRoot)           ← 真实 UI（玻璃面板在其内）
        val content = FrameLayout(this)
        val glassHostSource = FrameLayout(this).apply {
            // 柔和纵向渐变色场：玻璃可折射的"环境"；与暗色主题背景同系
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.argb(255, 24, 30, 46),
                    Color.argb(255, 18, 24, 38),
                    Color.argb(255, 12, 17, 30),
                ),
            )
        }
        GlassHost.attach(glassHostSource)

        content.addView(
            glassHostSource,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        val compose = androidx.compose.ui.platform.ComposeView(this).apply {
            setContent { AppRoot() }
        }
        content.addView(
            compose,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        setContentView(content)
    }
}
