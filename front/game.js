const canvas = document.getElementById('gameCanvas');
const ctx = canvas.getContext('2d');
const W = canvas.width;
const H = canvas.height;

const scoreDisplay = document.getElementById('scoreDisplay');
const livesDisplay = document.getElementById('livesDisplay');
const levelDisplay = document.getElementById('levelDisplay');
const finalScore = document.getElementById('finalScore');
const startOverlay = document.getElementById('startOverlay');
const gameOverOverlay = document.getElementById('gameOverOverlay');
const startBtn = document.getElementById('startBtn');
const restartBtn = document.getElementById('restartBtn');

let game = {
    running: false, over: false, paused: false, score: 0, lives: 3, level: 1,
    frame: 0, autoFire: false, lastShotFrame: 0, fireRate: 12,
    enemySpawnRate: 60, spawnCounter: 0, enemiesKilled: 0,
};

let player = null;
let bullets = [];
let enemies = [];
let particles = [];
let stars = [];
const keys = { left: false, right: false, space: false };

// ===== 星空 =====
function initStars() {
    stars = [];
    for (let i = 0; i < 120; i++) {
        stars.push({
            x: Math.random() * W, y: Math.random() * H,
            size: Math.random() * 2.5 + 0.5,
            speed: Math.random() * 1.5 + 0.3,
            brightness: Math.random() * 0.5 + 0.5,
        });
    }
}

function updateStars() {
    for (const s of stars) {
        s.y += s.speed;
        if (s.y > H) { s.y = 0; s.x = Math.random() * W; }
    }
}

function drawStars() {
    for (const s of stars) {
        const alpha = s.brightness * (0.7 + 0.3 * Math.sin(game.frame * 0.02 + s.x));
        ctx.fillStyle = 'rgba(255, 255, 255, ' + alpha + ')';
        ctx.beginPath();
        ctx.arc(s.x, s.y, s.size, 0, Math.PI * 2);
        ctx.fill();
    }
}

// ===== 玩家 =====
function createPlayer() {
    return { x: W / 2, y: H - 70, w: 40, h: 40, speed: 5, invincible: 0 };
}

function drawPlayer(p) {
    const cx = p.x, cy = p.y;
    if (p.invincible > 0 && Math.floor(p.invincible / 4) % 2 === 0) return;
    ctx.save();
    ctx.shadowColor = '#00f7ff';
    ctx.shadowBlur = 20;
    ctx.beginPath();
    ctx.moveTo(cx, cy - 25);
    ctx.lineTo(cx - 22, cy + 18);
    ctx.lineTo(cx - 8, cy + 8);
    ctx.lineTo(cx, cy + 14);
    ctx.lineTo(cx + 8, cy + 8);
    ctx.lineTo(cx + 22, cy + 18);
    ctx.closePath();
    const grad = ctx.createLinearGradient(cx, cy - 25, cx, cy + 18);
    grad.addColorStop(0, '#00d4ff');
    grad.addColorStop(0.5, '#0088ff');
    grad.addColorStop(1, '#0044aa');
    ctx.fillStyle = grad;
    ctx.fill();
    ctx.strokeStyle = '#00f7ff';
    ctx.lineWidth = 2;
    ctx.stroke();
    ctx.shadowBlur = 10;
    ctx.beginPath();
    ctx.ellipse(cx, cy - 8, 6, 10, 0, 0, Math.PI * 2);
    ctx.fillStyle = 'rgba(255, 255, 255, 0.3)';
    ctx.fill();
    ctx.shadowBlur = 25;
    ctx.shadowColor = '#ff6600';
    const flameLen = 10 + Math.random() * 12;
    ctx.beginPath();
    ctx.moveTo(cx - 10, cy + 16);
    ctx.quadraticCurveTo(cx - 4, cy + 16 + flameLen, cx, cy + 16 + flameLen + 4);
    ctx.quadraticCurveTo(cx + 4, cy + 16 + flameLen, cx + 10, cy + 16);
    ctx.closePath();
    const flameGrad = ctx.createLinearGradient(cx, cy + 16, cx, cy + 16 + flameLen + 4);
    flameGrad.addColorStop(0, '#ffaa00');
    flameGrad.addColorStop(0.4, '#ff6600');
    flameGrad.addColorStop(1, 'rgba(255, 0, 0, 0)');
    ctx.fillStyle = flameGrad;
    ctx.fill();
    ctx.restore();
}

