// Po „npx cap add android“: leidimai (GPS fone, Health Connect), ikonos, tekstai, pasirašymas.
const fs = require('fs'), path = require('path');
const A = path.join(__dirname, '..', 'android', 'app'), MAIN = path.join(A, 'src', 'main');
const cp = (src, dst) => { for (const e of fs.readdirSync(src, {withFileTypes: true})) { const s = path.join(src, e.name), d = path.join(dst, e.name); if (e.isDirectory()) { fs.mkdirSync(d, {recursive: true}); cp(s, d); } else fs.copyFileSync(s, d); } };

// 1. ikonos (oranžinis Daigelis) ir pranešimo ikona
cp(path.join(__dirname, '..', 'assets', 'res'), path.join(MAIN, 'res'));

// 2. leidimai
const mf = path.join(MAIN, 'AndroidManifest.xml'); let m = fs.readFileSync(mf, 'utf8');
const perms = ['android.permission.ACCESS_COARSE_LOCATION', 'android.permission.ACCESS_FINE_LOCATION', 'android.permission.FOREGROUND_SERVICE', 'android.permission.FOREGROUND_SERVICE_LOCATION', 'android.permission.POST_NOTIFICATIONS',
  'android.permission.health.READ_STEPS', 'android.permission.health.READ_DISTANCE', 'android.permission.health.READ_ACTIVE_CALORIES_BURNED', 'android.permission.health.READ_HEART_RATE', 'android.permission.health.READ_EXERCISE', 'android.permission.health.READ_HEALTH_DATA_HISTORY'];
for (const p of perms) if (!m.includes(`"${p}"`)) m = m.replace('</manifest>', `    <uses-permission android:name="${p}" />\n</manifest>`);
// Health Connect: kodėl reikia leidimų (Android 14+) – rodo privatumo puslapį
if (!m.includes('VIEW_PERMISSION_USAGE')) m = m.replace('</application>', `        <activity-alias android:name="ViewPermissionUsageActivity" android:exported="true" android:targetActivity=".MainActivity" android:permission="android.permission.START_VIEW_PERMISSION_USAGE">
            <intent-filter>
                <action android:name="android.intent.action.VIEW_PERMISSION_USAGE" />
                <category android:name="android.intent.category.HEALTH_PERMISSIONS" />
            </intent-filter>
        </activity-alias>
    </application>`);
if (!m.includes('ACTION_SHOW_PERMISSIONS_RATIONALE')) m = m.replace(/(<activity[^>]*\.MainActivity[\s\S]*?<\/intent-filter>)/, `$1
            <intent-filter>
                <action android:name="androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" />
            </intent-filter>`);
fs.writeFileSync(mf, m);

// 3. tekstai: privatumo puslapis, GPS pranešimas
const sx = path.join(MAIN, 'res', 'values', 'strings.xml'); let s = fs.readFileSync(sx, 'utf8');
const add = {
  health_connect_privacy_policy_url: 'https://bimox5.github.io/life-schedule/privatumas.html',
  capacitor_background_geolocation_notification_channel_name: 'Sportas',
  capacitor_background_geolocation_notification_icon: 'drawable/ic_tracking',
  capacitor_background_geolocation_notification_color: '#FF8040',
};
for (const [k, v] of Object.entries(add)) if (!s.includes(`name="${k}"`)) s = s.replace('</resources>', `    <string name="${k}">${v}</string>\n</resources>`);
fs.writeFileSync(sx, s);

// 4. pasirašymas (raktas – GitHub paslaptis) ir versijos numeris
const bg = path.join(A, 'build.gradle'); let g = fs.readFileSync(bg, 'utf8');
if (!g.includes('signingConfigs')) {
  g = g.replace(/android\s*\{/, `android {
    signingConfigs {
        release {
            storeFile file(System.getenv("KS_FILE") ?: "release.p12")
            storePassword System.getenv("KS_PASS")
            keyAlias System.getenv("KS_ALIAS") ?: "lifeschedule"
            keyPassword System.getenv("KS_PASS")
            storeType "pkcs12"
        }
    }`);
  g = g.replace(/buildTypes\s*\{\s*release\s*\{/, `buildTypes {
        release {
            signingConfig signingConfigs.release`);
}
g = g.replace(/versionCode \d+/, `versionCode ${parseInt(process.env.BUILD_NO || '1', 10)}`).replace(/versionName "[^"]*"/, `versionName "${process.env.APP_VER || '1.0'}"`);
fs.writeFileSync(bg, g);
// 5. Health Connect reikalauja Android 8+ (minSdk 26)
const vg = path.join(__dirname, '..', 'android', 'variables.gradle'); if (fs.existsSync(vg)) fs.writeFileSync(vg, fs.readFileSync(vg, 'utf8').replace(/minSdkVersion\s*=\s*\d+/, 'minSdkVersion = 26'));
// 6. Laisvės valdiklis (pagrindinio ekrano): Java klasės, išdėstymas, gavėjas manifeste, įskiepis MainActivity
const NAT = path.join(__dirname, '..', 'native'), JAVA = path.join(MAIN, 'java', 'lt', 'lifeschedule', 'app');
fs.mkdirSync(JAVA, {recursive: true});
for (const f of fs.readdirSync(path.join(NAT, 'java'))) fs.copyFileSync(path.join(NAT, 'java', f), path.join(JAVA, f));
cp(path.join(NAT, 'res'), path.join(MAIN, 'res'));
m = fs.readFileSync(mf, 'utf8');
if (!m.includes('.LsWidget"')) m = m.replace('</application>', `        <receiver android:name=".LsWidget" android:exported="false" android:label="Laisvė">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data android:name="android.appwidget.provider" android:resource="@xml/ls_widget_info" />
        </receiver>
    </application>`);
fs.writeFileSync(mf, m);
s = fs.readFileSync(sx, 'utf8');
if (!s.includes('name="ls_widget_desc"')) s = s.replace('</resources>', '    <string name="ls_widget_desc">Laisvės takas, dienos ir +1</string>\n</resources>');
fs.writeFileSync(sx, s);
console.log('android patched');
