/*
  app.js
  作用：本文件负责整个纯前端网站的交互逻辑。
  覆盖内容：
  1. 游戏商品数据
  2. localStorage 本地状态保存
  3. 首页商品列表渲染
  4. 详情页、个人中心、加载页渲染
  5. 登录、充值、购物车、购买、订单等交互
*/

// 游戏商品数据：纯前端项目没有数据库，因此用数组模拟商品表。
// cover 字段对应 assets/covers 目录下的本地图片。
const games = [
  { id: 1, title: '艾尔登法环', category: '角色扮演', studio: 'FromSoftware', price: 398, discount: 25, rating: 9.7, cover: 'assets/covers/elden.jpg', desc: '开放世界魂系角色扮演游戏，探索交界地、挑战半神，并收集强大的装备与法术。' },
  { id: 2, title: '赛博朋克2077', category: '开放世界', studio: 'CD Projekt', price: 299, discount: 50, rating: 9.1, cover: 'assets/covers/cyberpunk.jpg', desc: '霓虹都市、义体改造和高自由度任务线，体验夜之城的未来冒险。' },
  { id: 3, title: 'Counter-Strike 2', category: '射击竞技', studio: 'Valve', price: 0, discount: 0, rating: 9.1, cover: 'assets/covers/cs2.jpg', desc: '免费多人竞技射击游戏，经典爆破模式、精准枪法和团队配合是胜负关键。' },
  { id: 4, title: '黑神话：悟空', category: '动作冒险', studio: 'Game Science', price: 268, discount: 0, rating: 9.6, cover: 'assets/covers/wukong.jpg', desc: '东方神话题材动作冒险游戏，化身天命人，面对妖王与古老传说。' },
  { id: 5, title: '星露谷物语', category: '模拟经营', studio: 'ConcernedApe', price: 68, discount: 29, rating: 9.4, cover: 'assets/covers/stardew.jpg', desc: '经营农场、采矿、钓鱼、社交，打造属于自己的乡村生活。' },
  { id: 6, title: '只狼：影逝二度', category: '动作冒险', studio: 'FromSoftware', price: 268, discount: 0, rating: 9.5, cover: 'assets/covers/sekiro.jpg', desc: '高强度剑戟动作游戏，凭借格挡、忍具与身法击败强敌。' },
  { id: 7, title: '巫师3：狂猎', category: '角色扮演', studio: 'CD Projekt', price: 199, discount: 50, rating: 9.6, cover: 'assets/covers/witcher.jpg', desc: '扮演猎魔人杰洛特，调查怪物、政治阴谋与命运之子的踪迹。' },
  { id: 8, title: '最终幻想7 重制版', category: '角色扮演', studio: 'Square Enix', price: 446, discount: 0, rating: 9.0, cover: 'assets/covers/ff7.jpg', desc: '经典 RPG 的现代化重制，在米德加展开电影级冒险与即时战斗。' },
  { id: 9, title: '博德之门3', category: '角色扮演', studio: 'Larian Studios', price: 298, discount: 10, rating: 9.8, cover: 'assets/covers/baldur.jpg', desc: '基于队伍和骰子的史诗 RPG，自由选择会改变每段旅程。' },
  { id: 10, title: '幻兽帕鲁', category: '开放世界', studio: 'Pocketpair', price: 108, discount: 20, rating: 9.0, cover: 'assets/covers/palworld.jpg', desc: '捕获帕鲁、建造基地、探索岛屿，并与好友一起冒险。' },
  { id: 11, title: '空洞骑士', category: '动作冒险', studio: 'Team Cherry', price: 48, discount: 30, rating: 9.3, cover: 'assets/covers/hollow.jpg', desc: '手绘地下王国探索游戏，挑战首领、发现隐藏路线与古老秘密。' },
  { id: 12, title: '星际拓荒者', category: '可玩小游戏', studio: 'Nebula Lab', price: 128, discount: 35, rating: 9.2, cover: 'assets/covers/star.jpg', desc: '内置小游戏商品，购买后可进入独立启动页。' },
]

// 首页分类按钮数据，由 renderCategories 动态生成按钮。
const categories = ['全部', '角色扮演', '开放世界', '动作冒险', '模拟经营', '射击竞技', '免费游戏', '折扣商品']

// localStorage 键名：用于保存用户、余额、购物车、已购游戏、订单、主题。
const storageKey = 'pure-game-site-state'

