package com.example.recordmodule.record

import android.media.MediaPlayer
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.example.recordmodule.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.Timer
import kotlin.concurrent.timer
import kotlin.math.roundToInt

private const val TAG = "RecordUI"

val Background1Color = Color(0xFFFFFFFF)
val Background4Color = Color(0xFF111111)
val Sub1Color = Color(0xFF707D8D)
val TextColor = Color(0xFF111111)
val Point1Color = Color(0xFF3973B9)
val Point4Color = Color(0xFFEC6730)
val DisableColor = Color(0xFFDFDFDF)

val Divider1Color = Color(0xFFDFDFDF)
val Divider2Color = Color(0x61FFFFFF)

val Colors.background1Color: Color
    get() = Background1Color

val Colors.background4Color: Color
    get() = Background4Color

val Colors.sub1Color: Color
    get() = Sub1Color

val Colors.textColor: Color
    get() = TextColor

val Colors.point1Color: Color
    get() = Point1Color

val Colors.point4Color: Color
    get() = Point4Color

val Colors.disableColor: Color
    get() = DisableColor

@Preview(device = Devices.AUTOMOTIVE_1024p, backgroundColor = 0xFFFFFF)
@Composable
fun FloatingButtons() {
    var state by remember { mutableStateOf<RecordState>(RecordState.None) }
    val list = remember {
        mutableListOf(
            RecordData("test1", "path", "test1.mp3", 1000L, 100L),
            RecordData("test2", "path", "test2.mp3", 1000L, 100L),
            RecordData("test3", "path", "test3.mp3", 1000L, 100L),
            RecordData("test4", "path", "test4.mp3", 1000L, 100L)
        )
    }

    FloatingRecordButton(
        showFloatingRecordButton = true,
        recordState = state,
        recordTime = "01:00",
        recordFileList = list,
        onRecord = { state = RecordState.Recoding },
        onResume = { state = RecordState.Recoding },
        onPause = { state = RecordState.Paused },
        onStop = { state = RecordState.None },
        onRecordList = { }
    )
}

@Preview
@Composable
fun PreviewMicBubble() {

    var state by remember { mutableStateOf<RecordState>(RecordState.None) }
    val list = remember {
        mutableListOf(
            RecordData("test1", "path", "test1.mp3", 1000L, 100L),
            RecordData("test2", "path", "test2.mp3", 1000L, 100L),
            RecordData("test3", "path", "test3.mp3", 1000L, 100L),
            RecordData("test4", "path", "test4.mp3", 1000L, 100L)
        )
    }

    MicBubble(
        recordState = state,
        recordTime = "01:00",
        recordFileList = list,
        onRecord = { state = RecordState.Recoding },
        onResume = { state = RecordState.Recoding },
        onPause = { state = RecordState.Paused },
        onStop = { state = RecordState.None },
        onRecordList = { }
    )
}

