<script setup>
import { computed, reactive, ref } from 'vue'

const REALMS = ['凡人', '练气', '筑基', '金丹', '元婴', '化神']
const STAGES = ['初期', '中期', '后期']
const LIMITS = [[10, 20, 30], [50, 100, 150], [200, 350, 500], [800, 1200, 1800], [2500, 4000, 6000], [9000, 14000, 20000]]
const events = [
  ['福源', '发现灵石矿', '灵石+200'], ['福源', '观星悟道', '修为+80'], ['福源', '灵泉旁静坐顿悟', '修为+60'],
  ['福源', '偶遇高人指点', '修为+100'], ['福源', '秘境采药成功', '灵石+140'], ['福源', '灵兽引路寻宝', '灵石+220'],
  ['灾厄', '遭遇心魔幻象', '修为-30'], ['灾厄', '误入瘴气密林', '寿命-5'], ['灾厄', '被妖兽偷袭', '寿命-8'],
  ['灾厄', '洞府禁制反噬', '寿命-10'], ['灾厄', '丹炉炸裂受伤', '寿命-9'],
  ['奇遇', '获得《太虚剑典》', '心法效率+30%'], ['奇遇', '获得《九转玄功》', '心法效率+25%'], ['奇遇', '获得《星河观想法》', '心法效率+28%'],
]

const defaultCultivator = () => ({
  name: '云游散修',
  realm: 0,
  stage: 0,
  cultivation: 0,
  lifespan: 100,
  age: 0,
  spiritStones: 100,
  totalEarned: 100,
  successRate: 1,
  method: { name: '基础吐纳术', efficiency: 1, roots: ['金', '木', '水', '火', '土'] },
  roots: ['金', '木', '水', '火', '土'],
  qiElixir: 0,
  tribulationElixir: 0,
  breakthroughBonus: 0,
  protectedBreakthrough: false,
  title: '无',
  achievements: [],
  logs: ['初入仙途，身怀五行杂灵根。'],
})

function loadCultivator() {
  try {
    const saved = JSON.parse(localStorage.getItem('cultivation-save'))
    return saved ? { ...defaultCultivator(), ...saved } : defaultCultivator()
  } catch {
    return defaultCultivator()
  }
}

const cultivator = reactive(loadCultivator())
const cultivationLimit = computed(() => LIMITS[cultivator.realm][cultivator.stage])
const progress = computed(() => Math.min(100, Math.round(cultivator.cultivation / cultivationLimit.value * 100)))
const realmName = computed(() => `${REALMS[cultivator.realm]}${STAGES[cultivator.stage]}`)
const canBreakthrough = computed(() => cultivator.cultivation >= cultivationLimit.value)

const toast = ref('')

function notify(message) {
  toast.value = message
  window.setTimeout(() => {
    if (toast.value === message) toast.value = ''
  }, 2200)
}

function log(message) {
  cultivator.logs.unshift(message)
  cultivator.logs = cultivator.logs.slice(0, 10)
}

function saveCultivator() {
  localStorage.setItem('cultivation-save', JSON.stringify(cultivator))
  notify('修仙存档已保存')
}

function resetCultivator() {
  Object.assign(cultivator, defaultCultivator())
  localStorage.removeItem('cultivation-save')
  notify('已重开修仙档')
}

function cultivate() {
  if (cultivator.lifespan <= 0) return log('寿命耗尽，无法继续修炼。')
  if (canBreakthrough.value) return log('修为已满，可以尝试突破。')
  const gain = Math.max(1, Math.round(cultivator.method.efficiency * 10))
  cultivator.cultivation = Math.min(cultivationLimit.value, cultivator.cultivation + gain)
  cultivator.lifespan -= 1
  cultivator.age += 1
  log(`闭关一年，修为 +${gain}，寿命 -1。`)
}

