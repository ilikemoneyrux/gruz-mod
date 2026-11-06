// Emotki Gruz z zginaniem (łokcie, kolana, tułów) – źródło animacji dla Player Animation Library.
// `node tools/emotes/build.js` zapisuje z tego pliki assets/gruzclient/player_animations/<id>.json
// (format Emotecraft / PlayerAnimator, kąty w stopniach). Ten sam plik czyta podgląd tools/emotes/preview.html.
//
// Kąty jak w modelu Minecrafta: pitch = obrót do przodu/tyłu (ujemny = ręka do przodu i w górę),
// yaw = obrót w poziomie, roll = na boki (prawa ręka: dodatni = w bok i w górę).
// bend: łokieć – UJEMNY zgina przedramię do przodu; kolano – DODATNI zgina łydkę do tyłu;
// torso.bend – dodatni pochyla górną połowę tułowia (z głową i rękami) do przodu.
// torso.pitch/yaw/roll obraca całą postać wokół bioder (nogi trzeba wtedy kontrować).
(function (root) {
  const PI = Math.PI;
  const s = Math.sin, c = Math.cos;
  const clamp01 = (x) => Math.max(0, Math.min(1, x));
  const smooth = (x) => { x = clamp01(x); return x * x * x * (x * (x * 6 - 15) + 10); };
  const pulse = (a) => 0.5 - 0.5 * c(a);
  const back = (x) => { x = clamp01(x); const c1 = 1.4, c3 = c1 + 1, y = x - 1; return 1 + c3 * y * y * y + c1 * y * y; };
  const mix = (a, b, k) => a + (b - a) * k;

  // id, czas (s) – taki sam jak Emote.durationMs w modzie
  const EMOTES = {
    wave: {
      dur: 3.0,
      pose(t, d) {
        const amp = smooth((t - 0.2) / 0.3) * smooth((d - t - 0.2) / 0.35);
        const w = s(t * 11), w2 = s(t * 11 - 1.0);
        return {
          rightArm: { pitch: -92, yaw: 32, roll: 6 + 15 * w * amp, bend: -82 + 12 * w2 * amp },
          leftArm: { pitch: 6 + 3 * s(t * 2.2), roll: -6, bend: -12 },
          head: { pitch: -6, yaw: -16, roll: 4 + 2 * w * amp },
          torso: { yaw: -6 + 2.5 * w2 * amp, bend: 4 },
        };
      },
    },
    cheer: {
      dur: 2.6,
      pose(t) {
        const b = t * 2 * PI * 1.55, hop = pulse(b), sq = hop * hop;
        return {
          rightArm: { pitch: -10 - 6 * hop, yaw: -8, roll: 148 + 18 * sq, bend: -18 - 22 * (1 - hop) },
          leftArm: { pitch: -10 - 6 * hop, yaw: 8, roll: -148 - 18 * sq, bend: -18 - 22 * (1 - hop) },
          head: { pitch: -20 - 8 * hop, roll: 4 * s(b * 0.5) },
          torso: { bend: -4 * hop + 6 * (1 - hop) },
          rightLeg: { pitch: -14 * (1 - hop), bend: 26 * (1 - hop), roll: 3 * sq },
          leftLeg: { pitch: -14 * (1 - hop), bend: 26 * (1 - hop), roll: -3 * sq },
        };
      },
    },
    clap: {
      dur: 2.8,
      pose(t) {
        const ph = t * 2 * PI * 2.3, open = Math.pow(pulse(ph), 0.65), closed = 1 - open;
        const yaw = 14 + 24 * open;
        return {
          rightArm: { pitch: -62 + 4 * closed, yaw: -yaw, roll: -4, bend: -62 },
          leftArm: { pitch: -62 + 4 * closed, yaw: yaw, roll: 4, bend: -62 },
          head: { pitch: 4 + 5 * closed },
          torso: { bend: 3 + 3 * closed },
        };
      },
    },
    dance: {
      dur: 4.8,
      pose(t) {
        const beat = t * 2 * PI * 1.9;
        const step = s(beat * 0.5), bounce = pulse(beat), late = s(beat * 0.5 - 0.6);
        return {
          rightArm: { pitch: -60 - 55 * late, yaw: -8, roll: 22 + 14 * bounce, bend: -70 + 25 * late },
          leftArm: { pitch: -60 + 55 * late, yaw: 8, roll: -22 - 14 * bounce, bend: -70 - 25 * late },
          head: { pitch: -2 + 9 * bounce, roll: 8 * step },
          torso: { yaw: 16 * step, roll: 3 * step, bend: 8 * bounce },
          rightLeg: { pitch: -16 * step - 10 * bounce, yaw: -16 * step, roll: -3 * step, bend: 10 + 22 * bounce },
          leftLeg: { pitch: 16 * step - 10 * bounce, yaw: -16 * step, roll: -3 * step, bend: 10 + 22 * bounce },
        };
      },
    },
    salute: {
      dur: 2.6,
      pose(t) {
        const r = back((t - 0.0) / 0.45);
        return {
          rightArm: { pitch: mix(0, -96, r), yaw: mix(0, -24, r), roll: mix(0, 14, r), bend: mix(0, -108, r) },
          leftArm: { pitch: 2, roll: -3, bend: -4 },
          head: { pitch: -8, yaw: 0 },
          torso: { bend: -3 },
        };
      },
    },
    facepalm: {
      dur: 2.8,
      pose(t) {
        const r = back(t / 0.4), drop = smooth((t - 0.3) / 0.45);
        return {
          rightArm: { pitch: mix(0, -82, r), yaw: mix(0, -28, r), roll: mix(0, 4, r), bend: mix(0, -118, r) },
          leftArm: { pitch: -10, yaw: 6, roll: -4, bend: -20 },
          head: { pitch: 22 * drop, yaw: 8 * s(t * 5.2) * drop },
          torso: { bend: 14 * drop },
          rightLeg: { bend: 4 * drop },
          leftLeg: { bend: 4 * drop },
        };
      },
    },
    bow: {
      dur: 2.4,
      pose(t, d) {
        const down = smooth((t - 0.1) / 0.55) * smooth((d - t - 0.35) / 0.55);
        return {
          rightArm: { pitch: -48, yaw: -36, roll: 6, bend: -88 },
          leftArm: { pitch: 26, yaw: 18, roll: -8, bend: -55 },
          head: { pitch: 18 * down },
          torso: { bend: 52 * down },
          rightLeg: { pitch: -4 * down, bend: 8 * down },
          leftLeg: { pitch: -4 * down, bend: 8 * down },
        };
      },
    },
    point: {
      dur: 2.4,
      pose(t) {
        const r = back(t / 0.34), idle = s(t * 3.1);
        return {
          rightArm: { pitch: mix(0, -90, r) + 2 * idle, yaw: mix(0, -4, r), roll: 0, bend: mix(-40, -4, r) },
          leftArm: { pitch: 8, roll: -6, bend: -14 },
          head: { pitch: -3 + idle, yaw: -6 },
          torso: { yaw: -8 * r, bend: 2 },
          rightLeg: { yaw: 8 * r },
          leftLeg: { yaw: 8 * r },
        };
      },
    },
    // ---- płatne emotki we dwoje (grają dwie osoby naraz, stoją przodem do siebie) ----
    highfive: {
      dur: 2.6,
      pose(t) {
        const up = back((t - 0.05) / 0.45);
        const slap = smooth((t - 0.75) / 0.1) * smooth((1.2 - t) / 0.3);
        const after = smooth((t - 1.1) / 0.4);
        return {
          rightArm: { pitch: mix(0, -138, up) + 14 * slap, yaw: mix(0, -20, up), roll: mix(0, 10, up), bend: mix(0, -14, up) + 12 * slap - 40 * after },
          leftArm: { pitch: mix(0, 8, up), roll: -6, bend: -14 - 30 * after },
          head: { pitch: -10 + 6 * slap, roll: 6 * after * Math.sin(t * 6) },
          torso: { bend: 10 * slap - 3 * after, yaw: -6 * up },
          rightLeg: { pitch: -10 * slap, bend: 8 * slap },
        };
      },
    },
    hug: {
      dur: 3.4,
      pose(t) {
        const wrap = smooth((t - 0.1) / 0.5), sway = Math.sin(t * 2.4);
        return {
          rightArm: { pitch: mix(0, -82, wrap), yaw: mix(0, -52, wrap), roll: mix(0, 8, wrap), bend: mix(0, -62, wrap) },
          leftArm: { pitch: mix(0, -76, wrap), yaw: mix(0, 52, wrap), roll: mix(0, -8, wrap), bend: mix(0, -58, wrap) },
          head: { pitch: 10 * wrap, yaw: -18 * wrap, roll: 10 * wrap + 3 * sway * wrap },
          torso: { bend: 14 * wrap, roll: 3 * sway * wrap },
          rightLeg: { bend: 4 * wrap }, leftLeg: { bend: 4 * wrap },
        };
      },
    },
    duodance_a: { dur: 5.0, pose(t) { return EMOTES.dance.pose(t); } },
    duodance_b: { dur: 5.0, pose(t) { return mirror(EMOTES.dance.pose(t)); } },
  };

  /** Lustrzane odbicie pozy (lewa ↔ prawa) – druga osoba w tańcu w parze */
  function mirror(p) {
    const out = {};
    const flip = (a) => { const o = { ...a }; if ('yaw' in o) o.yaw = -o.yaw; if ('roll' in o) o.roll = -o.roll; return o; };
    for (const [part, axes] of Object.entries(p)) {
      const name = part.startsWith('right') ? 'left' + part.slice(5) : part.startsWith('left') ? 'right' + part.slice(4) : part;
      out[name] = flip(axes);
    }
    return out;
  }

  const BEGIN = 6;   // ticki płynnego wejścia (biblioteka miesza z normalną pozą)
  const OUT = 9;     // ticki płynnego wyjścia

  /** Plik JSON w formacie Emotecraft dla jednej emotki (klatka co tick, liniowo – i tak jest gładko) */
  function toEmotecraft(id) {
    const e = EMOTES[id];
    const stopTick = Math.round(e.dur * 20);
    const endTick = stopTick - OUT;
    const moves = [];
    const r1 = (v) => Math.round(v * 10) / 10;
    for (let tick = BEGIN; tick <= endTick; tick++) {
      const p = e.pose(tick / 20, e.dur);
      const move = { tick, easing: 'linear' };
      for (const [part, axes] of Object.entries(p)) {
        const o = {};
        for (const [k, v] of Object.entries(axes)) o[k] = r1(v);
        move[part] = o;
      }
      moves.push(move);
    }
    return {
      name: id, author: 'Gruz Client', description: 'Gruz Client emote: ' + id,
      emote: { beginTick: BEGIN, endTick, stopTick, isLoop: false, returnTick: 0, degrees: true, applyBendToOtherBones: true, moves },
    };
  }

  const api = { EMOTES, toEmotecraft, BEGIN, OUT };
  if (typeof module !== 'undefined') module.exports = api; else root.GruzEmotes = api;
})(this);
