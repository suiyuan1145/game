<script setup>
import { computed, reactive, ref } from 'vue'

import elden from './assets/covers/elden.jpg'
import cyberpunk from './assets/covers/cyberpunk.jpg'
import cs2 from './assets/covers/cs2.jpg'
import wukong from './assets/covers/wukong.jpg'
import stardew from './assets/covers/stardew.jpg'
import sekiro from './assets/covers/sekiro.jpg'
import witcher from './assets/covers/witcher.jpg'
import ff7 from './assets/covers/ff7.jpg'
import baldur from './assets/covers/baldur.jpg'
import palworld from './assets/covers/palworld.jpg'
import hollow from './assets/covers/hollow.jpg'
import star from './assets/covers/star.jpg'

const CULTIVATION_PORT = 'http://127.0.0.1:5174'
const SHOOTING_PORT = 'http://127.0.0.1:5175'

const games = [
  { id: 1, title: '艾尔登法环', category: '角色扮演', studio: 'FromSoftware', price: 398, discount: 25, rating: 9.7, cover: elden, desc: '开放世界魂系角色扮演游戏，探索交界地、挑战半神，并收集强大的装备与法术。' },
  { id: 2, title: '赛博朋克2077', category: '开放世界', studio: 'CD Projekt', price: 299, discount: 50, rating: 9.1, cover: cyberpunk, desc: '霓虹都市、义体改造和高自由度任务线，体验夜之城的未来冒险。' },
  { id: 3, title: 'Counter-Strike 2', category: '射击竞技', studio: 'Valve', price: 0, discount: 0, rating: 9.1, cover: cs2, desc: '免费多人竞技射击游戏，经典爆破模式、精准枪法和团队配合是胜负关键。' },
  { id: 4, title: '黑神话：悟空', category: '动作冒险', studio: 'Game Science', price: 268, discount: 0, rating: 9.6, cover: wukong, desc: '东方神话题材动作冒险游戏，化身天命人，面对妖王与古老传说。' },
  { id: 5, title: '星露谷物语', category: '模拟经营', studio: 'ConcernedApe', price: 68, discount: 29, rating: 9.4, cover: stardew, desc: '经营农场、采矿、钓鱼、社交，打造属于自己的乡村生活。' },
  { id: 6, title: '只狼：影逝二度', category: '动作冒险', studio: 'FromSoftware', price: 268, discount: 0, rating: 9.5, cover: sekiro, desc: '高强度剑戟动作游戏，凭借格挡、忍具与身法击败强敌。' },
  { id: 7, title: '巫师3：狂猎', category: '角色扮演', studio: 'CD Projekt', price: 199, discount: 50, rating: 9.6, cover: witcher, desc: '扮演猎魔人杰洛特，调查怪物、政治阴谋与命运之子的踪迹。' },
  { id: 8, title: '最终幻想7 重制版', category: '角色扮演', studio: 'Square Enix', price: 446, discount: 0, rating: 9.0, cover: ff7, desc: '经典 RPG 的现代化重制，在米德加展开电影级冒险与即时战斗。' },
  { id: 9, title: '博德之门3', category: '角色扮演', studio: 'Larian Studios', price: 298, discount: 10, rating: 9.8, cover: baldur, desc: '基于队伍和骰子的史诗 RPG，自由选择会改变每段旅程。' },
  { id: 10, title: '幻兽帕鲁', category: '开放世界', studio: 'Pocketpair', price: 108, discount: 20, rating: 9.0, cover: palworld, desc: '捕获帕鲁、建造基地、探索岛屿，并与好友一起冒险。' },
  { id: 11, title: '空洞骑士', category: '动作冒险', studio: 'Team Cherry', price: 48, discount: 30, rating: 9.3, cover: hollow, desc: '手绘地下王国探索游戏，挑战首领、发现隐藏路线与古老秘密。' },
  { id: 12, title: '修仙模拟器', category: '可玩小游戏', studio: 'Cultivation Studio', price: 0, discount: 0, rating: 9.2, cover: star, playable: true, miniGame: 'cultivation', gamePort: CULTIVATION_PORT, desc: '由桌面端修仙游戏移植而来的网页小游戏，包含修炼、突破、探索、炼丹和本地存档。' },
  { id: 13, title: '星河射击战', category: '可玩小游戏', studio: 'Nebula Arcade', price: 0, discount: 0, rating: 9.0, cover: cs2, playable: true, miniGame: 'shooter', gamePort: SHOOTING_PORT, desc: '从之前的网页射击小游戏思路改造而来，独立 Canvas 页面，支持移动、射击、计分和重新开始。' },
]

