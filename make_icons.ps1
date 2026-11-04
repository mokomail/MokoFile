$ErrorActionPreference = 'Stop'
$res = Join-Path $env:USERPROFILE 'Documents\nichevo\MokoFiles\app\src\main\res'
$dir = Join-Path $res 'drawable'
New-Item -ItemType Directory -Force $dir | Out-Null

function N([double]$v) {
    return $v.ToString([System.Globalization.CultureInfo]::InvariantCulture)
}

function C([double]$cx, [double]$cy, [double]$r) {
    $d = $r * 2
    return ("M{0},{1} m-{2},0 a{2},{2} 0 1,0 {3},0 a{2},{2} 0 1,0 -{3},0" -f (N $cx), (N $cy), (N $r), (N $d))
}

function RR([double]$x, [double]$y, [double]$w, [double]$h, [double]$r) {
    $iw = $w - 2 * $r
    $ih = $h - 2 * $r
    $x2 = $x + $r
    return ("M{0},{1} h{2} a{3},{3} 0 0 1 {3},{3} v{4} a{3},{3} 0 0 1 -{3},{3} h-{2} a{3},{3} 0 0 1 -{3},-{3} v-{4} a{3},{3} 0 0 1 {3},-{3} z" -f (N $x2), (N $y), (N $iw), (N $r), (N $ih))
}

$FOLDER = 'M3 7.5A2.5 2.5 0 0 1 5.5 5H9l2 2.2h7.5A2.5 2.5 0 0 1 21 9.7v7.8A2.5 2.5 0 0 1 18.5 20h-13A2.5 2.5 0 0 1 3 17.5z'
$FILE = 'M6.5 3.5h6L18 9v11a1.5 1.5 0 0 1-1.5 1.5h-10A1.5 1.5 0 0 1 5 20V5a1.5 1.5 0 0 1 1.5-1.5z'
$FOLD = 'M12.5 3.6V9H18'

