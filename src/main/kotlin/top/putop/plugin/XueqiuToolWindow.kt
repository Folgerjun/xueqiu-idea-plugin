package top.putop.plugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Insets
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.*

class XueqiuToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val xueqiuToolWindow = XueqiuToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(xueqiuToolWindow.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class XueqiuToolWindow(private val toolWindow: ToolWindow) {
    private val client = XueqiuClient()
    private val listModel = DefaultListModel<XueqiuPost>()
    private val postList = JBList(listModel)
    private var currentCategory = "hot"
    private var lastId: Long = -1

    init {
        postList.cellRenderer = PostCellRenderer()
        // 更新：双击调用详情接口
        postList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val selectedPost = postList.selectedValue
                    if (selectedPost != null) {
                        loadAndShowDetail(selectedPost)
                    }
                }
            }
        })
    }

    private fun loadAndShowDetail(post: XueqiuPost) {
        // 在后台线程获取详情，避免阻塞 UI
        ApplicationManager.getApplication().executeOnPooledThread {
            val fullContent = client.getPostDetail(post.id)
            // 在 UI 线程弹出对话框
            SwingUtilities.invokeLater {
                PostDetailDialog(post.author, fullContent).show()
            }
        }
    }

    fun getContent(): JPanel {
        val panel = JPanel(BorderLayout())
        
        // Toolbar
        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT))
        val hotBtn = JButton("热门")
        val followBtn = JButton("关注")
        val newsBtn = JButton("7*24")
        val loginBtn = JButton("设置Cookie")

        hotBtn.addActionListener { switchCategory("hot") }
        followBtn.addActionListener { switchCategory("following") }
        newsBtn.addActionListener { switchCategory("news") }
        loginBtn.addActionListener { showCookieDialog() }

        toolbar.add(hotBtn)
        toolbar.add(followBtn)
        toolbar.add(newsBtn)
        toolbar.add(loginBtn)

        // Pagination
        val pagination = JPanel(FlowLayout(FlowLayout.CENTER))
        val nextBtn = JButton("下一页")
        nextBtn.addActionListener { loadMore() }
        pagination.add(nextBtn)

        panel.add(toolbar, BorderLayout.NORTH)
        panel.add(JBScrollPane(postList), BorderLayout.CENTER)
        panel.add(pagination, BorderLayout.SOUTH)

        return panel
    }

    private fun switchCategory(category: String) {
        currentCategory = category
        lastId = -1
        listModel.clear()
        refresh()
    }

    private fun refresh() {
        if (client.cookie.isEmpty()) {
            JOptionPane.showMessageDialog(null, "请先设置 Cookie")
            return
        }
        ApplicationManager.getApplication().executeOnPooledThread {
            val posts = try {
                when (currentCategory) {
                    "hot" -> client.getHot(lastId)
                    "following" -> client.getFollowing(lastId)
                    "news" -> client.getNews(lastId)
                    else -> emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
            
            SwingUtilities.invokeLater {
                if (posts.isNotEmpty()) {
                    posts.forEach { listModel.addElement(it) }
                    lastId = if (currentCategory == "following") posts.last().id - 1 else posts.last().id
                }
            }
        }
    }

    private fun loadMore() {
        refresh()
    }

    private fun showCookieDialog() {
        val cookie = JOptionPane.showInputDialog("请输入雪球 Cookie:")
        if (cookie != null) {
            client.cookie = cookie
            refresh()
        }
    }
}

class PostDetailDialog(private val author: String, private val content: String) : DialogWrapper(true) {
    init {
        title = "内容详情 - $author"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.preferredSize = Dimension(600, 500)
        
        val textArea = JTextArea(content)
        textArea.lineWrap = true
        textArea.wrapStyleWord = true
        textArea.isEditable = false
        textArea.margin = Insets(10, 10, 10, 10)
        
        val scrollPane = JBScrollPane(textArea)
        panel.add(scrollPane, BorderLayout.CENTER)
        
        return panel
    }
}

class PostCellRenderer : DefaultListCellRenderer() {
    private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    override fun getListCellRendererComponent(
        list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
    ): Component {
        val post = value as XueqiuPost
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createEmptyBorder(8, 10, 8, 10)

        // 格式化时间
        val formattedTime = try {
            Instant.ofEpochMilli(post.time)
                .atZone(ZoneId.systemDefault())
                .format(timeFormatter)
        } catch (_: Exception) {
            post.time.toString() // 如果转换失败或者类型不对，回退到原始值
        }

        // 优化 Header 显示
        val authorColor = if (isSelected) "white" else "black" // 选中时适配字体颜色
        val timeColor = if (isSelected) "#E0E0E0" else "gray"

        val headerHtml = """
            <html>
            <body>
                <span style='color: $authorColor; font-size: 1.1em;'><b>${post.author}</b></span>
                &nbsp;&nbsp;
                <span style='color: $timeColor; font-size: 0.9em;'>$formattedTime</span>
            </body>
            </html>
        """.trimIndent()

        val header = JLabel(headerHtml)
        
        // 列表页展示截断内容
        val displayContent = if (post.content.length > 120) {
            post.content.take(120) + "..."
        } else {
            post.content
        }

        panel.toolTipText = post.content
        
        val content = JTextArea(displayContent)
        content.lineWrap = true
        content.wrapStyleWord = true
        content.isEditable = false
        content.font = UIManager.getFont("Label.font")

        // 处理选中状态的背景色和前景色
        if (isSelected) {
            panel.background = UIManager.getColor("List.selectionBackground")
            content.background = UIManager.getColor("List.selectionBackground")
            content.foreground = UIManager.getColor("List.selectionForeground")
        } else {
            panel.background = UIManager.getColor("List.background")
            content.background = UIManager.getColor("List.background")
            content.foreground = UIManager.getColor("List.foreground")
        }
        
        panel.add(header, BorderLayout.NORTH)
        panel.add(content, BorderLayout.CENTER)
        
        return panel
    }
}
