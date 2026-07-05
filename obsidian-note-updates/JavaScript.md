# JavaScript

JavaScript负责网页的行为和交互，例如按钮点击、表单验证、页面切换、购物车计算、弹窗、轮播图等。

## 基本语法

### 引入JavaScript

写在 HTML 文件中：

```html
<script>
    console.log("页面加载了");
</script>
```

引入外部文件：

```html
<script src="./js/app.js"></script>
```

通常把 `script` 写在 `body` 结束标签前面，这样可以先加载页面结构，再执行脚本。

### 输出内容

```js
console.log("控制台输出");
alert("弹窗提示");
document.write("写入页面");
```

开发时最常用的是 `console.log()`，可以在浏览器控制台查看结果。

### 变量

```js
let username = "admin";
const price = 99;
var oldName = "不推荐在新代码中使用";
```

区别：

- `let`：声明可以重新赋值的变量。
- `const`：声明常量，不能重新赋值。
- `var`：旧写法，作用域规则容易出问题，新代码少用。

### 数据类型

```js
let name = "星际战甲";       // 字符串
let price = 0;              // 数字
let isFree = true;          // 布尔值
let cover = null;           // 空值
let discount;               // undefined
let game = { title: "黑神话：悟空", type: "动作冒险" }; // 对象
let games = ["原神", "艾尔登法环", "赛博朋克2077"];     // 数组
```

常见类型：

- `string`：字符串。
- `number`：数字。
- `boolean`：布尔值，只有 `true` 和 `false`。
- `null`：主动设置为空。
- `undefined`：声明了但没有赋值。
- `object`：对象。
- `array`：数组，本质上也是对象。

### 运算符

```js
let total = 99 + 20;
let salePrice = 199 * 0.5;
let isEnough = balance >= salePrice;
let canBuy = isEnough && salePrice > 0;
let isFree = salePrice === 0;
```

常用运算符：

- 算术运算：`+`、`-`、`*`、`/`、`%`。
- 比较运算：`>`、`<`、`>=`、`<=`、`===`、`!==`。
- 逻辑运算：`&&`、`||`、`!`。
- 赋值运算：`=`、`+=`、`-=`。

建议比较时使用 `===`，不要随意使用 `==`。

### 条件语句

```js
const balance = 120;
const price = 99;

if (balance >= price) {
    console.log("购买成功");
} else {
    console.log("余额不足");
}
```

多条件判断：

```js
const score = 86;

if (score >= 90) {
    console.log("优秀");
} else if (score >= 60) {
    console.log("及格");
} else {
    console.log("不及格");
}
```

### switch语句

```js
const category = "rpg";

switch (category) {
    case "rpg":
        console.log("角色扮演");
        break;
    case "action":
        console.log("动作冒险");
        break;
    default:
        console.log("其他分类");
}
```

`switch` 适合固定值匹配，例如分类、状态、菜单类型。

### 循环

```js
for (let i = 0; i < 5; i++) {
    console.log(i);
}
```

遍历数组：

```js
const games = ["原神", "黑神话：悟空", "星露谷物语"];

games.forEach(function (game) {
    console.log(game);
});
```

使用箭头函数：

```js
games.forEach((game) => {
    console.log(game);
});
```

### 函数

```js
function getSalePrice(price, discount) {
    return price * discount;
}

const result = getSalePrice(200, 0.5);
console.log(result);
```

箭头函数：

```js
const getSalePrice = (price, discount) => {
    return price * discount;
};
```

函数可以把重复逻辑封装起来，例如计算折扣价、渲染商品卡片、校验表单。

### 数组常用方法

```js
const games = [
    { title: "原神", price: 0, category: "开放世界" },
    { title: "艾尔登法环", price: 298, category: "角色扮演" },
    { title: "星露谷物语", price: 48, category: "模拟经营" }
];

const freeGames = games.filter((game) => game.price === 0);
const names = games.map((game) => game.title);
const target = games.find((game) => game.title === "原神");
const total = games.reduce((sum, game) => sum + game.price, 0);
```

常用方法：

- `forEach`：遍历数组。
- `map`：生成新数组。
- `filter`：筛选数组。
- `find`：查找第一个符合条件的元素。
- `reduce`：累计计算。
- `push`：向数组末尾添加元素。
- `splice`：删除或替换元素。

### 对象

```js
const user = {
    username: "admin",
    balance: 9999,
    login: true
};

console.log(user.username);
user.balance = user.balance - 99;
```

对象适合保存一组相关数据，例如用户信息、商品信息、订单信息。

## DOM操作

DOM是浏览器把 HTML 页面解析后形成的对象结构，JavaScript 可以通过 DOM 操作页面内容。

### 获取元素

```js
const title = document.querySelector(".title");
const cards = document.querySelectorAll(".game-card");
const button = document.getElementById("buyBtn");
```

常用方法：