function breakthrough() {
  if (!canBreakthrough.value) return log('修为不足，无法突破。')
  const rate = Math.min(0.95, cultivator.successRate + cultivator.breakthroughBonus)
  cultivator.breakthroughBonus = 0
  if (Math.random() <= rate) {
    cultivator.cultivation = 0
    if (cultivator.stage < STAGES.length - 1) {
      cultivator.stage += 1
    } else if (cultivator.realm < REALMS.length - 1) {
      cultivator.realm += 1
      cultivator.stage = 0
      cultivator.lifespan += Math.pow(10, cultivator.realm)
      cultivator.successRate *= 0.75
    }
    log(`突破成功，当前境界：${realmName.value}。`)
    checkAchievements()
    return
  }
  if (cultivator.protectedBreakthrough) {
    cultivator.protectedBreakthrough = false
    log('突破失败，渡劫丹抵消了反噬。')
  } else {
    cultivator.cultivation = Math.floor(cultivator.cultivation / 2)
    cultivator.lifespan = Math.max(0, cultivator.lifespan - 10)
    log('突破失败，修为折损一半，寿命 -10。')
  }
}

function explore() {
  const event = events[Math.floor(Math.random() * events.length)]
  const [type, name, effect] = event
  if (effect.includes('灵石+')) {
    const amount = Number(effect.split('+')[1])
    cultivator.spiritStones += amount
    cultivator.totalEarned += amount
  } else if (effect.includes('修为+')) {
    cultivator.cultivation = Math.min(cultivationLimit.value, cultivator.cultivation + Number(effect.split('+')[1]))
  } else if (effect.includes('修为-')) {
    cultivator.cultivation = Math.max(0, cultivator.cultivation - Number(effect.split('-')[1]))
  } else if (effect.includes('寿命-')) {
    cultivator.lifespan = Math.max(0, cultivator.lifespan - Number(effect.split('-')[1]))
  } else if (effect.includes('心法效率+')) {
    const bonus = Number(effect.match(/\+(\d+)/)?.[1] || 0) / 100
    cultivator.method = { name: name.replace('获得', ''), efficiency: Number((1 + bonus).toFixed(2)), roots: cultivator.roots }
  }
  log(`【${type}】${name}，${effect}`)
  checkAchievements()
}

function refineQi() {
  if (cultivator.spiritStones < 50) return log('灵石不足，炼制聚气丹需要 50 灵石。')
  cultivator.spiritStones -= 50
  cultivator.qiElixir += 1
  log('炼制聚气丹成功。')
}

function refineTribulation() {
  if (cultivator.spiritStones < 200) return log('灵石不足，炼制渡劫丹需要 200 灵石。')
  cultivator.spiritStones -= 200
  cultivator.tribulationElixir += 1
  log('炼制渡劫丹成功。')
}

function useQi() {
  if (cultivator.qiElixir <= 0) return log('没有聚气丹。')
  cultivator.qiElixir -= 1
  cultivator.cultivation = Math.min(cultivationLimit.value, cultivator.cultivation + 50)
  log('服用聚气丹，修为 +50。')
}

function useTribulation() {
  if (cultivator.tribulationElixir <= 0) return log('没有渡劫丹。')
  cultivator.tribulationElixir -= 1
  cultivator.breakthroughBonus = Math.max(cultivator.breakthroughBonus, 0.2)
  cultivator.protectedBreakthrough = true
  log('服用渡劫丹，下次突破成功率 +20%，并获得失败保护。')
}

function checkAchievements() {
  if (cultivator.realm >= 3 && cultivator.age <= 100 && !cultivator.achievements.includes('百年金丹')) {
    cultivator.achievements.push('百年金丹')
    cultivator.title = '天纵奇才'
    log('解锁成就：百年金丹，获得称号：天纵奇才。')
  }
  if (cultivator.totalEarned >= 10000 && !cultivator.achievements.includes('灵石大亨')) {
    cultivator.achievements.push('灵石大亨')
    cultivator.title = '富甲仙门'
    log('解锁成就：灵石大亨，获得称号：富甲仙门。')
  }
}

function backToStore() {
  window.location.href = 'http://127.0.0.1:5173'
}
</script>