// 默认状态：首次打开网站或 localStorage 无数据时使用。
const defaultState = { user: '游客', loggedIn: false, balance: 9999, cart: [], owned: [], orders: [], theme: 'dark' }

// 当前选中的分类，默认显示全部游戏。
let activeCategory = '全部'

// 从 localStorage 读取网站状态。
// 如果读取失败，使用 defaultState，保证页面不会因为存档损坏而崩溃。
function loadState() {
  try {
    return { ...defaultState, ...JSON.parse(localStorage.getItem(storageKey)) }
  } catch {
    return { ...defaultState }
  }
}

// 把当前 state 保存到 localStorage。
// 这样刷新页面后购物车、订单、余额等信息不会丢失。
function saveState() {
  localStorage.setItem(storageKey, JSON.stringify(state))
}

// 全局状态对象。
// 注意：这是普通 JavaScript 对象，不依赖任何框架。
const state = loadState()

// 简化 document.getElementById 调用，让后面代码更简洁。
function byId(id) {
  return document.getElementById(id)
}

// 计算折后价格。
// 免费游戏价格固定为 0；折扣游戏按百分比计算。
function salePrice(game) {
  return game.price === 0 ? 0 : Math.round(game.price * (100 - game.discount)) / 100
}

// 格式化价格展示文本。
function priceText(game) {
  return game.price === 0 ? '免费' : `￥${salePrice(game).toFixed(2)}`
}

// 根据 id 查找游戏对象。
function gameById(id) {
  return games.find((game) => game.id === Number(id))
}

// 页面右下角短提示。
// 通过添加 show 类触发 CSS 动画，2.2 秒后自动隐藏。
function toast(message) {
  const el = byId('toast')
  if (!el) return
  el.textContent = message
  el.classList.add('show')
  setTimeout(() => el.classList.remove('show'), 2200)
}

// 更新所有页面共用的用户信息和余额信息。
// 因为多个页面都引用 app.js，所以这里会先判断 DOM 元素是否存在。
function updateHeader() {
  document.body.classList.toggle('neon', state.theme === 'neon')
  if (byId('welcomeUser')) byId('welcomeUser').textContent = `欢迎，${state.user}`
  if (byId('sideUser')) byId('sideUser').textContent = state.user
  if (byId('balanceText')) byId('balanceText').textContent = state.balance.toFixed(2)
  if (byId('profileUser')) byId('profileUser').textContent = state.user
  if (byId('profileBalance')) byId('profileBalance').textContent = state.balance.toFixed(2)
}

// 渲染首页分类按钮。
// 点击分类后更新 activeCategory，再重新渲染游戏列表。
function renderCategories() {
  const row = byId('categoryRow')
  if (!row) return
  row.innerHTML = categories.map((item) => `<button class="${item === activeCategory ? 'active' : ''}" data-category="${item}">${item}</button>`).join('')
  row.querySelectorAll('button').forEach((button) => {
    button.addEventListener('click', () => {
      activeCategory = button.dataset.category
      renderCategories()
      renderGames()
    })
  })
}

// 根据搜索关键字和分类条件过滤游戏列表。
function getFilteredGames() {
  const keyword = (byId('searchInput')?.value || '').trim().toLowerCase()
  return games.filter((game) => {
    const matchText = !keyword || `${game.title} ${game.category} ${game.studio}`.toLowerCase().includes(keyword)
    const matchCategory = activeCategory === '全部'
      || game.category === activeCategory
      || (activeCategory === '免费游戏' && game.price === 0)
      || (activeCategory === '折扣商品' && game.discount > 0)
    return matchText && matchCategory
  })
}

// 渲染首页游戏卡片。
// 这里使用模板字符串生成 HTML，体现 JavaScript 动态操作 DOM。
function renderGames() {
  const grid = byId('gameGrid')
  if (!grid) return
  const list = getFilteredGames()
  byId('gameCount').textContent = `共 ${list.length} 款游戏`
  grid.innerHTML = list.map((game) => `
    <article class="game-card">
      <a href="detail.html?id=${game.id}">
        <div class="cover">
          <img src="${game.cover}" alt="${game.title}">
          ${game.price === 0 ? '<span class="badge free-badge">免费</span>' : game.discount ? `<span class="badge">-${game.discount}%</span>` : ''}
        </div>
      </a>
      <div class="game-body">
        <h3>${game.title}</h3>
        <p>${game.category} / ${game.studio}</p>
        <div class="price-row">
          <strong>${priceText(game)}</strong>
          ${game.discount ? `<span>￥${game.price.toFixed(2)}</span>` : ''}
        </div>
        <button class="primary" data-add="${game.id}">${state.owned.includes(game.id) ? '启动游戏' : game.price === 0 ? '领取' : '加入购物车'}</button>
      </div>
    </article>
  `).join('')
  grid.querySelectorAll('[data-add]').forEach((button) => {
    button.addEventListener('click', () => handleGameAction(Number(button.dataset.add)))
  })
}

