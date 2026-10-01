package net.kaneonexus.lunatic

import android.content.Context
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

class LunaProfiles(ctx: Context) {
    private val sp = ctx.getSharedPreferences("luna_profiles", Context.MODE_PRIVATE)

    private fun users(): JSONObject = JSONObject(sp.getString("users", "{}") ?: "{}")

    private fun hash(salt: String, pw: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + pw).toByteArray()).joinToString("") { "%02x".format(it) }

    fun create(name: String, dob: String, pw: String): Boolean {
        val u = users()
        if (u.has(name)) return false
        val salt = UUID.randomUUID().toString()
        u.put(name, JSONObject().put("dob", dob).put("salt", salt).put("hash", hash(salt, pw)))
        sp.edit().putString("users", u.toString()).putString("current", name).apply()
        return true
    }

    fun login(name: String, pw: String): Boolean {
        val o = users().optJSONObject(name) ?: return false
        if (hash(o.getString("salt"), pw) != o.getString("hash")) return false
        sp.edit().putString("current", name).apply()
        return true
    }

    fun current(): String? {
        val n = sp.getString("current", null) ?: return null
        return if (users().has(n)) n else null
    }

    fun dob(name: String): String = users().getJSONObject(name).getString("dob")

    fun logout() { sp.edit().remove("current").apply() }
}