@Composable
fun FloatingRecordButton(
    showFloatingRecordButton: Boolean,
    recordState: RecordState,
    recordTime: String,
    recordFileList: List<RecordData>,
    onRecord: () -> Unit,
    onResume: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onRecordList: () -> Unit,
    onRecordList2: () -> Unit = {},
) {
    val density = LocalDensity.current
    var offsetX by remember { mutableStateOf(with(density) { -30.dp.toPx() }) }
    var offsetY by remember { mutableStateOf(with(density) { 80.dp.toPx() }) }
    var extended by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp
    val screenWidth = configuration.screenWidthDp

    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(x = offsetX.roundToInt(), y = offsetY.roundToInt())
    ) {
        AnimatedVisibility(showFloatingRecordButton) {
            Box(modifier = Modifier
                .padding(5.dp)
                .shadow(
                    if (extended) 0.dp else 3.dp,
                    if (extended) RoundedCornerShape(0.dp) else CircleShape
                )
                .clip(if (extended) RoundedCornerShape(0.dp) else CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                        // 화면 밖으로 나가는거 방지
                        if (offsetX > 0) {
                            offsetX = 0F
                        }
                        if (offsetY < 0) {
                            offsetY = 0F
                        }
                        if (-offsetX.toDp() > screenWidth.dp) {
                            offsetX = -screenWidth.dp.toPx()
                        }
                        if (offsetY.toDp() > screenHeight.dp) {
                            offsetY = screenHeight.dp.toPx()
                        }
                    }
                }
                .defaultMinSize(60.dp, 60.dp)
            ) {
                AnimatedVisibility(
                    extended,
                    enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                    exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.TopStart)
                ) {
                    MicBubble(
                        recordState = recordState,
                        recordTime = recordTime,
                        recordFileList = recordFileList,
                        onRecord = onRecord,
                        onResume = onResume,
                        onPause = onPause,
                        onStop = onStop,
                        onRecordList = onRecordList,
                        onRecordList2 = onRecordList2,
                        onClose = { extended = false },
                        isMainButton = true
                    )
                }
                AnimatedVisibility(
                    !extended,
                    enter = fadeIn() + expandIn(expandFrom = Alignment.TopStart),
                    exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
                ) {
                    Column(
                        modifier = Modifier
                            .size(60.dp, 60.dp)
                            .background(
                                when (recordState) {
                                    RecordState.None -> {
                                        Color(0xFFB5B8CA)
                                    }

                                    RecordState.Recoding -> {
                                        Color(0xFFEC6730)
                                    }

                                    RecordState.Paused -> {
                                        Color(0xFFB5B8CA)
                                    }
                                }
                            )
                            .clickable { extended = !extended },
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.bicon_mic),
                            modifier = Modifier.height(32.dp),
                            contentDescription = ""
                        )
                    }
                }
            }
        }
    }

    if (showFloatingRecordButton) {
        when (recordState) {
            RecordState.Recoding -> {}
            RecordState.None,
            RecordState.Paused -> {
//                Box(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .background(MaterialTheme.colors.background4Color.copy(alpha = 0.5f))
//                        .clickable(
//                            interactionSource = remember { MutableInteractionSource() },
//                            indication = null
//                        ) { }
//                ) {
//                    Column(
//                        modifier = Modifier.fillMaxSize(),
//                        verticalArrangement = Arrangement.Center,
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
//                        Text(text = "진행하려면 녹음을 시작해주세요.", color = Color.White)
//                    }
//                }
            }
        }
    }
}

@Composable
fun Bubble(
    showSub: Boolean = false,
    modifier: Modifier = Modifier,
    triangleAlign: Alignment.Horizontal = Alignment.CenterHorizontally,
    triangleOffsetX: Dp = 0.dp,
    shape: Shape = RoundedCornerShape(30.dp),
    backgroundColor: Color = MaterialTheme.colors.background4Color,
    content: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .defaultMinSize(80.dp, 60.dp)
            .then(modifier)
    ) {
        if (!showSub) {
            Canvas(
                modifier = Modifier
                    .align(triangleAlign)
                    .size(12.dp, 8.dp)
                    .offset(x = triangleOffsetX)
            ) {
                drawPath(
                    path = Path().apply {
                        moveTo(size.width * 0.5f, size.height * 0f)
                        lineTo(size.width * 0f, size.height * 1f)
                        lineTo(size.width * 1f, size.height * 1f)
                        close()
                    },
                    color = backgroundColor,
                )
            }
        }
        Row(
            modifier = Modifier
                .defaultMinSize(80.dp, 60.dp)
                .clip(shape)
                .background(backgroundColor)
                .then(modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            content?.invoke()
        }
    }
}