function updatePlayer(p) {
    if (!p) return;
    let moveSpeed = p.speed;
    if (game.level >= 3) moveSpeed += 1;
    if (game.level >= 6) moveSpeed += 1;
    if (keys.left) p.x -= moveSpeed;
    if (keys.right) p.x += moveSpeed;
    p.x = Math.max(24, Math.min(W - 24, p.x));
    if (p.invincible > 0) p.invincible--;
}

// ===== 子弹 =====
function fireBullet() {
    if (!player) return;
    if (game.frame - game.lastShotFrame < game.fireRate) return;
    game.lastShotFrame = game.frame;
    const count = Math.min(1 + Math.floor((game.level - 1) / 3), 3);
    if (count === 1) {
        bullets.push({ x: player.x, y: player.y - 25, w: 4, h: 14, speed: 9, damage: 1 });
    } else {
        const spread = 15;
        const startOffset = (count - 1) * spread / 2;
        for (let i = 0; i < count; i++) {
            bullets.push({ x: player.x - startOffset + i * spread, y: player.y - 25, w: 4, h: 14, speed: 9, damage: 1 });
        }
    }
}

function drawBullet(b) {
    ctx.save();
    ctx.shadowColor = '#00ffff';
    ctx.shadowBlur = 18;
    const grad = ctx.createLinearGradient(b.x, b.y, b.x, b.y + b.h);
    grad.addColorStop(0, '#00ffff');
    grad.addColorStop(1, '#0066ff');
    ctx.fillStyle = grad;
    ctx.fillRect(b.x - b.w / 2, b.y, b.w, b.h);
    ctx.shadowBlur = 30;
    ctx.fillStyle = 'rgba(150, 230, 255, 0.4)';
    ctx.beginPath();
    ctx.arc(b.x, b.y, 5, 0, Math.PI * 2);
    ctx.fill();
    ctx.restore();
}

function updateBullets() {
    for (let i = bullets.length - 1; i >= 0; i--) {
        bullets[i].y -= bullets[i].speed;
        if (bullets[i].y + bullets[i].h < 0) bullets.splice(i, 1);
    }
}

// ===== 敌人 =====
function spawnEnemy() {
    const enemyTypes = [
        { w: 34, h: 34, hp: 1, speed: 1.2, color: '#ff4466', score: 10 },
        { w: 40, h: 40, hp: 2, speed: 0.8, color: '#ff8800', score: 25 },
        { w: 48, h: 48, hp: 3, speed: 0.6, color: '#cc44ff', score: 50 },
    ];
    let maxType = 0;
    if (game.level >= 3) maxType = 1;
    if (game.level >= 5) maxType = 2;
    const typeIdx = Math.floor(Math.random() * (maxType + 1));
    const type = enemyTypes[typeIdx];
    const speedBonus = (game.level - 1) * 0.1;
    enemies.push({
        x: Math.random() * (W - 60) + 30, y: -30,
        w: type.w, h: type.h, hp: type.hp, maxHp: type.hp,
        speed: type.speed + speedBonus, color: type.color, score: type.score,
        wobblePhase: Math.random() * Math.PI * 2, type: typeIdx,
    });
}

function drawEnemy(e) {
    ctx.save();
    const cx = e.x, cy = e.y, halfW = e.w / 2, halfH = e.h / 2;
    ctx.shadowColor = e.color;
    ctx.shadowBlur = 15;
    ctx.beginPath();
    ctx.ellipse(cx, cy, halfW, halfH, 0, 0, Math.PI * 2);
    ctx.fillStyle = e.color + '33';
    ctx.fill();
    ctx.beginPath();
    ctx.ellipse(cx, cy - 4, halfW * 0.7, halfH * 0.5, 0, 0, Math.PI * 2);
    ctx.fillStyle = e.color;
    ctx.fill();
    ctx.strokeStyle = '#fff';
    ctx.lineWidth = 1.5;
    ctx.stroke();
    ctx.beginPath();
    ctx.ellipse(cx, cy - 6, halfW * 0.4, halfH * 0.35, 0, Math.PI, 0);
    ctx.fillStyle = 'rgba(255,255,255,0.15)';
    ctx.fill();
    ctx.shadowBlur = 8;
    for (let i = -1; i <= 1; i++) {
        ctx.beginPath();
        ctx.arc(cx + i * 10, cy + 2, 3, 0, Math.PI * 2);
        ctx.fillStyle = 'rgba(255,255,255,0.6)';
        ctx.fill();
    }
    if (e.hp < e.maxHp) {
        const barW = e.w + 10, barH = 4, barX = cx - barW / 2, barY = cy - halfH - 12;
        ctx.shadowBlur = 0;
        ctx.fillStyle = 'rgba(255,0,0,0.3)';
        ctx.fillRect(barX, barY, barW, barH);
        ctx.fillStyle = '#ff4444';
        ctx.fillRect(barX, barY, barW * (e.hp / e.maxHp), barH);
    }
    ctx.restore();
}

