'use strict'
// =============================================================
// bot_kakunin.js ── 2人以上が居ないと確かめられない物を、ボットで確かめる
//
//   docs/実機確認リスト.md の「2-a. ボットで確かめる物」21項目。
//
//   相手のサーバー: tests/botserver（このファイルが毎回 作って、終わったら消す）
//     jikki_1201（Paper 1.20.1・MOD 無し）を丸ごと写した物。
//     ★ 本物の jikki_1201 は【読むだけ】。jikki_mod には触らない。
//     ★ ボットはオフラインの名前で入るので、写しだけ online-mode=false にする。
//       そのかわり server-ip=127.0.0.1 にして、このパソコンの中からしか入れない。
//       RCON のパスワードは毎回その場で作り、写しごと消える（どこにも残さない）。
//     ★ 起動は WMI 経由（Claude の下から java を立てると NIO が死ぬ。記憶 env-gradle-java-nio-blocked）。
//
//   ボットは mineflayer。隣の置き場「アスレAI」のものを借りる（この置き場には入れない）。
//
//   使い方:  node tests/bot_kakunin.js [段 ...] [--nokosu]
//     段: nakama yumi koroshi gamen tp ryakudatsu saikidou（無ければ全部を この順で。
//         saikidou はサーバーを起動し直すので、必ず最後に置く）
//   ★ 写しに入れるプラグインとデータパックは【いまの元】（plugin.yml の版の jar と datapacks/jidai_craft）。
//     本番へ配る前に、ここで確かめられる。
//     --nokosu … 終わってもサーバーと写しを残す（調べ直す時だけ）
//   終了コード: 0=全部通った / 1=どこかで落ちた / 2=環境要因で実行不可
//
//   ★ 確かめ方の決まり
//     - 「起きない」ことを確かめる時は、同じ形で【起きる】対照も必ず取る。
//       （矢が外れただけなのに「通らなかった」と数えないため）
//     - 数字は実装から出す。ここに書いた期待値は Tatakai.UBAU_WARIAI(10%)・
//       略奪_割る数(100 → 1%) から計算した物。
// =============================================================

const path = require('path')
const fs = require('fs')
const net = require('net')
const crypto = require('crypto')
const { execFileSync } = require('child_process')

const NM = path.join(__dirname, '..', '..', 'アスレAI', 'bot', 'node_modules')
const mineflayer = require(path.join(NM, 'mineflayer'))
const { Vec3 } = require(path.join(NM, 'vec3'))

const MOTO = path.join(__dirname, '..', 'jikki_1201')   // 本物（読むだけ）
const SABA = path.join(__dirname, 'botserver')          // 写し（作って消す）
const HOST = '127.0.0.1'
// ★ 本物（25565）・他の検査（25601〜25642）と重ならない番号
const PORT = 25651
const RPORT = 25652
const RPW = crypto.randomBytes(12).toString('hex')

const sleep = ms => new Promise(r => setTimeout(r, ms))
class KankyouError extends Error {}

// その番号で誰かが待ち受けているか
function tsunagaru (port) {
  return new Promise(resolve => {
    const s = net.createConnection(port, HOST)
    s.setTimeout(1500)
    s.on('connect', () => { s.destroy(); resolve(true) })
    s.on('error', () => resolve(false))
    s.on('timeout', () => { s.destroy(); resolve(false) })
  })
}

// =============================================================
//  写しのサーバーを作る・立てる・止める・消す
// =============================================================
async function sabaWoTsukuru () {
  // ★ 動いている世界を写すと、書きかけのファイルを拾う。本物が動いていたら止める
  if (await tsunagaru(25565)) throw new KankyouError('ポート 25565 でサーバーが動いている（jikki_1201 か jikki_mod）。止めてからやり直すこと')
  if (await tsunagaru(PORT)) throw new KankyouError('ポート ' + PORT + ' を別のサーバーが使っている（前回の写しが残っているかもしれない）')
  if (!fs.existsSync(path.join(MOTO, 'server.properties'))) throw new KankyouError('写す元が無い: ' + MOTO)
  if (fs.existsSync(SABA)) fs.rmSync(SABA, { recursive: true, force: true })   // 前回の写し（このファイルが作った物）
  fs.cpSync(MOTO, SABA, { recursive: true, filter: s => !(path.basename(s) === 'logs' && path.dirname(s) === MOTO) })

  const kae = {
    'server-ip': HOST, 'server-port': String(PORT), 'query.port': String(PORT),
    'online-mode': 'false', 'enforce-secure-profile': 'false',
    'enable-rcon': 'true', 'rcon.port': String(RPORT), 'rcon.password': RPW,
    'max-players': '10', 'view-distance': '6', 'simulation-distance': '6', motd: 'bot kensa (copy of jikki_1201)'
  }
  const p = path.join(SABA, 'server.properties')
  const gyou = fs.readFileSync(p, 'utf8').split(/\r?\n/).filter(l => l !== '')
  const mita = new Set()
  const ato = gyou.map(l => { const k = l.split('=')[0]; if (k in kae) { mita.add(k); return k + '=' + kae[k] } return l })
  for (const k of Object.keys(kae)) if (!mita.has(k)) ato.push(k + '=' + kae[k])
  fs.writeFileSync(p, ato.join('\n') + '\n')

  // 起動の bat。java と jar は本物の start.bat と同じ物を使う（書き写さずに読む）
  const start = fs.readFileSync(path.join(MOTO, 'start.bat'), 'latin1')
  const java = (/"([^"]*java\.exe)"/i.exec(start) || [null, 'java'])[1]
  const jar = fs.readdirSync(MOTO).find(f => /^paper-.*\.jar$/.test(f))
  if (!jar) throw new KankyouError('Paper の jar が見つからない: ' + MOTO)
  fs.writeFileSync(path.join(SABA, 'kidou.bat'),
    '@echo off\r\ncd /d "%~dp0"\r\n"' + java + '" -Xms2G -Xmx4G -jar ' + jar + ' --nogui\r\n')

  // ★★ プラグインとデータパックは【いまの元】を入れる（本番へ配る前に確かめるため）★★
  //   プラグインは plugin.yml の版の jar（plugin/JidaiCraft-<版>.jar）。無ければ止める
  //   （古い jar のまま確かめて「通った」と言わないため）。
  const ban = (/^version:\s*(\S+)/m.exec(fs.readFileSync(path.join(__dirname, '..', 'plugin', 'src', 'main', 'resources', 'plugin.yml'), 'utf8')) || [])[1]
  const atarashii = path.join(__dirname, '..', 'plugin', 'JidaiCraft-' + ban + '.jar')
  if (!ban || !fs.existsSync(atarashii)) throw new KankyouError('いまの版の jar が無い: ' + atarashii + '（先に組むこと）')
  const pdir = path.join(SABA, 'plugins')
  for (const f of fs.readdirSync(pdir)) if (/^JidaiCraft-.*\.jar$/.test(f)) fs.rmSync(path.join(pdir, f))
  fs.copyFileSync(atarashii, path.join(pdir, path.basename(atarashii)))
  const dp = path.join(SABA, 'world', 'datapacks', 'jidai_craft')
  fs.rmSync(dp, { recursive: true, force: true })
  fs.cpSync(path.join(__dirname, '..', 'datapacks', 'jidai_craft'), dp, { recursive: true })
  console.log('写しを作った: ' + path.relative(path.join(__dirname, '..'), SABA) + '（' + jar + ' / ' +
    path.basename(atarashii) + ' / データパックは datapacks/jidai_craft から）')
}