// 首页卡片按钮行为：
// - 已购买：跳转到 loading.html 模拟启动
// - 免费游戏：直接领取
// - 付费游戏：加入购物车
function handleGameAction(id) {
  const game = gameById(id)
  if (!game) return
  if (state.owned.includes(id)) {
    location.href = `loading.html?id=${id}`
    return
  }
  if (game.price === 0) {
    buyGame(id, '免费领取')
    return
  }
  if (!state.cart.includes(id)) state.cart.push(id)
  saveState()
  renderMiniCart()
  toast('已加入购物车')
}

// 购买或领取单个游戏。
// type 用来标记订单来源，例如“免费领取”“详情页购买”。
function buyGame(id, type = '余额支付') {
  const game = gameById(id)
  if (!game || state.owned.includes(id)) return
  const amount = salePrice(game)
  if (state.balance < amount) {
    toast('余额不足，请先充值')
    return
  }
  state.balance -= amount
  state.owned.push(id)
  state.cart = state.cart.filter((item) => item !== id)
  state.orders.unshift({ id: Date.now(), type, title: game.title, amount, time: new Date().toLocaleString() })
  saveState()
  updateHeader()
  toast(type === '免费领取' ? '领取成功' : '购买成功')
}

// 购物车结算。
// 计算购物车总价，余额足够时写入已购游戏和订单记录。
function checkoutCart() {
  if (!state.cart.length) {
    toast('购物车暂无商品')
    return
  }
  const total = state.cart.reduce((sum, id) => sum + salePrice(gameById(id)), 0)
  if (state.balance < total) {
    toast('余额不足')
    return
  }
  const names = []
  state.cart.forEach((id) => {
    if (!state.owned.includes(id)) {
      const game = gameById(id)
      state.owned.push(id)
      names.push(game.title)
    }
  })
  state.balance -= total
  state.orders.unshift({ id: Date.now(), type: '购物车结算', title: names.join('、'), amount: total, time: new Date().toLocaleString() })
  state.cart = []
  saveState()
  toast('支付成功')
  renderAll()
}

// 渲染首页右侧迷你购物车。
function renderMiniCart() {
  const box = byId('miniCartList')
  if (!box) return
  if (!state.cart.length) {
    box.innerHTML = '<div class="empty">购物车暂无商品</div>'
    return
  }
  box.innerHTML = state.cart.map((id) => {
    const game = gameById(id)
    return `<div class="mini-cart-item">${game.title}<br><strong>${priceText(game)}</strong></div>`
  }).join('')
}

// 渲染游戏详情页。
// detail.html 通过 URL 参数 id 判断要展示哪个游戏，例如 detail.html?id=1。
function renderDetail() {
  const page = byId('detailPage')
  if (!page) return
  const id = new URLSearchParams(location.search).get('id') || 1
  const game = gameById(id) || games[0]
  page.innerHTML = `
    <article class="detail-card">
      <div class="detail-cover"><img src="${game.cover}" alt="${game.title}"></div>
      <div class="detail-info">
        <h2>${game.title}</h2>
        <p>${game.desc}</p>
        <p>类型：${game.category}　厂商：${game.studio}　评分：${game.rating}</p>
        <div class="price-row">
          <strong>${priceText(game)}</strong>
          ${game.discount ? `<span>￥${game.price.toFixed(2)}</span>` : ''}
        </div>
        <div class="detail-actions">
          <button class="primary" id="detailBuy">${state.owned.includes(game.id) ? '启动游戏' : game.price === 0 ? '免费领取' : '购买游戏'}</button>
          <a class="outline link-button" href="profile.html">查看个人中心</a>
        </div>
      </div>
    </article>
  `
  byId('detailBuy').addEventListener('click', () => {
    if (state.owned.includes(game.id)) location.href = `loading.html?id=${game.id}`
    else {
      buyGame(game.id, game.price === 0 ? '免费领取' : '详情页购买')
      renderDetail()
    }
  })
}