const categories = ['全部', '角色扮演', '开放世界', '动作冒险', '模拟经营', '射击竞技', '免费游戏', '折扣商品', '可玩小游戏']
const navItems = [
  ['store', '游戏列表'],
  ['library', '已购买游戏'],
  ['account', '我的账户'],
  ['orders', '我的订单'],
]

const accounts = reactive({ admin: '123456' })
const session = reactive({ loggedIn: false, user: '游客', balance: 9999 })
const loginForm = reactive({ name: 'admin', password: '123456' })
const registerForm = reactive({ name: '', password: '', confirm: '' })
const view = ref('store')
const authMode = ref('login')
const query = ref('')
const category = ref('全部')
const selectedGame = ref(games[0])
const activeGame = ref(null)
const cart = ref([])
const owned = ref([])
const orders = ref([])
const toast = ref('')

const salePrice = (game) => game.price === 0 ? 0 : Math.round(game.price * (100 - game.discount)) / 100
const priceText = (game) => game.price === 0 ? '免费' : `￥${salePrice(game).toFixed(2)}`
const ownedIds = computed(() => new Set(owned.value.map(game => game.id)))
const cartIds = computed(() => new Set(cart.value.map(game => game.id)))
const cartTotal = computed(() => cart.value.reduce((sum, game) => sum + salePrice(game), 0))
const filteredGames = computed(() => {
  const keyword = query.value.trim().toLowerCase()
  return games.filter((game) => {
    const matchKeyword = !keyword || `${game.title} ${game.category} ${game.studio}`.toLowerCase().includes(keyword)
    const matchCategory = category.value === '全部'
      || game.category === category.value
      || (category.value === '免费游戏' && game.price === 0)
      || (category.value === '折扣商品' && game.discount > 0)
      || (category.value === '可玩小游戏' && game.playable)
    return matchKeyword && matchCategory
  })
})

function notify(message) {
  toast.value = message
  window.setTimeout(() => {
    if (toast.value === message) toast.value = ''
  }, 2200)
}

function login() {
  if (accounts[loginForm.name] === loginForm.password) {
    session.loggedIn = true
    session.user = loginForm.name
    notify('登录成功')
    return
  }
  notify('用户名或密码错误')
}

function register() {
  if (registerForm.name.length < 3) return notify('用户名至少 3 位')
  if (accounts[registerForm.name]) return notify('该用户名已存在')
  if (registerForm.password.length < 6) return notify('密码至少 6 位')
  if (registerForm.password !== registerForm.confirm) return notify('两次密码不一致')
  accounts[registerForm.name] = registerForm.password
  session.loggedIn = true
  session.user = registerForm.name
  session.balance = 9999
  authMode.value = 'login'
  notify('注册成功，已自动登录')
}

function logout() {
  session.loggedIn = false
  session.user = '游客'
  cart.value = []
  owned.value = []
  orders.value = []
  view.value = 'store'
}

function addToCart(game) {
  selectedGame.value = game
  if (ownedIds.value.has(game.id)) return notify('该游戏已在游戏库')
  if (game.price === 0) return claimGame(game)
  if (!cartIds.value.has(game.id)) cart.value.push(game)
  notify('已加入购物车')
}

function claimGame(game) {
  if (!ownedIds.value.has(game.id)) {
    owned.value.push(game)
    orders.value.unshift({ id: Date.now(), type: '免费领取', title: game.title, amount: 0, time: new Date().toLocaleString() })
  }
  notify('已加入游戏库')
  if (game.playable) openGamePage(game)
}

function openGamePage(game) {
  activeGame.value = game
  if (game.gamePort) {
    window.open(game.gamePort, '_blank')
    return
  }
  view.value = 'loadingGame'
}