async function sabaWoTateru () {
  const bat = path.join(SABA, 'kidou.bat')
  const out = path.join(SABA, 'server_out.txt')
  fs.writeFileSync(out, '')
  const cl = 'cmd /c "cd /d "' + SABA + '" && "' + bat + '" > "' + out + '" 2>&1"'
  const ps = "$r = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{ CommandLine = '" +
    cl.replace(/'/g, "''") + "' }; Write-Output $r.ReturnValue"
  const rv = execFileSync('powershell', ['-NoProfile', '-NonInteractive', '-Command', ps], { encoding: 'utf8' }).trim()
  if (rv !== '0') throw new KankyouError('WMI で起動できない（ReturnValue=' + rv + '）')
  for (let i = 0; i < 120; i++) {
    await sleep(2000)
    const t = fs.readFileSync(out, 'latin1')
    if (/Done \(/.test(t)) { console.log('サーバーが立った（' + (i + 1) * 2 + '秒）'); return }
    if (/FAILED TO BIND|Exception in thread "main"/.test(t)) throw new KankyouError('起動に失敗した（' + out + ' を見ること）')
  }
  throw new KankyouError('4分 待っても立たない')
}

async function setup () {
  await c('jidai setup')
  for (let i = 0; i < 40; i++) {
    await sleep(1000)
    if ((await score('#shisetsu', 'sagyou')) === 37) {
      await sleep(1500)
      console.log('拠点を建てた（施設 37）')
      return
    }
  }
  throw new KankyouError('jidai setup で施設が 37 そろわない')
}

async function sabaWoTomeru () {
  try { await c('stop') } catch (e) {}
  for (let i = 0; i < 60 && await tsunagaru(PORT); i++) await sleep(1000)
  await sleep(3000)
}

async function utsushiWoKesu () {
  for (let i = 0; i < 10; i++) {
    try { fs.rmSync(SABA, { recursive: true, force: true }); console.log('写しを消した'); return } catch (e) { await sleep(1500) }
  }
  console.log('★ 写しを消せなかった（まだ java が掴んでいるかもしれない）: ' + SABA)
}
const kekka = { ok: [], ng: [] }
function check (label, cond, detail) {
  (cond ? kekka.ok : kekka.ng).push(label)
  console.log('  [' + (cond ? 'PASS' : 'FAIL') + '] ' + label + (detail ? '  ' + detail : ''))
}
function kiroku (...a) { console.log('    ' + a.join(' ')) }

// =============================================================
//  RCON（1本の接続を使い回す。1回ずつ順番に打つ）
//
//  ★★ サーバーは【1回の受信で包みを1つしか読まない】（実測）★★
//    2つ続けて送ると、同じ受信に入った2つ目以降は黙って捨てられ、
//    返事が来ないまま止まる。だから【返事が来てから次を送る】。
//    「終わりの印」を後ろに付ける手（よくある書き方）は、ここでは使えない。
//  ★ 返事は 4096 バイトで分かれるが、ここで打つ物はどれも短い。
//    長い返事の時だけ、続きを少し待ってから返す。
// =============================================================
class Rcon {
  constructor (port, pw) { this.port = port; this.pw = pw; this.id = 10; this.machi = new Map(); this.kusari = Promise.resolve() }

  connect () {
    return new Promise((resolve, reject) => {
      this.sock = net.createConnection(this.port, HOST)
      this.buf = Buffer.alloc(0)
      this.sock.on('data', d => this._uketoru(d))
      this.sock.on('error', reject)
      this.sock.on('connect', () => {
        this.machi.set(1, { auth: true, resolve, reject })
        this.sock.write(Rcon.tsutsumu(1, 3, this.pw))
      })
    })
  }

  static tsutsumu (id, type, body) {
    const b = Buffer.from(body, 'utf8')
    const p = Buffer.alloc(14 + b.length)
    p.writeInt32LE(10 + b.length, 0); p.writeInt32LE(id, 4); p.writeInt32LE(type, 8)
    b.copy(p, 12)
    return p
  }

  _uketoru (d) {
    this.buf = Buffer.concat([this.buf, d])
    while (this.buf.length >= 4) {
      const ln = this.buf.readInt32LE(0)
      if (this.buf.length < 4 + ln) break
      const id = this.buf.readInt32LE(4)
      const body = this.buf.toString('utf8', 12, 4 + ln - 2)
      this.buf = this.buf.slice(4 + ln)
      if (id === -1) { for (const m of this.machi.values()) m.reject && m.reject(new Error('RCON の認証に失敗')); continue }
      const m = this.machi.get(id)
      if (!m) continue
      if (m.auth) { this.machi.delete(id); m.resolve(); continue }
      m.parts.push(body)
      clearTimeout(m.tokei)
      const kansei = () => { this.machi.delete(id); m.resolve(m.parts.join('')) }
      if (Buffer.byteLength(body, 'utf8') < 4000) kansei()
      else m.tokei = setTimeout(kansei, 200)
    }
  }

  command (cmd) {
    const p = this.kusari.then(() => new Promise((resolve, reject) => {
      const a = ++this.id
      const t = setTimeout(() => { this.machi.delete(a); reject(new Error('RCON の返事が来ない: ' + cmd)) }, 15000)
      this.machi.set(a, { parts: [], resolve: s => { clearTimeout(t); resolve(s) } })
      this.sock.write(Rcon.tsutsumu(a, 2, cmd))
    }))
    this.kusari = p.catch(() => {})
    return p
  }

  close () { try { this.sock.end() } catch (e) {} }
}

let R = null
const c = cmd => R.command(cmd)
async function score (h, o) {
  const m = /has (-?\d+)/.exec(await c('scoreboard players get ' + h + ' ' + o))
  return m ? Number(m[1]) : null
}
async function blockAru (x, y, z, b) {
  return /passed/.test(await c('execute if block ' + x + ' ' + y + ' ' + z + ' ' + b))
}

// =============================================================
//  ボット
// =============================================================
const BOTS = {}

// 文字の部品（JSON）から、見える文字だけを取り出す
function mojiDake (j) {
  if (j == null) return ''
  if (typeof j === 'string') { try { return mojiDake(JSON.parse(j)) } catch (e) { return j } }
  if (Array.isArray(j)) return j.map(mojiDake).join('')
  let s = (j.text || '') + (j.translate || '')
  if (j.extra) s += j.extra.map(mojiDake).join('')
  return s
}
function namae (item) { return item ? mojiDake(item.customName).replace(/§./g, '') : '' }
function kamiNoNamae (bot) {
  return bot.inventory.items().filter(i => i.name === 'paper').map(namae)
}

async function hairu (name) {
  const bot = mineflayer.createBot({ host: HOST, port: PORT, username: name, auth: 'offline', version: '1.20.1' })
  bot.kiroku = []
  bot.on('messagestr', (s, pos) => { if (pos !== 'game_info') bot.kiroku.push({ t: Date.now(), s }) })
  bot.on('title', (s, shu) => bot.kiroku.push({ t: Date.now(), s: '[' + shu + '] ' + s }))
  bot.on('kicked', r => kiroku(name, '追い出された:', JSON.stringify(r)))
  bot.on('error', e => kiroku(name, 'エラー:', e.message))
  await new Promise((resolve, reject) => {
    const t = setTimeout(() => reject(new Error(name + ' が入れない（60秒）')), 60000)
    bot.once('spawn', () => { clearTimeout(t); resolve() })
  })
  BOTS[name] = bot
  // ★ connection-throttle が 4000ms なので、次の人は 4.5秒 空けて入る
  await sleep(4500)
  return bot
}

// その時刻より後に、その文字を見たか
function mita (bot, re, t0) { return bot.kiroku.some(k => k.t >= t0 && re.test(k.s)) }
function miraretaMoji (bot, re, t0) { const k = bot.kiroku.find(k => k.t >= t0 && re.test(k.s)); return k ? k.s : null }
async function matsu (bot, re, t0, ms) {
  const owari = Date.now() + ms
  while (Date.now() < owari) { if (mita(bot, re, t0)) return true; await sleep(100) }
  return false
}

async function tp (name, x, y, z, yaw = 0, pitch = 0) {
  await c('tp ' + name + ' ' + x + ' ' + y + ' ' + z + ' ' + yaw + ' ' + pitch)
  await sleep(700)
}

async function motsu (bot, mono) {
  const it = bot.inventory.items().find(i => i.name === mono)
  if (!it) throw new Error(bot.username + ' は ' + mono + ' を持っていない')
  await bot.equip(it, 'hand')
  await sleep(300)
}

function mawari (bot, name) { const p = bot.players[name]; return p && p.entity }

async function kaifuku (bot) {
  // 平和なので 1秒に1ずつ戻る。満タンになるまで待つ（最大25秒）
  for (let i = 0; i < 250 && bot.health < 20; i++) await sleep(100)
}

// 画面を開く（ブロックを右クリック）
// ★ 待ち受けの約束（Promise）は、先に作ってから押す。押す側が失敗すると
//   約束が誰にも受け止められないまま時間切れになり、node ごと落ちる（実測）。
//   だから作った時点で「受け止め役」を付けておく（後で await すれば失敗はそこで拾える）。
async function hiraku (bot, x, y, z) {
  const w = new Promise((resolve, reject) => {
    const t = setTimeout(() => reject(new Error(bot.username + ' の画面が開かない')), 5000)
    bot.once('windowOpen', win => { clearTimeout(t); resolve(win) })
  })
  w.catch(() => {})
  await bot.activateBlock(bot.blockAt(new Vec3(x, y, z)))
  const win = await w
  win.kousin = 0
  win.on('updateSlot', () => { win.kousin++ })
  return win
}
function tsugiNoGamen (bot, ms = 3000) {
  const p = new Promise((resolve, reject) => {
    const t = setTimeout(() => reject(new Error(bot.username + ' の次の画面が来ない')), ms)
    bot.once('windowOpen', win => { clearTimeout(t); win.kousin = 0; win.on('updateSlot', () => { win.kousin++ }); resolve(win) })
  })
  p.catch(() => {})
  return p
}

// 特殊アイテムの紙を、プラグイン本来の出し方（jidai game tokushu）で持たせる。
// ★ tokushu はゲーム内からしか打てないので、その間だけ op にする。
async function kamiWoMotaseru (name, mei) {
  const bot = BOTS[name]
  await c('op ' + name)
  await sleep(500)
  const t0 = Date.now()
  bot.chat('/jidai game tokushu ' + mei)
  const ok = await matsu(bot, /特殊アイテムを 1 枚 出しました/, t0, 4000)
  await c('deop ' + name)
  await sleep(400)
  if (!ok) throw new Error(name + ' に ' + mei + ' を出せなかった')
}

// 壊した銀行が戻るまでの秒数（壊れた瞬間から数える）。1-7 の「3秒で戻る」と突き合わせる
const MODORI = []

async function horu (bot, x, y, z) {
  const b = bot.blockAt(new Vec3(x, y, z))
  if (!b || b.name !== 'gold_block') throw new Error('銀行の金ブロックが見えない: ' + (b && b.name))
  await motsu(bot, 'netherite_pickaxe')
  // ★ dig は「サーバーが空気にした」と知らせてきた時に終わる。そこが壊れた瞬間
  await Promise.race([bot.dig(b, true), sleep(6000).then(() => { throw new Error('掘り終わらない') })])
  const kowareta = Date.now()
  for (let i = 0; i < 120; i++) {
    if (await blockAru(x, y, z, 'minecraft:gold_block')) { MODORI.push((Date.now() - kowareta) / 1000); return }
    await sleep(40)
  }
  MODORI.push(null)
}

// =============================================================
//  段0: 準備
// =============================================================
const ARENA = { x0: 30, x1: 50, y: 199, z0: -937, z1: -917 }

async function junbi () {
  console.log('=== 準備 ===')
  // ★★ jikki_1201 の世界は難易度が「ノーマル」のまま（level.dat の Difficulty=2）★★
  //   server.properties の difficulty=peaceful は、今ある世界には効いていなかった（実測）。
  //   夜にゾンビが湧いて、販売所の前のボットが倒された。
  //   本番の jikki_mod の世界は Difficulty=0（平和）なので、写しもそれに合わせる。
  await c('difficulty peaceful')
  // 腐敗で石油が減ると数字が合わなくなるので、確かめる間だけ止める（写しの世界だけ）
  await c('scoreboard players set 腐敗_猶予秒 settei 99999')
  // 空中に囲いを作る（地形に左右されないように）。スポーンの区画なので常に読み込まれている
  await c(`fill ${ARENA.x0} ${ARENA.y} ${ARENA.z0} ${ARENA.x1} ${ARENA.y + 4} ${ARENA.z1} minecraft:barrier hollow`)
  await c('fill 55 199 -937 60 199 -932 minecraft:barrier')
  await c('gamerule doImmediateRespawn true')
  await c('gamerule sendCommandFeedback false')
  for (const n of ['Kyu1', 'Kyu2', 'Shin1', 'Shin2']) {
    await hairu(n)
    kiroku(n, 'が入った')
  }
  for (const [n, t] of [['Kyu1', 'kyuryo'], ['Kyu2', 'kyuryo'], ['Shin1', 'shinrin'], ['Shin2', 'shinrin']]) {
    await c('team join ' + t + ' ' + n)
    await c('gamemode survival ' + n)
    await c('scoreboard players set ' + n + ' kane_kojin 1000')
    await c('scoreboard players set ' + n + ' sekiyu 0')
  }
  await c('scoreboard players set 丘陵 chokin 5000')
  await c('scoreboard players set 森林 chokin 10000')
  await sleep(1500)
  const n = /There are (\d+)/.exec(await c('list'))
  check('(準備) 4体とも入った', n && Number(n[1]) === 4, n && n[0])
}

async function arenaNi (name, x, z, yaw = -90) {
  await tp(name, x + 0.5, ARENA.y + 1, z + 0.5, yaw, 0)
}

// =============================================================
//  段1: 同じ勢力の仲間を剣で殴れない
// =============================================================
async function nakama () {
  console.log('=== 同士討ちの禁止（剣）===')
  const A = BOTS.Kyu1; const B = BOTS.Kyu2; const C = BOTS.Shin1
  await c('clear Kyu1')
  await c('give Kyu1 minecraft:iron_sword')
  await arenaNi('Kyu1', 35, -927)
  await arenaNi('Kyu2', 37, -927, 90)
  await arenaNi('Shin1', 40, -922, 90)
  await sleep(3500) // 入ってすぐ・飛んだすぐの無敵（3秒）を待つ
  await motsu(A, 'iron_sword')
  await sleep(1000)
  await kaifuku(B)

  const t0 = Date.now()
  for (let i = 0; i < 3; i++) {
    const e = mawari(A, 'Kyu2')
    await A.lookAt(e.position.offset(0, 1.5, 0), true)
    A.attack(e)
    await sleep(900)
  }
  await sleep(300)
  kiroku('Kyu2 の体力:', B.health, ' Kyu1 が見た文:', JSON.stringify(A.kiroku.filter(k => k.t >= t0).map(k => k.s)))
  check('同じ勢力の仲間を剣で殴れない（体力が 20 のまま）', B.health === 20, 'Kyu2 体力=' + B.health)
  // ★ 文は出さないで確定（2026-09-28 のご判断）。
  //   データパックのチームの friendlyFire=false を、サーバー本体が先に見て黙って止める。
  //   文が出たら、止め方が変わった（チームの判定を通り抜けている）ということ。
  const mongon = mita(A, /同じ勢力の仲間は傷つけられません/, t0)
  check('殴った側に文は出ない（チームの設定が先に黙って止める。2026-09-28 に確定）', !mongon,
    mongon ? '出た（チームの判定を通り抜けている）' : '')

  // ── 2枚目の門: チームの設定を外しても、プラグイン（Tatakai.fusegu）が止める ──
  //   MOD の武器などがチームの判定を通り抜けた時の備え。その間だけ friendlyFire を true にする。
  //   ★ すぐ false に戻す。
  await c('team modify kyuryo friendlyFire true')
  await sleep(300)
  const t1 = Date.now()
  const e1 = mawari(A, 'Kyu2')
  await A.lookAt(e1.position.offset(0, 1.5, 0), true)
  A.attack(e1)
  await sleep(700)
  await c('team modify kyuryo friendlyFire false')
  const deta = mita(A, /同じ勢力の仲間は傷つけられません/, t1)
  kiroku('(2枚目の門) friendlyFire を true にした間: 文', deta ? 'が出た' : 'は出ない', '/ Kyu2 の体力', B.health)
  check('(2枚目の門) チームの設定を外しても、プラグインが止める（体力 20 のまま・この時だけ文が出る）',
    deta && B.health === 20, 'Kyu2 体力=' + B.health)

  // ── 対照: 別の勢力の人は殴れる ──
  await arenaNi('Kyu2', 33, -922)
  await arenaNi('Shin1', 37, -927, 90)
  await sleep(3500)
  await kaifuku(C)
  const mae = C.health
  const e = mawari(A, 'Shin1')
  await A.lookAt(e.position.offset(0, 1.5, 0), true)
  A.attack(e)
  await sleep(500)
  kiroku('Shin1 の体力:', mae, '→', C.health)
  check('別の勢力の人は殴れる（対照）', C.health < mae, 'Shin1 体力 ' + mae + ' → ' + C.health)
}

// =============================================================
//  段2: 同じ勢力の仲間に弓の矢を当てても通らない
// =============================================================
async function iru (A, name) {
  const e = mawari(A, name)
  await A.lookAt(e.position.offset(0, 1.2, 0), true)
  await motsu(A, 'bow')
  A.activateItem()
  await sleep(1200)
  await A.lookAt(mawari(A, name).position.offset(0, 1.2, 0), true)
  A.deactivateItem()
  await sleep(1500)
}

async function yumi () {
  console.log('=== 同士討ちの禁止（弓）===')
  const A = BOTS.Kyu1; const B = BOTS.Kyu2; const C = BOTS.Shin1
  await c('give Kyu1 minecraft:bow')
  await c('give Kyu1 minecraft:arrow 64')
  await arenaNi('Kyu1', 34, -927)
  await arenaNi('Kyu2', 41, -927, 90)
  await arenaNi('Shin1', 46, -920, 90)
  await sleep(3500)
  await kaifuku(B)
  const ya0 = A.inventory.items().filter(i => i.name === 'arrow').reduce((a, i) => a + i.count, 0)
  const t0 = Date.now()
  await iru(A, 'Kyu2')
  const ya1 = A.inventory.items().filter(i => i.name === 'arrow').reduce((a, i) => a + i.count, 0)
  kiroku('矢', ya0, '→', ya1, ' Kyu2 の体力:', B.health)
  check('(弓) 矢を1本 放った', ya1 === ya0 - 1, ya0 + ' → ' + ya1)
  check('同じ勢力の仲間に弓の矢を当てても通らない（体力が 20 のまま）', B.health === 20, 'Kyu2 体力=' + B.health)
  kiroku('Kyu1 が見た文:', JSON.stringify(A.kiroku.filter(k => k.t >= t0).map(k => k.s)))

  // ── 対照: 同じ場所に別の勢力の人を立たせて、同じように射る ──
  await arenaNi('Kyu2', 46, -934, 90)
  await arenaNi('Shin1', 41, -927, 90)
  await sleep(3500)
  await kaifuku(C)
  const mae = C.health
  await iru(A, 'Shin1')
  kiroku('Shin1 の体力:', mae, '→', C.health)
  check('(弓) 対照: 同じ射ち方で別の勢力の人には当たる', C.health < mae, 'Shin1 体力 ' + mae + ' → ' + C.health)
}

// =============================================================
//  段3: 倒すと個人の金の 10% を奪う
// =============================================================
async function taosu (A, name) {
  const e = mawari(A, name)
  await A.lookAt(e.position.offset(0, 1.5, 0), true)
  A.attack(e)
  await sleep(1500)
}

async function koroshi () {
  console.log('=== 倒すと 10% 奪う ===')
  const A = BOTS.Kyu1; const C = BOTS.Shin1; const B = BOTS.Kyu2; const D = BOTS.Shin2
  await c('give Kyu1 minecraft:netherite_sword{Enchantments:[{id:"minecraft:sharpness",lvl:100s}]}')
  await arenaNi('Kyu1', 35, -927)
  await arenaNi('Shin1', 37, -927, 90)
  await arenaNi('Kyu2', 33, -920)
  await arenaNi('Shin2', 45, -920, 90)
  await sleep(3500)
  await motsu(A, 'netherite_sword')
  await sleep(1000)

  // (1) ふつうに倒す
  await c('scoreboard players set Kyu1 kane_kojin 500')
  await c('scoreboard players set Shin1 kane_kojin 1000')
  const kyu0 = await score('丘陵', 'chokin'); const shin0 = await score('森林', 'chokin')
  let t0 = Date.now()
  await taosu(A, 'Shin1')
  await sleep(1000)
  const a1 = await score('Kyu1', 'kane_kojin'); const c1 = await score('Shin1', 'kane_kojin')
  const mA = miraretaMoji(A, /を倒して/, t0); const mC = miraretaMoji(C, /を奪われた/, t0)
  kiroku('Kyu1:', mA, ' / Shin1:', mC)
  check('敵を倒すと「◯◯ を倒して N を奪った」が出て、個人の金が増える',
    !!mA && /Shin1 を倒して 100 を奪った/.test(mA) && a1 === 600, 'Kyu1 の金 500 → ' + a1)
  check('倒された側に「◯◯ に N を奪われた」が出て、個人の金が減る',
    !!mC && /Kyu1 に 100 を奪われた/.test(mC) && c1 === 900, 'Shin1 の金 1000 → ' + c1)
  const kyu1 = await score('丘陵', 'chokin'); const shin1 = await score('森林', 'chokin')
  check('勢力の貯金は1円も減らない（どちらの勢力も）', kyu1 === kyu0 && shin1 === shin0,
    '丘陵 ' + kyu0 + '→' + kyu1 + ' / 森林 ' + shin0 + '→' + shin1)
  const moreta = mita(B, /を倒して|を奪われた/, t0) || mita(D, /を倒して|を奪われた/, t0)
  check('知らせが全体チャットに流れない（当人2人だけ）', !moreta,
    'Kyu2 と Shin2 が見た文: ' + JSON.stringify(B.kiroku.concat(D.kiroku).filter(k => k.t >= t0).map(k => k.s)))

  // (2) 9円以下の相手を倒しても何も起きない
  await sleep(1500)
  await arenaNi('Shin1', 37, -927, 90)
  await sleep(3500)
  await c('scoreboard players set Shin1 kane_kojin 9')
  t0 = Date.now()
  await taosu(A, 'Shin1')
  await sleep(1000)
  const a2 = await score('Kyu1', 'kane_kojin'); const c2 = await score('Shin1', 'kane_kojin')
  const shinda2 = mita(C, /Kyu1|slain|倒/, t0)
  check('(9円) 倒せてはいる（対照）', shinda2, JSON.stringify(C.kiroku.filter(k => k.t >= t0).map(k => k.s)))
  check('9円以下の相手を倒しても何も起きない（10% が 0 のため）',
    a2 === 600 && c2 === 9 && !mita(A, /を倒して/, t0) && !mita(C, /を奪われた/, t0),
    'Kyu1 ' + a2 + ' / Shin1 ' + c2)

  // (3) 自分で死んだ（落ちた）時は誰も奪わない
  await sleep(1500)
  await c('scoreboard players set Shin1 kane_kojin 1000')
  t0 = Date.now()
  await tp('Shin1', 57.5, 240, -934.5)
  await matsu(C, /fell|落ち|hit the ground/, t0, 8000)
  await sleep(1500)
  const a3 = await score('Kyu1', 'kane_kojin'); const c3 = await score('Shin1', 'kane_kojin')
  kiroku('Shin1 が見た文:', JSON.stringify(C.kiroku.filter(k => k.t >= t0).map(k => k.s)))
  check('(落下) 落ちて死んだ（対照）', mita(C, /fell|hit the ground|落/, t0))
  check('自分で死んだ時は誰も奪わない', a3 === 600 && c3 === 1000 && !mita(C, /を奪われた/, t0),
    'Kyu1 ' + a3 + ' / Shin1 ' + c3)
}

// =============================================================
//  段4: 2人が同時に画面を開く
// =============================================================
const GACHA = [7, 101, -404]
const MISE = [4, 101, -404]

async function gamen () {
  console.log('=== 2人が同時に居る時（画面）===')
  const A = BOTS.Kyu1; const B = BOTS.Kyu2; const C = BOTS.Shin1; const D = BOTS.Shin2
  await c('scoreboard players set Kyu1 kane_kojin 1000')
  await c('scoreboard players set Kyu2 kane_kojin 1000')
  // ★ 手に何か持っていると、押した時に置いてしまう物がある。剣を持たせる
  await motsu(A, 'iron_sword')
  await tp('Kyu1', 6.5, 101, -402.5, 180, 30)
  await tp('Kyu2', 8.5, 101, -402.5, 180, 30)
  await sleep(1500)

  // ── ガチャ ──
  const wA = await hiraku(A, ...GACHA)
  const wB = await hiraku(B, ...GACHA)
  await sleep(1000)
  wA.kousin = 0; wB.kousin = 0
  const bMae = wB.slots.slice(0, 18).map(namae).join('|')
  await A.clickWindow(4, 0, 0)
  await sleep(5000)
  kiroku('ガチャ: Kyu1 の画面の書き換え', wA.kousin, '回 / Kyu2 の画面', wB.kousin, '回')
  const bAto = wB.slots.slice(0, 18).map(namae).join('|')
  check('(ガチャ) 回した本人の画面は動いた（対照）', wA.kousin >= 5, wA.kousin + ' 回')
  check('2人が同時にガチャを開き、片方の演出がもう片方の画面に流れない',
    wB.kousin === 0 && bAto === bMae && B.currentWindow && B.currentWindow.id === wB.id,
    'Kyu2 の画面の書き換え ' + wB.kousin + ' 回')
  // 逆も
  wA.kousin = 0; wB.kousin = 0
  await B.clickWindow(4, 0, 0)
  await sleep(5000)
  check('(ガチャ) 逆向き: Kyu2 が回しても Kyu1 の画面は動かない', wA.kousin === 0 && wB.kousin >= 5,
    'Kyu1 ' + wA.kousin + ' 回 / Kyu2 ' + wB.kousin + ' 回')
  A.closeWindow(wA); B.closeWindow(wB)
  await sleep(800)

  // ── 販売所 ──
  await tp('Kyu1', 4.5, 101, -402.5, 180, 30)
  await tp('Kyu2', 3.5, 101, -402.5, 180, 30)
  await sleep(1000)
  const sA = await hiraku(A, ...MISE)
  const sB = await hiraku(B, ...MISE)
  await sleep(800)
  sB.kousin = 0
  const tabB0 = [namae(sB.slots[0]), namae(sB.slots[1]), namae(sB.slots[2])]
  const tsugi = tsugiNoGamen(A)
  await A.clickWindow(1, 0, 0) // 「防具」のタブ
  const sA2 = await tsugi
  await sleep(800)
  const tabA = [namae(sA2.slots[0]), namae(sA2.slots[1]), namae(sA2.slots[2])]
  const tabB = [namae(sB.slots[0]), namae(sB.slots[1]), namae(sB.slots[2])]
  kiroku('Kyu1 のタブ:', JSON.stringify(tabA), ' Kyu2 のタブ:', JSON.stringify(tabB))
  check('(販売所) 押した本人は「防具」に切り替わった（対照）', /▶ 防具/.test(tabA[1]), JSON.stringify(tabA))
  check('2人が同時に販売所を開き、タブを切り替えても相手の画面が変わらない',
    sB.kousin === 0 && JSON.stringify(tabB) === JSON.stringify(tabB0) && /▶ 生活/.test(tabB[0]) &&
    B.currentWindow && B.currentWindow.id === sB.id, JSON.stringify(tabB))
  // 相手がタブを変えたあとでも、自分の買い物は自分のページで通る（パン＝生活の枠9・個人の金）
  let t0 = Date.now()
  await B.clickWindow(9, 0, 0)
  await sleep(1000)
  check('(販売所) 相手がタブを変えたあとも、自分のページの品（パン）が買える',
    mita(B, /パン ×3 を購入/, t0), miraretaMoji(B, /購入|ありません|足りません/, t0))

  // ── 勢力の金で買った時の知らせ ──
  const tsugi2 = tsugiNoGamen(A)
  await A.clickWindow(2, 0, 0) // 「武器」のタブ
  const sA3 = await tsugi2
  await sleep(800)
  kiroku('武器のタブの枠9:', namae(sA3.slots[9]))
  const kin0 = await score('丘陵', 'chokin')
  t0 = Date.now()
  await A.clickWindow(9, 0, 0) // 鉄の剣（勢力の金 100）
  await sleep(1500)
  const kin1 = await score('丘陵', 'chokin')
  const re = /Kyu1 が 勢力の金 で 鉄の剣 ×1 を購入/
  kiroku('Kyu1:', miraretaMoji(A, re, t0), ' / Kyu2:', miraretaMoji(B, re, t0))
  check('勢力の金で買った時、勢力の全員に知らせが届く', mita(A, re, t0) && mita(B, re, t0) && kin1 === kin0 - 100,
    '丘陵 ' + kin0 + ' → ' + kin1)
  check('勢力の金で買った時、他の勢力には届かない', !mita(C, /勢力の金 で/, t0) && !mita(D, /勢力の金 で/, t0))
  A.closeWindow(A.currentWindow); B.closeWindow(B.currentWindow)
  await sleep(500)
}

// =============================================================
//  段5: tp kyoten と risu
// =============================================================
const KYOTEN = { kyuryo: [0.5, 101, -401.5], shinrin: [389.5, 101, -118.5] }
function chikai (bot, p, haba = 1.5) {
  const q = bot.entity.position
  return Math.abs(q.x - p[0]) <= haba && Math.abs(q.z - p[2]) <= haba && Math.abs(q.y - p[1]) <= 3
}
function ichi (bot) { const q = bot.entity.position; return q.x.toFixed(1) + ',' + q.y.toFixed(1) + ',' + q.z.toFixed(1) }

async function tpKyoten () {
  console.log('=== tp kyoten / risu ===')
  for (const n of ['Kyu1', 'Kyu2', 'Shin1', 'Shin2']) await arenaNi(n, 32 + Object.keys(BOTS).indexOf(n) * 3, -930)
  await sleep(1000)
  const out = await c('jidai game tp kyoten')
  await sleep(1500)
  kiroku('返事:', out.trim())
  const ok = chikai(BOTS.Kyu1, KYOTEN.kyuryo) && chikai(BOTS.Kyu2, KYOTEN.kyuryo) &&
    chikai(BOTS.Shin1, KYOTEN.shinrin) && chikai(BOTS.Shin2, KYOTEN.shinrin)
  check('jidai game tp kyoten で勢力の全員が自分の拠点に飛ぶ', ok,
    ['Kyu1', 'Kyu2', 'Shin1', 'Shin2'].map(n => n + '=' + ichi(BOTS[n])).join(' '))

  // risu のあと、後から入った人も死ねば拠点に戻る
  const out2 = await c('jidai game risu')
  kiroku('返事:', out2.trim())
  const E = await hairu('Kyu3')
  await c('team join kyuryo Kyu3')
  await c('gamemode survival Kyu3')
  await sleep(1500)
  kiroku('Kyu3 が入った場所:', ichi(E))
  const t0 = Date.now()
  await c('kill Kyu3')
  await matsu(E, /Kyu3/, t0, 5000)
  await sleep(2500)
  kiroku('Kyu3 が生き返った場所:', ichi(E))
  check('jidai game risu のあと、後から入った人も死ぬと拠点に戻る', chikai(E, KYOTEN.kyuryo), ichi(E))
}

// =============================================================
//  段6: 略奪で相手が居る時（案D を含む）
// =============================================================
const GINKO_SHINRIN = [390, 101, -121]

async function ryakuMae () {
  return {
    kyu: await score('丘陵', 'chokin'), shin: await score('森林', 'chokin'),
    kyu1s: await score('Kyu1', 'sekiyu'), shin1s: await score('Shin1', 'sekiyu'), shin2s: await score('Shin2', 'sekiyu')
  }
}

async function ryakudatsu () {
  console.log('=== 略奪で相手が居る時 ===')
  const A = BOTS.Kyu1; const C = BOTS.Shin1
  // 戦争の支度（写しの世界だけ）: 準備5秒・交戦は長め・間隔1秒
  await c('scoreboard players set 戦争_準備秒 settei 5')
  await c('scoreboard players set 戦争_交戦秒 settei 3000')
  await c('scoreboard players set 略奪_間隔秒 settei 1')
  await c('scoreboard players set 丘陵 chokin 5000')
  await c('scoreboard players set 森林 chokin 10000')
  await c('scoreboard players set Kyu1 sekiyu 0')
  await c('scoreboard players set Shin1 sekiyu 300')
  await c('scoreboard players set Shin2 sekiyu 100')
  await c('jidai leader set shinrin Shin1')
  await c('jidai leader set kyuryo Kyu1')
  await c('scoreboard players set #w_kuni sagyou 1')
  await c('scoreboard players set #w_aite sagyou 2')
  await c('scoreboard players set #w_soku sagyou 0')
  await c('function jidai:sensou/sensen')
  await sleep(8000)
  const jotai = await score('丘陵', 'sensou')
  check('(略奪) 丘陵と森林が交戦中になった', jotai === 2, '丘陵 sensou=' + jotai)

  await c('clear Kyu1')
  await c('give Kyu1 minecraft:netherite_pickaxe{Enchantments:[{id:"minecraft:efficiency",lvl:5s}]}')
  await tp('Kyu1', 389.5, 101, -118.5, 180, 30)
  await tp('Shin1', 393.5, 101, -116.5, 180, 0)
  await tp('Shin2', 386.5, 101, -116.5, 180, 0)
  await sleep(2000)
  await kamiWoMotaseru('Shin1', '古びた護符')

  // ── (1) 相手が居る: 金と石油が1%ずつ・リーダーの紙が1枚 ──
  let m = await ryakuMae()
  let t0 = Date.now()
  await horu(A, ...GINKO_SHINRIN)
  await sleep(1500)
  let a = await ryakuMae()
  kiroku('前', JSON.stringify(m), '後', JSON.stringify(a))
  kiroku('Kyu1 が見た文:', JSON.stringify(A.kiroku.filter(k => k.t >= t0).map(k => k.s)))
  // 石油の合計 400（Shin1 300 + Shin2 100）の 1% = 4 本。いちばん多い Shin1 から
  check('相手の勢力の誰かがオンラインなら、石油も 1% 動く',
    a.shin1s === m.shin1s - 4 && a.kyu1s === m.kyu1s + 4,
    'Shin1 ' + m.shin1s + '→' + a.shin1s + ' / Kyu1 ' + m.kyu1s + '→' + a.kyu1s)
  check('(略奪) 金も 1% 動く（10000 → 9900）', a.shin === m.shin - 100 && a.kyu === m.kyu + 100,
    '森林 ' + m.shin + '→' + a.shin)
  const zenin = ['Kyu1', 'Kyu2', 'Shin1', 'Shin2', 'Kyu3'].filter(n => BOTS[n])
  const reD = /Kyu1 が 森林 のリーダーから「古びた護符」を奪った/
  check('案D: 略奪が成立すると、相手のリーダーの「集める5種」の紙が1枚 こちらへ移る',
    kamiNoNamae(A).some(s => s.includes('古びた護符')) && !kamiNoNamae(C).some(s => s.includes('古びた護符')),
    'Kyu1 の紙 ' + JSON.stringify(kamiNoNamae(A)) + ' / Shin1 の紙 ' + JSON.stringify(kamiNoNamae(C)))
  check('案D: 全員に「○○ が △△ のリーダーから「□□」を奪った」', zenin.every(n => mita(BOTS[n], reD, t0)),
    zenin.map(n => n + '=' + mita(BOTS[n], reD, t0)).join(' '))
  // ★ 1-7 の項目も、ここで見られる分だけ見ておく（落とし物が出ないこと）
  const ochita = /passed/.test(await c('execute if entity @e[type=item,x=390,y=101,z=-121,distance=..6,nbt={Item:{id:"minecraft:gold_block"}}]'))
  check('(1-7) 壊した金ブロックは落ちてこない', !ochita && !A.inventory.items().some(i => i.name === 'gold_block'))

  // ── (2) 遺物の紙は奪えない ──
  await sleep(1500)
  await kamiWoMotaseru('Shin1', 'ロスチャイルドの金庫')
  m = await ryakuMae()
  t0 = Date.now()
  await horu(A, ...GINKO_SHINRIN)
  await sleep(1500)
  a = await ryakuMae()
  check('案D: 遺物の紙は奪えない（金と石油は動くが、紙は動かない）',
    a.shin < m.shin && a.shin1s < m.shin1s && kamiNoNamae(C).some(s => s.includes('ロスチャイルドの金庫')) &&
    !kamiNoNamae(A).some(s => s.includes('金庫')) && !mita(A, /のリーダーから「/, t0),
    '森林 ' + m.shin + '→' + a.shin + ' / Shin1 の紙 ' + JSON.stringify(kamiNoNamae(C)))

  // ── (3) 間隔の最中で断られた略奪では、紙も動かない ──
  await c('scoreboard players set 略奪_間隔秒 settei 10')
  await sleep(1500)
  await kamiWoMotaseru('Shin1', '聖杯の欠片')
  await kamiWoMotaseru('Shin1', '羊皮紙の海図')
  t0 = Date.now()
  await horu(A, ...GINKO_SHINRIN) // 1回目は通る（1枚 移る）
  await sleep(1500)
  const kami1 = kamiNoNamae(C).filter(s => /聖杯|海図/.test(s))
  m = await ryakuMae()
  const t1 = Date.now()
  await horu(A, ...GINKO_SHINRIN) // 2回目は間隔の最中
  await sleep(1500)
  a = await ryakuMae()
  const kami2 = kamiNoNamae(C).filter(s => /聖杯|海図/.test(s))
  kiroku('Kyu1 が見た文（2回目）:', JSON.stringify(A.kiroku.filter(k => k.t >= t1).map(k => k.s)))
  check('(間隔) 2回目は「まだ手が離せない」で断られた（対照）', mita(A, /まだ手が離せない/, t1))
  check('案D: 間隔の最中で断られた略奪では、紙も動かない',
    kami1.length === 1 && kami2.length === 1 && a.shin === m.shin && !mita(A, /のリーダーから「/, t1),
    'Shin1 の紙（1回目のあと）' + JSON.stringify(kami1) + ' → ' + JSON.stringify(kami2) + ' / 森林 ' + m.shin + '→' + a.shin)
  await c('scoreboard players set 略奪_間隔秒 settei 1')
  await sleep(11000)

  // ── (4) 壊した人の持ち物が満杯 ──
  const aki = A.inventory.emptySlotCount()
  await c('give Kyu1 minecraft:dirt ' + aki * 64)
  await sleep(1000)
  kiroku('Kyu1 の空き枠:', aki, '→', A.inventory.emptySlotCount())
  const kamiC0 = kamiNoNamae(C).filter(s => /聖杯|海図/.test(s))
  m = await ryakuMae()
  t0 = Date.now()
  await horu(A, ...GINKO_SHINRIN)
  await sleep(1500)
  a = await ryakuMae()
  const kamiC1 = kamiNoNamae(C).filter(s => /聖杯|海図/.test(s))
  check('案D: 壊した人の持ち物が満杯だと「持ち物がいっぱいで、特殊アイテムを奪えなかった」。リーダーの紙は減らない',
    A.inventory.emptySlotCount() === 0 && mita(A, /持ち物がいっぱいで、特殊アイテムを奪えなかった/, t0) &&
    kamiC1.length === kamiC0.length && kamiC1.length === 1 && a.shin < m.shin,
    'Shin1 の紙 ' + JSON.stringify(kamiC0) + ' → ' + JSON.stringify(kamiC1) + ' / 森林 ' + m.shin + '→' + a.shin)
  await c('clear Kyu1 minecraft:dirt')
  await sleep(1500)

  // ── (5) 相手のリーダーが落ちている ──
  C.quit()
  delete BOTS.Shin1
  await sleep(2000)
  m = await ryakuMae()
  const gokei = await score('森林', 'sekiyu_gokei')
  t0 = Date.now()
  await horu(A, ...GINKO_SHINRIN)
  await sleep(1500)
  a = await ryakuMae()
  kiroku('森林の石油の合計（オンラインの人だけ）:', gokei, ' Shin2', m.shin2s, '→', a.shin2s)
  check('案D: 相手のリーダーが落ちている時は、金と石油だけが動く',
    a.shin < m.shin && a.shin2s < m.shin2s && a.kyu1s > m.kyu1s && !mita(A, /のリーダーから「/, t0),
    '森林 ' + m.shin + '→' + a.shin + ' / 石油 Shin2 ' + m.shin2s + '→' + a.shin2s)
  const C2 = await hairu('Shin1')
  await sleep(1500)
  const nokori = kamiNoNamae(C2).filter(s => /聖杯|海図/.test(s))
  check('(落ちている) 入り直したリーダーの紙は減っていない', nokori.length === 1, JSON.stringify(kamiNoNamae(C2)))

  // ── (6) 奪った紙で勝てる ──
  await tp('Shin1', 393.5, 101, -116.5, 180, 0)
  const motteru = kamiNoNamae(A)
  for (const mei of ['古びた護符', '聖杯の欠片', '羊皮紙の海図', '蒸気機関の歯車', '月の石']) {
    if (!motteru.some(s => s.includes(mei)) && !nokori.some(s => s.includes(mei))) await kamiWoMotaseru('Kyu1', mei)
  }
  kiroku('Kyu1（丘陵のリーダー）の紙:', JSON.stringify(kamiNoNamae(A)), ' Shin1:', JSON.stringify(kamiNoNamae(C2)))
  const shouri0 = await score('世界', 'shouri')
  await sleep(1500)
  t0 = Date.now()
  await horu(A, ...GINKO_SHINRIN)
  await matsu(A, /超特殊勝利/, t0, 5000)
  await sleep(1500)
  const shouri1 = await score('世界', 'shouri')
  const zenin2 = ['Kyu1', 'Kyu2', 'Shin1', 'Shin2', 'Kyu3'].filter(n => BOTS[n])
  kiroku('世界 shouri:', shouri0, '→', shouri1)
  check('案D: 奪った紙で勝てる（奪った側のリーダーが5種そろえば超特殊勝利）',
    shouri0 === 0 && shouri1 === 1 && zenin2.every(n => mita(BOTS[n], /超特殊勝利/, t0)),
    zenin2.map(n => n + '=' + mita(BOTS[n], /超特殊勝利/, t0)).join(' '))

  // ── (1-7) 壊した銀行は何秒で戻ったか ──
  //   プラグインは 60 tick（3秒）後に戻す約束をする（JidaiCraft.GINKO_MODORU）。
  //   ★ データパックの jidai:clock も、毎秒「銀行の印の所に金ブロックが無ければ置く」をしている。
  //     どちらが先に戻すかを、壊れた瞬間から測る。
  const bs = MODORI.filter(s => s != null)
  kiroku('壊した銀行が戻るまで（秒）:', MODORI.map(s => s == null ? '戻らない' : s.toFixed(2)).join(' / '))
  check('(1-7) 壊した銀行が 3秒で戻る（GINKO_MODORU = 60 tick）',
    bs.length === MODORI.length && bs.length > 0 && bs.every(s => s >= 2.5 && s <= 3.6),
    '実測 ' + bs.length + ' 回: ' + Math.min(...bs).toFixed(2) + '〜' + Math.max(...bs).toFixed(2) + ' 秒')
  const nokoru = /passed/.test(await c('execute if entity @e[type=marker,tag=jidai_ginko_modori]'))
  check('(1-7) 戻したあと、戻し待ちの印（jidai_ginko_modori）は残っていない', !nokoru)
}

// =============================================================
//  段7: 落ちた時の保険（起動し直すと、残った「戻し待ち」の印が外れて銀行が戻る）
//
//  ★ 本当に落とす（java を殺す）と世界が壊れかねないので、
//    「落ちて印だけが残った」状態を手で作ってから、ふつうに止めて起動し直す。
//    ふつうに止めても、プラグインが知らない印は onDisable では外れない。
//    外すのは起動の5秒後の jidoScan だけ ── そこを確かめる。
// =============================================================
async function saikidou () {
  console.log('=== 落ちた時の保険（起動し直す）===')
  for (const b of Object.values(BOTS)) { try { b.quit() } catch (e) {} }
  for (const k of Object.keys(BOTS)) delete BOTS[k]
  await sleep(1500)
  // 1回目の起動では印は1つも無かった。その時に印外しの命令が黙っていたか（失敗を出していないか）
  const shippai = () => fs.readFileSync(path.join(SABA, 'logs', 'latest.log')).toString('latin1')
    .split(/\r?\n/).filter(l => /No entity was found|Unknown or incomplete command|Incorrect argument/.test(l))
  const s1 = shippai()
  check('(保険) 印が無い時、起動時の印外しはコンソールに何も言わない', s1.length === 0, JSON.stringify(s1.slice(0, 3)))
  // ★ 印はブロックの底の真ん中に立っている（setup/kyoten の positioned が整数を真ん中へずらす）
  kiroku('拠点1の銀行の印の位置:', (await c('data get entity @e[type=marker,tag=jidai_ginko,limit=1,sort=nearest,x=0.5,y=101.5,z=-403.5] Pos')).trim())
  await c('tag @e[type=marker,tag=jidai_ginko,x=0.5,y=101.5,z=-403.5,distance=..1] add jidai_ginko_modori')
  const tsuita = /count: 1\b/i.test(await c('execute if entity @e[type=marker,tag=jidai_ginko_modori]'))
  check('(保険) 印を1つだけ付けられた（手で「落ちた」状態を作る）', tsuita)
  await c('setblock 0 101 -404 minecraft:air replace')
  await sleep(2500)
  check('(保険) 印が残っている間は、データパックも銀行を直さない', await blockAru(0, 101, -404, 'minecraft:air'))
  await sabaWoTomeru()
  R.close()
  await sabaWoTateru()
  R = new Rcon(RPORT, RPW)
  for (let i = 0; ; i++) {
    try { await R.connect(); break } catch (e) { if (i > 15) throw new KankyouError('RCON に繋がらない: ' + e.message); await sleep(2000) }
  }
  // jidoScan は起動の 100 tick（5秒）後。そこから clock が1秒以内に直す
  let naotta = false
  for (let i = 0; i < 30 && !naotta; i++) { await sleep(1000); naotta = await blockAru(0, 101, -404, 'minecraft:gold_block') }
  const nokoru = /passed/.test(await c('execute if entity @e[type=marker,tag=jidai_ginko_modori]'))
  check('(保険) 起動し直すと、残った印が外れて銀行が戻る', naotta && !nokoru, '戻った=' + naotta + ' / 印が残る=' + nokoru)
  const s2 = shippai()
  check('(保険) 印があった時も、起動時の印外しは失敗を出していない', s2.length === 0, JSON.stringify(s2.slice(0, 3)))
}

// =============================================================
async function main () {
  const nokosu = process.argv.includes('--nokosu')
  const dan = process.argv.slice(2).filter(a => !a.startsWith('--'))
  const zenbu = ['nakama', 'yumi', 'koroshi', 'gamen', 'tp', 'ryakudatsu', 'saikidou']
  const yaru = dan.length ? dan : zenbu
  const hyou = { nakama, yumi, koroshi, gamen, tp: tpKyoten, ryakudatsu, saikidou }
  for (const d of yaru) if (!hyou[d]) { console.log('知らない段: ' + d + '（' + zenbu.join(' ') + '）'); return 2 }

  console.log('=== 2-a. ボットで確かめる物（' + new Date().toISOString().slice(0, 10) + '）===')
  let tatta = false
  try {
    await sabaWoTsukuru()
    await sabaWoTateru()
    tatta = true
    R = new Rcon(RPORT, RPW)
    for (let i = 0; ; i++) {
      try { await R.connect(); break } catch (e) { if (i > 15) throw new KankyouError('RCON に繋がらない: ' + e.message); await sleep(2000) }
    }
    await setup()
    await junbi()
    for (const d of yaru) {
      try { await hyou[d]() } catch (e) { check('(' + d + ') 途中で止まった', false, e.stack.split('\n').slice(0, 3).join(' | ')) }
    }
  } catch (e) {
    if (e instanceof KankyouError) { console.log('[環境エラー] ' + e.message); return 2 }
    throw e
  } finally {
    for (const b of Object.values(BOTS)) { try { b.quit() } catch (e) {} }
    await sleep(1000)
    if (tatta && !nokosu) {
      await sabaWoTomeru()
      await utsushiWoKesu()
    }
    if (R) R.close()
  }
  console.log('')
  console.log('結果: PASS ' + kekka.ok.length + ' / FAIL ' + kekka.ng.length)
  for (const n of kekka.ng) console.log('  FAIL: ' + n)
  return kekka.ng.length ? 1 : 0
}

// ★ 受け止め損ねた失敗で node ごと落ちると、後片付け（サーバーを止めて写しを消す）が走らない。
//   落とさずに書き出すだけにする（その段の失敗は、段の中で FAIL として数えている）。
process.on('unhandledRejection', e => console.log('    （受け止め損ねた失敗）' + (e && e.message)))

main().then(code => process.exit(code), e => { console.log(e.stack); process.exit(2) })
