// Zapisuje emotki z emotes.js do zasobów moda (Player Animation Library czyta assets/<mod>/player_animations/*.json).
// Uruchom z głównego folderu repo: node tools/emotes/build.js
const fs = require('fs');
const path = require('path');
const { EMOTES, toEmotecraft } = require('./emotes.js');

const out = path.join(__dirname, '..', '..', 'src', 'main', 'resources', 'assets', 'gruzclient', 'player_animations');
fs.mkdirSync(out, { recursive: true });
for (const id of Object.keys(EMOTES)) {
  const json = toEmotecraft(id);
  fs.writeFileSync(path.join(out, `${id}.json`), JSON.stringify(json));
  console.log(`${id}.json – ${json.emote.moves.length} klatek, ${json.emote.stopTick} ticków`);
}