@Composable
fun BubbleButton(
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    @DrawableRes icon: Int,
    @DrawableRes selectedIcon: Int,
    text: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }

    val selectableModifier = Modifier.selectable(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        role = Role.RadioButton,
        interactionSource = interactionSource,
        indication = null
    )
    val pressed by interactionSource.collectIsPressedAsState()

    val color = if (selected or pressed) MaterialTheme.colors.point4Color else Color.Transparent

    val shape = RoundedCornerShape(24.dp)

    val imageId = if (selected or pressed) selectedIcon else icon

    val textStyle = TextStyle(
        color = if (enabled) {
            MaterialTheme.colors.background1Color
        } else MaterialTheme.colors.disableColor,
        fontSize = 16.sp
    )

    Box(
        Modifier
            .defaultMinSize(if (text == null) 40.dp else 105.dp, 40.dp)
            .then(modifier)
            .background(
                color = color,
                shape = shape
            )
            .clip(shape)
            .then(selectableModifier),
        propagateMinConstraints = true
    ) {
        Row(
            modifier = if (text != null) {
                Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            } else {
                Modifier.padding(8.dp)
            },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = imageId),
                contentDescription = "",
                contentScale = ContentScale.FillWidth,
                alpha = if (enabled) DefaultAlpha else ContentAlpha.disabled
            )

            if (text != null) {
                Text(modifier = Modifier.padding(start = 12.dp), text = text, style = textStyle)
            }
        }
    }
}

@Composable
fun DividerLine(dividerColor: Color = Divider1Color, height: Dp = 24.dp, width: Dp = 1.dp) {
    Divider(
        modifier = Modifier
            .height(height)
            .width(width),
        color = dividerColor
    )
}

