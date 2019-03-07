proxydroid
======
基于 [VpnService](https://developer.android.com/reference/android/net/VpnService) 的代理、中间人攻击 SDK

Features
------------
* 基于 android VpnService，代理全局的流量
* 支持上游代理服务，内置 Direct、Reject、Socks5
* 支持中间人攻击，解析 http/https 流量 (https 需要安装 rootCA 证书)

架构图
![](https://su.yzcdn.cn/public_files/2019/03/07/9bf6b2015c0718b954b3b223db6e874a.png)


接入
------------

定义你的规则
```kotlin
class CustomRules : Rules {

    val socksIp = "127.0.0.1"
    val socksPort = 8001
    val sockesUserName = "username"
    val socksPasswd = "passwd"

    class BaiduHttpInterceptor : HttpInterceptor {

        /**
         * request 支持的操作请参考 Netty http codec
         */
        override fun onRequest(request: FullHttpRequest): FullHttpRequest {
            return request
        }

        /**
         * response 支持的操作请参考 Netty http codec
         */
        override fun onResponse(response: FullHttpResponse): FullHttpResponse {
            return response.replace(Unpooled.wrappedBuffer(java.lang.String(
                    "<!DOCTYPE html>\n" +
                            "<html>\n" +
                            "<head>\n" +
                            "\t<title>shitty baidu</title>\n" +
                            "</head>\n" +
                            "<body>\n" +
                            "\t<h1>This is hooked html by bwelco</h1>\n" +
                            "\n" +
                            "</body>\n" +
                            "</html>").getBytes())).apply {
                this.headers().remove("Content-Encoding")
            }
        }
    }


    /**
     * 通过 host 来决定此次请求使用哪个拦截器
     */
    override val mitmConfig: HttpInterceptorMatcher? = object : HttpInterceptorMatcher {
        override fun match(host: String): HttpInterceptor? {
            return when(host) {
                "baidu.com" -> BaiduHttpInterceptor()
                else -> null
            }
        }
    }

    /**
     * 可以自定义添加上游代理服务
     */
    override val proxylist =
            mutableMapOf("socks" to Socks5Proxy(Inet4Address.getByName(socksIp), socksPort, sockesUserName, socksPasswd))

    /**
     * 内置 REJECT 和 DIRECT 两种代理类型，分别表示拒绝连接和直接连接
     * 你也可以使用用在 proxylist 中自己添加的代理类型
     */
    override fun proxyMatcher(host: String): String {
        return when {
            host.contains("google") -> "socks"
            host.contains("ads.google.com") -> "REJECT"
            else -> "DIRECT"
        }
    }
}
```
定义一个继承 ProxyService 的 Service，并在 Manifest 中声明。
```kotlin
class CustomProxyService : ProxyService() {
    override val rules: Rules by lazy {
        CustomRules()
    }
}
```
```xml
<service
    android:name=".CustomProxyService"
    android:directBootAware="true"
    android:exported="false"
    android:permission="android.permission.BIND_VPN_SERVICE">
    <intent-filter>
        <action android:name="android.net.VpnService" />
    </intent-filter>
</service>
```

启动服务
```kotlin
VpnHelper.startProxy(this, CustomProxyService::class.java)
```

关闭服务
```kotlin
VpnHelper.stopProxy(this, CustomProxyService::class.java)
```

安装 rootCA
```kotlin
VpnHelper.installCA(getContext())
```


自定义 RootCA
------------
参考 https://aboutssl.org/how-to-create-and-import-self-signed-certificate-to-android-device/    
```shell                                                                                                               
echo basicConstraints=CA:true > android_options.txt                                                            
openssl genrsa -out ca_private.key 1024                                                                        
openssl req -new -days 10680 -key ca_private.key -out ca.pem                                                   
openssl x509 -req -days 10680 -in ca.pem -signkey ca_private.key -extfile ./android_options.txt -out ca.crt    
openssl pkcs8 -topk8 -nocrypt -inform PEM -outform DER -in ca_private.key -out ca_private.der                  
                                                                                                               
generate android system ca                                                                                     
openssl x509 -inform PEM -subject_hash_old -in ca.crt | head -1                                                
cat ca.crt > ${hash}.0                                                                                         
openssl x509 -inform PEM -text -in ca.crt -out /dev/null >> ${hash}.0                                          
                                                                                                               
adb push ${hash}.0 /sdcard                                                                                     
adb shell                                                                                                      
mount -o remount,rw /system                                                                                    
su                                                                                                             
cp -rf /sdacard/${hash}.0 /system/etc/security/cacerts/                                                        
chmod 644 /system/etc/security/cacerts/${hash}.0                                                               
reboot                                                                                                         
```
