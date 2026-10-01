"""LUNA-TIC v4.0 - merged build.
Phase 3 profiles/login + real moon phase, Phase 4 Nexus message + termux launcher,
plus full decks, daily-stable draws and a 7-day forecast. Runs offline in Termux."""
import json, math, os, random, secrets, hashlib
from datetime import datetime, timedelta, timezone
from flask import Flask, render_template_string, request, redirect, session
from werkzeug.security import generate_password_hash, check_password_hash

BASE = os.path.dirname(os.path.abspath(__file__))
USER_FILE = os.path.join(BASE, "users.json")
KEY_FILE = os.path.join(BASE, ".secret_key")

def _secret():
    if not os.path.exists(KEY_FILE):
        with open(KEY_FILE, "w") as f:
            f.write(secrets.token_hex(32))
    return open(KEY_FILE).read().strip()

app = Flask(__name__)
app.secret_key = _secret()

# ---------- storage ----------
def load_users():
    if not os.path.exists(USER_FILE):
        return {}
    try:
        with open(USER_FILE) as f:
            return json.load(f)
    except (ValueError, OSError):
        return {}

def save_users(users):
    with open(USER_FILE, "w") as f:
        json.dump(users, f, indent=2)

def check_pw(user, pw):
    stored = user.get("password", "")
    if stored.startswith(("pbkdf2:", "scrypt:")):
        return check_password_hash(stored, pw)
    return stored == pw  # legacy plaintext from Phase 3

# ---------- moon ----------
SYNODIC = 29.53058867
PHASES = ["New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous",
          "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent"]
EMOJI = ["🌑", "🌒", "🌓", "🌔", "🌕", "🌖", "🌗", "🌘"]
PHASE_MAP = {
    "New Moon": ("Reset, seed, intention", "Quiet and reflective. Plant intentions and keep plans light."),
    "Waxing Crescent": ("Sprout, momentum, hope", "Curious and hopeful. Take small steps and avoid overcommitting."),
    "First Quarter": ("Action, challenge, build", "Driven but tense. Make decisive moves and cut distractions."),
    "Waxing Gibbous": ("Refine, align, adjust", "Analytical and purposeful. Review plans and fine-tune."),
    "Full Moon": ("Peak, reveal, release", "Intense and expressive. Celebrate wins and release what is heavy."),
    "Waning Gibbous": ("Harvest, integrate, teach", "Grateful and insightful. Share what you have learned."),
    "Last Quarter": ("Reassess, simplify, boundaries", "Sober and discerning. Say no and clear clutter."),
    "Waning Crescent": ("Rest, surrender, dream", "Sleepy and inward. Restore, journal and prepare for the reset."),
}

def moon(dt=None):
    dt = dt or datetime.now(timezone.utc)
    jd = dt.timestamp() / 86400 + 2440587.5
    frac = ((jd - 2451550.1) / SYNODIC) % 1
    i = int(frac * 8 + 0.5) % 8
    lit = round((1 - math.cos(2 * math.pi * frac)) / 2 * 100)
    return {"name": PHASES[i], "emoji": EMOJI[i], "lit": lit, "age": round(frac * SYNODIC, 1),
            "energy": PHASE_MAP[PHASES[i]][0], "advice": PHASE_MAP[PHASES[i]][1]}

# ---------- numerology ----------
LIFE_PATH = {
    1: "Leader, pioneer, independent force of will.", 2: "Peacemaker, intuitive, emotionally intelligent.",
    3: "Creative communicator, joyful and expressive.", 4: "Builder, grounded, stability through work.",
    5: "Adventurer, freedom seeker, change agent.", 6: "Nurturer, healer, home and heart focused.",
    7: "Seeker of truth, spiritual analyst, deep thinker.", 8: "Powerful manifestor, balanced with karma.",
    9: "Humanitarian, wise old soul, emotional depth.", 11: "Spiritual illuminator, sensitive channel.",
    22: "Master builder, grounded visionary.", 33: "Master teacher, divine nurturer of all."}

def parse_dob(s):
    try:
        return datetime.strptime(s.strip(), "%m/%d/%Y")
    except ValueError:
        return None

def life_path(dob):
    total = sum(int(d) for d in dob.strftime("%m%d%Y"))
    while total > 9 and total not in (11, 22, 33):
        total = sum(int(d) for d in str(total))
    return total

