package net.kaneonexus.lunatic

import android.graphics.Color
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val BG = Color.parseColor("#05060D")
    private val SURFACE = Color.parseColor("#0B0E1A")
    private val CYAN = Color.parseColor("#00E5FF")
    private val MAGENTA = Color.parseColor("#E040FB")
    private val TEAL = Color.parseColor("#16C79A")
    private val GOLD = Color.parseColor("#F59E0B")
    private val TEXT = Color.parseColor("#E6ECF5")
    private val MUTED = Color.parseColor("#8F9AB3")

    private lateinit var store: LunaProfiles
    private lateinit var box: LinearLayout
    private var tab = 0
    private var createMode = false
    private var message = ""

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        store = LunaProfiles(this)
        val sv = ScrollView(this)
        sv.setBackgroundColor(BG)
        sv.fitsSystemWindows = true
        box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(20), dp(16), dp(20), dp(40))
        sv.addView(box)
        setContentView(sv)
        render()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun tv(text: String, size: Float, color: Int, bold: Boolean = false): TextView {
        val t = TextView(this)
        t.text = text
        t.textSize = size
        t.setTextColor(color)
        if (bold) t.setTypeface(t.typeface, android.graphics.Typeface.BOLD)
        return t
    }

    private fun card(vararg views: View): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setBackgroundColor(SURFACE)
        c.setPadding(dp(16), dp(14), dp(16), dp(14))
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(14)
        c.layoutParams = lp
        for (v in views) c.addView(v)
        return c
    }

    private fun button(label: String, filled: Boolean, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = label
        b.setTextColor(if (filled) BG else CYAN)
        b.setBackgroundColor(if (filled) CYAN else SURFACE)
        b.setOnClickListener { onClick() }
        return b
    }

    private fun field(hint: String, type: Int): EditText {
        val e = EditText(this)
        e.hint = hint
        e.inputType = type
        e.setSingleLine()
        e.setTextColor(TEXT)
        e.setHintTextColor(MUTED)
        return e
    }

    private fun parseDob(s: String): Triple<Int, Int, Int>? {
        return try {
            val f = SimpleDateFormat("MM/dd/yyyy", Locale.US)
            f.isLenient = false
            val d = f.parse(s.trim()) ?: return null
            val c = Calendar.getInstance()
            c.time = d
            Triple(c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.YEAR))
        } catch (e: Exception) {
            null
        }
    }

    private fun render() {
        box.removeAllViews()
        val title = tv("LUNA-TIC", 30f, CYAN, true)
        title.letterSpacing = 0.08f
        box.addView(title)
        box.addView(tv("v4.0 \u00B7 lunar and emotional forecast", 13f, MUTED))
        box.addView(View(this), LinearLayout.LayoutParams(1, dp(16)))
        val name = store.current()
        if (name == null) authScreen() else dashboard(name)
    }

    private fun authScreen() {
        val nameIn = field("Name", InputType.TYPE_CLASS_TEXT)
        val dobIn = field("Birth date MM/DD/YYYY", InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_DATE)
        val pwIn = field("Password (4+ characters)", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        val msg = tv(message, 14f, Color.parseColor("#FF6B6B"))
        val views = ArrayList<View>()
        views.add(tv(if (createMode) "Create profile" else "Log in", 20f, MAGENTA, true))
        views.add(msg)
        views.add(nameIn)
        if (createMode) views.add(dobIn)
        views.add(pwIn)
        views.add(button(if (createMode) "Create profile" else "Log in", true) {
            val n = nameIn.text.toString().trim()
            val pw = pwIn.text.toString()
            if (createMode) {
                val dob = dobIn.text.toString().trim()
                if (n.isEmpty() || pw.length < 4) message = "Enter a name and a password of 4+ characters."
                else if (parseDob(dob) == null) message = "Birth date must look like 05/22/1984."
                else if (!store.create(n, dob, pw)) message = "That name is taken. Log in instead."
                else message = ""
            } else {
                message = if (store.login(n, pw)) "" else "Name or password did not match."
            }
            render()
        })
        views.add(button(if (createMode) "I already have a profile" else "Create a profile", false) {
            createMode = !createMode
            message = ""
            render()
        })
        box.addView(card(*views.toTypedArray()))
    }

    private fun dashboard(name: String) {
        val nav = LinearLayout(this)
        nav.orientation = LinearLayout.HORIZONTAL
        val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        nav.addView(button("Today", tab == 0) { tab = 0; render() }, lp)
        nav.addView(button("7-day", tab == 1) { tab = 1; render() }, lp)
        nav.addView(button("Log out", false) { store.logout(); tab = 0; render() }, lp)
        box.addView(nav)
        box.addView(View(this), LinearLayout.LayoutParams(1, dp(14)))
        if (tab == 0) today(name) else forecast()
    }

    private fun today(name: String) {
        val now = Date()
        val m = Cosmos.moon(now)
        val dial = LunaDial(this)
        dial.setPhase(m.frac)
        val dlp = LinearLayout.LayoutParams(dp(220), dp(220))
        dlp.gravity = Gravity.CENTER_HORIZONTAL
        dlp.bottomMargin = dp(10)
        box.addView(dial, dlp)

        val head = tv(m.name, 24f, TEAL, true)
        head.gravity = Gravity.CENTER_HORIZONTAL
        box.addView(head, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        val sub = tv("${m.lit}% lit \u00B7 day ${m.age} of 29.5", 13f, MUTED)
        sub.gravity = Gravity.CENTER_HORIZONTAL
        val slp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        slp.bottomMargin = dp(16)
        box.addView(sub, slp)
        box.addView(card(tv(m.energy, 16f, GOLD, true), tv(m.advice, 14f, TEXT)))

        val dob = parseDob(store.dob(name))
        val lp = if (dob != null) Cosmos.lifePath(dob.first, dob.second, dob.third) else 0
        if (dob != null) box.addView(card(tv("Life Path $lp", 18f, MAGENTA, true), tv(Cosmos.lifeMeaning(lp), 14f, TEXT)))

        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now)
        val d = Cosmos.draws(name, day)
        box.addView(card(tv("Tarot: ${d.tarot.first}", 16f, CYAN, true), tv(d.tarot.second, 14f, TEXT)))
        box.addView(card(tv("Rune: ${d.rune.first}", 16f, CYAN, true), tv(d.rune.second, 14f, TEXT)))
        box.addView(card(tv("Card: ${d.card.first}", 16f, CYAN, true), tv(d.card.second, 14f, TEXT)))
        box.addView(card(tv("Nexus says", 18f, MAGENTA, true), tv(Cosmos.nexusMessage(name, m, lp), 14f, TEXT)))
        box.addView(tv("Draws stay the same all day.", 12f, MUTED))
    }

    private fun forecast() {
        val fmt = SimpleDateFormat("EEE MMM d", Locale.US)
        val now = System.currentTimeMillis()
        for (i in 0..6) {
            val dt = Date(now + i * 86400000L)
            val m = Cosmos.moon(dt)
            box.addView(card(
                tv(m.name, 18f, TEAL, true),
                tv("${fmt.format(dt)} \u00B7 ${m.lit}% lit", 12f, MUTED),
                tv("${m.energy}. ${m.advice}", 14f, TEXT)))
        }
    }
}