@Composable
fun MicBubble(
    recordState: RecordState,
    recordTime: String,
    recordFileList: List<RecordData>,
    onRecord: () -> Unit,
    onResume: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onRecordList: () -> Unit,
    onRecordList2: () -> Unit = { },
    onClose: () -> Unit = {},
    isMainButton: Boolean = false
) {
    var listPopupControl by remember { mutableStateOf(false) }
    var listPopupControl2 by remember { mutableStateOf(false) }
    var selectedRecordData by remember { mutableStateOf<RecordData?>(null) }

    Column {
        Bubble(modifier = Modifier.defaultMinSize(minWidth = 368.dp), showSub = isMainButton) {
            Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (recordState) {
                    RecordState.None -> {
                        BubbleButton(
                            selected = false,
                            // 녹취 시작
                            onClick = {
                                onRecord()
                                listPopupControl = false
                                listPopupControl2 = false
                                selectedRecordData = null
                            },
                            icon = R.drawable.micon_record,
                            selectedIcon = R.drawable.micon_record_on,
                            text = "녹취"
                        )
                    }

                    RecordState.Paused -> {
                        Box(
                            Modifier
                                .size(105.dp, 40.dp)
                                .background(color = Color.Transparent)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp, 14.dp)
                                            .background(Color(0xFFDFDFDF))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp, 14.dp)
                                            .background(Color(0xFFDFDFDF))
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = recordTime,
                                    style = TextStyle(
                                        color = MaterialTheme.colors.background1Color,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                        }
                    }

                    RecordState.Recoding -> {
                        Box(
                            Modifier
                                .size(105.dp, 40.dp)
                                .background(color = Color.Transparent)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEC6730))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = recordTime,
                                    style = TextStyle(
                                        color = MaterialTheme.colors.background1Color,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                DividerLine(dividerColor = Divider2Color, height = 16.dp)
                Spacer(modifier = Modifier.width(12.dp))
                BubbleButton(
                    selected = false,
                    onClick = { if (recordState == RecordState.Paused) onResume() else onPause() },
                    enabled = recordState != RecordState.None,
                    icon = if (recordState == RecordState.Paused) R.drawable.micon_record else R.drawable.micon_pause_on,
                    selectedIcon = if (recordState == RecordState.Paused) R.drawable.micon_record_on else R.drawable.micon_pause_on
                )
                Spacer(modifier = Modifier.width(8.dp))
                BubbleButton(
                    selected = false,
                    // 녹취 종료
                    onClick = {
                        onStop()
                    },
                    enabled = recordState != RecordState.None,
//                    enabled = false, // 23.02.23 기준 녹음 중 중지 불가.
                    icon = R.drawable.micon_stop_on,
                    selectedIcon = R.drawable.micon_stop_on
                )
                Spacer(modifier = Modifier.width(12.dp))
                DividerLine(dividerColor = Divider2Color, height = 16.dp)
                Spacer(modifier = Modifier.width(12.dp))
                BubbleButton(
                    selected = listPopupControl,
                    onClick = {
                        onRecordList()
                        listPopupControl = !listPopupControl
                        listPopupControl2 = false
                        selectedRecordData = null
                    },
                    enabled = recordState == RecordState.None,
                    icon = R.drawable.micon_list_on,
                    selectedIcon = R.drawable.micon_list_on,
                    text = "목록"
                )
                BubbleButton(
                    selected = listPopupControl2,
                    onClick = {
                        onRecordList2()
                        listPopupControl2 = !listPopupControl2
                        listPopupControl = false
                        selectedRecordData = null
                    },
                    enabled = recordState == RecordState.None,
                    icon = R.drawable.micon_list_on,
                    selectedIcon = R.drawable.micon_list_on,
                    text = "임시목록"
                )
                if (isMainButton) {
                    Spacer(modifier = Modifier.width(12.dp))
                    DividerLine(dividerColor = Divider2Color, height = 16.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    BubbleButton(
                        selected = false,
                        // 플로팅버튼 닫기
                        onClick = {
                            onClose()
                        },
                        icon = R.drawable.micon_cancle_on,
                        selectedIcon = R.drawable.micon_cancle_on
                    )
                }
            }
        }
        AnimatedVisibility(listPopupControl) {
            Column(modifier = Modifier.width(368.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                Bubble(
                    showSub = true,
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = MaterialTheme.colors.background4Color.copy(alpha = 0.8f),
                    modifier = Modifier.requiredSizeIn(
                        minWidth = 368.dp,
                        maxWidth = 368.dp,
                        minHeight = 40.dp,
                        maxHeight = 160.dp
                    )
                ) {
                    if (recordFileList.isEmpty()) {
                        Text(
                            text = "녹취된 음성이 없습니다.", style = TextStyle(
                                color = MaterialTheme.colors.background1Color,
                                fontSize = 14.sp
                            )
                        )
                    } else {
                        LazyColumn {
                            items(items = recordFileList) { item ->
                                FileItem(file = item, onClick = {
                                    selectedRecordData = item
                                })
                                Divider(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp),
                                    color = Divider2Color
                                )
                            }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(listPopupControl2) {
            Column(modifier = Modifier.width(368.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                Bubble(
                    showSub = true,
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = MaterialTheme.colors.background4Color.copy(alpha = 0.8f),
                    modifier = Modifier.requiredSizeIn(
                        minWidth = 368.dp,
                        maxWidth = 368.dp,
                        minHeight = 40.dp,
                        maxHeight = 160.dp
                    )
                ) {
                    if (recordFileList.isEmpty()) {
                        Text(
                            text = "녹취된 음성이 없습니다.", style = TextStyle(
                                color = MaterialTheme.colors.background1Color,
                                fontSize = 14.sp
                            )
                        )
                    } else {
                        LazyColumn {
                            items(items = recordFileList) { item ->
                                FileItem(file = item, onClick = {
                                    selectedRecordData = item
                                })
                                Divider(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp),
                                    color = Divider2Color
                                )
                            }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(selectedRecordData != null) {
            selectedRecordData?.let {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    PlayerBubble(
                        recordData = it,
                        onClose = { selectedRecordData = null })
                }
            }
        }
    }
}

@Composable
fun FileItem(
    file: RecordData,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }

    val selectableModifier = Modifier.selectable(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        role = Role.RadioButton,
        interactionSource = interactionSource,
        indication = null
    )

    val pressed by interactionSource.collectIsPressedAsState()

    val color = if (selected or pressed) MaterialTheme.colors.point4Color else Color.Transparent

    val textStyle = TextStyle(
        color = MaterialTheme.colors.background1Color,
        fontSize = 14.sp
    )
    Box(
        Modifier
            .defaultMinSize(40.dp, 40.dp)
            .then(modifier)
            .background(color = color)
            .then(selectableModifier),
        propagateMinConstraints = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 24.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = file.fileName, style = textStyle)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = SimpleDateFormat(" mm : ss ").format(file.recordLength),
                style = textStyle
            )
            Spacer(modifier = Modifier.width(12.dp))
            DividerLine(dividerColor = Divider2Color, height = 12.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${String.format("%.2f", file.fileSize.toDouble() / 1024 / 1024)}mb",
                style = textStyle
            )
        }
    }
}

@Composable
fun PlayerBubble(
    recordData: RecordData,
    onClose: () -> Unit = { }
) {
    Bubble(
        showSub = true,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = MaterialTheme.colors.background4Color.copy(alpha = 0.8f),
        modifier = Modifier.size(368.dp, 116.dp)
    ) {
        var mediaPlayer by remember { mutableStateOf(MediaPlayer()) }
        var timerTask by remember { mutableStateOf(Timer()) }
        var currentPosition by remember { mutableStateOf(0) }

        DisposableEffect(recordData) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(recordData.filePath)    //경로+파일명
                    prepare()
                    start()     //음악 재생
                    isLooping = false   //반복 재생x
                }

                timerTask = timer(period = 1000) {
                    currentPosition = mediaPlayer.currentPosition
                }


            } catch (e: Exception) {
                e.localizedMessage?.let { Log.e(TAG, it) }
            }

            onDispose {
                mediaPlayer.stop()
                timerTask.cancel()
            }
        }

        Column {
            Row(
                modifier = Modifier.size(368.dp, 60.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.micon_playing_on),
                    contentDescription = "",
                    contentScale = ContentScale.Inside
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = recordData.fileName,
                    style = TextStyle(color = MaterialTheme.colors.background1Color)
                )
            }
            Row(
                modifier = Modifier
                    .size(368.dp, 56.dp)
                    .background(MaterialTheme.colors.background4Color),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00A18C))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${
                        SimpleDateFormat(" mm : ss ", Locale.getDefault())
                            .apply { timeZone = TimeZone.getTimeZone("UTC") }
                            .format(Date(currentPosition.toLong()))
                    } / ${
                        SimpleDateFormat(" mm : ss ", Locale.getDefault())
                            .apply { timeZone = TimeZone.getTimeZone("UTC") }
                            .format(Date(recordData.recordLength))
                    }",
                    style = TextStyle(
                        color = MaterialTheme.colors.background1Color,
                        fontSize = 14.sp
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                DividerLine(dividerColor = Divider2Color, height = 16.dp)
                Spacer(modifier = Modifier.width(12.dp))
                BubbleButton(
                    selected = false,
                    onClick = {
                        if (mediaPlayer.isPlaying) {
                            mediaPlayer.pause()
                        } else {
                            mediaPlayer.start()
                        }
                    },
                    icon =
                    if (mediaPlayer.isPlaying) R.drawable.micon_pause_on else R.drawable.micon_play,
                    selectedIcon =
                    if (mediaPlayer.isPlaying) R.drawable.micon_pause_on else R.drawable.micon_play,
                )
                Spacer(modifier = Modifier.width(8.dp))
                BubbleButton(
                    selected = false,
                    onClick = {
                        mediaPlayer.pause()
                        mediaPlayer.seekTo(0)
                    },
                    icon = R.drawable.micon_stop_on,
                    selectedIcon = R.drawable.micon_stop_on
                )
                Spacer(modifier = Modifier.width(8.dp))
                BubbleButton(
                    selected = false,
                    onClick = { onClose() },
                    icon = R.drawable.micon_cancle_on,
                    selectedIcon = R.drawable.micon_cancle_on
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
        }
    }
}