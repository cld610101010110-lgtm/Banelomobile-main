# Build.gradle Dependencies for PostgreSQL

Add these dependencies to your `app/build.gradle` file to enable PostgreSQL connectivity.

## 1. Open `app/build.gradle`

## 2. Add PostgreSQL JDBC Driver

In the `dependencies` section, add:

```gradle
dependencies {
    // Existing dependencies...

    // PostgreSQL JDBC Driver
    implementation 'org.postgresql:postgresql:42.7.1'

    // Coroutines (if not already added)
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3'
}
```

## 3. Add Internet Permission

In `AndroidManifest.xml`, add (if not already present):

```xml
<manifest ...>
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application ...>
        ...
    </application>
</manifest>
```

## 4. Configure Network Security (Android 9+)

Create `res/xml/network_security_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>

    <!-- Allow cleartext traffic for local development -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">localhost</domain>
        <domain includeSubdomains="true">192.168.1.0</domain>
    </domain-config>
</network-security-config>
```

Then reference it in `AndroidManifest.xml`:

```xml
<application
    ...
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

## 5. Sync Gradle

Click "Sync Now" when Android Studio prompts you, or:
- **Build → Clean Project**
- **Build → Rebuild Project**

## 6. Test Connection

Use the test function in your MainActivity:

```kotlin
lifecycleScope.launch {
    val result = PostgreSQLAdapter.testConnection()
    result.onSuccess { message ->
        Log.d("MainActivity", message)
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
    }.onFailure { error ->
        Log.e("MainActivity", "Connection failed: ${error.message}")
        Toast.makeText(this@MainActivity, "Connection failed: ${error.message}", Toast.LENGTH_LONG).show()
    }
}
```

## 7. Find Your Local IP Address

### For Android Emulator:
- Use `10.0.2.2` (special IP that points to host machine)

### For Physical Android Device:
1. Open Command Prompt
2. Run: `ipconfig`
3. Find "IPv4 Address" under your active network adapter
4. Example: `192.168.1.100`
5. Update `DB_HOST` in `PostgreSQLAdapter.kt`

## 8. Ensure PostgreSQL Accepts Remote Connections

Edit `C:\Program Files\PostgreSQL\18\data\postgresql.conf`:

```conf
listen_addresses = '*'
```

Edit `C:\Program Files\PostgreSQL\18\data\pg_hba.conf`, add:

```conf
host    all             all             0.0.0.0/0               md5
host    all             all             ::/0                    md5
```

Restart PostgreSQL service after changes.

## Troubleshooting

### "No suitable driver found"
- Make sure `org.postgresql:postgresql` is in dependencies
- Sync Gradle and rebuild project

### "Connection refused"
- Check PostgreSQL is running: `netstat -an | findstr 5432`
- Check firewall allows port 5432
- Verify `DB_HOST` IP address is correct

### "Connection timeout"
- Make sure Android device/emulator can reach your computer
- Ping from Android: Use apps like "Network Tools"
- Check Windows Firewall allows PostgreSQL

### "Password authentication failed"
- Verify password in `PostgreSQLAdapter.kt` matches your PostgreSQL password
- Check user exists: `psql -U postgres -c "\du"`