// 渲染个人中心。
// 包括：统计卡片、购物车、已购买游戏、订单记录。
function renderProfile() {
  if (!byId('profileUser')) return
  updateHeader()
  byId('cartMetric').textContent = `${state.cart.length} 件`
  byId('ownedMetric').textContent = `${state.owned.length} 款`
  const cartList = byId('cartList')
  cartList.innerHTML = state.cart.length ? state.cart.map((id) => {
    const game = gameById(id)
    return `<div class="cart-row"><img src="${game.cover}" alt="${game.title}"><span>${game.title}</span><strong>${priceText(game)}</strong></div>`
  }).join('') : '<div class="empty">购物车暂无商品</div>'

  const ownedList = byId('ownedList')
  ownedList.innerHTML = state.owned.length ? state.owned.map((id) => {
    const game = gameById(id)
    return `<div class="owned-item"><img src="${game.cover}" alt="${game.title}"><span>${game.title}</span><a class="outline" href="loading.html?id=${id}">启动</a></div>`
  }).join('') : '<div class="empty">暂无已购游戏</div>'

  const orderList = byId('orderList')
  orderList.innerHTML = state.orders.length ? state.orders.map((order) => `
    <div class="order-row"><span>${order.type}</span><strong>${order.title}</strong><em>￥${order.amount.toFixed(2)}</em><small>${order.time}</small></div>
  `).join('') : '<div class="empty">暂无订单记录</div>'
}

// 渲染加载页。
// loading.html 也是通过 URL 参数 id 获取游戏，并展示“正在启动”动画。
function renderLoading() {
  const page = byId('loadingPage')
  if (!page) return
  const id = new URLSearchParams(location.search).get('id') || 1
  const game = gameById(id) || games[0]
  page.innerHTML = `
    <div class="loading-card">
      <img src="${game.cover}" alt="${game.title}">
      <div class="loader-ring"></div>
      <h1>${game.title} 正在启动</h1>
      <p>正在加载游戏资源、连接服务器、初始化画面...</p>
      <p>这是纯前端模拟加载页。</p>
      <a class="outline link-button" href="profile.html">返回个人中心</a>
    </div>
  `
}

// 绑定登录弹窗相关事件。
// 演示账号固定为：admin / 123456。
function bindLogin() {
  byId('loginBtn')?.addEventListener('click', () => byId('loginModal').classList.remove('hidden'))
  byId('closeModal')?.addEventListener('click', () => byId('loginModal').classList.add('hidden'))
  byId('submitLogin')?.addEventListener('click', () => {
    const username = byId('usernameInput').value.trim()
    const password = byId('passwordInput').value
    if (username === 'admin' && password === '123456') {
      state.user = username
      state.loggedIn = true
      saveState()
      updateHeader()
      byId('loginModal').classList.add('hidden')
      toast('登录成功')
    } else {
      toast('账号或密码错误')
    }
  })
}

// 绑定页面中除登录外的其他交互：
// 搜索、充值、购物车结算、个人中心结算、主题切换。
function bindActions() {
  byId('searchInput')?.addEventListener('input', renderGames)
  byId('rechargeBtn')?.addEventListener('click', () => {
    state.balance += 1000
    saveState()
    updateHeader()
    toast('充值成功')
  })
  byId('checkoutBtn')?.addEventListener('click', checkoutCart)
  byId('profileCheckout')?.addEventListener('click', checkoutCart)
  byId('themeToggle')?.addEventListener('click', () => {
    state.theme = state.theme === 'dark' ? 'neon' : 'dark'
    saveState()
    updateHeader()
    toast('主题已切换')
  })
}

// 总渲染函数。
// 每个页面都引入同一个 app.js，但并不是每个页面都有所有 DOM。
// 各个渲染函数内部都会判断对应元素是否存在，所以可以安全复用。
function renderAll() {
  updateHeader()
  renderCategories()
  renderGames()
  renderMiniCart()
  renderDetail()
  renderProfile()
  renderLoading()
}

// 页面加载后立即绑定事件并进行首次渲染。
bindLogin()
bindActions()
renderAll()
