
---

## 补充：HTML常用标签速查

HTML负责网页的结构，写页面时可以先思考“页面由哪些区域组成”，再选择合适的标签。

### 文本类标签

```html
<h1>页面主标题</h1>
<p>普通段落文本</p>
<strong>重要内容，默认加粗</strong>
<em>强调内容，默认倾斜</em>
<span>一小段行内文字</span>
<br>
<hr>
```

常用说明：

- `h1` 到 `h6` 表示标题层级，数字越大级别越低。
- `p` 表示一段完整文字。
- `strong` 表示重要内容，不只是视觉加粗。
- `em` 表示强调语气，不只是视觉倾斜。
- `span` 没有特殊语义，常用于给部分文字单独设置样式。
- `br` 是换行，`hr` 是分割线。

### 布局和语义化标签

```html
<header>头部区域</header>
<nav>导航菜单</nav>
<main>页面主要内容</main>
<section>页面中的一个内容区域</section>
<article>文章、商品详情、帖子等独立内容</article>
<aside>侧边栏、推荐内容、广告位</aside>
<footer>底部区域</footer>
<div>普通块级容器</div>
```

写法建议：

- 页面只有一个主要内容区时，用 `main` 包住核心内容。
- 一个完整模块可以用 `section`，例如“热门游戏”“新品上架”。
- 一条能独立阅读的内容用 `article`，例如新闻、博客、商品详情。
- 没有合适语义标签时再使用 `div`。

### 链接和资源标签

```html
<a href="./detail.html">查看详情</a>
<a href="https://example.com" target="_blank">新窗口打开</a>

<img src="./images/game.jpg" alt="游戏封面">

<video src="./video/demo.mp4" controls poster="./images/poster.jpg"></video>
<audio src="./audio/music.mp3" controls></audio>
```

注意：

- `a` 标签的 `href` 是跳转地址。
- `target="_blank"` 表示新窗口或新标签页打开。
- `img` 必须写 `alt`，图片加载失败或无障碍阅读时会用到。
- 视频自动播放通常需要加 `muted`，否则很多浏览器会拦截。

### 列表标签

```html
<ul>
    <li>无序列表项</li>
    <li>适合菜单、商品特点</li>
</ul>

<ol>
    <li>第一步</li>
    <li>第二步</li>
</ol>

<dl>
    <dt>价格</dt>
    <dd>￥99.00</dd>
    <dt>类型</dt>
    <dd>角色扮演</dd>
</dl>
```

- `ul` 表示没有顺序的列表。
- `ol` 表示有顺序的列表。
- `dl` 适合“名词 + 解释”的信息，比如商品参数。

### 表格标签

```html
<table>
    <caption>订单列表</caption>
    <thead>
        <tr>
            <th>商品</th>
            <th>价格</th>
            <th>状态</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>星际战甲</td>
            <td>免费</td>
            <td>已入库</td>
        </tr>
    </tbody>
</table>
```

表格适合展示二维数据，例如成绩表、订单列表、商品参数对比。

## 补充：表单及表单控件

表单用于收集用户输入，常见场景有登录、注册、搜索、评论、下单和支付。

### 表单基本结构

```html
<form action="/login" method="post">
    <label for="account">账号</label>
    <input id="account" name="account" type="text" required>

    <label for="password">密码</label>
    <input id="password" name="password" type="password" required>

    <button type="submit">登录</button>
</form>
```

关键点：

- `action` 表示提交到哪里。
- `method="get"` 常用于搜索，参数会显示在地址栏。
- `method="post"` 常用于登录、注册、支付等数据提交。
- 每个需要提交的控件都应该有 `name`。
- `label` 的 `for` 要对应控件的 `id`，这样点击文字也能聚焦控件。

### input常用类型

```html
<input type="text" placeholder="普通文本">
<input type="password" placeholder="密码">
<input type="number" min="1" max="99" step="1">
<input type="email" placeholder="邮箱">
<input type="tel" placeholder="手机号">
<input type="url" placeholder="网址">
<input type="search" placeholder="搜索关键词">
<input type="date">
<input type="time">
<input type="color">
<input type="range" min="0" max="100">
<input type="file">
```

常用属性：

- `placeholder`：输入提示。
- `value`：默认值。
- `required`：必填。
- `disabled`：禁用，不能输入，也不会提交。
- `readonly`：只读，不能修改，但可以提交。
- `maxlength`：最大输入长度。
- `min`、`max`、`step`：常用于数字和滑块。
- `autocomplete`：是否启用浏览器自动填充。

### 单选框和复选框

```html
<p>支付方式：</p>
<label>
    <input type="radio" name="pay" value="wechat" checked>
    微信支付
</label>
<label>
    <input type="radio" name="pay" value="alipay">
    支付宝
</label>

<p>游戏标签：</p>
<label>
    <input type="checkbox" name="tag" value="rpg">
    角色扮演
</label>
<label>
    <input type="checkbox" name="tag" value="sale">
    折扣商品
</label>
```

注意：

- 同一组单选框 `name` 必须相同。
- 复选框可以选择多个。
- `checked` 表示默认选中。

### 下拉框、文本域和按钮

```html
<label for="category">游戏分类</label>
<select id="category" name="category">
    <option value="">请选择分类</option>
    <option value="rpg">角色扮演</option>
    <option value="action">动作冒险</option>
    <option value="free">免费游戏</option>
</select>

<label for="comment">评价内容</label>
<textarea id="comment" name="comment" rows="5" placeholder="请输入评价"></textarea>

<button type="submit">提交</button>
<button type="reset">重置</button>
<button type="button">普通按钮</button>
```

按钮类型：

- `submit`：提交表单。
- `reset`：重置表单。
- `button`：普通按钮，通常配合 JavaScript 使用。

### 表单验证示例

```html
<form>
    <label for="username">用户名</label>
    <input
        id="username"
        name="username"
        type="text"
        minlength="3"
        maxlength="12"
        required
        placeholder="3到12位用户名"
    >

    <label for="email">邮箱</label>
    <input id="email" name="email" type="email" required>

    <button type="submit">注册</button>
</form>
```

常见验证属性：

- `required`：不能为空。
- `minlength`、`maxlength`：限制文本长度。
- `min`、`max`：限制数字范围。
- `pattern`：使用正则表达式验证输入格式。

### 登录注册表单结构示例

```html
<form class="auth-form">
    <h2>注册账号</h2>

    <label for="reg-name">用户名</label>
    <input id="reg-name" name="username" type="text" required>

    <label for="reg-email">邮箱</label>
    <input id="reg-email" name="email" type="email" required>

    <label for="reg-password">密码</label>
    <input id="reg-password" name="password" type="password" required minlength="6">

    <label>
        <input type="checkbox" name="agree" required>
        我已阅读并同意用户协议
    </label>

    <button type="submit">立即注册</button>
</form>
```

### 表单编写建议

- 一个输入项尽量配一个清晰的 `label`。
- 登录、注册、支付等敏感表单使用 `post`。
- 必填项用 `required` 提醒用户。
- 表单布局可以使用 CSS 的 `flex` 或 `grid` 完成。
- 复杂交互和提示信息交给 JavaScript 处理。