$icons = [ordered]@{
    'ic_back'         = @(@{ d = 'M19 12H5' }, @{ d = 'm11 6-6 6 6 6' })
    'ic_chevron'      = @(@{ d = 'm9.5 6 6 6-6 6' })
    'ic_search'       = @(@{ d = (C 11 11 6.5) }, @{ d = 'm16 16 4.5 4.5' })
    'ic_close'        = @(@{ d = 'M6 6l12 12M18 6 6 18' })
    'ic_more'         = @(@{ d = (C 12 5.5 1.5); f = $true }, @{ d = (C 12 12 1.5); f = $true }, @{ d = (C 12 18.5 1.5); f = $true })
    'ic_grid'         = @(@{ d = (RR 4 4 7 7 2.2) }, @{ d = (RR 13 4 7 7 2.2) }, @{ d = (RR 4 13 7 7 2.2) }, @{ d = (RR 13 13 7 7 2.2) })
    'ic_list'         = @(@{ d = 'M9 6.5h11M9 12h11M9 17.5h11' }, @{ d = (C 4.6 6.5 1.3); f = $true }, @{ d = (C 4.6 12 1.3); f = $true }, @{ d = (C 4.6 17.5 1.3); f = $true })
    'ic_sort'         = @(@{ d = 'M4 7h9M4 12h6M4 17h3' }, @{ d = 'M18 6v12M18 18l3-3M18 18l-3-3' })
    'ic_folder'       = @(@{ d = $FOLDER })
    'ic_folder_open'  = @(@{ d = 'M3 8V6.5A2 2 0 0 1 5 4.5h4l2 2.2h6.5A2 2 0 0 1 19.5 8.7' }, @{ d = 'M3.4 9.5h17.2l-2 8.4a2 2 0 0 1-1.9 1.6H5.6a2 2 0 0 1-1.9-1.6z' })
    'ic_image'        = @(@{ d = (RR 3.5 4.5 17 15 3) }, @{ d = (C 9 10 1.6) }, @{ d = 'm4.5 17 4.6-4.6a1.6 1.6 0 0 1 2.3 0l2.2 2.2 1.6-1.6a1.6 1.6 0 0 1 2.3 0l2.5 2.5' })
    'ic_video'        = @(@{ d = (RR 3 5.5 13 13 3) }, @{ d = 'm16 11 4.2-2.6A1 1 0 0 1 21.8 9v6a1 1 0 0 1-1.6.6L16 13z' })
    'ic_music'        = @(@{ d = 'M9 18V7.5l9-2v10' }, @{ d = (C 6.8 18 2.2) }, @{ d = (C 15.8 15.5 2.2) })
    'ic_apk'          = @(@{ d = 'M7.5 10a4.5 4.5 0 0 1 9 0v6.5a2 2 0 0 1-2 2h-5a2 2 0 0 1-2-2z' }, @{ d = 'm8.5 6.5-1.5-2M15.5 6.5l1.5-2' }, @{ d = (C 10 11.5 0.9); f = $true }, @{ d = (C 14 11.5 0.9); f = $true })
    'ic_archive'      = @(@{ d = (RR 3.5 4.5 17 4.5 2) }, @{ d = 'M5 9v9.5A1.5 1.5 0 0 0 6.5 20h11a1.5 1.5 0 0 0 1.5-1.5V9' }, @{ d = 'M10 13h4' })
    'ic_document'     = @(@{ d = $FILE }, @{ d = $FOLD }, @{ d = 'M8.5 13.5h7M8.5 17h5' })
    'ic_file'         = @(@{ d = $FILE }, @{ d = $FOLD })
    'ic_copy'         = @(@{ d = (RR 8.5 8.5 11 11 2.5) }, @{ d = 'M15.5 8.5V6A1.5 1.5 0 0 0 14 4.5H6A1.5 1.5 0 0 0 4.5 6v8A1.5 1.5 0 0 0 6 15.5h2.5' })
    'ic_cut'          = @(@{ d = (C 6 6.2 2.3) }, @{ d = (C 6 17.8 2.3) }, @{ d = 'M8.1 7.5 20 18M8.1 16.5 20 6' })
    'ic_paste'        = @(@{ d = 'M9 4.5H7.5A1.5 1.5 0 0 0 6 6v13a1.5 1.5 0 0 0 1.5 1.5h9A1.5 1.5 0 0 0 18 19V6a1.5 1.5 0 0 0-1.5-1.5H15' }, @{ d = (RR 9 3 6 3.4 1.4) })
    'ic_delete'       = @(@{ d = 'M4.5 6.5h15' }, @{ d = 'M9.5 6.5v-2A1.5 1.5 0 0 1 11 3h2a1.5 1.5 0 0 1 1.5 1.5v2' }, @{ d = 'M6.6 6.5 7.5 19a1.6 1.6 0 0 0 1.6 1.5h5.8a1.6 1.6 0 0 0 1.6-1.5l.9-12.5' })
    'ic_edit'         = @(@{ d = 'M4.5 19.5h4L19 9a2.1 2.1 0 0 0-3-3L5.5 16.5z' }, @{ d = 'M14.5 7.5 17 10' })
    'ic_new_folder'   = @(@{ d = $FOLDER }, @{ d = 'M12 11.5v5M9.5 14h5' })
    'ic_upload'       = @(@{ d = 'M12 16V4.5' }, @{ d = 'm7 9 5-5 5 5' }, @{ d = 'M4.5 15.5V18A1.5 1.5 0 0 0 6 19.5h12a1.5 1.5 0 0 0 1.5-1.5v-2.5' })
    'ic_download'     = @(@{ d = 'M12 4.5V16' }, @{ d = 'm7 11 5 5 5-5' }, @{ d = 'M4.5 15.5V18A1.5 1.5 0 0 0 6 19.5h12a1.5 1.5 0 0 0 1.5-1.5v-2.5' })
    'ic_info'         = @(@{ d = (C 12 12 8.5) }, @{ d = 'M12 11v5.5' }, @{ d = (C 12 7.9 1.1); f = $true })
    'ic_check'        = @(@{ d = 'm5 12.5 4.5 4.5L19 7' })
    'ic_storage'      = @(@{ d = (RR 3 4.5 18 15 3) }, @{ d = 'M3 10h18M3 14.5h18' }, @{ d = (C 7 7.2 0.9); f = $true }, @{ d = (C 7 12.2 0.9); f = $true })
    'ic_palette'      = @(@{ d = 'M12 3.5a8.5 8.5 0 1 0 0 17c1.4 0 2.1-.9 2.1-1.9 0-.7-.3-1.2-.7-1.7-.4-.5-.7-1-.7-1.6 0-1 .8-1.8 1.8-1.8h1.6a4.4 4.4 0 0 0 4.4-4.4c0-3.1-3.8-5.6-8.5-5.6z' }, @{ d = (C 8 10 1.2); f = $true }, @{ d = (C 12 7.6 1.2); f = $true }, @{ d = (C 16 10 1.2); f = $true })
    'ic_plus'         = @(@{ d = 'M12 5v14M5 12h14' })
    'ic_settings'     = @(@{ d = 'M19.43 12.98c.04-.32.07-.65.07-.98s-.03-.66-.08-.98l2.11-1.65a.5.5 0 0 0 .12-.64l-2-3.46a.5.5 0 0 0-.61-.22l-2.49 1a7.3 7.3 0 0 0-1.69-.98l-.38-2.65A.49.49 0 0 0 14 2h-4a.49.49 0 0 0-.49.42l-.38 2.65c-.61.25-1.18.58-1.69.98l-2.49-1a.49.49 0 0 0-.61.22l-2 3.46a.5.5 0 0 0 .12.64l2.11 1.65c-.04.32-.08.66-.08.98s.03.66.08.98l-2.11 1.65a.5.5 0 0 0-.12.64l2 3.46c.12.22.39.3.61.22l2.49-1c.51.4 1.08.73 1.69.98l.38 2.65c.03.24.24.42.49.42h4c.25 0 .46-.18.49-.42l.38-2.65c.61-.25 1.18-.58 1.69-.98l2.49 1c.22.08.49 0 .61-.22l2-3.46a.5.5 0 0 0-.12-.64zM12 15.5A3.5 3.5 0 1 1 15.5 12 3.5 3.5 0 0 1 12 15.5z'; f = $true })
    'ic_clock'        = @(@{ d = (C 12 12 8.5) }, @{ d = 'M12 7.5V12l3 2' })
    'ic_select_all'   = @(@{ d = 'M4 8V5.5A1.5 1.5 0 0 1 5.5 4H8M16 4h2.5A1.5 1.5 0 0 1 20 5.5V8M20 16v2.5a1.5 1.5 0 0 1-1.5 1.5H16M8 20H5.5A1.5 1.5 0 0 1 4 18.5V16' }, @{ d = 'm9 12 2 2 4-4' })
    'ic_drive'        = @(@{ d = 'M5.5 19.5h13a2.5 2.5 0 0 0 2.3-3.5l-4.6-9.6a2.5 2.5 0 0 0-2.3-1.4h-3.8a2.5 2.5 0 0 0-2.3 1.4L3.2 16a2.5 2.5 0 0 0 2.3 3.5z' }, @{ d = 'M8 9.5h8' })
    'ic_refresh'      = @(@{ d = 'M20 12a8 8 0 1 1-2.4-5.7' }, @{ d = 'M20 4v4.5h-4.5' })
    'ic_star'         = @(@{ d = 'm12 3.6 2.6 5.5 6 .8-4.4 4.2 1.1 6-5.3-3-5.3 3 1.1-6L3.4 9.9l6-.8z' })
}

