LUNA-TIC v4.0 (merged build)
============================
Combines Phase 3 (profiles, login, real moon phase) with Phase 4 (Nexus message,
numerology, daily draws, Termux wake-lock launcher).

Run in Termux
  pkg update && pkg install python
  pip install -r requirements.txt
  chmod +x termux_wake_lock.sh
  ./termux_wake_lock.sh
Then open http://127.0.0.1:5000

What's in v4.0
- Create profile / log in (passwords are hashed; multiple profiles per device)
- Real moon phase, % lit and moon age, calculated from the date
- Life Path numerology with master numbers 11, 22, 33
- Full decks: 22 Major Arcana, 24 runes, 52 playing cards
- Daily draws that stay the same all day for each profile
- Nexus message built from your moon phase and Life Path
- 7-day lunar forecast
- Server listens on this device only (127.0.0.1). To allow other devices
  on your network, start with LUNATIC_HOST=0.0.0.0

Upgrading from Phase 3: copy your old users.json here. Old plaintext passwords
still work.