<template>
  <div class="cultivation-page">
    <div class="cultivation-hero">
      <div>
        <h1>修仙模拟器</h1>
        <p>修炼、突破、探索、炼丹、成就和本地存档。</p>
      </div>
      <div class="header-buttons">
        <button class="ghost" @click="saveCultivator">保存进度</button>
        <button class="ghost" @click="backToStore">返回商城</button>
      </div>
    </div>
    <div class="cultivation-grid">
      <section class="status-card">
        <h3>{{ cultivator.name }}</h3>
        <div class="realm">{{ realmName }}</div>
        <div class="progress">
          <span :style="{ width: progress + '%' }"></span>
        </div>
        <p>{{ cultivator.cultivation }} / {{ cultivationLimit }} 修为</p>
        <div class="stats">
          <span>寿命 {{ cultivator.lifespan }}</span>
          <span>年龄 {{ cultivator.age }}</span>
          <span>灵石 {{ cultivator.spiritStones }}</span>
          <span>称号 {{ cultivator.title }}</span>
        </div>
      </section>
      <section class="action-card">
        <h3>行动</h3>
        <button class="primary" @click="cultivate">闭关修炼</button>
        <button class="danger" :disabled="!canBreakthrough" @click="breakthrough">尝试突破</button>
        <button class="ghost" @click="explore">探索秘境</button>
        <button class="ghost" @click="resetCultivator">重开</button>
      </section>
      <section class="alchemy-card">
        <h3>炼丹房</h3>
        <button class="ghost" @click="refineQi">炼制聚气丹 50 灵石</button>
        <button class="ghost" @click="useQi">服用聚气丹 x{{ cultivator.qiElixir }}</button>
        <button class="ghost" @click="refineTribulation">炼制渡劫丹 200 灵石</button>
        <button class="ghost" @click="useTribulation">服用渡劫丹 x{{ cultivator.tribulationElixir }}</button>
      </section>
      <section class="method-card">
        <h3>功法与成就</h3>
        <p>当前心法：{{ cultivator.method.name }}</p>
        <p>效率：{{ cultivator.method.efficiency }}x</p>
        <div class="achievements">
          <span v-for="item in cultivator.achievements" :key="item">{{ item }}</span>
          <span v-if="!cultivator.achievements.length">暂无成就</span>
        </div>
      </section>
      <section class="log-card">
        <h3>修行日志</h3>
        <p v-for="item in cultivator.logs" :key="item">{{ item }}</p>
      </section>
    </div>
  </div>
  <div v-if="toast" class="toast">{{ toast }}</div>
</template>

<style scoped>
* {
  box-sizing: border-box;
}

.cultivation-page {
  min-height: 100vh;
  padding: 30px 34px;
  background:
    radial-gradient(circle at 80% 0, rgba(110, 230, 168, 0.14), transparent 32%),
    #132241;
  color: #eef5ff;
  font-family: "Microsoft YaHei UI", "Segoe UI", sans-serif;
}

.cultivation-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px;
  border-radius: 8px;
  background: linear-gradient(110deg, #1f3a34, #211c32);
  border: 1px solid rgba(126, 150, 255, 0.22);
  margin-bottom: 24px;
}

.cultivation-hero h1 {
  margin: 0;
  font-size: 28px;
}

.cultivation-hero p {
  margin-top: 8px;
  color: #aebce2;
}

.header-buttons {
  display: flex;
  gap: 12px;
}

.cultivation-grid {
  display: grid;
  grid-template-columns: 1.25fr 0.9fr 1fr;
  gap: 18px;
}

.status-card {
  grid-row: span 2;
}

.status-card,
.action-card,
.alchemy-card,
.method-card,
.log-card {
  padding: 22px;
  background: #1d2d51;
  border-radius: 8px;
}

.realm {
  margin: 16px 0;
  color: #ffd35c;
  font-size: 34px;
  font-weight: 900;
}

.progress {
  height: 14px;
  overflow: hidden;
  background: #101a30;
  border-radius: 999px;
}

.progress span {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #55dc75, #ffd15c);
}

.stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-top: 22px;
}

.stats span,
.achievements span {
  padding: 12px;
  border-radius: 8px;
  background: #16223e;
  color: #d7e3ff;
}

.action-card,
.alchemy-card,
.method-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.log-card {
  grid-column: 2 / 4;
  min-height: 230px;
}

.log-card p {
  padding: 10px 0;
  color: #dce7ff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.achievements {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 16px;
}

button {
  border: 0;
  font: inherit;
  min-height: 42px;
  padding: 0 18px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 800;
}

.primary {
  background: #667ded;
  color: #fff;
}

.ghost {
  color: #dbe5ff;
  background: #273253;
  border: 1px solid rgba(135, 159, 255, 0.28);
}

.danger {
  color: #ffdce0;
  background: #3d2030;
  border: 1px solid #ff5670;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.toast {
  position: fixed;
  right: 28px;
  bottom: 28px;
  padding: 14px 20px;
  border-radius: 8px;
  background: #22345a;
  color: #fff;
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.3);
}
</style>