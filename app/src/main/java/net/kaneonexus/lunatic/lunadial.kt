import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min

class LunaDial(ctx: Context) : View(ctx) {
    private var frac = 0.0
    private val dark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0D1226") }
    private val lit = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E9F6FF") }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8800E5FF"); style = Paint.Style.STROKE; strokeWidth = 4f
    }
    private val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#66E040FB"); style = Paint.Style.STROKE; strokeWidth = 2f
    }

    fun setPhase(f: Double) { frac = f; invalidate() }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = min(cx, cy) - 14f
        canvas.drawCircle(cx, cy, r + 8f, halo)
        canvas.drawCircle(cx, cy, r, dark)
        val c = cos(2 * PI * frac).toFloat()
        val box = RectF(cx - r, cy - r, cx + r, cy + r)
        if (frac < 0.5) canvas.drawArc(box, -90f, 180f, true, lit) else canvas.drawArc(box, 90f, 180f, true, lit)
        val rx = r * abs(c)
        canvas.drawOval(RectF(cx - rx, cy - r, cx + rx, cy + r), if (c > 0) dark else lit)
        canvas.drawCircle(cx, cy, r, ring)
    }
}