- `querySelector()`：获取第一个匹配元素。
- `querySelectorAll()`：获取所有匹配元素。
- `getElementById()`：通过 id 获取元素。

### 修改内容

```js
const title = document.querySelector(".title");

title.textContent = "游戏商城";
title.innerHTML = "<span>热门游戏</span>";
```

区别：

- `textContent`：只设置文本，更安全。
- `innerHTML`：可以解析 HTML 字符串，但要注意不要插入不可信内容。

### 修改样式

```js
const card = document.querySelector(".game-card");

card.style.backgroundColor = "#1f2a44";
card.style.transform = "translateY(-4px)";
```

更推荐通过切换类名控制样式：

```js
card.classList.add("active");
card.classList.remove("disabled");
card.classList.toggle("selected");
card.classList.contains("active");
```

### 修改属性

```js
const cover = document.querySelector(".cover");

cover.src = "./images/game.jpg";
cover.alt = "游戏封面";
cover.setAttribute("data-id", "1001");
cover.getAttribute("data-id");
```

### 创建和插入元素

```js
const list = document.querySelector(".game-list");
const item = document.createElement("li");

item.textContent = "新游戏";
list.appendChild(item);
```

使用模板字符串渲染列表：

```js
const games = [
    { title: "原神", price: "免费" },
    { title: "艾尔登法环", price: "￥298.00" }
];

const container = document.querySelector(".game-grid");

container.innerHTML = games.map((game) => {
    return `
        <article class="game-card">
            <h3>${game.title}</h3>
            <p>${game.price}</p>
        </article>
    `;
}).join("");
```

### 删除元素

```js
const item = document.querySelector(".cart-item");
item.remove();
```

## 事件处理

事件就是用户或浏览器发生的动作，例如点击、输入、提交、滚动、键盘按下。

### 点击事件

```js
const button = document.querySelector("#buyBtn");

button.addEventListener("click", () => {
    alert("购买成功");
});
```

### 输入事件

```js
const searchInput = document.querySelector("#searchInput");

searchInput.addEventListener("input", () => {
    console.log(searchInput.value);
});
```

`input` 事件适合搜索框实时筛选。

### 表单提交事件

```js
const form = document.querySelector("#loginForm");

form.addEventListener("submit", (event) => {
    event.preventDefault();

    const username = document.querySelector("#username").value;
    const password = document.querySelector("#password").value;

    if (username === "" || password === "") {
        alert("请输入账号和密码");
        return;
    }

    alert("登录成功");
});
```

`event.preventDefault()` 可以阻止表单默认提交，方便前端自己处理逻辑。

### 鼠标事件

```js
const card = document.querySelector(".game-card");

card.addEventListener("mouseenter", () => {
    card.classList.add("hover");
});

card.addEventListener("mouseleave", () => {
    card.classList.remove("hover");
});
```

### 键盘事件

```js
document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
        console.log("按下了ESC");
    }
});
```

### 事件对象

```js
document.addEventListener("click", (event) => {
    console.log(event.target);
});
```

`event.target` 表示真正被点击的元素。

### 事件委托

如果列表中有很多按钮，可以把事件绑定给父元素。

```html
<div class="game-grid">
    <button data-id="1">购买</button>
    <button data-id="2">购买</button>
</div>
```

```js
const grid = document.querySelector(".game-grid");

grid.addEventListener("click", (event) => {
    if (event.target.tagName !== "BUTTON") {
        return;
    }

    const id = event.target.dataset.id;
    console.log("购买游戏ID：", id);
});
```

事件委托适合动态渲染出来的列表按钮。

## 常见交互效果的详细结构

### 1. 搜索筛选

HTML结构：

```html
<input id="searchInput" type="search" placeholder="搜索游戏">
<div id="gameGrid"></div>
```

JavaScript逻辑：

```js
const games = [
    { title: "原神", category: "开放世界" },
    { title: "艾尔登法环", category: "角色扮演" },
    { title: "星露谷物语", category: "模拟经营" }
];

const searchInput = document.querySelector("#searchInput");
const gameGrid = document.querySelector("#gameGrid");

function renderGames(list) {
    gameGrid.innerHTML = list.map((game) => {
        return `
            <article class="game-card">
                <h3>${game.title}</h3>
                <p>${game.category}</p>
            </article>
        `;
    }).join("");
}

searchInput.addEventListener("input", () => {
    const keyword = searchInput.value.trim();
    const result = games.filter((game) => {
        return game.title.includes(keyword) || game.category.includes(keyword);
    });

    renderGames(result);
});

renderGames(games);
```

实现思路：

- 准备原始数据数组。
- 输入框监听 `input` 事件。
- 用 `filter` 筛选符合关键词的数据。
- 重新渲染列表。

### 2. 分类切换

HTML结构：

