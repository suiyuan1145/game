
---

## 补充：选择器

CSS选择器用来找到页面中的元素，然后给这些元素设置样式。

### 基础选择器

```css
/* 标签选择器：选择所有 p 标签 */
p {
    color: #333;
}

/* 类选择器：选择 class="card" 的元素 */
.card {
    background-color: #1f2a44;
}

/* id选择器：选择 id="app" 的元素 */
#app {
    width: 1200px;
}

/* 通配符选择器：选择所有元素 */
* {
    box-sizing: border-box;
}
```

说明：

- 标签选择器影响范围较大。
- 类选择器最常用，适合复用样式。
- id选择器优先级高，但不适合大量复用。
- 通配符常用于初始化样式，例如统一盒模型。

### 关系选择器

```css
/* 后代选择器：选择 .nav 里面所有 a */
.nav a {
    color: white;
}

/* 子代选择器：只选择 .menu 的直接子元素 li */
.menu > li {
    padding: 12px;
}

/* 相邻兄弟选择器：选择紧跟在 h2 后面的 p */
h2 + p {
    margin-top: 8px;
}

/* 通用兄弟选择器：选择 h2 后面所有同级 p */
h2 ~ p {
    color: #8ea0c7;
}
```

### 分组选择器

```css
h1,
h2,
h3 {
    font-weight: 700;
    color: #ffffff;
}
```

分组选择器可以减少重复代码。

### 属性选择器

```css
input[type="text"] {
    border: 1px solid #5b6f9e;
}

a[target="_blank"] {
    color: #6ea8ff;
}
```

属性选择器适合根据标签属性设置样式，例如不同类型的输入框。

### 伪类选择器

```css
a:hover {
    color: #7dd3fc;
}

button:active {
    transform: scale(0.98);
}

input:focus {
    outline: 2px solid #4f7cff;
}

li:first-child {
    font-weight: bold;
}

li:nth-child(2n) {
    background-color: #f5f7fb;
}
```

常见伪类：

- `:hover`：鼠标悬停。
- `:active`：鼠标按下。
- `:focus`：输入框获得焦点。
- `:first-child`：第一个子元素。
- `:last-child`：最后一个子元素。
- `:nth-child()`：按顺序选择元素。

### 伪元素选择器

```css
.title::before {
    content: "";
    display: inline-block;
    width: 4px;
    height: 18px;
    background-color: #4f7cff;
    margin-right: 8px;
}

.card::after {
    content: "NEW";
    color: #ff4d6d;
}
```

伪元素常用于添加装饰性内容，不需要额外写 HTML 标签。

### 选择器优先级

优先级大致规则：

```text
!important > 行内样式 > id选择器 > 类/属性/伪类选择器 > 标签/伪元素选择器 > 通配符
```

建议：

- 少用 `!important`。
- 多用类选择器组织样式。
- 不要为了提高优先级写过长选择器。

## 补充：盒模型

页面中的每个元素都可以看成一个盒子，盒子由内容、内边距、边框、外边距组成。

```text
margin 外边距
border 边框
padding 内边距
content 内容区域
```

### 标准盒模型

```css
.box {
    width: 200px;
    padding: 20px;
    border: 2px solid #333;
}
```

默认情况下，元素实际占用宽度为：

```text
实际宽度 = width + 左右padding + 左右border
```

### IE盒模型 / border-box

```css
* {
    box-sizing: border-box;
}

.box {
    width: 200px;
    padding: 20px;
    border: 2px solid #333;
}
```

使用 `border-box` 后，元素实际宽度就是 `width`，布局更容易控制。

### margin外边距

```css
.card {
    margin: 20px;
}

.card {
    margin: 10px 20px 30px 40px;
}
```

四值写法顺序：

```text
上 右 下 左
```

常见写法：

```css
.container {
    width: 1200px;
    margin: 0 auto;
}
```

`margin: 0 auto` 常用于块级元素水平居中。

### padding内边距

```css
.button {
    padding: 10px 18px;
}
```