$utf8 = New-Object System.Text.UTF8Encoding($false)

foreach ($name in $icons.Keys) {
    $sb = New-Object System.Text.StringBuilder
    [void]$sb.AppendLine('<?xml version="1.0" encoding="utf-8"?>')
    [void]$sb.AppendLine('<vector xmlns:android="http://schemas.android.com/apk/res/android"')
    [void]$sb.AppendLine('    android:width="24dp"')
    [void]$sb.AppendLine('    android:height="24dp"')
    [void]$sb.AppendLine('    android:viewportWidth="24"')
    [void]$sb.AppendLine('    android:viewportHeight="24">')

    foreach ($p in $icons[$name]) {
        if ($p.f) {
            [void]$sb.AppendLine(('    <path android:pathData="{0}" android:fillColor="#FFFFFFFF" />' -f $p.d))
        } else {
            [void]$sb.AppendLine(('    <path android:pathData="{0}" android:fillColor="#00000000" android:strokeColor="#FFFFFFFF" android:strokeWidth="1.7" android:strokeLineCap="round" android:strokeLineJoin="round" />' -f $p.d))
        }
    }
    [void]$sb.AppendLine('</vector>')
    [System.IO.File]::WriteAllText((Join-Path $dir "$name.xml"), $sb.ToString(), $utf8)
}

# иконка приложения
$fg = @'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <group
        android:scaleX="2.1"
        android:scaleY="2.1"
        android:translateX="28.8"
        android:translateY="28.8">
        <path
            android:pathData="M3 7.5A2.5 2.5 0 0 1 5.5 5H9l2 2.2h7.5A2.5 2.5 0 0 1 21 9.7v7.8A2.5 2.5 0 0 1 18.5 20h-13A2.5 2.5 0 0 1 3 17.5z"
            android:fillColor="#00000000"
            android:strokeColor="#FFEADDF7"
            android:strokeWidth="1.7"
            android:strokeLineCap="round"
            android:strokeLineJoin="round" />
    </group>
</vector>
'@
[System.IO.File]::WriteAllText((Join-Path $dir 'ic_launcher_foreground.xml'), $fg, $utf8)

$mipmap = Join-Path $res 'mipmap-anydpi-v26'
New-Item -ItemType Directory -Force $mipmap | Out-Null
$adaptive = @'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/launcher_bg" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
'@
[System.IO.File]::WriteAllText((Join-Path $mipmap 'ic_launcher.xml'), $adaptive, $utf8)
[System.IO.File]::WriteAllText((Join-Path $mipmap 'ic_launcher_round.xml'), $adaptive, $utf8)

Write-Output ("icons: " + (Get-ChildItem $dir -Filter 'ic_*.xml' | Measure-Object).Count)