```html
<div class="tabs">
    <button class="active" data-category="全部">全部</button>
    <button data-category="角色扮演">角色扮演</button>
    <button data-category="动作冒险">动作冒险</button>
</div>
<div id="gameGrid"></div>
```

JavaScript逻辑：

```js
const tabs = document.querySelector(".tabs");

tabs.addEventListener("click", (event) => {
    if (event.target.tagName !== "BUTTON") {
        return;
    }

    document.querySelectorAll(".tabs button").forEach((button) => {
        button.classList.remove("active");
    });

    event.target.classList.add("active");

    const category = event.target.dataset.category;
    const result = category === "全部"
        ? games
        : games.filter((game) => game.category === category);

    renderGames(result);
});
```

实现思路：

- 给分类按钮设置 `data-category`。
- 点击按钮后切换 `active` 样式。
- 根据分类筛选数据。
- 重新渲染商品卡片。

### 3. 弹窗

HTML结构：

```html
<button id="openModal">打开弹窗</button>

<div class="modal hidden" id="modal">
    <div class="modal-content">
        <h3>购买确认</h3>
        <p>确认购买该游戏吗？</p>
        <button id="confirmBtn">确认</button>
        <button id="closeModal">取消</button>
    </div>
</div>
```

CSS关键样式：

```css
.modal {
    position: fixed;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.6);
}

.hidden {
    display: none;
}
```

JavaScript逻辑：

```js
const modal = document.querySelector("#modal");
const openModal = document.querySelector("#openModal");
const closeModal = document.querySelector("#closeModal");

openModal.addEventListener("click", () => {
    modal.classList.remove("hidden");
});

closeModal.addEventListener("click", () => {
    modal.classList.add("hidden");
});

modal.addEventListener("click", (event) => {
    if (event.target === modal) {
        modal.classList.add("hidden");
    }
});
```

实现思路：

- 弹窗默认加 `hidden` 隐藏。
- 点击按钮移除 `hidden`。
- 点击取消或遮罩层时重新加上 `hidden`。

### 4. 购物车数量和总价

HTML结构：

```html
<button id="addCart">加入购物车</button>
<span id="cartCount">0</span>
<strong id="cartTotal">￥0.00</strong>
```

JavaScript逻辑：

```js
let cart = [];

const addCart = document.querySelector("#addCart");
const cartCount = document.querySelector("#cartCount");
const cartTotal = document.querySelector("#cartTotal");

addCart.addEventListener("click", () => {
    cart.push({
        title: "艾尔登法环",
        price: 298
    });

    updateCart();
});

function updateCart() {
    cartCount.textContent = cart.length;

    const total = cart.reduce((sum, item) => {
        return sum + item.price;
    }, 0);

    cartTotal.textContent = `￥${total.toFixed(2)}`;
}
```

实现思路：

- 用数组保存购物车商品。
- 点击按钮时向数组添加商品。
- 使用 `reduce` 计算总价。
- 更新页面上的数量和金额。

### 5. 轮播图

HTML结构：

```html
<div class="slider">
    <img id="banner" src="./images/banner1.jpg" alt="轮播图">
    <button id="prevBtn">上一张</button>
    <button id="nextBtn">下一张</button>
</div>
```

JavaScript逻辑：

```js
const banners = [
    "./images/banner1.jpg",
    "./images/banner2.jpg",
    "./images/banner3.jpg"
];

let currentIndex = 0;

const banner = document.querySelector("#banner");
const prevBtn = document.querySelector("#prevBtn");
const nextBtn = document.querySelector("#nextBtn");

function showBanner(index) {
    banner.src = banners[index];
}

nextBtn.addEventListener("click", () => {
    currentIndex++;

    if (currentIndex >= banners.length) {
        currentIndex = 0;
    }

    showBanner(currentIndex);
});

prevBtn.addEventListener("click", () => {
    currentIndex--;

    if (currentIndex < 0) {
        currentIndex = banners.length - 1;
    }

    showBanner(currentIndex);
});

setInterval(() => {
    nextBtn.click();
}, 3000);
```

实现思路：

- 用数组保存图片地址。
- 用变量记录当前图片下标。
- 点击上一张或下一张时修改下标。
- 使用 `setInterval` 实现自动播放。

### 6. 本地存储

本地存储可以把数据保存在浏览器中，刷新页面后仍然存在。

```js
const user = {
    username: "admin",
    balance: 9999
};

localStorage.setItem("user", JSON.stringify(user));

const savedUser = JSON.parse(localStorage.getItem("user"));
console.log(savedUser.username);
```

常用方法：

- `localStorage.setItem(key, value)`：保存数据。
- `localStorage.getItem(key)`：读取数据。
- `localStorage.removeItem(key)`：删除数据。
- `localStorage.clear()`：清空所有数据。

注意：`localStorage` 只能保存字符串，所以对象和数组需要用 `JSON.stringify()` 转成字符串，读取时再用 `JSON.parse()` 转回来。