function updateEnemies() {
    for (let i = enemies.length - 1; i >= 0; i--) {
        const e = enemies[i];
        e.y += e.speed;
        e.x += Math.sin(game.frame * 0.02 + e.wobblePhase) * 0.4;
        e.x = Math.max(e.w / 2 + 5, Math.min(W - e.w / 2 - 5, e.x));
        if (e.y + e.h / 2 > H) {
            game.lives--;
            updateHUD();
            enemies.splice(i, 1);
            if (game.lives <= 0) endGame();
            continue;
        }
        if (player && rectCollide(e, player)) {
            if (player.invincible <= 0) {
                game.lives--;
                updateHUD();
                player.invincible = 60;
                spawnHitParticles(player.x, player.y, '#00f7ff', 20);
                enemies.splice(i, 1);
                if (game.lives <= 0) endGame();
                continue;
            }
        }
        if (game.level >= 2 && Math.random() < 0.002 * game.level) {
            enemies.push({
                x: e.x + (Math.random() - 0.5) * 20, y: e.y + e.h / 2,
                w: 6, h: 6, hp: 1, maxHp: 1, speed: 3.5, color: '#ff3333',
                score: 0, wobblePhase: 0, type: -1, isEnemyBullet: true,
            });
        }
    }
    for (let i = enemies.length - 1; i >= 0; i--) {
        if (enemies[i].isEnemyBullet && enemies[i].y > H + 20) enemies.splice(i, 1);
    }
}

// ===== 粒子 =====
function spawnHitParticles(x, y, color, count = 15) {
    for (let i = 0; i < count; i++) {
        const angle = Math.random() * Math.PI * 2;
        const speed = Math.random() * 5 + 2;
        particles.push({
            x, y, vx: Math.cos(angle) * speed, vy: Math.sin(angle) * speed,
            life: 30 + Math.random() * 30, maxLife: 60, size: Math.random() * 4 + 2, color,
        });
    }
}

function drawParticles() {
    for (const p of particles) {
        const alpha = p.life / p.maxLife;
        ctx.save();
        ctx.globalAlpha = alpha;
        ctx.shadowColor = p.color;
        ctx.shadowBlur = 10;
        ctx.fillStyle = p.color;
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.size * alpha, 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();
    }
}

function updateParticles() {
    for (let i = particles.length - 1; i >= 0; i--) {
        const p = particles[i];
        p.x += p.vx; p.y += p.vy; p.vy += 0.05;
        p.life--;
        if (p.life <= 0) particles.splice(i, 1);
    }
}

// ===== 碰撞检测 =====
function rectCollide(a, b) {
    return (
        a.x - a.w / 2 < b.x + b.w / 2 &&
        a.x + a.w / 2 > b.x - b.w / 2 &&
        a.y - a.h / 2 < b.y + b.h / 2 &&
        a.y + a.h / 2 > b.y - b.h / 2
    );
}

function checkCollisions() {
    for (let bi = bullets.length - 1; bi >= 0; bi--) {
        const b = bullets[bi];
        for (let ei = enemies.length - 1; ei >= 0; ei--) {
            const e = enemies[ei];
            if (e.isEnemyBullet) continue;
            if (rectCollide(b, e)) {
                e.hp -= b.damage;
                spawnHitParticles(b.x, b.y, e.color, 8);
                bullets.splice(bi, 1);
                if (e.hp <= 0) {
                    game.score += e.score;
                    game.enemiesKilled++;
                    spawnHitParticles(e.x, e.y, e.color, 25);
                    enemies.splice(ei, 1);
                    updateHUD();
                    checkLevelUp();
                }
                break;
            }
        }
    }
    if (player) {
        for (let ei = enemies.length - 1; ei >= 0; ei--) {
            const e = enemies[ei];
            if (!e.isEnemyBullet) continue;
            if (rectCollide(player, e)) {
                if (player.invincible <= 0) {
                    game.lives--;
                    updateHUD();
                    player.invincible = 60;
                    spawnHitParticles(player.x, player.y, '#ff3333', 15);
                    enemies.splice(ei, 1);
                    if (game.lives <= 0) endGame();
                }
            }
        }
    }
}