内边距是内容和边框之间的距离，常用于按钮、卡片、输入框。

### border边框

```css
.input {
    border: 1px solid #46577c;
    border-radius: 8px;
}
```

边框常用属性：

- `border-width`：边框宽度。
- `border-style`：边框样式，例如 `solid`、`dashed`。
- `border-color`：边框颜色。
- `border-radius`：圆角。

### 块级元素和行内元素

```css
div {
    display: block;
}

span {
    display: inline;
}

button {
    display: inline-block;
}
```

区别：

- `block`：独占一行，可以设置宽高。
- `inline`：不独占一行，设置宽高通常无效。
- `inline-block`：不独占一行，同时可以设置宽高。

## 补充：常用样式

### 文本样式

```css
.title {
    color: #ffffff;
    font-size: 28px;
    font-weight: 700;
    line-height: 1.4;
    text-align: center;
}

.desc {
    color: #9aa7c7;
    text-decoration: none;
}
```

常用属性：

- `color`：文字颜色。
- `font-size`：字号。
- `font-weight`：文字粗细。
- `line-height`：行高。
- `text-align`：文字对齐。
- `text-decoration`：文字装饰线。

### 背景样式

```css
.hero {
    background-color: #10182d;
    background-image: url("../images/banner.jpg");
    background-size: cover;
    background-position: center;
    background-repeat: no-repeat;
}
```

常用属性：

- `background-color`：背景颜色。
- `background-image`：背景图片。
- `background-size: cover`：铺满容器，可能裁剪。
- `background-size: contain`：完整显示图片，可能留空。
- `background-position`：背景位置。

### 尺寸和溢出

```css
.cover {
    width: 100%;
    height: 180px;
    object-fit: cover;
}

.text {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}
```

常见用途：

- `object-fit: cover`：让图片保持比例并填满区域。
- `overflow: hidden`：隐藏超出内容。
- `text-overflow: ellipsis`：文字超出显示省略号。

### Flex布局

```css
.nav {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
}
```

常用属性：

- `display: flex`：开启弹性布局。
- `flex-direction`：主轴方向。
- `justify-content`：主轴对齐。
- `align-items`：交叉轴对齐。
- `gap`：子元素间距。
- `flex-wrap`：是否换行。

### Grid布局

```css
.game-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 22px;
}
```

适合商品列表、图片墙、仪表盘卡片等二维布局。

### 定位

```css
.card {
    position: relative;
}

.badge {
    position: absolute;
    top: 12px;
    right: 12px;
}

.topbar {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
}
```

常用定位：

- `relative`：相对自身位置，常作为绝对定位的参照物。
- `absolute`：相对最近的定位祖先元素。
- `fixed`：相对浏览器窗口固定。
- `sticky`：滚动到指定位置后吸顶。

### 过渡和动画

```css
.card {
    transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.card:hover {
    transform: translateY(-6px);
    box-shadow: 0 16px 40px rgba(0, 0, 0, 0.35);
}

@keyframes loading {
    from {
        transform: rotate(0deg);
    }
    to {
        transform: rotate(360deg);
    }
}

.spinner {
    animation: loading 1s linear infinite;
}
```

### 响应式布局

```css
@media (max-width: 768px) {
    .game-grid {
        grid-template-columns: repeat(2, 1fr);
    }
}

@media (max-width: 480px) {
    .game-grid {
        grid-template-columns: 1fr;
    }
}
```

媒体查询可以根据屏幕宽度调整布局，常用于手机端适配。

### 常用初始化样式

```css
* {
    box-sizing: border-box;
}

body {
    margin: 0;
    font-family: Arial, "Microsoft YaHei", sans-serif;
    background-color: #10182d;
    color: #ffffff;
}

a {
    color: inherit;
    text-decoration: none;
}

img {
    display: block;
    max-width: 100%;
}

button,
input,
textarea,
select {
    font: inherit;
}
```

这些初始化样式可以减少浏览器默认样式带来的差异。