function checkout(items = cart.value) {
  const payable = items.filter(game => !ownedIds.value.has(game.id))
  const total = payable.reduce((sum, game) => sum + salePrice(game), 0)
  if (!payable.length) return notify('没有可支付商品')
  if (session.balance < total) return notify('余额不足')
  session.balance -= total
  payable.forEach(game => {
    if (!ownedIds.value.has(game.id)) owned.value.push(game)
  })
  cart.value = cart.value.filter(game => !payable.some(item => item.id === game.id))
  orders.value.unshift({ id: Date.now(), type: '余额支付', title: payable.map(game => game.title).join('、'), amount: total, time: new Date().toLocaleString() })
  view.value = 'library'
  notify('支付成功')
}
</script>

<template>
  <div class="app">
    <aside class="sidebar">
      <div class="brand">星穹游戏商城</div>
      <div class="subbrand">Vue Web Edition</div>
      <nav>
        <button v-for="[key, label] in navItems" :key="key" :class="{ active: view === key }" @click="view = key">
          {{ label }}
        </button>
      </nav>
      <div class="version">v2.0.0</div>
    </aside>

    <main class="main">
      <header class="topbar">
        <div>
          <h1>{{ view === 'loadingGame' ? '游戏启动中' : '游戏购买系统' }}</h1>
          <p>深色科幻商城布局，内置真实游戏封面，小游戏启动独立端口</p>
        </div>
        <div class="user-actions">
          <span>欢迎，{{ session.user }}</span>
          <button v-if="session.loggedIn" class="danger" @click="logout">退出登录</button>
        </div>
      </header>

      <section v-if="!session.loggedIn" class="auth-panel">
        <div class="auth-card">
          <h2>{{ authMode === 'login' ? '账户登录' : '注册账户' }}</h2>
          <template v-if="authMode === 'login'">
            <input v-model="loginForm.name" placeholder="用户名" />
            <input v-model="loginForm.password" type="password" placeholder="密码" />
            <button class="primary" @click="login">登录</button>
            <button class="ghost" @click="authMode = 'register'">注册新账户</button>
            <p>默认账户：admin / 123456</p>
          </template>
          <template v-else>
            <input v-model="registerForm.name" placeholder="用户名" />
            <input v-model="registerForm.password" type="password" placeholder="密码" />
            <input v-model="registerForm.confirm" type="password" placeholder="确认密码" />
            <button class="primary" @click="register">完成注册</button>
            <button class="ghost" @click="authMode = 'login'">返回登录</button>
          </template>
        </div>
      </section>

      <template v-else>
        <section v-if="view === 'store'" class="store-layout">
          <div class="store-content">
            <div class="toolbar">
              <input v-model="query" placeholder="搜索游戏名称、类型或厂商..." />
              <span>共 {{ filteredGames.length }} 款游戏</span>
            </div>
            <div class="chips">
              <button v-for="item in categories" :key="item" :class="{ selected: category === item }" @click="category = item">
                {{ item }}
              </button>
            </div>
            <div class="game-grid">
              <article v-for="game in filteredGames" :key="game.id" class="game-card" @click="selectedGame = game">
                <div class="cover-wrap">
                  <img :src="game.cover" :alt="game.title" />
                  <span v-if="game.discount" class="discount">-{{ game.discount }}%</span>
                  <span v-if="game.playable" class="playable">可玩</span>
                </div>
                <div class="game-body">
                  <h3>{{ game.title }}</h3>
                  <p>{{ game.category }}</p>
                  <div class="price-row">
                    <strong>{{ priceText(game) }}</strong>
                    <span v-if="game.discount">￥{{ game.price.toFixed(2) }}</span>
                  </div>
                  <button class="primary" @click.stop="ownedIds.has(game.id) ? openGamePage(game) : addToCart(game)">
                    {{ ownedIds.has(game.id) ? '启动游戏' : game.price === 0 ? '领取' : '加入购物车' }}
                  </button>
                </div>
              </article>
            </div>
          </div>
          <aside class="right-panel">
            <div class="avatar">仙</div>
            <h2>{{ session.user }}</h2>
            <p>@{{ session.user }}</p>
            <div class="balance">￥ {{ session.balance.toFixed(2) }}</div>
            <button class="success" @click="session.balance += 1000">充值</button>
            <button class="ghost" @click="view = 'library'">我的游戏库</button>
            <div class="detail-card">
              <img :src="selectedGame.cover" :alt="selectedGame.title" />
              <h3>{{ selectedGame.title }}</h3>
              <p>{{ selectedGame.desc }}</p>
              <button v-if="ownedIds.has(selectedGame.id)" class="primary" @click="openGamePage(selectedGame)">启动游戏</button>
              <button v-else-if="selectedGame.playable" class="primary" @click="claimGame(selectedGame)">领取并进入</button>
            </div>
          </aside>
        </section>

        <section v-if="view === 'library'" class="page">
          <div class="hero">
            <h2>已购买游戏</h2>
            <p>已入库 {{ owned.length }} 款游戏。小游戏将在新标签页独立端口启动。</p>
          </div>
          <div v-if="!owned.length" class="empty">暂无已购游戏，去商城领取或购买吧。</div>
          <div v-else class="library-grid">
            <article v-for="game in owned" :key="game.id" class="library-card">
              <img :src="game.cover" :alt="game.title" />
              <div>
                <h3>{{ game.title }}</h3>
                <p>{{ game.studio }} / {{ game.category }}</p>
                <button class="play" @click="openGamePage(game)">
                  {{ game.miniGame === 'cultivation' ? '开始修仙' : game.miniGame === 'shooter' ? '开始射击' : '启动游戏' }}
                </button>
              </div>
            </article>
          </div>
        </section>

        <section v-if="view === 'account'" class="page">
          <div class="hero">
            <h2>我的账户</h2>
            <p>余额、购物车、游戏库与订单概览。</p>
          </div>
          <div class="metrics">
            <div><span>余额</span><strong>￥{{ session.balance.toFixed(2) }}</strong></div>
            <div><span>购物车</span><strong>{{ cart.length }} 件</strong></div>
            <div><span>已购买</span><strong>{{ owned.length }} 款</strong></div>
            <div><span>订单</span><strong>{{ orders.length }} 条</strong></div>
          </div>
          <div class="cart-list">
            <h3>购物车</h3>
            <div v-if="!cart.length" class="empty small">购物车暂无商品。</div>
            <div v-for="game in cart" :key="game.id" class="cart-row">
              <img :src="game.cover" :alt="game.title" />
              <span>{{ game.title }}</span>
              <strong>{{ priceText(game) }}</strong>
            </div>
            <button class="primary" :disabled="!cart.length" @click="checkout()">结算 ￥{{ cartTotal.toFixed(2) }}</button>
          </div>
        </section>

        <section v-if="view === 'orders'" class="page">
          <div class="hero">
            <h2>我的订单</h2>
            <p>购买、领取和支付记录。</p>
          </div>
          <div v-if="!orders.length" class="empty">暂无订单记录。</div>
          <div v-for="order in orders" :key="order.id" class="order-row">
            <span>{{ order.type }}</span>
            <strong>{{ order.title }}</strong>
            <em>￥{{ order.amount.toFixed(2) }}</em>
            <small>{{ order.time }}</small>
          </div>
        </section>

        <section v-if="view === 'loadingGame'" class="loading-page">
          <button class="ghost back-button" @click="view = 'library'">返回游戏库</button>
          <div class="loading-card">
            <img v-if="activeGame" :src="activeGame.cover" :alt="activeGame.title" />
            <div class="loader-ring"></div>
            <h2>{{ activeGame?.title || '游戏' }} 正在启动</h2>
            <p>正在连接服务器、校验资源、初始化画面...</p>
            <button v-if="activeGame?.gamePort" class="primary" style="margin-top: 20px" @click="window.open(activeGame.gamePort, '_blank')">
              在新标签页打开
            </button>
          </div>
        </section>
      </template>
    </main>
    <div v-if="toast" class="toast">{{ toast }}</div>
  </div>
</template>