// ===== 等级 / 增益选择 =====
function checkLevelUp() {
    const newLevel = Math.floor(game.enemiesKilled / 10) + 1;
    if (newLevel > game.level) {
        game.level = newLevel;
        game.fireRate = Math.max(6, 12 - Math.floor((game.level - 1) / 2));
        game.enemySpawnRate = Math.max(20, 60 - (game.level - 1) * 5);
        updateHUD();
        if (player) {
            spawnHitParticles(player.x, player.y - 20, '#ffd700', 30);
            spawnHitParticles(player.x, player.y - 20, '#00f7ff', 20);
        }
        // 每5级弹出增益选择（暂停游戏）
        if (game.level % 5 === 0) {
            showBonusChoice();
        }
    }
}

// ---------- 增益系统（选择时暂停） ----------
const bonusOptions = [
    { id: 'split', name: '💥 子弹分裂', desc: '子弹命中时分裂成3颗' },
    { id: 'bigger', name: '🔵 子弹变大', desc: '子弹尺寸 +1.0' },
    { id: 'wingman', name: '🛩️ 增加僚机', desc: '增加一个自动射击的僚机' },
];

let wingmen = [];
let bulletSizeBonus = 0;
let bulletSplit = false;

function showBonusChoice() {
    // 暂停游戏（先把更新停下，但保留画面）
    const wasPaused = game.paused;
    game.paused = true;

    const choice = prompt(
        '🌟 选择增益！\n\n' +
        '1. 💥 子弹分裂（命中时分裂成3颗）\n' +
        '2. 🔵 子弹变大（尺寸 +1.0）\n' +
        '3. 🛩️ 增加僚机（自动射击的僚机）\n\n' +
        '请输入 1、2 或 3：',
        '1'
    );

    let appliedBonus = null;
    switch (choice) {
        case '1':
            bulletSplit = true;
            appliedBonus = '💥 子弹分裂';
            break;
        case '2':
            bulletSizeBonus += 1.0;
            appliedBonus = '🔵 子弹变大（当前 +' + bulletSizeBonus + '）';
            break;
        case '3':
            wingmen.push({ offsetX: -30, offsetY: 0 });
            wingmen.push({ offsetX: 30, offsetY: 0 });
            appliedBonus = '🛩️ 增加僚机';
            break;
        default:
            appliedBonus = '未选择，自动分配：🔵 子弹变大';
            bulletSizeBonus += 1.0;
    }

    // 短暂显示选择结果（用 console）
    console.log('✨ 已获增益：' + appliedBonus);

    // 恢复游戏
    game.paused = false;
}

function drawWingmen() {
    if (!player || wingmen.length === 0) return;
    for (const wm of wingmen) {
        const wx = player.x + wm.offsetX;
        const wy = player.y + wm.offsetY - 10;
        ctx.save();
        ctx.shadowColor = '#00ffaa';
        ctx.shadowBlur = 12;
        ctx.fillStyle = '#00ffaa';
        ctx.beginPath();
        ctx.moveTo(wx, wy - 12);
        ctx.lineTo(wx - 8, wy + 6);
        ctx.lineTo(wx + 8, wy + 6);
        ctx.closePath();
        ctx.fill();
        ctx.strokeStyle = '#ffffff';
        ctx.lineWidth = 1;
        ctx.stroke();
        ctx.restore();
    }
}

function fireWingmenBullets() {
    if (!player || wingmen.length === 0) return;
    for (const wm of wingmen) {
        const baseX = player.x + wm.offsetX;
        const baseY = player.y + wm.offsetY - 10;
        if (game.frame % 18 === 0) {
            bullets.push({
                x: baseX, y: baseY, w: 3, h: 10, speed: 8, damage: 1,
                isWingman: true,
            });
        }
    }
}
// ---------- 增益系统结束 ----------

function updateHUD() {
    scoreDisplay.textContent = game.score;
    livesDisplay.textContent = game.lives;
    levelDisplay.textContent = game.level;
}