# ---------- decks ----------
TAROT = [("The Fool", "New beginnings, innocence."), ("The Magician", "Manifestation, resourcefulness."),
    ("The High Priestess", "Intuition, hidden knowledge."), ("The Empress", "Abundance, nurturing."),
    ("The Emperor", "Structure, authority."), ("The Hierophant", "Tradition, guidance."),
    ("The Lovers", "Union, meaningful choices."), ("The Chariot", "Willpower, forward drive."),
    ("Strength", "Quiet courage, compassion."), ("The Hermit", "Solitude, inner search."),
    ("Wheel of Fortune", "Cycles, turning luck."), ("Justice", "Truth, fair outcomes."),
    ("The Hanged One", "Surrender, new perspective."), ("Death", "Endings that clear the way."),
    ("Temperance", "Balance, patience."), ("The Devil", "Attachments to look at honestly."),
    ("The Tower", "Sudden change, breakthrough."), ("The Star", "Hope, renewal."),
    ("The Moon", "Illusions, intuition, dreams."), ("The Sun", "Joy, vitality, success."),
    ("Judgement", "Reckoning, calling."), ("The World", "Completion, wholeness.")]
RUNES = [("Fehu", "Wealth, energy flowing."), ("Uruz", "Strength, raw vitality."), ("Thurisaz", "Defense, a threshold."),
    ("Ansuz", "Communication, divine messages."), ("Raidho", "Journey, soul travel."), ("Kenaz", "Torch, insight."),
    ("Gebo", "Gift, exchange."), ("Wunjo", "Joy, harmony."), ("Hagalaz", "Disruption that clears."),
    ("Nauthiz", "Need, endurance."), ("Isa", "Stillness, pause."), ("Jera", "Harvest, right timing."),
    ("Eihwaz", "Resilience, the yew."), ("Perthro", "Mystery, fate."), ("Algiz", "Protection."),
    ("Sowilo", "Sun, success."), ("Tiwaz", "Justice, courage."), ("Berkano", "Growth, renewal."),
    ("Ehwaz", "Trust, partnership."), ("Mannaz", "Self, community."), ("Laguz", "Flow, intuition."),
    ("Ingwaz", "Seed, gestation."), ("Dagaz", "Dawn, breakthrough."), ("Othala", "Home, heritage.")]
SUITS = {"Hearts": "emotions and relationships", "Spades": "transformation and clarity",
         "Diamonds": "work, money and collaboration", "Clubs": "energy, drive and creativity"}
RANKS = {"Ace": "a fresh start in", "2": "balance and choice in", "3": "growth in", "4": "stability in",
    "5": "change or tension in", "6": "harmony in", "7": "reflection on", "8": "movement in", "9": "near completion of",
    "10": "fulfilment in", "Jack": "a messenger for", "Queen": "mastery and care in", "King": "authority over"}
CARDS = [(f"{r} of {s}", f"{m.capitalize()} {SUITS[s]}.") for s, _ in SUITS.items() for r, m in RANKS.items()]

def draws(name, day):
    seed = int(hashlib.sha256(f"{name}|{day}".encode()).hexdigest(), 16)
    rng = random.Random(seed)
    return {"tarot": rng.choice(TAROT), "rune": rng.choice(RUNES), "card": rng.choice(CARDS)}

def nexus_message(name, m, lp):
    return (f"{name}, under the {m['name']} your energy leans toward {m['energy'].lower()}. "
            f"Life Path {lp}: {LIFE_PATH.get(lp, 'A rare soul number.')} {m['advice']}")

# ---------- pages ----------
PAGE = """<!doctype html><html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1"><title>LUNA-TIC v4.0</title>
<style>
body{background:#05060d;color:#e6ecf5;font-family:system-ui,sans-serif;margin:0;padding:16px;text-align:center}
.wrap{max-width:460px;margin:0 auto}h1{color:#00e5ff;margin:8px 0 2px}.sub{color:#8f9ab3;margin:0 0 14px}
.card{background:#0b0e1a;border:1px solid #1a2036;border-radius:10px;padding:14px;margin:12px 0;text-align:left}
h2,h3{margin:0 0 6px;color:#e040fb}.big{font-size:44px}.m{color:#8f9ab3}
input,button{width:100%;padding:10px;margin:5px 0;border-radius:8px;border:1px solid #26304f;background:#10142a;color:#fff;box-sizing:border-box}
button{background:#00e5ff;color:#021018;font-weight:700;cursor:pointer}a{color:#00e5ff}.err{color:#ff6b6b}
nav a{margin:0 8px}
</style></head><body><div class="wrap"><h1>🌙 LUNA-TIC</h1><p class="sub">v4.0 · lunar and emotional forecast</p>
{% if session.get('user') %}<nav><a href="/dashboard">Today</a><a href="/forecast">7-day</a><a href="/logout">Log out</a></nav>{% endif %}
{% block body %}{% endblock %}</div></body></html>"""

