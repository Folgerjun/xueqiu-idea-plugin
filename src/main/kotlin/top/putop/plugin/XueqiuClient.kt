package top.putop.plugin

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class XueqiuClient {

    val userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36";
    val userMobileAgent: String = "Xueqiu Android 14.10";

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    var cookie: String = ""

    fun getHot(maxId: Long = -1): List<XueqiuPost> {
        val url = "https://xueqiu.com/v4/statuses/public_timeline_by_category.json?category=-1&count=15&since_id=-1&max_id=$maxId"
        return fetchPosts(url, "list")
    }

    fun getFollowing(maxId: Long = -1): List<XueqiuPost> {
        val url = "https://xueqiu.com/v4/statuses/system/home_timeline.json?usergroup_id=-1&count=15&max_id=$maxId"
        return fetchPosts(url, "home_timeline")
    }

    fun getNews(maxId: Long = -1): List<XueqiuPost> {
        val url = "https://xueqiu.com/v4/statuses/public_timeline_by_category.json?category=6&count=15&since_id=-1&max_id=$maxId"
        return fetchPosts(url, "list")
    }

    // 新增：获取帖子详情
    fun getPostDetail(id: Long): String {
        if (cookie.isEmpty()) return "请先设置 Cookie"
        val url = "https://api.xueqiu.com/statuses/show.json?id=$id"
        
        val request = Request.Builder()
            .url(url)
            .header("Host", "api.xueqiu.com")
            .header("User-Agent", userMobileAgent)
            .header("Cookie", cookie)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return "获取详情失败: ${response.code}"
                val body = response.body?.string() ?: return "详情内容为空"
                val json = gson.fromJson(body, JsonObject::class.java)
                cleanHtml(if (json.has("text")) json.get("text").asString else json.get("description").asString)
            }
        } catch (e: Exception) {
            "获取详情出错: ${e.message}"
        }
    }

    private fun fetchPosts(url: String, listKey: String): List<XueqiuPost> {
        if (cookie.isEmpty()) return emptyList()
        
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Cookie", cookie)
            .header("Referer", "https://xueqiu.com/")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                val json = gson.fromJson(body, JsonObject::class.java)
                val list = json.getAsJsonArray(listKey)
                
                list.map { element ->
                    val item = element.asJsonObject
                    val dataStr = if (item.has("data")) item.get("data").asString else null
                    
                    if (dataStr != null) {
                        val dataJson = gson.fromJson(dataStr, JsonObject::class.java)
                        XueqiuPost(
                            id = dataJson.get("id").asLong,
                            author = if (dataJson.has("user")) dataJson.getAsJsonObject("user").get("screen_name").asString else "雪球快讯",
                            content = cleanHtml(if (dataJson.has("description")) dataJson.get("description").asString else dataJson.get("text").asString),
                            time = dataJson.get("created_at").asLong
                        )
                    } else {
                        XueqiuPost(
                            id = item.get("id").asLong,
                            author = item.getAsJsonObject("user").get("screen_name").asString,
                            content = cleanHtml(item.get("description").asString),
                            time = item.get("created_at").asLong
                        )
                    }
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun cleanHtml(html: String): String {
        return html.replace(Regex("<[^>]*>"), "").replace("&nbsp;", " ").trim()
    }
}

data class XueqiuPost(
    val id: Long,
    val author: String,
    val content: String,
    val time: Long
)