// ===== 主循环 =====
function gameLoop() {
    if (!game.running) return;
    game.frame++;

    // 暂停状态：只绘制，不更新逻辑
    if (game.paused) {
        draw();                         // 保持画面
        if (game.running) {
            requestAnimationFrame(gameLoop);
        }
        return;
    }

    // 正常更新
    updateStars();
    updatePlayer(player);
    game.spawnCounter++;
    if (game.spawnCounter >= game.enemySpawnRate) {
        game.spawnCounter = 0;
        spawnEnemy();
        if (game.level >= 4 && Math.random() < 0.3) spawnEnemy();
        if (game.level >= 7 && Math.random() < 0.3) spawnEnemy();
    }
    if (game.autoFire || keys.space) fireBullet();
    fireWingmenBullets();
    updateBullets();
    updateEnemies();
    checkCollisions();
    updateParticles();
    draw();
    requestAnimationFrame(gameLoop);
}

function draw() {
    ctx.clearRect(0, 0, W, H);
    drawStars();
    ctx.strokeStyle = 'rgba(0, 247, 255, 0.03)';
    ctx.lineWidth = 1;
    for (let x = 0; x < W; x += 40) { ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, H); ctx.stroke(); }
    for (let y = 0; y < H; y += 40) { ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(W, y); ctx.stroke(); }
    drawParticles();
    for (const b of bullets) drawBullet(b);
    for (const e of enemies) drawEnemy(e);
    drawWingmen();
    if (player) drawPlayer(player);
    if (game.autoFire && game.running) {
        ctx.save();
        ctx.font = '12px sans-serif';
        ctx.textAlign = 'right';
        ctx.fillStyle = 'rgba(0, 247, 255, 0.6)';
        ctx.fillText('自动射击', W - 15, H - 15);
        ctx.restore();
    }
    // 暂停时显示标识
    if (game.paused) {
        ctx.save();
        ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
        ctx.fillRect(0, 0, W, H);
        ctx.font = 'bold 28px sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillStyle = '#ffd700';
        ctx.shadowColor = '#ffd700';
        ctx.shadowBlur = 20;
        ctx.fillText('⏸️ 选择增益中...', W / 2, H / 2);
        ctx.restore();
    }
}

// ===== 游戏控制 =====
function startGame() {
    game.running = true;
    game.over = false;
    game.paused = false;
    game.score = 0;
    game.lives = 3;
    game.level = 1;
    game.frame = 0;
    game.autoFire = false;
    game.lastShotFrame = 0;
    game.fireRate = 12;
    game.enemySpawnRate = 60;
    game.spawnCounter = 0;
    game.enemiesKilled = 0;
    bulletSizeBonus = 0;
    bulletSplit = false;
    wingmen = [];
    player = createPlayer();
    bullets = [];
    enemies = [];
    particles = [];
    initStars();
    updateHUD();
    startOverlay.classList.add('hidden');
    gameOverOverlay.classList.add('hidden');
    gameLoop();
}

function endGame() {
    game.running = false;
    game.over = true;
    game.paused = false;
    finalScore.textContent = game.score;
    gameOverOverlay.classList.remove('hidden');
}

// ===== 事件绑定 =====
document.addEventListener('keydown', (e) => {
    if (game.paused) { e.preventDefault(); return; }
    switch (e.key) {
        case 'ArrowLeft': case 'a': case 'A': keys.left = true; e.preventDefault(); break;
        case 'ArrowRight': case 'd': case 'D': keys.right = true; e.preventDefault(); break;
        case ' ': keys.space = true; e.preventDefault(); if (game.running) fireBullet(); break;
        case 'f': case 'F': game.autoFire = !game.autoFire; e.preventDefault(); break;
    }
});

document.addEventListener('keyup', (e) => {
    if (game.paused) { e.preventDefault(); return; }
    switch (e.key) {
        case 'ArrowLeft': case 'a': case 'A': keys.left = false; e.preventDefault(); break;
        case 'ArrowRight': case 'd': case 'D': keys.right = false; e.preventDefault(); break;
        case ' ': keys.space = false; e.preventDefault(); break;
    }
});

canvas.addEventListener('mousemove', (e) => {
    if (!game.running || !player || game.paused) return;
    const rect = canvas.getBoundingClientRect();
    const scale = canvas.width / rect.width;
    player.x = Math.max(24, Math.min(W - 24, (e.clientX - rect.left) * scale));
});

canvas.addEventListener('click', () => { if (game.running && !game.paused) fireBullet(); });
startBtn.addEventListener('click', startGame);
restartBtn.addEventListener('click', startGame);

// ===== 初始化 =====
initStars();

function menuAnimation() {
    if (!game.running) {
        updateStars();
        ctx.clearRect(0, 0, W, H);
        drawStars();
        requestAnimationFrame(menuAnimation);
    }
}
menuAnimation();