def page(body, **ctx):
    tpl = PAGE.replace("{% block body %}{% endblock %}", body)
    return render_template_string(tpl, **ctx)

AUTH = """<form method="post" class="card"><h2>{{title}}</h2>{% if err %}<p class="err">{{err}}</p>{% endif %}
<input name="name" placeholder="Name" required maxlength="40">
{% if create %}<input name="birthdate" placeholder="Birth date MM/DD/YYYY" required>{% endif %}
<input name="password" type="password" placeholder="Password" required>
<button>{{title}}</button><p><a href="{{other}}">{{other_text}}</a></p></form>"""

@app.route("/")
def index():
    return redirect("/dashboard" if "user" in session else "/login")

@app.route("/create", methods=["GET", "POST"])
def create():
    err = None
    if request.method == "POST":
        name = request.form["name"].strip()
        dob = parse_dob(request.form["birthdate"])
        pw = request.form["password"]
        users = load_users()
        if not name or len(pw) < 4:
            err = "Enter a name and a password of at least 4 characters."
        elif dob is None:
            err = "Birth date must look like 05/22/1984."
        elif name in users:
            err = "That name is taken. Log in instead."
        else:
            users[name] = {"birthdate": dob.strftime("%m/%d/%Y"), "password": generate_password_hash(pw)}
            save_users(users)
            session["user"] = name
            return redirect("/dashboard")
    return page(AUTH, title="Create profile", create=True, err=err, other="/login", other_text="Already have a profile?")

@app.route("/login", methods=["GET", "POST"])
def login():
    err = None
    if request.method == "POST":
        name = request.form["name"].strip()
        user = load_users().get(name)
        if user and check_pw(user, request.form["password"]):
            session["user"] = name
            return redirect("/dashboard")
        err = "Name or password did not match."
    return page(AUTH, title="Log in", create=False, err=err, other="/create", other_text="Create a profile")

def current():
    name = session.get("user")
    user = load_users().get(name) if name else None
    if not user:
        session.clear()
        return None, None
    return name, user

DASH = """<div class="card"><h2>{{m.emoji}} {{m.name}}</h2><p class="m">{{m.lit}}% lit · day {{m.age}} of 29.5</p>
<p>{{m.energy}}. {{m.advice}}</p></div>
<div class="card"><h2>Life Path {{lp}}</h2><p>{{meaning}}</p></div>
<div class="card"><h3>🔮 Tarot: {{d.tarot[0]}}</h3><p>{{d.tarot[1]}}</p></div>
<div class="card"><h3>ᚱ Rune: {{d.rune[0]}}</h3><p>{{d.rune[1]}}</p></div>
<div class="card"><h3>🃏 Card: {{d.card[0]}}</h3><p>{{d.card[1]}}</p></div>
<div class="card"><h2>🧠 Nexus says</h2><p>{{msg}}</p></div>
<p class="m">Draws stay the same all day.</p>"""

@app.route("/dashboard")
def dashboard():
    name, user = current()
    if not user:
        return redirect("/login")
    dob = parse_dob(user.get("birthdate", ""))
    lp = life_path(dob) if dob else None
    m = moon()
    d = draws(name, datetime.now().strftime("%Y-%m-%d"))
    return page(DASH, m=m, lp=lp or "?", meaning=LIFE_PATH.get(lp, "Add a valid birth date to see your number."),
                d=d, msg=nexus_message(name, m, lp or "?"))

FORE = """{% for day, m in days %}<div class="card"><h3>{{m.emoji}} {{m.name}}</h3>
<p class="m">{{day}} · {{m.lit}}% lit</p><p>{{m.energy}}. {{m.advice}}</p></div>{% endfor %}"""

@app.route("/forecast")
def forecast():
    name, user = current()
    if not user:
        return redirect("/login")
    now = datetime.now(timezone.utc)
    days = [((now + timedelta(days=i)).strftime("%a %b %d"), moon(now + timedelta(days=i))) for i in range(7)]
    return page(FORE, days=days)

@app.route("/logout")
def logout():
    session.clear()
    return redirect("/login")

if __name__ == "__main__":
    app.run(host=os.environ.get("LUNATIC_HOST", "127.0.0.1"), port=int(os.environ.get("LUNATIC_PORT", "5000")))
